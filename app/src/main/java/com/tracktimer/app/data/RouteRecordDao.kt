package com.tracktimer.app.data

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

/**
 * Data Access Object (DAO) for the RouteRecord entity.
 * Provides methods to interact with the route_records table in the database.
 */
@Dao
interface RouteRecordDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRouteRecord(routeRecord: RouteRecord): Long

    @Update
    suspend fun updateRouteRecord(routeRecord: RouteRecord)

    @Delete
    suspend fun deleteRouteRecord(routeRecord: RouteRecord)

    @Query("SELECT * FROM route_records ORDER BY startTime DESC")
    fun getAllRouteRecords(): LiveData<List<RouteRecord>>

    @Query("SELECT * FROM route_records WHERE id = :id")
    suspend fun getRouteRecordById(id: Long): RouteRecord?

    @Query("DELETE FROM route_records WHERE id = :id")
    suspend fun deleteRouteRecordById(id: Long)
}
