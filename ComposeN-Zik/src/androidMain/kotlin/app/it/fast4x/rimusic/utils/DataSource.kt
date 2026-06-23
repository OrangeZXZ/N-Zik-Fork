package app.it.fast4x.rimusic.utils

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.PlaybackException
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.HttpDataSource.InvalidResponseCodeException
import androidx.media3.datasource.TransferListener
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import app.n_zik.android.R
import app.n_zik.android.appContext
import it.fast4x.innertube.utils.ProxyPreferences
import it.fast4x.innertube.utils.getProxy
import okhttp3.OkHttpClient
import java.time.Duration
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty
import app.n_zik.android.playback.exceptions.UnplayableException
import app.n_zik.android.playback.exceptions.UnknownException


@UnstableApi
class RangeHandlerDataSourceFactory(private val parent: DataSource.Factory) : DataSource.Factory {
    class Source(private val parent: DataSource) : DataSource by parent {
        @OptIn(UnstableApi::class)
        override fun open(dataSpec: DataSpec) = runCatching {
            parent.open(dataSpec)
        }.getOrElse { e ->
            if (e.cause is InvalidResponseCodeException && (e.cause as InvalidResponseCodeException).responseCode == 416) parent.open(
                dataSpec
                    .withRequestHeaders(
                        dataSpec.httpRequestHeaders.filter {
                            it.key.equals("range", ignoreCase = true)
                        }
                    )
            )
            else throw e
        }
    }

    override fun createDataSource() = Source(parent.createDataSource())
}

@UnstableApi
class CatchingDataSourceFactory(private val parent: DataSource.Factory) : DataSource.Factory {
    class Source(private val parent: DataSource) : DataSource by parent {
        @OptIn(UnstableApi::class)
        override fun open(dataSpec: DataSpec) = runCatching {
            parent.open(dataSpec)
        }.getOrElse {
            it.printStackTrace()

            if (it is InvalidResponseCodeException && it.responseCode == 403) throw UnplayableException()
            if (it.cause is InvalidResponseCodeException && (it.cause as InvalidResponseCodeException).responseCode == 403) throw UnplayableException()
            if (it is UnplayableException) throw it
            if (it is UnknownException) throw it
            
            if (it is PlaybackException) throw it
            else throw PlaybackException(
                appContext().resources.getString(R.string.unknown_playback_error),
                it,
                PlaybackException.ERROR_CODE_UNSPECIFIED
            )
        }
    }

    override fun createDataSource() = Source(parent.createDataSource())
}

@OptIn(UnstableApi::class)
fun DataSource.Factory.handleRangeErrors(): DataSource.Factory = RangeHandlerDataSourceFactory(this)

@OptIn(UnstableApi::class)
fun DataSource.Factory.handleCatchingErrors(): DataSource.Factory = CatchingDataSourceFactory(this)

val Context.defaultDataSourceFactory
    @OptIn(UnstableApi::class)
    get() = DefaultDataSource.Factory(
        this,
        DefaultHttpDataSource.Factory().setConnectTimeoutMs(16000)
            .setReadTimeoutMs(8000)
            .apply {
                it.fast4x.innertube.Innertube.cookie?.let {
                    setDefaultRequestProperties(mapOf("Cookie" to it))
                }
            }
    )

val Context.okHttpDataSourceFactory
    @OptIn(UnstableApi::class)
    get() = DefaultDataSource.Factory(
        this,
        OkHttpDataSource.Factory { request -> okHttpClient().newCall(request) }
            .apply {
                val headers = mutableMapOf<String, String>()
                it.fast4x.innertube.Innertube.cookie?.let {
                    headers["Cookie"] = it
                }
                setDefaultRequestProperties(headers)
            }
    )

private fun okHttpClient(): OkHttpClient {
    return app.n_zik.android.core.network.client.NetworkClientFactory.getCachelessClient()
}


@OptIn(UnstableApi::class)
class ConditionalCacheDataSourceFactory(
    private val cacheDataSourceFactory: CacheDataSource.Factory,
    private val upstreamDataSourceFactory: DataSource.Factory,
    private val shouldCache: (DataSpec) -> Boolean
) : DataSource.Factory {
    init {
        cacheDataSourceFactory.setUpstreamDataSourceFactory(upstreamDataSourceFactory)
    }

    override fun createDataSource() = object : DataSource {
        private lateinit var selectedFactory: DataSource.Factory
        private val transferListeners = mutableListOf<TransferListener>()

        private fun createSource(factory: DataSource.Factory = selectedFactory) =
            factory.createDataSource().apply {
                transferListeners.forEach { addTransferListener(it) }
            }

        private val open = AtomicBoolean(false)
        private var source by object : ReadWriteProperty<Any?, DataSource> {
            var s: DataSource? = null

            override fun getValue(thisRef: Any?, property: KProperty<*>) = s ?: run {
                val newSource = runCatching {
                    createSource()
                }.getOrElse {
                    if (it is UninitializedPropertyAccessException) throw PlaybackException(
                        /* message = */ "Illegal access of data source methods before calling open()",
                        /* cause = */ it,
                        /* errorCode = */ PlaybackException.ERROR_CODE_UNSPECIFIED
                    ) else throw it
                }
                s = newSource
                newSource
            }

            override fun setValue(thisRef: Any?, property: KProperty<*>, value: DataSource) {
                s = value
            }
        }

        override fun read(buffer: ByteArray, offset: Int, length: Int) =
            source.read(buffer, offset, length)

        override fun addTransferListener(transferListener: TransferListener) {
            if (::selectedFactory.isInitialized) source.addTransferListener(transferListener)

            transferListeners += transferListener
        }

        override fun open(dataSpec: DataSpec): Long {
            selectedFactory =
                if (shouldCache(dataSpec)) cacheDataSourceFactory else upstreamDataSourceFactory

            return runCatching {
                // Source is still considered 'open' even when an error occurs. See DataSource::close
                open.set(true)
                source.open(dataSpec)
            }.getOrElse {
                if (it is ReadOnlyException) {
                    source = createSource(upstreamDataSourceFactory)
                    source.open(dataSpec)
                } else throw it
            }
        }

        override fun getUri() = if (open.get()) source.uri else null
        override fun close() = if (open.compareAndSet(true, false)) {
            source.close()
        } else Unit
    }
}


