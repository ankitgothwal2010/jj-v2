# Changelog — JARVIS AI (`com.ankit.jarvis`)

All notable changes to **JARVIS — Personal AI Assistant** by **Ankit Gothwal** are documented in this file.

## [1.0.0] — 2026-10-26
### Added
- **Official Branding & Identity:** Branded for **Ankit Gothwal** (`com.ankit.jarvis`) with dark black/navy interface, electric cyan and cobalt blue HUD panels, original vector application icon (`ic_jarvis_logo.xml`), and official `AboutActivity` with non-affiliation disclosure.
- **Animated Circular HUD Voice Visualizer:** Custom 60fps `HudVoiceVisualizerView` responding dynamically to IDLE, LISTENING (microphone RMS amplitude), and SPEAKING states, with 3 accessible theme palettes.
- **Offline-First Voice Input (`SpeechRecognitionManager.kt`):** Dual support for 100% offline Vosk speech models (`vosk-model-small-en-in` and `vosk-model-small-hi`) and Android offline `SpeechRecognizer` fallback, plus manual text input bar.
- **Spoken Assistant Replies (`TextToSpeechManager.kt`):** Android `TextToSpeech` integration with offline voice data verification, speech rate slider (`0.5x`–`2.0x`), English/Hindi switch, mute toggle, and immediate stop-speaking button.
- **Safe Android Phone Control (`CommandExecutor.kt`):** Official Android Intent execution for opening installed apps, launching Camera, opening System/Wi-Fi/Bluetooth/Display/Sound Settings, adjusting media volume, controlling media playback (Play/Pause/Next/Previous), creating/reading/deleting local notes, checking battery telemetry, reading time/date, and launching websites on explicit request.
- **Offline AI Brain (`LocalAIEngine.kt` & `jarvis_llama_jni.cpp`):** Hardware-aware RAM guard (protecting Infinix Hot 10 6GB RAM and ASUS Zenfone Max M1) with GGUF native JNI bridge support (`llama.cpp`) and a comprehensive deterministic bilingual (English + Hindi/Hinglish) rule-based fallback engine.
- **On-Device Memory & Privacy (`LocalMemoryManager.kt`):** Zero-dependency SQLite storage for conversations and notes with one-tap complete history wipe and zero network permissions.
