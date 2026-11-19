package com.etozhesandy.redpanda.features.profile.domain.usecase

import com.etozhesandy.redpanda.core.model.Attachment
import com.etozhesandy.redpanda.features.profile.domain.repository.ProfileInfoRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/** Streams the newest [limit] photos of a profile — the strip shown on its screen. */
class ObserveProfileAttachmentsPreviewUseCase @Inject constructor(
    private val repository: ProfileInfoRepository,
) {
    operator fun invoke(profileId: String, limit: Int): Flow<List<Attachment>> = repository.observeAttachmentsPreview(profileId, limit)
}
