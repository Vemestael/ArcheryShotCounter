package com.vemestael.archeryshotcounter.presentation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DataExportTest {

    @Test
    fun `export then import round-trips sessions and shots`() {
        val session = Session(id = 100L, startTime = 100L, lastShotTime = 200L, shotCount = 2)
        val shots = listOf(
            Shot(sessionId = 100L, timestamp = 110L, magnitude = 24.97f),
            Shot(sessionId = 100L, timestamp = 150L, magnitude = null) // manual adjustment
        )

        val json = buildExportJson(listOf(session), shots)
        val imported = parseImportJson(json)

        assertEquals(1, imported.size)
        val (importedSession, importedShots) = imported[0]
        assertEquals(session, importedSession)
        assertEquals(2, importedShots.size)

        val scored = importedShots.first { it.timestamp == 110L }
        assertEquals(24.97f, scored.magnitude!!, 0.001f)

        val manual = importedShots.first { it.timestamp == 150L }
        assertNull(manual.magnitude)
    }

    @Test
    fun `export with no sessions produces an empty but valid import`() {
        val json = buildExportJson(emptyList(), emptyList())
        val imported = parseImportJson(json)
        assertTrue(imported.isEmpty())
    }

    @Test
    fun `shots are attributed to the correct session`() {
        val sessionA = Session(id = 1L, startTime = 1L, lastShotTime = 5L, shotCount = 1)
        val sessionB = Session(id = 2L, startTime = 2L, lastShotTime = 6L, shotCount = 1)
        val shots = listOf(
            Shot(sessionId = 1L, timestamp = 3L, magnitude = 10f),
            Shot(sessionId = 2L, timestamp = 4L, magnitude = 20f)
        )

        val imported = parseImportJson(buildExportJson(listOf(sessionA, sessionB), shots))

        val importedA = imported.first { it.session.id == 1L }
        val importedB = imported.first { it.session.id == 2L }
        assertEquals(1, importedA.shots.size)
        assertEquals(10f, importedA.shots[0].magnitude!!, 0.001f)
        assertEquals(1, importedB.shots.size)
        assertEquals(20f, importedB.shots[0].magnitude!!, 0.001f)
    }
}
