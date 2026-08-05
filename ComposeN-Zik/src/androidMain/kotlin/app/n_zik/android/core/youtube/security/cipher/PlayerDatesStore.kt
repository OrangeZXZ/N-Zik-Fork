package app.n_zik.android.core.security.cipher

import android.content.Context
import android.util.Base64
import it.fast4x.innertube.utils.ProxyPreferences
import it.fast4x.innertube.utils.getProxy
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import timber.log.Timber
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

/**
 * Cosmetic "when did we add cipher support for this player" dates, shown in the song-details
 * sheet next to the player hash.
 *
 * Pulled **purely from a remote file** on the cipher repo — `player_dates.json` is NOT bundled
 * in the APK, so adding a date is just a push to that file and already-installed apps pick it
 * up with no APK update. A small on-disk cache makes it instant/offline on later launches.
 *
 * Deliberately decoupled from [PlayerConfigStore] and the decipher path: it is a separate file
 * old apps never fetch (so it cannot affect them), it is parsed tolerantly, and every failure
 * (no network, bad JSON, no cache yet) just yields an unknown date — playback is never touched.
 *
 * File shape — a flat map, no schemaVersion, no validation:
 *   { "959dabb2": "2026-06-12", "445213fb": "2026-06-10", ... }
 */
object PlayerDatesStore {
    private const val TAG = "NZik_CipherDates"

    private val REMOTE_URL by lazy {
        val encoded = "aHR0cHM6Ly9yYXcuZ2l0aHVidXNlcmNvbnRlbnQuY29tL1plbWVyVGVhbS96ZW1lci1jaXBoZXIvbWFzdGVyL3BsYXllcl9kYXRlcy5qc29u"
        String(Base64.decode(encoded, Base64.DEFAULT), StandardCharsets.UTF_8)
    }

    private const val CACHE_DIR = "cipher_dates"
    private const val CACHE_FILE = "player_dates.json"

    @Volatile
    private var dates: Map<String, String> = emptyMap()

    internal fun parse(text: String): Map<String, String> =
        runCatching {
            val root = Json.parseToJsonElement(text) as? JsonObject ?: return emptyMap()
            buildMap {
                for ((hash, value) in root) {
                    (value as? JsonPrimitive)?.takeIf { it.isString }?.content?.let { put(hash, it) }
                }
            }
        }.getOrDefault(emptyMap())

    fun initialize(context: Context) {
        val cache = File(File(context.filesDir, CACHE_DIR).apply { mkdirs() }, CACHE_FILE)

        dates = runCatching {
            if (cache.exists()) parse(cache.readText()) else emptyMap()
        }.getOrDefault(emptyMap())

        Thread {
            runCatching {
                val body = fetchRemote()
                val remote = parse(body)
                if (remote.isNotEmpty()) {
                    dates = remote
                    runCatching { cache.writeText(body) }
                }
            }.onFailure { Timber.tag(TAG).d("dates refresh skipped: ${it.message}") }
        }.apply { isDaemon = true; name = "PlayerDatesRefresh" }.start()
    }

    fun get(hash: String?): String? = hash?.let { dates[it] }

    private fun fetchRemote(): String {
        val url = URL(REMOTE_URL)
        val proxy = ProxyPreferences.preference?.let { getProxy(it) }
        val conn = (proxy?.let { url.openConnection(it) } ?: url.openConnection()) as HttpURLConnection
        return try {
            conn.run {
                connectTimeout = 10_000
                readTimeout = 10_000
                setRequestProperty("User-Agent", "Mozilla/5.0")
                inputStream.bufferedReader().use { it.readText() }
            }
        } finally {
            conn.disconnect()
        }
    }
}
