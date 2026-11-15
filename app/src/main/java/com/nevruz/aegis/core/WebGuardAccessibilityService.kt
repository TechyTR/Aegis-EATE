package com.aegisguard.core.service

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import com.aegisguard.core.analyzer.ThreatAnalyzerClient
import kotlinx.coroutines.*

class WebGuardAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val analyzer = ThreatAnalyzerClient()
    private var lastCheckedUrl: String = ""

    // Popüler tarayıcıların adres çubuğu View ID'leri
    private val browserUrlBarIds = listOf(
        "com.android.chrome:id/url_bar",
        "com.brave.browser:id/url_bar",
        "com.sec.android.app.sbrowser:id/location_bar_edit_text",
        "org.mozilla.firefox:id/mozac_browser_toolbar_url_view"
    )

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val rootNode = rootInActiveWindow ?: return
        val detectedUrl = extractUrlFromBrowser(rootNode) ?: return

        if (detectedUrl != lastCheckedUrl && detectedUrl.contains(".")) {
            lastCheckedUrl = detectedUrl

            serviceScope.launch {
                val result = analyzer.inspectDomain(detectedUrl, enableSpoofing = true)
                if (!result.isSafe) {
                    // Zararlı site tespit edildi! VPN kullanmadan anında geri çık ve uyar
                    performGlobalAction(GLOBAL_ACTION_BACK)
                    Toast.makeText(
                        applicationContext,
                        "AegisGuard Engelledi (${result.riskScore} Puan): ${result.reason}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun extractUrlFromBrowser(rootNode: AccessibilityNodeInfo): String? {
        for (viewId in browserUrlBarIds) {
            val nodes = rootNode.findAccessibilityNodeInfosByViewId(viewId)
            if (!nodes.isNullOrEmpty()) {
                val text = nodes[0].text?.toString()?.trim()
                if (!text.isNullOrEmpty() && !text.contains(" ")) {
                    return text
                }
            }
        }
        return null
    }

    override fun onInterrupt() {
        serviceScope.cancel()
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }
}
