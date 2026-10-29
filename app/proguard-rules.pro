# Copyright (c) 2026 Ankit Gothwal. All rights reserved, subject to applicable third-party license terms.
# Project: JARVIS AI | Developer: Ankit Gothwal
# Safe ProGuard / R8 configuration for release builds

-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# Keep Vosk & JNA classes required for offline speech recognition JNI reflection
-keep class com.sun.jna.** { *; }
-keep class org.vosk.** { *; }
-dontwarn com.sun.jna.**

# Keep LocalAIEngine JNI bridge methods from being stripped or renamed by R8
-keepclasseswithmembernames class com.ankit.jarvis.LocalAIEngine {
    native <methods>;
}
