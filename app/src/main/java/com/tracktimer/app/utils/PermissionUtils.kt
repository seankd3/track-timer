package com.tracktimer.app.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.provider.Settings
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.tracktimer.app.R

/**
 * Utility functions for handling permissions and location services checks.
 */
object PermissionUtils {

    const val LOCATION_PERMISSION_REQUEST_CODE = 1001

    /**
     * Check if the app has location permissions
     */
    fun hasLocationPermissions(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Request location permissions
     */
    fun requestLocationPermissions(activity: Activity) {
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ),
            LOCATION_PERMISSION_REQUEST_CODE
        )
    }

    /**
     * Check if location services are enabled
     */
    fun isLocationEnabled(context: Context): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    /**
     * Show dialog to enable location services
     */
    fun showLocationServicesDialog(context: Context) {
        AlertDialog.Builder(context)
            .setTitle("Location Services Disabled")
            .setMessage(context.getString(R.string.location_disabled))
            .setPositiveButton("Open Settings") { _, _ ->
                context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    /**
     * Show rationale for needing location permissions
     */
    fun showPermissionRationaleDialog(context: Context, onRequestPermission: () -> Unit) {
        AlertDialog.Builder(context)
            .setTitle("Location Permission Required")
            .setMessage(context.getString(R.string.permission_rationale))
            .setPositiveButton("Grant") { _, _ ->
                onRequestPermission()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    /**
     * Show dialog when permissions are permanently denied
     */
    fun showPermissionDeniedDialog(context: Context) {
        AlertDialog.Builder(context)
            .setTitle("Permission Denied")
            .setMessage("Location permission has been permanently denied. Please enable it in app settings.")
            .setPositiveButton("App Settings") { _, _ ->
                // Open app settings
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                val uri = Uri.fromParts("package", context.packageName, null)
                intent.data = uri
                context.startActivity(intent)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    /**
     * Shows dialog about battery optimization
     */
    fun showBatteryOptimizationDialog(context: Context) {
        AlertDialog.Builder(context)
            .setTitle("Improve Tracking Accuracy")
            .setMessage(context.getString(R.string.battery_optimization_message))
            .setPositiveButton("Battery Settings") { _, _ ->
                val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                context.startActivity(intent)
            }
            .setNegativeButton("Later", null)
            .show()
    }
}
