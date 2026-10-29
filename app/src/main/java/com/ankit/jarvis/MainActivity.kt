/*
 * Copyright ©️ 2026 Ankit Gothwal.
 * All rights reserved, subject to applicable third-party license terms.
 *
 * Project: JARVIS AI
 * Developer: Ankit Gothwal
 * Application ID: com.ankit.jarvis
 */
package com.ankit.jarvis

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.ankit.jarvis.databinding.ActivityMainBinding
import kotlinx.coroutines.launch

/**
 * Main HUD Activity for JARVIS — Personal AI Assistant.
 * Created and developed by Ankit Gothwal.
 *
 * Coordinates:
 * - Animated Circular HUD Voice Visualizer (`HudVoiceVisualizerView`)
 * - Offline Voice Input (`SpeechRecognitionManager`)
 * - Offline Spoken Replies (`TextToSpeechManager`)
 * - Safe Phone Control Intents (`CommandExecutor`)
 * - On-Device Local AI Engine (`LocalAIEngine`)
 * - Local SQLite Conversation & Notes Storage (`LocalMemoryManager`)
 */
class MainActivity : AppCompatActivity(), SpeechRecognitionManager.Listener {

    private lateinit var binding: ActivityMainBinding
    private lateinit var memoryManager: LocalMemoryManager
    private lateinit var permissionManager: PermissionManager
    private lateinit var commandExecutor: CommandExecutor
    private lateinit var localAIEngine: LocalAIEngine
    private lateinit var speechManager: SpeechRecognitionManager
    private lateinit var ttsManager: TextToSpeechManager
    private lateinit var chatAdapter: ChatAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        memoryManager = LocalMemoryManager(this)
        permissionManager = PermissionManager(this)
        commandExecutor = CommandExecutor(this, memoryManager)
        localAIEngine = LocalAIEngine(this, memoryManager)

