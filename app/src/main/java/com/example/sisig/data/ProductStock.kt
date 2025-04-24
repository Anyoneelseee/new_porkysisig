package com.example.sisig.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "product_stock")
data class ProductStock(
    @PrimaryKey(autoGenerate = true)
    val id: int = 0,
    val productName: String,
    val quantity: Int,
)
