package com.example.archeryshotcounter.presentation

import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.foundation.pager.HorizontalPager
import androidx.wear.compose.foundation.pager.rememberPagerState
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.HorizontalPageIndicator
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import com.example.archeryshotcounter.presentation.theme.ArcheryShotCounterTheme

class MainActivity : ComponentActivity() {

    private lateinit var shotDetector: ShotDetector
    private lateinit var vibrator: Vibrator

    private var shotCount by mutableIntStateOf(0)
    private var isDetecting by mutableStateOf(false)
    private var sensitivity by mutableStateOf(Sensitivity.MEDIUM)
    private var customThreshold by mutableIntStateOf(15)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager

        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(VIBRATOR_SERVICE) as Vibrator
        }

        shotDetector = ShotDetector(sensorManager) {
            runOnUiThread {
                shotCount++
                vibrator.vibrate(
                    VibrationEffect.createOneShot(150, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            }
        }

        setContent {
            ArcheryShotCounterTheme {
                ArcheryApp(
                    shotCount = shotCount,
                    isDetecting = isDetecting,
                    sensitivity = sensitivity,
                    customThreshold = customThreshold,
                    onToggleDetection = ::toggleDetection,
                    onReset = { shotCount = 0 },
                    onManualAdjust = { delta ->
                        shotCount = (shotCount + delta).coerceAtLeast(0)
                    },
                    onSensitivityChange = { s ->
                        sensitivity = s
                        shotDetector.sensitivity = s
                    },
                    onCustomThresholdChange = { value ->
                        customThreshold = value
                        shotDetector.customThreshold = value.toFloat()
                    }
                )
            }
        }
    }

    private fun toggleDetection() {
        if (isDetecting) {
            isDetecting = false
            shotDetector.stop()
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            shotDetector.sensitivity = sensitivity
            shotDetector.start()
            isDetecting = true
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    override fun onPause() {
        super.onPause()
        if (isDetecting) shotDetector.stop()
    }

    override fun onResume() {
        super.onResume()
        if (isDetecting) shotDetector.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        shotDetector.stop()
    }
}

@Composable
fun ArcheryApp(
    shotCount: Int,
    isDetecting: Boolean,
    sensitivity: Sensitivity,
    customThreshold: Int,
    onToggleDetection: () -> Unit,
    onReset: () -> Unit,
    onManualAdjust: (Int) -> Unit,
    onSensitivityChange: (Sensitivity) -> Unit,
    onCustomThresholdChange: (Int) -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { 2 })
    AppScaffold {
        Box(modifier = Modifier.fillMaxSize()) {
            HorizontalPager(state = pagerState) { page ->
                when (page) {
                    0 -> MainScreen(
                        shotCount = shotCount,
                        isDetecting = isDetecting,
                        onToggleDetection = onToggleDetection,
                        onReset = onReset,
                        onManualAdjust = onManualAdjust
                    )
                    1 -> SettingsScreen(
                        sensitivity = sensitivity,
                        customThreshold = customThreshold,
                        onSensitivityChange = onSensitivityChange,
                        onCustomThresholdChange = onCustomThresholdChange
                    )
                }
            }
            HorizontalPageIndicator(
                pagerState = pagerState,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    }
}

@Composable
fun MainScreen(
    shotCount: Int,
    isDetecting: Boolean,
    onToggleDetection: () -> Unit,
    onReset: () -> Unit,
    onManualAdjust: (Int) -> Unit
) {
    val listState = rememberTransformingLazyColumnState()
    val transformationSpec = rememberTransformationSpec()
    val statusColor = if (isDetecting) Color(0xFF4CAF50) else Color(0xFF9E9E9E)

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
                        text = if (isDetecting) "ОБНАРУЖЕНИЕ" else "ПАУЗА",
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
                    Text(
                        text = "$shotCount",
                        fontSize = 72.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "выстрелов",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF9E9E9E)
                    )
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
                Button(
                    onClick = onToggleDetection,
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, transformationSpec),
                    transformation = SurfaceTransformation(transformationSpec),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDetecting)
                            MaterialTheme.colorScheme.error
                        else
                            MaterialTheme.colorScheme.primary,
                        contentColor = if (isDetecting)
                            MaterialTheme.colorScheme.onError
                        else
                            MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(
                        text = if (isDetecting) "СТОП" else "СТАРТ",
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            item {
                Button(
                    onClick = onReset,
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, transformationSpec),
                    transformation = SurfaceTransformation(transformationSpec),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                ) {
                    Text("СБРОС")
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

@Composable
fun SettingsScreen(
    sensitivity: Sensitivity,
    customThreshold: Int,
    onSensitivityChange: (Sensitivity) -> Unit,
    onCustomThresholdChange: (Int) -> Unit
) {
    val listState = rememberTransformingLazyColumnState()
    val transformationSpec = rememberTransformationSpec()

    ScreenScaffold(scrollState = listState) { contentPadding ->
        TransformingLazyColumn(
            contentPadding = contentPadding,
            state = listState
        ) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp, bottom = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Чувствительность",
                        style = MaterialTheme.typography.titleSmall,
                        color = Color(0xFFCCCCCC)
                    )
                }
            }

            Sensitivity.entries.forEach { s ->
                item {
                    Button(
                        onClick = { onSensitivityChange(s) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .transformedHeight(this, transformationSpec),
                        transformation = SurfaceTransformation(transformationSpec),
                        colors = if (sensitivity == s)
                            ButtonDefaults.buttonColors()
                        else
                            ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF3A3A3A),
                                contentColor = Color(0xFFAAAAAA)
                            )
                    ) {
                        Text(s.labelRu)
                    }
                }

                if (s == Sensitivity.CUSTOM && sensitivity == Sensitivity.CUSTOM) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = { if (customThreshold > 5) onCustomThresholdChange(customThreshold - 1) },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF3A3A3A),
                                    contentColor = Color(0xFFCCCCCC)
                                )
                            ) { Text("−", fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) }

                            Text(
                                text = "$customThreshold",
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Center,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            Button(
                                onClick = { if (customThreshold < 50) onCustomThresholdChange(customThreshold + 1) },
                                modifier = Modifier.weight(1f)
                            ) { Text("+", fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) }
                        }
                    }
                    item {
                        Text(
                            text = "м/с²  •  диапазон 5–50",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 4.dp),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF666666)
                        )
                    }
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
