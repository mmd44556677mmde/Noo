package com.etozhesandy.redpanda.features.dialogs.domain.usecase

import com.etozhesandy.redpanda.core.common.importprogress.ImportProgressStore
import com.etozhesandy.redpanda.core.model.ImportProgress
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/** Streams the live counters of the import running for one profile, or null when none is running. */
class ObserveImportProgressUseCase @Inject constructor(
    private val store: ImportProgressStore,
) {
    operator fun invoke(profileId: String): Flow<ImportProgress?> = store.observe(profileId)
}
