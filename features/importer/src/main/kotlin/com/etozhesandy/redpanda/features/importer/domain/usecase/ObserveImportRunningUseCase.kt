package com.etozhesandy.redpanda.features.importer.domain.usecase

import com.etozhesandy.redpanda.core.archive.worker.ProfileImportScheduler
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/** Streams whether an import is queued or running; imports are one at a time, so this gates new ones. */
class ObserveImportRunningUseCase @Inject constructor(
    private val scheduler: ProfileImportScheduler,
) {
    operator fun invoke(): Flow<Boolean> = scheduler.observeImportRunning()
}
