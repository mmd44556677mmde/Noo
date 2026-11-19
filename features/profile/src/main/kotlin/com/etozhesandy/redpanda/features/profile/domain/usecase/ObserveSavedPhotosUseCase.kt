package com.etozhesandy.redpanda.features.profile.domain.usecase

import com.etozhesandy.redpanda.core.model.SavedPhoto
import com.etozhesandy.redpanda.features.profile.domain.repository.ProfileInfoRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/** Streams the photos a profile saved on the service. */
class ObserveSavedPhotosUseCase @Inject constructor(
    private val repository: ProfileInfoRepository,
) {
    operator fun invoke(profileId: String): Flow<List<SavedPhoto>> = repository.observeSavedPhotos(profileId)
}
