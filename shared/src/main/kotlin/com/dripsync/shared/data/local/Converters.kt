package com.dripsync.shared.data.local

import androidx.room.TypeConverter
import com.dripsync.shared.data.model.BeverageType
import com.dripsync.shared.data.model.SourceDevice
import java.time.Instant

/**
 * Room 用の型コンバーター
 */
class Converters {

    @TypeConverter
    fun fromInstant(value: Instant?): Long? {
        return value?.toEpochMilli()
    }

    @TypeConverter
    fun toInstant(value: Long?): Instant? {
        return value?.let { Instant.ofEpochMilli(it) }
    }

    @TypeConverter
    fun fromBeverageType(value: BeverageType): String {
        return value.name
    }

    @TypeConverter
    fun toBeverageType(value: String): BeverageType {
        return BeverageType.valueOf(value)
    }

    @TypeConverter
    fun fromSourceDevice(value: SourceDevice): String {
        return value.name
    }

    @TypeConverter
    fun toSourceDevice(value: String): SourceDevice {
        return SourceDevice.valueOf(value)
    }
}
