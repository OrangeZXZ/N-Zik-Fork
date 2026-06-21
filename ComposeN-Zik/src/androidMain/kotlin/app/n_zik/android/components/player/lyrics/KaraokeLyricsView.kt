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

    // Pre-compute gap windows for interval indicator
    val gapWindows = remember(karaokeLines) {
        buildMap {
            karaokeLines.forEachIndexed { index, line ->
                if (line.isBackground) return@forEachIndexed
                val startMs = line.timeMs
                val nextStartMs = if (index < karaokeLines.size - 1) karaokeLines.drop(index + 1).firstOrNull { !it.isBackground }?.timeMs ?: (startMs + 10000L) else startMs + 10000L
                val lineEndMs = if (line.words.isNotEmpty()) line.words.maxOf { it.endMs } else line.timeMs + 2000L

                val currentEnd = lineEndMs.coerceAtMost(nextStartMs - 2000L).coerceAtLeast(startMs + 1000L)
                if (currentEnd < nextStartMs) {
                    val gap = nextStartMs - currentEnd
                    if (gap > 2000L) {
                        put(index, Pair(currentEnd, nextStartMs - 650L))
                    }
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

                if (showLoader && isActiveLine) {
                    LyricsIntervalIndicator(
                        gapStartMs = gapWindow!!.first,
                        gapEndMs = gapWindow.second,
                        currentPositionMs = currentPositionMs,
                        visible = true,
                        color = accentColor,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                val lineAccent = if (line.isBackground) accentColor.copy(alpha = 0.85f) else accentColor
                val lineInactive = if (line.isBackground) accentColor.copy(alpha = 0.3f) else inactiveColor

                if (line.words.isNotEmpty() && isActiveLine) {
                    var textLayoutResult by remember { androidx.compose.runtime.mutableStateOf<androidx.compose.ui.text.TextLayoutResult?>(null) }

                    Box(contentAlignment = Alignment.Center) {
                        // Background (inactive) text
                        androidx.compose.foundation.text.BasicText(
                            text = line.text,
                            style = androidx.compose.ui.text.TextStyle(
                                color = lineInactive,
                                fontSize = textSize,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                textAlign = lineTextAlign
                            ),
                            onTextLayout = { textLayoutResult = it }
                        )

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
                                    
                                    val glowAlpha = (0.35f * impactFactor).coerceIn(0f, 0.4f)
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
                        }
                        
                        androidx.compose.foundation.text.BasicText(
                            text = glowText,
                            style = androidx.compose.ui.text.TextStyle(
                                fontSize = textSize,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                textAlign = lineTextAlign
                            )
                        )

                        // Foreground (active) text, clipped word by word to prevent multi-line bleed
                        val layout = textLayoutResult
                        if (layout != null) {
                            androidx.compose.foundation.text.BasicText(
                                text = line.text,
                                style = androidx.compose.ui.text.TextStyle(
                                    color = lineAccent,
                                    fontSize = textSize,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                    textAlign = lineTextAlign
                                ),
                                modifier = Modifier.drawWithContent {
                                    line.words.forEach { word ->
                                        val isWordActive = currentPositionMs >= word.startMs && currentPositionMs < word.endMs
                                        val isWordSung = currentPositionMs >= word.endMs
                                        
                                        if (isWordSung) {
                                            // Fully sung word
                                            val startIdx = word.charStartIndex.coerceIn(0, line.text.length - 1)
                                            val endIdx = (word.charStartIndex + word.text.length - 1).coerceIn(0, line.text.length - 1)
                                            val startBox = layout.getBoundingBox(startIdx)
                                            val endBox = layout.getBoundingBox(endIdx)
                                            clipRect(left = startBox.left, top = startBox.top, right = endBox.right, bottom = endBox.bottom) {
                                                this@drawWithContent.drawContent()
                                            }
                                        } else if (isWordActive) {
                                            // Partially sung word
                                            val dur = word.endMs - word.startMs
                                            val linearProgress = ((currentPositionMs - word.startMs).toFloat() / dur.coerceAtLeast(1L)).coerceIn(0f, 1f)
                                            val wordLen = word.text.length
                                            val activeCharIdxInWord = (linearProgress * wordLen).toInt().coerceAtMost(wordLen - 1)
                                            val charLp = ((linearProgress * wordLen) - activeCharIdxInWord).coerceIn(0f, 1f)
                                            val globalCharIdx = (word.charStartIndex + activeCharIdxInWord).coerceIn(0, line.text.length - 1)
                                            val charBox = layout.getBoundingBox(globalCharIdx)
                                            
                                            // Draw previous fully sung characters in this active word
                                            if (activeCharIdxInWord > 0) {
                                                val startBox = layout.getBoundingBox(word.charStartIndex.coerceIn(0, line.text.length - 1))
                                                val prevBox = layout.getBoundingBox((word.charStartIndex + activeCharIdxInWord - 1).coerceIn(0, line.text.length - 1))
                                                clipRect(left = startBox.left, top = startBox.top, right = prevBox.right, bottom = prevBox.bottom) {
                                                    this@drawWithContent.drawContent()
                                                }
                                            }
                                            
                                            // Active character transition with 12 slices
                                            val fXL = charBox.width * charLp
                                            val eW = (charBox.width * 0.45f).coerceAtLeast(1f)
                                            val sWL = (fXL - eW).coerceAtLeast(0f)
                                            
                                            val opaqueRight = charBox.left + sWL
                                            val activeRight = charBox.left + fXL
                                            
                                            if (opaqueRight > charBox.left) {
                                                clipRect(left = charBox.left, top = charBox.top, right = opaqueRight, bottom = charBox.bottom) {
                                                    this@drawWithContent.drawContent()
                                                }
                                            }
                                            
                                            val transitionWidth = activeRight - opaqueRight
                                            if (transitionWidth > 0) {
                                                for (j in 0 until 12) {
                                                    val startSlice = opaqueRight + (j * transitionWidth / 12f)
                                                    val endSlice = opaqueRight + ((j + 1) * transitionWidth / 12f)
                                                    val sliceAlpha = 1f - (j + 0.5f) / 12f
                                                    
                                                    clipRect(left = startSlice, top = charBox.top, right = endSlice, bottom = charBox.bottom) {
                                                        val paint = androidx.compose.ui.graphics.Paint().apply { alpha = sliceAlpha }
                                                        drawContext.canvas.saveLayer(androidx.compose.ui.geometry.Rect(startSlice, charBox.top, endSlice, charBox.bottom), paint)
                                                        this@drawWithContent.drawContent()
                                                        drawContext.canvas.restore()
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            )
                        }
                    }
                } else if (line.words.isNotEmpty() && !isActiveLine) {
                    // Inactive line with word timings — check if past or future
                    val isPastLine = currentPositionMs > (line.words.maxOfOrNull { it.endMs } ?: line.timeMs)

                    val styledText = buildAnnotatedString {
                        line.words.forEachIndexed { wordIndex, word ->
                            withStyle(
                                SpanStyle(
                                    color = if (isPastLine) lineAccent.copy(alpha = 0.5f) else lineInactive,
                                    fontWeight = if (isPastLine) FontWeight.Bold else FontWeight.Medium
                                )
                            ) {
                                append(word.text)
                            }
                            if (wordIndex < line.words.lastIndex) append(" ")
                        }
                    }

                    BasicText(
                        text = styledText,
                        style = TextStyle(
                            fontSize = textSize,
                            textAlign = lineTextAlign,
                            lineHeight = textSize * 1.4f
                        )
                    )
                } else {
                    // Fallback: no word timings (instrumental breaks, etc.)
                    BasicText(
                        text = line.text,
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
