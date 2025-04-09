package com.tracktimer.app.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

/**
 * Utility class for network-related operations.
 */
object NetworkUtils {
    
    /**
     * Check if the device has an active internet connection.
     * 
     * @param context The application context
     * @return True if internet connection is available, false otherwise
     */
    fun isInternetAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        
        // For API 29+
        val networkCapabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
        return networkCapabilities?.let {
            it.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            it.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        } ?: false
    }
    
    /**
     * Check if the device is in airplane mode.
     * 
     * @param context The application context
     * @return True if airplane mode is enabled, false otherwise
     */
    fun isAirplaneModeOn(context: Context): Boolean {
        return android.provider.Settings.Global.getInt(
            context.contentResolver,
            android.provider.Settings.Global.AIRPLANE_MODE_ON, 0
        ) != 0
    }
}
