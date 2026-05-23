package com.example.archeryshotcounter.presentation

import android.content.Context
import android.content.res.Configuration
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.mutableStateListOf
import androidx.core.content.edit
import java.util.Locale
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import com.example.archeryshotcounter.R
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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

private const val PREFS_NAME = "settings"
private const val KEY_LANGUAGE = "language"
private const val KEY_SENSITIVITY = "sensitivity"
private const val KEY_CUSTOM_THRESHOLD = "custom_threshold"
private const val KEY_PENDING_ID = "pending_id"
private const val KEY_PENDING_START = "pending_start"
private const val KEY_PENDING_LAST = "pending_last"
private const val KEY_PENDING_COUNT = "pending_count"
private const val KEY_SHOTS_PER_END = "shots_per_end"
private const val KEY_AUTO_PAUSE_ENABLED = "auto_pause_enabled"
private const val KEY_AUTO_PAUSE_DURATION = "auto_pause_duration"

enum class AppLanguage(val code: String, val nativeName: String, val englishName: String) {
    SYSTEM("system", "", "System"),
    ENGLISH("en", "English", "English"),
    RUSSIAN("ru", "Русский", "Russian"),
    SPANISH("es", "Español", "Spanish"),
    FRENCH("fr", "Français", "French"),
    GERMAN("de", "Deutsch", "German"),
    PORTUGUESE("pt", "Português", "Portuguese"),
    CHINESE("zh", "中文", "Chinese"),
    JAPANESE("ja", "日本語", "Japanese"),
    KOREAN("ko", "한국어", "Korean"),
    ARABIC("ar", "العربية", "Arabic"),
    TURKISH("tr", "Türkçe", "Turkish"),
    HINDI("hi", "हिन्दी", "Hindi")
}

class MainActivity : ComponentActivity() {

    private lateinit var shotDetector: ShotDetector
    private lateinit var vibrator: Vibrator
    private lateinit var sessionStorage: SessionStorage

    private var shotCount by mutableIntStateOf(0)
    private var isDetecting by mutableStateOf(false)
    private var currentSession by mutableStateOf<Session?>(null)
    private val sessions = mutableStateListOf<Session>()

    private var sensitivity by mutableStateOf(Sensitivity.MEDIUM)
    private var customThreshold by mutableIntStateOf(15)
    private var currentLanguage by mutableStateOf(AppLanguage.SYSTEM)

    private var shotsPerEnd by mutableIntStateOf(0)
    private var autoPauseEnabled by mutableStateOf(false)
    private var autoPauseDuration by mutableIntStateOf(60)
    private var autoPauseSecondsLeft by mutableIntStateOf(-1)
    private var autoPauseTimer: CountDownTimer? = null

    private var lastShotMagnitude by mutableStateOf<Float?>(null)
    private val magnitudeHandler = Handler(Looper.getMainLooper())
    private val magnitudeHideRunnable = Runnable { lastShotMagnitude = null }

    override fun attachBaseContext(newBase: Context) {
        val code = newBase.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            .getString(KEY_LANGUAGE, AppLanguage.SYSTEM.code) ?: AppLanguage.SYSTEM.code
        currentLanguage = AppLanguage.entries.find { it.code == code } ?: AppLanguage.SYSTEM
        if (code == AppLanguage.SYSTEM.code) {
            super.attachBaseContext(newBase)
        } else {
            val config = Configuration(newBase.resources.configuration)
            config.setLocale(Locale.forLanguageTag(code))
            super.attachBaseContext(newBase.createConfigurationContext(config))
        }
    }

    private fun changeLanguage(lang: AppLanguage) {
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            .edit { putString(KEY_LANGUAGE, lang.code) }
        recreate()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.attributes = window.attributes.also { it.preferredRefreshRate = 60f }

        val sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        sessionStorage = SessionStorage(this)

        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(VIBRATOR_SERVICE) as Vibrator
        }

