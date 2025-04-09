package com.tracktimer.app.data

import androidx.room.TypeConverter
import java.util.Date

/**
 * Type converters for Room database to handle Date objects.
 */
class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }
}
