/*
 * Copyright (C) 2026 Marko Boško, Mantra Productions
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.patrickgold.florisboard.dictate

import android.content.Context
import dev.patrickgold.florisboard.app.FlorisPreferenceStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * The language of this app, in one place.
 *
 * There were two languages before, and they were allowed to disagree. One decided what the speech
 * service was told it was listening to, the other decided which dictionary the suggestions came
 * from, and each had its own control. Two controls for one intention is one too many: nobody speaks
 * Croatian while wanting English suggestions, and the setup where that is possible is the setup
 * where one of the two is quietly wrong and there is no way to see which.
 *
 * So there is one switch, and what it moves is the **dictation** language: what the speech service
 * is told to expect. Every control that changes it goes through this object rather than writing the
 * preference directly.
 *
 * It deliberately does NOT move the keyboard's own language. That was tried and was wrong: typing
 * and speaking are two acts at two moments, and a control meant for the microphone must not relay
 * the keys.
 *
 * Two languages, Croatian and English, because that is what this app is for. Auto-detect is not one
 * of them: it exists in the catalog for the settings screen, but the toggle deliberately cannot land
 * on it, since a toggle whose third state means "I do not know" cannot be operated without looking.
 */
object MaLanguage {
    const val HR = "hr"
    const val EN = "en"

    /** The two, in the order the toggle moves through them. */
    val PAIR = listOf(HR, EN)

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    /**
     * The active language, always one of [PAIR].
     *
     * Anything else, including auto-detect left over from an older install, reads as Croatian rather
     * than as itself, so a caller never has to handle a third case.
     */
    fun active(): String {
        val prefs by FlorisPreferenceStore
        val code = prefs.dictate.activeInputLanguage.get().substringBefore('-').lowercase()
        return if (code == EN) EN else HR
    }

    /**
     * The badge shown wherever the language is displayed: **HR** or **ENG**.
     *
     * Three letters for English and two for Croatian, which is deliberately uneven. `EN` and `HR`
     * are the same shape and the same weight, so at a glance in the corner of a moving keyboard they
     * read as "some code" rather than as a word, and telling them apart takes a moment of actual
     * reading. `ENG` does not look like `HR`. Marko asked for these two spellings by name and this is
     * why they are right.
     *
     * Display only. The stored codes stay `en` and `hr`, and nothing branches on this string.
     */
    fun badge(): String = if (active() == EN) "ENG" else "HR"

    /**
     * Sets the language **the microphone is told to expect**, and nothing else.
     *
     * It used to switch the keyboard subtype at the same time, on the argument that nobody speaks
     * Croatian while wanting English suggestions. Marko's objection settles it: these are two
     * different things that happen at two different moments. He dictates in one language and types
     * in another all day, and a dictation language that quietly relaid his keyboard meant a press
     * meant for the microphone changed the keys under his thumb.
     *
     * The keyboard's own language keeps its own two routes, which are the ones every Android
     * keyboard has: hold the spacebar, or change it in settings. Neither of them touches this.
     */
    @Suppress("UNUSED_PARAMETER")
    fun set(context: Context, code: String) {
        val target = if (code.substringBefore('-').lowercase() == EN) EN else HR
        val prefs by FlorisPreferenceStore
        scope.launch { prefs.dictate.activeInputLanguage.set(target) }
    }

    /** Moves to the other one. */
    fun toggle(context: Context) {
        set(context, if (active() == HR) EN else HR)
    }
}
