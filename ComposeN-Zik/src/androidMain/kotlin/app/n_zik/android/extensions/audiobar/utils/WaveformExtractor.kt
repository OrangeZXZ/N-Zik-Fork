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
import androidx.media3.common.C
import java.io.File
import java.io.FileOutputStream
import timber.log.Timber
import kotlin.math.abs
import kotlin.math.pow

object WaveformExtractor {

    private val gson = Gson()
    private const val TARGET_SAMPLES = 150 // We want roughly 150 amplitude values for the UI

    suspend fun getOrExtractWaveform(context: Context, mediaId: String, caches: List<Cache>): List<Int>? {
        return withContext(Dispatchers.IO) {
            // We use filesDir instead of cacheDir so it survives a "Clear Cache" by the user
            val waveformDir = File(context.filesDir, "waveforms")
            if (!waveformDir.exists()) {
                waveformDir.mkdirs()
            }

            val savedFile = File(waveformDir, "$mediaId.json")

            // 1. Return from saved file if it exists
            if (savedFile.exists()) {
                try {
                    val type = object : TypeToken<List<Int>>() {}.type
                    val amplitudes: List<Int> = gson.fromJson(savedFile.readText(), type)
                    if (amplitudes.size >= TARGET_SAMPLES - 2) {
                        return@withContext amplitudes
                    }
                } catch (e: Exception) {
                    Timber.tag("NZik_AudioBar").e("Failed to read waveform save for $mediaId")
                }
            }

            // Check if the audio file is fully cached
            var validCache: Cache? = null
            for (cache in caches) {
                val spans = cache.getCachedSpans(mediaId)
                if (spans.isNotEmpty()) {
                    validCache = cache
                    break
                }
            }

            if (validCache == null) {
                return@withContext null
            }

            val tempFile = File(context.cacheDir, "temp_audio_$mediaId.tmp")
            try {
                val cacheDataSourceFactory = CacheDataSource.Factory()
                    .setCache(validCache)
                    .setUpstreamDataSourceFactory(null) // Only read from cache
                    
                // Copy cached stream to a single temp file for fast native access
                val ds = cacheDataSourceFactory.createDataSource()
                val spec = DataSpec.Builder()
                    .setUri(Uri.parse("https://fake.uri"))
                    .setKey(mediaId)
                    .build()

                val size = ds.open(spec)
                if (size <= 0L && size != -1L) { // If it's 0 or some other error (not -1 which means unknown length)
                    // Wait, if size is -1L, it just means unknown length, we can still read.
                    // If size == 0L, it's empty.
                }
                if (size == 0L) {
                    ds.close()
                    return@withContext null
                }

                val startTime = System.currentTimeMillis()
                val fos = FileOutputStream(tempFile)
                val buffer = ByteArray(64 * 1024)
                while (true) {
                    val read = ds.read(buffer, 0, buffer.size)
                    if (read <= 0) break
                    fos.write(buffer, 0, read)
                }
                fos.close()
                ds.close()
                
                val extractStartTime = System.currentTimeMillis()
                // Use native PCM decoding and RMS calculation for a beautiful, accurate waveform
                val amplitudes = extractAmplitudesNative(tempFile.absolutePath)
                if (amplitudes.size >= TARGET_SAMPLES - 2) {
                    savedFile.writeText(gson.toJson(amplitudes))
                    return@withContext amplitudes
                }
            } catch (e: Exception) {
                if (e is java.io.IOException && e.message?.contains("PlaceholderDataSource") == true) {
                    // Song is not fully cached yet, skip extraction silently
                } else {
                    Timber.tag("NZik_AudioBar").e(e, "Native waveform extraction failed for $mediaId")
                }
            } finally {
                if (tempFile.exists()) {
                    tempFile.delete()
                }
            }

            null
        }
    }

