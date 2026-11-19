package com.etozhesandy.redpanda.core.model

/**
 * An imported chat archive, scoped to its own directory under `filesDir/profiles/$id`.
 *
 * [displayName] is the name to show: the one the reader gave the profile, or [originalName] — the
 * name from the archive — when they have not. [notes] is their free-text note on the profile.
 */
data class Profile(
    val id: String,
    val sourceType: SourceType,
    val displayName: String,
    val originalName: String = displayName,
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
    val notes: String = "",
)
