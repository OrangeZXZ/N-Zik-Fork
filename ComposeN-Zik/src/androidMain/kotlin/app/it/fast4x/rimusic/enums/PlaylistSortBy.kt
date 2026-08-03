package app.it.fast4x.rimusic.enums

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import app.n_zik.android.R
import app.kreate.android.me.knighthat.enums.TextView

enum class PlaylistSortBy(
    @field:StringRes override val textId: Int,
    @field:DrawableRes override val iconId: Int
): TextView, Drawable {
    Name( R.string.sort_name, R.drawable.text ),

    SongCount( R.string.sort_songs_number, R.drawable.medical ),

    ListeningTime( R.string.sort_listening_time, R.drawable.trending ),

    PlayCount( R.string.sort_play_count, R.drawable.play ),

    DateAdded( R.string.sort_date_added, R.drawable.calendar ),

    Custom( R.string.sort_custom_order, R.drawable.position );
}
