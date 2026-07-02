package app.n_zik.android

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.os.StrictMode
import android.content.Context
import coil3.SingletonImageLoader
import coil3.ImageLoader

import app.n_zik.android.R
import app.n_zik.android.core.coil.ImageCacheFactory
import app.n_zik.android.core.network.client.NetworkClientFactory
import app.n_zik.android.playback.services.PlayerServiceModern
import app.n_zik.android.download.utils.MyDownloadHelper
import app.it.fast4x.rimusic.utils.CaptureCrash
import app.it.fast4x.rimusic.utils.FileLoggingTree
import app.it.fast4x.rimusic.utils.logDebugEnabledKey
import app.it.fast4x.rimusic.utils.preferences
import app.n_zik.android.core.security.cipher.CipherDeobfuscator
import app.it.fast4x.rimusic.utils.isProxyEnabledKey
import app.it.fast4x.rimusic.utils.proxyHostnameKey
import app.it.fast4x.rimusic.utils.proxyModeKey
import app.it.fast4x.rimusic.utils.proxyPortKey
import app.it.fast4x.rimusic.utils.ytCookieKey
import app.it.fast4x.rimusic.utils.ytVisitorDataKey
import app.it.fast4x.rimusic.utils.ytDataSyncIdKey
import app.it.fast4x.rimusic.utils.isValidIP
import app.it.fast4x.rimusic.utils.getEnum
import it.fast4x.innertube.utils.ProxyPreferenceItem
import it.fast4x.innertube.utils.ProxyPreferences
import timber.log.Timber
import java.io.File
import java.net.Proxy

class MainApplication : Application(), SingletonImageLoader.Factory {

    override fun onCreate() {
        super.onCreate()
        Dependencies.init(this)
        CipherDeobfuscator.initialize(this)

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
            it.fast4x.innertube.Innertube.proxy = proxy
            
            // Initialize YouTube session identifiers from Datastore
            val savedCookie = preferences.getString(ytCookieKey, "")
            if (!savedCookie.isNullOrBlank()) {
                it.fast4x.innertube.Innertube.cookie = savedCookie
                it.fast4x.innertube.Innertube.visitorData = preferences.getString(ytVisitorDataKey, "") ?: ""
                it.fast4x.innertube.Innertube.dataSyncId = preferences.getString(ytDataSyncIdKey, "")
            }

            runCatching {
                org.schabi.newpipe.extractor.NewPipe.init(
                    it.fast4x.innertube.utils.NewPipeDownloaderImpl {
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

            notificationManager.createNotificationChannels(listOf(playerChannel, sleepTimerChannel, downloadChannel))
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


