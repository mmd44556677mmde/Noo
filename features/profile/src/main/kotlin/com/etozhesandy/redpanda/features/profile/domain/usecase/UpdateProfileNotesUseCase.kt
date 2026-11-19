package com.etozhesandy.redpanda.features.profile.domain.usecase

import com.etozhesandy.redpanda.features.profile.domain.repository.ProfileInfoRepository
import javax.inject.Inject

/** Stores the reader's free-text notes on a profile, replacing the previous ones. */
class UpdateProfileNotesUseCase @Inject constructor(
    private val repository: ProfileInfoRepository,
) {
    suspend operator fun invoke(profileId: String, notes: String) = repository.setNotes(profileId, notes)
}
