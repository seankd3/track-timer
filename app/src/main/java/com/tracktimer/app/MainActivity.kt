package com.tracktimer.app

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.BitmapFactory
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.MotionEvent
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import com.tracktimer.app.databinding.ActivityMainBinding
import com.tracktimer.app.service.LocationTrackingService
import com.tracktimer.app.ui.HistoryActivity
import com.tracktimer.app.utils.LocationUtils
import com.tracktimer.app.utils.NetworkUtils
import com.tracktimer.app.utils.PermissionUtils
import com.tracktimer.app.viewmodel.MainViewModel
import java.util.Timer
import java.util.TimerTask

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: MainViewModel
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var map: MapView
    private var myLocationOverlay: MyLocationNewOverlay? = null
    private var startMarker: Marker? = null
    private var endMarker: Marker? = null
    private var routePolyline: Polyline? = null
    private var isSelectingStartPoint = false
    private var isSelectingEndPoint = false
    private var timer: Timer? = null
    
    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) ||
            permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
                // Location access granted
                setupMapLocationEnabled()
                getLastLocation()
                
                // Check if location services are enabled
                if (!PermissionUtils.isLocationEnabled(this)) {
                    PermissionUtils.showLocationServicesDialog(this)
                }
                
                // Show battery optimization info after a short delay
                binding.root.postDelayed({
                    PermissionUtils.showBatteryOptimizationDialog(this)
                }, 2000)
            }
            else -> {
                // Check if we should show rationale
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                    // Show permission rationale
                    PermissionUtils.showPermissionRationaleDialog(this) {
                        checkLocationPermissions()
                    }
                } else {
                    // Permission permanently denied
                    PermissionUtils.showPermissionDeniedDialog(this)
                }
            }
        }
    }
    
    // BroadcastReceiver to receive updates from the location tracking service
    private val locationUpdateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == LocationTrackingService.ACTION_LOCATION_UPDATE) {
                val elapsedTime = intent.getLongExtra(LocationTrackingService.EXTRA_ELAPSED_TIME, 0L)
                val distance = intent.getFloatExtra(LocationTrackingService.EXTRA_DISTANCE, 0f)
                val avgSpeed = intent.getFloatExtra(LocationTrackingService.EXTRA_AVERAGE_SPEED, 0f)
                
                // Update the ViewModel with the new data
                viewModel.updateTrackingInfo(elapsedTime, distance, avgSpeed)
                
                // Get location if available
                if (intent.hasExtra(LocationTrackingService.EXTRA_LOCATION)) {
                    val location = intent.getParcelableExtra<Location>(LocationTrackingService.EXTRA_LOCATION)
                    location?.let {
                        viewModel.setCurrentLocation(it)
                        updateMapWithLocation(it)
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // OSMDroid is configured in TrackTimerApplication class
        
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Initialize ViewModel
        viewModel = ViewModelProvider(this)[MainViewModel::class.java]
        
        // Setup map
        map = binding.map
        configureMap()
        
        // Initialize location client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        
        // Setup click listeners
        setupClickListeners()
        
        // Observe ViewModel
        observeViewModel()
        
        // Check for location permissions
        checkLocationPermissions()
    }

    override fun onStart() {
        super.onStart()
        // Register broadcast receiver
        val filter = IntentFilter(LocationTrackingService.ACTION_LOCATION_UPDATE)
        registerReceiver(locationUpdateReceiver, filter)
    }
    
    override fun onResume() {
        super.onResume()
        map.onResume() // OSMDroid needs this
    }
    
    override fun onPause() {
        super.onPause()
        map.onPause() // OSMDroid needs this
    }

    override fun onStop() {
        super.onStop()
        // Unregister broadcast receiver
        try {
            unregisterReceiver(locationUpdateReceiver)
        } catch (e: IllegalArgumentException) {
            // Receiver not registered
        }
    }

    /**
     * Configure the OSMDroid map with necessary settings and overlays
     */
    private fun configureMap() {
        try {
            // Basic map configuration
            map.apply {
                setTileSource(TileSourceFactory.MAPNIK) // Use standard OpenStreetMap tiles
                setMultiTouchControls(true) // Enable pinch to zoom
                controller.setZoom(15.0) // Initial zoom level
                
                // Add default overlays
                overlays.clear()
                
                // Add rotation gestures
                val rotationGestureOverlay = RotationGestureOverlay(map)
                rotationGestureOverlay.isEnabled = true
                overlays.add(rotationGestureOverlay)
                
                // Setup location tracking overlay if we have permission
                if (hasLocationPermission()) {
                    setupMapLocationEnabled()
                }
            }
            
            // Set map click listener for selecting points
            map.overlayManager.tilesOverlay.setOptionsMenuEnabled(true)
            
            // Add a map click listener
            val mapClickListener = object : MapView.OnSingleTapListener {
                override fun onSingleTap(e: MotionEvent): Boolean {
                    val projection = map.projection
                    val geoPoint = projection.fromPixels(e.x.toInt(), e.y.toInt()) as GeoPoint
                    
                    when {
                        isSelectingStartPoint -> {
                            viewModel.setStartPoint(geoPoint)
                            isSelectingStartPoint = false
                            Toast.makeText(this@MainActivity, getString(R.string.start_point_selected), Toast.LENGTH_SHORT).show()
                            // Animate marker addition for better UX
                            updateStartMarker(geoPoint, true)
                            return true
                        }
                        isSelectingEndPoint -> {
                            viewModel.setEndPoint(geoPoint)
                            isSelectingEndPoint = false
                            Toast.makeText(this@MainActivity, getString(R.string.end_point_selected), Toast.LENGTH_SHORT).show()
                            // Animate marker addition for better UX
                            updateEndMarker(geoPoint, true)
                            return true
                        }
                    }
                    return false
                }
            }
            map.addOnSingleTapListener(mapClickListener)
            
            // Get last location to center map
            getLastLocation()
            
        } catch (e: Exception) {
            Log.e("MainActivity", "Error configuring map", e)
        }
    }

    private fun setupClickListeners() {
        binding.btnSelectStart.setOnClickListener {
            isSelectingStartPoint = true
            isSelectingEndPoint = false
            Toast.makeText(this, "Tap on the map to select start point", Toast.LENGTH_LONG).show()
        }
        
        binding.btnSelectEnd.setOnClickListener {
            isSelectingStartPoint = false
            isSelectingEndPoint = true
            Toast.makeText(this, "Tap on the map to select end point", Toast.LENGTH_LONG).show()
        }
        
        binding.btnStartTracking.setOnClickListener {
            if (viewModel.trackingActive.value == true) {
                stopTracking()
            } else {
                startTracking()
            }
        }
        
        binding.btnReset.setOnClickListener {
            resetTracking()
        }
        
        binding.btnHistory.setOnClickListener {
            val intent = Intent(this, HistoryActivity::class.java)
            startActivity(intent)
        }
    }

    private fun observeViewModel() {
        viewModel.startPoint.observe(this) { latLng ->
            latLng?.let {
                updateStartMarker(it)
                updateMapView()
            }
        }
        
        viewModel.endPoint.observe(this) { latLng ->
            latLng?.let {
                updateEndMarker(it)
                updateMapView()
            }
        }
        
        viewModel.trackingActive.observe(this) { isActive ->
            updateTrackingUI(isActive)
        }
        
        viewModel.elapsedTimeMillis.observe(this) { timeMillis ->
            binding.tvElapsedTime.text = getString(
                R.string.elapsed_time, 
                LocationUtils.formatElapsedTime(timeMillis)
            )
        }
        
        viewModel.distanceMeters.observe(this) { distance ->
            binding.tvDistance.text = getString(
                R.string.distance, 
                LocationUtils.formatDistance(distance)
            )
        }
        
        viewModel.averageSpeedKmh.observe(this) { speed ->
            binding.tvAvgSpeed.text = getString(
                R.string.average_speed, 
                LocationUtils.formatSpeed(speed)
            )
        }
        
        viewModel.currentLocation.observe(this) { location ->
            // Update map with new location if tracking
            if (viewModel.trackingActive.value == true) {
                updateMapWithLocation(location)
            }
        }
    }

    private fun updateStartMarker(position: GeoPoint, animate: Boolean = false) {
        // Remove existing marker if exists
        if (startMarker != null) {
            map.overlays.remove(startMarker)
            startMarker = null
        }
        
        // Create new marker
        startMarker = Marker(map).apply {
            position = position
            title = "Start Point"
            snippet = "Tap to show info"
            icon = ContextCompat.getDrawable(this@MainActivity, R.drawable.ic_marker_start) ?: 
                   ContextCompat.getDrawable(this@MainActivity, android.R.drawable.ic_menu_mylocation)
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            infoWindow = null // We'll handle info display ourselves
        }
        
        if (animate) {
            // Start with 0 alpha for animation
            startMarker?.alpha = 0f
            
            // Add to map
            map.overlays.add(startMarker)
            
            // Animate with fade-in
            val handler = Handler(Looper.getMainLooper())
            var alpha = 0f
            val animator = object : Runnable {
                override fun run() {
                    alpha += 0.1f
                    if (alpha <= 1.0f) {
                        startMarker?.alpha = alpha
                        map.invalidate() // Force redraw
                        handler.postDelayed(this, 50)
                    }
                }
            }
            handler.post(animator)
        } else {
            // Add to map without animation
            map.overlays.add(startMarker)
        }
        
        // Refresh the map
        map.invalidate()
    }

    private fun updateEndMarker(position: GeoPoint, animate: Boolean = false) {
        // Remove existing marker if exists
        if (endMarker != null) {
            map.overlays.remove(endMarker)
            endMarker = null
        }
        
        // Create new marker
        endMarker = Marker(map).apply {
            position = position
            title = "End Point"
            snippet = "Tap to show info"
            icon = ContextCompat.getDrawable(this@MainActivity, R.drawable.ic_marker_end) ?: 
                   ContextCompat.getDrawable(this@MainActivity, android.R.drawable.ic_menu_myplaces)
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            infoWindow = null // We'll handle info display ourselves
        }
        
        if (animate) {
            // Start with 0 alpha for animation
            endMarker?.alpha = 0f
            
            // Add to map
            map.overlays.add(endMarker)
            
            // Animate with fade-in
            val handler = Handler(Looper.getMainLooper())
            var alpha = 0f
            val animator = object : Runnable {
                override fun run() {
                    alpha += 0.1f
                    if (alpha <= 1.0f) {
                        endMarker?.alpha = alpha
                        map.invalidate() // Force redraw
                        handler.postDelayed(this, 50)
                    }
                }
            }
            handler.post(animator)
        } else {
            // Add to map without animation
            map.overlays.add(endMarker)
        }
        
        // Refresh the map
        map.invalidate()
    }

    private fun updateMapView() {
        val startPoint = viewModel.startPoint.value
        val endPoint = viewModel.endPoint.value
        
        if (startPoint != null && endPoint != null) {
            // Calculate the bounds that include both points
            val north = Math.max(startPoint.latitude, endPoint.latitude)
            val south = Math.min(startPoint.latitude, endPoint.latitude)
            val east = Math.max(startPoint.longitude, endPoint.longitude)
            val west = Math.min(startPoint.longitude, endPoint.longitude)
            
            // Add some padding
            val latPadding = (north - south) * 0.3
            val lonPadding = (east - west) * 0.3
            
            // Set bounds with padding
            val boundingBox = org.osmdroid.util.BoundingBox(
                north + latPadding,
                east + lonPadding,
                south - latPadding,
                west - lonPadding
            )
            
            // Move map to show both points
            map.zoomToBoundingBox(boundingBox, true, 100)
            
            // Draw line between points
            if (routePolyline != null) {
                map.overlays.remove(routePolyline)
            }
            
            // Create new polyline
            routePolyline = Polyline().apply {
                outlinePaint.color = ContextCompat.getColor(this@MainActivity, android.R.color.holo_blue_dark)
                outlinePaint.strokeWidth = 10f
                addPoint(startPoint)
                addPoint(endPoint)
            }
            
            // Add to map
            map.overlays.add(routePolyline)
            map.invalidate()
        } else if (startPoint != null) {
            // Only start point is set
            map.controller.animateTo(startPoint)
            map.controller.setZoom(15.0)
        } else if (endPoint != null) {
            // Only end point is set
            map.controller.animateTo(endPoint)
            map.controller.setZoom(15.0)
        }
    }

    private fun updateMapWithLocation(location: Location) {
        val geoPoint = LocationUtils.locationToGeoPoint(location)
        
        // If tracking is active, update map view
        if (viewModel.trackingActive.value == true) {
            // Move map to follow user
            map.controller.animateTo(geoPoint)
            
            // If not zoomed in enough, set zoom level for tracking
            if (map.zoomLevelDouble < 15.0) {
                map.controller.setZoom(17.0)
            }
        }
    }

    private fun updateTrackingUI(isTracking: Boolean) {
        binding.btnStartTracking.text = if (isTracking) {
            getString(R.string.stop_tracking)
        } else {
            getString(R.string.start_tracking)
        }
        
        binding.btnSelectStart.isEnabled = !isTracking
        binding.btnSelectEnd.isEnabled = !isTracking
        
        if (isTracking) {
            // Start timer to update UI
            startTimerUpdates()
        } else {
            // Stop timer
            stopTimerUpdates()
        }
    }

    private fun startTimerUpdates() {
        timer?.cancel()
        timer = Timer()
        timer?.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                val startTimeValue = viewModel.startTime.value ?: return
                val elapsedTime = System.currentTimeMillis() - startTimeValue
                runOnUiThread {
                    binding.tvElapsedTime.text = getString(
                        R.string.elapsed_time,
                        LocationUtils.formatElapsedTime(elapsedTime)
                    )
                }
            }
        }, 0, 1000) // Update every second
    }

    private fun stopTimerUpdates() {
        timer?.cancel()
        timer = null
    }

    private fun startTracking() {
        val startPoint = viewModel.startPoint.value
        val endPoint = viewModel.endPoint.value
        
        if (startPoint == null || endPoint == null) {
            Toast.makeText(this, "Please select both start and end points", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Update ViewModel
        viewModel.startTracking()
        
        // Start the location tracking service
        val serviceIntent = Intent(this, LocationTrackingService::class.java).apply {
            action = LocationTrackingService.ACTION_START_TRACKING
        }
        startService(serviceIntent)
        
        Toast.makeText(this, R.string.tracking_active, Toast.LENGTH_SHORT).show()
    }

    private fun stopTracking() {
        // Update ViewModel
        viewModel.stopTracking()
        
        // Stop the location tracking service
        val serviceIntent = Intent(this, LocationTrackingService::class.java).apply {
            action = LocationTrackingService.ACTION_STOP_TRACKING
        }
        startService(serviceIntent)
        
        // Ask user if they want to save the record
        showSaveRecordDialog()
        
        Toast.makeText(this, R.string.tracking_stopped, Toast.LENGTH_SHORT).show()
    }

    private fun resetTracking() {
        // Reset ViewModel
        viewModel.resetTracking()
        
        // Remove markers and polyline from the map
        if (startMarker != null) {
            map.overlays.remove(startMarker)
            startMarker = null
        }
        
        if (endMarker != null) {
            map.overlays.remove(endMarker)
            endMarker = null
        }
        
        if (routePolyline != null) {
            map.overlays.remove(routePolyline)
            routePolyline = null
        }
        
        // Refresh the map
        map.invalidate()
    }

    private fun showSaveRecordDialog() {
        AlertDialog.Builder(this)
            .setTitle("Save Record")
            .setMessage("Do you want to save this route record?")
            .setPositiveButton("Save") { _, _ ->
                viewModel.saveRouteRecord()
                Toast.makeText(this, R.string.record_saved, Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Discard") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun getLastLocation() {
        try {
            if (hasLocationPermission()) {
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    location?.let {
                        val geoPoint = GeoPoint(it.latitude, it.longitude)
                        map.controller.setCenter(geoPoint)
                        map.controller.setZoom(15.0)
                        
                        // If we have a location overlay, update its location
                        myLocationOverlay?.onLocationChanged(it, null)
                    }
                }
            }
        } catch (e: SecurityException) {
            checkLocationPermissions()
        }
    }

    private fun hasLocationPermission(): Boolean {
        return PermissionUtils.hasLocationPermissions(this)
    }

    private fun checkLocationPermissions() {
        if (!hasLocationPermission()) {
            locationPermissionRequest.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        } else {
            // Permissions already granted, but check if location services are enabled
            if (!PermissionUtils.isLocationEnabled(this)) {
                PermissionUtils.showLocationServicesDialog(this)
            }
        }
    }

    /**
     * Enable location features on the map if permissions are granted
     */
    private fun setupMapLocationEnabled() {
        try {
            if (hasLocationPermission()) {
                // Create and configure the my location overlay
                myLocationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(this), map)
                myLocationOverlay?.apply {
                    enableMyLocation()
                    enableFollowLocation()
                    setPersonIcon(BitmapFactory.decodeResource(resources, android.R.drawable.ic_menu_mylocation))
                }
                
                // Add the overlay to the map
                map.overlays.add(myLocationOverlay)
                
                // Refresh map to show changes
                map.invalidate()
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error setting up location overlay", e)
        }
    }
    
    /**
     * Adds an overlay to inform the user about offline mode
     * This improves user experience by providing clear feedback when internet is unavailable
     */
    private fun addOfflineOverlay() {
        try {
            // Create offline mode banner
            val offlineBanner = TextView(this).apply {
                text = "OFFLINE MODE - Limited map functionality"
                setBackgroundColor(ContextCompat.getColor(this@MainActivity, android.R.color.holo_red_light))
                setTextColor(ContextCompat.getColor(this@MainActivity, android.R.color.white))
                textSize = 14f
                gravity = android.view.Gravity.CENTER
                setPadding(20, 10, 20, 10)
                alpha = 0.8f
            }
            
            // Add banner to map container layout
            binding.root.addView(offlineBanner, ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ))
            
            // Set banner to display at the top of the screen
            (offlineBanner.layoutParams as? ViewGroup.MarginLayoutParams)?.apply {
                topMargin = resources.getDimensionPixelSize(com.google.android.material.R.dimen.design_appbar_elevation)
            }
            
            // Update map settings for offline mode
            map.apply {
                // Limit zoom levels to prevent blank tiles
                maxZoomLevel = 15.0
                // Use cached tiles only
                setUseDataConnection(false)
            }
            
            // Schedule a periodic connection check
            Handler(Looper.getMainLooper()).postDelayed({
                if (NetworkUtils.isInternetAvailable(this)) {
                    // Connection restored
                    binding.root.removeView(offlineBanner)
                    map.setUseDataConnection(true)
                    map.invalidate()
                    Toast.makeText(this, "Connection restored", Toast.LENGTH_SHORT).show()
                }
            }, 30000) // Check every 30 seconds
        } catch (e: Exception) {
            Log.e("MainActivity", "Error adding offline overlay", e)
        }
    }
}
