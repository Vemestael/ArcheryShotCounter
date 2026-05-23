package com.vemestael.archeryshotcounter.presentation

import android.content.Context
import androidx.room.Entity
import androidx.room.PrimaryKey
import org.json.JSONArray
import java.io.File

@Entity(tableName = "sessions")
data class Session(
    @PrimaryKey val id: Long,
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
}
