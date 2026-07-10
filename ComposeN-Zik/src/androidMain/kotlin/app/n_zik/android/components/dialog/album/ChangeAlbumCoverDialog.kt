package app.n_zik.android.components.dialog.album

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import app.it.fast4x.rimusic.MODIFIED_PREFIX
import app.it.fast4x.rimusic.cleanPrefix
import app.it.fast4x.rimusic.models.Album
import app.n_zik.android.components.dialog.song.RenameDialog
import app.kreate.android.me.knighthat.utils.Toaster
import app.n_zik.android.R
import app.n_zik.android.core.database.Database

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import androidx.compose.ui.Modifier

class ChangeAlbumCoverDialog private constructor(
    activeState: MutableState<Boolean>,
    valueState: MutableState<TextFieldValue>,
    private val getAlbum: () -> Album?
) : RenameDialog(activeState, valueState) {

    companion object {
        @Composable
        operator fun invoke( getAlbum: () -> Album? ): ChangeAlbumCoverDialog =
            ChangeAlbumCoverDialog(
                remember { mutableStateOf(false) },
                remember {
                    mutableStateOf( TextFieldValue(cleanPrefix(getAlbum()?.thumbnailUrl ?: "")) )
                },
                getAlbum
            )
    }

    override val keyboardOption: KeyboardOptions = KeyboardOptions.Default
    override val iconId: Int = R.drawable.cover_edit
    override val messageId: Int = R.string.update_cover
    override val menuIconTitle: String
        @Composable
        get() = stringResource( messageId )
    override val dialogTitle: String
        @Composable
        get() = menuIconTitle

    override fun hideDialog() {
        super.hideDialog()
        value = TextFieldValue(cleanPrefix(getAlbum()?.thumbnailUrl ?: ""))
    }

    override fun onSet( newValue: String ) {
        super.onSet( newValue )
        if( errorMessage.isNotEmpty() ) return

        val album = getAlbum() ?: return
        val newUrl = "$MODIFIED_PREFIX$newValue"
        Database.asyncTransaction {
            albumTable.insertIgnore( album )
            albumTable.updateCover( album.id, newUrl )
            songTable.updateCoverForAlbum( album.id, newUrl )
            Toaster.done()
        }

        hideDialog()
    }
    @Composable
    override fun DialogBody() {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            super.DialogBody()
            
            val context = LocalContext.current
            val launcher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.GetContent()
            ) { uri: Uri? ->
                if (uri != null) {
                    val albumId = getAlbum()?.id ?: return@rememberLauncherForActivityResult
                    val savedUri = app.it.fast4x.rimusic.utils.saveImageToInternalStorage(context, uri, "app_covers", "cover_$albumId.jpg")
                    if (savedUri != null) {
                        value = TextFieldValue(savedUri.toString())
                    }
                }
            }
            
            androidx.compose.material3.OutlinedButton(
                onClick = { launcher.launch("image/*") },
                modifier = Modifier.fillMaxWidth(),
                shape = app.n_zik.android.uiRoundnessShape(),
                colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                    contentColor = app.n_zik.android.colorPalette().text
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    app.n_zik.android.colorPalette().textSecondary
                )
            ) {
                androidx.compose.material3.Icon(
                    painter = icon,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp).padding(end = 8.dp)
                )
                androidx.compose.material3.Text(stringResource(R.string.pick_from_gallery))
            }
        }
    }
}
