package com.example.ui.reciters

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.DownloadedAudioEntity
import com.example.data.local.ReciterCoverageEntity
import com.example.data.quran.SurahMeta
import com.example.data.reciters.ReciterCatalog
import com.example.ui.theme.LocalWirdColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReciterDetailScreen(
    reciterId: String,
    isDefaultReciter: Boolean,
    surahs: List<SurahMeta>,
    coverageMap: Map<Int, ReciterCoverageEntity>,
    downloadedAudio: List<DownloadedAudioEntity>,
    verifyProgress: Pair<Int, Int>?,
    downloadingSurahs: Map<Pair<String, Int>, Float>,
    statusMessage: String?,
    onBack: () -> Unit,
    onSetDefault: () -> Unit,
    onVerifyAll: () -> Unit,
    onVerifySurah: (Int) -> Unit,
    onPlaySurah: (Int) -> Unit,
    onPlayAyahRange: (surahIndex: Int, fromAyah: Int, toAyah: Int, repeatTimes: Int) -> Unit,
    onDownloadSurah: (Int) -> Unit,
    onDismissStatus: () -> Unit,
    modifier: Modifier = Modifier
) {
    val reciter = ReciterCatalog.findById(reciterId)
    val wirdColors = LocalWirdColors.current
    val downloadedSurahSet = downloadedAudio
        .filter { it.reciterId == reciterId && it.ayahNumber == 0 }
        .map { it.surahIndex }
        .toSet()

    var rangeDialogSurah by remember { mutableStateOf<SurahMeta?>(null) }

    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("الشيخ ${reciter.nameArabic}", fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = onBack, modifier = Modifier.testTag("reciter_back_button")) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                }
            },
            actions = {
                IconButton(
                    onClick = onSetDefault,
                    modifier = Modifier.testTag("set_default_reciter_button")
                ) {
                    Icon(
                        imageVector = if (isDefaultReciter) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = "تعيين قارئًا افتراضيًا",
                        tint = if (isDefaultReciter) wirdColors.goldAccent else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        )

        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 640.dp),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Reciter Profile & Verification Summary Card
                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, wirdColors.subtleBorder)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "${reciter.region.labelArabic} • ${reciter.styleArabic} (${reciter.riwayahArabic})",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(text = reciter.bioShort, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                text = "مصدر التسجيلات الموثق: ${reciter.sourceLabel}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            val verifiedCount = coverageMap.values.count { it.ayahAvailableCount > 0 || it.fullSurahAvailable }
                            Text(
                                text = if (coverageMap.isEmpty()) {
                                    "حالة التغطية: لم يتم فحص السور بعد — اضغط للتحقق الفعلي"
                                } else {
                                    "السور المتحققة: $verifiedCount من 114 سورة (${coverageMap.size} تم فحصها)"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                color = if (verifiedCount > 0) wirdColors.verifiedColor else MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            if (verifyProgress != null) {
                                LinearProgressIndicator(
                                    progress = { verifyProgress.first.toFloat() / verifyProgress.second.coerceAtLeast(1) },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Text(
                                    text = "جاري التحقق الفعلي من التسجيلات (${verifyProgress.first}/${verifyProgress.second})...",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            } else {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = onVerifyAll,
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("verify_coverage_button")
                                    ) {
                                        Icon(Icons.Default.VerifiedUser, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("التحقق الفعلي من التوفر")
                                    }
                                    if (!isDefaultReciter) {
                                        Button(onClick = onSetDefault) {
                                            Text("تعيين كافتراضي")
                                        }
                                    }
                                }
                            }

                            statusMessage?.let { msg ->
                                Surface(
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(msg, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                                        TextButton(onClick = onDismissStatus) { Text("إغلاق") }
                                    }
                                }
                            }
                        }
                    }
                }

                // 114 Surahs list
                items(surahs, key = { it.index }) { surah ->
                    val cov = coverageMap[surah.index]
                    val isDownloaded = downloadedSurahSet.contains(surah.index)
                    val dlFrac = downloadingSurahs[reciterId to surah.index]

                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, wirdColors.subtleBorder)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${surah.index}. سورة ${surah.nameArabic}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                val covLabel = when {
                                    isDownloaded -> "محفوظة محليًا (تعمل دون إنترنت)"
                                    cov == null -> "${surah.ayahCount} آية • اضغط للتشغيل أو التحقق"
                                    cov.ayahAvailableCount > 0 || cov.fullSurahAvailable ->
                                        buildString {
                                            append("متاح")
                                            if (cov.hasTimestamps) append(" • مزامنة الآيات متوفرة")
                                        }
                                    else -> "غير متاح حاليًا على الخادم"
                                }
                                Text(
                                    text = covLabel,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = when {
                                        isDownloaded || (cov != null && (cov.ayahAvailableCount > 0 || cov.fullSurahAvailable)) ->
                                            wirdColors.verifiedColor
                                        cov != null -> MaterialTheme.colorScheme.error
                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { rangeDialogSurah = surah }
                                ) {
                                    Icon(Icons.Default.Repeat, contentDescription = "تكرار مقطع آيات")
                                }

                                if (dlFrac != null) {
                                    CircularProgressIndicator(
                                        progress = { dlFrac },
                                        modifier = Modifier.size(28.dp),
                                        strokeWidth = 3.dp
                                    )
                                } else if (isDownloaded) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = "تم التنزيل",
                                        tint = wirdColors.verifiedColor,
                                        modifier = Modifier.padding(8.dp)
                                    )
                                } else {
                                    IconButton(
                                        onClick = { onDownloadSurah(surah.index) }
                                    ) {
                                        Icon(Icons.Default.CloudDownload, contentDescription = "تنزيل السورة")
                                    }
                                }

                                IconButton(
                                    onClick = {
                                        if (cov == null) onVerifySurah(surah.index)
                                        onPlaySurah(surah.index)
                                    },
                                    modifier = Modifier.testTag("play_reciter_surah_${surah.index}")
                                ) {
                                    Icon(
                                        Icons.Default.PlayArrow,
                                        contentDescription = "تشغيل سورة ${surah.nameArabic}",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    rangeDialogSurah?.let { surah ->
        var fromStr by remember { mutableStateOf("1") }
        var toStr by remember { mutableStateOf(surah.ayahCount.coerceAtMost(5).toString()) }
        var repeatStr by remember { mutableStateOf("3") }
        AlertDialog(
            onDismissRequest = { rangeDialogSurah = null },
            title = { Text("تكرار مجموعة آيات — سورة ${surah.nameArabic}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("حدد نطاق الآيات (من 1 إلى ${surah.ayahCount}) وعدد مرات التكرار:")
                    OutlinedTextField(
                        value = fromStr,
                        onValueChange = { fromStr = it.filter(Char::isDigit) },
                        label = { Text("من آية") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = toStr,
                        onValueChange = { toStr = it.filter(Char::isDigit) },
                        label = { Text("إلى آية") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = repeatStr,
                        onValueChange = { repeatStr = it.filter(Char::isDigit) },
                        label = { Text("عدد مرات التكرار (0 = مستمر)") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val f = fromStr.toIntOrNull() ?: 1
                        val t = toStr.toIntOrNull() ?: f
                        val r = repeatStr.toIntOrNull() ?: 3
                        onPlayAyahRange(surah.index, f, t, r)
                        rangeDialogSurah = null
                    }
                ) {
                    Text("تشغيل المقطع")
                }
            },
            dismissButton = {
                TextButton(onClick = { rangeDialogSurah = null }) { Text("إلغاء") }
            }
        )
    }
}
