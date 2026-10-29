# GGUF inference integration notes

This source package now connects the JNI bridge to the real `llama.cpp` library:
- CMake fetches `ggml-org/llama.cpp` at configure time (first build requires internet).
- Gradle configures the Android native build and the GitHub Actions workflow installs NDK/CMake.
- The JNI code loads the GGUF file, tokenizes a Qwen ChatML prompt, runs CPU inference, and returns generated text.
- The deterministic rule engine remains as a fallback if the model cannot load or available RAM is too low.

## Build and test
Push this source to GitHub and open **Actions → Build JARVIS AI APK**. The build may take longer because llama.cpp is compiled for Android. Download the APK from the successful workflow run's **Artifacts** section.

This is an integration attempt, not a verified APK: this environment did not run Android Gradle/NDK compilation or test inference on a phone. If the workflow fails, share the complete first compiler error from Actions so the native API/build can be adjusted. Because CMake fetches the current `master` branch, upstream API changes can affect future builds; pin `GIT_TAG` to a tested llama.cpp release/commit after the first successful build.

The JNI prompt formatting is optimized for Qwen2.5 Instruct ChatML. Other model families may need their own chat template. A GGUF model file must still be copied into the app's private `files/models` folder, and the model's license should be checked before distribution.
