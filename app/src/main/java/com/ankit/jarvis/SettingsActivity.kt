/*
 * Copyright ©️ 2026 Ankit Gothwal.
 * All rights reserved, subject to applicable third-party license terms.
 *
 * Project: JARVIS AI
 * Developer: Ankit Gothwal
 * Application ID: com.ankit.jarvis
 */
package com.ankit.jarvis

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.ankit.jarvis.databinding.ActivitySettingsBinding
import java.util.Locale

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var memoryManager: LocalMemoryManager
    private lateinit var permissionManager: PermissionManager
    private lateinit var localAIEngine: LocalAIEngine
    private lateinit var speechManager: SpeechRecognitionManager
    private lateinit var ttsManager: TextToSpeechManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        memoryManager = LocalMemoryManager(this)
        permissionManager = PermissionManager(this)
        localAIEngine = LocalAIEngine(this, memoryManager)
        speechManager = SpeechRecognitionManager(this, kotlinx.coroutines.MainScope(), object : SpeechRecognitionManager.Listener {
            override fun onListeningStateChanged(isListening: Boolean, engineLabel: String) {}
            override fun onPartialTranscript(partialText: String) {}
            override fun onFinalTranscript(finalText: String) {}
            override fun onSpeechError(userFriendlyError: String) {}
            override fun onAudioLevelUpdate(normalizedLevel: Float) {}
        })
        ttsManager = TextToSpeechManager(this, memoryManager) { _, _ -> }

        populateCurrentSettings()
        setupListeners()
    }

    override fun onResume() {
        super.onResume()
        refreshDiagnosticsText()
    }

    private fun populateCurrentSettings() {
        when (memoryManager.preferredLanguage) {
            "hi-IN" -> binding.rbLangHindi.isChecked = true
            else -> binding.rbLangEnglish.isChecked = true
        }

        val progress = ((memoryManager.speechRate - 0.5f) * 100f).toInt().coerceIn(0, 150)
        binding.seekSpeechRate.max = 150
        binding.seekSpeechRate.progress = progress
        binding.tvSpeechRateValue.text = String.format(Locale.US, "Speech Rate: %.2fx", memoryManager.speechRate)

        when (memoryManager.themeAccent) {
            "BLUE" -> binding.rbThemeBlue.isChecked = true
            "HIGH_CONTRAST" -> binding.rbThemeHighContrast.isChecked = true
            else -> binding.rbThemeCyan.isChecked = true
        }

        binding.switchFooterCredit.isChecked = memoryManager.showFooterCredit
        refreshDiagnosticsText()
    }

    private fun refreshDiagnosticsText() {
        val voskStatus = speechManager.getDiagnosticStatus(memoryManager.preferredLanguage)
        val ggufDir = localAIEngine.getModelsDirectory().absolutePath
        val freeRam = localAIEngine.getAvailableRamMb()
        val totalRam = localAIEngine.getTotalRamMb()
        val a11yActive = JarvisAccessibilityService.isServiceActive()
        val writeSys = PermissionManager.canWriteSystemSettings(this)
        val dndOk = PermissionManager.hasDndPermission(this)

        binding.tvModelDirectoriesInfo.text = """
            Device Memory: $freeRam MB Free / $totalRam MB Total RAM
            Accessibility Control: ${if (a11yActive) "ACTIVE" else "NOT ENABLED"}
            Brightness/Settings Control: ${if (writeSys) "ACTIVE" else "NOT ENABLED"}
            Silent/DND Mode Control: ${if (dndOk) "ACTIVE" else "NOT ENABLED"}
            
            Vosk Speech Diagnostic:
            $voskStatus
            
            Offline GGUF LLM Folder:
            $ggufDir
        """.trimIndent()
    }

    private fun setupListeners() {
        binding.btnBackFromSettings.setOnClickListener {
            finish()
        }

        binding.rgLanguage.setOnCheckedChangeListener { _, checkedId ->
            val lang = if (checkedId == R.id.rbLangHindi) "hi-IN" else "en-IN"
            memoryManager.preferredLanguage = lang
            val status = ttsManager.applyLanguageAndRate(lang, memoryManager.speechRate)
            refreshDiagnosticsText()
            Toast.makeText(this, status, Toast.LENGTH_LONG).show()
        }

        binding.seekSpeechRate.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val rate = 0.5f + (progress / 100f)
                memoryManager.speechRate = rate
                binding.tvSpeechRateValue.text = String.format(Locale.US, "Speech Rate: %.2fx", rate)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                ttsManager.applyLanguageAndRate(memoryManager.preferredLanguage, memoryManager.speechRate)
            }
        })

        binding.btnInstallOfflineTts.setOnClickListener {
            ttsManager.promptInstallVoiceData(this)
        }

        binding.btnGrantPhoneControlPerms.setOnClickListener {
            permissionManager.requestAllPhoneControlPermissions()
        }

        binding.btnEnableScreenLockControl.setOnClickListener {
            val adminComponent = ComponentName(this, JarvisDeviceAdminReceiver::class.java)
            val dpm = getSystemService(DEVICE_POLICY_SERVICE) as DevicePolicyManager
            if (!dpm.isAdminActive(adminComponent)) {
                val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                    putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
                    putExtra(
                        DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                        "Allow JARVIS AI to lock your phone screen when you command 'Lock my phone'."
                    )
                }
                startActivity(intent)
            } else {
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
        }

        binding.btnEnableBrightnessAndDnd.setOnClickListener {
            if (!PermissionManager.canWriteSystemSettings(this)) {
                permissionManager.openWriteSettingsPermissionPage()
            } else if (!PermissionManager.hasDndPermission(this)) {
                permissionManager.openDndPolicyPermissionPage()
            } else {
                Toast.makeText(this, "Brightness & Silent/DND permissions are already active!", Toast.LENGTH_SHORT).show()
            }
        }

        binding.rgThemeAccent.setOnCheckedChangeListener { _, checkedId ->
            val accent = when (checkedId) {
                R.id.rbThemeBlue -> "BLUE"
                R.id.rbThemeHighContrast -> "HIGH_CONTRAST"
                else -> "CYAN"
            }
            memoryManager.themeAccent = accent
        }

        binding.switchFooterCredit.setOnCheckedChangeListener { _, isChecked ->
            memoryManager.showFooterCredit = isChecked
        }

        binding.btnClearHistory.setOnClickListener {
            AlertDialog.Builder(this, R.style.Theme_JarvisAI_Dialog)
                .setTitle("Delete All Local History & Notes?")
                .setMessage("This will permanently wipe all saved conversations and local notes from your device's internal SQLite database.")
                .setPositiveButton("Delete Everything") { _, _ ->
                    memoryManager.wipeAllMemory()
                    Toast.makeText(this, "All local conversations and notes deleted.", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        }
    }

    override fun onDestroy() {
        speechManager.destroy()
        ttsManager.shutdown()
        super.onDestroy()
    }
}
