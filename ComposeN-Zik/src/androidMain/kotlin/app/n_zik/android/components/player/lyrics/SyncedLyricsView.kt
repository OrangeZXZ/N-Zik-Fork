package app.n_zik.android.components.player.lyrics

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.it.fast4x.rimusic.enums.ColorPaletteMode
import app.n_zik.android.enums.lyrics.LyricsAlignment
import app.n_zik.android.enums.lyrics.LyricsBackground
import app.n_zik.android.enums.lyrics.LyricsColor
import app.n_zik.android.enums.lyrics.LyricsFontSize
import app.n_zik.android.enums.lyrics.LyricsHighlight
import app.n_zik.android.enums.lyrics.LyricsOutline
import app.n_zik.android.R

import app.n_zik.android.components.player.lyrics.utils.SynchronizedLyrics
import app.it.fast4x.rimusic.utils.verticalFadingEdge
import app.kreate.android.me.knighthat.utils.Toaster
import app.n_zik.android.colorPalette
import dev.rebelonion.translator.Language
import dev.rebelonion.translator.Translator
import it.fast4x.lrclib.LrcLib
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import timber.log.Timber

/** Minimum silence duration (ms) between two lines required to show the interval indicator. */
private const val GAP_THRESHOLD_MS = 4000L

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
    romanizationEnabled: Boolean,
    languageDestination: Language,
    translator: Translator,
    lyricsOutline: LyricsOutline,
    colorPaletteMode: ColorPaletteMode,
    fontSize: LyricsFontSize,
    customSize: Float,
    lyricsAlignment: LyricsAlignment,
    lyricsSizeAnimate: Boolean,
    lyricsColor: LyricsColor,
    lyricsCustomColor: Int,
    dominantColor: Int,
    lyricsHighlight: LyricsHighlight = LyricsHighlight.None,
    isAutoScrollEnabled: Boolean = true,
    onAutoScrollEnabledChange: (Boolean) -> Unit = {},
    clickLyricsText: Boolean,
    thumbnailSize: Dp,
    isDisplayed: Boolean,
    onDismiss: () -> Unit,
    onInvalidLrc: (Boolean) -> Unit,
    showIntervalIndicator: Boolean = true
) {
    val density = LocalDensity.current
    val synchronizedLyrics = remember(text) {
        val decodedText = app.n_zik.android.components.player.lyrics.utils.HtmlDecoder.decodeHtmlEntities(text)
        val sentences = LrcLib.Lyrics(decodedText).sentences
        if (sentences.isEmpty()) onInvalidLrc(true) else onInvalidLrc(false)
        SynchronizedLyrics(sentences) { currentPositionProvider() + 50L }
    }

    // Pre-compute gap windows: for each sentence index, the gap to the next sentence (if > threshold)
    // Structure: Map<lineIndex, Pair<gapStartMs, gapEndMs>>
    val gapWindows = remember(text) {
        val sentences = synchronizedLyrics.sentences
        buildMap {
            sentences.forEachIndexed { index, sentence ->
                val startMs = sentence.first
                val nextStartMs = if (index < sentences.size - 1) sentences[index + 1].first else startMs + 10000L
                val sentenceText = sentence.second.trim()
                
                if (sentenceText.isBlank()) {
                    var currentEnd = startMs
                    val prevSentence = if (index > 0) sentences[index - 1] else null
                    if (prevSentence != null && prevSentence.second.isNotBlank()) {
                        val prevStartMs = prevSentence.first
                        val prevText = prevSentence.second.trim()
                        
                        // Estimate end of singing: ~120ms per character + 500ms trailing
                        val estimatedDuration = (prevText.length * 120L) + 500L
                        // Max end is 2 seconds before the next line starts, so we guarantee a 2s gap if possible
                        val maxEstimatedEnd = nextStartMs - 2000L
                        
                        currentEnd = (prevStartMs + estimatedDuration)
                            .coerceAtMost(maxEstimatedEnd)
                            // Minimum 1 second after the previous line started
                            .coerceAtLeast(prevStartMs + 1000L)
                            // We can even start the gap before the blank line's official timestamp!
                            .coerceAtMost(startMs)
                    }

                    if (currentEnd < nextStartMs) {
                        val gap = nextStartMs - currentEnd
                        if (gap > 2000L) {
                            put(index, Pair(currentEnd, nextStartMs - 650L))
                        }
                    }
                }
            }
        }
    }

    // Live playback position for the interval indicator
    var currentPositionMs by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        while (isActive) {
            currentPositionMs = currentPositionProvider()
            delay(100)
        }
    }

    // --- Translation cache ---
    // Keyed on options that affect translation output; survives scroll without re-fetching.
    val translationCache = remember(text, showSecondLine, translateEnabled, romanizationEnabled, languageDestination) {
        mutableStateMapOf<Int, String>()
    }

    // Pre-compute translations for every sentence in the background, once per key change.
    LaunchedEffect(text, showSecondLine, translateEnabled, romanizationEnabled, languageDestination) {
        if (!showSecondLine && !translateEnabled && !romanizationEnabled) return@LaunchedEffect

        val linesToTranslate = mutableListOf<Pair<Int, String>>()
        synchronizedLyrics.sentences.forEachIndexed { index, sentence ->
            val trimmed = sentence.second.trim()
            if (trimmed.isEmpty()) {
                translationCache[index] = trimmed
            } else if (!translationCache.containsKey(index)) {
                linesToTranslate.add(index to trimmed)
            }
        }

        if (linesToTranslate.isEmpty()) return@LaunchedEffect

        if (translateEnabled) {
            withContext(Dispatchers.Main) {
                Toaster.i(R.string.translation_in_progress)
            }
        }

        withContext(Dispatchers.IO) {
            try {
                // Join all lines to translate in one go
                val textToTranslate = linesToTranslate.joinToString("\n") { it.second }

                val helperTranslation = translator.translate(textToTranslate, Language.CHINESE_TRADITIONAL, Language.AUTO)
                var destLanguage = languageDestination
                if (destLanguage == Language.AUTO) {
                    destLanguage = if (helperTranslation.translatedText == textToTranslate)
                        Language.CHINESE_TRADITIONAL
                    else
                        helperTranslation.sourceLanguage
                }
                val mainTranslation = translator.translate(textToTranslate, destLanguage, Language.AUTO)

                val cleanTranslatedText = mainTranslation.translatedText.replace("\\\"", "\"").trim()
                val cleanPronunciation = mainTranslation.translatedPronunciation?.replace("\\\"", "\"")?.trim()

                val helpPronLineText = helperTranslation.sourcePronunciation?.trim() ?: ""
                val mainPronLineText = mainTranslation.sourcePronunciation?.trim() ?: textToTranslate

                val cleanTransLines = cleanTranslatedText.split("\n")
                val cleanPronLines = cleanPronunciation?.split("\n")
                val helpPronLines = helpPronLineText.split("\n")
                val mainPronLines = mainPronLineText.split("\n")

                linesToTranslate.forEachIndexed { i, (sentenceIndex, trimmed) ->
                    val cleanTransLine = cleanTransLines.getOrNull(i)?.trim() ?: ""
                    val cleanPronLine = cleanPronLines?.getOrNull(i)?.trim()
                    
                    val hPronLine = helpPronLines.getOrNull(i)?.trim() ?: ""
                    val mPronLine = mainPronLines.getOrNull(i)?.trim() ?: trimmed
                    val transPronLine = cleanPronLine ?: cleanTransLine

                    val isSameText = mainTranslation.sourceText == mainTranslation.translatedText

                    val outputText = if (!showSecondLine || isSameText) {
                        if (translateEnabled && romanizationEnabled) {
                            transPronLine
                        } else if (translateEnabled) {
                            cleanTransLine
                        } else if (romanizationEnabled) {
                            if (helperTranslation.sourceText == helperTranslation.translatedText)
                                hPronLine
                            else
                                mPronLine.ifEmpty { trimmed }
                        } else {
                            trimmed
                        }
                    } else {
                        if (translateEnabled && romanizationEnabled) {
                            val pron = if (helperTranslation.sourceText == helperTranslation.translatedText) hPronLine else mPronLine.ifEmpty { trimmed }
                            pron + "\n[$transPronLine]"
                        } else if (translateEnabled) {
                            trimmed + "\n[$cleanTransLine]"
                        } else if (romanizationEnabled) {
                            val pron = if (helperTranslation.sourceText == helperTranslation.translatedText) hPronLine else mPronLine.ifEmpty { trimmed }
                            trimmed + "\n[$pron]"
                        } else {
                            trimmed
                        }
                    }

                    val finalText = outputText.replace("\\r", "\r").replace("\\n", "\n")
                    translationCache[sentenceIndex] = finalText
                }

                if (translateEnabled) {
                    withContext(Dispatchers.Main) {
                        Toaster.s(R.string.translation_successful)
                    }
                }
            } catch (e: Exception) {
                Timber.e("Lyrics sync translation error: ${e.message}")
                if (translateEnabled) {
                    withContext(Dispatchers.Main) {
                        Toaster.e(R.string.translation_failed)
                    }
                }
            }
        }
    }

    val lazyListState = rememberLazyListState()

    val isDragged by lazyListState.interactionSource.collectIsDraggedAsState()

    LaunchedEffect(isDragged) {
        if (isDragged) {
            onAutoScrollEnabledChange(false)
        }
    }

    LaunchedEffect(synchronizedLyrics, density, isAutoScrollEnabled) {
        val centerOffset = with(density) {
            (-thumbnailSize.div(
                if (!showlyricsthumbnail && !isLandscape) if (trailingContent == null) 2 else 1
                else if (trailingContent == null) 3 else 2
            )).roundToPx()
        }

        if (isAutoScrollEnabled) {
            try {
                lazyListState.animateScrollToItem(
                    index = synchronizedLyrics.index + 1,
                    scrollOffset = centerOffset
                )
            } catch (e: kotlinx.coroutines.CancellationException) {
                if (!isActive) throw e
            }
        }

        while (isActive) {
            delay(50)
            if (!synchronizedLyrics.update()) continue
            if (isAutoScrollEnabled) {
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
    }

    var modifierBG = Modifier.verticalFadingEdge()
if (showBackgroundLyrics && showlyricsthumbnail) modifierBG =
        modifierBG.background(colorPalette().accent)

    val accentColor = colorPalette().accent

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = lazyListState,
            userScrollEnabled = true,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = modifierBG
                .fillMaxSize()
                .background(
                    if (isDisplayed && !showlyricsthumbnail)
                        when (lyricsBackground) {
                            LyricsBackground.Black -> Color.Black.copy(0.6f)
                            LyricsBackground.White -> Color.White.copy(0.4f)
                            else -> Color.Transparent
                        }
                    else Color.Transparent
                )
        ) {
            item(key = "header", contentType = 0) {
                Spacer(modifier = Modifier.height(thumbnailSize))
            }

            itemsIndexed(
                items = synchronizedLyrics.sentences
            ) { index, sentence ->
                val trimmedSentence = sentence.second.trim()

                // Read from cache — no network call here
                val displayText = if (showSecondLine || translateEnabled || romanizationEnabled) {
                    translationCache[index] ?: trimmedSentence
                } else {
                    trimmedSentence
                }

                val hasGapIndicator = showIntervalIndicator && gapWindows.containsKey(index)

                if (!hasGapIndicator) {
                    LyricsTextPainter(
                        text = displayText,
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
                        lyricsCustomColor = lyricsCustomColor,
                        dominantColor = dominantColor,
                        lyricsHighlight = lyricsHighlight,
                        clickLyricsText = clickLyricsText,
                        onClick = {
                            if (clickLyricsText) onSeekTo(sentence.first) else onDismiss()
                        }
                    )
                }

                // Interval indicator after this line (if there is a gap and the feature is enabled)
                if (showIntervalIndicator) {
                    gapWindows[index]?.let { (gapStart, gapEnd) ->
                        val isVisible = currentPositionMs in gapStart until gapEnd
                        LyricsIntervalIndicator(
                            gapStartMs = gapStart,
                            gapEndMs = gapEnd,
                            currentPositionMs = currentPositionMs,
                            visible = isVisible,
                            color = accentColor,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            item(key = "footer", contentType = 2) {
                Spacer(modifier = Modifier.height(thumbnailSize))
            }
        }
    }
}
