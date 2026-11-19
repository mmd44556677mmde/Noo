package com.etozhesandy.redpanda.core.storage.db.profile

import com.etozhesandy.redpanda.core.model.Profile

fun ProfileEntity.toDomain(): Profile = Profile(
    id = id,
    sourceType = sourceType,
    // What the reader sees: their own name when they gave one, the archive's otherwise.
    displayName = customName?.takeIf { it.isNotBlank() } ?: displayName,
    originalName = displayName,
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
    notes = notes,
)
