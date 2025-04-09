package com.tracktimer.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.tracktimer.app.MainActivity
import com.tracktimer.app.R
import com.tracktimer.app.utils.LocationUtils
import java.util.concurrent.TimeUnit

/**
 * Foreground service that tracks user location for the route timing.
 * This ensures location updates continue even when app is in the background.
 */
class LocationTrackingService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private val locationList = mutableListOf<Location>()
    private var startTime: Long = 0
    private var isTracking = false

    companion object {
        const val ACTION_START_TRACKING = "ACTION_START_TRACKING"
        const val ACTION_STOP_TRACKING = "ACTION_STOP_TRACKING"
        const val ACTION_LOCATION_UPDATE = "ACTION_LOCATION_UPDATE"
        const val EXTRA_LOCATION = "EXTRA_LOCATION"
        const val EXTRA_START_TIME = "EXTRA_START_TIME"
        const val EXTRA_ELAPSED_TIME = "EXTRA_ELAPSED_TIME"
        const val EXTRA_DISTANCE = "EXTRA_DISTANCE"
        const val EXTRA_AVERAGE_SPEED = "EXTRA_AVERAGE_SPEED"
        
        private const val NOTIFICATION_ID = 1
        private const val NOTIFICATION_CHANNEL_ID = "tracking_channel"
    }

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        
        // Initialize location callback
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                if (isTracking) {
                    for (location in locationResult.locations) {
                        addLocationToList(location)
                        broadcastLocationUpdate(location)
                    }
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null) {
            when (intent.action) {
                ACTION_START_TRACKING -> {
                    startTracking()
                }
                ACTION_STOP_TRACKING -> {
                    stopTracking()
                }
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun startTracking() {
        if (!isTracking) {
            isTracking = true
            startTime = System.currentTimeMillis()
            locationList.clear()
            
            // Start as foreground service with notification
            startForeground(NOTIFICATION_ID, createNotification())
            
            // Request location updates
            requestLocationUpdates()
        }
    }

    private fun stopTracking() {
        if (isTracking) {
            isTracking = false
            fusedLocationClient.removeLocationUpdates(locationCallback)
            
            // Calculate final statistics
            val elapsedTime = System.currentTimeMillis() - startTime
            val totalDistance = calculateTotalDistance()
            val avgSpeed = calculateAverageSpeed(totalDistance, elapsedTime)
            
            // Broadcast final results
            broadcastFinalResults(elapsedTime, totalDistance, avgSpeed)
            
            // Stop foreground service
            stopForeground(true)
            stopSelf()
        }
    }

    private fun requestLocationUpdates() {
        try {
            val locationRequest = LocationRequest.create().apply {
                interval = 1000 // Update interval in milliseconds
                fastestInterval = 500
                priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            }
            
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    private fun addLocationToList(location: Location) {
        locationList.add(location)
    }

    private fun calculateTotalDistance(): Float {
        return LocationUtils.calculatePathDistance(locationList)
    }

    private fun calculateAverageSpeed(distanceMeters: Float, timeMillis: Long): Float {
        val hours = TimeUnit.MILLISECONDS.toHours(timeMillis).toFloat() + 
                   (TimeUnit.MILLISECONDS.toMinutes(timeMillis) % 60) / 60f
        
        return if (hours > 0) {
            // Convert meters to kilometers and divide by hours
            (distanceMeters / 1000f) / hours
        } else {
            0f
        }
    }

    private fun broadcastLocationUpdate(location: Location) {
        val intent = Intent(ACTION_LOCATION_UPDATE).apply {
            putExtra(EXTRA_LOCATION, location)
            putExtra(EXTRA_START_TIME, startTime)
            putExtra(EXTRA_ELAPSED_TIME, System.currentTimeMillis() - startTime)
            putExtra(EXTRA_DISTANCE, calculateTotalDistance())
        }
        sendBroadcast(intent)
    }

    private fun broadcastFinalResults(elapsedTime: Long, distance: Float, avgSpeed: Float) {
        val intent = Intent(ACTION_LOCATION_UPDATE).apply {
            putExtra(EXTRA_START_TIME, startTime)
            putExtra(EXTRA_ELAPSED_TIME, elapsedTime)
            putExtra(EXTRA_DISTANCE, distance)
            putExtra(EXTRA_AVERAGE_SPEED, avgSpeed)
        }
        sendBroadcast(intent)
    }

    private fun createNotification(): Notification {
        // Create notification channel for Android O and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Location Tracking",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Used for tracking your location during route timing"
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
        
        // Create intent for notification tap
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Build the notification
        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("TrackTimer")
            .setContentText("Tracking your route...")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }
}
