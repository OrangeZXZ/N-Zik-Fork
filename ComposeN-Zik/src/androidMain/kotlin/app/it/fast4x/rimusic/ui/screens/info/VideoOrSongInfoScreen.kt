package app.it.fast4x.rimusic.ui.screens.info

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import app.it.fast4x.rimusic.enums.NavRoutes
import app.it.fast4x.rimusic.ui.components.themed.Loader
import app.n_zik.android.R
import app.n_zik.android.colorPalette
import app.n_zik.android.core.coil.ImageCacheFactory
import app.n_zik.android.typography
import app.n_zik.android.uiRoundnessShape
import it.fast4x.innertube.Innertube
import it.fast4x.innertube.models.VideoOrSongInfo
import it.fast4x.innertube.requests.songInfo
import timber.log.Timber

@Composable
fun VideoOrSongInfoScreen(
    videoId: String,
    songTitle: String = "",
    songArtist: String = "",
    songThumbnailUrl: String = "",
    albumId: String = "",
    albumTitle: String = "",
    navController: NavController? = null,
    onNavigateUp: () -> Unit,
    onClose: () -> Unit,
    onPlay: (() -> Unit)? = null
) {
    if (videoId.isBlank()) return

    var info by remember { mutableStateOf<VideoOrSongInfo?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(videoId) {
        isLoading = true
        try {
            val result = Innertube.songInfo(videoId)
            if (result != null && result.isSuccess) {
                info = result.getOrNull()
            }
        } catch (e: Exception) {
            Timber.e(e, "VideoOrSongInfo exception")
        }
        isLoading = false
    }

    val displayTitle = info?.title?.takeIf { it.isNotBlank() } ?: songTitle
    val displayArtist = info?.author?.takeIf { it.isNotBlank() } ?: songArtist
    val displayThumbnail = songThumbnailUrl.takeIf { it.isNotBlank() }
    val displayAuthorId = info?.authorId

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (isLoading) 300.dp else 700.dp) // Limits the height so it behaves like a bottom sheet
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .background(colorPalette().background0)
    ) {
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Loader()
            }
        } else {
            // ── Top Section (Separated Header) ───────────────────────────────────
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colorPalette().background1)
            ) {
                // Arrow down to close
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = colorPalette().textSecondary,
                    modifier = Modifier
                        .padding(top = 10.dp)
                        .size(24.dp)
                        .clickable { onClose() }
                )

                // ── Main Info: Cover + Title + Artist + Album ─────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Pochette
                    if (displayThumbnail != null) {
                        ImageCacheFactory.AsyncImage(
                            thumbnailUrl = displayThumbnail,
                            contentDescription = displayTitle,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(100.dp) // Slightly smaller for the menu header
                                .clip(uiRoundnessShape())
                        )
                        Spacer(Modifier.width(16.dp))
                    }

                    // Text Infos
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.Center
                    ) {
                        // Titre + Flèche
                        InfoRow(
                            label = stringResource(R.string.title),
                            value = displayTitle,
                            showArrow = onPlay != null,
                            onClick = {
                                if (onPlay != null) {
                                    onPlay()
                                }
                            }
                        )
                        
                        Spacer(Modifier.height(8.dp))
                        
                        // Artiste + Flèche
                        InfoRow(
                            label = stringResource(R.string.artists),
                            value = displayArtist,
                            showArrow = displayAuthorId != null,
                            onClick = { 
                                navController?.navigate("${NavRoutes.artist.name}/$displayAuthorId") 
                            }
                        )

                        // Album + Flèche
                        if (albumId.isNotBlank()) {
                            Spacer(Modifier.height(8.dp))
                            InfoRow(
                                label = stringResource(R.string.sort_album),
                                value = albumTitle.takeIf { it.isNotBlank() } ?: stringResource(R.string.sort_album),
                                showArrow = true,
                                onClick = { 
                                    navController?.navigate("${NavRoutes.album.name}/$albumId") 
                                }
                            )
                        }
                    }
                }
                
                HorizontalDivider(Modifier.height(1.dp))
            }

            // ── Body ───────────────────────────────────────────────────────────
            LazyColumn(
                state = rememberLazyListState(),
                modifier = Modifier.fillMaxWidth().weight(1f, fill = false),
                contentPadding = PaddingValues(
                    top = 16.dp,
                    start = 16.dp,
                    end = 16.dp,
                    bottom = 32.dp
                )
            ) {
                // ── Stats card ────────────────────────────────────────────────────
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        shape = uiRoundnessShape(),
                        colors = CardDefaults.cardColors(
                            containerColor = colorPalette().accent.copy(alpha = 0.2f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                            .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            StatItem(label = stringResource(R.string.subscribers), value = info?.subscribers ?: "-")
                            StatItem(label = stringResource(R.string.views), value = info?.viewCount ?: "-")
                            StatItem(label = stringResource(R.string.likes), value = info?.like ?: "-")
                            StatItem(label = stringResource(R.string.dislikes), value = info?.dislike ?: "-")
                        }
                    }
                }

                // ── Description ───────────────────────────────────────────────────
                val desc = info?.description
                item {
                    Column(modifier = Modifier.padding(bottom = 8.dp)) {
                        Text(
                            text = stringResource(R.string.description),
                            style = typography().m,
                            color = colorPalette().accent,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Text(
                            text = if (!desc.isNullOrBlank()) desc else stringResource(R.string.no_description_available),
                            style = typography().xs,
                            color = colorPalette().text,
                            fontStyle = if (desc.isNullOrBlank()) androidx.compose.ui.text.font.FontStyle.Italic else null
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String,
    showArrow: Boolean,
    onClick: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(enabled = showArrow && onClick != null) { onClick?.invoke() }
            .padding(vertical = 4.dp, horizontal = 4.dp)
    ) {
        Text(
            text = label,
            style = typography().xs,
            color = colorPalette().accent
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = value,
                style = typography().xs,
                color = colorPalette().text,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (showArrow) {
                Icon(
                    painter = painterResource(R.drawable.chevron_forward),
                    contentDescription = null,
                    tint = colorPalette().accent,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun StatItem(
    label: String,
    value: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 4.dp)
    ) {
        Text(
            text = value,
            style = typography().xxs,
            color = colorPalette().text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = label,
            style = typography().xxs,
            color = colorPalette().text,
        )
    }
}
