package com.example.ui.tools

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Today
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.adhkar.AdhkarRepository
import com.example.data.local.DownloadedAudioEntity
import com.example.data.quran.QuranAuditReport
import com.example.data.quran.QuranIntegrityVerifier
import com.example.data.reciters.ReciterCatalog
import com.example.data.tafsir.TafsirPackStatus
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.LocalWirdColors

@Composable
fun MoreToolsScreen(
    themeMode: AppThemeMode,
    onSelectTheme: (AppThemeMode) -> Unit,
    onOpenQibla: () -> Unit,
    onOpenTasbih: () -> Unit,
    onOpenWird: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenBookmarks: () -> Unit,
    onOpenStorage: () -> Unit,
    onOpenSourcesAudit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val wirdColors = LocalWirdColors.current
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 640.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = "الأدوات والإعدادات",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, wirdColors.subtleBorder)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("المظهر", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(
                                AppThemeMode.SYSTEM to "تلقائي",
                                AppThemeMode.LIGHT to "نهاري",
                                AppThemeMode.DARK to "ليلي"
                            ).forEach { (mode, label) ->
                                androidx.compose.material3.FilterChip(
                                    selected = themeMode == mode,
                                    onClick = { onSelectTheme(mode) },
                                    label = { Text(label) }
                                )
                            }
                        }
                    }
                }
            }

            item {
                ToolRowCard("القبلة", "بوصلة حقيقية بحساب جغرافي ومستشعرات الهاتف", Icons.Default.Explore, onOpenQibla, "more_qibla")
            }
            item {
                ToolRowCard("السبحة الإلكترونية", "عداد تسبيح هادئ مع حفظ الجلسات دون إنترنت", Icons.Default.RadioButtonChecked, onOpenTasbih, "more_tasbih")
            }
            item {
                ToolRowCard("الورد اليومي والختمة", "خطة القراءة، التقويم اليومي، والتنبيه الاختياري", Icons.Default.Today, onOpenWird, "more_wird")
            }
            item {
                ToolRowCard("البحث في القرآن", "بحث فوري دون إنترنت مع دعم الهمزات والتشكيل", Icons.Default.Search, onOpenSearch, "more_search")
            }
            item {
                ToolRowCard("المفضلة والملاحظات", "الآيات المحفوظة والمجموعات والملاحظات الشخصية", Icons.Default.Bookmark, onOpenBookmarks, "more_bookmarks")
            }
            item {
                ToolRowCard("إدارة التخزين والحزم", "تنزيل حزم التفسير وحذف التسجيلات الصوتية", Icons.Default.Storage, onOpenStorage, "more_storage")
            }
            item {
                ToolRowCard("المصادر والمراجعة", "بيان مصادر القرآن والتفسير والأذكار والفحص الآلي", Icons.Default.FactCheck, onOpenSourcesAudit, "more_sources")
            }
        }
    }
}

