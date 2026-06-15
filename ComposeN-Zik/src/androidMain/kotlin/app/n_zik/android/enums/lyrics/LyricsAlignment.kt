package app.n_zik.android.enums.lyrics

import androidx.compose.ui.text.style.TextAlign


// TODO: Remove this
enum class LyricsAlignment {
    Left,
    Center,
    Right;

    val selected: TextAlign
        get() = when (this) {
            Left -> TextAlign.Start
            Center -> TextAlign.Center
            Right -> TextAlign.End
        }
}



