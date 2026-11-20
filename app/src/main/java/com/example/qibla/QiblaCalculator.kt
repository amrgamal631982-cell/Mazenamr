package com.example.qibla

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

object QiblaCalculator {
    // Verified coordinates of the Holy Kaaba, Masjid al-Haram, Makkah
    const val KAABA_LATITUDE = 21.422487
    const val KAABA_LONGITUDE = 39.826206
    private const val EARTH_RADIUS_KM = 6371.0

    /**
     * Computes Qibla azimuth in degrees clockwise from True North [0, 360).
     * Pure spherical trigonometry — works 100% offline.
     */
    fun calculateQiblaBearing(latitude: Double, longitude: Double): Double {
        val userLatRad = Math.toRadians(latitude)
        val userLngRad = Math.toRadians(longitude)
        val kaabaLatRad = Math.toRadians(KAABA_LATITUDE)
        val kaabaLngRad = Math.toRadians(KAABA_LONGITUDE)

        val deltaLng = kaabaLngRad - userLngRad
        val y = sin(deltaLng) * cos(kaabaLatRad)
        val x = cos(userLatRad) * sin(kaabaLatRad) -
            sin(userLatRad) * cos(kaabaLatRad) * cos(deltaLng)

        val bearingDeg = Math.toDegrees(atan2(y, x))
        return ((bearingDeg % 360.0) + 360.0) % 360.0
    }

    /**
     * Haversine great-circle distance in kilometers to the Holy Kaaba.
     */
    fun distanceToKaabaKm(latitude: Double, longitude: Double): Double {
        val dLat = Math.toRadians(KAABA_LATITUDE - latitude)
        val dLng = Math.toRadians(KAABA_LONGITUDE - longitude)
        val lat1 = Math.toRadians(latitude)
        val lat2 = Math.toRadians(KAABA_LATITUDE)

        val a = sin(dLat / 2).pow(2) + cos(lat1) * cos(lat2) * sin(dLng / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return EARTH_RADIUS_KM * c
    }
}