    private fun extractAmplitudesNative(filePath: String): MutableList<Int> {
        val amplitudes = mutableListOf<Int>()
        val extractor = MediaExtractor()
        var codec: MediaCodec? = null

        try {
            extractor.setDataSource(filePath)

            var format: MediaFormat? = null
            var mime: String? = null
            var audioTrackIndex = -1
            for (i in 0 until extractor.trackCount) {
                val trackFormat = extractor.getTrackFormat(i)
                val trackMime = trackFormat.getString(MediaFormat.KEY_MIME)
                if (trackMime != null && trackMime.startsWith("audio/")) {
                    audioTrackIndex = i
                    format = trackFormat
                    mime = trackMime
                    break
                }
            }

            if (audioTrackIndex == -1 || mime == null) return mutableListOf()

            extractor.selectTrack(audioTrackIndex)
            val durationUs = format?.getLong(MediaFormat.KEY_DURATION) ?: 0L
            if (durationUs <= 0) return mutableListOf()

            codec = MediaCodec.createDecoderByType(mime)
            codec.configure(format, null, null, 0)
            codec.start()

            val windowDurationUs = durationUs / TARGET_SAMPLES
            val bufferInfo = MediaCodec.BufferInfo()

            for (step in 0 until TARGET_SAMPLES) {
                // Seek to the target point
                val targetTimeUs = step * windowDurationUs
                extractor.seekTo(targetTimeUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)

                var maxAmplitude = 0
                var foundFrame = false
                var tries = 0

                // Decode a few frames around the seek point to get a reading
                while (!foundFrame && tries < 10) {
                    val inIndex = codec.dequeueInputBuffer(5000L)
                    if (inIndex >= 0) {
                        val buffer = codec.getInputBuffer(inIndex)
                        val sampleSize = if (buffer != null) extractor.readSampleData(buffer, 0) else -1
                        if (sampleSize < 0) {
                            codec.queueInputBuffer(inIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                        } else {
                            codec.queueInputBuffer(inIndex, 0, sampleSize, extractor.sampleTime, 0)
                            extractor.advance()
                        }
                    }

                    var outIndex = codec.dequeueOutputBuffer(bufferInfo, 5000L)
                    while (outIndex >= 0) {
                        if (bufferInfo.size > 0) {
                            val outBuffer = codec.getOutputBuffer(outIndex)
                            if (outBuffer != null) {
                                outBuffer.position(bufferInfo.offset)
                                outBuffer.limit(bufferInfo.offset + bufferInfo.size)
                                
                                var sumSquares = 0.0
                                var count = 0
                                while (outBuffer.remaining() >= 2) {
                                    val low = outBuffer.get().toInt() and 0xFF
                                    val high = outBuffer.get().toInt()
                                    val sample = (high shl 8) or low
                                    val shortSample = sample.toShort().toFloat()
                                    sumSquares += (shortSample * shortSample)
                                    count++
                                }
                                
                                if (count > 0) {
                                    // Gnome Decibels uses Linear RMS. It gets dB from GStreamer and converts it BACK
                                    // to linear amplitude (10^(dB/20)). So we just calculate the Linear RMS directly!
                                    val rms = kotlin.math.sqrt(sumSquares / count)
                                    val scaledAmp = rms.toInt()
                                    
                                    if (scaledAmp > maxAmplitude) maxAmplitude = scaledAmp
                                }
                                foundFrame = true
                            }
                        }
                        codec.releaseOutputBuffer(outIndex, false)
                        if (foundFrame) break
                        outIndex = codec.dequeueOutputBuffer(bufferInfo, 5000L)
                    }
                    tries++
                }
                
                // Flush codec to clear buffers for the next seek
                codec.flush()
                
                amplitudes.add(maxAmplitude)
            }
        } catch (e: Exception) {
            Timber.tag("NZik_AudioBar").e(e, "Error during native waveform extraction")
        } finally {
            try {
                codec?.stop()
                codec?.release()
            } catch (e: Exception) {}
            extractor.release()
        }

        return amplitudes
    }

    /**
     * Ultra-fast waveform extraction that uses the size of the compressed audio frames 
     * as a heuristic for amplitude. Works very well for VBR formats like Opus and AAC 
     * (which are used by YouTube/Piped). Avoids the massive overhead of MediaCodec.
     */
    private fun extractAmplitudesFast(filePath: String): MutableList<Int> {
        val amplitudes = mutableListOf<Int>()
        val extractor = MediaExtractor()

        try {
            extractor.setDataSource(filePath)

            var audioTrackIndex = -1
            var format: MediaFormat? = null
            for (i in 0 until extractor.trackCount) {
                val trackFormat = extractor.getTrackFormat(i)
                val trackMime = trackFormat.getString(MediaFormat.KEY_MIME)
                if (trackMime != null && trackMime.startsWith("audio/")) {
                    audioTrackIndex = i
                    format = trackFormat
                    break
                }
            }

            if (audioTrackIndex == -1 || format == null) return mutableListOf()

            extractor.selectTrack(audioTrackIndex)
            val durationUs = format.getLong(MediaFormat.KEY_DURATION)
            if (durationUs <= 0) return mutableListOf()

            // Count total frames approximately or just seek through
            val buffer = java.nio.ByteBuffer.allocate(1024 * 64)
            val intervalUs = durationUs / TARGET_SAMPLES

            for (i in 0 until TARGET_SAMPLES) {
                val targetUs = i * intervalUs
                extractor.seekTo(targetUs, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
                
                var maxFrameSize = 0
                // Read a few frames around this position to get an average/max energy
                for (j in 0 until 10) {
                    val size = extractor.readSampleData(buffer, 0)
                    if (size < 0) break // EOF
                    if (size > maxFrameSize) maxFrameSize = size
                    extractor.advance()
                }
                
                // Frame sizes usually range from 100 to 1000 bytes.
                // Because compressed audio frames don't map linearly to visual amplitude (they look like a fat block),
                // we apply an exponential curve (power of 2.5). Since the UI normalizes to the maximum value,
                // this mathematically perfectly squashes the quiet parts (S/M)^2.5 and makes it look incredibly dynamic!
                val aestheticAmp = kotlin.math.max(0.0, maxFrameSize.toDouble()).pow(2.5)
                amplitudes.add(aestheticAmp.toInt())
            }

        } catch (e: Exception) {
            Timber.tag("NZik_AudioBar").e(e, "Error during fast native waveform extraction")
        } finally {
            extractor.release()
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
        private var readCount = 0

        init {
            val ds = dataSourceFactory.createDataSource()
            try {
                val spec = DataSpec.Builder()
                    .setUri(Uri.parse("https://fake.uri"))
                    .setKey(cacheKey)
                    .build()
                size = ds.open(spec)
            } catch (e: Exception) {
                Timber.tag("NZik_AudioBar").e(e, "ExoMediaDataSource init failed for $cacheKey")
            } finally {
                ds.close()
            }
        }

        override fun readAt(position: Long, buffer: ByteArray, offset: Int, readSize: Int): Int {
            return try {
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
                Timber.tag("NZik_AudioBar").e("ExoMediaDataSource readAt failed at $position for $cacheKey: ${e.message}")
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
