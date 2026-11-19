package com.etozhesandy.redpanda.features.profile.domain.usecase

import com.etozhesandy.redpanda.core.model.Attachment
import com.etozhesandy.redpanda.features.profile.domain.repository.ProfileInfoRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/** Streams the first [limit] archive files of a profile — the strip shown on its screen. */
class ObserveArchiveFilesPreviewUseCase @Inject constructor(
    private val repository: ProfileInfoRepository,
) {
    operator fun invoke(profileId: String, limit: Int): Flow<List<Attachment>> = repository.observeArchiveFilesPreview(profileId, limit)
}
