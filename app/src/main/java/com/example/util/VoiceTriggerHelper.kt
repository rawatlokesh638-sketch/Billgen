package com.example.util

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import java.util.Locale

class VoiceTriggerHelper(
    private val context: Context,
    private val onWakeWordDetected: () -> Unit,
    private val onCommandDetected: (String) -> Unit
) {
    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false
    private var isWaitingForCommand = false

    private val mainHandler = Handler(Looper.getMainLooper())

    private val listener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            Log.d("VoiceTriggerHelper", "Ready for speech...")
        }

        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {}

        override fun onError(error: Int) {
            Log.e("VoiceTriggerHelper", "Speech error: $error")
            // Restart listening if we got an error (e.g. timeout) to maintain active listening
            if (isListening) {
                mainHandler.postDelayed({
                    restartListening()
                }, 1000)
            }
        }

        override fun onResults(results: Bundle?) {
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val text = matches?.firstOrNull()?.lowercase(Locale.getDefault()) ?: ""
            Log.d("VoiceTriggerHelper", "Heard text: $text")

            if (!isWaitingForCommand) {
                // Look for wake word
                if (text.contains("hello billgen") || text.contains("hello bill gen") || text.contains("hello bilgen") || text.contains("hello bill") || text.contains("hello billgin")) {
                    isWaitingForCommand = true
                    onWakeWordDetected()
                    // Re-start listening immediately to capture the command
                    mainHandler.postDelayed({
                        startCommandListening()
                    }, 500)
                } else {
                    // Not wake word, continue listening
                    restartListening()
                }
            } else {
                // Command detected!
                if (text.isNotBlank()) {
                    onCommandDetected(text)
                }
                isWaitingForCommand = false
                // Return to waiting for wake word
                restartListening()
            }
        }

        override fun onPartialResults(partialResults: Bundle?) {}
        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    fun startListening() {
        if (isListening) return
        isListening = true
        isWaitingForCommand = false
        mainHandler.post {
            try {
                if (SpeechRecognizer.isRecognitionAvailable(context)) {
                    speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                        setRecognitionListener(listener)
                    }
                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                    }
                    speechRecognizer?.startListening(intent)
                }
            } catch (e: Exception) {
                Log.e("VoiceTriggerHelper", "Failed to start listening: ${e.message}")
            }
        }
    }

    private fun startCommandListening() {
        mainHandler.post {
            try {
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                    putExtra(RecognizerIntent.EXTRA_PROMPT, "Batao kya karna hai?")
                }
                speechRecognizer?.startListening(intent)
            } catch (e: Exception) {
                isWaitingForCommand = false
                restartListening()
            }
        }
    }

    private fun restartListening() {
        if (!isListening) return
        mainHandler.post {
            try {
                speechRecognizer?.cancel()
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                }
                speechRecognizer?.startListening(intent)
            } catch (e: Exception) {
                Log.e("VoiceTriggerHelper", "Error restarting listener: ${e.message}")
            }
        }
    }

    fun stopListening() {
        isListening = false
        isWaitingForCommand = false
        mainHandler.post {
            try {
                speechRecognizer?.stopListening()
                speechRecognizer?.cancel()
                speechRecognizer?.destroy()
                speechRecognizer = null
            } catch (e: Exception) {
                Log.e("VoiceTriggerHelper", "Error stopping listener: ${e.message}")
            }
        }
    }
}
