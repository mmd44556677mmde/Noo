package com.etozhesandy.redpanda.core.storage.db.profile

import com.etozhesandy.redpanda.core.model.ProfileStatus
import com.etozhesandy.redpanda.core.model.Sex
import com.etozhesandy.redpanda.core.model.SourceType

/**
 * The columns of a profile the import writes — everything except what the reader put there.
 *
 * Updating through this rather than replacing the whole row is what keeps their custom name and
 * notes: the import can run for hours, and the profile is open for editing the whole time.
 */
data class ProfileImportFields(
    val id: String,
    val sourceType: SourceType,
    val displayName: String,
    val avatarPath: String?,
    val rootDirPath: String,
    val status: ProfileStatus,
    val importedAt: Long,
    val updatedAt: Long,
    val vkId: String?,
    val screenName: String?,
    val birthDate: String?,
    val sex: Sex,
    val country: String?,
    val city: String?,
)

fun ProfileEntity.toImportFields(): ProfileImportFields = ProfileImportFields(
    id = id,
    sourceType = sourceType,
    displayName = displayName,
    avatarPath = avatarPath,
    rootDirPath = rootDirPath,
    status = status,
    importedAt = importedAt,
    updatedAt = updatedAt,
    vkId = vkId,
    screenName = screenName,
    birthDate = birthDate,
    sex = sex,
    country = country,
    city = city,
)

/**
 * Writes what an import produced about a profile: inserts the row, or — when it is already there —
 * updates only the columns the import owns, leaving the reader's custom name and notes as they are.
 */
suspend fun ProfileDao.saveImported(profile: ProfileEntity) {
    if (insertIfAbsent(profile) == -1L) updateImportFields(profile.toImportFields())
}
