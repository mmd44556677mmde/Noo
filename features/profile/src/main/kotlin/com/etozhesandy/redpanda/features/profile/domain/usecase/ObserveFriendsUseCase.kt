package com.etozhesandy.redpanda.features.profile.domain.usecase

import com.etozhesandy.redpanda.core.model.Friend
import com.etozhesandy.redpanda.features.profile.domain.repository.ProfileInfoRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/** Streams the friends of a profile as imported from its archive. */
class ObserveFriendsUseCase @Inject constructor(
    private val repository: ProfileInfoRepository,
) {
    operator fun invoke(profileId: String): Flow<List<Friend>> = repository.observeFriends(profileId)
}