@Composable
private fun ToolRowCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    tag: String
) {
    val wirdColors = LocalWirdColors.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag(tag),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, wirdColors.subtleBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StorageManagerScreen(
    tafsirPacks: List<TafsirPackStatus>,
    wordMeaningsBytes: Long,
    downloadProgress: Map<String, Pair<Int, Int>>,
    storageMessage: String?,
    downloadedAudio: List<DownloadedAudioEntity>,
    totalAudioBytes: Long,
    onRefresh: () -> Unit,
    onDownloadPack: (String) -> Unit,
    onDeletePack: (String) -> Unit,
    onClearWordMeanings: () -> Unit,
    onDeleteAudioFile: (DownloadedAudioEntity) -> Unit,
    onDeleteAllAudio: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val wirdColors = LocalWirdColors.current
    LaunchedEffect(Unit) { onRefresh() }

    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("إدارة التخزين والحزم", fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = onBack, modifier = Modifier.testTag("storage_back_button")) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
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
                item {
                    val totalTafsirBytes = tafsirPacks.sumOf { it.sizeBytes } + wordMeaningsBytes
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("ملخص المساحة المستخدمة محليًا", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("حزم التفسير والمعاني: ${formatBytes(totalTafsirBytes)}")
                            Text("التلاوات الصوتية المحملة: ${formatBytes(totalAudioBytes)} (${downloadedAudio.size} ملف)")
                            Text("النص القرآني والأذكار المدمجة: ~1.7 MB (مدمجة للعمل الدائم دون إنترنت)", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                storageMessage?.let { msg ->
                    item {
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(msg, modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                item {
                    Text("حزم التفسير ومعاني الكلمات الموثقة", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }

                items(tafsirPacks, key = { it.source.id }) { pack ->
                    val prog = downloadProgress[pack.source.id]
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, wirdColors.subtleBorder)
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(pack.source.titleArabic, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text(pack.source.authorArabic, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            Text(
                                text = "المحمل محليًا: ${pack.downloadedSurahs} / 114 سورة (${formatBytes(pack.sizeBytes)})",
                                style = MaterialTheme.typography.bodySmall
                            )
                            if (prog != null) {
                                LinearProgressIndicator(
                                    progress = { prog.first.toFloat() / prog.second.coerceAtLeast(1) },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Text("جاري التنزيل: ${prog.first} / ${prog.second} سورة", style = MaterialTheme.typography.labelSmall)
                            } else {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    if (pack.downloadedSurahs < 114) {
                                        Button(onClick = { onDownloadPack(pack.source.id) }) {
                                            Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(6.dp))
                                            Text("تنزيل الحزمة كاملة")
                                        }
                                    }
                                    if (pack.totalEntries > 0) {
                                        OutlinedButton(onClick = { onDeletePack(pack.source.id) }) {
                                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text("حذف")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (wordMeaningsBytes > 0) {
                    item {
                        OutlinedButton(onClick = onClearWordMeanings, modifier = Modifier.fillMaxWidth()) {
                            Text("حذف ذاكرة معاني الكلمات المؤقتة (${formatBytes(wordMeaningsBytes)})")
                        }
                    }
                }

                item {
                    Spacer(Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("التلاوات المحملة", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        if (downloadedAudio.isNotEmpty()) {
                            OutlinedButton(onClick = onDeleteAllAudio) {
                                Text("حذف جميع الصوتيات")
                            }
                        }
                    }
                }

                if (downloadedAudio.isEmpty()) {
                    item {
                        Text(
                            text = "لا توجد ملفات تلاوة محملة حاليًا.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    items(downloadedAudio, key = { "${it.reciterId}_${it.surahIndex}_${it.ayahNumber}" }) { item ->
                        val reciterName = ReciterCatalog.findById(item.reciterId).nameArabic
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
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("سورة ${item.surahIndex} — $reciterName", style = MaterialTheme.typography.titleSmall)
                                    Text(formatBytes(item.sizeBytes), style = MaterialTheme.typography.labelSmall)
                                }
                                IconButton(onClick = { onDeleteAudioFile(item) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "حذف الملف")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SourcesAuditScreen(
    auditReport: QuranAuditReport?,
    isAuditing: Boolean,
    onRunAudit: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val wirdColors = LocalWirdColors.current

    LaunchedEffect(Unit) {
        if (auditReport == null) onRunAudit()
    }

    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("المصادر والمراجعة", fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = onBack, modifier = Modifier.testTag("sources_back_button")) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                }
            }
        )

        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 640.dp),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Live Quran Integrity Self-Audit
                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (auditReport?.passed == true) {
                                wirdColors.verifiedBg
                            } else {
                                MaterialTheme.colorScheme.surface
                            }
                        ),
                        border = BorderStroke(1.dp, wirdColors.verifiedColor.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = wirdColors.verifiedColor)
                                Text("نتيجة الفحص الآلي لنص القرآن الكريم", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }
                            if (isAuditing) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                    Text("جاري مطابقة جميع الآيات (6,236) وبصمة SHA-256...")
                                }
                            } else if (auditReport != null) {
                                Text(auditReport.details, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    text = "السور: ${auditReport.surahCount} • الآيات: ${auditReport.totalAyahCount} • الاختلافات: ${auditReport.mismatchedAyahs} • النقص/التكرار: ${auditReport.duplicateOrMissingCount}",
                                    style = MaterialTheme.typography.labelMedium
                                )
                                Text(
                                    text = "SHA-256 (Uthmani): ${auditReport.uthmaniXmlSha256}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Button(
                                onClick = onRunAudit,
                                modifier = Modifier.testTag("run_quran_audit_button")
                            ) {
                                Text("إعادة فحص ومطابقة المصحف الآن")
                            }
                        }
                    }
                }

                // Tanzil Copyright & Link
                item {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, wirdColors.subtleBorder)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("1. النص القرآني العثماني والبيانات الهيكلية", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text(QuranIntegrityVerifier.TANZIL_COPYRIGHT_NOTICE, style = MaterialTheme.typography.bodySmall)
                            Text("تاريخ المراجعة والتحقق البرمجي: 2026-11-20", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            OutlinedButton(
                                onClick = {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(QuranIntegrityVerifier.TANZIL_SOURCE_URL)))
                                }
                            ) {
                                Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("زيارة موقع مشروع تنزيل (tanzil.net)")
                            }
                        }
                    }
                }

                // Tafsir & Word Meanings Sources
                item {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, wirdColors.subtleBorder)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("2. التفاسير ومعاني الكلمات", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text("• تفسير القرآن العظيم — الحافظ ابن كثير (نسخة spa5k/tafsir_api الموثقة)", style = MaterialTheme.typography.bodySmall)
                            Text("• جامع البيان عن تأويل آي القرآن — الإمام الطبري (نسخة spa5k/tafsir_api)", style = MaterialTheme.typography.bodySmall)
                            Text("• تيسير الكريم الرحمن — الشيخ عبد الرحمن السعدي (نسخة spa5k/tafsir_api)", style = MaterialTheme.typography.bodySmall)
                            Text("• الميسر في غريب القرآن الكريم — مركز الدراسات القرآنية / مجمع الملك فهد", style = MaterialTheme.typography.bodySmall)
                            Text("• معاني الكلمات كلمة بكلمة — Quran.com API v4 (عرض الكلمات العثمانية)", style = MaterialTheme.typography.bodySmall)
                            Text("ملاحظة الأمانة العلمية: لا يتم استخدام أي ذكاء اصطناعي لتوليد التفاسير أو أسباب النزول، وتُعرض رسالة عدم توفر صريحة عند غياب النص المحمّل.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                // Adhkar & Duas Sources
                item {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, wirdColors.subtleBorder)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("3. الأذكار والأدعية المأثورة", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text("• ${AdhkarRepository.HISN_SOURCE_ATTRIBUTION}", style = MaterialTheme.typography.bodySmall)
                            Text("• أدعية القرآن الكريم والأنبياء مستخرجة حرفيًا من نص تنزيل العثماني المدمج.", style = MaterialTheme.typography.bodySmall)
                            Text("• أرشيف حصن المسلم الموثق: SHA-256 baaa876a36069a80... (النسخة العربية المشكولة مع التخريج).", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                // Recitations Sources
                item {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, wirdColors.subtleBorder)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("4. مصادر التلاوات القرآنية", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            ReciterCatalog.RECITERS.forEach { rec ->
                                Text("• الشيخ ${rec.nameArabic}: ${rec.sourceLabel}", style = MaterialTheme.typography.bodySmall)
                            }
                            Text("يتم التحقق الفعلي من وجود الملفات عبر HTTP HEAD قبل إتاحتها، ولا يتم تشغيل تسجيل قارئ آخر إطلاقًا.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }
}

private fun formatBytes(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${"%.1f".format(bytes / 1024.0)} KB"
        else -> "${"%.2f".format(bytes / (1024.0 * 1024.0))} MB"
    }
}
