package com.example.ui.adhkar

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.adhkar.AdhkarCategory
import com.example.data.adhkar.AdhkarRepository
import com.example.data.adhkar.DhikrItem
import com.example.data.duas.DuaCategory
import com.example.data.duas.DuaOriginBadge
import com.example.ui.theme.AmiriFontFamily
import com.example.ui.theme.LocalWirdColors

@Composable
fun AdhkarDuasScreen(
    uiState: AdhkarDuasUiState,
    onSelectTab: (Int) -> Unit,
    onSelectAdhkarCategory: (AdhkarCategory) -> Unit,
    onSelectDuaCategory: (DuaCategory) -> Unit,
    onDecrementDhikr: (DhikrItem) -> Unit,
    onResetDhikr: (DhikrItem) -> Unit,
    onResetCategory: (AdhkarCategory) -> Unit,
    modifier: Modifier = Modifier
) {
    val wirdColors = LocalWirdColors.current

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 640.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = if (uiState.activeTab == 0) "الأذكار المأثورة" else "مكتبة الأدعية",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = uiState.activeTab == 0,
                        onClick = { onSelectTab(0) },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                        modifier = Modifier.testTag("tab_adhkar")
                    ) {
                        Text("الأذكار (6 أقسام)")
                    }
                    SegmentedButton(
                        selected = uiState.activeTab == 1,
                        onClick = { onSelectTab(1) },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                        modifier = Modifier.testTag("tab_duas")
                    ) {
                        Text("الأدعية (9 أقسام)")
                    }
                }
            }

            if (uiState.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (uiState.activeTab == 0) {
                // 6 Adhkar Categories
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(AdhkarCategory.entries) { cat ->
                        FilterChip(
                            selected = uiState.selectedAdhkarCategory == cat,
                            onClick = { onSelectAdhkarCategory(cat) },
                            label = { Text(cat.titleArabic) }
                        )
                    }
                }

                val activeCat = uiState.selectedAdhkarCategory
                val items = uiState.adhkarByCategory[activeCat].orEmpty()

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = activeCat.subtitleArabic,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { onResetCategory(activeCat) }) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("إعادة الكل")
                    }
                }

                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(items, key = { it.id }) { dhikr ->
                        val remaining = uiState.dhikrRemainingCounts[dhikr.id] ?: dhikr.repeatCount
                        val isDone = remaining <= 0
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onDecrementDhikr(dhikr) }
                                .testTag("dhikr_card_${dhikr.id}"),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isDone) {
                                    wirdColors.verifiedBg.copy(alpha = 0.55f)
                                } else {
                                    MaterialTheme.colorScheme.surface
                                }
                            ),
                            border = BorderStroke(
                                1.dp,
                                if (isDone) wirdColors.verifiedColor.copy(alpha = 0.45f) else wirdColors.subtleBorder
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = dhikr.chapterTitle,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = dhikr.arabicText,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontFamily = AmiriFontFamily,
                                        fontSize = 19.sp,
                                        lineHeight = 32.sp
                                    )
                                )
                                dhikr.explanation?.let { exp ->
                                    Surface(
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text(
                                            text = "الفضل والبيان: $exp",
                                            style = MaterialTheme.typography.bodySmall,
                                            modifier = Modifier.padding(10.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = dhikr.reference,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Button(
                                        onClick = { onDecrementDhikr(dhikr) },
                                        enabled = !isDone,
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        if (isDone) {
                                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(Modifier.width(6.dp))
                                            Text("تم الذكر (${dhikr.repeatCount})")
                                        } else {
                                            Text("تسبيح / عدّ — المتبقي: $remaining من ${dhikr.repeatCount}")
                                        }
                                    }
                                    IconButton(onClick = { onResetDhikr(dhikr) }) {
                                        Icon(Icons.Default.Refresh, contentDescription = "إعادة العداد")
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Text(
                            text = AdhkarRepository.HISN_SOURCE_ATTRIBUTION,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                }
            } else {
                // 9 Dua Categories
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(DuaCategory.entries) { cat ->
                        FilterChip(
                            selected = uiState.selectedDuaCategory == cat,
                            onClick = { onSelectDuaCategory(cat) },
                            label = { Text(cat.titleArabic) }
                        )
                    }
                }

                val duas = uiState.duasByCategory[uiState.selectedDuaCategory].orEmpty()
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(duas, key = { it.id }) { dua ->
                        val isQuran = dua.originBadge == DuaOriginBadge.QURAN
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(
                                1.dp,
                                if (isQuran) wirdColors.goldAccent.copy(alpha = 0.6f) else wirdColors.subtleBorder
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = dua.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Surface(
                                        color = if (isQuran) wirdColors.goldContainer else MaterialTheme.colorScheme.secondaryContainer,
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = dua.originBadge.label,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isQuran) wirdColors.onGoldContainer else MaterialTheme.colorScheme.onSecondaryContainer,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                Text(
                                    text = dua.arabicText,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontFamily = AmiriFontFamily,
                                        fontSize = 20.sp,
                                        lineHeight = 34.sp
                                    )
                                )

                                Text(
                                    text = "المصدر: ${dua.sourceCitation}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
