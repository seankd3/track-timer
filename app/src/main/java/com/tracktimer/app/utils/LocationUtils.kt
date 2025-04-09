package com.tracktimer.app.utils

import android.location.Location
import org.osmdroid.util.GeoPoint
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Utility functions for location-related calculations and formatting.
 */
object LocationUtils {

    /**
     * Calculate the total distance of a path given a list of locations.
     * @return Total distance in meters
     */
    fun calculatePathDistance(locations: List<Location>): Float {
        var totalDistance = 0f
        for (i in 0 until locations.size - 1) {
            val start = locations[i]
            val end = locations[i + 1]
            totalDistance += start.distanceTo(end)
        }
        return totalDistance
    }

    /**
     * Format distance in meters to a human-readable string.
     * @return Formatted distance string (e.g., "1.2 km" or "450 m")
     */
    fun formatDistance(distanceMeters: Float): String {
        return if (distanceMeters >= 1000) {
            String.format("%.2f km", distanceMeters / 1000)
        } else {
            String.format("%.0f m", distanceMeters)
        }
    }

    /**
     * Format speed in km/h to a human-readable string.
     * @return Formatted speed string (e.g., "45.2 km/h")
     */
    fun formatSpeed(speedKmh: Float): String {
        return String.format("%.1f km/h", speedKmh)
    }

    /**
     * Format time in milliseconds to a human-readable string (HH:MM:SS).
     * @return Formatted time string
     */
    fun formatElapsedTime(timeMillis: Long): String {
        val hours = TimeUnit.MILLISECONDS.toHours(timeMillis)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(timeMillis) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(timeMillis) % 60
        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }

    /**
     * Format date to a readable string.
     * @return Formatted date string
     */
    fun formatDate(timestamp: Long): String {
        val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
        return dateFormat.format(timestamp)
    }

    /**
     * Calculate the center point between start and end locations.
     * @return GeoPoint of the center point
     */
    fun calculateCenterPoint(start: GeoPoint, end: GeoPoint): GeoPoint {
        val lat = (start.latitude + end.latitude) / 2
        val lng = (start.longitude + end.longitude) / 2
        return GeoPoint(lat, lng)
    }

    /**
     * Convert Location to GeoPoint.
     */
    fun locationToGeoPoint(location: Location): GeoPoint {
        return GeoPoint(location.latitude, location.longitude)
    }
    
    /**
     * Convert GeoPoint to Location.
     */
    fun geoPointToLocation(geoPoint: GeoPoint): Location {
        val location = Location("OSM")
        location.latitude = geoPoint.latitude
        location.longitude = geoPoint.longitude
        return location
    }

    /**
     * Calculate appropriate zoom level based on distance between points.
     * @return Zoom level (higher values = more zoomed in)
     */
    fun calculateZoomLevel(distanceInMeters: Float): Float {
        // Simple heuristic for zoom level based on distance
        return when {
            distanceInMeters < 500 -> 16f
            distanceInMeters < 1000 -> 15f
            distanceInMeters < 2000 -> 14f
            distanceInMeters < 5000 -> 13f
            distanceInMeters < 10000 -> 12f
            else -> 11f
        }
    }
}
