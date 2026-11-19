package com.etozhesandy.redpanda

import android.content.Context
import com.etozhesandy.redpanda.core.common.net.DumpMedia
import com.etozhesandy.redpanda.core.common.net.VideoTransfers
import com.etozhesandy.redpanda.core.storage.db.AppDatabase
import com.etozhesandy.redpanda.core.storage.db.VideoTransfer
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.IOException
import java.io.FileOutputStream
import javax.inject.Inject
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.Request

class VideoDownloadManager @Inject constructor(@ApplicationContext private val context: Context, db: AppDatabase) : VideoTransfers {
    private val dao = db.videoTransferDao()
    private val client = DumpMedia.client(context)
    private val locks = Array(32) { Mutex() }
    private fun lock(key: String) = locks[(key.hashCode() and Int.MAX_VALUE) % locks.size]
    fun discard(file: File): Boolean {
        val mutex = lock(file.name)
        if (!mutex.tryLock()) return false
        try { if (!file.delete()) return false; dao.delete(file.name); return true } finally { mutex.unlock() }
    }
    override suspend fun downloadVideo(url: String, confirmed: Boolean, progress: (Long, Long) -> Unit): File = withContext(Dispatchers.IO) {
        val key = DumpMedia.file(context, url).name
        lock(key).withLock {
            val file = File(context.cacheDir, "videos/$key")
            file.parentFile!!.mkdirs()
            val row = dao.find(key)
            val offset = file.length()
            if (file.exists() && row?.complete == true && offset == row.bytes) { com.etozhesandy.redpanda.core.common.net.CacheInventory.touch(context,file); return@withLock file }
            val request = Request.Builder().url(url).header("Accept-Encoding", "identity").tag(if (confirmed) "confirmed" else null)
            if (offset > 0) {
                request.header("Range", "bytes=$offset-")
                row?.validator?.let { request.header("If-Range", it) }
            }
            val call = client.newCall(request.build())
            coroutineScope {
                val cancel = launch(Dispatchers.Unconfined, start = CoroutineStart.UNDISPATCHED) { try { awaitCancellation() } finally { call.cancel() } }
                try {
                    call.execute().use { res ->
                        val range = Regex("bytes (\\d+)-(\\d+)/(\\d+)").matchEntire(res.header("Content-Range").orEmpty())
                        if (offset > 0 && (res.code != 206 || range?.groupValues?.get(1)?.toLongOrNull() != offset)) throw IOException("Server refused resume")
                        if (offset == 0L && res.code != 200) throw IOException("HTTP ${res.code}")
                        val total = range?.groupValues?.get(3)?.toLong() ?: res.body.contentLength()
                        val validator = res.header("ETag") ?: res.header("Last-Modified")
                        var bytes = offset
                        var checkpoint = offset
                        fun save(done: Boolean) = dao.save(VideoTransfer(key, bytes, total, validator, done))
                        save(false)
                        try {
                            FileOutputStream(file, offset > 0).use { out ->
                                val input = res.body.byteStream(); val buffer = ByteArray(65536)
                                while (true) {
                                    ensureActive()
                                    val n = input.read(buffer); if (n < 0) break
                                    out.write(buffer, 0, n); bytes += n
                                    progress(bytes, total)
                                    if (bytes - checkpoint >= 1048576) { save(false); checkpoint = bytes }
                                }
                            }
                            if (total >= 0 && bytes != total) throw IOException("Incomplete response")
                            save(true)
                            com.etozhesandy.redpanda.core.common.net.CacheInventory.record(context, file, url)
                        } catch (e: Exception) { save(false); throw e }
                    }
                } finally { cancel.cancel() }
            }
            file
        }
    }
}
