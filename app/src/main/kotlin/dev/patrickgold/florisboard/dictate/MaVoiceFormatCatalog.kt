/*
 * Copyright (C) 2026 Marko Bosko, Mantra Productions
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package dev.patrickgold.florisboard.dictate

/**
 * The list of spoken formatting commands, as the settings screen shows them.
 *
 * One row per mark: what it does, what to say, and a recording of the words in British English so
 * the pronunciation can be heard rather than guessed at.
 *
 * ### Why the audio exists
 *
 * Baba's English carries an accent the transcriber sometimes mishears, and a command that is
 * misheard is worse than one that does not exist — it types a stray word into the middle of his
 * sentence. Reading "say: ampersand" does not tell him which vowels the recogniser is listening for.
 * Hearing it once does.
 *
 * The recordings were made with Hume's text to speech in Received Pronunciation and compressed to
 * Opus at 24 kbit mono: 31 files, 76 KB in total, about 2.5 KB each. Small enough that carrying all
 * of them costs less than a single photograph.
 *
 * ### The id is the filename and the command
 *
 * One string ties the row, the audio file `assets/dictate/say/<id>.ogg`, and the word
 * [MaSpokenFormat] matches. There is no table mapping one to another, so they cannot drift apart.
 */
object MaVoiceFormatCatalog {

    /**
     * @param id       matches the audio file and the command word
     * @param say      what he actually says, spaces and all
     * @param result   what appears, shown as a tiny worked example
     * @param group    for the headings on the settings screen
     */
    data class Mark(
        val id: String,
        val say: String,
        val result: String,
        val group: String,
    )

    private const val G_END = "Ending a sentence"
    private const val G_WRAP = "Around the last word"
    private const val G_MARK = "After the last word"
    private const val G_WHOLE = "The whole dictation"

    val ALL: List<Mark> = listOf(
        Mark("dot", "dot", "hello dot  \u2192  hello.", G_END),
        Mark("questionmark", "question mark", "ready question mark  \u2192  ready?", G_END),
        Mark("exclamationmark", "exclamation mark", "stop exclamation mark  \u2192  stop!", G_END),

        Mark("parenthesis", "parenthesis", "the word parenthesis  \u2192  the (word)", G_WRAP),
        Mark("squarebracket", "square bracket", "index square bracket  \u2192  [index]", G_WRAP),
        Mark("curlybracket", "curly bracket", "body curly bracket  \u2192  {body}", G_WRAP),
        Mark("anglebracket", "angle bracket", "tag angle bracket  \u2192  <tag>", G_WRAP),
        Mark("quote", "quote", "name quote  \u2192  \"name\"", G_WRAP),
        Mark("singlequote", "single quote", "name single quote  \u2192  'name'", G_WRAP),
        Mark("backtick", "backtick", "code backtick  \u2192  `code`", G_WRAP),
        Mark("star", "star", "bold star  \u2192  *bold*", G_WRAP),

        Mark("comma", "comma", "wait comma  \u2192  wait,", G_MARK),
        Mark("colon", "colon", "note colon  \u2192  note:", G_MARK),
        Mark("semicolon", "semicolon", "one semicolon  \u2192  one;", G_MARK),
        Mark("hash", "hash", "tag hash  \u2192  tag#", G_MARK),
        Mark("at", "at", "me at  \u2192  me@", G_MARK),
        Mark("percent", "percent", "ten percent  \u2192  ten%", G_MARK),
        Mark("ampersand", "ampersand", "you ampersand  \u2192  you&", G_MARK),
        Mark("plus", "plus", "one plus  \u2192  one+", G_MARK),
        Mark("minus", "minus", "one minus  \u2192  one-", G_MARK),
        Mark("slash", "slash", "path slash  \u2192  path/", G_MARK),
        Mark("backslash", "backslash", "path backslash  \u2192  path\\", G_MARK),
        Mark("pipe", "pipe", "a pipe  \u2192  a|", G_MARK),
        Mark("equals", "equals", "x equals  \u2192  x=", G_MARK),
        Mark("tilde", "tilde", "home tilde  \u2192  home~", G_MARK),
        Mark("caret", "caret", "up caret  \u2192  up^", G_MARK),
        Mark("dollar", "dollar", "cost dollar  \u2192  cost$", G_MARK),
        Mark("euro", "euro", "cost euro  \u2192  cost\u20AC", G_MARK),
        Mark("ellipsis", "ellipsis", "wait ellipsis  \u2192  wait\u2026", G_MARK),

        Mark("uppercase", "uppercase", "this matters uppercase  \u2192  THIS MATTERS", G_WHOLE),
        Mark(
            "underscore",
            "underscore",
            "my new project underscore  \u2192  my_new_project",
            G_WHOLE,
        ),
    )

    /** The groups in the order the screen shows them. */
    val GROUPS: List<String> = listOf(G_END, G_WRAP, G_MARK, G_WHOLE)

    fun assetPath(id: String): String = "dictate/say/$id.ogg"
}
