package com.cdcs.data.local

import androidx.room.TypeConverter

class Converters {
    private val separator = ","

    @TypeConverter
    fun fromList(list: List<String>?): String? {
        return list?.joinToString(separator)
    }

    @TypeConverter
    fun toList(data: String?): List<String>? {
        return data?.split(separator)?.map { it.trim() }?.filter { it.isNotEmpty() }
    }
}
