package com.etozhesandy.redpanda.core.designsystem.media

import android.net.Uri
import androidx.media3.common.C
import androidx.media3.datasource.BaseDataSource
import androidx.media3.datasource.DataSpec
import java.io.File
import java.io.InterruptedIOException
import java.io.RandomAccessFile

/** ExoPlayer reads only the contiguous prefix the download manager has already written. */
internal class GrowingVideo(val file: File) {
    @Volatile var total = -1L
    @Volatile var complete = false
    @Volatile var closed = false
}

internal class GrowingVideoSource(private val transfer: GrowingVideo) : BaseDataSource(false) {
    private var reader: RandomAccessFile? = null
    private var remaining = -1L
    override fun getUri(): Uri = Uri.fromFile(transfer.file)
    override fun open(dataSpec: DataSpec): Long {
        transferInitializing(dataSpec)
        reader = RandomAccessFile(transfer.file, "r").apply { seek(dataSpec.position) }
        remaining = if (dataSpec.length >= 0) dataSpec.length else if (transfer.total >= 0) (transfer.total-dataSpec.position).coerceAtLeast(0) else -1
        transferStarted(dataSpec)
        return remaining
    }
    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        if (length == 0) return 0
        if (remaining == 0L) return C.RESULT_END_OF_INPUT
        val file = checkNotNull(reader)
        while (!transfer.closed) {
            val n = file.read(buffer, offset, if (remaining < 0) length else minOf(remaining,length.toLong()).toInt())
            if (n > 0) { if (remaining >= 0) remaining -= n; bytesTransferred(n); return n }
            if (transfer.complete) return C.RESULT_END_OF_INPUT
            try { Thread.sleep(40) } catch (_: InterruptedException) { throw InterruptedIOException() }
        }
        throw InterruptedIOException()
    }
    override fun close() { reader?.let { it.close(); transferEnded() }; reader = null }
}
