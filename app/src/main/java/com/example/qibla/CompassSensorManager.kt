package com.example.qibla

import android.content.Context
import android.hardware.GeomagneticField
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

enum class CompassAccuracyStatus(val labelArabic: String, val needsCalibration: Boolean) {
    HIGH("دقة البوصلة عالية", false),
    MEDIUM("دقة البوصلة متوسطة", false),
    LOW("دقة البوصلة منخفضة — حرّك الهاتف على شكل رقم 8 للمعايرة", true),
    UNRELIABLE("البوصلة غير معايرة — حرّك الهاتف على شكل رقم 8 بعيداً عن المعادن", true),
    UNAVAILABLE("مستشعر البوصلة المغناطيسية غير متوفر في هذا الجهاز", true)
}

data class CompassReading(
    val magneticAzimuthDeg: Float,
    val trueNorthAzimuthDeg: Float,
    val accuracyStatus: CompassAccuracyStatus,
    val sensorsAvailable: Boolean
)

class CompassSensorManager(context: Context) {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    fun observeAzimuth(latitude: Double?, longitude: Double?): Flow<CompassReading> = callbackFlow {
        val rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        val accelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val magSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        if (rotationSensor == null && (accelSensor == null || magSensor == null)) {
            trySend(
                CompassReading(
                    magneticAzimuthDeg = 0f,
                    trueNorthAzimuthDeg = 0f,
                    accuracyStatus = CompassAccuracyStatus.UNAVAILABLE,
                    sensorsAvailable = false
                )
            )
            awaitClose { }
            return@callbackFlow
        }

        val declination = if (latitude != null && longitude != null) {
            GeomagneticField(
                latitude.toFloat(),
                longitude.toFloat(),
                0f,
                System.currentTimeMillis()
            ).declination
        } else {
            0f
        }

        var currentAccuracy = CompassAccuracyStatus.MEDIUM
        val gravity = FloatArray(3)
        val geomagnetic = FloatArray(3)
        var hasGravity = false
        var hasMagnetic = false
        var smoothedRad: Double? = null

        val listener = object : SensorEventListener {
            override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
                currentAccuracy = when (accuracy) {
                    SensorManager.SENSOR_STATUS_ACCURACY_HIGH -> CompassAccuracyStatus.HIGH
                    SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM -> CompassAccuracyStatus.MEDIUM
                    SensorManager.SENSOR_STATUS_ACCURACY_LOW -> CompassAccuracyStatus.LOW
                    else -> CompassAccuracyStatus.UNRELIABLE
                }
            }

            override fun onSensorChanged(event: SensorEvent) {
                val rotationMatrix = FloatArray(9)
                val orientation = FloatArray(3)

                when (event.sensor.type) {
                    Sensor.TYPE_ROTATION_VECTOR -> {
                        SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                        SensorManager.getOrientation(rotationMatrix, orientation)
                        emitAngle(orientation[0].toDouble())
                    }
                    Sensor.TYPE_ACCELEROMETER -> {
                        System.arraycopy(event.values, 0, gravity, 0, 3)
                        hasGravity = true
                        if (hasMagnetic && SensorManager.getRotationMatrix(rotationMatrix, null, gravity, geomagnetic)) {
                            SensorManager.getOrientation(rotationMatrix, orientation)
                            emitAngle(orientation[0].toDouble())
                        }
                    }
                    Sensor.TYPE_MAGNETIC_FIELD -> {
                        System.arraycopy(event.values, 0, geomagnetic, 0, 3)
                        hasMagnetic = true
                        if (hasGravity && SensorManager.getRotationMatrix(rotationMatrix, null, gravity, geomagnetic)) {
                            SensorManager.getOrientation(rotationMatrix, orientation)
                            emitAngle(orientation[0].toDouble())
                        }
                    }
                }
            }

            private fun emitAngle(rawRad: Double) {
                val prev = smoothedRad
                val next = if (prev == null) {
                    rawRad
                } else {
                    // Circular exponential smoothing to eliminate jitter across 0/360 boundary
                    val alpha = 0.18
                    val s = (1 - alpha) * sin(prev) + alpha * sin(rawRad)
                    val c = (1 - alpha) * cos(prev) + alpha * cos(rawRad)
                    atan2(s, c)
                }
                smoothedRad = next
                val magDeg = (((Math.toDegrees(next) % 360.0) + 360.0) % 360.0).toFloat()
                val trueDeg = (((magDeg + declination) % 360f) + 360f) % 360f
                trySend(
                    CompassReading(
                        magneticAzimuthDeg = magDeg,
                        trueNorthAzimuthDeg = trueDeg,
                        accuracyStatus = currentAccuracy,
                        sensorsAvailable = true
                    )
                )
            }
        }

        if (rotationSensor != null) {
            sensorManager.registerListener(listener, rotationSensor, SensorManager.SENSOR_DELAY_UI)
        } else {
            sensorManager.registerListener(listener, accelSensor, SensorManager.SENSOR_DELAY_UI)
            sensorManager.registerListener(listener, magSensor, SensorManager.SENSOR_DELAY_UI)
        }

        awaitClose {
            sensorManager.unregisterListener(listener)
        }
    }
}
