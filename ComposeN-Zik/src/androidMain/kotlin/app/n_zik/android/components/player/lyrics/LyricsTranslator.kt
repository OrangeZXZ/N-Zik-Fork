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
                val outputText = if (textToTranslate == "") {
                    ""
               }
                else if (!showSecondLine || (mainTranslation.sourceText == mainTranslation.translatedText)){
                    if (!romanizationEnabled) {
                        if (translateEnabled) mainTranslation.translatedText else textToTranslate
                    }
                    else if (helperTranslation.sourceText == helperTranslation.translatedText) helperTranslation.sourcePronunciation else mainTranslation.sourcePronunciation ?: mainTranslation.sourceText
                } else {
                    if (!romanizationEnabled) {
                        textToTranslate + "\\n[${mainTranslation.translatedText}]"
                    } else
                        if (helperTranslation.sourceText == helperTranslation.translatedText){
                            helperTranslation.sourcePronunciation
                        } else {mainTranslation.sourcePronunciation ?: mainTranslation.sourceText} + "\\n[${mainTranslation.translatedPronunciation ?: mainTranslation.translatedText}]"
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
        val translatedText =
            if (result.toString() == "kotlin.Unit") "" else result.toString()
        onPlaceholderDismissed()
        output.value = translatedText
    }
}
