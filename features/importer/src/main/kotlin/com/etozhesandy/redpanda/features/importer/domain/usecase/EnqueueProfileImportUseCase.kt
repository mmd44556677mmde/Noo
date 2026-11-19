package com.etozhesandy.redpanda.features.importer.domain.usecase

import com.etozhesandy.redpanda.core.archive.source.ArchiveSource
import com.etozhesandy.redpanda.core.archive.worker.ProfileImportScheduler
import javax.inject.Inject

/** Queues the import of [source] into the profile [profileId] as durable background work. */
class EnqueueProfileImportUseCase @Inject constructor(
    private val scheduler: ProfileImportScheduler,
) {
    operator fun invoke(profileId: String, source: ArchiveSource) = scheduler.enqueue(profileId, source)
}
