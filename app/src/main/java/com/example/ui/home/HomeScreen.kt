package com.example.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Brightness7
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.adhkar.AdhkarCategory
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.DeepEmerald
import com.example.ui.theme.DeepEmeraldDark
import com.example.ui.theme.LocalWirdColors

@Composable
fun HomeScreen(
    uiState: HomeUiState,
    themeMode: AppThemeMode,
    onToggleTheme: () -> Unit,
    onContinueReading: (surahIndex: Int, ayahNumber: Int, pageNumber: Int) -> Unit,
    onOpenSurah: (surahIndex: Int) -> Unit,
    onOpenAdhkar: (AdhkarCategory) -> Unit,
    onOpenQibla: () -> Unit,
    onOpenTasbih: () -> Unit,
    onOpenDuas: () -> Unit,
    onOpenWirdTracker: () -> Unit,
    onOpenSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    val wirdColors = LocalWirdColors.current

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 640.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Serene Hero Banner: Islamic Greeting + Last Reading Position + Continue Button
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = DeepEmerald),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Image(
                            painter = painterResource(id = R.drawable.img_hero_arabesque_1795180108477),
                            contentDescription = "زخرفة إسلامية هادئة",
                            modifier = Modifier
                                .matchParentSize()
                                .alpha(0.24f),
                            contentScale = ContentScale.Crop
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            DeepEmerald.copy(alpha = 0.72f),
                                            DeepEmeraldDark.copy(alpha = 0.92f)
                                        )
                                    )
                                )
                        )

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "وِرد | WIRD",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = Color(0xFFE5C878)
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = uiState.greeting,
                                        style = MaterialTheme.typography.headlineSmall,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = uiState.timeSubtitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFFD8EBE3)
                                    )
                                }
                                IconButton(
                                    onClick = onToggleTheme,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.12f))
                                        .testTag("theme_toggle_button")
                                ) {
                                    Icon(
                                        imageVector = if (themeMode == AppThemeMode.DARK) {
                                            Icons.Default.Brightness7
                                        } else {
                                            Icons.Default.Brightness4
                                        },
                                        contentDescription = "تبديل الوضع الليلي والنهاري",
                                        tint = Color(0xFFF3E7C9)
                                    )
                                }
                            }

                            // Last reading position inner panel
                            Surface(
                                color = Color.White.copy(alpha = 0.10f),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "آخر موضع قراءة",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = Color(0xFFE5C878)
                                        )
                                        Spacer(Modifier.height(2.dp))
                                        Text(
                                            text = "سورة ${uiState.lastSurahName} — آية ${uiState.readingPosition.ayahNumber}",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = Color.White,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "الصفحة ${uiState.readingPosition.pageNumber} من 604",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFFD2E6DD)
                                        )
                                    }

                                    Spacer(Modifier.width(12.dp))

                                    Button(
                                        onClick = {
                                            onContinueReading(
                                                uiState.readingPosition.surahIndex,
                                                uiState.readingPosition.ayahNumber,
                                                uiState.readingPosition.pageNumber
                                            )
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFFD4AF37),
                                            contentColor = Color(0xFF1A1504)
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                                        modifier = Modifier.testTag("continue_reading_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text(
                                            text = "متابعة القراءة",
                                            style = MaterialTheme.typography.labelLarge
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 2. Daily Wird & Khatmah Statistics Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Daily Wird Card
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable(onClick = onOpenWirdTracker)
                            .testTag("home_wird_card"),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, wirdColors.subtleBorder)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "الورد اليومي",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Box(contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(
                                        progress = { uiState.wirdProgressFraction },
                                        modifier = Modifier.size(40.dp),
                                        color = MaterialTheme.colorScheme.primary,
                                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                        strokeWidth = 4.dp
                                    )
                                    Text(
                                        text = "${(uiState.wirdProgressFraction * 100).toInt()}%",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Text(
                                text = "${uiState.todayPagesRead} / ${uiState.wirdPlan.dailyTargetPages} صفحة",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "اضغط لمتابعة الورد",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Khatmah Stats Card
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable(onClick = onOpenWirdTracker)
                            .testTag("home_khatmah_card"),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, wirdColors.subtleBorder)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "إحصائية الختمة",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Text(
                                text = "${(uiState.khatmahProgressFraction * 100).toInt()}% من المصحف",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            LinearProgressIndicator(
                                progress = { uiState.khatmahProgressFraction },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(7.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = wirdColors.goldAccent,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                            Text(
                                text = "خطة ${uiState.wirdPlan.khatmahDays} يومًا • صفحة ${uiState.readingPosition.pageNumber}/604",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // 3. Quick Access Row: Qibla, Tasbih, Duas, Search
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "الوصول السريع",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        QuickActionTile(
                            title = "القبلة",
                            icon = Icons.Default.Explore,
                            onClick = onOpenQibla,
                            modifier = Modifier.weight(1f).testTag("quick_qibla_button")
                        )
                        QuickActionTile(
                            title = "السبحة",
                            icon = Icons.Default.RadioButtonChecked,
                            onClick = onOpenTasbih,
                            modifier = Modifier.weight(1f).testTag("quick_tasbih_button")
                        )
                        QuickActionTile(
                            title = "الأدعية",
                            icon = Icons.Default.VolunteerActivism,
                            onClick = onOpenDuas,
                            modifier = Modifier.weight(1f).testTag("quick_duas_button")
                        )
                        QuickActionTile(
                            title = "البحث",
                            icon = Icons.Default.Search,
                            onClick = onOpenSearch,
                            modifier = Modifier.weight(1f).testTag("quick_search_button")
                        )
                    }
                }
            }

            // 4. Surah of the Day
            uiState.surahOfTheDay?.let { surah ->
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenSurah(surah.index) }
                            .testTag("surah_of_day_card"),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, wirdColors.goldAccent.copy(alpha = 0.45f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = wirdColors.goldContainer,
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.MenuBook,
                                            contentDescription = null,
                                            tint = wirdColors.onGoldContainer
                                        )
                                    }
                                }
                                Column {
                                    Text(
                                        text = "سورة اليوم",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = wirdColors.goldAccent
                                    )
                                    Text(
                                        text = "سورة ${surah.nameArabic}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${surah.revelationTypeArabic} • ${surah.ayahCount} آية • صفحة ${surah.startPage}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Text(
                                text = "اقرأ الآن",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // 5. Morning / Evening Adhkar Contextual Card
            item {
                val isMorning = uiState.suggestedAdhkarCategory == AdhkarCategory.MORNING
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenAdhkar(uiState.suggestedAdhkarCategory) }
                        .testTag("home_adhkar_card"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(46.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (isMorning) Icons.Default.WbSunny else Icons.Default.NightsStay,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSecondary
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = uiState.suggestedAdhkarCategory.titleArabic,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${uiState.suggestedAdhkarCount} ذكرًا موثقًا من حصن المسلم",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(12.dp)) }
        }
    }
}

@Composable
private fun QuickActionTile(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val wirdColors = LocalWirdColors.current
    Surface(
        modifier = modifier
            .height(84.dp)
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, wirdColors.subtleBorder, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1
            )
        }
    }
}
