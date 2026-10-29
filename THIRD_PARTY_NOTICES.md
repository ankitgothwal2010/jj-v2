# Third-Party Notices and License Attribution

**JARVIS — Personal AI Assistant** (`com.ankit.jarvis`) is created and developed by **Ankit Gothwal**.  
While original application source code and original visual assets are Copyright ©️ 2026 Ankit Gothwal, this application uses and supports the following third-party open-source libraries, engines, and models. Ankit Gothwal claims no ownership over any third-party component listed below.

---

## 1. AndroidX & Material Components for Android
- **Copyright:** Copyright ©️ The Android Open Source Project / Google LLC
- **License:** Apache License, Version 2.0
- **Usage:** Core Android UI compatibility, RecyclerView, ConstraintLayout, Lifecycle, and Material Design components.
- **License URL:** https://www.apache.org/licenses/LICENSE-2.0

## 2. Kotlin Standard Library & Kotlinx Coroutines
- **Copyright:** Copyright ©️ 2010–2026 JetBrains s.r.o. and Kotlin Programming Language contributors
- **License:** Apache License, Version 2.0
- **Usage:** Primary language runtime and asynchronous background coroutine execution (`kotlinx-coroutines-android`).
- **License URL:** https://www.apache.org/licenses/LICENSE-2.0

## 3. Vosk Offline Speech Recognition API (`com.alphacephei:vosk-android`) & Kaldi
- **Copyright:** Copyright ©️ Alpha Cephei Inc. and Kaldi Speech Recognition Toolkit Contributors
- **License:** Apache License, Version 2.0
- **Usage:** On-device offline speech-to-text recognition engine (`org.vosk`).
- **Recommended Local Models:**
  - `vosk-model-small-en-in-0.4` (~36 MB, Indian English) — Apache License 2.0
  - `vosk-model-small-hi-0.22` (~42 MB, Hindi) — Apache License 2.0
- **Source & Models:** https://alphacephei.com/vosk/models

## 4. Java Native Access (`net.java.dev.jna:jna`)
- **Copyright:** Copyright ©️ Timothy Wall and JNA Contributors
- **License:** Apache License, Version 2.0 (or LGPL 2.1+ dual license; used under Apache License 2.0)
- **Usage:** Native bridge dependency required by `vosk-android`.
- **License URL:** https://www.apache.org/licenses/LICENSE-2.0

## 5. llama.cpp On-Device Inference Engine (Optional Native C++ Submodule)
- **Copyright:** Copyright ©️ 2023–2026 Georgi Gerganov and `llama.cpp` contributors
- **License:** MIT License
- **Usage:** Local C/C++ GGUF quantized large language model inference engine integrated via `jarvis_llama_jni.cpp`.
- **Repository:** https://github.com/ggerganov/llama.cpp

## 6. Recommended Local Quantized GGUF Language Models (Downloaded Once by User)
To keep the APK compact and respect model redistribution terms, GGUF model weights are downloaded once to `/sdcard/Android/data/com.ankit.jarvis/files/models/`:
- **Primary Recommended Model (Infinix Hot 10, 6 GB RAM & ASUS Zenfone Max M1):**
  - **Model:** `Qwen2.5-0.5B-Instruct-GGUF` (`qwen2.5-0.5b-instruct-q4_k_m.gguf`, ~398 MB)
  - **Creator:** Qwen Team, Alibaba Cloud
  - **License:** Apache License, Version 2.0
- **Alternative Model (Infinix Hot 10, 6 GB RAM):**
  - **Model:** `TinyLlama-1.1B-Chat-v1.0-GGUF` (`tinyllama-1.1b-chat-v1.0.Q4_K_M.gguf`, ~669 MB)
  - **Creator:** StatNLP Research Group, Singapore University of Technology and Design (SUTD)
  - **License:** Apache License, Version 2.0

---

## Apache License 2.0 Summary Notice
Licensed under the Apache License, Version 2.0 (the "License"); you may not use these third-party files except in compliance with the License. You may obtain a copy of the License at:
http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.

## MIT License Summary Notice (for `llama.cpp`)
Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, subject to the inclusion of the above copyright notice and this permission notice in all copies or substantial portions of the Software.
