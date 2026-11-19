package com.etozhesandy.redpanda.image

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.media.MediaMetadataRetriever
import android.os.Build
import android.webkit.MimeTypeMap
import coil.ImageLoader
import coil.annotation.ExperimentalCoilApi
import coil.decode.DataSource
import coil.decode.ImageSource
import coil.disk.DiskCache
import coil.fetch.DrawableResult
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.fetch.SourceResult
import coil.request.Options
import java.io.File
import kotlin.math.max
import kotlin.math.roundToInt
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

/** Longest side of a stored preview: enough for a full-screen placeholder, small for a tile. */
private const val THUMBNAIL_MAX_SIDE_PX = 720

private const val THUMBNAIL_JPEG_QUALITY = 85

private const val THUMBNAIL_MIME_TYPE = "image/jpeg"

/**
 * Frame decodes allowed at once. Each one reads into a video that may be hundreds of megabytes;
 * run a whole screen of them together and they all finish late instead of one by one.
 */
private val decodePermits = Semaphore(2)

/**
 * A still preview of a local video file, kept in Coil's disk cache.
 *
 * Coil's own `VideoFrameDecoder` pulls the frame out of the video on every load, since its disk
 * cache only holds what came over the network — so an archive's media grid redid that work after
 * every cold start. This extracts the frame once, stores it as a small JPEG under a key that
 * changes with the file, and serves it from there afterwards, both to grid tiles and to the
 * placeholder a video player shows until its first frame.
 */
@OptIn(ExperimentalCoilApi::class)
class VideoThumbnailFetcher(
    private val file: File,
    private val options: Options,
    private val diskCache: DiskCache?,
    private val isPhoto: Boolean,
) : Fetcher {

    private val tileWidth = (options.size.width as? coil.size.Dimension.Pixels)?.px ?: THUMBNAIL_MAX_SIDE_PX
    private val tileHeight = (options.size.height as? coil.size.Dimension.Pixels)?.px ?: THUMBNAIL_MAX_SIDE_PX

    override suspend fun fetch(): FetchResult {
        val key = "tile-v2:$tileWidth:$tileHeight:${file.path}:${file.lastModified()}:${file.length()}"
        readCached(key)?.let { return it }

        return decodePermits.withPermit {
            // Another request for the same video may have stored it while this one waited.
            readCached(key) ?: (if (isPhoto) extractPhoto() else extractFrame()).let { frame -> store(key, frame) ?: frame.asResult() }
        }
    }

    private fun readCached(key: String): FetchResult? {
        val cache = diskCache ?: return null
        val snapshot = cache.openSnapshot(key) ?: return null
        return SourceResult(
            source = ImageSource(snapshot.data, cache.fileSystem, key, snapshot),
            mimeType = THUMBNAIL_MIME_TYPE,
            dataSource = DataSource.DISK,
        )
    }

    private fun store(key: String, frame: Bitmap): FetchResult? {
        val cache = diskCache ?: return null
        val editor = cache.openEditor(key) ?: return null
        try {
            cache.fileSystem.write(editor.data) {
                frame.compress(Bitmap.CompressFormat.JPEG, THUMBNAIL_JPEG_QUALITY, outputStream())
            }
            editor.commit()
        } catch (_: Exception) {
            editor.abort()
            return null
        }
        return readCached(key)
    }

    private fun extractPhoto(): Bitmap {
        val opts = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
        android.graphics.BitmapFactory.decodeFile(file.path, opts)
        require(opts.outWidth > 0 && opts.outHeight > 0)
        var sample = 1
        while (opts.outWidth / sample > tileWidth * 2 || opts.outHeight / sample > tileHeight * 2) sample *= 2
        opts.inSampleSize = sample
        opts.inJustDecodeBounds = false
        return checkNotNull(android.graphics.BitmapFactory.decodeFile(file.path, opts)).scaledDown()
    }

    private fun extractFrame(): Bitmap {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(file.path)
            val option = MediaMetadataRetriever.OPTION_CLOSEST_SYNC
            val width = retriever.extractInt(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
            val height = retriever.extractInt(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
            val isLarger = width > tileWidth || height > tileHeight
            val frame = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1 && isLarger) {
                // Square bounds: the frame keeps its aspect ratio inside them, and the reported
                // width and height ignore rotation, so a portrait clip would not fit theirs.
                retriever.getScaledFrameAtTime(0L, option, tileWidth, tileHeight)
            } else {
                if (isLarger) null else retriever.getFrameAtTime(0L, option)
            }
            return checkNotNull(frame) { "No frame could be extracted from ${file.path}" }
        } finally {
            retriever.release()
        }
    }

    private fun Bitmap.scaledDown(): Bitmap {
        val scale = minOf(tileWidth.toFloat() / width, tileHeight.toFloat() / height)
        if (scale >= 1f) return this
        return Bitmap.createScaledBitmap(this, (width * scale).roundToInt(), (height * scale).roundToInt(), true)
    }

    private fun MediaMetadataRetriever.extractInt(keyCode: Int): Int =
        extractMetadata(keyCode)?.toIntOrNull() ?: 0

    /** Without a disk cache the frame is still worth showing; it just isn't kept. */
    private fun Bitmap.asResult(): FetchResult = DrawableResult(
        drawable = BitmapDrawable(options.context.resources, this),
        isSampled = true,
        dataSource = DataSource.DISK,
    )

    class Factory : Fetcher.Factory<File> {
        override fun create(data: File, options: Options, imageLoader: ImageLoader): Fetcher? {
            val mimeType = MimeTypeMap.getSingleton()
                .getMimeTypeFromExtension(data.extension.lowercase())
            // Anything else falls through to Coil's own file fetcher.
            val photo = mimeType?.startsWith("video/") != true
            if (photo && options.parameters.value<Boolean>("tile") != true) return null
            return VideoThumbnailFetcher(data, options, imageLoader.diskCache, photo)
        }
    }
}
