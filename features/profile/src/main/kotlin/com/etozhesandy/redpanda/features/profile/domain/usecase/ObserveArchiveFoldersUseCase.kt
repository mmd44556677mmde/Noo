package com.etozhesandy.redpanda.features.profile.domain.usecase

import com.etozhesandy.redpanda.features.profile.domain.model.ArchiveFolder
import com.etozhesandy.redpanda.features.profile.domain.repository.ProfileInfoRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/** Streams the folders of a profile's archive, each with its file count and a preview. */
class ObserveArchiveFoldersUseCase @Inject constructor(
    private val repository: ProfileInfoRepository,
) {
    operator fun invoke(profileId: String): Flow<List<ArchiveFolder>> = repository.observeArchiveFolders(profileId)
}
