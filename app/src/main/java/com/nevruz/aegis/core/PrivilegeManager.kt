package com.nevruz.aegis.core.privilege

import android.content.Context
import android.content.pm.PackageManager
import rikka.shizuku.Shizuku
import java.io.DataOutputStream

enum class PrivilegeTier { STANDARD, SHIZUKU, ROOT }

object PrivilegeManager {

    fun getActiveTier(useRoot: Boolean, useShizuku: Boolean): PrivilegeTier {
        if (useRoot && hasRootAccess()) return PrivilegeTier.ROOT
        if (useShizuku && isShizukuAvailable()) return PrivilegeTier.SHIZUKU
        return PrivilegeTier.STANDARD
    }

    fun hasRootAccess(): Boolean = try {
        val p = Runtime.getRuntime().exec("su")
        DataOutputStream(p.outputStream).apply {
            writeBytes("id\nexit\n")
            flush()
        }
        p.waitFor() == 0
    } catch (_: Exception) { false }

    fun isShizukuAvailable(): Boolean = try {
        Shizuku.pingBinder() && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
    } catch (_: Exception) { false }

    fun requestShizukuPermission(code: Int) {
        try {
            if (Shizuku.pingBinder() && Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
                Shizuku.requestPermission(code)
            }
        } catch (_: Exception) { }
    }

    fun applyDeepHooks(context: Context, tier: PrivilegeTier, enable: Boolean) {
        when (tier) {
            PrivilegeTier.ROOT -> {
                val op = if (enable) "-A" else "-D"
                execSu("iptables -t nat $op OUTPUT -p tcp --dport 80 -j REDIRECT --to-port 8888")
            }
            PrivilegeTier.SHIZUKU -> {
                if (enable) {
                    execShizuku("dumpsys deviceidle whitelist +${context.packageName}")
                    execShizuku("settings put global http_proxy 127.0.0.1:8888")
                } else {
                    execShizuku("settings put global http_proxy :0")
                }
            }
            PrivilegeTier.STANDARD -> { /* VPN ve Root olmadan standart servis çalışır */ }
        }
    }

    private fun execSu(cmd: String) {
        try {
            val p = Runtime.getRuntime().exec("su")
            DataOutputStream(p.outputStream).apply { writeBytes("$cmd\nexit\n"); flush() }
            p.waitFor()
        } catch (_: Exception) { }
    }

    private fun execShizuku(cmd: String) {
        try {
            // Shizuku.newProcess(arrayOf("sh", "-c", cmd), null, null)
        } catch (_: Exception) { }
    }
}
