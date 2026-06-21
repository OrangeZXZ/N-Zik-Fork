package app.n_zik.android.components.player.lyrics

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.ripple
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.it.fast4x.rimusic.enums.ColorPaletteMode
import app.n_zik.android.enums.lyrics.LyricsBackground
import app.n_zik.android.enums.lyrics.LyricsColor
import app.n_zik.android.enums.lyrics.LyricsFontSize
import app.n_zik.android.enums.lyrics.LyricsHighlight
import app.n_zik.android.enums.lyrics.LyricsOutline
import app.n_zik.android.colorPalette
import app.n_zik.android.typography
import app.n_zik.android.uiRoundnessShape
import app.it.fast4x.rimusic.ui.styling.PureBlackColorPalette
import app.it.fast4x.rimusic.utils.verticalFadingEdge
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.drawscope.clipRect
import app.n_zik.android.R
import app.kreate.android.me.knighthat.utils.Toaster
import dev.rebelonion.translator.Language
import dev.rebelonion.translator.Translator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * A single word with its start and end time in milliseconds.
 */
data class KaraokeWord(
    val text: String,
    val startMs: Long,
    val endMs: Long,
    val charStartIndex: Int = 0
)

/**
 * A single karaoke line: the text, its timestamp, word-level timings,
 * and agent/background info for positioning.
 */
data class KaraokeLine(
    val timeMs: Long,
    val text: String,
    val words: List<KaraokeWord>,
    val agent: String? = null,     // "v1", "v2", "v1000", etc.
    val isBackground: Boolean = false
)

/**
 * Helper to create a fast clipping path using bounding boxes instead of complex text glyph outlines.
 */
fun getFastPathForRange(layout: androidx.compose.ui.text.TextLayoutResult, startIdx: Int, endIdx: Int): androidx.compose.ui.graphics.Path {
    val path = androidx.compose.ui.graphics.Path()
    if (startIdx > endIdx || startIdx < 0 || endIdx >= layout.layoutInput.text.length) return path
    
    val startLine = layout.getLineForOffset(startIdx)
    val endLine = layout.getLineForOffset(endIdx)
    
    for (i in startLine..endLine) {
        val lineStartIdx = layout.getLineStart(i)
        val lineEndIdx = layout.getLineEnd(i, visibleEnd = true)
        val clipStartIdx = maxOf(startIdx, lineStartIdx)
        val clipEndIdx = minOf(endIdx, lineEndIdx - 1)
        
        if (clipStartIdx <= clipEndIdx) {
            val startBox = layout.getBoundingBox(clipStartIdx)
            val endBox = layout.getBoundingBox(clipEndIdx)
            path.addRect(androidx.compose.ui.geometry.Rect(startBox.left, startBox.top, endBox.right, endBox.bottom))
        }
    }
    return path
}

/**
 * Parse the extended LRC format produced by TTMLParser.toLRC().
 *
 * Format:
 * ```
 * [MM:SS.cc]{agent:v1}Line text
 * <word1:startSec:endSec|word2:startSec:endSec>
 * [MM:SS.cc]{bg}Background line
 * <word1:startSec:endSec|word2:startSec:endSec>
 * ```
 */
fun parseKaraokeLrc(text: String): List<KaraokeLine> {
    val lines = text.trim().lines()
    val result = mutableListOf<KaraokeLine>()

    val agentRegex = Regex("\\{agent:([^}]+)\\}")
    val bgRegex = Regex("\\{bg\\}")

    var i = 0
    while (i < lines.size) {
        val line = lines[i]
        if (line.startsWith("[")) {
            // Parse [MM:SS.cc]text
            val closeBracket = line.indexOf(']')
            if (closeBracket < 0) { i++; continue }
            val timeStr = line.substring(1, closeBracket)
            var lineText = line.substring(closeBracket + 1)

            // Parse agent tag
            val agentMatch = agentRegex.find(lineText)
            val agent = agentMatch?.groupValues?.get(1)
            if (agentMatch != null) {
                lineText = lineText.replaceFirst(agentRegex, "")
            }

            // Parse background tag
            val isBg = bgRegex.containsMatchIn(lineText)
            if (isBg) {
                lineText = lineText.replaceFirst(bgRegex, "")
            }

            val cleanText = lineText.trim()
            val timeMs = parseLrcTime(timeStr)

            // Check if next lines are word-timing lines (can be multiline)
            var words = emptyList<KaraokeWord>()
            if (i + 1 < lines.size && lines[i + 1].trim().startsWith("<")) {
                val wordsRawBuilder = StringBuilder()
                i++ // Move to the first word timing line
                while (i < lines.size) {
                    val lineTrim = lines[i].trim()
                    wordsRawBuilder.append(lineTrim)
                    if (lineTrim.endsWith(">")) {
                        break
                    }
                    i++
                }
                words = parseWordTimings(wordsRawBuilder.toString())
                var searchIndex = 0
                words = words.map { w ->
                    val idx = cleanText.indexOf(w.text, searchIndex)
                    if (idx >= 0) {
                        searchIndex = idx + w.text.length
                        w.copy(charStartIndex = idx)
                    } else {
                        w
                    }
                }
            }

            result.add(KaraokeLine(timeMs, cleanText, words, agent, isBg))
        }
        i++
    }
    return result
}

