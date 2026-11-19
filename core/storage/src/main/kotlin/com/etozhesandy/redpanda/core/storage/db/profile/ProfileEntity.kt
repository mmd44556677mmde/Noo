package com.etozhesandy.redpanda.core.storage.db.profile

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.etozhesandy.redpanda.core.model.ProfileStatus
import com.etozhesandy.redpanda.core.model.Sex
import com.etozhesandy.redpanda.core.model.SourceType

/**
 * [displayName] is the name the archive gave the profile and is only ever written by the import.
 * [customName] and [notes] are the reader's own: kept in columns the import never builds a value for,
 * so that a profile can be renamed and annotated while it is still being imported, and the import
 * carries them over when it writes the row again.
 */
@Entity(tableName = "profiles")
data class ProfileEntity(
    @PrimaryKey val id: String,
    val sourceType: SourceType,
    val displayName: String,
    val avatarPath: String?,
    val rootDirPath: String,
    val status: ProfileStatus,
    val importedAt: Long,
    val updatedAt: Long,
    val vkId: String? = null,
    val screenName: String? = null,
    val birthDate: String? = null,
    val sex: Sex = Sex.UNKNOWN,
    val country: String? = null,
    val city: String? = null,
    val customName: String? = null,
    val notes: String = "",
)
