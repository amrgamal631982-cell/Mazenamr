package com.example.ui.qibla

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.AppContainer
import com.example.data.local.SavedLocation
import com.example.qibla.CompassAccuracyStatus
import com.example.qibla.CompassReading
import com.example.qibla.QiblaCalculator
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class QiblaUiState(
    val hasLocation: Boolean = false,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val locationLabel: String = "",
    val isFetchingLocation: Boolean = false,
    val locationError: String? = null,
    val qiblaBearingDeg: Double? = null,
    val distanceToKaabaKm: Double? = null,
    val compassReading: CompassReading = CompassReading(
        magneticAzimuthDeg = 0f,
        trueNorthAzimuthDeg = 0f,
        accuracyStatus = CompassAccuracyStatus.MEDIUM,
        sensorsAvailable = true
    )
) {
    val relativeQiblaAngleDeg: Float?
        get() = qiblaBearingDeg?.let { qibla ->
            (((qibla.toFloat() - compassReading.trueNorthAzimuthDeg) % 360f) + 360f) % 360f
        }

    val isFacingQibla: Boolean
        get() {
            val rel = relativeQiblaAngleDeg ?: return false
            return rel <= 5f || rel >= 355f
        }
}

@OptIn(ExperimentalCoroutinesApi::class)
class QiblaViewModel(private val container: AppContainer) : ViewModel() {
    private val _isFetchingLocation = MutableStateFlow(false)
    private val _locationError = MutableStateFlow<String?>(null)

    private val compassFlow = container.preferences.savedLocation.flatMapLatest { loc ->
        container.compassSensorManager.observeAzimuth(loc.latitude, loc.longitude)
    }

    val uiState: StateFlow<QiblaUiState> = combine(
        container.preferences.savedLocation,
        compassFlow,
        _isFetchingLocation,
        _locationError
    ) { saved: SavedLocation, compass: CompassReading, fetching: Boolean, err: String? ->
        val lat = saved.latitude
        val lng = saved.longitude
        val hasLoc = lat != null && lng != null
        val bearing = if (hasLoc) QiblaCalculator.calculateQiblaBearing(lat!!, lng!!) else null
        val distance = if (hasLoc) QiblaCalculator.distanceToKaabaKm(lat!!, lng!!) else null

        QiblaUiState(
            hasLocation = hasLoc,
            latitude = lat,
            longitude = lng,
            locationLabel = saved.label,
            isFetchingLocation = fetching,
            locationError = err,
            qiblaBearingDeg = bearing,
            distanceToKaabaKm = distance,
            compassReading = compass
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = QiblaUiState()
    )

    @SuppressLint("MissingPermission")
    fun requestCurrentLocation(context: Context) {
        _isFetchingLocation.value = true
        _locationError.value = null

        val fused = LocationServices.getFusedLocationProviderClient(context)
        val cts = CancellationTokenSource()
        fused.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
            .addOnSuccessListener { loc: Location? ->
                if (loc != null) {
                    saveLocation(loc)
                } else {
                    fallbackLastKnownLocation(context)
                }
            }
            .addOnFailureListener {
                fallbackLastKnownLocation(context)
            }
    }

    @SuppressLint("MissingPermission")
    private fun fallbackLastKnownLocation(context: Context) {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val best = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
            .mapNotNull { runCatching { lm.getLastKnownLocation(it) }.getOrNull() }
            .maxByOrNull { it.time }

        if (best != null) {
            saveLocation(best)
        } else {
            _isFetchingLocation.value = false
            _locationError.value = "تعذر تحديد موقعك عبر GPS حاليًا. تأكد من تفعيل خدمة الموقع في الهاتف."
        }
    }

    private fun saveLocation(loc: Location) {
        viewModelScope.launch {
            val label = "خط العرض ${"%.4f".format(loc.latitude)}° — خط الطول ${"%.4f".format(loc.longitude)}°"
            container.preferences.saveQiblaLocation(loc.latitude, loc.longitude, label)
            _isFetchingLocation.value = false
            _locationError.value = null
        }
    }

    class Factory(private val container: AppContainer) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = QiblaViewModel(container) as T
    }
}
