package com.aegisguard.core.spoofing

data class SpoofedProfile(
    val userAgent: String,
    val secChUaPlatform: String,
    val secChUaMobile: String,
    val acceptLanguage: String
)

object DeviceProfileGenerator {

    private val profiles = listOf(
        SpoofedProfile(
            userAgent = "Mozilla/5.0 (Linux; Android 14; Pixel 8 Pro) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36",
            secChUaPlatform = "\"Android\"",
            secChUaMobile = "?1",
            acceptLanguage = "en-US,en;q=0.9"
        ),
        SpoofedProfile(
            userAgent = "Mozilla/5.0 (Linux; Android 13; SM-S918B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.0.0 Mobile Safari/537.36",
            secChUaPlatform = "\"Android\"",
            secChUaMobile = "?1",
            acceptLanguage = "tr-TR,tr;q=0.9,en-US;q=0.8"
        ),
        SpoofedProfile(
            userAgent = "Mozilla/5.0 (iPhone; CPU iPhone OS 17_4 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.4 Mobile/15E148 Safari/604.1",
            secChUaPlatform = "\"iOS\"",
            secChUaMobile = "?1",
            acceptLanguage = "en-GB,en;q=0.9"
        )
    )

    fun getRandomProfile(): SpoofedProfile = profiles.random()
}
