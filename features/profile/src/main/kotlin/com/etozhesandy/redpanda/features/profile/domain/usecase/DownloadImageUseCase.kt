package com.etozhesandy.redpanda.features.profile.domain.usecase

import com.etozhesandy.redpanda.core.common.files.ImageDownloader
import javax.inject.Inject

/**
 * Saves an image into the public Downloads folder, under `RedPanda` and the given [folders]
 * (profile, dialog, archive folder), and returns the folder it landed in, or null if saving failed.
 */
class DownloadImageUseCase @Inject constructor(
    private val downloader: ImageDownloader,
) {
    suspend operator fun invoke(source: String, vararg folders: String?): String? =
        downloader.download(source, *folders).getOrNull()
}
