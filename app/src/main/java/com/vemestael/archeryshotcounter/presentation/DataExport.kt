package com.vemestael.archeryshotcounter.presentation

import org.json.JSONArray
import org.json.JSONObject

fun sessionJson(session: Session, shots: List<Shot>): JSONObject {
    val shotsArray = JSONArray()
    shots.sortedBy { it.timestamp }.forEach { shot ->
        shotsArray.put(
            JSONObject().apply {
                put("timestamp", shot.timestamp)
                put("magnitude", shot.magnitude?.toDouble() ?: JSONObject.NULL)
            }
        )
    }
    return JSONObject().apply {
        put("id", session.id)
        put("startTime", session.startTime)
        put("lastShotTime", session.lastShotTime)
        put("shotCount", session.shotCount)
        put("shotsPerEndAtStart", session.shotsPerEndAtStart)
        put("lastModified", session.lastModified)
        put("deletedAt", session.deletedAt ?: JSONObject.NULL)
        put("shots", shotsArray)
    }
}

/** Single-session payload shape carried by each Data Layer DataItem, in both sync directions. */
fun buildSessionJson(session: Session, shots: List<Shot>): String = sessionJson(session, shots).toString()

fun parseSessionJson(json: String): Pair<Session, List<Shot>> {
    val obj = JSONObject(json)
    val session = Session(
        id = obj.getLong("id"),
        startTime = obj.getLong("startTime"),
        lastShotTime = obj.getLong("lastShotTime"),
        shotCount = obj.getInt("shotCount"),
        shotsPerEndAtStart = obj.optInt("shotsPerEndAtStart", 0),
        lastModified = obj.optLong("lastModified", 0L),
        deletedAt = if (obj.isNull("deletedAt")) null else obj.optLong("deletedAt")
    )
    val shotsArray = obj.getJSONArray("shots")
    val shots = List(shotsArray.length()) { j ->
        val shotObj = shotsArray.getJSONObject(j)
        Shot(
            sessionId = session.id,
            timestamp = shotObj.getLong("timestamp"),
            magnitude = if (shotObj.isNull("magnitude")) null else shotObj.getDouble("magnitude").toFloat()
        )
    }
    return session to shots
}
