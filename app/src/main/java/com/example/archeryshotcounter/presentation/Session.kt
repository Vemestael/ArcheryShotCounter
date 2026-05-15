package com.example.archeryshotcounter.presentation

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class Session(
    val id: Long,
    val startTime: Long,
    val lastShotTime: Long,
    val shotCount: Int
)

class SessionStorage(context: Context) {
    private val file = File(context.filesDir, "sessions.json")

    fun load(): List<Session> {
        if (!file.exists()) return emptyList()
        return try {
            val arr = JSONArray(file.readText())
            List(arr.length()) { i ->
                arr.getJSONObject(i).run {
                    Session(getLong("id"), getLong("startTime"), getLong("lastShotTime"), getInt("shotCount"))
                }
            }.sortedByDescending { it.startTime }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun save(sessions: List<Session>) {
        val arr = JSONArray()
        sessions.forEach { s ->
            arr.put(JSONObject().apply {
                put("id", s.id)
                put("startTime", s.startTime)
                put("lastShotTime", s.lastShotTime)
                put("shotCount", s.shotCount)
            })
        }
        file.writeText(arr.toString())
    }
}
