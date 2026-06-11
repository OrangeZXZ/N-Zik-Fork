package app.n_zik.android.extensions.audiobar.utils

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaDataSource
import android.net.Uri
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.CacheDataSource
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import timber.log.Timber
import kotlin.math.abs

object WaveformExtractor {

    private val gson = Gson()
    private const val TARGET_SAMPLES = 100 // We want roughly 100 amplitude values for the UI

    suspend fun getOrExtractWaveform(context: Context, mediaId: String, caches: List<Cache>): List<Int>? {
        return withContext(Dispatchers.IO) {
            val cacheDir = File(context.cacheDir, "waveforms")
            if (!cacheDir.exists()) {
                cacheDir.mkdirs()
            }

            val safeMediaId = mediaId.replace("[^a-zA-Z0-9.-]".toRegex(), "_")
            val cacheFile = File(cacheDir, "${safeMediaId}.json")

            if (cacheFile.exists()) {
                try {
                    val json = cacheFile.readText()
                    val type = object : TypeToken<List<Int>>() {}.type
                    val cached: List<Int>? = gson.fromJson(json, type)
                    if (cached != null && cached.isNotEmpty()) {
                        return@withContext cached
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Failed to read cached waveform")
                }
            }

            Timber.d("WaveformExtractor: looking for $mediaId in caches.")
            
            // Look for the cache that has the entire file or enough of it
            var validCache: Cache? = null
            for (cache in caches) {
                val spans = cache.getCachedSpans(mediaId)
                if (spans.isNotEmpty()) {
                    validCache = cache
                    break
                }
            }

            if (validCache == null) {
                Timber.w("Audio file not found in any cache for $mediaId")
                return@withContext null
            }

            try {
                // Read from ExoPlayer cache without hitting the network
                val cacheDataSourceFactory = CacheDataSource.Factory()
                    .setCache(validCache)
                    .setUpstreamDataSourceFactory(null) // Only read from cache
                
                val amplitudes = extractAmplitudesNative(cacheDataSourceFactory, mediaId)
                if (amplitudes.size >= TARGET_SAMPLES - 2) {
                    cacheFile.writeText(gson.toJson(amplitudes))
                    return@withContext amplitudes
                } else {
                    Timber.w("WaveformExtractor: Extracted only ${amplitudes.size}/$TARGET_SAMPLES for $mediaId. Probably not fully cached.")
                }
            } catch (e: Exception) {
                Timber.e(e, "Native waveform extraction failed for $mediaId")
            }

            null
        }
    }

    private fun extractAmplitudesNative(dataSourceFactory: DataSource.Factory, mediaId: String): List<Int> {
        val amplitudes = mutableListOf<Int>()
        val extractor = MediaExtractor()
        var codec: MediaCodec? = null
        val mediaDataSource = ExoMediaDataSource(dataSourceFactory, mediaId)

        try {
            // Set data source to our ExoPlayer cache wrapper
            extractor.setDataSource(mediaDataSource)
            
            var audioTrackIndex = -1
            var format: MediaFormat? = null

            for (i in 0 until extractor.trackCount) {
                val trackFormat = extractor.getTrackFormat(i)
                val mime = trackFormat.getString(MediaFormat.KEY_MIME) ?: continue
                if (mime.startsWith("audio/")) {
                    audioTrackIndex = i
                    format = trackFormat
                    break
                }
            }

            if (audioTrackIndex == -1 || format == null) {
                return emptyList()
            }

            extractor.selectTrack(audioTrackIndex)
            val mime = format.getString(MediaFormat.KEY_MIME) ?: return emptyList()
            val durationUs = format.getLong(MediaFormat.KEY_DURATION)
            if (durationUs <= 0) return emptyList()

            codec = MediaCodec.createDecoderByType(mime)
            codec.configure(format, null, null, 0)
            codec.start()

            val windowDurationUs = durationUs / TARGET_SAMPLES
            var currentWindowStartUs = 0L
            var sampleSum = 0L
            var sampleCount = 0

            val bufferInfo = MediaCodec.BufferInfo()
            var isEOS = false

            while (!isEOS && amplitudes.size < TARGET_SAMPLES) {
                val inIndex = codec.dequeueInputBuffer(10000L)
                if (inIndex >= 0) {
                    val buffer = codec.getInputBuffer(inIndex)
                    val sampleSize = if (buffer != null) extractor.readSampleData(buffer, 0) else -1
                    if (sampleSize < 0) {
                        codec.queueInputBuffer(inIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                        isEOS = true
                    } else {
                        codec.queueInputBuffer(inIndex, 0, sampleSize, extractor.sampleTime, 0)
                        extractor.advance()
                    }
                }

                var outIndex = codec.dequeueOutputBuffer(bufferInfo, 10000L)
                while (outIndex >= 0) {
                    if (bufferInfo.size > 0) {
                        val outBuffer = codec.getOutputBuffer(outIndex)
                        if (outBuffer != null) {
                            outBuffer.position(bufferInfo.offset)
                            outBuffer.limit(bufferInfo.offset + bufferInfo.size)
                            
                            while (outBuffer.remaining() >= 2) {
                                val low = outBuffer.get().toInt() and 0xFF
                                val high = outBuffer.get().toInt()
                                val sample = (high shl 8) or low
                                sampleSum += abs(sample.toShort().toInt())
                                sampleCount++
                            }

                            while (bufferInfo.presentationTimeUs >= currentWindowStartUs + windowDurationUs && amplitudes.size < TARGET_SAMPLES) {
                                val avgAmplitude = if (sampleCount > 0) (sampleSum / sampleCount).toInt() else 0
                                amplitudes.add(avgAmplitude)
                                sampleSum = 0L
                                sampleCount = 0
                                currentWindowStartUs += windowDurationUs
                            }
                        }
                    }
                    codec.releaseOutputBuffer(outIndex, false)
                    outIndex = codec.dequeueOutputBuffer(bufferInfo, 10000L)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error during native waveform extraction")
        } finally {
            try { codec?.stop() } catch (e: Exception) {}
            try { codec?.release() } catch (e: Exception) {}
            try { extractor.release() } catch (e: Exception) {}
            try { mediaDataSource.close() } catch (e: Exception) {}
        }

        return amplitudes
    }

    private class ExoMediaDataSource(
        private val dataSourceFactory: DataSource.Factory,
        private val cacheKey: String
    ) : MediaDataSource() {

        private var dataSource: DataSource? = null
        private var currentPosition: Long = -1
        private var size: Long = -1

        init {
            try {
                val ds = dataSourceFactory.createDataSource()
                // A fake URI is enough since CacheDataSource uses the custom key
                val spec = DataSpec.Builder()
                    .setUri(Uri.parse("https://fake.uri"))
                    .setKey(cacheKey)
                    .build()
                size = ds.open(spec)
                ds.close()
                Timber.d("ExoMediaDataSource init: size=$size for $cacheKey")
            } catch (e: Exception) {
                Timber.e(e, "ExoMediaDataSource init failed for $cacheKey")
            }
        }

        override fun readAt(position: Long, buffer: ByteArray, offset: Int, readSize: Int): Int {
            if (readSize == 0) return 0
            try {
                var ds = this.dataSource
                if (ds == null || position != currentPosition) {
                    ds?.close()
                    ds = dataSourceFactory.createDataSource()
                    this.dataSource = ds
                    val spec = DataSpec.Builder()
                        .setUri(Uri.parse("https://fake.uri"))
                        .setKey(cacheKey)
                        .setPosition(position)
                        .build()
                    val bytesToRead = ds.open(spec)
                    if (bytesToRead == 0L) {
                        return -1
                    }
                    currentPosition = position
                }
                
                var totalRead = 0
                while (totalRead < readSize) {
                    val bytesRead = ds.read(buffer, offset + totalRead, readSize - totalRead)
                    if (bytesRead <= 0) {
                        break
                    }
                    totalRead += bytesRead
                    currentPosition += bytesRead
                }
                return if (totalRead == 0) -1 else totalRead
            } catch (e: Exception) {
                Timber.e("ExoMediaDataSource readAt failed at $position for $cacheKey: ${e.message}")
                return -1
            }
        }

        override fun getSize(): Long = size

        override fun close() {
            try {
                dataSource?.close()
            } catch (e: Exception) {}
            dataSource = null
        }
    }
}