        shotDetector = ShotDetector(sensorManager) { magnitude ->
            runOnUiThread {
                shotCount++
                lastShotMagnitude = magnitude
                magnitudeHandler.removeCallbacks(magnitudeHideRunnable)
                magnitudeHandler.postDelayed(magnitudeHideRunnable, 5000)
                vibrator.vibrate(VibrationEffect.createOneShot(150, VibrationEffect.DEFAULT_AMPLITUDE))
                recordShot()
            }
        }

        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        sensitivity = Sensitivity.entries.find { it.name == prefs.getString(KEY_SENSITIVITY, null) }
            ?: Sensitivity.MEDIUM
        customThreshold = prefs.getInt(KEY_CUSTOM_THRESHOLD, 15)
        shotDetector.sensitivity = sensitivity
        shotDetector.customThreshold = customThreshold.toFloat()
        shotsPerEnd = prefs.getInt(KEY_SHOTS_PER_END, 0)
        autoPauseEnabled = prefs.getBoolean(KEY_AUTO_PAUSE_ENABLED, false)
        autoPauseDuration = prefs.getInt(KEY_AUTO_PAUSE_DURATION, 60)

        sessions.addAll(sessionStorage.load())

        val pendingId = prefs.getLong(KEY_PENDING_ID, -1L)
        if (pendingId != -1L) {
            val restored = Session(
                id = pendingId,
                startTime = prefs.getLong(KEY_PENDING_START, pendingId),
                lastShotTime = prefs.getLong(KEY_PENDING_LAST, pendingId),
                shotCount = prefs.getInt(KEY_PENDING_COUNT, 0)
            )
            currentSession = restored
            shotCount = restored.shotCount
        }

