/*
 * Copyright ©️ 2026 Ankit Gothwal.
 * All rights reserved, subject to applicable third-party license terms.
 *
 * Project: JARVIS AI
 * Developer: Ankit Gothwal
 * Application ID: com.ankit.jarvis
 */

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.ankit.jarvis"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.ankit.jarvis"
        // minSdk 26 (Android 8.0 Oreo) supports both ASUS Zenfone Max M1 (Android 8.0)
        // and Infinix Hot 10 (Android 10, 6 GB RAM).
        minSdk = 26
        targetSdk = 34
        ndkVersion = "26.3.11579264"
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ndk {
            // Supports both 64-bit (Infinix Hot 10 Helio G70) and 32-bit/64-bit (ASUS Zenfone Max M1 Snapdragon 430)
            abiFilters += listOf("arm64-v8a", "armeabi-v7a")
        }

        externalNativeBuild {
            cmake {
                cppFlags += "-std=c++17 -O3"
            }
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }
}

dependencies {
    // AndroidX & Material Design (Apache License 2.0)
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")

    // Kotlin Coroutines for non-blocking offline AI & speech processing (Apache License 2.0)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // Vosk Offline Speech Recognition API & JNA (Apache License 2.0)
    implementation("com.alphacephei:vosk-android:0.3.47")
    implementation("net.java.dev.jna:jna:5.13.0@aar")
}
