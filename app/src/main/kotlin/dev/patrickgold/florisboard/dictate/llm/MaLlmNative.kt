/*
 * Copyright (C) 2026 Marko Bosko, Mantra Productions
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package dev.patrickgold.florisboard.dictate.llm

/**
 * The JNI side of the model on the phone (app/src/main/cpp/ma_llm.cpp, llama.cpp underneath).
 *
 * Called only by [MaLlmService], in the ":llm" process. Loading the library in the keyboard's own
 * process would put a 2.4 GB model next to the keyboard, and a native crash would take the keyboard
 * with it.
 */
object MaLlmNative {
    init {
        System.loadLibrary("mallm")
    }

    /** Loads [path] unless it is loaded already. Null, or what went wrong. */
    external fun load(path: String, nCtx: Int, nThreads: Int): String?

    external fun unload()

    /** Stops [generate] at the next token, from another thread. */
    external fun cancel()

    /** Prompt tokens, of them reused from the last prompt, generated, prefill ms, generation ms. */
    external fun lastStats(): LongArray

    /** Continues [prompt] (UTF-8, chat format) greedily. Null with the reason in `err[0]`. */
    external fun generate(prompt: ByteArray, maxTokens: Int, err: Array<String?>): ByteArray?
}
