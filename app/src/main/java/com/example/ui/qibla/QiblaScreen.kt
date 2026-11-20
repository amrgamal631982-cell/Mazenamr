package com.example.ui.qibla

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.SensorsOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.ui.theme.LocalWirdColors
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QiblaScreen(
    uiState: QiblaUiState,
    onRequestLocation: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val wirdColors = LocalWirdColors.current
    var permissionDenied by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val granted = result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            result[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        permissionDenied = !granted
        if (granted) onRequestLocation()
    }

    fun triggerLocationFetch() {
        val hasFine = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (hasFine || hasCoarse) {
            permissionDenied = false
            onRequestLocation()
        } else {
            permissionLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            )
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("اتجاه القبلة", fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = onBack, modifier = Modifier.testTag("qibla_back_button")) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                }
            }
        )

        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 600.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Calibration / Sensor warning banner
                if (!uiState.compassReading.sensorsAvailable || uiState.compassReading.accuracyStatus.needsCalibration) {
                    Surface(
                        color = if (!uiState.compassReading.sensorsAvailable) {
                            MaterialTheme.colorScheme.errorContainer
                        } else {
                            wirdColors.warningBg
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = if (!uiState.compassReading.sensorsAvailable) {
                                    Icons.Default.SensorsOff
                                } else {
                                    Icons.Default.Warning
                                },
                                contentDescription = null,
                                tint = if (!uiState.compassReading.sensorsAvailable) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    wirdColors.warningColor
                                }
                            )
                            Text(
                                text = uiState.compassReading.accuracyStatus.labelArabic,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (!uiState.compassReading.sensorsAvailable) {
                                    MaterialTheme.colorScheme.onErrorContainer
                                } else {
                                    wirdColors.warningColor
                                }
                            )
                        }
                    }
                }

                // Only render live rotating Qibla compass when BOTH real location AND sensors are available
                if (uiState.hasLocation && uiState.compassReading.sensorsAvailable) {
                    val needleAngle = uiState.relativeQiblaAngleDeg ?: 0f
                    val animatedNeedle by animateFloatAsState(
                        targetValue = needleAngle,
                        animationSpec = spring(stiffness = 220f),
                        label = "qibla_needle"
                    )
                    val primaryColor = MaterialTheme.colorScheme.primary
                    val goldColor = wirdColors.goldAccent
                    val outlineColor = MaterialTheme.colorScheme.outline
                    val activeColor = if (uiState.isFacingQibla) wirdColors.verifiedColor else goldColor

                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(
                            2.dp,
                            if (uiState.isFacingQibla) wirdColors.verifiedColor else wirdColors.subtleBorder
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Text(
                                text = if (uiState.isFacingQibla) {
                                    "أنت متجه الآن نحو القبلة الشريفة"
                                } else {
                                    "أدر الهاتف حتى تتطابق الإبرة مع رمز الكعبة"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (uiState.isFacingQibla) wirdColors.verifiedColor else MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            )

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.75f)
                                    .aspectRatio(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val radius = size.minDimension / 2f * 0.88f
                                    val center = Offset(size.width / 2f, size.height / 2f)

                                    drawCircle(
                                        color = outlineColor.copy(alpha = 0.4f),
                                        radius = radius,
                                        center = center,
                                        style = Stroke(width = 4f)
                                    )
                                    drawCircle(
                                        color = primaryColor.copy(alpha = 0.08f),
                                        radius = radius * 0.85f,
                                        center = center
                                    )

                                    // Compass degree ticks rotated by device azimuth
                                    rotate(degrees = -uiState.compassReading.trueNorthAzimuthDeg, pivot = center) {
                                        for (deg in 0 until 360 step 15) {
                                            val rad = Math.toRadians(deg.toDouble())
                                            val startR = if (deg % 45 == 0) radius * 0.82f else radius * 0.90f
                                            val p1 = Offset(
                                                center.x + (startR * sin(rad)).toFloat(),
                                                center.y - (startR * cos(rad)).toFloat()
                                            )
                                            val p2 = Offset(
                                                center.x + (radius * sin(rad)).toFloat(),
                                                center.y - (radius * cos(rad)).toFloat()
                                            )
                                            drawLine(
                                                color = if (deg == 0) Color(0xFFD32F2F) else outlineColor,
                                                start = p1,
                                                end = p2,
                                                strokeWidth = if (deg % 45 == 0) 5f else 2f,
                                                cap = StrokeCap.Round
                                            )
                                        }
                                    }

                                    // Qibla Needle pointing toward Kaaba
                                    rotate(degrees = animatedNeedle, pivot = center) {
                                        val tip = Offset(center.x, center.y - radius * 0.78f)
                                        val left = Offset(center.x - 18f, center.y)
                                        val right = Offset(center.x + 18f, center.y)
                                        val path = Path().apply {
                                            moveTo(tip.x, tip.y)
                                            lineTo(right.x, right.y)
                                            lineTo(center.x, center.y + radius * 0.25f)
                                            lineTo(left.x, left.y)
                                            close()
                                        }
                                        drawPath(path = path, color = activeColor)
                                        drawCircle(color = primaryColor, radius = 14f, center = center)
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("زاوية القبلة", style = MaterialTheme.typography.labelSmall)
                                    Text(
                                        text = "${uiState.qiblaBearingDeg?.toInt() ?: 0}°",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("المسافة إلى الكعبة", style = MaterialTheme.typography.labelSmall)
                                    Text(
                                        text = "${uiState.distanceToKaabaKm?.toInt() ?: 0} كم",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // Honest unavailable state — never shows a fake direction!
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, wirdColors.subtleBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(44.dp)
                            )
                            Text(
                                text = when {
                                    !uiState.compassReading.sensorsAvailable ->
                                        "لا تتوفر مستشعرات البوصلة على هذا الجهاز"
                                    permissionDenied ->
                                        "تم رفض صلاحية الموقع. لا يمكن حساب اتجاه القبلة دون إحداثيات موقعك."
                                    else ->
                                        "حدّد موقعك الجغرافي لحساب اتجاه القبلة الدقيق"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "بعد تحديد الإحداثيات مرة واحدة عبر GPS، يعمل حساب القبلة دون إنترنت.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            if (uiState.hasLocation && !uiState.compassReading.sensorsAvailable) {
                                Text(
                                    text = "زاوية القبلة المحسوبة من الشمال الحقيقي: ${uiState.qiblaBearingDeg?.toInt()}° (المسافة ${uiState.distanceToKaabaKm?.toInt()} كم)",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                // Location Status + Refresh GPS Button
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, wirdColors.subtleBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("إحداثيات الموقع", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(
                            text = if (uiState.hasLocation) uiState.locationLabel else "لم يتم حفظ إحداثيات موقع بعد",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        uiState.locationError?.let { err ->
                            Text(err, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                        }
                        Button(
                            onClick = { triggerLocationFetch() },
                            enabled = !uiState.isFetchingLocation,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("qibla_locate_button"),
                            contentPadding = PaddingValues(vertical = 12.dp)
                        ) {
                            if (uiState.isFetchingLocation) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                Spacer(Modifier.width(8.dp))
                                Text("جاري تحديد الموقع عبر GPS...")
                            } else {
                                Icon(Icons.Default.MyLocation, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text(if (uiState.hasLocation) "تحديث الموقع عبر GPS" else "تحديد موقعي الآن (GPS)")
                            }
                        }
                    }
                }
            }
        }
    }
}
