import dev.patrickgold.florisboard.dictate.nlp.*
// Types [script]: letters append, '<' is backspace, '#' deletes the whole previous word (as a word delete).
fun run(start: String, script: String): List<String> {
    val w = MaCorrectionWatcher(); var t = start; val out = ArrayList<String>()
    w.observe(t)
    for (c in script) {
        t = when (c) { '<' -> t.dropLast(1); '#' -> t.dropLastWhile { MaCorrectionWatcher.isWordChar(it) }; else -> t + c }
        w.observe(t)?.let { out += "${it.typo}->${it.fixed}" }
    }
    return out
}
fun check(name: String, got: Any?, want: Any?) { println((if (got == want) "ok   " else "FAIL ") + name + "  got=" + got) }
fun correctionsMain() {
    check("teh fixed by backspacing all", run("I saw ", "teh <<<<the "), listOf("teh->the"))
    check("teh fixed by backspacing two", run("I saw ", "teh <<<he "), listOf("teh->the"))
    check("word delete then retype", run("Hello ", "montza <#montaža "), listOf("montza->montaža"))
    check("continuing a word is not a typo", run("", "the <re "), emptyList<String>())
    check("a different word is not a typo", run("my ", "cat <<<<dog "), emptyList<String>())
    check("Croatian: kuca -> kuća", run("Idem ", "kuca <<<ća "), listOf("kuca->kuća"))
    check("normal typing learns nothing", run("", "the quick brown fox. "), emptyList<String>())
    check("moving away cancels", run("one two ", "tow <<<<<<<<<<<<<<abc "), emptyList<String>())
    check("fixed with comma", run("say ", "helo,<<<llo,"), listOf("helo->hello"))
    check("damerau swap", MaCorrectionWatcher.damerau("teh", "the"), 1)
    check("isCorrection short", MaCorrectionWatcher.isCorrection("ot", "to"), true)
    val st = MaCorrectionStore(); st.record("teh", "the"); st.record("teh", "the"); st.record("teh", "ten")
    check("store fix", st.fixFor("Teh"), "the")
    val st2 = MaCorrectionStore(); st2.parse(st.serialize()); check("store round trip", st2.fixFor("teh"), "the")
    check("case", MaCorrectionStore.matchCase("Teh", "the"), "The")
    val r = MaRecency(); r.touch("Auroville", 100)
    check("recency today", r.weight("auroville", 100), 1.0)
    check("recency 30 days", r.weight("auroville", 130), 0.5)
    check("recency floor", r.weight("auroville", 1000), 0.15)
    check("unknown floor", r.weight("x", 5), 0.15)
}
