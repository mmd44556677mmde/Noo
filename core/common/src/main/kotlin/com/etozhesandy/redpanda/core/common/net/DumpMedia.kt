package com.etozhesandy.redpanda.core.common.net

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

object DumpMedia {
    enum class Policy { WIFI, ALWAYS, ASK, NEVER }
    private fun prefs(c: Context) = c.getSharedPreferences("remote-media", Context.MODE_PRIVATE)
    fun policy(c: Context) = Policy.valueOf(prefs(c).getString("policy", null) ?: Policy.WIFI.name)
    fun setPolicy(c: Context, value: Policy) = prefs(c).edit().putString("policy", value.name).apply()
    private val roots = setOf("userapi.com", "vkuser.net", "vkuservideo.net", "vkuseraudio.net", "vk-cdn.net")
    private val locks = Array(64) { Mutex() }
    fun client(context: Context) = OkHttpClient.Builder().followRedirects(false).followSslRedirects(false)
        .addInterceptor { chain ->
            val req = chain.request()
            val u = req.url
            val permitted = when (policy(context)) {
                Policy.NEVER -> false
                Policy.ASK -> req.tag() == "confirmed"
                Policy.ALWAYS -> true
                Policy.WIFI -> context.getSystemService(ConnectivityManager::class.java).let { cm ->
                    cm.getNetworkCapabilities(cm.activeNetwork)?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
                }
            }
            if (!permitted || !u.isHttps || u.port != 443 || u.username.isNotEmpty() || u.password.isNotEmpty() ||
                roots.none { u.host == it || u.host.endsWith(".$it") }) throw IOException("Remote media blocked")
            chain.proceed(req)
        }.build()
    fun file(c: Context, url: String): File {
        val key = MessageDigest.getInstance("SHA-256").digest(url.toByteArray()).joinToString("") { "%02x".format(it) }
        return File(c.cacheDir, "photos/$key")
    }
    suspend fun fetchPhoto(c: Context, url: String, confirmed: Boolean) = withContext(Dispatchers.IO) {
        val target = file(c, url)
        locks[(url.hashCode() and Int.MAX_VALUE) % locks.size].withLock {
            if (!target.isFile) {
                target.parentFile!!.mkdirs()
                val tmp = File(target.path + ".part")
                try {
                    val req = Request.Builder().url(url).tag(if (confirmed) "confirmed" else null).build()
                    client(c).newCall(req).execute().use { res ->
                        if (!res.isSuccessful) throw IOException("HTTP ${res.code}")
                        tmp.outputStream().use { res.body.byteStream().copyTo(it) }
                    }
                    check(tmp.renameTo(target))
                } finally { tmp.delete() }
            }
        }
    }
}
