package com.etozhesandy.redpanda.features.settings.domain.usecase

import com.etozhesandy.redpanda.core.settings.SettingsRepository
import javax.inject.Inject

/** Stores the size limit, in megabytes, of the image disk cache; it takes effect on the next cold start. */
class UpdateCoilCacheSizeUseCase @Inject constructor(
    private val repository: SettingsRepository,
) {
    suspend operator fun invoke(valueMb: Int) = repository.setCoilCacheSizeMb(valueMb)
}
