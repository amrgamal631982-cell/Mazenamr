package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.audio.PlaybackUiState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RecitationPlayerBar(
    state: PlaybackUiState,
    onTogglePlayPause: () -> Unit,
    onNextAyah: () -> Unit,
    onPreviousAyah: () -> Unit,
    onSpeedChange: (Float) -> Unit,
    onRepeatTargetChange: (Int) -> Unit,
    onSleepTimerChange: (Int?) -> Unit,
    onSeekTo: (Long) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!state.isVisible) return

    var showExpandedSheet by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .clickable { showExpandedSheet = true },
        shape = RoundedCornerShape(18.dp),
        tonalElevation = 6.dp,
        shadowElevation = 4.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Column {
            if (state.durationMs > 0) {
                LinearProgressIndicator(
                    progress = { (state.currentPositionMs.toFloat() / state.durationMs).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(3.dp)
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "سورة ${state.surahName} — آية ${state.currentAyah}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = state.errorMessage ?: "الشيخ ${state.reciter.nameArabic}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (state.errorMessage != null) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onPreviousAyah) {
                        Icon(Icons.Default.SkipNext, contentDescription = "الآية السابقة")
                    }
                    if (state.isBuffering) {
                        CircularProgressIndicator(modifier = Modifier.size(26.dp), strokeWidth = 2.5.dp)
                    } else {
                        IconButton(
                            onClick = onTogglePlayPause,
                            modifier = Modifier.testTag("mini_player_play_pause")
                        ) {
                            Icon(
                                imageVector = if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (state.isPlaying) "إيقاف مؤقت" else "تشغيل"
                            )
                        }
                    }
                    IconButton(onClick = onNextAyah) {
                        Icon(Icons.Default.SkipPrevious, contentDescription = "الآية التالية")
                    }
                    IconButton(onClick = { showExpandedSheet = true }) {
                        Icon(Icons.Default.ExpandLess, contentDescription = "خيارات المشغل")
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق المشغل")
                    }
                }
            }
        }
    }

    if (showExpandedSheet) {
        ModalBottomSheet(onDismissRequest = { showExpandedSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column {
                    Text(
                        text = "سورة ${state.surahName} • آية ${state.currentAyah} من ${state.totalAyahs}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "الشيخ ${state.reciter.nameArabic} (${state.reciter.styleArabic})",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (state.rangeStartAyah != null && state.rangeEndAyah != null) {
                        Text(
                            text = "تكرار المقطع: من آية ${state.rangeStartAyah} إلى ${state.rangeEndAyah}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }

                state.errorMessage?.let { err ->
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = err,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                if (state.durationMs > 0) {
                    Column {
                        Slider(
                            value = state.currentPositionMs.toFloat(),
                            onValueChange = { onSeekTo(it.toLong()) },
                            valueRange = 0f..state.durationMs.toFloat().coerceAtLeast(1f)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(formatMs(state.currentPositionMs), style = MaterialTheme.typography.labelSmall)
                            Text(formatMs(state.durationMs), style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                // Transport Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onPreviousAyah, modifier = Modifier.size(52.dp)) {
                        Icon(Icons.Default.SkipNext, contentDescription = "الآية السابقة", modifier = Modifier.size(32.dp))
                    }
                    Spacer(Modifier.width(16.dp))
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(64.dp).clickable(onClick = onTogglePlayPause)
                    ) {
                        androidx.compose.foundation.layout.Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "تشغيل أو إيقاف",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(34.dp)
                            )
                        }
                    }
                    Spacer(Modifier.width(16.dp))
                    IconButton(onClick = onNextAyah, modifier = Modifier.size(52.dp)) {
                        Icon(Icons.Default.SkipPrevious, contentDescription = "الآية التالية", modifier = Modifier.size(32.dp))
                    }
                }

                // Playback Speed
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text("سرعة التلاوة", style = MaterialTheme.typography.titleSmall)
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(0.75f, 1.0f, 1.25f, 1.5f).forEach { sp ->
                            FilterChip(
                                selected = state.speed == sp,
                                onClick = { onSpeedChange(sp) },
                                label = { Text("${sp}x") }
                            )
                        }
                    }
                }

                // Single Ayah Repeat
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.RepeatOne, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text("تكرار الآية الحالية", style = MaterialTheme.typography.titleSmall)
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(1 to "بدون", 3 to "3 مرات", 5 to "5 مرات", 0 to "مستمر").forEach { (count, label) ->
                            FilterChip(
                                selected = state.singleAyahRepeatTarget == count,
                                onClick = { onRepeatTargetChange(count) },
                                label = { Text(label) }
                            )
                        }
                    }
                }

                // Sleep Timer
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.Timer, contentDescription = null, modifier = Modifier.size(18.dp))
                        val timerTitle = state.sleepTimerRemainingSec?.let {
                            "مؤقت الإيقاف (متبقي ${it / 60}:${(it % 60).toString().padStart(2, '0')})"
                        } ?: "مؤقت إيقاف التلاوة"
                        Text(timerTitle, style = MaterialTheme.typography.titleSmall)
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(null to "إيقاف المؤقت", 10 to "10 د", 15 to "15 د", 30 to "30 د", 60 to "60 د").forEach { (min, label) ->
                            FilterChip(
                                selected = (min == null && state.sleepTimerRemainingSec == null),
                                onClick = { onSleepTimerChange(min) },
                                label = { Text(label) }
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatMs(ms: Long): String {
    val totalSec = (ms / 1000).coerceAtLeast(0)
    val mins = totalSec / 60
    val secs = totalSec % 60
    return "$mins:${secs.toString().padStart(2, '0')}"
}
