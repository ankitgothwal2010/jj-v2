/*
 * Copyright ©️ 2026 Ankit Gothwal.
 * All rights reserved, subject to applicable third-party license terms.
 *
 * Project: JARVIS AI
 * Developer: Ankit Gothwal
 * Application ID: com.ankit.jarvis
 */
package com.ankit.jarvis

import android.content.Context
import android.content.Intent
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale
import java.util.UUID

/**
 * Manages spoken assistant replies using Android's on-device TextToSpeech engine.
 *
 * Features:
 * - Verifies whether offline voice data is actually installed for the selected locale
 *   (never assumes a voice works offline without checking `Voice.features` / `isLanguageAvailable`).
 * - Adjustable speech rate (0.5x to 2.0x).
 * - Language selection (`en-IN`, `en-US`, `hi-IN`).
 * - Mute toggle and immediate stop-speaking controls.
 */
class TextToSpeechManager(
    private val context: Context,
    private val memoryManager: LocalMemoryManager,
    private val onStateChanged: (isSpeaking: Boolean, statusMessage: String) -> Unit
) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    var isInitialized: Boolean = false
        private set

    var isSpeakingNow: Boolean = false
        private set

    init {
        tts = TextToSpeech(context.applicationContext, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isInitialized = true
             tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    isSpeakingNow = true
                    onStateChanged(true, "SPEAKING...")
                }

                override fun onDone(utteranceId: String?) {
                    isSpeakingNow = false
                    onStateChanged(false, "VOICE READY")
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    isSpeakingNow = false
                    onStateChanged(false, "TTS ERROR")
                }
            })
            applyLanguageAndRate(memoryManager.preferredLanguage, memoryManager.speechRate)
        } else {
            isInitialized = false
            onStateChanged(false, "TTS ENGINE UNAVAILABLE")
        }
    }

    /**
     * Configures the TTS language and speech rate, verifying whether offline voice data is present.
     */
    fun applyLanguageAndRate(languageTag: String, rate: Float): String {
        val engine = tts ?: return "Text-to-Speech engine not initialized."
        if (!isInitialized) return "Text-to-Speech is still initializing."

        engine.setSpeechRate(rate.coerceIn(0.5f, 2.0f))

        val locale = Locale.forLanguageTag(languageTag)
        val availability = engine.isLanguageAvailable(locale)

        return when (availability) {
            TextToSpeech.LANG_COUNTRY_AVAILABLE,
            TextToSpeech.LANG_COUNTRY_VAR_AVAILABLE,
            TextToSpeech.LANG_AVAILABLE -> {
                engine.language = locale
                val currentVoice = engine.voice
                val requiresNetwork = currentVoice?.isNetworkConnectionRequired ?: false
                if (requiresNetwork) {
                    onStateChanged(false, "TTS WARNING: VOICE REQUIRES NETWORK")
                    "Selected language ($languageTag) is available, but its active voice may require offline voice data installation in Android TTS Settings."
                } else {
                    onStateChanged(false, "OFFLINE TTS READY ($languageTag)")
                    "Offline voice verified for $languageTag at ${String.format(Locale.US, "%.1fx", rate)} speed."
                }
            }
            TextToSpeech.LANG_MISSING_DATA -> {
                onStateChanged(false, "TTS DATA MISSING ($languageTag)")
                "Offline voice data for $languageTag is missing on this phone. Use 'Install Offline Voice Data' in Settings."
            }
            TextToSpeech.LANG_NOT_SUPPORTED -> {
                // Fallback to English if Hindi or regional variant is unsupported
                engine.language = Locale.US
                onStateChanged(false, "FALLBACK TO EN-US TTS")
                "Language $languageTag is not supported by your device's TTS engine. Reverted to English."
            }
            else -> "TTS language status code: $availability"
        }
    }

    /**
     * Speaks the assistant response aloud if voice output is not muted.
     */
    fun speak(text: String) {
        if (memoryManager.isVoiceMuted) {
            onStateChanged(false, "VOICE MUTED")
            return
        }
        val engine = tts ?: return
        if (!isInitialized) return

        // Strip markdown bullets for cleaner spoken output
        val cleanText = text
            .replace("•", "")
            .replace("*", "")
            .trim()

        engine.setSpeechRate(memoryManager.speechRate)
        val utteranceId = UUID.randomUUID().toString()
        engine.speak(cleanText, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    /**
     * Immediately halts any ongoing spoken reply.
     */
    fun stopSpeaking() {
        try {
            tts?.stop()
        } catch (_: Exception) {
        }
        isSpeakingNow = false
        onStateChanged(false, "SPEECH STOPPED")
    }

    /**
     * Opens the system Text-to-Speech settings so the user can download official offline voice packs.
     */
    fun promptInstallVoiceData(context: Context) {
        try {
            val installIntent = Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(installIntent)
        } catch (e: Exception) {
            try {
                val fallback = Intent("com.android.settings.TTS_SETTINGS").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallback)
            } catch (_: Exception) {
            }
        }
    }

    fun shutdown() {
        stopSpeaking()
        try {
            tts?.shutdown()
        } catch (_: Exception) {
        }
        tts = null
        isInitialized = false
    }
}
