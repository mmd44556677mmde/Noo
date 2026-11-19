package com.etozhesandy.redpanda.features.profile.domain.usecase

import com.etozhesandy.redpanda.core.model.Attachment
import com.etozhesandy.redpanda.features.profile.domain.repository.ProfileInfoRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/** Streams every photo attached anywhere in a profile, newest first. */
class ObserveProfileAttachmentsUseCase @Inject constructor(
    private val repository: ProfileInfoRepository,
) {
    operator fun invoke(profileId: String): Flow<List<Attachment>> = repository.observeAttachments(profileId)
}
