package com.nevruz.aegis.core.bridge

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class ScanVerdict(val isSafe: Boolean, val riskScore: Int, val reason: String)

object AegisNativeBridge {
    @Volatile private var nativeAvailable = false

    init {
        nativeAvailable = try {
            System.loadLibrary("aegis_core")
            true
        } catch (_: UnsatisfiedLinkError) { false }
          catch (_: SecurityException) { false }
    }

    private external fun nativeInspectUrl(url: String): String

    suspend fun inspectUrlFast(url: String): ScanVerdict = withContext(Dispatchers.Default) {
        if (!nativeAvailable) return@withContext ScanVerdict(false, 0, "Yerel tarama motoru yüklenemedi")
        try {
            val json = JSONObject(nativeInspectUrl(url))
            ScanVerdict(
                json.optBoolean("is_safe", false),
                json.optInt("risk_score", 0).coerceIn(0, 100),
                json.optString("reason", "Analiz sonucu alınamadı")
            )
        } catch (_: Exception) {
            ScanVerdict(false, 0, "Analiz tamamlanamadı")
        }
    }

    fun isNativeAvailable(): Boolean = nativeAvailable
}
