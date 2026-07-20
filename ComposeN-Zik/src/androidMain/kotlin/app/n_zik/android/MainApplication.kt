package app.n_zik.android

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.StrictMode
import coil3.ImageLoader
import coil3.SingletonImageLoader
import java.io.File
import timber.log.Timber

import app.it.fast4x.rimusic.utils.CaptureCrash
import app.it.fast4x.rimusic.utils.FileLoggingTree
import app.it.fast4x.rimusic.utils.discordAvatarKey
import app.it.fast4x.rimusic.utils.discordPersonalAccessTokenKey
import app.it.fast4x.rimusic.utils.discordUsernameKey
import app.it.fast4x.rimusic.utils.enableYouTubeLoginKey
import app.it.fast4x.rimusic.utils.enableYouTubeSyncKey
import app.it.fast4x.rimusic.utils.encryptedPreferences
import app.it.fast4x.rimusic.utils.getEnum
import app.it.fast4x.rimusic.utils.isDiscordBrowsingEnabledKey
import app.it.fast4x.rimusic.utils.isDiscordPresenceEnabledKey
import app.it.fast4x.rimusic.utils.isProxyEnabledKey
import app.it.fast4x.rimusic.utils.isValidIP
import app.it.fast4x.rimusic.utils.logDebugEnabledKey
import app.it.fast4x.rimusic.utils.preferences
import app.it.fast4x.rimusic.utils.proxyHostnameKey
import app.it.fast4x.rimusic.utils.proxyModeKey
import app.it.fast4x.rimusic.utils.proxyPortKey
import app.it.fast4x.rimusic.utils.useYtLoginOnlyForBrowseKey
import app.it.fast4x.rimusic.utils.ytAccountChannelHandleKey
import app.it.fast4x.rimusic.utils.ytAccountEmailKey
import app.it.fast4x.rimusic.utils.ytAccountNameKey
import app.it.fast4x.rimusic.utils.ytAccountThumbnailKey
import app.it.fast4x.rimusic.utils.ytCookieKey
import app.it.fast4x.rimusic.utils.ytDataSyncIdKey
import app.it.fast4x.rimusic.utils.ytVisitorDataKey
import app.n_zik.android.core.coil.ImageCacheFactory
import app.n_zik.android.core.network.client.NetworkClientFactory
import app.n_zik.android.core.network.client.Store
import app.n_zik.android.core.security.cipher.CipherDeobfuscator
import app.n_zik.android.core.security.cipher.PlayerConfigStore
import app.n_zik.android.core.security.cipher.PlayerDatesStore
import app.n_zik.android.download.utils.MyDownloadHelper
import app.n_zik.android.playback.services.PlayerServiceModern
import it.fast4x.innertube.utils.ProxyPreferenceItem
import it.fast4x.innertube.utils.ProxyPreferences
import java.net.Proxy
import app.n_zik.android.playback.services.prewarmPoToken
import it.fast4x.innertube.Innertube
import it.fast4x.innertube.utils.NewPipeDownloaderImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class MainApplication : Application(), SingletonImageLoader.Factory {

    override fun onCreate() {
        super.onCreate()
        Dependencies.init(this)
        migrateCredentialsToEncrypted()
        CipherDeobfuscator.initialize(this)
        PlayerConfigStore.initialize(this)
        PlayerConfigStore.scheduleStartupRefresh()
        PlayerDatesStore.initialize(this)

        // Prewarm cipher and PoToken in background to reduce first-play latency
        // Matches Metrolist's approach: staggered delays + wait for visitorData
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            // Warm cipher WebView after 1.5s (no session needed)
            kotlinx.coroutines.delay(1500)
            runCatching { CipherDeobfuscator.prewarm() }
                .onFailure { Timber.tag("MainApplication").w(it, "Cipher prewarm skipped") }
        }
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            // Warm PoToken after 2.5s, but wait for visitorData first (max 12s)
            kotlinx.coroutines.delay(2500)
            var waitedMs = 0
            while (Store.getIosVisitorData().isNullOrBlank() && waitedMs < 12_000) {
                kotlinx.coroutines.delay(500)
                waitedMs += 500
            }
            runCatching { prewarmPoToken() }
                .onFailure { Timber.tag("MainApplication").w(it, "PoToken prewarm skipped") }
        }

        val oldPolicy = StrictMode.allowThreadDiskReads()
        try {
            var proxy: Proxy? = null
            if (preferences.getBoolean(isProxyEnabledKey, false)) {
                val hostName = preferences.getString(proxyHostnameKey, null)
                val proxyPort = preferences.getInt(proxyPortKey, 8080)
                val proxyMode = preferences.getEnum(proxyModeKey, Proxy.Type.HTTP)
                if (isValidIP(hostName)) {
                    hostName?.let { hName ->
                        ProxyPreferences.preference = ProxyPreferenceItem(hName, proxyPort, proxyMode)
                        proxy = it.fast4x.innertube.utils.getProxy(ProxyPreferences.preference!!)
                    }
                }
            }
            NetworkClientFactory.configure(
                proxy = proxy,
                cacheDir = externalCacheDir ?: cacheDir
            )
            Innertube.proxy = proxy
            
            // Initialize YouTube session identifiers from Datastore
            val savedCookie = encryptedPreferences.getString(ytCookieKey, "")
            if (!savedCookie.isNullOrBlank()) {
                Innertube.cookie = savedCookie
                Innertube.visitorData = encryptedPreferences.getString(ytVisitorDataKey, "") ?: ""
                Innertube.dataSyncId = encryptedPreferences.getString(ytDataSyncIdKey, "")
            }

            runCatching {
                org.schabi.newpipe.extractor.NewPipe.init(
                    NewPipeDownloaderImpl {
                        NetworkClientFactory.getClient()
                    }
                )
            }
        } finally {
            StrictMode.setThreadPolicy(oldPolicy)
        }

        createNotificationChannels()

        /**** LOG *********/
        val logEnabled = preferences.getBoolean(logDebugEnabledKey, false)
        
        // Always create logs directory and set up crash handler
        val dir = filesDir.resolve("logs").also {
            if (it.exists()) return@also
            it.mkdir()
        }
        
        // Always set up crash handler regardless of debug mode
        Thread.setDefaultUncaughtExceptionHandler(CaptureCrash(dir.absolutePath))
        
        if (logEnabled) {
            Timber.plant(FileLoggingTree(File(dir, "N-Zik_log.txt")))
            Timber.tag("MainApplication").d("Log enabled at ${dir.absolutePath}")
        } else {
            Timber.uprootAll()
            Timber.plant(Timber.DebugTree())
        }
        /**** LOG *********/
    }

    private fun migrateCredentialsToEncrypted() {
        val keysToMigrateString = listOf(
            ytCookieKey, ytVisitorDataKey, ytDataSyncIdKey, ytAccountNameKey, ytAccountEmailKey,
            ytAccountChannelHandleKey, ytAccountThumbnailKey, discordPersonalAccessTokenKey,
            discordAvatarKey, discordUsernameKey
        )
        val keysToMigrateBoolean = listOf(
            enableYouTubeLoginKey, enableYouTubeSyncKey, useYtLoginOnlyForBrowseKey,
            isDiscordPresenceEnabledKey, isDiscordBrowsingEnabledKey
        )

        val edit = preferences.edit()
        val encryptedEdit = encryptedPreferences.edit()
        var migrated = false

        for (key in keysToMigrateString) {
            if (preferences.contains(key)) {
                val value = preferences.getString(key, null)
                if (value != null) {
                    encryptedEdit.putString(key, value)
                    edit.remove(key)
                    migrated = true
                }
            }
        }
        
        for (key in keysToMigrateBoolean) {
            if (preferences.contains(key)) {
                val value = preferences.getBoolean(key, false)
                encryptedEdit.putBoolean(key, value)
                edit.remove(key)
                migrated = true
            }
        }

        if (migrated) {
            encryptedEdit.commit()
            edit.commit()
            Timber.tag("MainApplication").i("Migrated credentials to encryptedPreferences")
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NotificationManager::class.java)

            // Channel for music player
            val playerChannel = NotificationChannel(
                PlayerServiceModern.NotificationChannelId,
                applicationContext.getString(R.string.player),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = applicationContext.getString(R.string.player)
                setShowBadge(false)
            }

            // Channel for sleep timer
            val sleepTimerChannel = NotificationChannel(
                PlayerServiceModern.SleepTimerNotificationChannelId,
                applicationContext.getString(R.string.sleep_timer),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = applicationContext.getString(R.string.sleep_timer)
                setShowBadge(false)
            }

            // Channel for downloads
            val downloadChannel = NotificationChannel(
                MyDownloadHelper.DOWNLOAD_NOTIFICATION_CHANNEL_ID,
                applicationContext.getString(R.string.download),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = applicationContext.getString(R.string.download)
                setShowBadge(false)
            }

            // Channel for sync
            val syncChannel = NotificationChannel(
                "sync_channel_id",
                applicationContext.getString(R.string.sync),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = applicationContext.getString(R.string.sync_notifications)
                setShowBadge(false)
            }

            notificationManager.createNotificationChannels(listOf(playerChannel, sleepTimerChannel, downloadChannel, syncChannel))
        }
    }

    override fun newImageLoader(context: Context): ImageLoader {
        return if (Dependencies.isInitialized) {
            ImageCacheFactory.LOADER
        } else {
            ImageLoader.Builder(context).build()
        }
    }



}

object Dependencies {
    lateinit var application: MainApplication
        private set

    val isInitialized: Boolean
        get() = ::application.isInitialized

    internal fun init(application: MainApplication) {

        this.application = application
    }
}