        setupRecyclerView()
        setupSpeechAndTts()
        setupControls()
        initializeOfflineBrain()
    }

    override fun onResume() {
        super.onResume()
        applyUserPreferences()
    }

    private fun setupRecyclerView() {
        val history = memoryManager.getRecentMessages().toMutableList()
        if (history.isEmpty()) {
            val welcome = memoryManager.saveMessage(
                ChatMessage.Sender.JARVIS,
                "JARVIS HUD online. Created by Ankit Gothwal. Tap the microphone or type a command below (e.g., 'What time is it?', 'Open Camera', 'Note down meeting at 5 PM', or 'Help').",
                "SYSTEM INIT"
            )
            history.add(welcome)
        }
        chatAdapter = ChatAdapter(history)
        binding.rvConversation.apply {
            layoutManager = LinearLayoutManager(this@MainActivity).apply {
                stackFromEnd = true
            }
            adapter = chatAdapter
        }
    }

    private fun setupSpeechAndTts() {
        speechManager = SpeechRecognitionManager(this, lifecycleScope, this)
        ttsManager = TextToSpeechManager(this, memoryManager) { isSpeaking, statusText ->
            runOnUiThread {
                binding.tvTtsStatus.text = statusText
                if (isSpeaking) {
                    binding.hudVisualizer.setHudMode(HudVoiceVisualizerView.HudMode.SPEAKING)
                    binding.btnStopSpeaking.visibility = View.VISIBLE
                } else {
                    binding.hudVisualizer.setHudMode(HudVoiceVisualizerView.HudMode.IDLE)
                    binding.btnStopSpeaking.visibility = View.GONE
                }
            }
        }
    }

    private fun setupControls() {
        // Microphone Button
        binding.btnMic.setOnClickListener {
            ttsManager.stopSpeaking()
            permissionManager.ensureMicrophonePermission(
                onGranted = {
                    speechManager.startListening(memoryManager.preferredLanguage)
                },
                onDenied = {
                    appendSystemNotice("Microphone permission declined. You can continue using the manual text input bar below.")
                }
            )
        }

        // Stop Speaking Button
        binding.btnStopSpeaking.setOnClickListener {
            ttsManager.stopSpeaking()
        }

        // Mute / Unmute Toggle Button
        binding.btnMuteToggle.setOnClickListener {
            val newMute = !memoryManager.isVoiceMuted
            memoryManager.isVoiceMuted = newMute
            if (newMute) {
                ttsManager.stopSpeaking()
            }
            updateMuteButtonUI()
        }

        // Send Text Command Button
        binding.btnSendText.setOnClickListener {
            submitManualText()
        }

        binding.etCommandInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND || actionId == EditorInfo.IME_ACTION_DONE) {
                submitManualText()
                true
            } else {
                false
            }
        }

        // Top Navigation Buttons (Settings & About)
        binding.btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        binding.btnAbout.setOnClickListener {
            startActivity(Intent(this, AboutActivity::class.java))
        }
    }

    private fun initializeOfflineBrain() {
        lifecycleScope.launch {
            localAIEngine.initializeEngine { state, statusDetail ->
                binding.tvModelStatusBadge.text = state.label
                binding.tvTelemetryDetail.text = statusDetail
            }
        }
    }

    private fun applyUserPreferences() {
        binding.hudVisualizer.setAccentPalette(memoryManager.themeAccent)
        binding.tvCreatorFooter.visibility = if (memoryManager.showFooterCredit) View.VISIBLE else View.GONE
        updateMuteButtonUI()
        if (ttsManager.isInitialized) {
            ttsManager.applyLanguageAndRate(memoryManager.preferredLanguage, memoryManager.speechRate)
        }
    }

    private fun updateMuteButtonUI() {
        val muted = memoryManager.isVoiceMuted
        binding.btnMuteToggle.text = if (muted) getString(R.string.btn_unmute_voice) else getString(R.string.btn_mute_voice)
    }

    private fun submitManualText() {
        val text = binding.etCommandInput.text?.toString()?.trim().orEmpty()
        if (text.isEmpty()) return
        binding.etCommandInput.setText("")
        handleUserQuery(text)
    }

    private fun handleUserQuery(query: String) {
        val userMsg = memoryManager.saveMessage(ChatMessage.Sender.USER, query, "USER")
        chatAdapter.addMessage(userMsg)
        binding.rvConversation.smoothScrollToPosition(chatAdapter.itemCount - 1)

        // Step 1: Check deterministic safe Android phone commands first
        val cmdResult = commandExecutor.processCommand(query)
        if (cmdResult.handled) {
            val replyMsg = memoryManager.saveMessage(
                ChatMessage.Sender.JARVIS,
                cmdResult.spokenResponse,
                cmdResult.badge
            )
            chatAdapter.addMessage(replyMsg)
            binding.rvConversation.smoothScrollToPosition(chatAdapter.itemCount - 1)
            ttsManager.speak(cmdResult.spokenResponse)
            return
        }

        // Step 2: Otherwise dispatch to the local on-device AI engine
        binding.tvListeningBanner.text = getString(R.string.status_generating_local_reply)
        lifecycleScope.launch {
            val (aiReply, sourceBadge) = localAIEngine.generateResponse(query)
            val replyMsg = memoryManager.saveMessage(ChatMessage.Sender.JARVIS, aiReply, sourceBadge)
            chatAdapter.addMessage(replyMsg)
            binding.rvConversation.smoothScrollToPosition(chatAdapter.itemCount - 1)
            binding.tvListeningBanner.text = getString(R.string.status_tap_mic_or_type)
            ttsManager.speak(aiReply)
        }
    }

    private fun appendSystemNotice(notice: String) {
        val sysMsg = memoryManager.saveMessage(ChatMessage.Sender.SYSTEM, notice, "NOTICE")
        chatAdapter.addMessage(sysMsg)
        binding.rvConversation.smoothScrollToPosition(chatAdapter.itemCount - 1)
    }

    // --- SpeechRecognitionManager.Listener Callbacks ---

    override fun onListeningStateChanged(isListening: Boolean, engineLabel: String) {
        runOnUiThread {
            if (isListening) {
                binding.hudVisualizer.setHudMode(HudVoiceVisualizerView.HudMode.LISTENING)
                binding.tvListeningBanner.text = "LISTENING [$engineLabel]... SPEAK NOW"
                binding.btnMic.text = getString(R.string.btn_listening_active)
            } else {
                binding.hudVisualizer.setHudMode(HudVoiceVisualizerView.HudMode.IDLE)
                binding.tvListeningBanner.text = getString(R.string.status_tap_mic_or_type)
                binding.btnMic.text = getString(R.string.btn_tap_to_speak)
            }
        }
    }

    override fun onPartialTranscript(partialText: String) {
        runOnUiThread {
            binding.tvListeningBanner.text = "HEARING: \"$partialText\""
        }
    }

    override fun onFinalTranscript(finalText: String) {
        runOnUiThread {
            binding.tvListeningBanner.text = getString(R.string.status_tap_mic_or_type)
            handleUserQuery(finalText)
        }
    }

    override fun onSpeechError(userFriendlyError: String) {
        runOnUiThread {
            binding.tvListeningBanner.text = getString(R.string.status_tap_mic_or_type)
            appendSystemNotice(userFriendlyError)
        }
    }

    override fun onAudioLevelUpdate(normalizedLevel: Float) {
        runOnUiThread {
            binding.hudVisualizer.updateAudioLevel(normalizedLevel)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permissionManager.handlePermissionsResult(
            requestCode = requestCode,
            grantResults = grantResults,
            onGranted = {
                speechManager.startListening(memoryManager.preferredLanguage)
            },
            onPermanentlyDenied = {
                appendSystemNotice("Microphone access was denied. You can always use the text input field below.")
            }
        )
    }

    override fun onDestroy() {
        speechManager.destroy()
        ttsManager.shutdown()
        localAIEngine.release()
        super.onDestroy()
    }
}
