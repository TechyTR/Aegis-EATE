package com.nevruz.aegis.core.service

import android.accessibilityservice.AccessibilityService
import android.util.LruCache
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import com.nevruz.aegis.core.bridge.AegisNativeBridge
import com.nevruz.aegis.core.bridge.ScanVerdict
import kotlinx.coroutines.*

class WebGuardAccessibilityService : AccessibilityService() {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var lastUrl = ""

    // Aynı siteye tekrar tekrar istek atıp pili/CPU'yu yormamak için bellek içi LRU önbellek
    private val verdictCache = LruCache<String, ScanVerdict>(250)

    private val urlBarIds = listOf(
        "com.android.chrome:id/url_bar",
        "com.brave.browser:id/url_bar",
        "com.sec.android.app.sbrowser:id/location_bar_edit_text",
        "org.mozilla.firefox:id/mozac_browser_toolbar_url_view"
    )

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val root = rootInActiveWindow ?: return
        val url = findUrl(root) ?: return

        if (url != lastUrl && url.contains(".")) {
            lastUrl = url
            scope.launch {
                val verdict = verdictCache.get(url) ?: AegisNativeBridge.inspectUrlFast(url).also {
                    verdictCache.put(url, it)
                }

                if (!verdict.isSafe) {
                    performGlobalAction(GLOBAL_ACTION_BACK)
                    Toast.makeText(
                        applicationContext,
                        "AegisGuard Engelledi (${verdict.riskScore}): ${verdict.reason}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun findUrl(root: AccessibilityNodeInfo): String? {
        for (id in urlBarIds) {
            val nodes = root.findAccessibilityNodeInfosByViewId(id)
            if (!nodes.isNullOrEmpty()) {
                val text = nodes[0].text?.toString()?.trim()
                if (!text.isNullOrEmpty() && !text.contains(" ")) return text
            }
        }
        return null
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
