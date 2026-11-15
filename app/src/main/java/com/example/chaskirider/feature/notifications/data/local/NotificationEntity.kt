package com.example.chaskirider.feature.notifications.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "notifications", primaryKeys = ["id"])
data class NotificationEntity(
    val id: String,
    val riderId: String?,
    val title: String,
    val body: String,
    val createdAt: Long,
    val read: Boolean
)

@Dao
interface NotificationDao {
    @Query("SELECT * FROM notifications ORDER BY createdAt DESC LIMIT 50")
    fun observe(): Flow<List<NotificationEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: NotificationEntity)

    @Query("UPDATE notifications SET read = 1")
    suspend fun markAllRead()

    @Query("DELETE FROM notifications")
    suspend fun clear()
}
