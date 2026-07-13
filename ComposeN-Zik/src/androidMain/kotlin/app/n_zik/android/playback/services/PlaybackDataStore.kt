package app.n_zik.android.playback.services

import android.content.Context
import timber.log.Timber

/**
 * Persists stream client data per videoId in SharedPreferences.
 * Survives app cache wipes (SharedPreferences lives in filesDir, not cacheDir).
 * Used to restore playbackDataCache after process death.
 */
object PlaybackDataStore {

    private const val PREFS_NAME = "playback_data"
    private const val STREAM_CLIENT_PREFIX = "stream_client_"

    fun saveStreamClient(context: Context, videoId: String, streamClient: String) {
        runCatching {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString("$STREAM_CLIENT_PREFIX$videoId", streamClient)
                .apply()
            Timber.tag("PlaybackDataStore").d("Saved streamClient=$streamClient for videoId=$videoId")
        }.onFailure { e ->
            Timber.tag("PlaybackDataStore").e(e, "Failed to save streamClient for videoId=$videoId")
        }
    }

    fun clearStreamClients(context: Context) {
        runCatching {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val keys = prefs.all.keys.filter { it.startsWith(STREAM_CLIENT_PREFIX) }
            prefs.edit().apply {
                keys.forEach { remove(it) }
                apply()
            }
            Timber.tag("PlaybackDataStore").d("Cleared ${keys.size} streamClient entries")
        }.onFailure { e ->
            Timber.tag("PlaybackDataStore").e(e, "Failed to clear stream clients")
        }
    }

    fun loadStreamClients(context: Context): Map<String, String> {
        return runCatching {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.all
                .filterKeys { it.startsWith(STREAM_CLIENT_PREFIX) }
                .mapKeys { it.key.removePrefix(STREAM_CLIENT_PREFIX) }
                .mapValues { it.value as? String ?: "" }
                .also { Timber.tag("PlaybackDataStore").d("Loaded ${it.size} streamClient entries") }
        }.getOrElse { e ->
            Timber.tag("PlaybackDataStore").e(e, "Failed to load stream clients")
            emptyMap()
        }
    }
}
