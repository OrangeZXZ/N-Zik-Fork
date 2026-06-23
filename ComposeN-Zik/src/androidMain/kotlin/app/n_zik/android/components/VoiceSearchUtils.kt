package app.n_zik.android.components

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.core.content.ContextCompat
import app.n_zik.android.R
import java.util.Locale

class VoiceSearchUtils(
    private val context: Context,
    private val onResult: (String) -> Unit,
    private val onPartialResult: (String) -> Unit = {},
    private val onError: () -> Unit = {},
    private val onListeningStateChanged: (Boolean) -> Unit = {},
    private val onSpeechDetected: () -> Unit = {}
) {
    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false
    private var hasReceivedResults = false
    private var isCancelled = false
    private val mainHandler = Handler(Looper.getMainLooper())

    private val recognitionListener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            Log.d("VoiceSearch", "Ready")
            hasReceivedResults = false
            isCancelled = false
            isListening = true
            mainHandler.post { onListeningStateChanged(true) }
        }

        override fun onBeginningOfSpeech() {
            Log.d("VoiceSearch", "Speech detected")
            mainHandler.post { onSpeechDetected() }
        }

        override fun onRmsChanged(rmsdB: Float) {}

        override fun onBufferReceived(buffer: ByteArray?) {}

        override fun onEndOfSpeech() {
            Log.d("VoiceSearch", "End of speech")
            if (!isCancelled) {
                mainHandler.post { onListeningStateChanged(false) }
            }
            speechRecognizer?.stopListening()
        }

        override fun onError(error: Int) {
            if (hasReceivedResults || isCancelled) {
                Log.d("VoiceSearch", "Ignoring error $error after results/cancel")
                return
            }
            Log.e("VoiceSearch", "Error: $error")
            isListening = false
            mainHandler.post {
                onListeningStateChanged(false)
                onError()
            }
            speechRecognizer?.stopListening()
            speechRecognizer?.destroy()
            speechRecognizer = null
        }

        override fun onResults(results: Bundle?) {
            Log.d("VoiceSearch", "Results")
            hasReceivedResults = true
            isListening = false
            mainHandler.post { onListeningStateChanged(false) }
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            if (!matches.isNullOrEmpty()) {
                Log.d("VoiceSearch", "Result: ${matches[0]}, Language: ${Locale.getDefault()}")
                mainHandler.post { onResult(matches[0]) }
            }
            speechRecognizer?.stopListening()
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            if (!matches.isNullOrEmpty()) {
                mainHandler.post { onPartialResult(matches[0]) }
            }
        }

        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    fun startListening() {
        if (isListening) {
            stopListening()
            return
        }

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            mainHandler.post { onError() }
            return
        }

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            mainHandler.post { onError() }
            return
        }

        if (speechRecognizer == null) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
            speechRecognizer?.setRecognitionListener(recognitionListener)
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        }

        Log.d("VoiceSearch", "Starting (auto-detect language)")

        speechRecognizer?.startListening(intent)
    }

    fun stopListening() {
        isCancelled = true
        isListening = false
        mainHandler.post { onListeningStateChanged(false) }
        try {
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
        } catch (e: Exception) {
            Log.e("VoiceSearch", "Error stopping", e)
        }
        speechRecognizer = null
    }

    fun destroy() {
        speechRecognizer?.destroy()
        speechRecognizer = null
        isListening = false
    }
}
