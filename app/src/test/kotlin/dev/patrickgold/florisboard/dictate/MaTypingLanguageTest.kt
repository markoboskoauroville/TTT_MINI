/*
 * Copyright (C) 2026 Marko Boško, Mantra Productions
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package dev.patrickgold.florisboard.dictate

import dev.patrickgold.florisboard.dictate.nlp.MaTypingLanguage
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class MaTypingLanguageTest : FunSpec({

    val none: (String) -> Boolean = { false }
    fun decide(text: String, en: Set<String> = emptySet(), hr: Set<String> = emptySet()) =
        MaTypingLanguage.decide(text, { it in en }, { it in hr })

    test("English small words: English") {
        decide("I think that the meeting is ") shouldBe "en"
    }
    test("Croatian without diacritics: Croatian") {
        decide("Dobro jutro, kako si danas ") shouldBe "hr"
    }
    test("one Croatian letter settles it") {
        decide("The word čovjek ") shouldBe "hr"
    }
    test("not English is Croatian: unknown words lean Croatian") {
        decide("Moramo pogledati raspored ") shouldBe "hr"
    }
    test("a typo in an English sentence stays English") {
        decide("Can you send the documnet to me ") shouldBe "en"
    }
    test("the word being typed is not read") {
        decide("Kako si dan") shouldBe "hr"
        decide("ka") shouldBe null
    }
    test("nothing to go on: no answer, so the last one stands") {
        decide("") shouldBe null
        decide("   ") shouldBe null
    }
    test("words shared by both count for neither") {
        decide("i a to on ") shouldBe null
    }
    test("his personal models break the tie") {
        decide("Mantreshvar presentation ", en = setOf("presentation")) shouldBe "en"
        decide("sastanak sutra ", hr = setOf("sastanak")) shouldBe "hr"
    }
    test("only the last eight words count: a switch of language is followed") {
        decide("This is the English part of the text and now evo je hrvatski dio ovdje sada ") shouldBe "hr"
    }
    test("the function with no models still works") {
        MaTypingLanguage.decide("the and is ", none, none) shouldBe "en"
    }
})
