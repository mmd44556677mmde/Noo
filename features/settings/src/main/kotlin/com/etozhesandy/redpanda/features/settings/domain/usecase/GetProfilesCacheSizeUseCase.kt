package com.etozhesandy.redpanda.features.settings.domain.usecase

import com.etozhesandy.redpanda.core.settings.cache.ProfileCacheSizeCalculator
import javax.inject.Inject

/** Totals the disk space the imported profiles occupy, for the cache size shown in the settings. */
class GetProfilesCacheSizeUseCase @Inject constructor(
    private val calculator: ProfileCacheSizeCalculator,
) {
    suspend operator fun invoke(): Long = calculator.calculateTotalBytes()
}
