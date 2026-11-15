package com.aegisguard.core.privilege

import android.content.Context
import android.content.pm.PackageManager
import rikka.shizuku.Shizuku
import java.io.DataOutputStream

enum class PrivilegeTier {
    STANDARD, // Varsayılan: Accessibility Kalkanı + Spoofing + Ön Analiz (VPN Yok)
    SHIZUKU,  // Gelişmiş: Sistem geneli yerel HTTP Proxy + Pil kısıtlaması kaldırma
    ROOT      // Maksimum: iptables transparan proxy yönlendirmesi (VPN'e dokunmaz)
}

object PrivilegeManager {

    fun getActiveTier(useRoot: Boolean, useShizuku: Boolean): PrivilegeTier {
        if (useRoot && hasRootAccess()) return PrivilegeTier.ROOT
        if (useShizuku && isShizukuAvailable()) return PrivilegeTier.SHIZUKU
        return PrivilegeTier.STANDARD
    }

    fun hasRootAccess(): Boolean {
        return try {
            val process = Runtime.getRuntime().exec("su")
            val os = DataOutputStream(process.outputStream)
            os.writeBytes("id\nexit\n")
            os.flush()
            process.waitFor() == 0
        } catch (e: Exception) {
            false
        }
    }

    fun isShizukuAvailable(): Boolean {
        return try {
            Shizuku.pingBinder() && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        } catch (e: Exception) {
            false
        }
    }

    fun requestShizukuPermission(requestCode: Int) {
        try {
            if (Shizuku.pingBinder() && Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
                Shizuku.requestPermission(requestCode)
            }
        } catch (_: Exception) { }
    }

    /**
     * VPN kullanmadan sistem genelinde derin koruma özelliklerini uygular.
     */
    fun applyEnhancedProtections(context: Context, tier: PrivilegeTier, enable: Boolean) {
        when (tier) {
            PrivilegeTier.ROOT -> {
                if (enable) {
                    // VPN arayüzüne dokunmadan yerel analiz portuna (8888) transparan yönlendirme
                    executeRootCommand("iptables -t nat -A OUTPUT -p tcp --dport 80 -j REDIRECT --to-port 8888")
                } else {
                    executeRootCommand("iptables -t nat -D OUTPUT -p tcp --dport 80 -j REDIRECT --to-port 8888")
                }
            }
            PrivilegeTier.SHIZUKU -> {
                if (enable) {
                    // Shizuku üzerinden pil optimizasyonu muafiyeti ve yerel proxy ayarı
                    executeShizukuShell("dumpsys deviceidle whitelist +${context.packageName}")
                    executeShizukuShell("settings put global http_proxy 127.0.0.1:8888")
                } else {
                    executeShizukuShell("settings put global http_proxy :0")
                }
            }
            PrivilegeTier.STANDARD -> {
                // Ekstra sistem komutu gerekmez, WebGuardAccessibilityService standart katmanda çalışır.
            }
        }
    }

    private fun executeRootCommand(command: String): Boolean {
        return try {
            val process = Runtime.getRuntime().exec("su")
            val os = DataOutputStream(process.outputStream)
            os.writeBytes("$command\nexit\n")
            os.flush()
            process.waitFor() == 0
        } catch (e: Exception) {
            false
        }
    }

    private fun executeShizukuShell(command: String) {
        try {
            // Shizuku kabuk yürütme taslağı
            // Shizuku.newProcess(arrayOf("sh", "-c", command), null, null)
        } catch (_: Exception) { }
    }
}
