package app.n_zik.android.components.menu.artist

import android.content.Intent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import app.it.fast4x.rimusic.cleanPrefix
import app.it.fast4x.rimusic.enums.MenuStyle
import app.it.fast4x.rimusic.ui.components.LocalMenuState
import app.it.fast4x.rimusic.ui.components.MenuState
import app.it.fast4x.rimusic.ui.components.tab.toolbar.Button
import app.it.fast4x.rimusic.ui.components.tab.toolbar.Clickable
import app.it.fast4x.rimusic.ui.components.tab.toolbar.Descriptive
import app.it.fast4x.rimusic.ui.components.tab.toolbar.Menu
import app.it.fast4x.rimusic.ui.components.tab.toolbar.MenuIcon
import app.it.fast4x.rimusic.ui.components.themed.IconButton
import app.it.fast4x.rimusic.ui.styling.Dimensions
import app.it.fast4x.rimusic.ui.styling.favoritesIcon
import app.it.fast4x.rimusic.utils.conditional
import app.it.fast4x.rimusic.utils.disableScrollingTextKey
import app.it.fast4x.rimusic.utils.menuStyleKey
import app.it.fast4x.rimusic.utils.rememberPreference
import app.it.fast4x.rimusic.utils.semiBold
import app.n_zik.android.components.menu.GridMenu
import app.n_zik.android.components.menu.ListMenu
import app.n_zik.android.R
import app.n_zik.android.colorPalette
import app.n_zik.android.components.artist.ChangeArtistTitleDialog
import app.n_zik.android.components.artist.ChangeArtistCoverDialog
import app.n_zik.android.core.coil.ImageCacheFactory
import app.n_zik.android.core.database.Database
import app.n_zik.android.uiRoundnessShape
import app.n_zik.android.thumbnailShape
import app.n_zik.android.typography
import app.it.fast4x.rimusic.utils.secondary
import it.fast4x.innertube.Innertube
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import app.it.fast4x.rimusic.utils.asSong
import app.it.fast4x.rimusic.utils.asMediaItem
import app.it.fast4x.rimusic.utils.forcePlayAtIndex
import app.kreate.android.me.knighthat.utils.Toaster
import app.n_zik.android.LocalPlayerServiceBinder

