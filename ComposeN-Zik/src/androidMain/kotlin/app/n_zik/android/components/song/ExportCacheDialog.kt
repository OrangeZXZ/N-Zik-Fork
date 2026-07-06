package app.n_zik.android.components.song

import app.n_zik.android.core.database.*

import android.net.Uri
import android.provider.DocumentsContract
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.cache.CacheSpan
import app.n_zik.android.R
import app.n_zik.android.core.database.Database
import app.n_zik.android.appContext
import app.it.fast4x.rimusic.models.Song
import app.n_zik.android.playback.services.PlayerServiceModern
import app.it.fast4x.rimusic.ui.components.tab.toolbar.Descriptive
import app.it.fast4x.rimusic.ui.components.tab.toolbar.MenuIcon
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import app.n_zik.android.components.ExportToFileDialog
import app.kreate.android.me.knighthat.utils.Toaster
import timber.log.Timber
import androidx.media3.transformer.Transformer
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.Composition
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import java.io.File
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.toBitmap
import android.graphics.Bitmap
import java.io.ByteArrayOutputStream
import it.fast4x.innertube.requests.songInfo

class ExportCacheDialog(
    activeState: MutableState<Boolean>,
    valueState: MutableState<TextFieldValue>,
    launcher: ManagedActivityResultLauncher<String, Uri?>,
    private val getSong: () -> Song,
    val isExporting: MutableState<Boolean> = mutableStateOf(false)
) : ExportToFileDialog(valueState, activeState, launcher), MenuIcon, Descriptive {

    companion object {
        @UnstableApi
        private fun onExport(
            uri: Uri,
            binder: PlayerServiceModern.Binder ,
            song: Song,
            isExporting: MutableState<Boolean>
        ) = CoroutineScope( Dispatchers.IO ).launch {
            kotlinx.coroutines.withContext(Dispatchers.Main) { isExporting.value = true }
            try {
                Timber.tag("ExportCache").i("onExport triggered for song: ${song.title}")
                val contentLength =  Database.formatTable.findContentLengthOf( song.id ).first()

                val isCached = binder.cache.isCached( song.id, 0, contentLength )
                val isDownloaded = binder.downloadCache.isCached( song.id, 0, contentLength )
                Timber.tag("ExportCache").i("isCached: $isCached, isDownloaded: $isDownloaded, contentLength: $contentLength")

                if( !isCached && !isDownloaded ) {
                    Toaster.i( R.string.song_must_be_cached_or_downloaded_to_export )

                    try {
                        // Attempt to delete created file
                        DocumentsContract.deleteDocument( appContext().contentResolver, uri )
                    } catch ( _: Exception ) {}

                    kotlinx.coroutines.withContext(Dispatchers.Main) { isExporting.value = false }
                    return@launch
                }

                val cacheDir = appContext().cacheDir
                val rawFile = File(cacheDir, "temp_raw_${System.currentTimeMillis()}.m4a")
                val outFile = File(cacheDir, "temp_out_${System.currentTimeMillis()}.m4a")

                try {
                    Timber.tag("ExportCache").i("Creating raw file...")
                    val spans = (if( isCached ) binder.cache else binder.downloadCache).getCachedSpans( song.id )
                    rawFile.outputStream().use { outStream ->
                        spans.mapNotNull(CacheSpan::file).forEach { fileSpan ->
                            fileSpan.inputStream().use { it.copyTo(outStream) }
                        }
                    }
                    Timber.tag("ExportCache").i("Raw file created. Size: ${rawFile.length()} bytes")

                    val album = Database.songAlbumMapTable.findAlbumOf(song.id).first()
                    val trackPosition = Database.songAlbumMapTable.findPositionOf(song.id).first()
                    Timber.tag("ExportCache").i("Album: ${album?.title}, year: ${album?.year}, position: $trackPosition")

                    // Try to fetch song description for extra metadata (offline-safe)
                    var description: String? = null
                    try {
                        val songInfo = it.fast4x.innertube.Innertube.songInfo(song.id)?.getOrNull()
                        description = songInfo?.description
                        Timber.tag("ExportCache").i("Fetched description: ${description?.take(200)}")
                    } catch (e: Exception) {
                        Timber.tag("ExportCache").w(e, "Failed to fetch song info (offline?)")
                    }

                    // Parse description for metadata
                    val composers = mutableListOf<String>()
                    var publisher: String? = null
                    var genre: String? = null
                    description?.lines()?.forEach { line ->
                        val trimmed = line.trim()
                        when {
                            trimmed.startsWith("Composer:", ignoreCase = true) -> {
                                composers.add(trimmed.removePrefix("Composer:").trim())
                            }
                            trimmed.startsWith("℗", ignoreCase = false) || trimmed.startsWith("(P)", ignoreCase = true) -> {
                                publisher = trimmed.removePrefix("℗").removePrefix("(P)").trim()
                            }
                            trimmed.startsWith("Genre:", ignoreCase = true) -> {
                                genre = trimmed.removePrefix("Genre:").trim()
                            }
                        }
                    }
                    Timber.tag("ExportCache").i("Parsed: composers=$composers, publisher=$publisher, genre=$genre")

                    var artworkData: ByteArray? = null
                    if (!song.thumbnailUrl.isNullOrEmpty()) {
                        try {
                            val request = ImageRequest.Builder(appContext())
                                .data(song.thumbnailUrl)
                                .build()
                            val result = SingletonImageLoader.get(appContext()).execute(request)
                            if (result is SuccessResult) {
                                val bitmap = result.image.toBitmap()
                                val stream = ByteArrayOutputStream()
                                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
                                artworkData = stream.toByteArray()
                            }
                        } catch (e: Exception) {
                            Timber.tag("ExportCache").w(e, "Failed to download artwork for export")
                        }
                    }

                    val mediaMetadataBuilder = MediaMetadata.Builder()
                        .setTitle(song.title)
                        .setArtist(song.cleanArtistsText())

                    album?.title?.let { mediaMetadataBuilder.setAlbumTitle(it) }
                    album?.year?.toIntOrNull()?.let { mediaMetadataBuilder.setReleaseYear(it) }
                    trackPosition?.let { if (it >= 0) mediaMetadataBuilder.setTrackNumber(it) }

                    if (artworkData != null) {
                        mediaMetadataBuilder.setArtworkData(artworkData, MediaMetadata.PICTURE_TYPE_FRONT_COVER)
                    }

                    val mediaMetadata = mediaMetadataBuilder.build()

                    Timber.tag("ExportCache").i("MediaMetadata built. Starting Transformer on Main thread...")

                    val mediaItem = MediaItem.Builder()
                        .setUri(Uri.fromFile(rawFile))
                        .setMediaMetadata(mediaMetadata)
                        .build()

                    val editedMediaItem = androidx.media3.transformer.EditedMediaItem.Builder(mediaItem).build()

                    kotlinx.coroutines.withContext(Dispatchers.Main) {
                        Timber.tag("ExportCache").i("Building Transformer...")
                        val transformer = Transformer.Builder(appContext())
                            .setAudioMimeType(MimeTypes.AUDIO_AAC)
                            .build()

                        transformer.addListener(object : Transformer.Listener {
                            override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                                Timber.tag("ExportCache").i("Transformer onCompleted! ExportResult: $exportResult")
                                CoroutineScope(Dispatchers.IO).launch {
                                    try {
                                        // 1. Manually write tags with jaudiotagger before copying
                                        try {
                                            val audioFile = org.jaudiotagger.audio.AudioFileIO.read(outFile)
                                            val tag = audioFile.tagOrCreateAndSetDefault
                                            tag.setField(org.jaudiotagger.tag.FieldKey.TITLE, song.title)
                                            tag.setField(org.jaudiotagger.tag.FieldKey.ARTIST, song.cleanArtistsText())
                                            album?.title?.let { tag.setField(org.jaudiotagger.tag.FieldKey.ALBUM, it) }
                                            album?.year?.let { tag.setField(org.jaudiotagger.tag.FieldKey.YEAR, it) }
                                            trackPosition?.let { if (it >= 0) tag.setField(org.jaudiotagger.tag.FieldKey.TRACK, (it + 1).toString()) }
                                            tag.setField(org.jaudiotagger.tag.FieldKey.ENCODER, "Exported from N-Zik")
                                            publisher?.let { tag.setField(org.jaudiotagger.tag.FieldKey.COPYRIGHT, it) }
                                            genre?.let { tag.setField(org.jaudiotagger.tag.FieldKey.GENRE, it) }
                                            if (composers.isNotEmpty()) {
                                                tag.setField(org.jaudiotagger.tag.FieldKey.COMPOSER, composers.joinToString(", "))
                                            }

                                            if (artworkData != null) {
                                                val artwork = org.jaudiotagger.tag.images.ArtworkFactory.getNew()
                                                artwork.binaryData = artworkData
                                                artwork.mimeType = "image/jpeg"
                                                tag.setField(artwork)
                                            }
                                            audioFile.commit()
                                            Timber.tag("ExportCache").i("jaudiotagger successfully wrote tags!")
                                        } catch (e: Exception) {
                                            Timber.tag("ExportCache").e(e, "jaudiotagger failed to write tags")
                                        }

                                        // 2. Copy the tagged file to the destination SAF URI
                                        appContext().contentResolver.openOutputStream(uri)?.use { outStream ->
                                            outFile.inputStream().use { inStream ->
                                                inStream.copyTo(outStream)
                                            }
                                        }
                                        CoroutineScope(Dispatchers.Main).launch {
                                            Timber.tag("ExportCache").i("Toaster.done() called")
                                            isExporting.value = false
                                            Toaster.done()
                                        }
                                    } catch (e: Exception) {
                                        Timber.tag("ExportCache").e(e, "Export copy error")
                                        CoroutineScope(Dispatchers.Main).launch {
                                            isExporting.value = false
                                            Toaster.e(R.string.export_copy_error, e.message)
                                        }
                                    } finally {
                                        rawFile.delete()
                                        outFile.delete()
                                    }
                                }
                            }

                            override fun onError(composition: Composition, exportResult: ExportResult, exportException: ExportException) {
                                Timber.tag("ExportCache").e(exportException, "Media3 Transformer export failed")
                                CoroutineScope(Dispatchers.Main).launch {
                                    isExporting.value = false
                                    Toaster.e(R.string.export_failed, exportException.message)
                                }
                                try { DocumentsContract.deleteDocument(appContext().contentResolver, uri) } catch (_: Exception) {}
                                rawFile.delete()
                                outFile.delete()
                            }
                        })

                        try {
                            Timber.tag("ExportCache").i("Calling transformer.start()...")
                            transformer.start(editedMediaItem, outFile.absolutePath)
                            Timber.tag("ExportCache").i("transformer.start() returned successfully.")
                        } catch (e: Exception) {
                            Timber.tag("ExportCache").e(e, "Media3 Transformer start error")
                            isExporting.value = false
                            Toaster.e(R.string.export_start_error, e.message)
                            rawFile.delete()
                            outFile.delete()
                        }
                    }

                } catch (e: Exception) {
                    Timber.tag("ExportCache").e(e, "Export overall error")
                    CoroutineScope(Dispatchers.Main).launch {
                        isExporting.value = false
                        Toaster.e(R.string.export_error, e.message)
                    }
                    try { DocumentsContract.deleteDocument(appContext().contentResolver, uri) } catch (_: Exception) {}
                    rawFile.delete()
                    outFile.delete()
                }
            } catch (e: Exception) {
                Timber.tag("ExportCache").e(e, "Export init error")
                CoroutineScope(Dispatchers.Main).launch {
                    isExporting.value = false
                    Toaster.e(R.string.export_error, e.message)
                }
                try { DocumentsContract.deleteDocument(appContext().contentResolver, uri) } catch (_: Exception) {}
            }
        }

        @UnstableApi
        @Composable
        operator fun invoke(
            binder: PlayerServiceModern.Binder?,
            getSong: () -> Song
        ): ExportCacheDialog {
            val isExporting = remember { mutableStateOf(false) }
            return ExportCacheDialog(
            remember { mutableStateOf(false) },
            remember( getSong().title ) {
                mutableStateOf( TextFieldValue("${getSong().title} - ${getSong().cleanArtistsText()}") )
            },
            rememberLauncherForActivityResult(
                ActivityResultContracts.CreateDocument( "audio/mp4" )
            ) { uri ->
                // [uri] must be non-null (meaning path exists) in or
                uri ?: return@rememberLauncherForActivityResult
                // Same thing with binder
                binder ?: return@rememberLauncherForActivityResult

                onExport( uri, binder, getSong(), isExporting )
            },
            getSong,
            isExporting
        )
        }
    }

    override val extension: String = "m4a"
    override val iconId: Int = R.drawable.export_outline
    override val messageId: Int = R.string.info_export_cached_or_downloaded_song
    override val dialogTitle: String
        @Composable
        get() = stringResource( R.string.title_name_your_export )
    override val menuIconTitle: String
        @Composable
        get() = stringResource( R.string.export_cached )

    override fun onShortClick() = showDialog()

    override fun defaultFileName(): String =
        with( getSong() ) { "$title - ${cleanArtistsText()}" }
}


