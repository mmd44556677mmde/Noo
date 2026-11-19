package com.etozhesandy.redpanda.core.storage.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.etozhesandy.redpanda.core.storage.db.attachment.AttachmentDao
import com.etozhesandy.redpanda.core.storage.db.attachment.AttachmentEntity
import com.etozhesandy.redpanda.core.storage.db.chatbookmark.ChatBookmarkDao
import com.etozhesandy.redpanda.core.storage.db.chatbookmark.ChatBookmarkEntity
import com.etozhesandy.redpanda.core.storage.db.dialog.DialogDao
import com.etozhesandy.redpanda.core.storage.db.dialog.DialogEntity
import com.etozhesandy.redpanda.core.storage.db.favorite.FavoriteMessageDao
import com.etozhesandy.redpanda.core.storage.db.favorite.FavoriteMessageEntity
import com.etozhesandy.redpanda.core.storage.db.friend.FriendDao
import com.etozhesandy.redpanda.core.storage.db.friend.FriendEntity
import com.etozhesandy.redpanda.core.storage.db.group.GroupDao
import com.etozhesandy.redpanda.core.storage.db.group.GroupEntity
import com.etozhesandy.redpanda.core.storage.db.message.MessageDao
import com.etozhesandy.redpanda.core.storage.db.message.MessageEntity
import com.etozhesandy.redpanda.core.storage.db.message.MessageFtsEntity
import com.etozhesandy.redpanda.core.storage.db.profile.ProfileDao
import com.etozhesandy.redpanda.core.storage.db.profile.ProfileEntity
import com.etozhesandy.redpanda.core.storage.db.savedphoto.SavedPhotoDao
import com.etozhesandy.redpanda.core.storage.db.savedphoto.SavedPhotoEntity

/** The schema version, which a migration has to be written for every time it grows. */
const val DATABASE_VERSION = 8

/** File name of the app's database inside its databases directory. */
const val DATABASE_NAME = "redpanda.db"

@Database(
    entities = [
        ProfileEntity::class,
        VideoTransfer::class,
        DialogEntity::class,
        MessageEntity::class,
        MessageFtsEntity::class,
        FavoriteMessageEntity::class,
        AttachmentEntity::class,
        FriendEntity::class,
        GroupEntity::class,
        SavedPhotoEntity::class,
        ChatBookmarkEntity::class,
    ],
    version = DATABASE_VERSION,
    exportSchema = false,
)
@TypeConverters(StorageTypeConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun videoTransferDao(): VideoTransferDao
    abstract fun profileDao(): ProfileDao
    abstract fun dialogDao(): DialogDao
    abstract fun messageDao(): MessageDao
    abstract fun favoriteMessageDao(): FavoriteMessageDao
    abstract fun attachmentDao(): AttachmentDao
    abstract fun friendDao(): FriendDao
    abstract fun groupDao(): GroupDao
    abstract fun savedPhotoDao(): SavedPhotoDao
    abstract fun chatBookmarkDao(): ChatBookmarkDao
}
