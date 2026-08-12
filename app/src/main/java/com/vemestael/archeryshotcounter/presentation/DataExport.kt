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
                put("shots", shotsArray)
            }
        )
    }
    return JSONObject().apply {
        put("exportedAt", System.currentTimeMillis())
        put("sessions", sessionsArray)
    }.toString(2)
}
