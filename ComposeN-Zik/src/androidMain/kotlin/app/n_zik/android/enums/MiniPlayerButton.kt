package app.n_zik.android.enums

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import app.n_zik.android.R

enum class MiniPlayerButton(
    @DrawableRes val iconRes: Int,
    @StringRes val labelRes: Int
) {
    PlayPause(R.drawable.play, R.string.miniplayer_button_play_pause),
    SkipBack(R.drawable.play_skip_back, R.string.miniplayer_button_skip_back),
    SkipForward(R.drawable.play_skip_forward, R.string.miniplayer_button_skip_forward),
    Shuffle(R.drawable.shuffle, R.string.miniplayer_button_shuffle),
    Repeat(R.drawable.repeat, R.string.miniplayer_button_repeat),
    Like(R.drawable.heart, R.string.miniplayer_button_like),
    AddToPlaylist(R.drawable.add_in_playlist, R.string.miniplayer_button_add_to_playlist),
    Download(R.drawable.download, R.string.miniplayer_button_download),
    Share(R.drawable.share_social, R.string.miniplayer_button_share),
    Radio(R.drawable.radio, R.string.miniplayer_button_radio),
    AudioOutput(R.drawable.speaker, R.string.miniplayer_button_audio_output),
    SleepTimer(R.drawable.sleep, R.string.miniplayer_button_sleep_timer),
    Lyrics(R.drawable.song_lyrics, R.string.miniplayer_button_lyrics),
    Visualizer(R.drawable.sound_effect, R.string.miniplayer_button_visualizer),
    Queue(R.drawable.reorder, R.string.miniplayer_button_queue),
    Video(R.drawable.video, R.string.miniplayer_button_video),
    Discover(R.drawable.discover, R.string.miniplayer_button_discover)
}

enum class PendingMiniPlayerAction {
    Lyrics,
    Visualizer,
    Queue,
    SleepTimer,
    Video
}
