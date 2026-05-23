package com.vemestael.archeryshotcounter.presentation

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
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.content.edit
import androidx.wear.compose.foundation.pager.HorizontalPager
import androidx.wear.compose.foundation.pager.rememberPagerState
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.HorizontalPageIndicator
import com.vemestael.archeryshotcounter.presentation.theme.ArcheryShotCounterTheme
import java.util.Locale
import java.util.concurrent.Executors

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

class MainActivity : ComponentActivity() {

    private lateinit var shotDetector: ShotDetector
    private lateinit var vibrator: Vibrator
    private lateinit var database: AppDatabase
    private val dbExecutor = Executors.newSingleThreadExecutor()

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
        database = AppDatabase.getInstance(this)

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
                recordShot(magnitude)
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

        dbExecutor.execute {
            if (database.sessionDao().getAll().isEmpty()) {
                SessionStorage(applicationContext).load()
                    .forEach { database.sessionDao().insertOrUpdate(it) }
            }
            val all = database.sessionDao().getAll()
            runOnUiThread {
                sessions.addAll(all)
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
            }
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
                val session = Session(id = now, startTime = now, lastShotTime = now, shotCount = shotCount)
                currentSession = session
                if (shotCount > 0) sessions.add(0, session)
                dbExecutor.execute { database.sessionDao().insertOrUpdate(session) }
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
            dbExecutor.execute { database.sessionDao().insertOrUpdate(updated) }
        } else {
            sessions.removeIf { it.id == session.id }
            dbExecutor.execute { database.sessionDao().delete(session) }
        }
        currentSession = null
        shotCount = 0
        clearPendingSession()
    }

    private fun recordShot(magnitude: Float) {
        val session = currentSession ?: return
        val now = System.currentTimeMillis()
        val updated = session.copy(lastShotTime = now, shotCount = shotCount)
        currentSession = updated
        val idx = sessions.indexOfFirst { it.id == updated.id }
        if (idx >= 0) sessions[idx] = updated else sessions.add(0, updated)
        dbExecutor.execute {
            database.sessionDao().insertOrUpdate(updated)
            database.shotDao().insert(Shot(sessionId = session.id, timestamp = now, magnitude = magnitude))
        }
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
                val session = Session(id = now, startTime = now, lastShotTime = now, shotCount = newCount)
                currentSession = session
                sessions.add(0, session)
                dbExecutor.execute { database.sessionDao().insertOrUpdate(session) }
                return
            }
            val session = currentSession!!
            val updated = session.copy(lastShotTime = System.currentTimeMillis(), shotCount = newCount)
            currentSession = updated
            val idx = sessions.indexOfFirst { it.id == updated.id }
            if (idx >= 0) sessions[idx] = updated else sessions.add(0, updated)
            dbExecutor.execute { database.sessionDao().insertOrUpdate(updated) }
        } else if (delta < 0 && currentSession != null) {
            val session = currentSession!!
            val updated = session.copy(shotCount = newCount)
            currentSession = updated
            val idx = sessions.indexOfFirst { it.id == updated.id }
            if (idx >= 0) {
                sessions[idx] = updated
                dbExecutor.execute { database.sessionDao().insertOrUpdate(updated) }
            }
        }
    }

    private fun editSession(updated: Session) {
        val idx = sessions.indexOfFirst { it.id == updated.id }
        if (idx >= 0) {
            sessions[idx] = updated
            dbExecutor.execute { database.sessionDao().insertOrUpdate(updated) }
        }
        if (currentSession?.id == updated.id) {
            currentSession = updated
            shotCount = updated.shotCount
        }
    }

    private fun deleteSession(session: Session) {
        sessions.removeIf { it.id == session.id }
        dbExecutor.execute { database.sessionDao().delete(session) }
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
        dbExecutor.shutdown()
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
