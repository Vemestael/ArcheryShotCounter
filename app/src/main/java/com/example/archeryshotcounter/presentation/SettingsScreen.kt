package com.example.archeryshotcounter.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import com.example.archeryshotcounter.R

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
