package com.aegisguard.core.analyzer

import com.aegisguard.core.spoofing.DeviceProfileGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

data class AnalysisResult(
    val url: String,
    val isSafe: Boolean,
    val riskScore: Int,
    val reason: String
)

class ThreatAnalyzerClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(3, TimeUnit.SECONDS)
        .readTimeout(3, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private val cache = mutableMapOf<String, AnalysisResult>()

    suspend fun inspectDomain(targetUrl: String, enableSpoofing: Boolean = true): AnalysisResult = withContext(Dispatchers.IO) {
        val normalizedUrl = if (targetUrl.startsWith("http")) targetUrl else "https://$targetUrl"
        cache[normalizedUrl]?.let { return@withContext it }

        var riskScore = 0
        val reasons = mutableListOf<String>()

        try {
            val requestBuilder = Request.Builder().url(normalizedUrl)

            if (enableSpoofing) {
                val spoofed = DeviceProfileGenerator.getRandomProfile()
                requestBuilder
                    .header("User-Agent", spoofed.userAgent)
                    .header("Sec-CH-UA-Platform", spoofed.secChUaPlatform)
                    .header("Sec-CH-UA-Mobile", spoofed.secChUaMobile)
                    .header("Accept-Language", spoofed.acceptLanguage)
                    .header("DNT", "1")
            }

            client.newCall(requestBuilder.build()).execute().use { response ->
                val finalUrl = response.request.url.toString()
                val bodySnippet = response.body?.string()?.take(15000) ?: ""

                if (!finalUrl.contains(extractHost(normalizedUrl))) {
                    riskScore += 35
                    reasons.add("Farklı alan adına yönlendirme ($finalUrl)")
                }

                val suspiciousKeywords = listOf("password", "cvv", "iban", "tc kimlik", "card_number", "eval(atob(")
                val matched = suspiciousKeywords.filter { bodySnippet.contains(it, ignoreCase = true) }
                if (matched.size >= 2) {
                    riskScore += 45
                    reasons.add("Hassas veri toplama/gizlenmiş kod kalıpları")
                }

                if (finalUrl.startsWith("http://")) {
                    riskScore += 20
                    reasons.add("Şifrelenmemiş HTTP bağlantısı")
                }
            }
        } catch (e: Exception) {
            reasons.add("Analiz zaman aşımı veya erişim hatası")
        }

        val result = AnalysisResult(
            url = normalizedUrl,
            isSafe = riskScore < 50,
            riskScore = riskScore,
            reason = if (reasons.isEmpty()) "Temiz" else reasons.joinToString(" | ")
        )

        cache[normalizedUrl] = result
        return@withContext result
    }

    private fun extractHost(url: String): String =
        url.removePrefix("https://").removePrefix("http://").substringBefore("/")
}
