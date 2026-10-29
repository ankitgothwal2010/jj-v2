# Privacy Policy — JARVIS AI (`com.ankit.jarvis`)

**Effective Date:** October 26, 2026  
**Application Name:** JARVIS — Personal AI Assistant  
**Developer:** Ankit Gothwal

---

## 1. Overview and Offline-First Design

**JARVIS — Personal AI Assistant** is designed from the ground up as an offline-first personal assistant. By default, the application does **not** request the `android.permission.INTERNET` permission in its `AndroidManifest.xml`, does **not** include advertising SDKs or analytics trackers, and does **not** transmit your conversations, voice recordings, or notes to any external server.

## 2. What Data Is Stored and Where

1. **Conversation History & Local Notes:**
   - Stored exclusively on your Android device inside the application's private internal SQLite database (`/data/data/com.ankit.jarvis/databases/jarvis_local_memory.db`).
   - Accessible only to the JARVIS application on your phone (unless your device is rooted).
2. **User Preferences:**
   - Language choice (`en-IN` or `hi-IN`), speech rate, mute state, and HUD theme preference are stored locally in Android `SharedPreferences` (`jarvis_preferences.xml`).
3. **Local AI & Speech Models:**
   - Offline Vosk acoustic models and GGUF language models are stored in the app-specific external storage directory (`/sdcard/Android/data/com.ankit.jarvis/files/`).

## 3. Microphone and Voice Processing

- The application requests `android.permission.RECORD_AUDIO` solely to capture your spoken commands when you actively tap the **MIC: SPEAK** button on the main HUD screen.
- Audio is processed in real time in memory by the local Vosk engine (or Android's system `SpeechRecognizer` in offline mode) and is immediately discarded after transcription.
- JARVIS **never** records audio in the background or when the microphone button is inactive.

## 4. Optional Network-Dependent Actions

- If you explicitly command JARVIS to open an external website (for example, `"Open website example.com"`), JARVIS launches your phone's default web browser via a standard Android `ACTION_VIEW` intent. Once your browser opens, your browsing activity is governed by your browser's privacy policy and network connection.

## 5. Deleting Your Data

- You can permanently delete all stored conversation messages and local notes at any time by opening **CFG (Settings) → Delete All Local Conversations & Notes**, or by saying `"Clear my notes"`, or by clearing storage / uninstalling the application in Android Settings.

## 6. No Legal Compliance Warranty

This privacy policy is a factual technical disclosure of how the JARVIS application handles data on your device. It does not constitute formal legal advice or a certification of regulatory compliance under specific regional privacy statutes.
