package com.example.sisig.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationDao {
    @Insert
    fun insert(notification: Notification): Long

    @Delete
    fun delete(notification: Notification): Int

    @Query("DELETE FROM notifications WHERE id = :id")
    fun deleteById(id: Long): Int

    @Query("SELECT * FROM notifications ORDER BY timestamp DESC")
    fun getAllNotifications(): Flow<List<Notification>>

    @Query("SELECT * FROM notifications ORDER BY timestamp DESC")
    fun getAllNotificationsSync(): List<Notification>

    @Query("DELETE FROM notifications")
    fun clearAll(): Int
}