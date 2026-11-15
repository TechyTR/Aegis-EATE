
package com.nevruz.aegis.core

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class SecurityEventStore(context: Context) {

    private val helper = EventDatabase(context.applicationContext)

    @Synchronized
    fun record(event: SecurityEvent): Long {
        require(event.module.isNotBlank())
        require(event.code.isNotBlank())
        require(event.title.isNotBlank())

        val db = helper.writableDatabase
        val values = ContentValues().apply {
            put("occurred_at", event.occurredAt)
            put("module", event.module.take(64))
            put("code", event.code.take(96))
            put("severity", event.severity.name)
            put("title", event.title.take(160))
            put("description", event.description.take(1000))
            put("is_resolved", if (event.isResolved) 1 else 0)
        }

        db.beginTransaction()
        try {
            val id = db.insertOrThrow(TABLE, null, values)

            val retentionCutoff =
                System.currentTimeMillis() - RETENTION_MS

            db.delete(
                TABLE,
                "occurred_at < ?",
                arrayOf(retentionCutoff.toString())
            )

            db.execSQL(
                """
                DELETE FROM $TABLE
                WHERE id NOT IN (
                    SELECT id FROM $TABLE
                    ORDER BY occurred_at DESC
                    LIMIT $MAX_EVENTS
                )
                """.trimIndent()
            )

            db.setTransactionSuccessful()
            return id
        } finally {
            db.endTransaction()
        }
    }

    @Synchronized
    fun getRecentEvents(
        limit: Int = 50,
        unresolvedOnly: Boolean = false
    ): List<SecurityEvent> {
        val safeLimit = limit.coerceIn(1, MAX_EVENTS)

        val selection =
            if (unresolvedOnly) "is_resolved = 0" else null

        val result = mutableListOf<SecurityEvent>()

        helper.readableDatabase.query(
            TABLE,
            null,
            selection,
            null,
            null,
            null,
            "occurred_at DESC",
            safeLimit.toString()
        ).use { cursor ->
            while (cursor.moveToNext()) {
                val severityName =
                    cursor.getString(
                        cursor.getColumnIndexOrThrow("severity")
                    )

                val severity = runCatching {
                    EventSeverity.valueOf(severityName)
                }.getOrDefault(EventSeverity.INFO)

                result += SecurityEvent(
                    id = cursor.getLong(
                        cursor.getColumnIndexOrThrow("id")
                    ),
                    occurredAt = cursor.getLong(
                        cursor.getColumnIndexOrThrow("occurred_at")
                    ),
                    module = cursor.getString(
                        cursor.getColumnIndexOrThrow("module")
                    ),
                    code = cursor.getString(
                        cursor.getColumnIndexOrThrow("code")
                    ),
                    severity = severity,
                    title = cursor.getString(
                        cursor.getColumnIndexOrThrow("title")
                    ),
                    description = cursor.getString(
                        cursor.getColumnIndexOrThrow("description")
                    ),
                    isResolved = cursor.getInt(
                        cursor.getColumnIndexOrThrow("is_resolved")
                    ) == 1
                )
            }
        }

        return result
    }

    @Synchronized
    fun resolve(eventId: Long): Boolean {
        if (eventId <= 0L) return false

        val values = ContentValues().apply {
            put("is_resolved", 1)
        }

        return helper.writableDatabase.update(
            TABLE,
            values,
            "id = ? AND is_resolved = 0",
            arrayOf(eventId.toString())
        ) > 0
    }

    @Synchronized
    fun close() {
        helper.close()
    }

    private class EventDatabase(context: Context) :
        SQLiteOpenHelper(
            context,
            DATABASE_NAME,
            null,
            DATABASE_VERSION
        ) {

        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE $TABLE (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    occurred_at INTEGER NOT NULL,
                    module TEXT NOT NULL,
                    code TEXT NOT NULL,
                    severity TEXT NOT NULL,
                    title TEXT NOT NULL,
                    description TEXT NOT NULL,
                    is_resolved INTEGER NOT NULL DEFAULT 0
                )
                """.trimIndent()
            )

            db.execSQL(
                """
                CREATE INDEX idx_events_time
                ON $TABLE(occurred_at DESC)
                """.trimIndent()
            )

            db.execSQL(
                """
                CREATE INDEX idx_events_unresolved
                ON $TABLE(is_resolved, severity)
                """.trimIndent()
            )
        }

        override fun onUpgrade(
            db: SQLiteDatabase,
            oldVersion: Int,
            newVersion: Int
        ) {
            // Future schema migrations will be added here.
        }
    }

    companion object {
        private const val DATABASE_NAME = "aegis_security_events.db"
        private const val DATABASE_VERSION = 1
        private const val TABLE = "security_events"

        private const val MAX_EVENTS = 500
        private const val RETENTION_MS =
            30L * 24L * 60L * 60L * 1000L
    }
}
