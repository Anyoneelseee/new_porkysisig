package com.example.sisig.data

import androidx.room.TypeConverter
import com.example.sisig.MenuItem
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.*

class Converters {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    @TypeConverter
    fun fromMenuItemList(menuItems: List<MenuItem>): String {
        return Gson().toJson(menuItems)
    }

    @TypeConverter
    fun toMenuItemList(menuItemsString: String): List<MenuItem> {
        val type = object : TypeToken<List<MenuItem>>() {}.type
        return Gson().fromJson(menuItemsString, type)
    }

    @TypeConverter
    fun fromDate(date: Date?): String? {
        return date?.let { dateFormat.format(it) }
    }

    @TypeConverter
    fun toDate(dateString: String?): Date? {
        return dateString?.let { dateFormat.parse(it) }
    }

    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }
}