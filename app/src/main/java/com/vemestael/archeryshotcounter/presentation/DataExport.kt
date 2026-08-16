package com.vemestael.archeryshotcounter.presentation

import org.json.JSONArray
import org.json.JSONObject

fun buildExportJson(sessions: List<Session>, shots: List<Shot>): String {
    val shotsBySession = shots.groupBy { it.sessionId }
    val sessionsArray = JSONArray()
    sessions.sortedByDescending { it.startTime }.forEach { session ->
        val shotsArray = JSONArray()
        shotsBySession[session.id].orEmpty().sortedBy { it.timestamp }.forEach { shot ->
            shotsArray.put(
                JSONObject().apply {
                    put("timestamp", shot.timestamp)
                    put("magnitude", shot.magnitude?.toDouble() ?: JSONObject.NULL)
                }
            )
        }
        sessionsArray.put(
            JSONObject().apply {
                put("id", session.id)
                put("startTime", session.startTime)
                put("lastShotTime", session.lastShotTime)
                put("shotCount", session.shotCount)
                put("shotsPerEndAtStart", session.shotsPerEndAtStart)
                put("shots", shotsArray)
            }
        )
    }
    return JSONObject().apply {
        put("exportedAt", System.currentTimeMillis())
        put("sessions", sessionsArray)
    }.toString(2)
}

data class ImportedSession(val session: Session, val shots: List<Shot>)

fun parseImportJson(json: String): List<ImportedSession> {
    val sessionsArray = JSONObject(json).getJSONArray("sessions")
    return List(sessionsArray.length()) { i ->
        val sessionObj = sessionsArray.getJSONObject(i)
        val session = Session(
            id = sessionObj.getLong("id"),
            startTime = sessionObj.getLong("startTime"),
            lastShotTime = sessionObj.getLong("lastShotTime"),
            shotCount = sessionObj.getInt("shotCount"),
            shotsPerEndAtStart = sessionObj.optInt("shotsPerEndAtStart", 0)
        )
        val shotsArray = sessionObj.getJSONArray("shots")
        val shots = List(shotsArray.length()) { j ->
            val shotObj = shotsArray.getJSONObject(j)
            Shot(
                sessionId = session.id,
                timestamp = shotObj.getLong("timestamp"),
                magnitude = if (shotObj.isNull("magnitude")) null else shotObj.getDouble("magnitude").toFloat()
            )
        }
        ImportedSession(session, shots)
    }
}
