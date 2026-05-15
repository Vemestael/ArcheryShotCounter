package com.example.archeryshotcounter.presentation

import android.text.format.DateFormat as AndroidDateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import com.example.archeryshotcounter.R
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date


private sealed class HistoryListItem {
    data class SessionItem(
        val session: Session,
        val isActive: Boolean,
        val displayCount: Int
    ) : HistoryListItem()
    data class YearLabel(val year: Int) : HistoryListItem()
}

@Composable
fun HistoryScreen(
    sessions: List<Session>,
    currentSession: Session?,
    activeShotCount: Int,
    onEdit: (Session) -> Unit,
    onDelete: (Session) -> Unit
) {
    val context = LocalContext.current
    val listState = rememberTransformingLazyColumnState()
    val transformationSpec = rememberTransformationSpec()
    val editingSession = remember { mutableStateOf<Session?>(null) }

    val historyItems = buildHistoryItems(sessions, currentSession, activeShotCount)
    val timeFormat = remember(context) { AndroidDateFormat.getTimeFormat(context) }
    val locale = LocalConfiguration.current.locales[0]
    val dateFormat = remember(locale) { SimpleDateFormat("d MMM", locale) }

    ScreenScaffold(scrollState = listState) { contentPadding ->
        TransformingLazyColumn(
            contentPadding = contentPadding,
            state = listState
        ) {
            if (historyItems.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.history_empty),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF666666),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                historyItems.forEach { histItem ->
                    when (histItem) {
                        is HistoryListItem.YearLabel -> item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = histItem.year.toString(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF666666)
                                )
                            }
                        }
                        is HistoryListItem.SessionItem -> item {
                            val dotColor = if (histItem.isActive) Color(0xFF4CAF50) else Color(0xFF666666)
                            val dateText = dateFormat.format(Date(histItem.session.startTime))
                            val startTimeText = timeFormat.format(Date(histItem.session.startTime))
                            val endTimeText = timeFormat.format(Date(histItem.session.lastShotTime))
                            val durationMin = (histItem.session.lastShotTime - histItem.session.startTime) / 60000
                            val durationText = when {
                                durationMin < 1 -> "<1m"
                                durationMin < 60 -> "${durationMin}m"
                                else -> {
                                    val h = durationMin / 60
                                    val m = durationMin % 60
                                    if (m == 0L) "${h}h" else "${h}h ${m}m"
                                }
                            }
                            val shotsLabel = stringResource(R.string.shots_label)

                            Button(
                                onClick = { editingSession.value = histItem.session },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .transformedHeight(this, transformationSpec),
                                transformation = SurfaceTransformation(transformationSpec),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF2A2A2A),
                                    contentColor = Color(0xFFDDDDDD)
                                )
                            ) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .background(color = dotColor, shape = CircleShape)
                                        )
                                        Text(
                                            text = dateText,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFF9E9E9E)
                                        )
                                    }
                                    Text(
                                        text = "$startTimeText – $endTimeText",
                                        fontSize = 12.sp,
                                        color = Color(0xFFCCCCCC)
                                    )
                                    Text(
                                        text = "${histItem.displayCount} $shotsLabel • $durationText",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.fillMaxWidth().height(16.dp))
            }
        }
    }

    editingSession.value?.let { session ->
        EditSessionDialog(
            session = session,
            onSave = { updated ->
                onEdit(updated)
                editingSession.value = null
            },
            onDelete = {
                onDelete(session)
                editingSession.value = null
            },
            onDismiss = { editingSession.value = null }
        )
    }
}

private fun buildHistoryItems(
    sessions: List<Session>,
    currentSession: Session?,
    activeShotCount: Int
): List<HistoryListItem> {
    val allSessions = if (currentSession != null && sessions.none { it.id == currentSession.id }) {
        listOf(currentSession.copy(shotCount = activeShotCount)) + sessions
    } else {
        sessions
    }

    val sorted = allSessions.sortedByDescending { it.startTime }
    val currentYear = Calendar.getInstance().get(Calendar.YEAR)
    val result = mutableListOf<HistoryListItem>()
    var lastYear: Int? = null

    sorted.forEach { session ->
        val sessionYear = Calendar.getInstance().apply { timeInMillis = session.startTime }.get(Calendar.YEAR)
        if (sessionYear != currentYear && sessionYear != lastYear) {
            result.add(HistoryListItem.YearLabel(sessionYear))
            lastYear = sessionYear
        }
        val isActive = currentSession?.id == session.id
        val displayCount = if (isActive) activeShotCount else session.shotCount
        result.add(HistoryListItem.SessionItem(session, isActive, displayCount))
    }

    return result
}

@Composable
private fun EditSessionDialog(
    session: Session,
    onSave: (Session) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    var count by remember { mutableIntStateOf(session.shotCount) }
    val locale = LocalConfiguration.current.locales[0]
    val dateFormat = remember(locale) { SimpleDateFormat("d MMM", locale) }
    val dateText = remember(session, locale) { dateFormat.format(Date(session.startTime)) }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF1A1A1A))
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = dateText,
                style = MaterialTheme.typography.titleSmall,
                color = Color(0xFFCCCCCC),
                textAlign = TextAlign.Center
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { if (count > 0) count-- },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF3A3A3A),
                        contentColor = Color(0xFFCCCCCC)
                    )
                ) {
                    Text("−", fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                }
                Text(
                    text = "$count",
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Button(
                    onClick = { count++ },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("+", fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                }
            }
            Button(
                onClick = { onSave(session.copy(shotCount = count)) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.dialog_save), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            }
            Button(
                onClick = onDelete,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                )
            ) {
                Text(stringResource(R.string.dialog_delete), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}
