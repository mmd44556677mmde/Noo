package com.etozhesandy.redpanda.features.profile.domain.usecase

import com.etozhesandy.redpanda.core.model.Attachment
import com.etozhesandy.redpanda.features.profile.domain.repository.ProfileInfoRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/** Streams the files of one folder of a profile's archive, newest first. */
class ObserveArchiveFilesInFolderUseCase @Inject constructor(
    private val repository: ProfileInfoRepository,
) {
    operator fun invoke(profileId: String, folder: String): Flow<List<Attachment>> = repository.observeArchiveFilesInFolder(profileId, folder)
}
