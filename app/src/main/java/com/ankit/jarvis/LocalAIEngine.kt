/*
 * Copyright ©️ 2026 Ankit Gothwal.
 * All rights reserved, subject to applicable third-party license terms.
 *
 * Project: JARVIS AI
 * Developer: Ankit Gothwal
 * Application ID: com.ankit.jarvis
 */
package com.ankit.jarvis

import android.app.ActivityManager
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

/**
 * Represents the current operational state of the local on-device AI engine.
 */
enum class AIModelState(val label: String) {
    UNINITIALIZED("INITIALIZING..."),
    READY_GGUF_NATIVE("OFFLINE LLM ACTIVE (GGUF)"),
    READY_RULE_ENGINE("OFFLINE DETERMINISTIC BRAIN (RULE FALLBACK)"),
    LOW_MEMORY_FALLBACK("LOW RAM SAFEGUARD — RULE ENGINE ACTIVE"),
    LOADING("LOADING LOCAL MODEL...")
}

/**
 * Offline-first AI Engine for JARVIS AI.
 *
 * Architecture:
 * 1. Checks device available RAM via ActivityManager to protect both the Infinix Hot 10 (6 GB RAM)
 *    and the secondary ASUS Zenfone Max M1 (2/3 GB RAM) from OutOfMemory crashes.
 * 2. Searches for a user-supplied quantized `.gguf` model (e.g., Qwen2.5-0.5B-Instruct-Q4_K_M.gguf
 *    or TinyLlama-1.1B-Chat-Q4_K_M.gguf) inside the app's private external directory:
 *    `/sdcard/Android/data/com.ankit.jarvis/files/models/`
 * 3. If the native `jarvis_llama_jni` bridge is compiled and a valid GGUF model is present with sufficient
 *    free RAM (>= 700 MB free), executes local on-device GGUF inference.
 * 4. Otherwise, seamlessly runs the fast, zero-latency bilingual (English + Hindi/Hinglish) Rule-Based
 *    Knowledge & Assistant Fallback Engine so JARVIS works out-of-the-box with 0 MB downloads required.
 *
 * Never connects to cloud APIs or falsely claims cloud inference as offline inference.
 */
