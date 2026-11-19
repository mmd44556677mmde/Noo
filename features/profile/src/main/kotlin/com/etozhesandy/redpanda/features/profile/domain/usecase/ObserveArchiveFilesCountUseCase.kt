package com.etozhesandy.redpanda.features.profile.domain.usecase

import com.etozhesandy.redpanda.features.profile.domain.repository.ProfileInfoRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/** Streams how many archive files a profile has in all. */
class ObserveArchiveFilesCountUseCase @Inject constructor(
    private val repository: ProfileInfoRepository,
) {
    operator fun invoke(profileId: String): Flow<Int> = repository.observeArchiveFilesCount(profileId)
}
