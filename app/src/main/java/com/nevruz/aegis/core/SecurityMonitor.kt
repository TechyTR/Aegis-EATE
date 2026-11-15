
package com.nevruz.aegis.core

import android.content.Context
import kotlin.math.roundToInt

class SecurityMonitor(context: Context) {

    private val eventStore = SecurityEventStore(context)

    fun reportEvent(
        module: String,
        code: String,
        severity: EventSeverity,
        title: String,
        description: String = ""
    ): Long {
        require(module.isNotBlank())
        require(code.isNotBlank())
        require(title.isNotBlank())

        return eventStore.record(
            SecurityEvent(
                occurredAt = System.currentTimeMillis(),
                module = module,
                code = code,
                severity = severity,
                title = title,
                description = description
            )
        )
    }

    fun resolveEvent(eventId: Long): Boolean {
        return eventStore.resolve(eventId)
    }

    fun getRecentEvents(limit: Int = 50): List<SecurityEvent> {
        return eventStore.getRecentEvents(limit)
    }

    fun getSnapshot(): SecurityMonitorSnapshot {
        val activeEvents = eventStore.getRecentEvents(
            limit = 500,
            unresolvedOnly = true
        )

        var remainingSafety = 1.0

        activeEvents.forEach { event ->
            val weight = event.severity.riskWeight
            remainingSafety *= 1.0 - (weight / 100.0)
        }

        val riskScore = (
            (1.0 - remainingSafety) * 100.0
        ).roundToInt().coerceIn(0, 100)

        val criticalCount = activeEvents.count {
            it.severity == EventSeverity.CRITICAL
        }

        val highCount = activeEvents.count {
            it.severity == EventSeverity.HIGH
        }

        val posture = when {
            criticalCount > 0 || highCount > 0 ->
                SecurityPosture.THREAT_DETECTED

            riskScore >= 15 ->
                SecurityPosture.ATTENTION

            else ->
                SecurityPosture.HEALTHY
        }

        return SecurityMonitorSnapshot(
            riskScore = riskScore,
            posture = posture,
            unresolvedCount = activeEvents.size,
            criticalCount = criticalCount,
            highCount = highCount,
            recentEvents = activeEvents.take(10),
            generatedAt = System.currentTimeMillis()
        )
    }

    fun close() {
        eventStore.close()
    }
}
