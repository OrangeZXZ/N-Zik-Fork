package app.n_zik.android.components.player.lyrics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import app.it.fast4x.rimusic.enums.ColorPaletteMode
import app.it.fast4x.rimusic.enums.LyricsAlignment
import app.it.fast4x.rimusic.enums.LyricsBackground
import app.it.fast4x.rimusic.enums.LyricsColor
import app.it.fast4x.rimusic.enums.LyricsFontSize
import app.it.fast4x.rimusic.enums.LyricsHighlight
import app.it.fast4x.rimusic.enums.LyricsOutline
import app.it.fast4x.rimusic.enums.Romanization
import app.it.fast4x.rimusic.utils.SynchronizedLyrics
import app.it.fast4x.rimusic.utils.verticalFadingEdge
import app.n_zik.android.colorPalette
import dev.rebelonion.translator.Language
import dev.rebelonion.translator.Translator
import it.fast4x.lrclib.LrcLib
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
fun SyncedLyricsView(
    text: String,
    currentPositionProvider: () -> Long,
    onSeekTo: (Long) -> Unit,
    showlyricsthumbnail: Boolean,
    isLandscape: Boolean,
    trailingContent: (@Composable () -> Unit)?,
    showBackgroundLyrics: Boolean,
    lyricsBackground: LyricsBackground,
    showSecondLine: Boolean,
    translateEnabled: Boolean,
    romanization: Romanization,
    languageDestination: Language,
    translator: Translator,
    lyricsOutline: LyricsOutline,
    colorPaletteMode: ColorPaletteMode,
    fontSize: LyricsFontSize,
    customSize: Float,
    lyricsAlignment: LyricsAlignment,
    lyricsSizeAnimate: Boolean,
    lyricsColor: LyricsColor,
    lyricsHighlight: LyricsHighlight,
    clickLyricsText: Boolean,
    thumbnailSize: Dp,
    isDisplayed: Boolean,
    onDismiss: () -> Unit,
    onInvalidLrc: (Boolean) -> Unit
) {
    val density = LocalDensity.current
    val synchronizedLyrics = remember(text) {
        val sentences = LrcLib.Lyrics(text).sentences
        
        if (sentences.isEmpty()) {
            onInvalidLrc(true)
        } else {
            onInvalidLrc(false)
        }

        run {
            SynchronizedLyrics(sentences) {
                currentPositionProvider() + 50L
            }
        }
    }

    val lazyListState = rememberLazyListState()

    LaunchedEffect(synchronizedLyrics, density) {
        val centerOffset = with(density) {
            (-thumbnailSize.div(
                if (!showlyricsthumbnail && !isLandscape) if (trailingContent == null) 2 else 1
                else if (trailingContent == null) 3 else 2
            )).roundToPx()
        }

        try {
            lazyListState.animateScrollToItem(
                index = synchronizedLyrics.index + 1,
                scrollOffset = centerOffset
            )
        } catch (e: kotlinx.coroutines.CancellationException) {
            if (!isActive) throw e
        }

        while (isActive) {
            delay(50)
            if (!synchronizedLyrics.update()) continue

            try {
                lazyListState.animateScrollToItem(
                    index = synchronizedLyrics.index + 1,
                    scrollOffset = centerOffset
                )
            } catch (e: kotlinx.coroutines.CancellationException) {
                if (!isActive) throw e
            }
        }
    }

    var modifierBG = Modifier.verticalFadingEdge()
    if (showBackgroundLyrics && showlyricsthumbnail) modifierBG =
        modifierBG.background(colorPalette().accent)

    LazyColumn(
        state = lazyListState,
        userScrollEnabled = true,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifierBG
            .background(
                if (isDisplayed && !showlyricsthumbnail) if (lyricsBackground == LyricsBackground.Black) Color.Black.copy(0.6f)
                else if (lyricsBackground == LyricsBackground.White) Color.White.copy(0.4f)
                else Color.Transparent else Color.Transparent
            )
    ) {
        item(key = "header", contentType = 0) {
            Spacer(modifier = Modifier.height(thumbnailSize))
        }
        
        itemsIndexed(
            items = synchronizedLyrics.sentences
        ) { index, sentence ->
            var translatedText by remember { mutableStateOf("") }
            val trimmedSentence = sentence.second.trim()
            
            if (showSecondLine || translateEnabled || romanization != Romanization.Off) {
                val mutState = remember { mutableStateOf("") }
                TranslateLyricsWithRomanization(
                    output = mutState,
                    textToTranslate = trimmedSentence,
                    isSync = true,
                    showSecondLine = showSecondLine,
                    romanization = romanization,
                    translateEnabled = translateEnabled,
                    translator = translator,
                    onPlaceholderDismissed = {},
                    destinationLanguage = languageDestination
                )
                translatedText = mutState.value
            } else {
                translatedText = trimmedSentence
            }

            LyricsTextPainter(
                text = translatedText,
                isSync = true,
                isCurrentIndex = index == synchronizedLyrics.index,
                showlyricsthumbnail = showlyricsthumbnail,
                lyricsOutline = lyricsOutline,
                colorPaletteMode = colorPaletteMode,
                fontSize = fontSize,
                customSize = customSize,
                lyricsAlignment = lyricsAlignment,
                lyricsSizeAnimate = lyricsSizeAnimate,
                lyricsColor = lyricsColor,
                lyricsHighlight = lyricsHighlight,
                clickLyricsText = clickLyricsText,
                onClick = {
                    if (clickLyricsText) {
                        onSeekTo(sentence.first)
                    } else {
                        onDismiss()
                    }
                }
            )
        }
        
        item(key = "footer", contentType = 0) {
            Spacer(modifier = Modifier.height(thumbnailSize))
        }
    }
}
