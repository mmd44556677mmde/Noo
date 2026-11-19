package com.etozhesandy.redpanda.core.common.net

import java.io.File

interface VideoTransfers {
    suspend fun downloadVideo(url: String, confirmed: Boolean, progress: (Long, Long) -> Unit): File
}
