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

import android.content.Context
import dev.patrickgold.florisboard.dictate.provider.LocalModelFile
import dev.patrickgold.florisboard.dictate.provider.LocalModelSpec
import dev.patrickgold.florisboard.dictate.provider.LocalTranscriptionProvider
import java.io.File

/**
 * The language models that run on the phone: the same Qwen3 models MARKO_TRANSCRIBE runs on the Mac
 * (server/styled.py), in llama.cpp's file format instead of MLX's.
 *
 * Downloaded by the same machinery as Parakeet ([dev.patrickgold.florisboard.dictate.provider.LocalModelDownloads]):
 * a foreground service, size and SHA-256 checked, installed only when whole. Hugging Face is the
 * source, the publisher named on each entry; the files are Apache-2.0.
 *
 * A prompt chooses one by storing [choice] of it as its model ("phone:<id>"), beside the Claude
 * tiers. The name is always shown; nothing on the phone is called "default".
 */
object MaPhoneModels {

    const val PREFIX = "phone:"
    private const val FILE = "model.gguf"

    data class PhoneModel(
        val id: String,
        val name: String,
        /** One line for the list: size, where it comes from, what it costs in time. */
        val note: String,
        val url: String,
        val sizeBytes: Long,
        val sha256: String,
        /**
         * True for a model that thinks before it answers unless told not to (Qwen3 8B). Told not to
         * with the empty think block, which is what `enable_thinking=False` does on the Mac.
         */
        val thinks: Boolean,
        /** Context size: the rules, his text and the answer must fit. */
        val nCtx: Int = 3072,
    ) {
        val choice: String get() = PREFIX + id
        val spec: LocalModelSpec
            get() = LocalModelSpec(id, name, note, listOf(LocalModelFile(url, FILE, sizeBytes, sha256)))
    }

    /** The Mac's fast model (STYLE_MODEL=Qwen3-4B-Instruct-2507), the one that fits a phone. */
    val QWEN3_4B = PhoneModel(
        id = "qwen3-4b-instruct-2507-q4_0",
        name = "Qwen3 4B Instruct 2507",
        note = "2.4 GB · the Mac's fast model · fits this phone",
        url = "https://huggingface.co/unsloth/Qwen3-4B-Instruct-2507-GGUF/resolve/main/Qwen3-4B-Instruct-2507-Q4_0.gguf",
        sizeBytes = 2_375_773_280,
        sha256 = "e0ba675d86ab277c61701c6793659b2ae801d95e3be791464c321e6fbf613be2",
        thinks = false,
    )

    /** The model the Mac uses now (Qwen3 8B). Twice the size and about half the speed. */
    val QWEN3_8B = PhoneModel(
        id = "qwen3-8b-q4_k_m",
        name = "Qwen3 8B",
        note = "5.0 GB · the model the Mac uses now · needs a 12 GB phone, about twice as slow",
        url = "https://huggingface.co/Qwen/Qwen3-8B-GGUF/resolve/main/Qwen3-8B-Q4_K_M.gguf",
        sizeBytes = 5_027_783_488,
        sha256 = "d98cdcbd03e17ce47681435b5150e34c1417f50b5c0019dd560e4882c5745785",
        thinks = true,
    )

    val all: List<PhoneModel> = listOf(QWEN3_4B, QWEN3_8B)

    fun isPhone(model: String?): Boolean = model?.trim()?.startsWith(PREFIX) == true

    fun byChoice(model: String?): PhoneModel? {
        val id = model?.trim()?.removePrefix(PREFIX) ?: return null
        return all.firstOrNull { it.id == id }
    }

    fun file(context: Context, m: PhoneModel): File =
        File(LocalTranscriptionProvider.modelDir(context, m.id), FILE)

    fun isInstalled(context: Context, m: PhoneModel): Boolean = file(context, m).length() == m.sizeBytes

    /**
     * The whole prompt in Qwen's chat format (ChatML). Pure, so it can be checked without a phone.
     * The empty think block goes after the assistant tag for a model that [PhoneModel.thinks].
     */
    fun chatPrompt(m: PhoneModel, system: String, user: String): String = buildString {
        append("<|im_start|>system\n").append(system.trim()).append("<|im_end|>\n")
        append("<|im_start|>user\n").append(user.trim()).append("<|im_end|>\n")
        append("<|im_start|>assistant\n")
        if (m.thinks) append("<think>\n\n</think>\n\n")
    }

    private val THINK = Regex("<think>.*?</think>", RegexOption.DOT_MATCHES_ALL)

    /** The answer without a think block, the end tag or quotation marks around the whole of it. */
    fun clean(answer: String): String =
        answer.replace(THINK, "").replace("<|im_end|>", "").trim().trim('"', '“', '”').trim()

    /** Room for the answer: three tokens a word, as on the Mac, and never fewer than 64. */
    fun maxTokens(input: String): Int =
        maxOf(64, input.trim().split(Regex("\\s+")).count { it.isNotEmpty() } * 3)
}
