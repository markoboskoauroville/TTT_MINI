import dev.patrickgold.florisboard.dictate.nlp.*
fun modelMain() {
    val m = MaNgramModel()
    repeat(5) { m.learn("montaža je gotova.") }
    m.learn("monitor radi.")
    m.learn("teh plan.")
    println("before unlearn knows teh: " + m.knows("teh"))
    m.unlearn("teh")
    println("after unlearn knows teh: " + m.knows("teh") + "  (want false)")
    // recency: montaža counted 5 but old, monitor 1 but today
    val r = MaRecency(); r.touch("montaža", 0); r.touch("monitor", 200)
    val p = m.predict(null, null, "mon", weight = { r.weight(it, 200) })
    println("ranking by recency: " + p.map { it.word } + "  (want monitor first)")
    val p2 = m.predict(null, null, "mon")
    println("ranking by count: " + p2.map { it.word } + "  (want montaža first)")
    val st = MaCorrectionStore(); st.record("monitr", "monitor"); m.learn("monitr")
    println("skip typo: " + m.predict(null, null, "moni", skip = { st.isTypo(it) }).map { it.word } + "  (want [monitor])")
}
