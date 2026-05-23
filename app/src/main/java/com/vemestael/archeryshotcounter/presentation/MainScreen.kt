package com.vemestael.archeryshotcounter.presentation

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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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

@Composable
fun MainScreen(
    shotCount: Int,
    isDetecting: Boolean,
    currentSession: Session?,
    shotsPerEnd: Int,
    autoPauseEnabled: Boolean,
    autoPauseSecondsLeft: Int,
    lastShotMagnitude: Float?,
    onPrimaryButton: () -> Unit,
    onSecondaryButton: () -> Unit,
    onEnd: () -> Unit,
    onManualAdjust: (Int) -> Unit
) {
    val listState = rememberTransformingLazyColumnState()
    val transformationSpec = rememberTransformationSpec()

    val sessionExists = currentSession != null
    val pausedLabel = stringResource(R.string.status_paused)
    val unitM = stringResource(R.string.time_m)
    val unitS = stringResource(R.string.time_s)
    val statusText = when {
        !sessionExists -> stringResource(R.string.status_ready)
        autoPauseSecondsLeft >= 0 -> {
            val m = autoPauseSecondsLeft / 60
            val s = autoPauseSecondsLeft % 60
            val timeStr = if (m > 0) "${m}$unitM ${s.toString().padStart(2, '0')}$unitS" else "${s}$unitS"
            "$pausedLabel · $timeStr"
        }
        isDetecting -> stringResource(R.string.status_detecting)
        else -> pausedLabel
    }
    val statusColor = when {
        !sessionExists -> Color(0xFF9E9E9E)
        isDetecting -> Color(0xFF4CAF50)
        else -> Color(0xFFFFC107)
    }

    ScreenScaffold(scrollState = listState) { contentPadding ->
        TransformingLazyColumn(
            contentPadding = contentPadding,
            state = listState
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(color = statusColor, shape = CircleShape)
                    )
                    Spacer(modifier = Modifier.size(6.dp))
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelSmall,
                        color = statusColor
                    )
                }
            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (shotsPerEnd > 0 && currentSession != null) {
                        val seriesIndex = shotCount / shotsPerEnd
                        val prevBoundary = seriesIndex * shotsPerEnd
                        val nextBoundary = prevBoundary + shotsPerEnd
                        val effectivePrev = if (shotCount == prevBoundary && prevBoundary > 0) prevBoundary - shotsPerEnd else prevBoundary
                        val leftDelta = shotCount - effectivePrev
                        val rightDelta = nextBoundary - shotCount
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (leftDelta > 0) "−$leftDelta" else "",
                                fontSize = 13.sp,
                                color = Color(0xFF666666),
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Start
                            )
                            Text(
                                text = "$shotCount",
                                fontSize = 72.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "+$rightDelta",
                                fontSize = 13.sp,
                                color = Color(0xFF666666),
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.End
                            )
                        }
                    } else {
                        Text(
                            text = "$shotCount",
                            fontSize = 72.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        text = stringResource(R.string.shots_label),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF9E9E9E)
                    )
                    if (lastShotMagnitude != null) {
                        Text(
                            text = "↑ ${"%.1f".format(lastShotMagnitude)} ${stringResource(R.string.unit_accel)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF666666)
                        )
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { onManualAdjust(-1) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF3A3A3A),
                            contentColor = Color(0xFFCCCCCC)
                        )
                    ) {
                        Text("−1", fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                    }
                    Button(
                        onClick = { onManualAdjust(1) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("+1", fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                    }
                }
            }

            item {
                if (!sessionExists || !autoPauseEnabled) {
                    val label = when {
                        !sessionExists -> stringResource(R.string.btn_start)
                        isDetecting -> stringResource(R.string.btn_stop)
                        else -> stringResource(R.string.btn_resume)
                    }
                    val colors = if (isDetecting && sessionExists)
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        )
                    else
                        ButtonDefaults.buttonColors()
                    Button(
                        onClick = onPrimaryButton,
                        modifier = Modifier
                            .fillMaxWidth()
                            .transformedHeight(this, transformationSpec),
                        transformation = SurfaceTransformation(transformationSpec),
                        colors = colors
                    ) {
                        Text(
                            text = label,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val leftLabel = if (isDetecting)
                            stringResource(R.string.btn_stop)
                        else
                            stringResource(R.string.btn_resume)
                        val leftColors = if (isDetecting)
                            ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError
                            )
                        else
                            ButtonDefaults.buttonColors()
                        Button(
                            onClick = onPrimaryButton,
                            modifier = Modifier.weight(2f),
                            colors = leftColors
                        ) {
                            Text(
                                text = leftLabel,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        val rightLabel = if (autoPauseSecondsLeft >= 0)
                            "+5$unitS"
                        else
                            stringResource(R.string.btn_auto_pause)
                        Button(
                            onClick = onSecondaryButton,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF3A3A3A),
                                contentColor = Color(0xFFCCCCCC)
                            )
                        ) {
                            Text(
                                text = rightLabel,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            item {
                Button(
                    onClick = onEnd,
                    enabled = sessionExists,
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, transformationSpec),
                    transformation = SurfaceTransformation(transformationSpec),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        disabledContainerColor = Color(0xFF2A2A2A),
                        disabledContentColor = Color(0xFF555555)
                    )
                ) {
                    Text(stringResource(R.string.btn_reset), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                }
            }

            item {
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(16.dp)
                )
            }
        }
    }
}

fun formatAutoPauseDuration(seconds: Int, unitM: String, unitS: String): String {
    val m = seconds / 60
    val s = seconds % 60
    return when {
        m == 0 -> "${s}$unitS"
        s == 0 -> "${m}$unitM"
        else -> "${m}$unitM ${s}$unitS"
    }
}
