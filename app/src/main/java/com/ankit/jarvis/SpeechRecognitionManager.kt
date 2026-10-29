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
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Environment
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.vosk.LibVosk
import org.vosk.LogLevel
import org.vosk.Model
import org.vosk.Recognizer
import java.io.File
import java.io.FileOutputStream

/**
 * Upgraded Offline Voice Input Manager for JARVIS AI (`com.ankit.jarvis`).
 *
 * WHY VOSK MODELS PREVIOUSLY FAILED & HOW THIS FIXES IT:
 * 1. Nested Folder Bug Fixed: When users unzip `vosk-model-small-en-in-0.4.zip`, Windows often creates
 *    a double nested folder (`vosk-model-small-en-in/vosk-model-small-en-in-0.4/am/...`), causing
 *    `org.vosk.Model` to throw an `IOException: Failed to create a model`.
 *    -> `findValidVoskModelDirectory()` now recursively scans up to 4 levels deep to locate the exact
 *       directory containing `conf/model.conf`, `am/final.mdl`, or `final.mdl`.
 * 2. Multi-Location Scanner: Automatically searches:
 *    - `/sdcard/Android/data/com.ankit.jarvis/files/vosk-models/`
 *    - `/sdcard/Download/` and `/sdcard/JarvisModels/`
 *    - Internal app storage (`context.filesDir/vosk-models/`)
 *    - APK bundled assets (`assets/model-en-in` or `assets/model-hi`)
 * 3. Direct AudioRecord Streaming Loop: Instead of relying on `org.vosk.android.SpeechService` (which
 *    can fail on certain MediaTek/Infinix audio hal implementations), uses a dedicated 16kHz mono
 *    PCM 16-bit `AudioRecord` coroutine loop with live RMS level updates and clean silence detection.
 */
