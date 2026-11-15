
package com.nevruz.aegis.core

enum class EventSeverity(val riskWeight: Int) {
    INFO(0),
    LOW(10),
    MEDIUM(25),
    HIGH(45),
    CRITICAL(70)
}

enum class SecurityPosture {
    HEALTHY,
    ATTENTION,
    THREAT_DETECTED
}

data class SecurityEvent(
    val id: Long = 0L,
    val occurredAt: Long,
    val module: String,
    val code: String,
    val severity: EventSeverity,
    val title: String,
    val description: String,
    val isResolved: Boolean = false
)

data class SecurityMonitorSnapshot(
    val riskScore: Int,
    val posture: SecurityPosture,
    val unresolvedCount: Int,
    val criticalCount: Int,
    val highCount: Int,
    val recentEvents: List<SecurityEvent>,
    val generatedAt: Long
)
