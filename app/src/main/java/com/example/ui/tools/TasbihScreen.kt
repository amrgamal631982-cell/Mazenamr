package com.example.ui.tools

import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.TasbihConfig
import com.example.data.local.TasbihSessionEntity
import com.example.ui.theme.AmiriFontFamily
import com.example.ui.theme.LocalWirdColors

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TasbihScreen(
    config: TasbihConfig,
    sessions: List<TasbihSessionEntity>,
    totalAllTime: Long,
    onIncrement: () -> Unit,
    onResetAndSave: () -> Unit,
    onUpdateSettings: (dhikr: String, target: Int, vibration: Boolean, sound: Boolean) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val wirdColors = LocalWirdColors.current
    val defaultAdhkar = remember {
        listOf("سبحان الله", "الحمد لله", "الله أكبر", "أستغفر الله")
    }
    var showCustomDialog by remember { mutableStateOf(false) }
    var customDhikrInput by remember { mutableStateOf("") }

    val toneGen = remember {
        runCatching { ToneGenerator(AudioManager.STREAM_MUSIC, 40) }.getOrNull()
    }
    DisposableEffect(Unit) {
        onDispose { toneGen?.release() }
    }

    fun performFeedback() {
        if (config.vibrationEnabled) {
            runCatching {
                val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    (context.getSystemService(android.content.Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
                } else {
                    @Suppress("DEPRECATION")
                    context.getSystemService(android.content.Context.VIBRATOR_SERVICE) as Vibrator
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(25, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(25)
                }
            }
        }
        if (config.soundEnabled) {
            runCatching { toneGen?.startTone(ToneGenerator.TONE_PROP_BEEP, 35) }
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("السبحة الإلكترونية", fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = onBack, modifier = Modifier.testTag("tasbih_back_button")) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                }
            },
            actions = {
                IconButton(
                    onClick = {
                        onUpdateSettings(config.selectedDhikr, config.targetCount, !config.vibrationEnabled, config.soundEnabled)
                    },
                    modifier = Modifier.testTag("tasbih_vibration_toggle")
                ) {
                    Icon(
                        Icons.Default.Vibration,
                        contentDescription = "تبديل الاهتزاز",
                        tint = if (config.vibrationEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                    )
                }
                IconButton(
                    onClick = {
                        onUpdateSettings(config.selectedDhikr, config.targetCount, config.vibrationEnabled, !config.soundEnabled)
                    },
                    modifier = Modifier.testTag("tasbih_sound_toggle")
                ) {
                    Icon(
                        if (config.soundEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                        contentDescription = "تبديل الصوت",
                        tint = if (config.soundEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                    )
                }
            }
        )

        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 600.dp),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Dhikr Selector Chips
                item {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val allChoices = if (defaultAdhkar.contains(config.selectedDhikr)) {
                            defaultAdhkar
                        } else {
                            defaultAdhkar + config.selectedDhikr
                        }
                        allChoices.forEach { d ->
                            FilterChip(
                                selected = config.selectedDhikr == d,
                                onClick = {
                                    onUpdateSettings(d, config.targetCount, config.vibrationEnabled, config.soundEnabled)
                                },
                                label = { Text(d) }
                            )
                        }
                        FilterChip(
                            selected = false,
                            onClick = { showCustomDialog = true },
                            label = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("ذكر مخصص")
                                }
                            }
                        )
                    }
                }

                // Target Selector
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("الهدف:", style = MaterialTheme.typography.titleSmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(33, 99, 100, 1000).forEach { target ->
                                FilterChip(
                                    selected = config.targetCount == target,
                                    onClick = {
                                        onUpdateSettings(config.selectedDhikr, target, config.vibrationEnabled, config.soundEnabled)
                                    },
                                    label = { Text("$target") }
                                )
                            }
                        }
                    }
                }

                // Main Tactile Tasbih Orb
                item {
                    val progress = (config.currentCount.toFloat() / config.targetCount.coerceAtLeast(1)).coerceIn(0f, 1f)
                    val animatedProgress by animateFloatAsState(targetValue = progress, label = "tasbih_ring")

                    Card(
                        shape = RoundedCornerShape(28.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, wirdColors.subtleBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(18.dp)
                        ) {
                            Text(
                                text = config.selectedDhikr,
                                style = MaterialTheme.typography.headlineSmall.copy(fontFamily = AmiriFontFamily),
                                color = MaterialTheme.colorScheme.primary
                            )

                            Box(
                                modifier = Modifier
                                    .size(210.dp)
                                    .clip(CircleShape)
                                    .clickable {
                                        performFeedback()
                                        onIncrement()
                                    }
                                    .testTag("tasbih_tap_button"),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    progress = { animatedProgress },
                                    modifier = Modifier.size(210.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                    strokeWidth = 10.dp
                                )
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                                    modifier = Modifier.size(174.dp)
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = "${config.currentCount}",
                                            style = MaterialTheme.typography.displaySmall.copy(fontSize = 46.sp),
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                        Text(
                                            text = "من ${config.targetCount}",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = onResetAndSave,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("tasbih_reset_save_button")
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("تصفير وحفظ الجلسة")
                                }
                                Button(
                                    onClick = {
                                        performFeedback()
                                        onIncrement()
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("تسبيح")
                                }
                            }

                            Text(
                                text = "مجموع التسبيحات المحفوظة: ${totalAllTime + config.currentCount}",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }

                // Saved Sessions History
                if (sessions.isNotEmpty()) {
                    item {
                        Text(
                            text = "سجل الجلسات المحفوظة (دون إنترنت)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    items(sessions.take(15), key = { it.id }) { session ->
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, wirdColors.subtleBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(Icons.Default.Save, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                    Text(session.dhikrText, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                }
                                Text(
                                    text = "${session.count} / ${session.target}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCustomDialog) {
        AlertDialog(
            onDismissRequest = { showCustomDialog = false },
            title = { Text("إضافة ذكر مخصص") },
            text = {
                OutlinedTextField(
                    value = customDhikrInput,
                    onValueChange = { customDhikrInput = it },
                    label = { Text("اكتب الذكر") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (customDhikrInput.isNotBlank()) {
                            onUpdateSettings(customDhikrInput.trim(), config.targetCount, config.vibrationEnabled, config.soundEnabled)
                        }
                        showCustomDialog = false
                        customDhikrInput = ""
                    }
                ) { Text("اعتماد") }
            },
            dismissButton = {
                TextButton(onClick = { showCustomDialog = false }) { Text("إلغاء") }
            }
        )
    }
}
