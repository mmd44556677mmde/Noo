package com.etozhesandy.redpanda.core.storage.db

import androidx.room.*

@Entity(tableName = "video_transfer")
data class VideoTransfer(@PrimaryKey val key: String, val bytes: Long, val total: Long, val validator: String?, val complete: Boolean)

@Dao
interface VideoTransferDao {
    @Query("SELECT * FROM video_transfer WHERE `key` = :key")
    fun find(key: String): VideoTransfer?
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun save(value: VideoTransfer)
}
