package com.example.sisig.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.sisig.MenuItem
import java.util.Date

@Entity(tableName = "all_orders")
data class AllOrder(
    @PrimaryKey(autoGenerate = true)
    val orderId: Long = 0,
    @TypeConverters(Converters::class)
    val orderDetail: List<MenuItem>,
    val totalAmount: Double,
    @TypeConverters(Converters::class)
    val date: Date = Date()
)