class LocalAIEngine(
    private val context: Context,
    private val memoryManager: LocalMemoryManager
) {

    var currentState: AIModelState = AIModelState.UNINITIALIZED
        private set

    var activeModelName: String = "Built-in Rule Engine v1.0"
        private set

    private var nativeBridgeAvailable = false
    private var nativeContextPtr: Long = 0L

    companion object {
        private const val MIN_FREE_RAM_MB_FOR_LLM = 650L
        const val DEFAULT_GGUF_FILENAME = "qwen2.5-0.5b-instruct-q4_k_m.gguf"
    }

    init {
        try {
            System.loadLibrary("jarvis_llama_jni")
            nativeBridgeAvailable = true
        } catch (e: UnsatisfiedLinkError) {
            // Native llama.cpp shared library not compiled in this build; rule-based fallback remains 100% functional
            nativeBridgeAvailable = false
        }
    }

    /**
     * Returns the directory where the user can place offline `.gguf` quantized LLM models.
     */
    fun getModelsDirectory(): File {
        val baseDir = context.getExternalFilesDir(null) ?: context.filesDir
        val modelDir = File(baseDir, "models")
        if (!modelDir.exists()) {
            modelDir.mkdirs()
        }
        return modelDir
    }

    /**
     * Checks device free RAM in megabytes.
     */
    fun getAvailableRamMb(): Long {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        am.getMemoryInfo(memInfo)
        return memInfo.availMem / (1024 * 1024)
    }

    /**
     * Checks total device RAM in megabytes.
     */
    fun getTotalRamMb(): Long {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        am.getMemoryInfo(memInfo)
        return memInfo.totalMem / (1024 * 1024)
    }

    /**
     * Initializes or reloads the local AI model asynchronously on Dispatchers.IO.
     */
    suspend fun initializeEngine(onProgress: (AIModelState, String) -> Unit) = withContext(Dispatchers.IO) {
        currentState = AIModelState.LOADING
        withContext(Dispatchers.Main) {
            onProgress(currentState, "Checking device memory & local GGUF model files...")
        }

        val availRamMb = getAvailableRamMb()
        val totalRamMb = getTotalRamMb()
        val modelDir = getModelsDirectory()

        // Find either the custom configured GGUF file or any .gguf file in the models folder
        val customPath = memoryManager.customModelPath
        val candidateFile: File? = when {
            customPath.isNotEmpty() && File(customPath).exists() -> File(customPath)
            File(modelDir, DEFAULT_GGUF_FILENAME).exists() -> File(modelDir, DEFAULT_GGUF_FILENAME)
            else -> modelDir.listFiles()?.firstOrNull { it.isFile && it.name.endsWith(".gguf", ignoreCase = true) }
        }

        if (candidateFile != null && candidateFile.exists()) {
            if (availRamMb < MIN_FREE_RAM_MB_FOR_LLM) {
                currentState = AIModelState.LOW_MEMORY_FALLBACK
                activeModelName = "Low RAM Guard (${availRamMb}MB free / ${totalRamMb}MB total)"
                withContext(Dispatchers.Main) {
                    onProgress(
                        currentState,
                        "Low available memory (${availRamMb} MB free). Using fast Rule Engine to prevent device lag."
                    )
                }
                return@withContext
            }

            if (nativeBridgeAvailable) {
                try {
                    if (nativeContextPtr != 0L) {
                        nativeFreeModel(nativeContextPtr)
                        nativeContextPtr = 0L
                    }
                    nativeContextPtr = nativeInitModel(candidateFile.absolutePath, 2)
                    if (nativeContextPtr != 0L) {
                        currentState = AIModelState.READY_GGUF_NATIVE
                        activeModelName = candidateFile.name
                        withContext(Dispatchers.Main) {
                            onProgress(currentState, "Loaded offline GGUF model: ${candidateFile.name} (${availRamMb} MB RAM free)")
                        }
                        return@withContext
                    }
                } catch (t: Throwable) {
                    nativeContextPtr = 0L
                }
            }
        }

        // Default to the built-in deterministic offline Rule-Based Assistant
        currentState = AIModelState.READY_RULE_ENGINE
        activeModelName = "Built-in Offline Rule Engine (EN/HI)"
        val hint = if (candidateFile == null) {
            "Offline Rule Engine Active (${availRamMb} MB free RAM). Optional: Place a .gguf model in ${modelDir.absolutePath} for LLM chat."
        } else {
            "GGUF file detected (${candidateFile.name}), running built-in Rule Engine until NDK llama.cpp bridge is enabled."
        }
        withContext(Dispatchers.Main) {
            onProgress(currentState, hint)
        }
    }

    /**
     * Generates a response strictly on-device.
     */
    suspend fun generateResponse(userPrompt: String): Pair<String, String> = withContext(Dispatchers.Default) {
        val availRamMb = getAvailableRamMb()

        // If native GGUF model is loaded and sufficient RAM remains, run native inference
        if (currentState == AIModelState.READY_GGUF_NATIVE && nativeContextPtr != 0L) {
            if (availRamMb < 350L) {
                // Graceful memory pressure fallback mid-session
                val fallbackReply = generateRuleBasedReply(userPrompt)
                return@withContext Pair(
                    """$fallbackReply

[Memory Notice: Switched to Rule Fallback because free RAM dropped to $availRamMb MB.]""",
                    "LOW-RAM FALLBACK"
                )
            }
            try {
                val reply = nativeCompletion(nativeContextPtr, userPrompt, 128)
                if (reply.isNotBlank()) {
                    return@withContext Pair(reply.trim(), "LOCAL GGUF LLM")
                }
            } catch (t: Throwable) {
                // Fall through to deterministic rule-based response
            }
        }

        val reply = generateRuleBasedReply(userPrompt)
        return@withContext Pair(reply, "OFFLINE RULE BRAIN")
    }

    /**
     * Comprehensive deterministic offline conversational, math, productivity, and Hinglish assistant fallback.
     */
    private fun generateRuleBasedReply(input: String): String {
        val trimmed = input.trim()
        val lower = trimmed.lowercase(Locale.ROOT)

        // 1. Greetings (English & Hindi/Hinglish)
        if (lower in listOf("hi", "hello", "hey", "jarvis", "hello jarvis", "hey jarvis", "namaste", "namaskar", "kaise ho", "kya haal hai")) {
            return if (lower.contains("namaste") || lower.contains("kaise ho") || lower.contains("kya haal")) {
                "Namaste! Main JARVIS hoon, Ankit Gothwal dwara banaya gaya aapka offline personal AI assistant. Main aapki kya madad kar sakta hoon?"
            } else {
                "Hello! I am JARVIS, your offline personal AI assistant created by Ankit Gothwal. All systems are online and running locally on your device. How can I assist you?"
            }
        }

        // 2. Capabilities & Help
        if (lower.contains("what can you do") || lower.contains("help") || lower.contains("commands") || lower.contains("kya kar sakte ho")) {
            return """
                I operate 100% offline on your phone. Here are commands you can speak or type:
                • Phone Control: "Open Camera", "Open Settings", "Open Wi-Fi Settings", or "Open <App Name>"
                • Audio & Media: "Volume up", "Volume down", "Mute volume", "Play music", "Pause music", "Next song"
                • Local Notes: "Note down buy groceries at 6 PM", "Show my notes", "Clear my notes"
                • Time, Date & Telemetry: "What time is it?", "What is today's date?", "Battery status"
                • Quick Math: "Calculate 45 * 18"
                • Hindi / Hinglish: "Time kya hua hai", "Camera kholo", "Awaz badhao", "Mere notes dikhao"
            """.trimIndent()
        }

        // 3. Offline / Privacy verification question
        if (lower.contains("are you offline") || lower.contains("internet") || lower.contains("privacy") || lower.contains("cloud")) {
            return "Yes. I am strictly offline-first. Your voice commands, conversations, and notes are stored and processed locally on your Android device and are never transmitted to external servers."
        }

        // 4. Arithmetic Evaluator (e.g., "calculate 125 + 375", "what is 18 * 12")
        val mathMatch = Regex("(-?\\d+(?:\\.\\d+)?)\\s*([+*/xX-])\\s*(-?\\d+(?:\\.\\d+)?)").find(lower)
        if (mathMatch != null) {
            val a = mathMatch.groupValues[1].toDoubleOrNull()
            val op = mathMatch.groupValues[2]
            val b = mathMatch.groupValues[3].toDoubleOrNull()
            if (a != null && b != null) {
                val res = when (op) {
                    "+" -> a + b
                    "-" -> a - b
                    "*", "x", "X" -> a * b
                    "/" -> if (b != 0.0) a / b else Double.NaN
                    else -> Double.NaN
                }
                if (!res.isNaN()) {
                    val formatted = if (res % 1.0 == 0.0) res.toLong().toString() else String.format(Locale.US, "%.4f", res)
                    return "The result of $a $op $b is $formatted."
                } else {
                    return "Division by zero is undefined."
                }
            }
        }

        // 5. System & Memory Status
        if (lower.contains("status") || lower.contains("system report") || lower.contains("diagnostics") || lower.contains("memory")) {
            val freeMb = getAvailableRamMb()
            val totalMb = getTotalRamMb()
            val notesCount = memoryManager.getAllNotes().size
            return "System Diagnostics: Mode = ${currentState.label} | Model = $activeModelName | Free RAM = $freeMb MB / $totalMb MB | Saved Local Notes = $notesCount."
        }

        // 6. Thank you / Goodbye
        if (lower.contains("thank you") || lower.contains("thanks") || lower.contains("shukriya") || lower.contains("dhanyavad")) {
            return "You're welcome! Let me know whenever you need assistance."
        }

        // 7. Informative Offline Fallback Response
        return "I heard: \"$trimmed\". I am currently running in Offline Deterministic Rule Mode (${getAvailableRamMb()} MB free RAM). For device actions, try saying \"Open Camera\", \"What time is it?\", \"Volume up\", or \"Note down <your text>\". To enable open-ended generative AI chat offline, place a quantized `.gguf` model in ${getModelsDirectory().absolutePath}."
    }

    fun release() {
        if (nativeBridgeAvailable && nativeContextPtr != 0L) {
            try {
                nativeFreeModel(nativeContextPtr)
            } catch (_: Throwable) {
            }
            nativeContextPtr = 0L
        }
    }

    // Optional JNI declarations for llama.cpp C++ bridge (see app/src/main/cpp/jarvis_llama_jni.cpp)
    private external fun nativeInitModel(modelPath: String, numThreads: Int): Long
    private external fun nativeCompletion(contextPtr: Long, prompt: String, maxTokens: Int): String
    private external fun nativeFreeModel(contextPtr: Long)
}
