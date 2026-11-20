package com.example.ui.tools

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.data.local.ReadingPosition
import com.example.data.local.WirdLogEntity
import com.example.data.local.WirdPlanConfig
import com.example.ui.theme.LocalWirdColors
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun WirdTrackerScreen(
    plan: WirdPlanConfig,
    logs: List<WirdLogEntity>,
    readingPosition: ReadingPosition,
    onUpdatePlan: (dailyPages: Int, khatmahDays: Int, reminderEnabled: Boolean, hour: Int, minute: Int) -> Unit,
    onLogPages: (Int) -> Unit,
    onContinueReading: (surahIndex: Int, ayahNumber: Int, pageNumber: Int) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val wirdColors = LocalWirdColors.current
    val todayIso = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    val todayLog = logs.firstOrNull { it.dateIso == todayIso }
    val todayPages = todayLog?.pagesRead ?: 0
    val progress = (todayPages.toFloat() / plan.dailyTargetPages.coerceAtLeast(1)).coerceIn(0f, 1f)

    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            onUpdatePlan(plan.dailyTargetPages, plan.khatmahDays, true, plan.reminderHour, plan.reminderMinute)
        }
    }

    fun toggleReminder(enabled: Boolean) {
        if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                return
            }
        }
        onUpdatePlan(plan.dailyTargetPages, plan.khatmahDays, enabled, plan.reminderHour, plan.reminderMinute)
    }

    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("الورد اليومي وختم القرآن", fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = onBack, modifier = Modifier.testTag("wird_back_button")) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                }
            }
        )

        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 600.dp),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Today's Progress Card
                item {
                    Card(
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, wirdColors.subtleBorder)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("إنجاز ورد اليوم", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text(
                                        text = "$todayPages من ${plan.dailyTargetPages} صفحة",
                                        style = MaterialTheme.typography.headlineSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "الموضع الحالي: صفحة ${readingPosition.pageNumber} من 604",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Box(contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(
                                        progress = { progress },
                                        modifier = Modifier.size(68.dp),
                                        strokeWidth = 6.dp
                                    )
                                    Text(
                                        text = "${(progress * 100).toInt()}%",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { onLogPages(1) },
                                    modifier = Modifier.weight(1f).testTag("log_one_page_button")
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("+1 صفحة")
                                }
                                OutlinedButton(
                                    onClick = { onLogPages(5) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("+5 صفحات")
                                }
                                OutlinedButton(
                                    onClick = {
                                        onContinueReading(
                                            readingPosition.surahIndex,
                                            readingPosition.ayahNumber,
                                            readingPosition.pageNumber
                                        )
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(2.dp))
                                    Text("اقرأ")
                                }
                            }
                        }
                    }
                }

                // Plan Configuration (Khatmah Days, Daily Pages / Juz)
                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, wirdColors.subtleBorder)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text("تخصيص خطة الختم", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("اختر مدة الختم (يتم حساب الصفحات يوميًا تلقائيًا من 604 صفحة):", style = MaterialTheme.typography.bodySmall)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(15 to "15 يومًا (جزءان)", 30 to "30 يومًا (جزء)", 60 to "60 يومًا (نصف جزء)", 90 to "90 يومًا").forEach { (days, label) ->
                                    FilterChip(
                                        selected = plan.khatmahDays == days,
                                        onClick = {
                                            val pages = kotlin.math.ceil(604.0 / days).toInt()
                                            onUpdatePlan(pages, days, plan.reminderEnabled, plan.reminderHour, plan.reminderMinute)
                                        },
                                        label = { Text(label) }
                                    )
                                }
                            }

                            Text("أو حدد عدد الصفحات يوميًا مباشرة:", style = MaterialTheme.typography.bodySmall)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(2, 4, 10, 20, 40).forEach { pages ->
                                    FilterChip(
                                        selected = plan.dailyTargetPages == pages,
                                        onClick = {
                                            val days = kotlin.math.ceil(604.0 / pages).toInt()
                                            onUpdatePlan(pages, days, plan.reminderEnabled, plan.reminderHour, plan.reminderMinute)
                                        },
                                        label = { Text("$pages ص") }
                                    )
                                }
                            }
                        }
                    }
                }

                // 14-Day Tracking Calendar Grid
                item {
                    val logsMap = logs.associateBy { it.dateIso }
                    val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                    val dayFmt = SimpleDateFormat("d", Locale.getDefault())
                    val recentDays = (13 downTo 0).map { offset ->
                        val c = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -offset) }
                        Triple(fmt.format(c.time), dayFmt.format(c.time), offset == 0)
                    }

                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, wirdColors.subtleBorder)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("تقويم المتابعة (آخر 14 يومًا)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                recentDays.forEach { (iso, dayLabel, isToday) ->
                                    val entry = logsMap[iso]
                                    val completed = entry?.completed == true
                                    val partial = (entry?.pagesRead ?: 0) > 0 && !completed
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    when {
                                                        completed -> wirdColors.verifiedColor
                                                        partial -> wirdColors.goldAccent.copy(alpha = 0.7f)
                                                        isToday -> MaterialTheme.colorScheme.primaryContainer
                                                        else -> MaterialTheme.colorScheme.surfaceVariant
                                                    }
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (completed) {
                                                Icon(Icons.Default.Check, contentDescription = "مكتمل", tint = androidx.compose.ui.graphics.Color.White, modifier = Modifier.size(18.dp))
                                            } else {
                                                Text(dayLabel, style = MaterialTheme.typography.labelMedium)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Daily Reminder Switch
                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, wirdColors.subtleBorder)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Column {
                                    Text("تذكير يومي بالورد", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    Text(
                                        text = "إشعار يومي الساعة ${plan.reminderHour.toString().padStart(2, '0')}:${plan.reminderMinute.toString().padStart(2, '0')} صباحًا",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Switch(
                                checked = plan.reminderEnabled,
                                onCheckedChange = { toggleReminder(it) },
                                modifier = Modifier.testTag("wird_reminder_switch")
                            )
                        }
                    }
                }
            }
        }
    }
}
