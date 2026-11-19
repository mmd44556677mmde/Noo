package com.etozhesandy.redpanda.core.designsystem.utils

private val videoFileExtensions = listOf(".mp4", ".webm", ".mov", ".mkv", ".m3u8")

/**
 * The export keeps most videos as a link to a player page — VK's, YouTube's, OK's — which is HTML
 * rather than a picture or a file, so only a link that names a video file can be loaded as a tile.
 */
fun String.isVideoPageUrl(): Boolean {
    if (!startsWith("http")) return false
    val file = substringBefore('?').lowercase()
    return videoFileExtensions.none { file.endsWith(it) }
}
