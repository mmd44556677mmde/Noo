package com.etozhesandy.redpanda.features.dialogs.domain.usecase

import com.etozhesandy.redpanda.core.settings.AppSettings
import com.etozhesandy.redpanda.core.settings.SettingsRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/** Streams the app settings this feature takes its defaults from, such as sort orders and thumbnail width. */
class ObserveAppSettingsUseCase @Inject constructor(
    private val repository: SettingsRepository,
) {
    operator fun invoke(): Flow<AppSettings> = repository.settings
}
