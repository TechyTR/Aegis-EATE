package com.nevruz.aegis.core.service

import android.accessibilityservice.AccessibilityService
import android.util.LruCache
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import com.nevruz.aegis.core.bridge.AegisNativeBridge
import com.nevruz.aegis.core.bridge.ScanVerdict
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/** Optional browser URL observer. Heuristic matches warn only; they never close pages. */
class WebGuardAccessibilityService : AccessibilityService() {
    private val scope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())
    private var lastUrl = ""
    private val verdictCache = LruCache<String, ScanVerdict>(96)

    private val urlBarIds = listOf(
        "com.android.chrome:id/url_bar",
        "com.brave.browser:id/url_bar",
        "com.sec.android.app.sbrowser:id/location_bar_edit_text",
        "org.mozilla.firefox:id/mozac_browser_toolbar_url_view"
    )

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val root = rootInActiveWindow ?: return
        val url = try { findUrl(root) } finally { root.recycle() } ?: return
        if (url == lastUrl || !url.contains('.') || url.length > 2048) return
        lastUrl = url

        scope.launch {
            val verdict = verdictCache.get(url) ?: AegisNativeBridge.inspectUrlFast(url).also {
                verdictCache.put(url, it)
            }
            if (!verdict.isSafe) {
                Toast.makeText(
                    applicationContext,
                    "Aegis uyarısı (${verdict.riskScore}/100): ${verdict.reason}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun findUrl(root: AccessibilityNodeInfo): String? {
        for (id in urlBarIds) {
            val nodes = root.findAccessibilityNodeInfosByViewId(id)
            if (!nodes.isNullOrEmpty()) {
                val text = nodes[0].text?.toString()?.trim()
                nodes.forEach { it.recycle() }
                if (!text.isNullOrEmpty() && !text.contains(' ')) return text
            }
        }
        return null
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
