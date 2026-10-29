/*
 * Copyright (c) 2026 Ankit Gothwal. All rights reserved.
 * JARVIS AI native GGUF inference bridge, backed by llama.cpp (MIT).
 */
#include <jni.h>
#include <android/log.h>
#include <algorithm>
#include <string>
#include <vector>
#include <mutex>
#include "llama.h"

#define LOG_TAG "JarvisLlamaJNI"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

struct JarvisModel {
    llama_model * model = nullptr;
    llama_context * context = nullptr;
    const llama_vocab * vocab = nullptr;
    int threads = 2;
    std::mutex mutex;
};

extern "C" JNIEXPORT jlong JNICALL
Java_com_ankit_jarvis_LocalAIEngine_nativeInitModel(
        JNIEnv * env, jobject, jstring modelPath, jint numThreads) {
    if (!modelPath) return 0;
    const char * path = env->GetStringUTFChars(modelPath, nullptr);
    if (!path) return 0;

    llama_backend_init();
    auto * state = new JarvisModel();
    state->threads = std::max(1, std::min(4, (int) numThreads));

    llama_model_params mp = llama_model_default_params();
    mp.n_gpu_layers = 0; // portable CPU inference on Android
    state->model = llama_model_load_from_file(path, mp);
    env->ReleaseStringUTFChars(modelPath, path);
    if (!state->model) {
        LOGE("llama.cpp could not load the GGUF model");
        delete state;
        return 0;
    }

    llama_context_params cp = llama_context_default_params();
    cp.n_ctx = 1024;        // conservative context for phones
    cp.n_batch = 128;
    cp.n_threads = state->threads;
    cp.n_threads_batch = state->threads;
    state->context = llama_init_from_model(state->model, cp);
    if (!state->context) {
        LOGE("Could not create llama context");
        llama_model_free(state->model);
        delete state;
        return 0;
    }
    state->vocab = llama_model_get_vocab(state->model);
    LOGI("GGUF model loaded with llama.cpp");
    return reinterpret_cast<jlong>(state);
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_ankit_jarvis_LocalAIEngine_nativeCompletion(
        JNIEnv * env, jobject, jlong contextPtr, jstring prompt, jint maxTokens) {
    auto * state = reinterpret_cast<JarvisModel *>(contextPtr);
    if (!state || !prompt) return env->NewStringUTF("");
    const char * raw = env->GetStringUTFChars(prompt, nullptr);
    if (!raw) return env->NewStringUTF("");

    // Qwen2.5 Instruct uses ChatML. Other GGUF models may use a different template.
    std::string formatted =
        "<|im_start|>system\nYou are JARVIS, a helpful offline assistant created by Ankit Gothwal. "
        "Answer clearly and concisely. You can understand English and Hindi/Hinglish.\n<|im_end|>\n"
        "<|im_start|>user\n";
    formatted += raw;
    formatted += "\n<|im_end|>\n<|im_start|>assistant\n";
    env->ReleaseStringUTFChars(prompt, raw);

    std::lock_guard<std::mutex> guard(state->mutex);
    const int32_t needed = -llama_tokenize(state->vocab, formatted.c_str(),
        (int32_t) formatted.size(), nullptr, 0, true, true);
    if (needed <= 0 || needed > 8192) return env->NewStringUTF("");
    std::vector<llama_token> tokens((size_t) needed);
    const int32_t tokenCount = llama_tokenize(state->vocab, formatted.c_str(),
        (int32_t) formatted.size(), tokens.data(), (int32_t) tokens.size(), true, true);
    if (tokenCount < 0) return env->NewStringUTF("");
    tokens.resize((size_t) tokenCount);

    llama_memory_clear(llama_get_memory(state->context), true);
    llama_batch batch = llama_batch_get_one(tokens.data(), (int32_t) tokens.size());
    if (llama_decode(state->context, batch) != 0) {
        return env->NewStringUTF("");
    }
    llama_sampler * sampler = llama_sampler_chain_init(llama_sampler_chain_default_params());
    llama_sampler_chain_add(sampler, llama_sampler_init_temp(0.7f));
    llama_sampler_chain_add(sampler, llama_sampler_init_dist(1234));
    std::string output;
    const int limit = std::max(1, std::min(256, (int) maxTokens));
    char piece[4096];

    for (int i = 0; i < limit; ++i) {
        llama_token token = llama_sampler_sample(sampler, state->context, -1);
        if (llama_vocab_is_eog(state->vocab, token)) break;
        llama_sampler_accept(sampler, token);

        const int n = llama_token_to_piece(state->vocab, token, piece,
            (int32_t) sizeof(piece), 0, true);
        if (n > 0) output.append(piece, (size_t) n);

        llama_batch next = llama_batch_get_one(&token, 1);
        if (llama_decode(state->context, next) != 0) {
            break;
        }
    }
    llama_sampler_free(sampler);
    return env->NewStringUTF(output.c_str());
}

extern "C" JNIEXPORT void JNICALL
Java_com_ankit_jarvis_LocalAIEngine_nativeFreeModel(
        JNIEnv *, jobject, jlong contextPtr) {
    auto * state = reinterpret_cast<JarvisModel *>(contextPtr);
    if (!state) return;
    {
        std::lock_guard<std::mutex> guard(state->mutex);
        if (state->context) llama_free(state->context);
        if (state->model) llama_model_free(state->model);
    }
    delete state;
}
