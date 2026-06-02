package com.vemestael.archeryshotcounter.presentation

import android.text.format.DateFormat as AndroidDateFormat
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.vemestael.archeryshotcounter.R
import java.text.SimpleDateFormat
import java.util.Date

@Composable
fun ShotDetailScreen(
    session: Session,
    shots: List<Shot>,
    onDismiss: () -> Unit
) {
    BackHandler(onBack = onDismiss)

    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    val timeFormat = remember(context) { AndroidDateFormat.getTimeFormat(context) }
    val dateFormat = remember(locale) { SimpleDateFormat("d MMM", locale) }
    val unitAccel = stringResource(R.string.unit_accel)
    val totalShots = shots.size

    val listState = rememberTransformingLazyColumnState()
    val transformationSpec = rememberTransformationSpec()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        ScreenScaffold(scrollState = listState) { contentPadding ->
            TransformingLazyColumn(
                contentPadding = contentPadding,
                state = listState
            ) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp, bottom = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = dateFormat.format(Date(session.startTime)),
                            style = MaterialTheme.typography.titleSmall,
                            color = Color(0xFFCCCCCC)
                        )
                    }
                }

                if (shots.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.shots_empty),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF666666),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    // shots are already sorted DESC — index 0 = newest = highest number
                    shots.forEachIndexed { index, shot ->
                        val shotNumber = totalShots - index
                        item {
                            Button(
                                onClick = {},
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
                                    Text(
                                        text = "#$shotNumber",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF9E9E9E)
                                    )
                                    Text(
                                        text = timeFormat.format(Date(shot.timestamp)),
                                        fontSize = 12.sp,
                                        color = Color(0xFFCCCCCC)
                                    )
                                    Text(
                                        text = if (shot.magnitude != null) "↑ ${"%.1f".format(shot.magnitude)} $unitAccel" else "—",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (shot.magnitude != null) MaterialTheme.colorScheme.primary else Color(0xFF666666)
                                    )
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
    }
}
