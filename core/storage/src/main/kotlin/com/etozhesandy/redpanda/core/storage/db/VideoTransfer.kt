package com.etozhesandy.redpanda.core.storage.db

import androidx.room.*

@Entity(tableName = "video_transfer")
data class VideoTransfer(@PrimaryKey val key: String, val bytes: Long, val total: Long, val validator: String?, val complete: Boolean)

@Dao
interface VideoTransferDao {
    @Query("SELECT p.displayName AS profile, d.peerName AS chat, d.kind AS kind FROM attachments a JOIN profiles p ON p.id=a.profileId LEFT JOIN dialogs d ON d.id=a.dialogId WHERE a.path=:url UNION ALL SELECT p.displayName, NULL, NULL FROM profile_saved_photos s JOIN profiles p ON p.id=s.profileId WHERE s.url=:url LIMIT 1")
    fun owner(url: String): MediaOwner?
    @Query("SELECT type FROM attachments WHERE path = :url LIMIT 1")
    fun attachmentKind(url: String): String?
    @Query("SELECT complete FROM video_transfer WHERE `key`=:key")
    fun completed(key: String): Boolean?
    @Query("SELECT * FROM video_transfer WHERE `key` = :key")
    fun find(key: String): VideoTransfer?
    @Query("DELETE FROM video_transfer WHERE `key`=:key")
    fun delete(key: String)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun save(value: VideoTransfer)
}
data class MediaOwner(val profile: String, val chat: String?, val kind: String?)
