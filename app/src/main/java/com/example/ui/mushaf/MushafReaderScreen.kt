package com.example.ui.mushaf

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.TextDecrease
import androidx.compose.material.icons.filled.TextIncrease
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.BookmarkEntity
import com.example.data.quran.Ayah
import com.example.ui.theme.AmiriQuranFontFamily
import com.example.ui.theme.LocalWirdColors

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MushafReaderScreen(
    uiState: MushafReaderUiState,
    fontSizeSp: Float,
    bookmarks: List<BookmarkEntity>,
    activePlayingSurah: Int?,
    activePlayingAyah: Int?,
    isAudioPlaying: Boolean,
    onBack: () -> Unit,
    onSwitchMode: (ReaderViewMode) -> Unit,
    onPrevPageOrSurah: () -> Unit,
    onNextPageOrSurah: () -> Unit,
    onFontSizeChange: (Float) -> Unit,
    onPlaySurah: (Int) -> Unit,
    onAyahLongPress: (Ayah) -> Unit,
    modifier: Modifier = Modifier
) {
    val wirdColors = LocalWirdColors.current
    val bookmarkedSet = bookmarks.map { it.surahIndex to it.ayahNumber }.toSet()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(wirdColors.mushafPaper)
    ) {
        TopAppBar(
            title = {
                Column {
                    Text(
                        text = uiState.currentSurah?.let { "سورة ${it.nameArabic}" } ?: "المصحف الشريف",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    val firstAyah = uiState.ayahs.firstOrNull()
                    if (firstAyah != null) {
                        Text(
                            text = "صفحة ${uiState.currentPage} • الجزء ${firstAyah.juzNumber} • الحزب ${firstAyah.hizbNumber}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            navigationIcon = {
                IconButton(onClick = onBack, modifier = Modifier.testTag("mushaf_back_button")) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                }
            },
            actions = {
                IconButton(
                    onClick = { onFontSizeChange(fontSizeSp - 2f) },
                    modifier = Modifier.testTag("font_decrease_button")
                ) {
                    Icon(Icons.Default.TextDecrease, contentDescription = "تصغير الخط")
                }
                IconButton(
                    onClick = { onFontSizeChange(fontSizeSp + 2f) },
                    modifier = Modifier.testTag("font_increase_button")
                ) {
                    Icon(Icons.Default.TextIncrease, contentDescription = "تكبير الخط")
                }
                uiState.currentSurah?.let { s ->
                    IconButton(
                        onClick = { onPlaySurah(s.index) },
                        modifier = Modifier.testTag("play_surah_button")
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "تشغيل السورة")
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        )

        // View mode switch & quick page/surah navigator
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ReaderViewMode.entries.forEach { mode ->
                    FilterChip(
                        selected = uiState.viewMode == mode,
                        onClick = { onSwitchMode(mode) },
                        label = { Text(mode.labelArabic, style = MaterialTheme.typography.labelMedium) }
                    )
                }
            }
            Text(
                text = "اضغط مطولًا على أي آية للتفسير والاستماع",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (uiState.isLoading) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.TopCenter
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 680.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(uiState.ayahs, key = { it.id }) { ayah ->
                        val surahMeta = uiState.allSurahs[ayah.surahIndex]
                        val isFirstAyahOfSurah = ayah.ayahNumber == 1
                        val isPlayingThisAyah = isAudioPlaying &&
                            activePlayingSurah == ayah.surahIndex &&
                            activePlayingAyah == ayah.ayahNumber
                        val isBookmarked = bookmarkedSet.contains(ayah.surahIndex to ayah.ayahNumber)

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (isFirstAyahOfSurah && surahMeta != null) {
                                // Ornamental Surah Banner
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                                    border = BorderStroke(1.2.dp, wirdColors.goldAccent)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 12.dp, horizontal = 16.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = "سُورَةُ ${surahMeta.nameArabic}",
                                            style = MaterialTheme.typography.headlineSmall.copy(
                                                fontFamily = AmiriQuranFontFamily
                                            ),
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                        Text(
                                            text = "${surahMeta.revelationTypeArabic} • آياتها ${surahMeta.ayahCount}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                // Bismillah line when present in Tanzil XML (all surahs except Fatiha ayah 1 itself & Tawbah 9)
                                ayah.bismillahPrefix?.let { bismillah ->
                                    Text(
                                        text = bismillah,
                                        style = MaterialTheme.typography.headlineSmall.copy(
                                            fontFamily = AmiriQuranFontFamily,
                                            fontSize = (fontSizeSp + 1).sp,
                                            lineHeight = (fontSizeSp * 1.9f).sp
                                        ),
                                        color = MaterialTheme.colorScheme.primary,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 6.dp)
                                    )
                                }
                            }

                            // Ayah Card (Verbatim Tanzil Uthmani Text)
                            val cardBg = if (isPlayingThisAyah) {
                                wirdColors.goldContainer.copy(alpha = 0.65f)
                            } else {
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                            }
                            val cardBorder = if (isPlayingThisAyah) {
                                wirdColors.goldAccent
                            } else {
                                wirdColors.subtleBorder.copy(alpha = 0.6f)
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(cardBg)
                                    .border(1.dp, cardBorder, RoundedCornerShape(14.dp))
                                    .combinedClickable(
                                        onClick = { onAyahLongPress(ayah) },
                                        onLongClick = { onAyahLongPress(ayah) }
                                    )
                                    .padding(horizontal = 16.dp, vertical = 14.dp)
                                    .testTag("ayah_${ayah.surahIndex}_${ayah.ayahNumber}")
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        text = "${ayah.uthmaniText} ﴿${toArabicDigits(ayah.ayahNumber)}﴾",
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            fontFamily = AmiriQuranFontFamily,
                                            fontSize = fontSizeSp.sp,
                                            lineHeight = (fontSizeSp * 1.95f).sp
                                        ),
                                        color = wirdColors.mushafInk,
                                        textAlign = TextAlign.Start,
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "آية ${ayah.ayahNumber} • ص ${ayah.pageNumber}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            if (ayah.sajdaType != null) {
                                                Surface(
                                                    color = wirdColors.goldContainer,
                                                    shape = RoundedCornerShape(6.dp)
                                                ) {
                                                    Text(
                                                        text = if (ayah.sajdaType == "obligatory") "سجدة واجبة ۩" else "سجدة تلاوة ۩",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = wirdColors.onGoldContainer,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }
                                        if (isBookmarked) {
                                            Icon(
                                                imageVector = Icons.Default.Bookmark,
                                                contentDescription = "محفوظة في المفضلة",
                                                tint = wirdColors.goldAccent,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Bottom Page/Surah Navigation Bar
        Surface(
            tonalElevation = 3.dp,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onPrevPageOrSurah,
                    modifier = Modifier.testTag("prev_page_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "السابق")
                }

                Text(
                    text = if (uiState.viewMode == ReaderViewMode.BY_PAGE) {
                        "الصفحة ${uiState.currentPage} من 604"
                    } else {
                        "سورة ${uiState.currentSurah?.nameArabic ?: ""} (${uiState.currentSurah?.index ?: 1}/114)"
                    },
                    style = MaterialTheme.typography.labelLarge
                )

                IconButton(
                    onClick = onNextPageOrSurah,
                    modifier = Modifier.testTag("next_page_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "التالي")
                }
            }
        }
    }
}

fun toArabicDigits(number: Int): String {
    val digits = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')
    return number.toString().map { c ->
        if (c in '0'..'9') digits[c - '0'] else c
    }.joinToString("")
}
