package com.tracktimer.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

/**
 * Entity representing a saved route record with start and end points,
 * elapsed time, distance, and other metrics.
 */
@Entity(tableName = "route_records")
data class RouteRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val startPointLat: Double,
    val startPointLng: Double,
    val endPointLat: Double,
    val endPointLng: Double,
    val startTime: Date,
    val endTime: Date,
    val elapsedTimeMillis: Long,
    val distanceMeters: Float,
    val averageSpeedKmh: Float,
    val routeName: String? = null,
    val notes: String? = null
)
