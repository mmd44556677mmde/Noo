package com.etozhesandy.redpanda.features.settings.domain.usecase

import com.etozhesandy.redpanda.core.settings.AppSettings
import com.etozhesandy.redpanda.core.settings.SettingsRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/** Streams the app settings, emitting again whenever any of them changes. */
class ObserveSettingsUseCase @Inject constructor(
    private val repository: SettingsRepository,
) {
    operator fun invoke(): Flow<AppSettings> = repository.settings
}
