# JARVIS — Personal AI Assistant (`com.ankit.jarvis`)

**Created and developed by Ankit Gothwal**  
Copyright ©️ 2026 Ankit Gothwal. All rights reserved, subject to applicable third-party license terms.  
**Version:** 1.0.0 | **Platform:** Android (Kotlin, MinSDK 26 / Android 8.0+) | **Software Budget:** ₹0 INR

---

## 1. Project Overview

**JARVIS — Personal AI Assistant** is a complete, private, offline-first Android voice and text assistant created and owned by **Ankit Gothwal**. It features a custom dark black and navy futuristic HUD with electric cyan/blue circular voice animations, on-device speech recognition (Vosk + Android offline ASR), spoken replies (Android TTS with offline verification), safe intent-based phone control, local SQLite memory, and an offline AI brain with both a deterministic bilingual (English + Hindi/Hinglish) rule engine and a native C++ JNI bridge for quantized `.gguf` models (`llama.cpp`).

> **Non-Affiliation Notice:** This is an independent personal AI assistant project created by Ankit Gothwal. It is not affiliated with, endorsed by, or connected to OpenAI, Marvel, Iron Man, or any other company or franchise.

---

## 2. Hardware Compatibility & Resource Analysis

| Target Environment | Hardware Specs | Optimization Applied in Project |
| :--- | :--- | :--- |
| **Development PC** | Windows 10, 6 GB RAM, 1 TB HDD/SSD, older CPU | `gradle.properties` limits JVM heap to `-Xmx1536m`, disables Gradle Daemon (`org.gradle.daemon=false`), disables parallel workers (`workers.max=2`), and uses real-device USB debugging (no Android Emulator needed). |
| **Primary Phone** | **Infinix Hot 10** (MediaTek Helio G70, `arm64-v8a`, 6 GB RAM, Android 10 / API 29) | Full support for Vosk offline speech models (~40 MB RAM) + quantized GGUF LLM (`Qwen2.5-0.5B-Instruct-Q4_K_M.gguf`, ~398 MB disk / ~650 MB RAM) or `TinyLlama-1.1B-Chat-Q4_K_M.gguf`. |
| **Secondary Test Phone** | **ASUS Zenfone Max M1** (Snapdragon 430, `arm64-v8a`/`armeabi-v7a`, 2–3 GB RAM, Android 8.0 / API 26) | `minSdk = 26` ensures full compatibility. `LocalAIEngine.kt` automatically checks free RAM (`getAvailableRamMb()`) and activates the Low-RAM Safeguard (< 650 MB free) so the phone runs smoothly on the deterministic Rule Engine without OutOfMemory crashes. |

---

## 3. Phased Development & Build Roadmap

To build and verify reliably on a 6 GB RAM Windows 10 PC, follow these three testable phases:

- **Phase 1 — Core HUD App, TTS & Safe Voice Command Engine (Zero External Downloads Needed):**
  - Compile and install the APK immediately. The built-in Rule Engine, safe Android Intent executor (`CommandExecutor.kt`), local SQLite notes/history (`LocalMemoryManager.kt`), and Android TTS (`TextToSpeechManager.kt`) work out-of-the-box.
- **Phase 2 — 100% Air-Gapped Offline Speech Recognition (Vosk Models):**
  - Download the free Apache-2.0 Vosk model (`vosk-model-small-en-in-0.4` or `vosk-model-small-hi-0.22`) once and extract it into `/sdcard/Android/data/com.ankit.jarvis/files/vosk-models/`.
- **Phase 3 — Local Quantized GGUF LLM Integration (`llama.cpp`):**
  - Place `qwen2.5-0.5b-instruct-q4_k_m.gguf` into `/sdcard/Android/data/com.ankit.jarvis/files/models/` and optionally enable the NDK `llama.cpp` CMake bridge in `app/build.gradle.kts` when you want open-ended generative chat on your Infinix Hot 10.

---

## 4. Windows 10 (6 GB RAM) Lightweight Setup & Command-Line Build Guide

Because Android Studio + Gradle Daemon + Emulator will exhaust 6 GB of RAM on Windows 10, **never run the Android Emulator**. You can either use Android Studio in lightweight mode (with Gradle Daemon disabled) or compile directly from **Windows Command Prompt / PowerShell** using the free **Android Command-Line Tools**:

