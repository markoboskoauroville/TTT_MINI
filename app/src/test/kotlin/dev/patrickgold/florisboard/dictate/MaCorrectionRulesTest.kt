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

import dev.patrickgold.florisboard.dictate.nlp.MaCorrectionRules
import dev.patrickgold.florisboard.dictate.nlp.MaCorrectionRules.Rule
import dev.patrickgold.florisboard.dictate.nlp.MaCorrectionRules.Source
import dev.patrickgold.florisboard.dictate.nlp.MaCorrectionRules.Swap
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

class MaCorrectionRulesTest : FunSpec({

    context("diff finds his corrections") {
        test("one name fixed in the middle") {
            MaCorrectionRules.diff("I spoke with Amina today.", "I spoke with Emina today.") shouldContainExactly
                listOf(Swap("Amina", "Emina"))
        }
        test("the first word fixed counts: the field is read from where the dictation began") {
            MaCorrectionRules.diff("Fever is down again.", "Fiverr is down again.") shouldContainExactly
                listOf(Swap("Fever", "Fiverr"))
        }
        test("two words to one") {
            MaCorrectionRules.diff("Ask You Cloud to render it.", "Ask Claude to render it.") shouldContainExactly
                listOf(Swap("You Cloud", "Claude"))
        }
        test("text written after the dictation is not a correction") {
            MaCorrectionRules.diff("Send it to Emina.", "Send it to Emina. And then lunch at 13:00").shouldBeEmpty()
        }
        test("a rewrite teaches nothing") {
            MaCorrectionRules.diff(
                "This is the first version of the sentence.",
                "Completely different words now stand here instead, okay.",
            ).shouldBeEmpty()
        }
        test("a deleted word is not a swap") {
            MaCorrectionRules.diff("I really really want it.", "I really want it.").shouldBeEmpty()
        }
        test("a capital for a name mid-sentence counts, at the start of a sentence it does not") {
            MaCorrectionRules.diff("I saw emina there.", "I saw Emina there.") shouldContainExactly
                listOf(Swap("emina", "Emina"))
            MaCorrectionRules.diff("hello there. Next one.", "Hello there. Next one.").shouldBeEmpty()
        }
        test("punctuation changes are not word fixes") {
            MaCorrectionRules.diff("Yes, I will.", "Yes; I will.").shouldBeEmpty()
        }
        test("more than three words replaced is not a correction") {
            MaCorrectionRules.diff(
                "We met at the old place near the river yesterday evening.",
                "We met at a completely new bar in town yesterday evening.",
            ).shouldBeEmpty()
        }
    }

    context("record counts, and only the second time turns a fix on") {
        val swap = Swap("Amina", "Emina")
        test("once is counted, twice is in use") {
            val once = MaCorrectionRules.record(emptyList(), swap, "hr")
            once.single().count shouldBe 1
            once.single().isActive shouldBe false
            val twice = MaCorrectionRules.record(once, swap, "hr")
            twice.single().count shouldBe 2
            twice.single().isActive shouldBe true
        }
        test("a different fix of the same word starts again") {
            val r = MaCorrectionRules.record(listOf(Rule("Amina", "Emina", "hr", 3, true, Source.LEARNED)), Swap("Amina", "Amna"), "hr")
            r.single().right shouldBe "Amna"
            r.single().count shouldBe 1
        }
        test("languages are kept apart") {
            val r = MaCorrectionRules.record(MaCorrectionRules.record(emptyList(), swap, "hr"), swap, "en")
            r.size shouldBe 2
            r.all { it.count == 1 } shouldBe true
        }
        test("undoing an active rule switches it off") {
            val r = MaCorrectionRules.record(listOf(Rule("Amina", "Emina", "hr", 2, true, Source.LEARNED)), Swap("Emina", "Amina"), "hr")
            r.first { it.wrong == "Amina" }.enabled shouldBe false
        }
        test("his own rule is never overwritten by a learned one") {
            val mine = listOf(Rule("KIA", "Kie", "*", 0, true, Source.MINE))
            MaCorrectionRules.record(mine, Swap("KIA", "Kia"), "*") shouldBe mine
        }
    }

    context("apply") {
        val rules = MaCorrectionRules.withSeeds(emptyList()) + Rule("Kukljca", "Kukljica", "hr", 2, true, Source.LEARNED)
        test("whole words, any case, the right form written") {
            MaCorrectionRules.apply("amina and Amina, but not Aminata.", rules, "hr") shouldBe "Emina and Emina, but not Aminata."
        }
        test("a rule of one language does not touch the other") {
            MaCorrectionRules.apply("Kukljca je blizu.", rules, "hr") shouldBe "Kukljica je blizu."
            MaCorrectionRules.apply("Kukljca is near.", rules, "en") shouldBe "Kukljca is near."
        }
        test("phrases, and the capital at the start of a sentence kept") {
            MaCorrectionRules.apply("Leaving with them. We are leaving with them.", rules, "en") shouldBe
                "Living with them. We are living with them."
        }
        test("already right is left alone") {
            MaCorrectionRules.apply("Cut in DaVinci Resolve today.", rules, "en") shouldBe "Cut in DaVinci Resolve today."
            MaCorrectionRules.apply("Cut in Davinci today.", rules, "en") shouldBe "Cut in DaVinci Resolve today."
            MaCorrectionRules.apply("Mantra Productions and Mantra production.", rules, "en") shouldBe
                "Mantra Productions and Mantra Productions."
        }
        test("the longer rule first: Kia AI is not split by KIA") {
            MaCorrectionRules.apply("I used Kia AI and KIA.", rules, "en") shouldBe "I used Kie and Kie."
        }
        test("a rule seen once, or switched off, does nothing") {
            val r = listOf(Rule("Amina", "Emina", "hr", 1, true, Source.LEARNED), Rule("Fever", "Fiverr", "*", 0, false, Source.MINE))
            MaCorrectionRules.apply("Amina on Fever.", r, "hr") shouldBe "Amina on Fever."
        }
        test("the glossary carries the right forms of rules in use") {
            MaCorrectionRules.glossary(listOf(Rule("Amina", "Emina", "hr", 2, true, Source.LEARNED), Rule("x", "Y", "hr", 1, true, Source.LEARNED)), "hr") shouldBe listOf("Emina")
        }
    }

    test("storage round trip") {
        val rules = listOf(
            Rule("Amina", "Emina", "hr", 2, true, Source.LEARNED),
            Rule("You Cloud", "Claude", "*", 0, false, Source.MINE),
        )
        MaCorrectionRules.parse(MaCorrectionRules.serialize(rules)) shouldBe rules
        MaCorrectionRules.parse("garbage\n\t\t").shouldBeEmpty()
    }

    test("the seeds are added once") {
        val once = MaCorrectionRules.withSeeds(emptyList())
        MaCorrectionRules.withSeeds(once).size shouldBe once.size
        once.none { it.wrong.equals("Cloud", ignoreCase = true) } shouldBe true
    }
})