private fun parseLrcTime(timeStr: String): Long {
    // Format: MM:SS.cc
    return try {
        val parts = timeStr.split(":")
        val minutes = parts[0].toLong()
        val secondsParts = parts[1].split(".")
        val seconds = secondsParts[0].toLong()
        val centiseconds = secondsParts[1].toLong()
        minutes * 60_000 + seconds * 1000 + centiseconds * 10
    } catch (_: Exception) {
        0L
    }
}

private fun parseWordTimings(line: String): List<KaraokeWord> {
    // Format: <word1:startSec:endSec|word2:startSec:endSec>
    val content = line.removePrefix("<").removeSuffix(">")
    if (content.isBlank()) return emptyList()

    return content.split("|").mapNotNull { part ->
        // Handle words that may contain colons (unlikely but defensive)
        // Format: text:startTime:endTime
        val lastColon = part.lastIndexOf(':')
        if (lastColon < 0) return@mapNotNull null
        val secondLastColon = part.lastIndexOf(':', lastColon - 1)
        if (secondLastColon < 0) return@mapNotNull null

        val wordText = part.substring(0, secondLastColon)
        val startSec = part.substring(secondLastColon + 1, lastColon).toDoubleOrNull() ?: return@mapNotNull null
        val endSec = part.substring(lastColon + 1).toDoubleOrNull() ?: return@mapNotNull null
        KaraokeWord(
            text = wordText,
            startMs = (startSec * 1000).toLong(),
            endMs = (endSec * 1000).toLong()
        )
    }
}

