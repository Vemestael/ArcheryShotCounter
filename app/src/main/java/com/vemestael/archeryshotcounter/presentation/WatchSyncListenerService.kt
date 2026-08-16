package com.vemestael.archeryshotcounter.presentation

import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.WearableListenerService
import java.util.concurrent.Executors

/** Background delivery path for phone-originated session edits/deletes. Play Services binds
 * this even when the app isn't running. */
class WatchSyncListenerService : WearableListenerService() {

    private val executor = Executors.newSingleThreadExecutor()

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        val jsonPayloads = dataEvents.mapNotNull { event ->
            if (event.type != DataEvent.TYPE_CHANGED) return@mapNotNull null
            if (!event.dataItem.uri.path.orEmpty().startsWith("/session")) return@mapNotNull null
            DataMapItem.fromDataItem(event.dataItem).dataMap.getString("json")
        }
        dataEvents.release()
        if (jsonPayloads.isEmpty()) return

        val db = AppDatabase.getInstance(applicationContext)
        executor.execute {
            jsonPayloads.forEach { json ->
                try {
                    val (session, shots) = parseSessionJson(json)
                    db.mergeIncomingSession(session, shots)
                } catch (_: Exception) {
                    // skip malformed item, keep processing the rest
                }
            }
        }
    }
}
