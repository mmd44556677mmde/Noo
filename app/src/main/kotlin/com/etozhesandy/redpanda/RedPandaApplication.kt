package com.etozhesandy.redpanda

import coil.decode.ImageDecoderDecoder
import coil.decode.GifDecoder
import android.os.Build
import com.etozhesandy.redpanda.core.storage.db.DatabaseOpener
import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.imageLoader
import coil.decode.VideoFrameDecoder
import coil.disk.DiskCache
import com.etozhesandy.redpanda.core.common.logging.AppLogger
import com.etozhesandy.redpanda.core.settings.SettingsRepository
import com.etozhesandy.redpanda.image.VideoThumbnailFetcher
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

@HiltAndroidApp
class RedPandaApplication : Application(), Configuration.Provider, ImageLoaderFactory, com.etozhesandy.redpanda.core.common.net.VideoTransfers, com.etozhesandy.redpanda.core.common.net.CacheHost {
    @Inject lateinit var db: com.etozhesandy.redpanda.core.storage.db.AppDatabase
    override fun owner(url: String) = db.videoTransferDao().owner(url).let { listOf(it?.profile, it?.chat, it?.kind) }
    override fun clearThumbnails() { imageLoader.diskCache?.clear(); imageLoader.memoryCache?.clear() }
    override fun thumbnailBytes() = imageLoader.diskCache?.size ?: 0L
    override fun videoComplete(key: String) = db.videoTransferDao().completed(key) == true
    @Inject lateinit var videos: VideoDownloadManager
    override suspend fun downloadVideo(url: String, confirmed: Boolean, progress: (Long, Long) -> Unit) = videos.downloadVideo(url, confirmed, progress)


    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var settingsRepository: SettingsRepository

    @Inject
    lateinit var appLogger: AppLogger

    @Inject
    lateinit var databaseOpener: DatabaseOpener

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        // Follows the setting for the whole process, so toggling it applies immediately. Collected
        // asynchronously rather than read up front to keep DataStore off the cold-start path.
        // Opened here rather than by whichever screen reads first, so that a migration starts at
        // once and the update screen is on for exactly as long as it runs.
        appScope.launch { databaseOpener.open() }
        appScope.launch {
            settingsRepository.settings
                .map { it.loggingEnabled }
                .distinctUntilChanged()
                .collect(appLogger::setEnabled)
        }
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this)
            .okHttpClient { com.etozhesandy.redpanda.core.common.net.DumpMedia.client(this) }
            // The fetcher serves local videos a cached still; the decoder remains for the rest.
            // Animated images play in place of their first frame: the platform decoder from Android 9
            // (it also plays animated WebP), a software one below that.
            .components {
                add(VideoThumbnailFetcher.Factory())
                add(VideoFrameDecoder.Factory())
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
            // Coil calls this initializer lazily, off the main thread, the first time it needs the
            // disk cache — which is what makes the blocking read below acceptable. Reading the
            // setting in `newImageLoader` instead would block the main thread during cold start.
            // The size is still resolved once per process: a changed setting takes effect on the
            // next cold start.
            .diskCache {
                val cacheSizeMb = runBlocking { settingsRepository.settings.first().coilCacheSizeMb }
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(cacheSizeMb * 1024L * 1024L)
                    .build()
            }
            .build()
}
