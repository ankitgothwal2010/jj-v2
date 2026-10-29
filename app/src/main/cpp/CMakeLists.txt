# Copyright (c) 2026 Ankit Gothwal. All rights reserved.
cmake_minimum_required(VERSION 3.22.1)
project(jarvis_llama_jni LANGUAGES C CXX)

# Fetch the real llama.cpp engine at configure time. A network connection is
# required the first time this project is configured.
include(FetchContent)
set(LLAMA_BUILD_TESTS OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_SERVER OFF CACHE BOOL "" FORCE)
set(LLAMA_CURL OFF CACHE BOOL "" FORCE)
set(GGML_NATIVE OFF CACHE BOOL "" FORCE)
set(GGML_OPENMP OFF CACHE BOOL "" FORCE)
set(GGML_LLAMAFILE OFF CACHE BOOL "" FORCE)
set(LLAMA_OPENSSL OFF CACHE BOOL "" FORCE)

FetchContent_Declare(
    llama_cpp
    GIT_REPOSITORY https://github.com/ggml-org/llama.cpp.git
    GIT_TAG master
    GIT_SHALLOW TRUE
)
FetchContent_MakeAvailable(llama_cpp)

add_library(jarvis_llama_jni SHARED jarvis_llama_jni.cpp)
find_library(log-lib log)
target_compile_features(jarvis_llama_jni PRIVATE cxx_std_17)
target_compile_definitions(jarvis_llama_jni PRIVATE LLAMA_CPP_ENABLED=1)
target_link_libraries(jarvis_llama_jni PRIVATE llama ${log-lib})
