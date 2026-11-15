package com.nevruz.aegis.core.bridge

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class ScanVerdict(
    val isSafe: Boolean,
    val riskScore: Int,
    val reason: String
)

object AegisNativeBridge {

    init {
        System.loadLibrary("aegis_core")
    }

    private external fun nativeInspectUrl(url: String): String

    suspend fun inspectUrlFast(url: String): ScanVerdict = withContext(Dispatchers.IO) {
        val rawJson = nativeInspectUrl(url)
        val json = JSONObject(rawJson)
        ScanVerdict(
            isSafe = json.getBoolean("is_safe"),
            riskScore = json.getInt("risk_score"),
            reason = json.getString("reason")
        )
    }
}