@Composable
fun KaraokeLyricsView(
    text: String,
    currentPositionProvider: () -> Long,
    isPlayingProvider: () -> Boolean,
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
    lyricsSizeAnimate: Boolean,
    lyricsColor: LyricsColor,
    lyricsCustomColor: Int,
    dominantColor: Int,
    lyricsHighlight: LyricsHighlight,
    lyricsAlignment: app.n_zik.android.enums.lyrics.LyricsAlignment,
    clickLyricsText: Boolean,
    karaokeRespectAgentPosition: Boolean,
    thumbnailSize: Dp,
    isDisplayed: Boolean,
    onDismiss: () -> Unit,
    onInvalidLrc: (Boolean) -> Unit,
    showIntervalIndicator: Boolean = true
) {
    val density = LocalDensity.current

    val karaokeLines = remember(text) {
        val parsed = parseKaraokeLrc(text)
        if (parsed.isEmpty()) onInvalidLrc(true) else onInvalidLrc(false)
        parsed
    }

    // Track current position for word-by-word animation with exact Metrolist interpolation
    var currentPositionMs by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        var lastPlayerPos = currentPositionProvider()
        var lastUpdateTime = System.currentTimeMillis()
        while (isActive) {
            androidx.compose.runtime.withFrameMillis {
                val now = System.currentTimeMillis()
                val playerPos = currentPositionProvider()
                if (playerPos != lastPlayerPos) {
                    lastPlayerPos = playerPos
                    lastUpdateTime = now
                }
                val elapsed = now - lastUpdateTime
                currentPositionMs = lastPlayerPos + (if (isPlayingProvider()) elapsed else 0L)
            }
        }
    }

    // --- Translation cache ---
    val translationCache = remember(text, showSecondLine, translateEnabled, romanizationEnabled, languageDestination) {
        mutableStateMapOf<Int, String>()
    }

    LaunchedEffect(text, showSecondLine, translateEnabled, romanizationEnabled, languageDestination) {
        if (!showSecondLine && !translateEnabled && !romanizationEnabled) return@LaunchedEffect

        val linesToTranslate = mutableListOf<Pair<Int, String>>()
        karaokeLines.forEachIndexed { index, line ->
            val trimmed = line.text.trim()
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

    // Pre-compute gap windows for interval indicator
    val initialGapWindow = remember(karaokeLines) {
        val firstStart = karaokeLines.firstOrNull { !it.isBackground }?.timeMs ?: 0L
        if (firstStart > 3000L) Pair(0L, firstStart - 650L) else null
    }

    val gapWindows = remember(karaokeLines) {
        buildMap {
            karaokeLines.forEachIndexed { index, line ->
                if (line.isBackground) return@forEachIndexed
                val startMs = line.timeMs
                val nextStartMs = if (index < karaokeLines.size - 1) karaokeLines.drop(index + 1).firstOrNull { !it.isBackground }?.timeMs ?: (startMs + 10000L) else startMs + 10000L
                val lineEndMs = if (line.words.isNotEmpty()) line.words.maxOf { it.endMs } else line.timeMs + 2000L

                val gap = nextStartMs - lineEndMs
                if (gap > 2500L) {
                    put(index, Pair(lineEndMs, nextStartMs - 650L))
                }
            }
        }.toMutableMap().apply {
            // Remove overlapping gap windows to prevent double loaders in duets
            val keysToRemove = mutableSetOf<Int>()
            for ((idx1, window1) in this) {
                for ((idx2, window2) in this) {
                    if (idx1 < idx2) {
                        if (window1.first < window2.second && window1.second > window2.first) {
                            keysToRemove.add(idx1)
                        }
                    }
                }
            }
            keysToRemove.forEach { remove(it) }
        }
    }

    // Determine active lines (multiple can be active for overlapping agents)
    val activeLineIndices = remember(currentPositionMs, karaokeLines) {
        val active = mutableSetOf<Int>()
        var lastPassedIndex = 0
        for (i in karaokeLines.indices) {
            val line = karaokeLines[i]
            if (line.timeMs > currentPositionMs + 50L) break
            if (!line.isBackground) lastPassedIndex = i

            // Determine line end time
            val lineEndMs = if (line.words.isNotEmpty()) {
                line.words.maxOf { it.endMs }
            } else {
                // Fallback: use next non-background line's start
                karaokeLines.getOrNull(i + 1)?.timeMs ?: Long.MAX_VALUE
            }

            if (currentPositionMs <= lineEndMs) {
                active.add(i)
            }
        }
        if (active.isEmpty()) {
            active.add(lastPassedIndex)
        }
        active
    }

    // Primary active line for scroll targeting
    val primaryActiveIndex = activeLineIndices
        .filter { !karaokeLines[it].isBackground }
        .maxOrNull() ?: activeLineIndices.maxOrNull() ?: 0

    val lazyListState = rememberLazyListState()

    LaunchedEffect(primaryActiveIndex, density) {
        val centerOffset = with(density) {
            (-thumbnailSize.div(
                if (!showlyricsthumbnail && !isLandscape) if (trailingContent == null) 2 else 1
                else if (trailingContent == null) 3 else 2
            )).roundToPx()
        }

        try {
            lazyListState.animateScrollToItem(
                index = primaryActiveIndex + 1, // +1 for header spacer
                scrollOffset = centerOffset
            )
        } catch (_: kotlinx.coroutines.CancellationException) {}
    }

    // Resolve the accent color
    val accentColor = when (lyricsColor) {
        LyricsColor.White -> Color.White
        LyricsColor.Cover -> Color(dominantColor)
        LyricsColor.Custom -> Color(lyricsCustomColor)
        LyricsColor.Thememode -> if (showlyricsthumbnail) PureBlackColorPalette.text else colorPalette().text
    }

    val inactiveColor = accentColor.copy(alpha = 0.4f)

    val textSize = when (fontSize) {
        LyricsFontSize.Light -> typography().m.fontSize
        LyricsFontSize.Medium -> typography().l.fontSize
        LyricsFontSize.Heavy -> typography().xl.fontSize
        LyricsFontSize.Large -> typography().xlxl.fontSize
        else -> customSize.sp
    }

    var modifierBG = Modifier.verticalFadingEdge()
    if (showBackgroundLyrics && showlyricsthumbnail) modifierBG = modifierBG.background(colorPalette().accent)

    LazyColumn(
        state = lazyListState,
        userScrollEnabled = true,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifierBG
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

        if (showIntervalIndicator && initialGapWindow != null) {
            item(key = "initial_loader", contentType = 2) {
                val isVisible = currentPositionMs in initialGapWindow.first until initialGapWindow.second
                LyricsIntervalIndicator(
                    gapStartMs = initialGapWindow.first,
                    gapEndMs = initialGapWindow.second,
                    currentPositionMs = currentPositionMs,
                    visible = isVisible,
                    color = accentColor,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
                )
            }
        }

        itemsIndexed(karaokeLines) { index, line ->
            val isActiveLine = index in activeLineIndices

            // Agent-based alignment: v1=Left, v2=Right, bg/v1000=Center
            val lineAlignment = when {
                karaokeRespectAgentPosition && line.agent == "v1" -> Alignment.Start
                karaokeRespectAgentPosition && line.agent == "v2" -> Alignment.End
                karaokeRespectAgentPosition && line.agent == "v1000" -> Alignment.CenterHorizontally
                karaokeRespectAgentPosition && line.isBackground -> Alignment.CenterHorizontally
                else -> when (lyricsAlignment) {
                    app.n_zik.android.enums.lyrics.LyricsAlignment.Left -> Alignment.Start
                    app.n_zik.android.enums.lyrics.LyricsAlignment.Center -> Alignment.CenterHorizontally
                    app.n_zik.android.enums.lyrics.LyricsAlignment.Right -> Alignment.End
                }
            }
            val lineTextAlign = when {
                karaokeRespectAgentPosition && line.agent == "v1" -> TextAlign.Left
                karaokeRespectAgentPosition && line.agent == "v2" -> TextAlign.Right
                karaokeRespectAgentPosition && line.agent == "v1000" -> TextAlign.Center
                karaokeRespectAgentPosition && line.isBackground -> TextAlign.Center
                else -> when (lyricsAlignment) {
                    app.n_zik.android.enums.lyrics.LyricsAlignment.Left -> TextAlign.Left
                    app.n_zik.android.enums.lyrics.LyricsAlignment.Center -> TextAlign.Center
                    app.n_zik.android.enums.lyrics.LyricsAlignment.Right -> TextAlign.Right
                }
            }

            // Background vocals are smaller/dimmer
            val bgScale = if (line.isBackground) 0.85f else 1f
            val bgAlphaFactor = if (line.isBackground) 0.8f else 1f

            val animateOpacity by animateFloatAsState(
                targetValue = if (isActiveLine) 1f * bgAlphaFactor else 0.6f * bgAlphaFactor,
                animationSpec = tween(500, easing = LinearOutSlowInEasing),
                label = ""
            )
            val animateScale by animateFloatAsState(
                targetValue = if (isActiveLine && lyricsSizeAnimate) 1.05f * bgScale
                              else if (lyricsSizeAnimate) 0.85f * bgScale
                              else 1f * bgScale,
                animationSpec = tween(500, easing = LinearOutSlowInEasing),
                label = ""
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp, horizontal = 32.dp)
                    .graphicsLayer {
                        alpha = animateOpacity
                        if (lyricsSizeAnimate || line.isBackground) {
                            scaleX = animateScale
                            scaleY = animateScale
                        }
                    }
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                    .clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = if (clickLyricsText) androidx.compose.foundation.LocalIndication.current else null,
                        onClick = {
                            if (clickLyricsText) onSeekTo(line.timeMs) else onDismiss()
                        }
                    )
                    .background(
                        if (isActiveLine && !line.isBackground && lyricsHighlight == LyricsHighlight.White) Color.White.copy(0.5f)
                        else if (isActiveLine && !line.isBackground && lyricsHighlight == LyricsHighlight.Black) Color.Black.copy(0.5f)
                        else Color.Transparent,
                        androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                    ),
                horizontalAlignment = lineAlignment
            ) {
                val gapWindow = gapWindows[index]
                val showLoader = showIntervalIndicator && gapWindow != null &&
                        currentPositionMs >= gapWindow.first && currentPositionMs <= gapWindow.second

                val lineAccent = if (line.isBackground) accentColor.copy(alpha = 0.85f) else accentColor
                val lineInactive = if (line.isBackground) accentColor.copy(alpha = 0.3f) else inactiveColor

                val displayedText = translationCache[index] ?: line.text

                if (line.words.isNotEmpty() && isActiveLine) {
                    var textLayoutResult by remember { androidx.compose.runtime.mutableStateOf<androidx.compose.ui.text.TextLayoutResult?>(null) }

                    Box(contentAlignment = Alignment.Center) {
                        val isTextReplaced = displayedText != line.text && !showSecondLine

                        val backgroundText = androidx.compose.ui.text.buildAnnotatedString {
                            if (displayedText != line.text && showSecondLine) {
                                val originalLen = line.text.trim().length
                                val safeLen = originalLen.coerceAtMost(displayedText.length)
                                withStyle(androidx.compose.ui.text.SpanStyle(color = lineInactive)) {
                                    append(displayedText.substring(0, safeLen))
                                }
                                withStyle(androidx.compose.ui.text.SpanStyle(color = lineAccent.copy(alpha = 0.85f))) {
                                    append(displayedText.substring(safeLen))
                                }
                            } else {
                                withStyle(androidx.compose.ui.text.SpanStyle(color = lineInactive)) {
                                    append(displayedText)
                                }
                            }
                        }

                        // Background (inactive) text
                        androidx.compose.foundation.text.BasicText(
                            text = backgroundText,
                            style = androidx.compose.ui.text.TextStyle(
                                fontSize = textSize,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                textAlign = lineTextAlign
                            ),
                            onTextLayout = { textLayoutResult = it }
                        )

                        if (!isTextReplaced) {
                            // Unclipped Glow Layer (Restored with interpolation)
                            val glowText = androidx.compose.ui.text.buildAnnotatedString {
                                line.words.forEachIndexed { wordIndex, word ->
                                    val isWordActive = currentPositionMs >= word.startMs && currentPositionMs < word.endMs
                                    val isWordSung = currentPositionMs >= word.endMs
                                    
                                    if (isWordActive || isWordSung) {
                                        val dur = word.endMs - word.startMs
                                        val sungFactor = if (isWordSung) 1f 
                                                         else if (isWordActive) ((currentPositionMs - word.startMs).toFloat() / dur.coerceAtLeast(1L)).coerceIn(0f, 1f)
                                                         else 0f
                                        
                                        val wordLenText = word.text.length.coerceAtLeast(1)
                                        val impactRatio = dur.toFloat() / wordLenText
                                        val fadeFactor = (sungFactor * 5f).coerceIn(0f, 1f) * ((1f - sungFactor) * 8f).coerceIn(0f, 1f)
                                        val impactFactor = (((impactRatio - 100f) / 250f).coerceIn(0f, 1f) * 0.6f + ((dur.toFloat() - 300f) / 1500f).coerceIn(0f, 1f) * 0.4f).coerceIn(0f, 1f) * fadeFactor
                                        
                                        // Make the glow completely opaque/solid to combat the transparency
                                        val glowAlpha = (2f * impactFactor).coerceIn(0f, 1f)
                                        val baseGlowRadius = with(density) { 12.dp.toPx() } * impactFactor
                                        
                                        if (impactFactor > 0.01f && baseGlowRadius > 0f) {
                                            withStyle(
                                                androidx.compose.ui.text.SpanStyle(
                                                    color = androidx.compose.ui.graphics.Color.Transparent,
                                                    shadow = androidx.compose.ui.graphics.Shadow(
                                                        color = lineAccent.copy(alpha = glowAlpha),
                                                        blurRadius = baseGlowRadius
                                                    )
                                                )
                                            ) {
                                                append(word.text)
                                            }
                                        } else {
                                            withStyle(androidx.compose.ui.text.SpanStyle(color = androidx.compose.ui.graphics.Color.Transparent)) { append(word.text) }
                                        }
                                    } else {
                                        withStyle(androidx.compose.ui.text.SpanStyle(color = androidx.compose.ui.graphics.Color.Transparent)) { append(word.text) }
                                    }
                                    if (wordIndex < line.words.lastIndex) {
                                        withStyle(androidx.compose.ui.text.SpanStyle(color = androidx.compose.ui.graphics.Color.Transparent)) { append(" ") }
                                    }
                                }
                                
                                // Preserve layout identical to displayedText by appending the translation part transparently
                                val builtLen = this.length
                                if (builtLen < displayedText.length) {
                                    withStyle(androidx.compose.ui.text.SpanStyle(color = androidx.compose.ui.graphics.Color.Transparent)) {
                                        append(displayedText.substring(builtLen))
                                    }
                                }
                            }
                            
                            androidx.compose.foundation.text.BasicText(
                                text = glowText,
                                style = androidx.compose.ui.text.TextStyle(
                                    fontSize = textSize,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                    textAlign = lineTextAlign
                                )
                            )
                        }

                        // Foreground (active) text with character-level clipping
                        androidx.compose.foundation.text.BasicText(
                            text = displayedText,
                            style = androidx.compose.ui.text.TextStyle(
                                color = lineAccent,
                                fontSize = textSize,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                textAlign = lineTextAlign
                            ),
                            modifier = Modifier.drawWithContent {
                                val layout = textLayoutResult ?: return@drawWithContent
                                
                                if (isTextReplaced) {
                                    val lineStartMs = line.timeMs
                                    val lineEndMs = line.words.maxOfOrNull { it.endMs } ?: (lineStartMs + 2000L)
                                    
                                    if (currentPositionMs >= lineEndMs) {
                                        this@drawWithContent.drawContent()
                                    } else if (currentPositionMs > lineStartMs) {
                                        val progress = ((currentPositionMs - lineStartMs).toFloat() / (lineEndMs - lineStartMs).coerceAtLeast(1L)).coerceIn(0f, 1f)
                                        val totalLength = displayedText.length
                                        val activeCharIdx = (progress * totalLength).toInt().coerceIn(0, totalLength - 1)
                                        
                                        if (activeCharIdx > 0) {
                                            val path = getFastPathForRange(layout, 0, activeCharIdx - 1)
                                            drawContext.canvas.save()
                                            drawContext.canvas.clipPath(path)
                                            this@drawWithContent.drawContent()
                                            drawContext.canvas.restore()
                                        }
                                        
                                        // Active character pop for translated text
                                        val charLp = ((progress * totalLength) - activeCharIdx).coerceIn(0f, 1f)
                                        val ease = { t: Float -> t * t * (3f - 2f * t) }
                                        val bounceFactor = when {
                                            charLp < 0.4f -> ease(charLp / 0.4f)
                                            charLp > 0.6f -> ease((1f - charLp) / 0.4f)
                                            else -> 1f
                                        }
                                        val charBounceY = -bounceFactor * with(density) { 0.8.dp.toPx() }
                                        val scaleFactor = 1f + (bounceFactor * 0.05f)
                                        
                                        val charBox = layout.getBoundingBox(activeCharIdx)
                                        drawContext.canvas.save()
                                        val pivotX = charBox.left + charBox.width / 2f
                                        val pivotY = charBox.bottom
                                        drawContext.canvas.translate(pivotX, pivotY + charBounceY)
                                        drawContext.canvas.scale(scaleFactor, scaleFactor)
                                        drawContext.canvas.translate(-pivotX, -pivotY)
                                        
                                        drawContext.canvas.clipRect(androidx.compose.ui.geometry.Rect(charBox.left, charBox.top, charBox.right, charBox.bottom))
                                        this@drawWithContent.drawContent()
                                        drawContext.canvas.restore()
                                    }
                                } else {
                                    line.words.forEach { word ->
                                    val isWordActive = currentPositionMs >= word.startMs && currentPositionMs < word.endMs
                                    val isWordSung = currentPositionMs >= word.endMs
                                    
                                    val wStartIdx = word.charStartIndex.coerceIn(0, displayedText.length.coerceAtLeast(1) - 1)
                                    val wEndIdx = (word.charStartIndex + word.text.length - 1).coerceIn(0, displayedText.length.coerceAtLeast(1) - 1)
                                    
                                    val startBox = layout.getBoundingBox(wStartIdx)
                                    val endBox = layout.getBoundingBox(wEndIdx)
                                    
                                    if (isWordSung) {
                                        // Fully sung word: clipRect to full word bounds
                                        val path = getFastPathForRange(layout, wStartIdx, wEndIdx)
                                        drawContext.canvas.save()
                                        drawContext.canvas.clipPath(path)
                                        this@drawWithContent.drawContent()
                                        drawContext.canvas.restore()
                                    } else if (isWordActive) {
                                        val dur = word.endMs - word.startMs
                                        val linearProgress = ((currentPositionMs - word.startMs).toFloat() / dur.coerceAtLeast(1L)).coerceIn(0f, 1f)
                                        val wordLen = word.text.length
                                        val activeCharIdxInWord = (linearProgress * wordLen).toInt().coerceAtMost(wordLen - 1)
                                        val charLp = ((linearProgress * wordLen) - activeCharIdxInWord).coerceIn(0f, 1f)
                                        
                                        val activeCharGlobalIdx = (word.charStartIndex + activeCharIdxInWord).coerceIn(0, displayedText.length.coerceAtLeast(1) - 1)
                                        val charBox = layout.getBoundingBox(activeCharGlobalIdx)
                                        
                                            val path = getFastPathForRange(layout, wStartIdx, activeCharGlobalIdx - 1)
                                            drawContext.canvas.save()
                                            drawContext.canvas.clipPath(path)
                                            this@drawWithContent.drawContent()
                                            drawContext.canvas.restore()
                                        
                                        // Active character smoothly pops up, holds, and drops
                                        val ease = { t: Float -> t * t * (3f - 2f * t) }
                                        val bounceFactor = when {
                                            charLp < 0.4f -> ease(charLp / 0.4f)
                                            charLp > 0.6f -> ease((1f - charLp) / 0.4f)
                                            else -> 1f
                                        }
                                        val charBounceY = -bounceFactor * with(density) { 0.8.dp.toPx() }
                                        val scaleFactor = 1f + (bounceFactor * 0.05f)
                                        
                                        drawContext.canvas.save()
                                        val pivotX = charBox.left + charBox.width / 2f
                                        val pivotY = charBox.bottom
                                        drawContext.canvas.translate(pivotX, pivotY + charBounceY)
                                        drawContext.canvas.scale(scaleFactor, scaleFactor)
                                        drawContext.canvas.translate(-pivotX, -pivotY)
                                        
                                        // The active character is fully highlighted (glowing) instantly while bouncing
                                        drawContext.canvas.save()
                                        drawContext.canvas.clipRect(androidx.compose.ui.geometry.Rect(charBox.left, charBox.top, charBox.right, charBox.bottom))
                                        this@drawWithContent.drawContent()
                                        drawContext.canvas.restore()
                                        
                                        
                                        drawContext.canvas.restore()
                                    }
                                }
                                }
                            }
                        )
                    }
                } else if (line.words.isNotEmpty() && !isActiveLine) {
                    // Inactive line with word timings — check if past or future
                    val isPastLine = currentPositionMs > (line.words.maxOfOrNull { it.endMs } ?: line.timeMs)

                    BasicText(
                        text = displayedText,
                        style = TextStyle(
                            fontSize = textSize,
                            textAlign = lineTextAlign,
                            lineHeight = textSize * 1.4f,
                            color = if (isPastLine) lineAccent.copy(alpha = 0.5f) else lineInactive,
                            fontWeight = if (isPastLine) FontWeight.Bold else FontWeight.Medium
                        )
                    )
                } else {
                    // Fallback: no word timings (instrumental breaks, etc.)
                    BasicText(
                        text = displayedText,
                        style = TextStyle(
                            fontSize = textSize,
                            textAlign = lineTextAlign,
                            lineHeight = textSize * 1.4f,
                            color = if (isActiveLine) lineAccent else lineInactive,
                            fontWeight = if (isActiveLine) FontWeight.ExtraBold else FontWeight.Medium,
                            shadow = if (isActiveLine) Shadow(
                                color = lineAccent.copy(alpha = 0.3f),
                                offset = Offset.Zero,
                                blurRadius = 12f
                            ) else null
                        )
                    )
                }
                if (!line.isBackground && showIntervalIndicator) {
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
        }
        item(key = "footer", contentType = 2) {
            Spacer(modifier = Modifier.height(thumbnailSize))
        }
    }
}
