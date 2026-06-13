package app.n_zik.android.components.artist

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import app.it.fast4x.rimusic.MODIFIED_PREFIX
import app.it.fast4x.rimusic.cleanPrefix
import app.it.fast4x.rimusic.models.Artist
import app.n_zik.android.components.RenameDialog
import app.kreate.android.me.knighthat.utils.Toaster
import app.n_zik.android.R
import app.n_zik.android.core.database.Database
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ChangeArtistCoverDialog private constructor(
    activeState: MutableState<Boolean>,
    valueState: MutableState<TextFieldValue>,
    private val getArtist: () -> Artist?
) : RenameDialog(activeState, valueState) {

    companion object {
        @Composable
        operator fun invoke( getArtist: () -> Artist? ): ChangeArtistCoverDialog =
            ChangeArtistCoverDialog(
                remember { mutableStateOf(false) },
                remember {
                    mutableStateOf( TextFieldValue(cleanPrefix(getArtist()?.thumbnailUrl ?: "")) )
                },
                getArtist
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
        value = TextFieldValue(cleanPrefix(getArtist()?.thumbnailUrl ?: ""))
    }

    override fun onSet( newValue: String ) {
        super.onSet( newValue )
        if( errorMessage.isNotEmpty() ) return

        val artist = getArtist() ?: return
        Database.asyncTransaction {
            artistTable.insertIgnore( artist )
            artistTable.update( artist.copy(thumbnailUrl = "$MODIFIED_PREFIX$newValue") )
            Toaster.done()
        }
        hideDialog()
    }
}