### Step 1: Install Free Prerequisites (₹0 Budget)
1. **JDK 17 (Eclipse Temurin 17, Free):** Download and install OpenJDK 17 for Windows x64, and set `JAVA_HOME`.
2. **Android SDK Command-Line Tools (Free):** Download `commandlinetools-win-*_latest.zip` from developer.android.com/studio#command-tools and extract to `C:\Android\cmdline-tools\latest\`.
3. Install the required SDK packages via PowerShell/CMD:
   ```bat
   C:\Android\cmdline-tools\latest\bin\sdkmanager.bat "platform-tools" "platforms;android-34" "build-tools;34.0.0"
   ```
4. Set environment variable `ANDROID_HOME=C:\Android` and add `C:\Android\platform-tools` to your Windows `PATH`.

### Step 2: Enable USB Debugging on Infinix Hot 10 & ASUS Zenfone Max M1
1. On the phone, open **Settings → About Phone** and tap **Build Number** 7 times until *"You are now a developer!"* appears.
2. Go to **Settings → System → Developer Options** and enable **USB Debugging**.
3. Connect the phone to your Windows 10 PC via USB cable, tap **Allow USB Debugging** on the phone screen, and verify in CMD:
   ```bat
   adb devices
   ```

### Step 3: Build & Install Debug APK
Inside the extracted `JARVIS-AI-Ankit-Gothwal` project folder, create a file named `local.properties` containing:
```properties
sdk.dir=C\:\\Android
```
Then build and install directly to your connected phone without keeping a background daemon in RAM:
```bat
gradlew.bat assembleDebug --no-daemon --max-workers=2
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

---

## 5. Offline Model Setup (Speech & AI)

### A. Offline Vosk Speech Models (English & Hindi/Hinglish)
1. Download from https://alphacephei.com/vosk/models (Apache 2.0 License):
   - English (India): `vosk-model-small-en-in-0.4.zip` (~36 MB)
   - Hindi: `vosk-model-small-hi-0.22.zip` (~42 MB)
2. Extract and rename the folders to `vosk-model-small-en-in` and `vosk-model-small-hi`.
3. Push them to your phone via USB (`adb`):
   ```bat
   adb shell mkdir -p /sdcard/Android/data/com.ankit.jarvis/files/vosk-models/
   adb push vosk-model-small-en-in /sdcard/Android/data/com.ankit.jarvis/files/vosk-models/
   adb push vosk-model-small-hi /sdcard/Android/data/com.ankit.jarvis/files/vosk-models/
   ```

### B. Offline Quantized GGUF LLM Model
1. Download `qwen2.5-0.5b-instruct-q4_k_m.gguf` (~398 MB, Apache 2.0 License) from Hugging Face (`Qwen/Qwen2.5-0.5B-Instruct-GGUF`).
2. Push to the phone via USB:
   ```bat
   adb shell mkdir -p /sdcard/Android/data/com.ankit.jarvis/files/models/
   adb push qwen2.5-0.5b-instruct-q4_k_m.gguf /sdcard/Android/data/com.ankit.jarvis/files/models/
   ```

---

## 6. Release APK Signing Instructions

1. Generate your private signing keystore once using JDK 17 `keytool`:
   ```bat
   keytool -genkey -v -keystore jarvis-ankit-release.jks -keyalg RSA -keysize 2048 -validity 10000 -alias ankit_jarvis
   ```
   *(Keep `jarvis-ankit-release.jks` backed up offline; never commit it to Git!)*
2. Build the unsigned release APK:
   ```bat
   gradlew.bat assembleRelease --no-daemon --max-workers=2
   ```
3. Align and sign the APK using `zipalign` and `apksigner` from `C:\Android\build-tools\34.0.0\`:
   ```bat
   C:\Android\build-tools\34.0.0\zipalign.exe -v -p 4 app\build\outputs\apk\release\app-release-unsigned.apk app-release-aligned.apk
   C:\Android\build-tools\34.0.0\apksigner.bat sign --ks jarvis-ankit-release.jks --out JARVIS-AI-v1.0.0-AnkitGothwal.apk app-release-aligned.apk
   C:\Android\build-tools\34.0.0\apksigner.bat verify --verbose JARVIS-AI-v1.0.0-AnkitGothwal.apk
   ```