        setContent {
            ArcheryShotCounterTheme {
                ArcheryApp(
                    shotCount = shotCount,
                    isDetecting = isDetecting,
                    currentSession = currentSession,
                    sessions = sessions,
                    sensitivity = sensitivity,
                    customThreshold = customThreshold,
                    currentLanguage = currentLanguage,
                    shotsPerEnd = shotsPerEnd,
                    autoPauseEnabled = autoPauseEnabled,
                    autoPauseDuration = autoPauseDuration,
                    autoPauseSecondsLeft = autoPauseSecondsLeft,
                    lastShotMagnitude = lastShotMagnitude,
                    onStartOrToggle = ::onPrimaryButton,
                    onSecondaryButton = ::onSecondaryButton,
                    onEnd = ::endSession,
                    onManualAdjust = ::manualAdjust,
                    onSensitivityChange = { s ->
                        sensitivity = s
                        shotDetector.sensitivity = s
                        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                            .edit { putString(KEY_SENSITIVITY, s.name) }
                    },
                    onCustomThresholdChange = { value ->
                        customThreshold = value
                        shotDetector.customThreshold = value.toFloat()
                        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                            .edit { putInt(KEY_CUSTOM_THRESHOLD, value) }
                    },
                    onLanguageChange = ::changeLanguage,
                    onEditSession = ::editSession,
                    onDeleteSession = ::deleteSession,
                    onShotsPerEndChange = { value ->
                        shotsPerEnd = value
                        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                            .edit { putInt(KEY_SHOTS_PER_END, value) }
                    },
                    onAutoPauseEnabledChange = { enabled ->
                        autoPauseEnabled = enabled
                        if (!enabled) cancelAutoPause()
                        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                            .edit { putBoolean(KEY_AUTO_PAUSE_ENABLED, enabled) }
                    },
                    onAutoPauseDurationChange = { value ->
                        autoPauseDuration = value
                        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                            .edit { putInt(KEY_AUTO_PAUSE_DURATION, value) }
                    }
                )
            }
        }
    }

    private fun startAutoPause() {
        stopDetection()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 100, 100, 100), -1))
        autoPauseSecondsLeft = autoPauseDuration
        autoPauseTimer = object : CountDownTimer(autoPauseDuration * 1000L, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                autoPauseSecondsLeft = ((millisUntilFinished + 999) / 1000).toInt()
            }
            override fun onFinish() {
                autoPauseSecondsLeft = -1
                autoPauseTimer = null
                startDetection()
                shotDetector.resetCooldown()
                vibrator.vibrate(VibrationEffect.createOneShot(300, VibrationEffect.DEFAULT_AMPLITUDE))
            }
        }.start()
    }

    private fun cancelAutoPause() {
        autoPauseTimer?.cancel()
        autoPauseTimer = null
        autoPauseSecondsLeft = -1
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun extendAutoPause(seconds: Int) {
        autoPauseTimer?.cancel()
        autoPauseSecondsLeft += seconds
        val remaining = autoPauseSecondsLeft * 1000L
        autoPauseTimer = object : CountDownTimer(remaining, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                autoPauseSecondsLeft = ((millisUntilFinished + 999) / 1000).toInt()
            }
            override fun onFinish() {
                autoPauseSecondsLeft = -1
                autoPauseTimer = null
                startDetection()
                shotDetector.resetCooldown()
                vibrator.vibrate(VibrationEffect.createOneShot(300, VibrationEffect.DEFAULT_AMPLITUDE))
            }
        }.start()
    }

    private fun onPrimaryButton() {
        when {
            currentSession == null -> {
                val now = System.currentTimeMillis()
                currentSession = Session(id = now, startTime = now, lastShotTime = now, shotCount = shotCount)
                startDetection()
            }
            autoPauseSecondsLeft >= 0 -> { cancelAutoPause(); startDetection(); shotDetector.resetCooldown() }
            isDetecting -> stopDetection()
            else -> startDetection()
        }
    }

    private fun onSecondaryButton() {
        if (autoPauseSecondsLeft >= 0) extendAutoPause(5) else startAutoPause()
    }

    private fun startDetection() {
        shotDetector.sensitivity = sensitivity
        shotDetector.customThreshold = customThreshold.toFloat()
        shotDetector.start()
        isDetecting = true
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun stopDetection() {
        shotDetector.stop()
        isDetecting = false
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun endSession() {
        cancelAutoPause()
        if (isDetecting) stopDetection()
        val session = currentSession ?: return
        if (shotCount > 0) {
            val updated = session.copy(lastShotTime = System.currentTimeMillis(), shotCount = shotCount)
            val idx = sessions.indexOfFirst { it.id == updated.id }
            if (idx >= 0) sessions[idx] = updated else sessions.add(0, updated)
            sessionStorage.save(sessions.toList())
        } else {
            val idx = sessions.indexOfFirst { it.id == session.id }
            if (idx >= 0) {
                sessions.removeAt(idx)
                sessionStorage.save(sessions.toList())
            }
        }
        currentSession = null
        shotCount = 0
        clearPendingSession()
    }

    private fun recordShot() {
        val session = currentSession ?: return
        val updated = session.copy(lastShotTime = System.currentTimeMillis(), shotCount = shotCount)
        currentSession = updated
        val idx = sessions.indexOfFirst { it.id == updated.id }
        if (idx >= 0) sessions[idx] = updated else sessions.add(0, updated)
        sessionStorage.save(sessions.toList())
        if (shotsPerEnd > 0 && autoPauseEnabled && shotCount % shotsPerEnd == 0) {
            startAutoPause()
        }
    }

    private fun manualAdjust(delta: Int) {
        val newCount = (shotCount + delta).coerceAtLeast(0)
        shotCount = newCount
        if (delta > 0) {
            if (currentSession == null) {
                val now = System.currentTimeMillis()
                currentSession = Session(id = now, startTime = now, lastShotTime = now, shotCount = newCount)
            }
            val session = currentSession!!
            val updated = session.copy(lastShotTime = System.currentTimeMillis(), shotCount = newCount)
            currentSession = updated
            val idx = sessions.indexOfFirst { it.id == updated.id }
            if (idx >= 0) sessions[idx] = updated else sessions.add(0, updated)
            sessionStorage.save(sessions.toList())
        } else if (delta < 0 && currentSession != null) {
            val session = currentSession!!
            val updated = session.copy(shotCount = newCount)
            currentSession = updated
            val idx = sessions.indexOfFirst { it.id == updated.id }
            if (idx >= 0) {
                sessions[idx] = updated
                sessionStorage.save(sessions.toList())
            }
        }
    }

    private fun editSession(updated: Session) {
        val idx = sessions.indexOfFirst { it.id == updated.id }
        if (idx >= 0) {
            sessions[idx] = updated
            sessionStorage.save(sessions.toList())
        }
        if (currentSession?.id == updated.id) {
            currentSession = updated
            shotCount = updated.shotCount
        }
    }

    private fun deleteSession(session: Session) {
        sessions.removeIf { it.id == session.id }
        sessionStorage.save(sessions.toList())
        if (currentSession?.id == session.id) {
            if (isDetecting) stopDetection()
            currentSession = null
            shotCount = 0
            clearPendingSession()
        }
    }

    private fun clearPendingSession() {
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit {
            remove(KEY_PENDING_ID)
            remove(KEY_PENDING_START)
            remove(KEY_PENDING_LAST)
            remove(KEY_PENDING_COUNT)
        }
    }

    override fun onPause() {
        super.onPause()
        if (isDetecting) shotDetector.stop()
        cancelAutoPause()
    }

    override fun onResume() {
        super.onResume()
        if (isDetecting) shotDetector.start()
    }

    override fun onStop() {
        super.onStop()
        val session = currentSession
        if (session != null && shotCount > 0) {
            getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit {
                putLong(KEY_PENDING_ID, session.id)
                putLong(KEY_PENDING_START, session.startTime)
                putLong(KEY_PENDING_LAST, session.lastShotTime)
                putInt(KEY_PENDING_COUNT, shotCount)
            }
        } else {
            clearPendingSession()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cancelAutoPause()
        magnitudeHandler.removeCallbacks(magnitudeHideRunnable)
        shotDetector.stop()
    }
}

@Composable
fun ArcheryApp(
    shotCount: Int,
    isDetecting: Boolean,
    currentSession: Session?,
    sessions: List<Session>,
    sensitivity: Sensitivity,
    customThreshold: Int,
    currentLanguage: AppLanguage,
    shotsPerEnd: Int,
    autoPauseEnabled: Boolean,
    autoPauseDuration: Int,
    autoPauseSecondsLeft: Int,
    lastShotMagnitude: Float?,
    onStartOrToggle: () -> Unit,
    onSecondaryButton: () -> Unit,
    onEnd: () -> Unit,
    onManualAdjust: (Int) -> Unit,
    onSensitivityChange: (Sensitivity) -> Unit,
    onCustomThresholdChange: (Int) -> Unit,
    onLanguageChange: (AppLanguage) -> Unit,
    onEditSession: (Session) -> Unit,
    onDeleteSession: (Session) -> Unit,
    onShotsPerEndChange: (Int) -> Unit,
    onAutoPauseEnabledChange: (Boolean) -> Unit,
    onAutoPauseDurationChange: (Int) -> Unit
) {
    val pagerState = rememberPagerState(initialPage = 1, pageCount = { 3 })
    var showLanguagePicker by remember { mutableStateOf(false) }
    AppScaffold {
        Box(modifier = Modifier.fillMaxSize()) {
            HorizontalPager(state = pagerState) { page ->
                when (page) {
                    0 -> HistoryScreen(
                        sessions = sessions,
                        currentSession = currentSession,
                        activeShotCount = shotCount,
                        onEdit = onEditSession,
                        onDelete = onDeleteSession
                    )
                    1 -> MainScreen(
                        shotCount = shotCount,
                        isDetecting = isDetecting,
                        currentSession = currentSession,
                        shotsPerEnd = shotsPerEnd,
                        autoPauseEnabled = autoPauseEnabled,
                        autoPauseSecondsLeft = autoPauseSecondsLeft,
                        lastShotMagnitude = lastShotMagnitude,
                        onPrimaryButton = onStartOrToggle,
                        onSecondaryButton = onSecondaryButton,
                        onEnd = onEnd,
                        onManualAdjust = onManualAdjust
                    )
                    2 -> SettingsScreen(
                        sensitivity = sensitivity,
                        customThreshold = customThreshold,
                        currentLanguage = currentLanguage,
                        shotsPerEnd = shotsPerEnd,
                        autoPauseEnabled = autoPauseEnabled,
                        autoPauseDuration = autoPauseDuration,
                        onSensitivityChange = onSensitivityChange,
                        onCustomThresholdChange = onCustomThresholdChange,
                        onShowLanguagePicker = { showLanguagePicker = true },
                        onShotsPerEndChange = onShotsPerEndChange,
                        onAutoPauseEnabledChange = onAutoPauseEnabledChange,
                        onAutoPauseDurationChange = onAutoPauseDurationChange
                    )
                }
            }
            HorizontalPageIndicator(
                pagerState = pagerState,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
            if (showLanguagePicker) {
                LanguagePickerScreen(
                    currentLanguage = currentLanguage,
                    onSelect = { lang ->
                        showLanguagePicker = false
                        onLanguageChange(lang)
                    },
                    onDismiss = { showLanguagePicker = false }
                )
            }
        }
    }
}

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
                            text = "↑ ${"%.1f".format(lastShotMagnitude)} m/s²",
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

@Composable
fun SettingsScreen(
    sensitivity: Sensitivity,
    customThreshold: Int,
    currentLanguage: AppLanguage,
    shotsPerEnd: Int,
    autoPauseEnabled: Boolean,
    autoPauseDuration: Int,
    onSensitivityChange: (Sensitivity) -> Unit,
    onCustomThresholdChange: (Int) -> Unit,
    onShowLanguagePicker: () -> Unit,
    onShotsPerEndChange: (Int) -> Unit,
    onAutoPauseEnabledChange: (Boolean) -> Unit,
    onAutoPauseDurationChange: (Int) -> Unit
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
                        text = stringResource(R.string.sensitivity_title),
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
                        Text(stringResource(s.labelRes), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
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
                            text = stringResource(R.string.sensitivity_range),
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
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp, bottom = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.series_title),
                        style = MaterialTheme.typography.titleSmall,
                        color = Color(0xFFCCCCCC)
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { if (shotsPerEnd > 0) onShotsPerEndChange(shotsPerEnd - 1) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3A3A3A), contentColor = Color(0xFFCCCCCC))
                    ) { Text("−", fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) }
                    Text(
                        text = if (shotsPerEnd == 0) stringResource(R.string.series_off) else "$shotsPerEnd",
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Button(
                        onClick = { if (shotsPerEnd < 99) onShotsPerEndChange(shotsPerEnd + 1) },
                        modifier = Modifier.weight(1f)
                    ) { Text("+", fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) }
                }
            }

            if (shotsPerEnd > 0) {
                item {
                    Button(
                        onClick = { onAutoPauseEnabledChange(!autoPauseEnabled) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .transformedHeight(this, transformationSpec),
                        transformation = SurfaceTransformation(transformationSpec),
                        colors = if (autoPauseEnabled)
                            ButtonDefaults.buttonColors()
                        else
                            ButtonDefaults.buttonColors(containerColor = Color(0xFF3A3A3A), contentColor = Color(0xFFAAAAAA))
                    ) {
                        Text(
                            text = stringResource(if (autoPauseEnabled) R.string.auto_pause_on else R.string.auto_pause_off),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                if (autoPauseEnabled) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp, bottom = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.pause_duration_title),
                                style = MaterialTheme.typography.titleSmall,
                                color = Color(0xFFCCCCCC)
                            )
                        }
                    }
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = { if (autoPauseDuration > 5) onAutoPauseDurationChange(autoPauseDuration - 5) },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3A3A3A), contentColor = Color(0xFFCCCCCC))
                            ) { Text("−", fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) }
                            Text(
                                text = formatAutoPauseDuration(autoPauseDuration, stringResource(R.string.time_m), stringResource(R.string.time_s)),
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Center,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Button(
                                onClick = { onAutoPauseDurationChange(autoPauseDuration + 5) },
                                modifier = Modifier.weight(1f)
                            ) { Text("+", fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) }
                        }
                    }
                }
            }

            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp, bottom = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.lang_section_title),
                        style = MaterialTheme.typography.titleSmall,
                        color = Color(0xFFCCCCCC)
                    )
                }
            }

            item {
                val currentLabel = if (currentLanguage == AppLanguage.SYSTEM)
                    stringResource(R.string.lang_system)
                else
                    currentLanguage.nativeName
                Button(
                    onClick = onShowLanguagePicker,
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, transformationSpec),
                    transformation = SurfaceTransformation(transformationSpec),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF3A3A3A),
                        contentColor = Color(0xFFCCCCCC)
                    )
                ) {
                    Text(currentLabel, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
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
fun LanguagePickerScreen(
    currentLanguage: AppLanguage,
    onSelect: (AppLanguage) -> Unit,
    onDismiss: () -> Unit
) {
    BackHandler(onBack = onDismiss)
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
                AppLanguage.entries.forEach { lang ->
                    item {
                        val mainName = if (lang == AppLanguage.SYSTEM)
                            stringResource(R.string.lang_system)
                        else
                            lang.nativeName
                        val subtitle = if (lang != AppLanguage.SYSTEM && lang.nativeName != lang.englishName)
                            lang.englishName
                        else
                            null
                        Button(
                            onClick = { onSelect(lang) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .transformedHeight(this, transformationSpec),
                            transformation = SurfaceTransformation(transformationSpec),
                            colors = if (currentLanguage == lang)
                                ButtonDefaults.buttonColors()
                            else
                                ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF3A3A3A),
                                    contentColor = Color(0xFFAAAAAA)
                                )
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = mainName,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                if (subtitle != null) {
                                    Text(
                                        text = subtitle,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth(),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (currentLanguage == lang)
                                            MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f)
                                        else
                                            Color(0xFF666666)
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
