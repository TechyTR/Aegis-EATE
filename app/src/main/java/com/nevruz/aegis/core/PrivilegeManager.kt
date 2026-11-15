package com.nevruz.aegis.core.privilege

import android.content.Context
import android.content.pm.PackageManager
import rikka.shizuku.Shizuku

enum class PrivilegeTier { STANDARD, SHIZUKU, ROOT }

/** Capability detection only. Global system mutations remain disabled pending rollback tests. */
object PrivilegeManager {
    fun getActiveTier(useRoot: Boolean, useShizuku: Boolean): PrivilegeTier {
        if (useRoot && hasRootAccess()) return PrivilegeTier.ROOT
        if (useShizuku && isShizukuAvailable()) return PrivilegeTier.SHIZUKU
        return PrivilegeTier.STANDARD
    }

    fun hasRootAccess(): Boolean = try {
        val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "id"))
        process.inputStream.close()
        process.errorStream.close()
        process.waitFor() == 0
    } catch (_: Exception) { false }

    fun isShizukuAvailable(): Boolean = try {
        Shizuku.pingBinder() &&
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
    } catch (_: Exception) { false }

    fun requestShizukuPermission(code: Int) {
        try {
            if (Shizuku.pingBinder() &&
                Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED
            ) Shizuku.requestPermission(code)
        } catch (_: Exception) { }
    }

    fun applyDeepHooks(context: Context, tier: PrivilegeTier, enable: Boolean) {
        // Intentionally disabled: never alter global proxy, firewall, or device-idle settings here.
    }
}
