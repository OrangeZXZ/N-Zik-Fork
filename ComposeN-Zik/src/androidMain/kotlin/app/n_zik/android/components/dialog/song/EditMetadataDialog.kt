package app.n_zik.android.components.dialog.song

import android.content.ContentUris
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import app.n_zik.android.R
import app.n_zik.android.components.dialog.common.InteractiveDialog
import app.n_zik.android.components.dialog.common.InputDialog
import app.n_zik.android.colorPalette
import app.n_zik.android.core.database.Database
import app.n_zik.android.playback.services.LOCAL_KEY_PREFIX
import app.n_zik.android.typography
import app.n_zik.android.uiRoundnessShape
import app.it.fast4x.rimusic.MODIFIED_PREFIX
import app.it.fast4x.rimusic.cleanPrefix
import app.it.fast4x.rimusic.models.Song
import app.it.fast4x.rimusic.ui.components.tab.toolbar.Descriptive
import app.it.fast4x.rimusic.ui.components.tab.toolbar.MenuIcon
import app.kreate.android.me.knighthat.utils.Toaster
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import org.jaudiotagger.tag.images.ArtworkFactory
import org.jaudiotagger.tag.mp4.Mp4FieldKey
import timber.log.Timber
import java.io.File

class EditMetadataDialog private constructor(
    activeState: MutableState<Boolean>,
    private val getSong: () -> Song?
) : InteractiveDialog, InputDialog, MenuIcon, Descriptive {

    override var isActive: Boolean by activeState

    private var fields = mutableStateListOf<EditableField>()
    private var isLoading by mutableStateOf(false)
    private var filePath: String? = null
    private var coverArtBytes: ByteArray? = null

    private var writePermissionLauncher: androidx.activity.result.ActivityResultLauncher<androidx.activity.result.IntentSenderRequest>? = null
    private var pendingTempFile: File? = null
    private var pendingMediaStoreUri: Uri? = null
    private var pendingSong: Song? = null

    override val iconId: Int = R.drawable.cover_edit
    override val messageId: Int = R.string.edit_metadata
    override val menuIconTitle: String
        @Composable
        get() = stringResource(messageId)
    override val dialogTitle: String
        @Composable
        get() = stringResource(messageId)

    override val keyboardOption: KeyboardOptions = KeyboardOptions.Default
    override var value: TextFieldValue
        get() = fields.firstOrNull()?.value ?: TextFieldValue("")
        set(newValue) { if (fields.isNotEmpty()) fields[0] = fields[0].copy(value = newValue) }

    override fun onValueChanged(newValue: String): Boolean = true
    override fun onSet(newValue: String) {}
    override fun onShortClick() = showDialog()

    data class EditableField(
        val key: String,
        val rawId: String,
        val value: TextFieldValue,
        val isStandardTag: Boolean,
        val isCoverArt: Boolean = false
    )

    companion object {
        private val TAG_LABELS = mapOf(
            "©nam" to "Title", "©ART" to "Artist", "©alb" to "Album",
            "©gen" to "Genre", "©day" to "Year", "©cmt" to "Comment",
            "©wrt" to "Composer", "©too" to "Encoder", "cprt" to "Copyright",
            "aART" to "Album Artist", "trkn" to "Track", "disk" to "Disc Number",
            "covr" to "Cover", "tmpo" to "BPM", "cpil" to "Compilation",
            "sonm" to "Title Sort", "soar" to "Artist Sort", "soal" to "Album Sort",
            "TIT2" to "Title", "TPE1" to "Artist", "TALB" to "Album",
            "TCON" to "Genre", "TDRC" to "Year", "COMM" to "Comment",
            "TPE2" to "Album Artist", "TRCK" to "Track", "TPOS" to "Disc Number",
            "TCOP" to "Copyright", "TBPM" to "BPM",
            "TCOM" to "Composer", "TENC" to "Encoder", "TSRC" to "ISRC",
            "TPUB" to "Publisher", "TEXT" to "Lyricist", "TPE3" to "Conductor",
            "TPE4" to "Remixer", "USLT" to "Lyrics",
            "TITLE" to "Title", "ARTIST" to "Artist", "ALBUM" to "Album",
            "GENRE" to "Genre", "DATE" to "Year", "COMMENT" to "Comment",
            "ALBUMARTIST" to "Album Artist", "TRACKNUMBER" to "Track",
            "DISCNUMBER" to "Disc Number", "COPYRIGHT" to "Copyright",
            "BPM" to "BPM", "COMPOSER" to "Composer", "ENCODEDBY" to "Encoder",
            "ISRC" to "ISRC", "LABEL" to "Publisher", "LYRICIST" to "Lyricist",
            "CONDUCTOR" to "Conductor", "REMIXER" to "Remixer",
        )

        private val COVER_ART_IDS = setOf("covr", "APIC", "METADATA_BLOCK_PICTURE")

        private fun labelFromKey(rawId: String): String =
            TAG_LABELS[rawId] ?: rawId

        private val LABEL_TO_STRING_RES = mapOf(
            "TITLE" to R.string.metadata_title,
            "ARTIST" to R.string.metadata_artist,
            "ALBUM" to R.string.metadata_album,
            "GENRE" to R.string.metadata_genre,
            "DATE" to R.string.metadata_year,
            "YEAR" to R.string.metadata_year,
            "TRACK" to R.string.metadata_track,
            "DISC_NUMBER" to R.string.metadata_disc_number,
            "ALBUM_ARTIST" to R.string.metadata_album_artist,
            "COMPOSER" to R.string.metadata_composer,
            "COPYRIGHT" to R.string.metadata_copyright,
            "COMMENT" to R.string.metadata_comment,
            "ENCODER" to R.string.metadata_encoder,
            "BPM" to R.string.metadata_bpm,
            "ISRC" to R.string.metadata_isrc,
            "PUBLISHER" to R.string.metadata_publisher,
            "LYRICIST" to R.string.metadata_lyricist,
            "CONDUCTOR" to R.string.metadata_conductor,
            "REMIXER" to R.string.metadata_remixer,
            "COVER" to R.string.metadata_cover,
            "COVERART" to R.string.metadata_cover,
        )

        @Composable
        private fun displayLabel(rawId: String): String {
            val key = labelFromKey(rawId)
            val resId = LABEL_TO_STRING_RES[key]
            return if (resId != null) stringResource(resId) else key
        }

        @Composable
        operator fun invoke(getSong: () -> Song?): EditMetadataDialog =
            EditMetadataDialog(remember { mutableStateOf(false) }, getSong)
    }

    private fun resolveFilePath(context: android.content.Context, songId: String): String? {
        val mediaStoreId = songId.substringAfter(LOCAL_KEY_PREFIX).toLongOrNull() ?: return null
        val uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, mediaStoreId)
        context.contentResolver.query(uri, arrayOf(MediaStore.Audio.Media.DATA), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val path = cursor.getString(0)
                if (!path.isNullOrEmpty()) return path
            }
        }
        return null
    }

    override fun showDialog() { super<InteractiveDialog>.showDialog() }
    override fun hideDialog() { isActive = false }

    @Composable
    override fun DialogBody() {
        val context = LocalContext.current
        val song = remember(isActive) { if (isActive) getSong() else null }

        val pickerLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            if (uri != null) {
                try {
                    val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    if (bytes != null) {
                        coverArtBytes = bytes
                        val idx = fields.indexOfFirst { it.isCoverArt }
                        if (idx >= 0) fields[idx] = fields[idx].copy(value = TextFieldValue("New cover selected"))
                    }
                } catch (e: Exception) {
                    Timber.tag("EditMetadata").e(e, "Failed to read cover image")
                }
            }
        }

        val writePermLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartIntentSenderForResult()
        ) { result ->
            if (result.resultCode == android.app.Activity.RESULT_OK) {
                performWriteToMediaStore(context)
            }
        }
        writePermissionLauncher = writePermLauncher

        LaunchedEffect(isActive) {
            if (!isActive || song == null) return@LaunchedEffect
            isLoading = true
            fields.clear()
            coverArtBytes = null

            val result = withContext(Dispatchers.IO) { readTagsFromFile(context, song) }

            filePath = result.path
            coverArtBytes = result.coverBytes
            fields.clear()
            fields.addAll(result.fields)
            isLoading = false
        }

        val scrollState = rememberScrollState()

        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .heightIn(max = 500.dp)
                .verticalScroll(scrollState)
        ) {
            if (isLoading) {
                Text(stringResource(R.string.metadata_loading), modifier = Modifier.fillMaxWidth().padding(8.dp))
            } else {
                val displayOrder = listOf(
                    "TITLE", "ARTIST", "ALBUM", "GENRE", "DATE", "DAY", "YEAR", "TRACK", "TRACKNUMBER",
                    "DISCNUMBER", "DISC_NUMBER", "TPOS", "ALBUM_ARTIST", "ALBUMARTIST", "COMPOSER", "TCOM",
                    "COPYRIGHT", "TCOP", "COMMENT", "COMM",
                    "ENCODER", "TENC", "ENCODEDBY", "BPM", "TBPM", "ISRC", "TSRC",
                    "LABEL", "PUBLISHER", "TPUB", "LYRICIST", "TEXT", "CONDUCTOR", "TPE3",
                    "REMIXER", "TPE4",
                    "COMPILATION", "TEMPO", "RATING", "GROUPING", "LYRICS", "USLT", "MOVEMENT"
                )
                val sortedFields = remember {
                    androidx.compose.runtime.derivedStateOf {
                        fields.sortedBy { f ->
                            when {
                                f.isCoverArt -> -2
                                f.value.text.isNotBlank() -> {
                                    val label = labelFromKey(f.rawId).uppercase()
                                    val idx = displayOrder.indexOfFirst { it.equals(f.key, ignoreCase = true) || it.equals(label, ignoreCase = true) }
                                    if (idx >= 0) idx else Int.MAX_VALUE
                                }
                                else -> Int.MAX_VALUE - 1
                            }
                        }
                    }
                }.value
                sortedFields.forEach { field ->
                    val index = fields.indexOfFirst { it.key == field.key }
                    if (field.isCoverArt) {
                        CoverArtField(
                            coverBytes = coverArtBytes,
                            onPickImage = { pickerLauncher.launch("image/*") }
                        )
                    } else {
                        EditField(
                            label = displayLabel(field.rawId),
                            fieldValue = field.value,
                            onValueChange = { newValue ->
                                fields[index] = fields[index].copy(value = newValue)
                            }
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun CoverArtField(coverBytes: ByteArray?, onPickImage: () -> Unit) {
        Row(
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clip(uiRoundnessShape())
                .background(colorPalette().background1)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            if (coverBytes != null) {
                val bitmap = remember(coverBytes) {
                    BitmapFactory.decodeByteArray(coverBytes, 0, coverBytes.size)
                }
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Cover art",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                }
            } else {
                androidx.compose.foundation.Canvas(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(colorPalette().background2)
                ) {}
            }

            Text(
                text = stringResource(R.string.pick_from_gallery),
                style = typography().xs,
                color = colorPalette().text,
                modifier = Modifier.weight(1f)
            )

            androidx.compose.material3.OutlinedButton(
                onClick = onPickImage,
                shape = uiRoundnessShape(),
                colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                    contentColor = colorPalette().accent
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, colorPalette().accent),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text(stringResource(R.string.pick_from_gallery))
            }
        }
    }

    @Composable
    private fun EditField(label: String, fieldValue: TextFieldValue, onValueChange: (TextFieldValue) -> Unit) {
        TextField(
            value = fieldValue,
            onValueChange = onValueChange,
            label = { Text(label) },
            singleLine = true,
            keyboardOptions = keyboardOption,
            modifier = Modifier.fillMaxWidth(),
            colors = InputDialog.defaultTextFieldColors()
        )
    }

    @Composable
    override fun Buttons() = Row(
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 5.dp)
    ) {
        InteractiveDialog.CancelButton(
            modifier = InteractiveDialog.ButtonModifier()
                .weight(1f).fillMaxWidth(.98f)
                .border(2.dp, androidx.compose.ui.graphics.Color(android.graphics.Color.RED).copy(alpha = .3f), uiRoundnessShape())
                .padding(vertical = 10.dp),
            onCancel = ::hideDialog
        )
        InteractiveDialog.ConfirmButton(
            modifier = InteractiveDialog.ButtonModifier()
                .weight(1f).fillMaxWidth(.98f)
                .background(colorPalette().accent)
                .padding(vertical = 10.dp),
            onConfirm = { saveToFile() }
        )
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun saveToFile() {
        val path = filePath ?: run { Toaster.e("Cannot resolve file path"); return }
        val song = getSong() ?: return

        CoroutineScope(Dispatchers.IO).launch {
            val context = app.n_zik.android.appContext()
            val cacheDir = context.cacheDir
            val originalFile = File(path)
            val ext = originalFile.extension
            val tempFile = File(cacheDir, "edit_meta_${song.id.hashCode()}.$ext")
            val mediaStoreId = song.id.substringAfter(LOCAL_KEY_PREFIX).toLongOrNull()
            val mediaStoreUri = mediaStoreId?.let {
                ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, it)
            }

            try {
                File(path).copyTo(tempFile, overwrite = true)

                val audioFile = AudioFileIO.read(tempFile)
                val tag = audioFile.tagOrCreateAndSetDefault

                val keysToDelete = mutableListOf<FieldKey>()
                val it = tag.fields
                while (it.hasNext()) {
                    val f = it.next()
                    try { keysToDelete.add(FieldKey.valueOf(f.id)) } catch (_: Exception) { }
                }
                for (key in keysToDelete) tag.deleteField(key)

                for (field in fields) {
                    if (field.isCoverArt) continue
                    val value = field.value.text.trim()
                    if (value.isEmpty()) continue
                    val fieldKey = try { FieldKey.valueOf(field.key) } catch (_: Exception) { null }
                    if (fieldKey != null) tag.setField(fieldKey, value)
                }

                coverArtBytes?.let { bytes ->
                    val artwork = ArtworkFactory.getNew()
                    artwork.binaryData = bytes
                    artwork.mimeType = "image/jpeg"
                    tag.setField(artwork)
                }

                audioFile.commit()

                if (mediaStoreUri != null) {
                    try {
                        context.contentResolver.openOutputStream(mediaStoreUri)?.use { out ->
                            tempFile.inputStream().use { inp -> inp.copyTo(out) }
                        }
                        onWriteSuccess(song, tempFile)
                    } catch (e: android.app.RecoverableSecurityException) {
                        pendingTempFile = tempFile
                        pendingMediaStoreUri = mediaStoreUri
                        pendingSong = song
                        withContext(Dispatchers.Main) {
                            writePermissionLauncher?.launch(
                                androidx.activity.result.IntentSenderRequest.Builder(e.userAction.actionIntent.intentSender).build()
                            )
                        }
                    }
                } else {
                    originalFile.outputStream().use { out ->
                        tempFile.inputStream().use { inp -> inp.copyTo(out) }
                    }
                    onWriteSuccess(song, tempFile)
                }
            } catch (e: Exception) {
                Timber.tag("EditMetadata").e(e, "Failed to write tags")
                tempFile.delete()
                withContext(Dispatchers.Main) { Toaster.e("Failed to save: ${e.message}") }
            }
        }
    }

    private fun performWriteToMediaStore(context: android.content.Context) {
        val tempFile = pendingTempFile ?: return
        val uri = pendingMediaStoreUri ?: return
        val song = pendingSong ?: return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    tempFile.inputStream().use { inp -> inp.copyTo(out) }
                }
                onWriteSuccess(song, tempFile)
            } catch (e: Exception) {
                Timber.tag("EditMetadata").e(e, "Failed to write after permission")
                tempFile.delete()
                withContext(Dispatchers.Main) { Toaster.e("Failed to save: ${e.message}") }
            } finally {
                pendingTempFile = null
                pendingMediaStoreUri = null
                pendingSong = null
            }
        }
    }

    private suspend fun onWriteSuccess(song: Song, tempFile: File) {
        tempFile.delete()

        Database.asyncTransaction {
            songTable.insertIgnore(song)
            val newTitle = fields.firstOrNull { it.key == "TITLE" }?.value?.text?.trim() ?: ""
            val newArtist = fields.firstOrNull { it.key == "ARTIST" }?.value?.text?.trim() ?: ""
            if (newTitle.isNotEmpty()) songTable.updateTitle(song.id, "$MODIFIED_PREFIX$newTitle")
            if (newArtist.isNotEmpty()) songTable.updateArtists(song.id, "$MODIFIED_PREFIX$newArtist")
        }

        withContext(Dispatchers.Main) { Toaster.done(); hideDialog() }
    }

    private data class ReadResult(
        val path: String?,
        val coverBytes: ByteArray?,
        val fields: List<EditableField>
    )

    private fun readTagsFromFile(context: android.content.Context, song: Song): ReadResult {
        val path = resolveFilePath(context, song.id)
            ?: return ReadResult(null, null, listOf(
                EditableField("TITLE", "TITLE", TextFieldValue(cleanPrefix(song.title)), true),
                EditableField("ARTIST", "ARTIST", TextFieldValue(song.artistsText?.let { cleanPrefix(it) } ?: ""), true)
            ))

        try {
            val audioFile = AudioFileIO.read(File(path))
            val tag = audioFile.tag
            val result = mutableListOf<EditableField>()
            var cover: ByteArray? = null
            val seen = mutableSetOf<String>()
            val isMp4 = tag?.javaClass?.simpleName?.contains("Mp4", ignoreCase = true) == true

            if (tag != null) {
                if (isMp4) {
                    readMp4Tags(tag, result, seen, { cover = it })
                } else {
                    tag.fields?.forEach { field ->
                        val rawId = field.id ?: return@forEach
                        if (COVER_ART_IDS.any { rawId.contains(it, ignoreCase = true) }) return@forEach
                        val value = try { field.toString().trim() } catch (_: Exception) { return@forEach }
                        if (value.isEmpty()) return@forEach
                        val key = try { FieldKey.valueOf(rawId).name } catch (_: Exception) { rawId }
                        if (rawId !in seen && key !in seen) {
                            seen += rawId; seen += key
                            result.add(EditableField(key, rawId, TextFieldValue(value), true))
                        }
                    }

                    try { tag.firstArtwork?.let { cover = it.binaryData } } catch (_: Exception) { }
                }

                Timber.tag("EditMetadata").d("Tag read: %d fields (isMp4=%s)", result.size, isMp4)
                result.forEach { Timber.tag("EditMetadata").d("  [%s] %s = %s", it.rawId, it.key, it.value.text.take(80)) }
            }

            val existingLabels = result.map { labelFromKey(it.rawId) }.toSet()
            val existingKeys = seen.toMutableSet()

            if (!isMp4) {
                try {
                    MediaMetadataRetriever().use { retriever ->
                        retriever.setDataSource(path)
                        val mmr = mapOf(
                            MediaMetadataRetriever.METADATA_KEY_TITLE to "TITLE",
                            MediaMetadataRetriever.METADATA_KEY_ARTIST to "ARTIST",
                            MediaMetadataRetriever.METADATA_KEY_ALBUM to "ALBUM",
                            MediaMetadataRetriever.METADATA_KEY_GENRE to "GENRE",
                            MediaMetadataRetriever.METADATA_KEY_DATE to "DATE",
                            MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER to "TRACK",
                            MediaMetadataRetriever.METADATA_KEY_COMPOSER to "COMPOSER",
                        )
                        for ((mmrKey, tagKey) in mmr) {
                            if (tagKey in existingKeys) continue
                            val label = labelFromKey(tagKey)
                            if (label in existingLabels) continue
                            val value = retriever.extractMetadata(mmrKey)?.trim() ?: continue
                            if (value.isNotEmpty()) {
                                seen += tagKey; existingKeys += tagKey
                                result.add(EditableField(tagKey, tagKey, TextFieldValue(value), true))
                            }
                        }
                    }
                } catch (_: Exception) { }
            }

            result.add(EditableField("COVERART", "covr", TextFieldValue(""), true, isCoverArt = true))

            val standardFields = listOf("TITLE", "ARTIST", "ALBUM", "GENRE", "DATE", "TRACK", "DISC_NUMBER", "ALBUM_ARTIST", "COMPOSER", "COPYRIGHT", "COMMENT", "ENCODER", "BPM", "ISRC", "PUBLISHER", "LYRICIST", "CONDUCTOR", "REMIXER")
            for (key in standardFields) {
                if (key !in seen && labelFromKey(key) !in existingLabels) {
                    result.add(result.size - 1, EditableField(key, key, TextFieldValue(""), true))
                }
            }

            Timber.tag("EditMetadata").i("Total: %d fields", result.size)
            return ReadResult(path, cover, result)
        } catch (e: Exception) {
            Timber.tag("EditMetadata").e(e, "Failed to read tags")
            return ReadResult(path, null, listOf(
                EditableField("TITLE", "TITLE", TextFieldValue(cleanPrefix(song.title)), true),
                EditableField("ARTIST", "ARTIST", TextFieldValue(song.artistsText?.let { cleanPrefix(it) } ?: ""), true)
            ))
        }
    }

    private fun readMp4Tags(
        tag: org.jaudiotagger.tag.Tag,
        result: MutableList<EditableField>,
        seen: MutableSet<String>,
        onCover: (ByteArray) -> Unit
    ) {
        val mp4Tag = tag as? org.jaudiotagger.tag.mp4.Mp4Tag
        if (mp4Tag != null) {
            val mp4KeyMap = Mp4FieldKey.entries.associateBy { it.name }

            val logicalOrder = listOf(
                "TITLE", "ARTIST", "ALBUM", "GENRE", "DAY", "TRACK",
                "DISCNUMBER", "ALBUM_ARTIST", "COMPOSER", "COPYRIGHT", "COMMENT",
                "ENCODER", "BPM", "ISRC", "LABEL", "LYRICIST", "CONDUCTOR", "REMIXER",
                "COMPILATION", "TEMPO", "RATING", "GROUPING", "LYRICS", "MOVEMENT"
            )

            for (key in logicalOrder) {
                val mp4Key = mp4KeyMap[key] ?: continue
                if (mp4Key == Mp4FieldKey.ARTWORK) continue
                val atomId = mp4Key.fieldName
                if (mp4Key.name in seen || atomId in seen) continue

                try {
                    val value = mp4Tag.getFirst(mp4Key)?.trim()
                    seen += mp4Key.name
                    seen += atomId
                    if (!value.isNullOrEmpty()) {
                        result.add(EditableField(mp4Key.name, atomId, TextFieldValue(value), true))
                    }
                } catch (_: Exception) { }
            }

            for (mp4Key in Mp4FieldKey.entries) {
                if (mp4Key == Mp4FieldKey.ARTWORK) continue
                val atomId = mp4Key.fieldName
                if (mp4Key.name in seen || atomId in seen) continue

                try {
                    val value = mp4Tag.getFirst(mp4Key)?.trim()
                    if (!value.isNullOrEmpty()) {
                        seen += mp4Key.name
                        seen += atomId
                        result.add(EditableField(mp4Key.name, atomId, TextFieldValue(value), true))
                    }
                } catch (_: Exception) { }
            }

            try {
                val art = mp4Tag.firstArtwork
                if (art != null) onCover(art.binaryData)
            } catch (_: Exception) { }
        } else {
            tag.fields?.forEach { field ->
                val rawId = field.id ?: return@forEach
                if (COVER_ART_IDS.any { rawId.contains(it, ignoreCase = true) }) return@forEach
                val value = try { field.toString().trim() } catch (_: Exception) { return@forEach }
                if (value.isEmpty()) return@forEach
                val key = try { FieldKey.valueOf(rawId).name } catch (_: Exception) { rawId }
                if (rawId !in seen && key !in seen) {
                    seen += rawId; seen += key
                    result.add(EditableField(key, rawId, TextFieldValue(value), true))
                }
            }
            try { tag.firstArtwork?.let { onCover(it.binaryData) } } catch (_: Exception) { }
        }
    }
}

