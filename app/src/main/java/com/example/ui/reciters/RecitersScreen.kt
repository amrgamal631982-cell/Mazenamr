package com.example.ui.reciters

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.ListeningPosition
import com.example.data.quran.SurahMeta
import com.example.data.reciters.ReciterCatalog
import com.example.data.reciters.ReciterInfo
import com.example.data.reciters.ReciterRegion
import com.example.ui.theme.LocalWirdColors

@Composable
fun RecitersScreen(
    reciters: List<ReciterInfo>,
    defaultReciterId: String,
    listeningPosition: ListeningPosition,
    surahs: List<SurahMeta>,
    onOpenReciter: (String) -> Unit,
    onResumeListening: (reciterId: String, surahIndex: Int, ayahNumber: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val wirdColors = LocalWirdColors.current
    val egyptian = reciters.filter { it.region == ReciterRegion.EGYPT }
    val others = reciters.filter { it.region == ReciterRegion.OTHER }
    val lastReciter = ReciterCatalog.findById(listeningPosition.reciterId)
    val lastSurahName = surahs.firstOrNull { it.index == listeningPosition.surahIndex }?.nameArabic ?: "الفاتحة"

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 640.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "التلاوات القرآنية",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "تسجيلات صوتية أصلية موثقة المصدر مع التحقق الفعلي من التوفر والمزامنة",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Resume Last Listening Position Card
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "آخر موضع استماع",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "سورة $lastSurahName — آية ${listeningPosition.ayahNumber}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "بصوت الشيخ ${lastReciter.nameArabic}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Button(
                            onClick = {
                                onResumeListening(
                                    listeningPosition.reciterId,
                                    listeningPosition.surahIndex,
                                    listeningPosition.ayahNumber
                                )
                            },
                            modifier = Modifier.testTag("resume_listening_button")
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("استكمال")
                        }
                    }
                }
            }

            // Egyptian Reciters
            item {
                Text(
                    text = ReciterRegion.EGYPT.labelArabic,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            items(egyptian, key = { it.id }) { reciter ->
                ReciterCard(
                    reciter = reciter,
                    isDefault = reciter.id == defaultReciterId,
                    subtleBorder = wirdColors.subtleBorder,
                    goldAccent = wirdColors.goldAccent,
                    onClick = { onOpenReciter(reciter.id) }
                )
            }

            // Other Reciters
            item {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = ReciterRegion.OTHER.labelArabic,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            items(others, key = { it.id }) { reciter ->
                ReciterCard(
                    reciter = reciter,
                    isDefault = reciter.id == defaultReciterId,
                    subtleBorder = wirdColors.subtleBorder,
                    goldAccent = wirdColors.goldAccent,
                    onClick = { onOpenReciter(reciter.id) }
                )
            }
        }
    }
}

@Composable
private fun ReciterCard(
    reciter: ReciterInfo,
    isDefault: Boolean,
    subtleBorder: androidx.compose.ui.graphics.Color,
    goldAccent: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("reciter_card_${reciter.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, if (isDefault) goldAccent else subtleBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(50.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "الشيخ ${reciter.nameArabic}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (isDefault) {
                        Surface(
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Icon(
                                    Icons.Default.Star,
                                    contentDescription = "افتراضي",
                                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = "الافتراضي",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                        }
                    }
                }

                Text(
                    text = "${reciter.styleArabic} • ${reciter.riwayahArabic}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = reciter.bioShort,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "المصدر: ${reciter.sourceLabel}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