class SpeechRecognitionManager(
    private val context: Context,
    private val coroutineScope: CoroutineScope,
    private val callback: Listener
) {

    interface Listener {
        fun onListeningStateChanged(isListening: Boolean, engineLabel: String)
        fun onPartialTranscript(partialText: String)
        fun onFinalTranscript(finalText: String)
        fun onSpeechError(userFriendlyError: String)
        fun onAudioLevelUpdate(normalizedLevel: Float)
    }

    private var voskModel: Model? = null
    private var loadedModelPath: String = ""
    private var voskJob: Job? = null
    private var androidSpeechRecognizer: SpeechRecognizer? = null
    @Volatile
    private var isCurrentlyListening = false

    companion object {
        const val FOLDER_EN_IN = "vosk-model-small-en-in"
        const val FOLDER_HI = "vosk-model-small-hi"
        private const val SAMPLE_RATE = 16000
    }

    init {
        try {
            LibVosk.setLogLevel(LogLevel.WARNINGS)
        } catch (_: Throwable) {
        }
    }

    fun getVoskModelsRootDir(): File {
        val base = context.getExternalFilesDir(null) ?: context.filesDir
        val dir = File(base, "vosk-models")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    /**
     * Checks whether a directory is a genuine Vosk acoustic model root folder.
     */
    private fun isActualVoskModelRoot(dir: File): Boolean {
        if (!dir.exists() || !dir.isDirectory) return false
        val hasConf = File(dir, "conf").isDirectory || File(dir, "mfcc.conf").exists()
        val hasAm = File(dir, "am").isDirectory || File(dir, "final.mdl").exists() || File(dir, "ivector").isDirectory
        val hasGraph = File(dir, "graph").isDirectory || File(dir, "HCLG.fst").exists() || File(dir, "Gr.fst").exists()
        return (hasConf && hasAm) || (hasAm && hasGraph)
    }

    /**
     * Recursively searches inside `root` (up to `maxDepth` levels) for a valid Vosk model directory,
     * preferring folders matching the requested language (`hi` vs `en`).
     */
    private fun scanRecursivelyForModel(root: File, preferHindi: Boolean, depth: Int = 0, maxDepth: Int = 4): List<File> {
        if (!root.exists() || !root.isDirectory || depth > maxDepth) return emptyList()
        if (isActualVoskModelRoot(root)) {
            return listOf(root)
        }
        val found = mutableListOf<File>()
        val children = root.listFiles() ?: return emptyList()
        for (child in children) {
            if (child.isDirectory) {
                found.addAll(scanRecursivelyForModel(child, preferHindi, depth + 1, maxDepth))
            }
        }
        return found.sortedByDescending { candidate ->
            val name = candidate.absolutePath.lowercase()
            if (preferHindi) {
                name.contains("-hi") || name.contains("_hi") || name.contains("hindi")
            } else {
                name.contains("-en") || name.contains("_en") || name.contains("english")
            }
        }
    }

    /**
     * Copies a bundled Vosk model from APK `assets/vosk-model-en` or `assets/vosk-model-hi` if present.
     */
    private fun extractAssetModelIfBundled(languageCode: String): File? {
        val assetFolder = if (languageCode.startsWith("hi", ignoreCase = true)) "vosk-model-hi" else "vosk-model-en"
        return try {
            val list = context.assets.list(assetFolder)
            if (list.isNullOrEmpty()) return null
            val targetDir = File(context.filesDir, "vosk-assets/$assetFolder")
            if (isActualVoskModelRoot(targetDir)) return targetDir
            copyAssetDirRecursively(assetFolder, targetDir)
            if (isActualVoskModelRoot(targetDir)) targetDir else null
        } catch (_: Exception) {
            null
        }
    }

    private fun copyAssetDirRecursively(assetPath: String, destDir: File) {
        val children = context.assets.list(assetPath) ?: return
        if (children.isEmpty()) {
            destDir.parentFile?.mkdirs()
            context.assets.open(assetPath).use { input ->
                FileOutputStream(destDir).use { output ->
                    input.copyTo(output)
                }
            }
        } else {
            destDir.mkdirs()
            for (child in children) {
                copyAssetDirRecursively("$assetPath/$child", File(destDir, child))
            }
        }
    }

    /**
     * Searches all candidate storage locations on the phone and returns the exact Vosk model folder.
     */
    fun getLocalVoskModelDir(languageCode: String): File? {
        val preferHindi = languageCode.startsWith("hi", ignoreCase = true)

        // 1. Check APK assets first if user bundled a model inside app/src/main/assets/
        extractAssetModelIfBundled(languageCode)?.let { return it }

        // 2. Search candidate directories on internal & external storage
        val candidateRoots = mutableListOf<File>()
        candidateRoots.add(getVoskModelsRootDir())
        context.getExternalFilesDir(null)?.let { candidateRoots.add(it) }
        candidateRoots.add(File(context.filesDir, "vosk-models"))
        try {
            val extRoot = Environment.getExternalStorageDirectory()
            candidateRoots.add(File(extRoot, "JarvisModels"))
            candidateRoots.add(File(extRoot, "Download/vosk-models"))
            candidateRoots.add(File(extRoot, "Download"))
        } catch (_: Exception) {
        }

        for (root in candidateRoots) {
            val matches = scanRecursivelyForModel(root, preferHindi)
            if (matches.isNotEmpty()) {
                return matches.first()
            }
        }
        return null
    }

    /**
     * Returns a human-readable diagnostic report of Vosk model detection for SettingsActivity.
     */
    fun getDiagnosticStatus(languageCode: String): String {
        val detected = getLocalVoskModelDir(languageCode)
        return if (detected != null) {
            "VOSK MODEL READY: ${detected.absolutePath}"
        } else {
            """VOSK MODEL NOT FOUND YET. Place unzipped Vosk model folder inside:
${getVoskModelsRootDir().absolutePath}
(or inside /sdcard/JarvisModels/)"""
        }
    }

    fun startListening(languageCode: String) {
        if (!PermissionManager.hasAudioPermission(context)) {
            callback.onSpeechError("Microphone permission is not granted. Please allow Microphone access.")
            return
        }

        if (isCurrentlyListening) {
            stopListening()
            return
        }

        val localVoskDir = getLocalVoskModelDir(languageCode)
        if (localVoskDir != null) {
            startVoskAudioRecordLoop(localVoskDir, languageCode)
        } else {
            startAndroidOfflineRecognizer(languageCode)
        }
    }

    /**
     * Reliable 16kHz AudioRecord streaming loop feeding Vosk `Recognizer` on Dispatchers.IO.
     */
    private fun startVoskAudioRecordLoop(modelDir: File, languageCode: String) {
        voskJob?.cancel()
        voskJob = coroutineScope.launch(Dispatchers.IO) {
            var audioRecord: AudioRecord? = null
            var recognizer: Recognizer? = null
            try {
                withContext(Dispatchers.Main) {
                    callback.onListeningStateChanged(true, "LOADING VOSK MODEL (${modelDir.name})...")
                }

                if (voskModel == null || loadedModelPath != modelDir.absolutePath) {
                    voskModel?.close()
                    voskModel = Model(modelDir.absolutePath)
                    loadedModelPath = modelDir.absolutePath
                }

                recognizer = Recognizer(voskModel, SAMPLE_RATE.toFloat())

                val minBufferSize = AudioRecord.getMinBufferSize(
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                ).coerceAtLeast(4096)

                audioRecord = AudioRecord(
                    MediaRecorder.AudioSource.VOICE_RECOGNITION,
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    minBufferSize * 2
                )

                if (audioRecord.state != AudioRecord.STATE_INITIALIZED) {
                    throw IllegalStateException("Microphone AudioRecord failed to initialize on this device.")
                }

                audioRecord.startRecording()
                isCurrentlyListening = true

                withContext(Dispatchers.Main) {
                    callback.onListeningStateChanged(true, "VOSK OFFLINE (${modelDir.name})")
                }

                val buffer = ShortArray(2048)
                val startTime = System.currentTimeMillis()
                var lastSpeechTime = startTime
                var heardAnySpeech = false

                while (isActive && isCurrentlyListening) {
                    val readCount = audioRecord.read(buffer, 0, buffer.size)
                    if (readCount > 0) {
                        // Calculate RMS audio level for the circular HUD visualizer
                        var sumSquares = 0.0
                        for (i in 0 until readCount) {
                            val sample = buffer[i].toDouble()
                            sumSquares += sample * sample
                        }
                        val rms = kotlin.math.sqrt(sumSquares / readCount)
                        val normalized = (rms / 3500.0).toFloat().coerceIn(0.05f, 1.0f)
                        withContext(Dispatchers.Main) {
                            callback.onAudioLevelUpdate(normalized)
                        }

                        if (recognizer.acceptWaveForm(buffer, readCount)) {
                            val resultText = parseVoskJson(recognizer.result, "text")
                            if (resultText.isNotEmpty()) {
                                isCurrentlyListening = false
                                withContext(Dispatchers.Main) {
                                    callback.onListeningStateChanged(false, "READY")
                                    callback.onFinalTranscript(resultText)
                                }
                                return@launch
                            }
                        } else {
                            val partialText = parseVoskJson(recognizer.partialResult, "partial")
                            if (partialText.isNotEmpty()) {
                                heardAnySpeech = true
                                lastSpeechTime = System.currentTimeMillis()
                                withContext(Dispatchers.Main) {
                                    callback.onPartialTranscript(partialText)
                                }
                            }
                        }
                    }

                    val now = System.currentTimeMillis()
                    // Stop after 2.2 seconds of silence once speech has started, or 8 seconds total timeout
                    if ((heardAnySpeech && now - lastSpeechTime > 2200L) || (now - startTime > 8500L)) {
                        break
                    }
                }

                val finalText = parseVoskJson(recognizer.finalResult, "text")
                isCurrentlyListening = false
                withContext(Dispatchers.Main) {
                    callback.onAudioLevelUpdate(0f)
                    callback.onListeningStateChanged(false, "READY")
                    if (finalText.isNotEmpty()) {
                        callback.onFinalTranscript(finalText)
                    } else {
                        callback.onSpeechError("No clear speech heard. Tap MIC: SPEAK and speak clearly.")
                    }
                }
            } catch (e: Throwable) {
                isCurrentlyListening = false
                withContext(Dispatchers.Main) {
                    callback.onAudioLevelUpdate(0f)
                    callback.onListeningStateChanged(false, "VOSK FALLBACK")
                    callback.onSpeechError(
                        "Vosk model error (${modelDir.absolutePath}): ${e.localizedMessage}. Switching to Android speech recognizer."
                    )
                    startAndroidOfflineRecognizer(languageCode)
                }
            } finally {
                try {
                    audioRecord?.stop()
                    audioRecord?.release()
                } catch (_: Exception) {
                }
                try {
                    recognizer?.close()
                } catch (_: Exception) {
                }
            }
        }
    }

    private fun startAndroidOfflineRecognizer(languageCode: String) {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            val voskPath = getVoskModelsRootDir().absolutePath
            callback.onSpeechError(
                """No system speech service found and no Vosk model found yet. Copy unzipped 'vosk-model-small-en-in' into:
$voskPath"""
            )
            return
        }

        try {
            androidSpeechRecognizer?.destroy()
            val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
            androidSpeechRecognizer = recognizer

            // Try offline first; if device lacks offline pack, allow standard recognizer intent
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageCode)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            }

            recognizer.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    isCurrentlyListening = true
                    callback.onListeningStateChanged(true, "ANDROID ASR ($languageCode)")
                }

                override fun onBeginningOfSpeech() {
                    callback.onAudioLevelUpdate(0.5f)
                }

                override fun onRmsChanged(rmsdB: Float) {
                    val normalized = ((rmsdB + 2f) / 12f).coerceIn(0.05f, 1.0f)
                    callback.onAudioLevelUpdate(normalized)
                }

                override fun onBufferReceived(buffer: ByteArray?) {}

                override fun onEndOfSpeech() {
                    isCurrentlyListening = false
                    callback.onListeningStateChanged(false, "PROCESSING SPEECH...")
                }

                override fun onError(error: Int) {
                    isCurrentlyListening = false
                    callback.onListeningStateChanged(false, "READY")
                    val voskPath = getVoskModelsRootDir().absolutePath
                    val message = when (error) {
                        SpeechRecognizer.ERROR_NO_MATCH,
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT ->
                            "No speech detected. Speak clearly after tapping MIC: SPEAK."
                        SpeechRecognizer.ERROR_NETWORK,
                        SpeechRecognizer.ERROR_NETWORK_TIMEOUT ->
                            """Android built-in speech needs offline voice pack or Vosk model. For 100% offline voice, place unzipped 'vosk-model-small-en-in' inside:
$voskPath"""
                        else ->
                            """Speech error (code $error). For guaranteed offline voice recognition, extract 'vosk-model-small-en-in' into:
$voskPath"""
                    }
                    callback.onSpeechError(message)
                }

                override fun onResults(results: Bundle?) {
                    isCurrentlyListening = false
                    callback.onListeningStateChanged(false, "READY")
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val best = matches?.firstOrNull()?.trim().orEmpty()
                    if (best.isNotEmpty()) {
                        callback.onFinalTranscript(best)
                    } else {
                        callback.onSpeechError("Could not recognize speech. Please try again.")
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val partial = matches?.firstOrNull()?.trim().orEmpty()
                    if (partial.isNotEmpty()) {
                        callback.onPartialTranscript(partial)
                    }
                }

                override fun onEvent(eventType: Int, params: Bundle?) {}
            })

            recognizer.startListening(intent)
        } catch (e: Exception) {
            isCurrentlyListening = false
            callback.onListeningStateChanged(false, "READY")
            callback.onSpeechError("Unable to start speech recognition: ${e.localizedMessage}")
        }
    }

    fun stopListening() {
        isCurrentlyListening = false
        try {
            voskJob?.cancel()
            voskJob = null
        } catch (_: Exception) {
        }
        try {
            androidSpeechRecognizer?.stopListening()
            androidSpeechRecognizer?.cancel()
        } catch (_: Exception) {
        }
        callback.onAudioLevelUpdate(0f)
        callback.onListeningStateChanged(false, "READY")
    }

    fun destroy() {
        stopListening()
        try {
            voskModel?.close()
            voskModel = null
        } catch (_: Exception) {
        }
        try {
            androidSpeechRecognizer?.destroy()
            androidSpeechRecognizer = null
        } catch (_: Exception) {
        }
    }

    private fun parseVoskJson(jsonStr: String?, key: String): String {
        if (jsonStr.isNullOrBlank()) return ""
        return try {
            val obj = JSONObject(jsonStr)
            obj.optString(key, "").trim()
        } catch (_: Exception) {
            ""
        }
    }
}
