package app.it.fast4x.rimusic.enums

import androidx.annotation.StringRes
import app.n_zik.android.R
import app.kreate.android.me.knighthat.enums.TextView

enum class WallpaperType(@StringRes override val textId: Int) : TextView {
    Home(R.string.wallpaper_home),
    Lockscreen(R.string.wallpaper_lockscreen),
    Both(R.string.wallpaper_both);
}



