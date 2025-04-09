package com.tracktimer.app.data

import androidx.lifecycle.LiveData

/**
 * Repository for handling data operations between the ViewModel and the database.
 */
class RouteRepository(private val routeRecordDao: RouteRecordDao) {
    
    val allRouteRecords: LiveData<List<RouteRecord>> = routeRecordDao.getAllRouteRecords()
    
    suspend fun insert(routeRecord: RouteRecord): Long {
        return routeRecordDao.insertRouteRecord(routeRecord)
    }
    
    suspend fun update(routeRecord: RouteRecord) {
        routeRecordDao.updateRouteRecord(routeRecord)
    }
    
    suspend fun delete(routeRecord: RouteRecord) {
        routeRecordDao.deleteRouteRecord(routeRecord)
    }
    
    suspend fun getRouteById(id: Long): RouteRecord? {
        return routeRecordDao.getRouteRecordById(id)
    }
    
    suspend fun deleteRouteById(id: Long) {
        routeRecordDao.deleteRouteRecordById(id)
    }
}
