package app.n_zik.android.components.player

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import app.n_zik.android.R
import app.n_zik.android.colorPalette
import app.n_zik.android.components.dialog.InputDialog
import app.n_zik.android.components.dialog.TextInputDialog
import app.n_zik.android.components.dialog.InputDialogConstraints
import app.n_zik.android.core.database.Database
import app.n_zik.android.models.Lyrics

class EditLyricsDialog private constructor(
    activeState: MutableState<Boolean>,
    valueState: MutableState<TextFieldValue>,
    private val mediaId: String,
    private val isShowingSynchronizedLyrics: Boolean,
    private val getLyrics: () -> Lyrics?,
    private val ensureSongInserted: () -> Unit
) : TextInputDialog(InputDialogConstraints.ALL) {

    companion object {
        @Composable
        operator fun invoke(
            mediaId: String,
            isShowingSynchronizedLyrics: Boolean,
            getLyrics: () -> Lyrics?,
            ensureSongInserted: () -> Unit
        ): EditLyricsDialog {
            val lyrics = getLyrics()
            val initialText = if (isShowingSynchronizedLyrics) lyrics?.synced else lyrics?.fixed
            
            return EditLyricsDialog(
                remember { mutableStateOf(false) },
                remember(initialText) { mutableStateOf(TextFieldValue(initialText ?: "")) },
                mediaId,
                isShowingSynchronizedLyrics,
                getLyrics,
                ensureSongInserted
            )
        }
    }

    override var value: TextFieldValue by valueState
    override var isActive: Boolean by activeState

    override val keyboardOption: KeyboardOptions = KeyboardOptions.Default
    override val dialogTitle: String
        @Composable
        get() = stringResource(R.string.enter_the_lyrics)

    fun onShortClick() = showDialog()

    override fun hideDialog() {
        super.hideDialog()
        val lyrics = getLyrics()
        val text = if (isShowingSynchronizedLyrics) lyrics?.synced else lyrics?.fixed
        value = TextFieldValue(text ?: "")
    }

    @Composable
    override fun DialogBody() {
        androidx.compose.material3.TextField(
            value = value,
            onValueChange = { value = it },
            placeholder = { 
                androidx.compose.material3.Text(
                    text = stringResource(R.string.enter_the_lyrics),
                    color = colorPalette().textDisabled
                ) 
            },
            maxLines = Int.MAX_VALUE,
            singleLine = false,
            keyboardOptions = KeyboardOptions.Default,
            modifier = Modifier.fillMaxWidth().heightIn(
                max = (LocalConfiguration.current.screenHeightDp * 0.4f).dp
            ),
            colors = InputDialog.defaultTextFieldColors()
        )
    }

    override fun onSet(newValue: String) {
        super.onSet(newValue)
        val lyrics = getLyrics()
        Database.asyncTransaction {
            ensureSongInserted()
            Database.lyricsTable.upsert(
                Lyrics(
                    songId = mediaId,
                    fixed = if (isShowingSynchronizedLyrics) lyrics?.fixed else newValue,
                    synced = if (isShowingSynchronizedLyrics) newValue else lyrics?.synced,
                )
            )
        }
        hideDialog()
    }
}

