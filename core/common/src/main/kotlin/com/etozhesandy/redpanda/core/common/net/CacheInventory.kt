package com.etozhesandy.redpanda.core.common.net

import android.content.Context
import java.io.File

interface CacheHost {
    fun owner(url: String): List<String?>
    fun clearThumbnails()
    fun thumbnailBytes(): Long
    fun videoComplete(key: String): Boolean
}

object CacheInventory {
    val categories = listOf("Photos", "Videos", "Audio", "Documents", "Other")
    val limits = listOf(5, 16, 32, 0)
    data class Entry(val category: String, val file: File?, val size: Long, val profile: String?, val chat: String?, val kind: String?)
    fun prefs(c: Context) = c.getSharedPreferences("media-retention", Context.MODE_PRIVATE)
    fun record(c: Context, file: File, url: String) {
        val owner = (c.applicationContext as CacheHost).owner(url)
        val p = prefs(c).edit()
        owner.forEachIndexed { i, value -> p.putString("${file.name}:$i", value) }; p.apply()
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
    private fun removable(c: Context, entry: Entry) = entry.file?.let { !it.name.endsWith(".part") && (entry.category != "Videos" || (c.applicationContext as CacheHost).videoComplete(it.name)) } == true
    fun clear(c: Context, selected: Set<String>) {
        entries(c).filter { it.category in selected && removable(c, it) }.forEach { it.file?.delete() }
        if ("Other" in selected) (c.applicationContext as CacheHost).clearThumbnails()
    }
    fun trim(c: Context, protect: File? = null) {
        val p = prefs(c); val limit = p.getInt("limit", 0) * 1024L * 1024 * 1024
        val all = entries(c); var bytes = all.sumOf { it.size }; val now = System.currentTimeMillis()
        all.filter { it.file != protect && removable(c, it) }.sortedBy { it.file!!.lastModified() }.forEach {
            val days = p.getInt("${it.category}:${it.kind ?: "PROFILE"}", 0)
            if ((limit > 0 && bytes > limit || days > 0 && now - it.file!!.lastModified() > days * 86400000L) && it.file!!.delete()) bytes -= it.size
        }
    }
}
