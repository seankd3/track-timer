package com.tracktimer.app

import android.app.Application
import android.content.Context
import android.os.StrictMode
import org.osmdroid.config.Configuration
import java.io.File

/**
 * Application class for TrackTimer app.
 * This class handles global initialization for OSMDroid and other app-wide configurations.
 */
class TrackTimerApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Fix for OSMDroid strict mode policy violations
        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)
        
        // Initialize OSMDroid configuration
        initializeOSMDroid(this)
    }
    
    /**
     * Initialize OSMDroid configuration.
     * This sets up the user agent, tile cache location, and other OSMDroid settings.
     */
    private fun initializeOSMDroid(context: Context) {
        // Configure OSMDroid user agent and storage paths
        Configuration.getInstance().apply {
            userAgentValue = context.packageName
            
            // Set OSMDroid storage paths to application-specific directories
            val osmDroidBasePath = File(context.getExternalFilesDir(null), "osmdroid")
            if (!osmDroidBasePath.exists()) {
                osmDroidBasePath.mkdirs()
            }
            
            // Set the tile cache path
            val tileCache = File(osmDroidBasePath, "tiles")
            if (!tileCache.exists()) {
                tileCache.mkdirs()
            }
            
            osmdroidBasePath = osmDroidBasePath
            osmdroidTileCache = tileCache
            
            // Set a reasonable tile cache size limit (50MB) to prevent excessive storage usage
            tileFileSystemCacheMaxBytes = 50L * 1024 * 1024
            
            // Set a timeout for tile downloads to prevent hanging if network is slow
            gpsWaitTime = 20000
            tileDownloadThreads = 2
            tileDownloadMaxQueueSize = 20
        }
    }
}