@UnstableApi
@OptIn(ExperimentalFoundationApi::class)
class OnlineArtistItemMenu private constructor(
    private val navController: NavController,
    private val artist: Innertube.ArtistItem,
    override val menuState: MenuState,
    styleState: MutableState<MenuStyle>
) : Menu {

    companion object {
        @Composable
        operator fun invoke(navController: NavController, artist: Innertube.ArtistItem): OnlineArtistItemMenu =
            OnlineArtistItemMenu(
                navController = navController,
                artist = artist,
                menuState = LocalMenuState.current,
                styleState = rememberPreference(menuStyleKey, MenuStyle.List)
            )
    }

    lateinit var buttons: List<Button>
    override var menuStyle: MenuStyle by styleState

    @Composable
    override fun ListMenu() = ListMenu.Menu(showDragHandle = false) {
        buttons.forEach {
            if (it is MenuIcon) it.ListMenuItem()
        }
    }

    @Composable
    override fun GridMenu() = GridMenu.Menu(showDragHandle = false) {
        items(buttons, Button::hashCode) {
            if (it is MenuIcon) it.GridMenuItem()
        }
    }

    @Composable
    private fun ArtistItemDisplay(
        title: String?,
        thumbnailUrl: String?,
        subscribersCount: String?,
        modifier: Modifier = Modifier
    ) {
        val context = LocalContext.current
        val disableScrollingText by rememberPreference(disableScrollingTextKey, false)

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = modifier
                .fillMaxWidth()
                .background(colorPalette().background1)
        ) {
            Box(
                modifier = Modifier
                    .padding(top = 18.dp, bottom = 6.dp)
                    .size(width = 40.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White)
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        vertical = Dimensions.itemsVerticalPadding,
                        horizontal = 16.dp
                    )
            ) {
                // Artist's thumbnail
                Box(
                    Modifier.size(Dimensions.thumbnails.album / 2)
                ) {
                    ImageCacheFactory.Thumbnail(
                        thumbnailUrl = thumbnailUrl,
                        modifier = Modifier
                            .size(Dimensions.thumbnails.album / 2)
                            .clip(thumbnailShape())
                    )
                }

                // Artist's information
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    BasicText(
                        text = cleanPrefix(title ?: ""),
                        style = typography().xs.semiBold.copy(
                            color = colorPalette().text,
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .conditional(!disableScrollingText) { basicMarquee(iterations = Int.MAX_VALUE) }
                    )

                    subscribersCount?.let {
                        if (it.isNotBlank()) {
                            BasicText(
                                text = it,
                                style = typography().xxs.semiBold.secondary.copy(
                                    color = colorPalette().textSecondary,
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .conditional(!disableScrollingText) { basicMarquee(iterations = Int.MAX_VALUE) }
                            )
                        }
                    }
                }

                // Trailing content (Bookmark & Share)
                val isFollowing by remember(artist.key) {
                    Database.artistTable
                        .isFollowing(artist.key)
                        .distinctUntilChanged()
                }.collectAsState(false, Dispatchers.IO)

                Column(
                    Modifier.width(48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    IconButton(
                        icon = if (isFollowing) R.drawable.bookmark else R.drawable.bookmark_outline,
                        color = colorPalette().favoritesIcon,
                        onClick = {
                            CoroutineScope(Dispatchers.IO).launch {
                                Database.artistTable.toggleFollow(artist.key)
                            }
                        },
                        modifier = Modifier
                            .padding(all = 4.dp)
                            .size(20.dp)
                    )

                    IconButton(
                        icon = R.drawable.share_social,
                        color = colorPalette().text,
                        onClick = {
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, "https://music.youtube.com/channel/${artist.key}")
                            }
                            context.startActivity(Intent.createChooser(intent, null))
                        },
                        modifier = Modifier
                            .padding(all = 4.dp)
                            .size(20.dp)
                    )
                }
            }

            HorizontalDivider(Modifier.height(1.dp))
        }
    }

    @Composable
    override fun MenuComponent() {
        val dbArtist by app.n_zik.android.core.database.Database.artistTable.findById(artist.key).collectAsState(initial = null, context = kotlinx.coroutines.Dispatchers.IO)

        var displayTitle by remember { mutableStateOf(artist.info?.name) }
        var displayThumbnailUrl by remember { mutableStateOf(artist.thumbnail?.url) }

        val artistProvider = {
            dbArtist ?: app.it.fast4x.rimusic.models.Artist(
                id = artist.key,
                name = displayTitle,
                thumbnailUrl = displayThumbnailUrl,
                timestamp = System.currentTimeMillis(),
                isYoutubeArtist = true
            )
        }
        val changeTitle = ChangeArtistTitleDialog(artistProvider)
        val changeCover = ChangeArtistCoverDialog(artistProvider)

        LaunchedEffect(dbArtist) {
            dbArtist?.let {
                displayTitle = it.name.takeIf { !it.isNullOrBlank() } ?: displayTitle
                displayThumbnailUrl = it.thumbnailUrl.takeIf { !it.isNullOrBlank() } ?: displayThumbnailUrl
            }
        }

        val binder = LocalPlayerServiceBinder.current

        var artistPage by remember { mutableStateOf<it.fast4x.innertube.requests.ArtistPage?>(null) }
        var isFetching by remember { mutableStateOf(true) }

        LaunchedEffect(artist.key) {
            withContext(kotlinx.coroutines.Dispatchers.IO) {
                artistPage = it.fast4x.innertube.YtMusic.getArtistPage(artist.key).getOrNull()
            }
            isFetching = false
        }

        val playRadio = object : app.it.fast4x.rimusic.ui.components.tab.toolbar.MenuIcon, app.it.fast4x.rimusic.ui.components.tab.toolbar.Descriptive, app.it.fast4x.rimusic.ui.components.tab.toolbar.Clickable {
            override val iconId: Int = R.drawable.radio
            override val color: androidx.compose.ui.graphics.Color
                @Composable
                get() = if (binder?.isRadioActive == true) app.n_zik.android.colorPalette().accent else app.n_zik.android.colorPalette().text
            override val messageId: Int = R.string.start_radio
            @get:Composable override val menuIconTitle: String get() = stringResource(binder?.radioActionTextRes ?: messageId)
            override fun onShortClick() {
                if (isFetching) {
                    Toaster.w(R.string.opening_url)
                } else {
                    val allMediaItems = mutableListOf<MediaItem>()
                    artistPage?.sections?.forEach { section ->
                        section.items.forEach { item ->
                            if (item is Innertube.SongItem) {
                                item.asSong?.asMediaItem?.let { allMediaItems.add(it) }
                            } else if (item is Innertube.VideoItem) {
                                allMediaItems.add(item.asMediaItem)
                            }
                        }
                    }
                    if (allMediaItems.isNotEmpty()) {
                        binder?.stopRadio()
                        binder?.player?.forcePlayAtIndex(allMediaItems, 0)
                        menuState.hide()
                    } else {
                        Toaster.e(R.string.no_song_found)
                    }
                }
            }
            override fun onLongClick() {}
        }

        buttons = mutableListOf<app.it.fast4x.rimusic.ui.components.tab.toolbar.Button>().apply {
            add(playRadio)
            add(changeTitle)
            add(changeCover)
        }

        changeTitle.Render()
        changeCover.Render()

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(colorPalette().background0)
        ) {
            ArtistItemDisplay(
                title = displayTitle,
                thumbnailUrl = displayThumbnailUrl,
                subscribersCount = artist.subscribersCountText
            )

            if (menuStyle == MenuStyle.List)
                ListMenu()
            else
                GridMenu()
        }
    }
}

