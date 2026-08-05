package app.it.fast4x.rimusic.enums

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import app.n_zik.android.R
import app.kreate.android.me.knighthat.enums.TextView

enum class AlbumSortBy(
    @field:StringRes override val textId: Int,
    @field:DrawableRes override val iconId: Int
): Drawable, TextView {

    Title( R.string.sort_name, R.drawable.text ),

    Artist( R.string.sort_artist, R.drawable.artist ),

    Songs( R.string.sort_songs_number, R.drawable.medical ),

    Duration( R.string.sort_duration, R.drawable.time ),

    PlayCount( R.string.sort_play_count, R.drawable.play ),

    ListeningTime( R.string.sort_listening_time, R.drawable.trending ),

    DateAdded( R.string.sort_date_added, R.drawable.time ),

    Year( R.string.sort_album_year, R.drawable.calendar ),

    Custom( R.string.sort_custom_order, R.drawable.position );
}




