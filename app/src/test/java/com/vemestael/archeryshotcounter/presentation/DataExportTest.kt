package com.vemestael.archeryshotcounter.presentation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DataExportTest {

    @Test
    fun `session json round-trips through buildSessionJson and parseSessionJson`() {
        val session = Session(
            id = 100L,
            startTime = 100L,
            lastShotTime = 200L,
            shotCount = 2,
            shotsPerEndAtStart = 3,
            lastModified = 12345L
        )
        val shots = listOf(
            Shot(sessionId = 100L, timestamp = 110L, magnitude = 24.97f),
            Shot(sessionId = 100L, timestamp = 150L, magnitude = null) // manual adjustment
        )

        val (importedSession, importedShots) = parseSessionJson(buildSessionJson(session, shots))

        assertEquals(session, importedSession)
        assertEquals(2, importedShots.size)

        val scored = importedShots.first { it.timestamp == 110L }
        assertEquals(24.97f, scored.magnitude!!, 0.001f)

        val manual = importedShots.first { it.timestamp == 150L }
        assertNull(manual.magnitude)
    }

    @Test
    fun `deletedAt round-trips for tombstones and stays null otherwise`() {
        val tombstone = Session(id = 1L, startTime = 1L, lastShotTime = 1L, shotCount = 0, lastModified = 5L, deletedAt = 999L)
        val (importedTombstone, _) = parseSessionJson(buildSessionJson(tombstone, emptyList()))
        assertEquals(999L, importedTombstone.deletedAt)

        val live = Session(id = 2L, startTime = 2L, lastShotTime = 2L, shotCount = 0, lastModified = 5L)
        val (importedLive, _) = parseSessionJson(buildSessionJson(live, emptyList()))
        assertNull(importedLive.deletedAt)
    }

    @Test
    fun `lastModified and shotsPerEndAtStart default to 0 for payloads missing those fields`() {
        val legacyJson = """{"id":6,"startTime":6,"lastShotTime":6,"shotCount":0,"shots":[]}"""
        val (session, _) = parseSessionJson(legacyJson)
        assertEquals(0, session.shotsPerEndAtStart)
        assertEquals(0L, session.lastModified)
        assertNull(session.deletedAt)
    }
}
