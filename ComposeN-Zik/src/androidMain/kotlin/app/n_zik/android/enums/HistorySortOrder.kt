package app.n_zik.android.enums

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import app.n_zik.android.R
import app.kreate.android.me.knighthat.enums.TextView
import app.it.fast4x.rimusic.enums.Drawable

enum class HistorySortOrder(
    @field:StringRes override val textId: Int,
    @field:DrawableRes override val iconId: Int
): TextView, Drawable {
    DATE(R.string.date, R.drawable.time),
    ALPHABETICAL(R.string.alphabetical, R.drawable.text),
    ARTIST(R.string.artist, R.drawable.artist)
}
