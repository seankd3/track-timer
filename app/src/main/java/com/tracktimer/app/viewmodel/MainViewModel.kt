package com.tracktimer.app.viewmodel

import android.app.Application
import android.location.Location
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import org.osmdroid.util.GeoPoint
import com.tracktimer.app.data.AppDatabase
import com.tracktimer.app.data.RouteRecord
import com.tracktimer.app.data.RouteRepository
import com.tracktimer.app.utils.LocationUtils
import kotlinx.coroutines.launch
import java.util.Date

/**
 * ViewModel that manages UI-related data for the main activity.
 */
class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: RouteRepository
    val allRouteRecords: LiveData<List<RouteRecord>>
    
    // Location-related LiveData
    private val _startPoint = MutableLiveData<GeoPoint>()
    val startPoint: LiveData<GeoPoint> = _startPoint
    
    private val _endPoint = MutableLiveData<GeoPoint>()
    val endPoint: LiveData<GeoPoint> = _endPoint
    
    private val _currentLocation = MutableLiveData<Location>()
    val currentLocation: LiveData<Location> = _currentLocation
    
    private val _trackingActive = MutableLiveData<Boolean>()
    val trackingActive: LiveData<Boolean> = _trackingActive
    
    private val _elapsedTimeMillis = MutableLiveData<Long>()
    val elapsedTimeMillis: LiveData<Long> = _elapsedTimeMillis
    
    private val _distanceMeters = MutableLiveData<Float>()
    val distanceMeters: LiveData<Float> = _distanceMeters
    
    private val _averageSpeedKmh = MutableLiveData<Float>()
    val averageSpeedKmh: LiveData<Float> = _averageSpeedKmh
    
    private val _startTime = MutableLiveData<Long>()
    val startTime: LiveData<Long> = _startTime
    
    private val locationList = mutableListOf<Location>()

    init {
        val routeRecordDao = AppDatabase.getDatabase(application).routeRecordDao()
        repository = RouteRepository(routeRecordDao)
        allRouteRecords = repository.allRouteRecords
        
        _trackingActive.value = false
        _elapsedTimeMillis.value = 0L
        _distanceMeters.value = 0f
        _averageSpeedKmh.value = 0f
    }

    // Set start point for route
    fun setStartPoint(geoPoint: GeoPoint) {
        _startPoint.value = geoPoint
    }

    // Set end point for route
    fun setEndPoint(geoPoint: GeoPoint) {
        _endPoint.value = geoPoint
    }

    // Set current location from GPS
    fun setCurrentLocation(location: Location) {
        _currentLocation.value = location
        
        if (_trackingActive.value == true) {
            locationList.add(location)
            updateDistance()
        }
    }

    // Start tracking the route
    fun startTracking() {
        if (_trackingActive.value != true) {
            _trackingActive.value = true
            _startTime.value = System.currentTimeMillis()
            locationList.clear()
            _elapsedTimeMillis.value = 0L
            _distanceMeters.value = 0f
            _averageSpeedKmh.value = 0f
        }
    }

    // Stop tracking the route
    fun stopTracking() {
        if (_trackingActive.value == true) {
            _trackingActive.value = false
        }
    }

    // Update tracking information from service
    fun updateTrackingInfo(elapsedTime: Long, distance: Float, avgSpeed: Float = 0f) {
        _elapsedTimeMillis.value = elapsedTime
        _distanceMeters.value = distance
        
        if (avgSpeed > 0) {
            _averageSpeedKmh.value = avgSpeed
        } else {
            updateAverageSpeed()
        }
    }

    // Reset all tracking data
    fun resetTracking() {
        _trackingActive.value = false
        _startPoint.value = null
        _endPoint.value = null
        locationList.clear()
        _elapsedTimeMillis.value = 0L
        _distanceMeters.value = 0f
        _averageSpeedKmh.value = 0f
        _startTime.value = null
    }

    // Update the total distance based on location list
    private fun updateDistance() {
        _distanceMeters.value = LocationUtils.calculatePathDistance(locationList)
        updateAverageSpeed()
    }

    // Update the average speed based on distance and time
    private fun updateAverageSpeed() {
        val distance = _distanceMeters.value ?: 0f
        val time = _elapsedTimeMillis.value ?: 0L
        
        if (time > 0) {
            val hours = time / 3600000.0f
            _averageSpeedKmh.value = if (hours > 0) {
                (distance / 1000f) / hours
            } else {
                0f
            }
        }
    }

    // Save route record to database
    fun saveRouteRecord(routeName: String? = null, notes: String? = null) = viewModelScope.launch {
        val startPoint = _startPoint.value
        val endPoint = _endPoint.value
        val elapsedTime = _elapsedTimeMillis.value ?: 0L
        val distance = _distanceMeters.value ?: 0f
        val avgSpeed = _averageSpeedKmh.value ?: 0f
        val startTimeValue = _startTime.value ?: System.currentTimeMillis() - elapsedTime
        
        if (startPoint != null && endPoint != null && elapsedTime > 0) {
            val record = RouteRecord(
                startPointLat = startPoint.latitude,
                startPointLng = startPoint.longitude,
                endPointLat = endPoint.latitude,
                endPointLng = endPoint.longitude,
                startTime = Date(startTimeValue),
                endTime = Date(startTimeValue + elapsedTime),
                elapsedTimeMillis = elapsedTime,
                distanceMeters = distance,
                averageSpeedKmh = avgSpeed,
                routeName = routeName,
                notes = notes
            )
            
            repository.insert(record)
        }
    }

    // Delete route record from database
    fun deleteRouteRecord(routeRecord: RouteRecord) = viewModelScope.launch {
        repository.delete(routeRecord)
    }
}
