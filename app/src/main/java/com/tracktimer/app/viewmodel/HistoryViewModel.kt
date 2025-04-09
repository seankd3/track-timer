package com.tracktimer.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.tracktimer.app.data.AppDatabase
import com.tracktimer.app.data.RouteRecord
import com.tracktimer.app.data.RouteRepository
import kotlinx.coroutines.launch

/**
 * ViewModel for the history screen to display and manage route records.
 */
class HistoryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: RouteRepository
    val allRouteRecords: LiveData<List<RouteRecord>>

    init {
        val routeRecordDao = AppDatabase.getDatabase(application).routeRecordDao()
        repository = RouteRepository(routeRecordDao)
        allRouteRecords = repository.allRouteRecords
    }

    /**
     * Delete a route record
     */
    fun deleteRouteRecord(routeRecord: RouteRecord) = viewModelScope.launch {
        repository.delete(routeRecord)
    }

    /**
     * Delete a route record by ID
     */
    fun deleteRouteRecordById(id: Long) = viewModelScope.launch {
        repository.deleteRouteById(id)
    }
}
