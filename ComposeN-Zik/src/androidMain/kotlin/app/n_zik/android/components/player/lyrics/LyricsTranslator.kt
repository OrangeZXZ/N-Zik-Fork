package app.n_zik.android.components.player.lyrics

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState

import dev.rebelonion.translator.Language
import dev.rebelonion.translator.Translator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

@Composable
fun TranslateLyricsWithRomanization(
    output: MutableState<String>,
    textToTranslate: String,
    isSync: Boolean,
    showSecondLine: Boolean,
    romanizationEnabled: Boolean,
    translateEnabled: Boolean,
    translator: Translator,
    onPlaceholderDismissed: () -> Unit,
    destinationLanguage: Language = Language.AUTO
) {
    LaunchedEffect(showSecondLine, romanizationEnabled, textToTranslate, destinationLanguage, translateEnabled){
        var destLanguage = destinationLanguage
        val result = withContext(Dispatchers.IO) {
            try {
                /** used to find the source language of the text and detect CHINESE_TRADITIONAL*/
                val helperTranslation = translator.translate(
                    textToTranslate,
                    Language.CHINESE_TRADITIONAL,
                    Language.AUTO
                )
                if(destinationLanguage == Language.AUTO){
                    destLanguage = if(helperTranslation.translatedText == textToTranslate){
                        Language.CHINESE_TRADITIONAL
                    } else {
                        helperTranslation.sourceLanguage
                    }
                }
                val mainTranslation = translator.translate(
                    textToTranslate,
                    destLanguage,
                    Language.AUTO
                )
                // Clean up escaped quotes from JSON responses
                val cleanTranslatedText = mainTranslation.translatedText.replace("\\\"", "\"")
                val cleanPronunciation = mainTranslation.translatedPronunciation?.replace("\\\"", "\"")
                
                val outputText = if (textToTranslate == "") {
                    ""
                } else if (!showSecondLine || (mainTranslation.sourceText == mainTranslation.translatedText)){
                    if (!romanizationEnabled) {
                        if (translateEnabled) cleanTranslatedText else textToTranslate
                    }
                    else if (helperTranslation.sourceText == helperTranslation.translatedText) helperTranslation.sourcePronunciation else mainTranslation.sourcePronunciation ?: mainTranslation.sourceText
                } else {
                    val originalLines = textToTranslate.split("\n")
                    val translatedLines = if (!romanizationEnabled) {
                        cleanTranslatedText.split("\n")
                    } else {
                        val pron = if (helperTranslation.sourceText == helperTranslation.translatedText) {
                            helperTranslation.sourcePronunciation
                        } else {
                            mainTranslation.sourcePronunciation ?: mainTranslation.sourceText
                        }
                        (pron ?: "").split("\n")
                    }
                    
                    val translationOrPronLines = if (!romanizationEnabled) {
                        cleanTranslatedText.split("\n")
                    } else {
                        (cleanPronunciation ?: cleanTranslatedText).split("\n")
                    }

                    // Interleave the original and translated lines
                    val interleaved = StringBuilder()
                    val maxLines = maxOf(originalLines.size, translationOrPronLines.size)
                    for (i in 0 until maxLines) {
                        val origLine = originalLines.getOrNull(i)?.trim() ?: ""
                        val transLine = translationOrPronLines.getOrNull(i)?.trim() ?: ""
                        
                        if (origLine.isNotEmpty()) {
                            interleaved.append(origLine)
                        }
                        if (transLine.isNotEmpty()) {
                            if (origLine.isNotEmpty()) interleaved.append("\n")
                            interleaved.append("[$transLine]")
                        }
                        if (i < maxLines - 1) interleaved.append("\n")
                    }
                    interleaved.toString()
                }
                outputText?.replace("\\r","\r")?.replace("\\n","\n")
            } catch (e: Exception) {
                if(isSync){
                    Timber.e("Lyrics sync translation ${e.stackTraceToString()}")
                } else {
                    Timber.e("Lyrics not sync translation ${e.stackTraceToString()}")
                }
            }
        }
        val translatedText = if (result.toString() == "kotlin.Unit") "" else result.toString()
        onPlaceholderDismissed()
        output.value = translatedText
        
        withContext(Dispatchers.Main) {
            if (translatedText.isNotEmpty()) {
                app.kreate.android.me.knighthat.utils.Toaster.e(app.n_zik.android.R.string.translation_successful)
            } else if (translateEnabled) {
                app.kreate.android.me.knighthat.utils.Toaster.e(app.n_zik.android.R.string.translation_failed)
            }
        }
    }
}
