package com.example.ui.mushaf

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.BookmarkFolderEntity
import com.example.data.quran.Ayah
import com.example.data.quran.SurahMeta
import com.example.data.tafsir.AyahStudyBundle
import com.example.data.tafsir.TafsirRepository
import com.example.ui.theme.AmiriQuranFontFamily
import com.example.ui.theme.LocalWirdColors

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AyahStudyBottomSheet(
    ayah: Ayah,
    surah: SurahMeta?,
    studyBundle: AyahStudyBundle?,
    isLoadingStudy: Boolean,
    isBookmarked: Boolean,
    initialNote: String,
    initialFolderId: Long,
    folders: List<BookmarkFolderEntity>,
    onDismiss: () -> Unit,
    onPlayAyah: (Ayah) -> Unit,
    onRetryFetch: () -> Unit,
    onSaveBookmark: (Ayah, Long, String) -> Unit,
    onRemoveBookmark: (Ayah) -> Unit,
    onMarkLastRead: (Ayah) -> Unit
) {
    val context = LocalContext.current
    val wirdColors = LocalWirdColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var selectedTafsirTab by remember { mutableIntStateOf(0) } // 0: Ibn Kathir, 1: Tabari, 2: Saadi
    var noteDraft by remember(ayah.id, initialNote) { mutableStateOf(initialNote) }
    var selectedFolderId by remember(ayah.id, initialFolderId) { mutableLongStateOf(initialFolderId) }

    val tafsirSources = remember {
        TafsirRepository.SOURCES.filter { it.id != "gharib" }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Verbatim Ayah Text
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, wirdColors.goldAccent.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "${ayah.uthmaniText} ﴿${toArabicDigits(ayah.ayahNumber)}﴾",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontFamily = AmiriQuranFontFamily,
                            fontSize = 22.sp,
                            lineHeight = 40.sp
                        )
                    )
                    // 6. Surah Information
                    Text(
                        text = "سورة ${surah?.nameArabic ?: ayah.surahIndex} (${surah?.revelationTypeArabic ?: ""}) • آية ${ayah.ayahNumber} من ${surah?.ayahCount ?: ""} • صفحة ${ayah.pageNumber} • جزء ${ayah.juzNumber} • حزب ${ayah.hizbNumber}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // 7, 8, 9, 10 Quick Action Row: Listen, Copy, Share, Last Read
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { onPlayAyah(ayah) },
                    modifier = Modifier.testTag("sheet_play_ayah_button"),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("استماع للآية")
                }

                OutlinedButton(
                    onClick = {
                        val copyText = "${ayah.uthmaniText} ﴿${ayah.ayahNumber}﴾ — [سورة ${surah?.nameArabic ?: ayah.surahIndex}: ${ayah.ayahNumber}]"
                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        cm.setPrimaryClip(ClipData.newPlainText("آية قرآنية", copyText))
                        Toast.makeText(context, "تم نسخ الآية", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.testTag("sheet_copy_ayah_button")
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("نسخ")
                }

                OutlinedButton(
                    onClick = {
                        val shareText = "${ayah.uthmaniText} ﴿${ayah.ayahNumber}﴾\n[سورة ${surah?.nameArabic ?: ayah.surahIndex}: ${ayah.ayahNumber}]"
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, shareText)
                        }
                        context.startActivity(Intent.createChooser(intent, "مشاركة الآية"))
                    },
                    modifier = Modifier.testTag("sheet_share_ayah_button")
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("مشاركة")
                }

                OutlinedButton(
                    onClick = {
                        onMarkLastRead(ayah)
                        Toast.makeText(context, "تم حفظ موضع القراءة", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Icon(Icons.Default.MenuBook, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("تعيين آخر قراءة")
                }
            }

            HorizontalDivider()

            if (isLoadingStudy) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.5.dp)
                    Spacer(Modifier.width(10.dp))
                    Text("جاري جلب التفاسير ومعاني الكلمات الموثقة...")
                }
            } else {
                // 2. Word-by-Word Meanings & 5. Quranic Vocabulary (Gharib)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "معاني الكلمات والمفردات القرآنية",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        TextButton(onClick = onRetryFetch) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("تحديث")
                        }
                    }

                    val words = studyBundle?.wordMeanings.orEmpty()
                    val gharib = studyBundle?.gharibSummary
                    if (words.isNotEmpty()) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            words.forEach { w ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                                    border = BorderStroke(1.dp, wirdColors.subtleBorder)
                                ) {
                                    Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                                        Text(
                                            text = w.wordUthmani,
                                            style = MaterialTheme.typography.titleSmall.copy(fontFamily = AmiriQuranFontFamily),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        if (w.meaning.isNotBlank()) {
                                            Text(
                                                text = w.meaning,
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        Text(
                            text = "المصدر اللغوي: ${words.first().sourceLabel}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else if (!gharib.isNullOrBlank()) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(gharib, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    text = "المصدر: الميسر في غريب القرآن الكريم — مركز الدراسات القرآنية",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    } else {
                        Text(
                            text = "لم تتوفر معاني مفردات محملة لهذه الآية دون إنترنت بعد. يمكنك تنزيل حزمة «الميسر في غريب القرآن» من إدارة التخزين.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                HorizontalDivider()

                // 3. Authentic Tafsir Section (Ibn Kathir, Tabari, Saadi — strictly separated)
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "التفسير الموثق",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    TabRow(selectedTabIndex = selectedTafsirTab) {
                        tafsirSources.forEachIndexed { idx, src ->
                            val shortTitle = when (src.id) {
                                "ibn_kathir" -> "ابن كثير"
                                "tabari" -> "الطبري"
                                else -> "السعدي"
                            }
                            Tab(
                                selected = selectedTafsirTab == idx,
                                onClick = { selectedTafsirTab = idx },
                                text = { Text(shortTitle, style = MaterialTheme.typography.labelLarge) }
                            )
                        }
                    }

                    val activeSource = tafsirSources[selectedTafsirTab]
                    val tafsirText = studyBundle?.tafsirs?.get(activeSource.id)

                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "${activeSource.titleArabic} — ${activeSource.authorArabic}",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            if (!tafsirText.isNullOrBlank()) {
                                Text(
                                    text = tafsirText,
                                    style = MaterialTheme.typography.bodyMedium,
                                    lineHeight = 24.sp
                                )
                            } else {
                                Text(
                                    text = "نص ${activeSource.titleArabic} غير محمّل لهذه السورة بعد. اتصل بالإنترنت واضغط تحديث أو نزّل الحزمة كاملة من إدارة التخزين.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // 4. Asbab al-Nuzul (only if verified)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "سبب النزول",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    val sabab = studyBundle?.sababNuzul
                    if (!sabab.isNullOrBlank()) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = wirdColors.goldContainer.copy(alpha = 0.35f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = sabab,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    } else {
                        Text(
                            text = "لا يتوفر سبب نزول صريح مثبت لهذه الآية في الحزم الموثقة المحملة (وليس لكل آية في القرآن سبب نزول خاص).",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            HorizontalDivider()

            // 10. Bookmarks & Separate Personal Reflection Notes
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "المفضلة والملاحظات الشخصية (منفصلة تمامًا عن النص القرآني)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                if (folders.isNotEmpty()) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        folders.forEach { folder ->
                            FilterChip(
                                selected = selectedFolderId == folder.id,
                                onClick = { selectedFolderId = folder.id },
                                label = { Text(folder.name, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = noteDraft,
                    onValueChange = { noteDraft = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("ملاحظة شخصية أو فائدة تدبرية") },
                    minLines = 2,
                    maxLines = 4,
                    shape = RoundedCornerShape(12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            onSaveBookmark(ayah, selectedFolderId, noteDraft)
                            Toast.makeText(context, "تم الحفظ في المفضلة", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("sheet_save_bookmark_button")
                    ) {
                        Icon(
                            imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(if (isBookmarked) "تحديث المفضلة والملاحظة" else "حفظ في المفضلة")
                    }

                    if (isBookmarked) {
                        OutlinedButton(
                            onClick = {
                                onRemoveBookmark(ayah)
                                Toast.makeText(context, "تمت الإزالة من المفضلة", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Text("حذف")
                        }
                    }
                }
            }
        }
    }
}
