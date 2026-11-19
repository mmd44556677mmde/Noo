package com.etozhesandy.redpanda.features.profile.domain.usecase

import com.etozhesandy.redpanda.features.profile.domain.repository.ProfileInfoRepository
import javax.inject.Inject

/**
 * Gives a profile the name the reader chose for it. Surrounding spaces are dropped, and a blank
 * name takes the choice back, so the profile shows the name from its archive again.
 */
class RenameProfileUseCase @Inject constructor(
    private val repository: ProfileInfoRepository,
) {
    suspend operator fun invoke(profileId: String, name: String) =
        repository.setCustomName(profileId, name.trim().takeIf { it.isNotEmpty() })
}
