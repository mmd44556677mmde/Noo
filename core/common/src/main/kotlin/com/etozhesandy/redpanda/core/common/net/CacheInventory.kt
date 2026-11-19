package com.etozhesandy.redpanda.core.common.net

import android.content.Context
import java.io.File

interface CacheHost {
    fun attachmentKind(url: String): String?
    fun owner(url: String): List<String?>
    fun clearThumbnails()
    fun thumbnailBytes(): Long
    fun videoComplete(key: String): Boolean
    fun discardVideo(file: File): Boolean
}

object CacheInventory {
    val categories = listOf("Photos", "Videos", "Audio", "Documents", "Other")
    val limits = listOf(5, 16, 32, 0)
    data class Entry(val category: String, val file: File?, val size: Long, val profile: String?, val chat: String?, val kind: String?)
    fun prefs(c: Context) = c.getSharedPreferences("media-retention", Context.MODE_PRIVATE)
    fun touch(c: Context, file: File) { prefs(c).edit().putLong("access:${file.name}",System.currentTimeMillis()).apply() }
    fun record(c: Context, file: File, url: String) {
        val owner = (c.applicationContext as CacheHost).owner(url)
        val p = prefs(c).edit()
        owner.forEachIndexed { i, value -> p.putString("${file.name}:$i", value) }; p.putLong("access:${file.name}",System.currentTimeMillis()).apply()
        trim(c, file)
    }
    fun entries(c: Context): List<Entry> {
        val p = prefs(c)
        val entries = categories.dropLast(1).flatMap { category ->
            File(c.cacheDir, category.lowercase()).listFiles().orEmpty().filter { it.isFile }.map {
                Entry(category, it, it.length(), p.getString("${it.name}:0", null), p.getString("${it.name}:1", null), p.getString("${it.name}:2", null))
            }
        }
        return entries + Entry("Other", null, (c.applicationContext as CacheHost).thumbnailBytes(), null, null, null)
    }
    private fun discard(c: Context, entry: Entry): Boolean {
        val file = entry.file ?: return false
        if (file.name.endsWith(".part")) return false
        val deleted = if (entry.category == "Videos") (c.applicationContext as CacheHost).discardVideo(file) else file.delete()
        if (deleted) prefs(c).edit().remove("access:${file.name}").remove("${file.name}:0").remove("${file.name}:1").remove("${file.name}:2").apply()
        return deleted
    }
    fun clear(c: Context, selected: Set<String>) {
        entries(c).filter { it.category in selected }.forEach { discard(c, it) }
        if ("Other" in selected) (c.applicationContext as CacheHost).clearThumbnails()
    }
    fun trim(c: Context, protect: File? = null) {
        val p = prefs(c); val limit = p.getInt("limit", 0) * 1024L * 1024 * 1024
        val all = entries(c); var bytes = all.sumOf { it.size }; val now = System.currentTimeMillis()
        all.filter { it.file != null && it.file != protect }.sortedBy { p.getLong("access:${it.file!!.name}",it.file.lastModified()) }.forEach {
            val days = p.getInt("${it.category}:${it.kind ?: "PROFILE"}", 0)
            if ((limit > 0 && bytes > limit || days > 0 && now - p.getLong("access:${it.file!!.name}",it.file.lastModified()) > days * 86400000L) && discard(c,it)) bytes -= it.size
        }
    }
}
