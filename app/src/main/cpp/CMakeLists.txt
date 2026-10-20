# libmallm.so: llama.cpp and the JNI bridge for the style model on the phone (MaPhoneStyle, §191).
#
# llama.cpp is fetched, not committed: tools/fetch-llama-cpp.sh, pinned by tag and commit.
cmake_minimum_required(VERSION 3.22.1)
project(mallm C CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

set(LLAMA_DIR ${CMAKE_CURRENT_LIST_DIR}/llama.cpp)
if(NOT EXISTS ${LLAMA_DIR}/CMakeLists.txt)
    message(FATAL_ERROR "llama.cpp is missing. Run: tools/fetch-llama-cpp.sh")
endif()

# One static engine inside one .so: nothing else to load, nothing to find at run time.
set(BUILD_SHARED_LIBS OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_COMMON OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_TESTS OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_TOOLS OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_SERVER OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_APP OFF CACHE BOOL "" FORCE)
set(LLAMA_OPENSSL OFF CACHE BOOL "" FORCE)
# Threads without OpenMP: libomp would be one more native library to ship.
set(GGML_OPENMP OFF CACHE BOOL "" FORCE)
# Not this machine's CPU but the phone's, and only what every arm64 phone of the last six years has.
# dotprod makes the 4-bit model about twice as fast; i8mm is left out because a phone without it
# would crash on the first instruction instead of running slower.
set(GGML_NATIVE OFF CACHE BOOL "" FORCE)
set(GGML_CPU_ARM_ARCH "armv8.2-a+dotprod+fp16" CACHE STRING "" FORCE)
# KleidiAI downloads another source tree at configure time; not worth a second fetch.
set(GGML_CPU_KLEIDIAI OFF CACHE BOOL "" FORCE)

add_subdirectory(${LLAMA_DIR} llama-build EXCLUDE_FROM_ALL)

add_library(mallm SHARED ma_llm.cpp)
target_link_libraries(mallm PRIVATE llama ggml android log)
