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

package dev.patrickgold.florisboard.dictate.nlp

import android.content.Context
import dev.patrickgold.florisboard.app.FlorisPreferenceStore
import dev.patrickgold.florisboard.dictate.DictateController
import dev.patrickgold.florisboard.dictate.MaLanguage
import dev.patrickgold.florisboard.dictate.MaLog
import dev.patrickgold.florisboard.dictate.data.history.DictateHistoryStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

/**
 * Owns the personal n-gram model: its file, when it learns, and when it is written out.
 *
 * One instance for the process. The model is small enough to hold in memory and too small to be
 * worth a database, so it lives as one file read once at start and written back on a delay.
 */
object MaNgram {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    /**
     * ONE MODEL PER LANGUAGE, AND THEY NEVER MEET.
     *
     * There was one model for everything he wrote. It learned Croatian and English into the same
     * counts and offered whichever was commoner, so typing Croatian produced English words with a
     * matching prefix and the reverse — and every AI word he accepted went into the same undivided
     * store.
     *
     * The EN/HR key already moves the transcription language, the keyboard subtype and the shipped
     * dictionary. **It moves the personal model now too, and that was the only part still deaf to
     * it.** What he wants is exactly this: a sentence in English, the badge, a sentence in Croatian,
     * and each one finished from words of its own language.
     *
     * Isolated, not weighted. A shared model with a language column would still let one language's
     * counts decide the ranking of the other's, which is the same failure wearing a schema.
     */
    private val models = mutableMapOf<String, MaNgramModel>()

    private val files = mutableMapOf<String, File>()

    private val modelsLock = Any()

    /** The language being written right now: the badge, which is the one control he touches. */
    private fun active(): String = MaLanguage.active()

    private fun modelFor(language: String): MaNgramModel = synchronized(modelsLock) {
        models.getOrPut(language) { MaNgramModel() }
    }

    /**
     * The model for the language being written.
     *
     * Kept as a property because the settings screen reads `MaNgram.model.vocabularySize` to report
     * how much has been learned. It now reports the ACTIVE language's model, which is the honest
     * answer to "how many words does it know" asked while writing in one of them.
     */
    val model: MaNgramModel
        get() = modelFor(active())

    @Volatile
    private var loaded = false

    /**
     * The application context, kept for reading the shipped dictionary asset.
     *
     * The application context and never an Activity or the keyboard service: this object outlives
     * both, and holding either would leak a whole view hierarchy for the sake of opening a file.
     */
    @Volatile
    private var appContext: Context? = null

    private var saveJob: Job? = null

    /**
     * Reads the model from disk, once.
     *
     * Safe to call repeatedly; the second call does nothing. Failure is silent by design: an
     * unreadable model is an empty model, which costs the user their learned words but not their
     * keyboard.
     */
    fun initialize(context: Context) {
        appContext = context.applicationContext
        if (loaded) return
        loaded = true
        // Both, at startup, rather than the active one on demand. He switches language mid-sentence
        // and a load on first use would put a disk read in the middle of typing.
        for (language in LANGUAGES) {
            val target = File(context.filesDir, "ma_ngram_$language.tsv")
            synchronized(modelsLock) { files[language] = target }
            scope.launch { modelFor(language).load(target) }
        }
        // The old single mixed file, removed once.
        //
        // It cannot be split: nothing in it records which language each count came from, and
        // guessing per word is exactly the mixing this change exists to end. It is deleted and the
        // backfill below rebuilds both models from the dictation history, which DOES carry a
        // language per entry. Nothing of value is lost that the history cannot give back.
        scope.launch {
            runCatching {
                val legacy = File(context.filesDir, LEGACY_FILE_NAME)
                if (legacy.exists()) {
                    legacy.delete()
                    val prefs by FlorisPreferenceStore
                    prefs.dictate.maNgramBackfilled.set(false)
                    MaLog.add("ngram", "mixed model removed; rebuilding one per language")
                }
            }
        }
    }

    /**
     * The sentence being written, assembled from single-character commits.
     *
     * Typing arrives one character at a time, so learning each commit as it came would fill the
     * model with single letters and nothing else. Characters are collected here and handed to the
     * model as whole sentences, which is the unit the counts are actually about. Dictation does not
     * pass through here: a transcript arrives complete and is learned as it stands.
     */
    private val pending = StringBuilder()

    private val pendingLock = Any()

    /**
     * Everything committed to the editor passes through here.
     *
     * A commit longer than one character is already a unit worth learning: a transcript, or a
     * suggestion accepted whole. A single character is a keystroke, and keystrokes are collected
     * until a sentence ends.
     *
     * A known limitation, and an accepted one: text typed and then deleted is still learned. Undoing
     * a count needs an edit history this has no access to, and the cost of the odd wrong word is one
     * count among thousands, which the ranking absorbs.
     */
    fun onCommit(text: String, isIncognito: Boolean) {
        if (isIncognito || text.isEmpty()) return
        if (text.length > 1) {
            learn(text, isIncognito)
            return
        }
        val ch = text[0]
        val ready: String? = synchronized(pendingLock) {
            pending.append(ch)
            when {
                ch in SENTENCE_ENDS || pending.length >= MAX_PENDING -> {
                    val out = pending.toString()
                    pending.setLength(0)
                    out
                }
                else -> null
            }
        }
        if (ready != null) learn(ready, isIncognito)
    }

    /** Hands over whatever sentence is half-written, for when the keyboard is closing. */
    fun flushPending(isIncognito: Boolean) {
        val ready: String? = synchronized(pendingLock) {
            if (pending.isEmpty()) null else pending.toString().also { pending.setLength(0) }
        }
        if (ready != null) learn(ready, isIncognito)
    }

    /**
     * Folds [text] into the model.
     *
     * Refused in incognito mode, which is the whole promise of incognito mode; refused when the
     * feature is off; and refused for anything long enough to be a paste rather than a sentence,
     * because a pasted document would swamp the counts with somebody else's vocabulary.
     */
    fun learn(text: String, isIncognito: Boolean) {
        if (isIncognito || text.isBlank()) return
        val prefs by FlorisPreferenceStore
        if (!prefs.dictate.maNgramEnabled.get()) return
        if (text.length > MAX_LEARN_LENGTH) return
        // The language at the moment of writing, captured here rather than inside the coroutine: he
        // may press the badge between the commit and the save, and the sentence belongs to the
        // language it was written in.
        val badge = active()
        scope.launch {
            // EVERY WORD GOES THROUGH THE GATE, AND ALMOST NONE OF THEM COST ANYTHING.
            //
            // The badge is right about a sentence and not always about a word: he writes an English
            // word inside a Croatian sentence and back again, and a word filed under the badge alone
            // is offered in the wrong language for good.
            //
            // So each word is checked against what the models already know. **A word already in a
            // model is already in the right place** — it was filed once, correctly, and asking again
            // would pay to be told what is on disk. Words carrying č ć ž š đ are Croatian with no
            // help from anybody. Only a word that is new AND unmarked is queued to be asked about,
            // once, ever.
            //
            // The sentence is still learned WHOLE into the badge's model, because an n-gram learns
            // sequences and a sentence chopped into per-word destinations would teach neither model
            // how the words follow each other. The gate decides where a NEW WORD belongs; the queue
            // fixes it afterwards if the badge was wrong about it.
            for (raw in text.split(Regex("\\s+"))) {
                val word = MaWordLanguage.normalise(raw)
                if (word.isBlank()) continue
                when (
                    val answer = MaWordLanguage.decide(
                        word = word,
                        badge = badge,
                        knownEn = modelFor(MaWordLanguage.EN).knows(word),
                        knownHr = modelFor(MaWordLanguage.HR).knows(word),
                    )
                ) {
                    is MaWordLanguage.Answer.Known -> Unit
                    is MaWordLanguage.Answer.Certain ->
                        if (answer.language != badge) modelFor(answer.language).learn(word)
                    is MaWordLanguage.Answer.Ask -> queueForAsking(word)
                }
            }
            modelFor(badge).learn(text)
            scheduleSave()
        }
    }

    /**
     * Reads every past transcription into the model, once.
     *
     * ### Why this is the single best thing that can be done for prediction
     *
     * The model only ever learned from words committed **through this keyboard**, which meant a
     * fresh install knew nothing and had to be taught his vocabulary again by hand, one sentence at
     * a time, over weeks. Meanwhile every dictation he has ever made was sitting in the history
     * database — thousands of his own words, in his own phrasing, about his own subjects.
     *
     * That is a better corpus for predicting his writing than any general model, because it is not
     * a sample of how people write. It is a record of how *he* writes.
     *
     * ### Once, and marked
     *
     * Guarded by a preference rather than by "is the model empty", because a model that has learned
     * a little is still worth backfilling, and a second pass would double every count and skew the
     * ranking towards whatever happened to be in history.
     *
     * ### Newest first, and capped
     *
     * [BACKFILL_MAX] entries, taken from the most recent, because the far past is the least like
     * what he is writing today and reading all of it on a first run would stall the app behind a
     * database it does not need yet. The work happens on the io scope like every other write here.
     */
    fun backfillFromHistory(context: Context) {
        val prefs by FlorisPreferenceStore
        if (!prefs.dictate.maNgramEnabled.get()) return
        if (prefs.dictate.maNgramBackfilled.get()) return
        scope.launch {
            runCatching {
                val entries = DictateHistoryStore.getAll(context.applicationContext)
                    .take(BACKFILL_MAX)
                var learned = 0
                for (entry in entries) {
                    val text = entry.text
                    if (text.isBlank() || text.length > MAX_LEARN_LENGTH) continue
                    // Routed by the language the entry was DICTATED in, which the history has
                    // recorded all along. This is why the mixed file could be thrown away without
                    // losing anything: the evidence for the split was already on disk.
                    val language = if (entry.language.substringBefore('-').lowercase() == MaLanguage.HR) {
                        MaLanguage.HR
                    } else {
                        MaLanguage.EN
                    }
                    modelFor(language).learn(text)
                    learned++
                }
                prefs.dictate.maNgramBackfilled.set(true)
                if (learned > 0) {
                    scheduleSave()
                    MaLog.add("ngram", "learned $learned past dictations into ${LANGUAGES.size} models")
                }
            }.onFailure { MaLog.add("ngram", "backfill failed: ${it.message}") }
        }
    }

    /**
     * The next-word predictions for the text before the cursor.
     *
     * Returns nothing when the feature is off, in incognito mode, or before the model has read
     * enough to be worth trusting. That last one matters: a model with a hundred words in it will
     * answer every query, and every answer will be noise, which teaches the user to ignore the strip
     * before the model is ever good enough to be worth reading.
     */
    fun predict(textBeforeCursor: String, currentWord: String, isIncognito: Boolean): List<MaNgramSuggestion> {
        if (isIncognito) return emptyList()
        val prefs by FlorisPreferenceStore
        if (!prefs.dictate.maNgramEnabled.get()) return emptyList()
        // The personal model, when it has seen enough to be worth asking. Below that threshold it
        // is not silent because it is broken; it is silent because a handful of words produces
        // confident nonsense.
        // The word being typed, worked out from the text when the editor will not say.
        //
        // `currentWordText` comes from the composing region, and many apps never set one — a web
        // view, a dialog, anything drawing its own input. In those the prefix arrived empty, and an
        // empty prefix means something entirely different downstream: it stops being "complete this
        // word" and becomes "what word comes next". That is why typing `other` offered `things` and
        // `parts`. They were not wrong answers to the question asked; the wrong question was asked.
        //
        // So when the editor does not tell us, read it off the end of the text. The trailing run of
        // letters before the cursor IS the word in progress, apostrophes and hyphens included so
        // `don't` and `well-known` are not cut in half.
        val trailing = textBeforeCursor.takeLastWhile { it.isLetter() || it == '\'' || it == '-' }
        val derived = currentWord.isBlank() && trailing.isNotEmpty()
        val word = if (derived) trailing else currentWord
        // And when the word was derived, the context has to lose it too. `contextOf` reads the last
        // complete words before the cursor, and it would otherwise count the half-typed word as the
        // previous one — predicting what follows `other` while he is still writing `other`.
        val context = if (derived) textBeforeCursor.dropLast(trailing.length) else textBeforeCursor
        // Only this language's model. Never the other one, not even to fill an empty row: an English
        // word offered while writing Croatian is not a weaker suggestion, it is a wrong one.
        val active = modelFor(active())
        val personal = if (active.totalWords >= MIN_WORDS_BEFORE_PREDICTING) {
            val (two, one) = active.contextOf(context)
            active.predict(previousTwo = two, previousOne = one, prefix = word)
        } else {
            emptyList()
        }
        if (personal.size >= PREDICT_LIMIT) return personal
        // The shipped Croatian list fills what is left, and only then.
        //
        // Three conditions, each of them the point rather than caution. A prefix, because a word
        // list with no prefix is the commonest word in the language rather than a prediction.
        // Croatian, because English already has a dictionary from upstream and offering both would
        // put Croatian words under an English sentence. And last, always, so a word he has actually
        // written outranks a word the language merely contains.
        //
        // Deliberately outside the totalWords gate: an empty personal model is exactly the state
        // this exists for. A fresh install should suggest Croatian on the first word, not after
        // three hundred.
        if (word.isEmpty()) return personal
        if (MaLanguage.active() != MaLanguage.HR) return personal
        val appCtx = appContext ?: return personal
        val already = personal.mapTo(HashSet()) { it.word.lowercase() }
        return personal + MaBaseDictionary.suggest(
            context = appCtx,
            prefix = word,
            limit = PREDICT_LIMIT - personal.size,
            exclude = already,
        )
    }

    /**
     * Writes the model out after a pause in typing.
     *
     * Debounced rather than written per word: learning happens on every commit, and a file write per
     * keystroke would be both wasteful and a good way to be killed mid-write. The delay is long
     * enough to batch a sentence and short enough that little is lost if the process dies.
     */
    private fun scheduleSave() {
        saveJob?.cancel()
        saveJob = scope.launch {
            delay(SAVE_DELAY_MS)
            // Every model, not the active one. He switches language mid-sentence, so the one that
            // just learned something may not be the one in front of him by the time this runs.
            for (language in LANGUAGES) {
                val m = modelFor(language)
                if (!m.isDirty) continue
                m.prune()
                synchronized(modelsLock) { files[language] }?.let { m.save(it) }
            }
        }
    }

    /** Writes immediately, for when the keyboard is going away and the delay would not finish. */
    fun flush() {
        saveJob?.cancel()
        scope.launch {
            for (language in LANGUAGES) {
                val m = modelFor(language)
                if (m.isDirty) synchronized(modelsLock) { files[language] }?.let { m.save(it) }
            }
        }
    }

    /** Forgets everything and removes the file. */
    fun forgetEverything() {
        scope.launch {
            // Everything means everything. "Forget what you have learned" asked while writing
            // English cannot sensibly leave the Croatian model standing — he would clear it, see the
            // count go to zero, and still be offered his own words the moment he pressed the badge.
            for (language in LANGUAGES) {
                modelFor(language).clear()
                synchronized(modelsLock) { files[language] }?.delete()
            }
        }
    }

    /** The two, and the only two. The badge has two states and so has this. */
    private val LANGUAGES = listOf(MaLanguage.EN, MaLanguage.HR)

    /** The single mixed model, deleted on the first run after the split. */
    private const val LEGACY_FILE_NAME = "ma_ngram.tsv"

    /** Long enough to batch a sentence, short enough to lose little if the process is killed. */
    private const val SAVE_DELAY_MS = 4_000L

    /** Beyond this a commit is a paste, not writing, and learning it would swamp the counts. */
    private const val MAX_LEARN_LENGTH = 2_000

    /** Below this the model answers everything and is right about nothing. */
    /** How many suggestions the row asks for. Matches MaNgramModel.predict's own default. */
    private const val PREDICT_LIMIT = 3

    /** How many past dictations the first run reads. Newest first; the far past predicts least. */
    private const val BACKFILL_MAX = 2000

    private const val MIN_WORDS_BEFORE_PREDICTING = 300L

    /** What ends a sentence for the purpose of handing it over. */
    private val SENTENCE_ENDS = charArrayOf('.', '!', '?', '\n', ':', ';')

    /** A sentence this long without punctuation is handed over anyway. */
    private const val MAX_PENDING = 400

    // ---------------------------------------------------------------------------------------------
    // The queue of words nobody has classified yet
    // ---------------------------------------------------------------------------------------------

    /**
     * Words seen once, carrying no Croatian letters, and known to neither model.
     *
     * They are already learned under the badge, so nothing is lost and nothing waits. This queue is
     * only for CORRECTING that guess, later, in the background, in one batch.
     *
     * Persisted, because the correction is worth having tomorrow and the phone will be killed
     * tonight. Capped: past a few hundred the list is not a queue, it is a leak, and the words at
     * the front are the ones he actually uses.
     */
    // A lock of its own. `pendingLock` already guards the commit buffer above and the two protect
    // different things; sharing one would make a word waiting to be classified block a keystroke
    // waiting to be learned, which is the wrong way round for the only one of them he can feel.
    private val askLock = Any()

    /**
     * Adds [word] to the queue.
     *
     * The lock computes the new list; the WRITE happens outside it.
     *
     * `set` on a preference is a suspend function, and suspending inside `synchronized` is not
     * allowed — Kotlin refuses it outright, which is the right refusal: a coroutine that suspends
     * while holding a monitor can resume on a different thread and try to release a lock it does not
     * own. So the critical section decides what the list should be and hands it out, and the write
     * is an ordinary suspending call in the coroutine that called this.
     */
    private suspend fun queueForAsking(word: String) {
        val prefs by FlorisPreferenceStore
        val next = synchronized(askLock) {
            val current = prefs.dictate.maNgramPending.get()
                .split(' ').filter { it.isNotBlank() }.toMutableList()
            if (word in current) return
            current += word
            while (current.size > PENDING_CAP) current.removeAt(0)
            current.joinToString(" ")
        }
        prefs.dictate.maNgramPending.set(next)
    }

    /** How many words are waiting to be classified. Shown on the settings screen. */
    fun pendingCount(): Int {
        val prefs by FlorisPreferenceStore
        return prefs.dictate.maNgramPending.get().split(' ').count { it.isNotBlank() }
    }

    /**
     * Asks the model about the queue, in one batch, and files the answers.
     *
     * Never on the typing path and never automatic. It is a button he presses, because it costs
     * money and takes a second and there is no moment while typing when either is acceptable.
     *
     * A word answered `both` is learned into both models: "radio", "auto", "student", "film" are
     * ordinary in both languages, and forcing a choice would delete a suggestion he had earned to
     * satisfy a schema.
     *
     * Words that come back unclassified are dropped from the queue rather than retried. They stay
     * where the badge put them, which was never worse than a guess, and a retry loop over a word
     * nobody can classify is a bill with no end.
     */
    suspend fun classifyPending(onMessage: (String) -> Unit) {
        val prefs by FlorisPreferenceStore
        val words = synchronized(askLock) {
            prefs.dictate.maNgramPending.get().split(' ').filter { it.isNotBlank() }
        }.take(BATCH)
        if (words.isEmpty()) {
            onMessage("Nothing waiting")
            return
        }
        val reply = DictateController.askCheapModel(MaWordLanguage.prompt(words))
        if (reply == null) {
            onMessage("No answer \u2014 check the key and the network")
            return
        }
        val answers = MaWordLanguage.readAnswer(reply, words.toSet())
        for ((word, langs) in answers) {
            for (language in langs) modelFor(language).learn(word)
        }
        // Same shape as above, and for the same reason: decide inside the lock, write outside it.
        val rest = synchronized(askLock) {
            prefs.dictate.maNgramPending.get()
                .split(' ').filter { it.isNotBlank() && it !in words }
                .joinToString(" ")
        }
        prefs.dictate.maNgramPending.set(rest)
        scheduleSave()
        onMessage("Filed ${answers.size} of ${words.size}")
    }

    /** How many words go in one request. Enough to be worth the round trip, small enough to read. */
    private const val BATCH = 60

    /** Past this the queue is a leak rather than a queue. */
    private const val PENDING_CAP = 400

    /** What each model knows, for the settings screen. */
    fun vocabularyOf(language: String): Int = modelFor(language).vocabularySize

    /** How many words that model has read in total. */
    fun totalOf(language: String): Long = modelFor(language).totalWords

    /** Forgets one language only, leaving the other standing. */
    fun forget(language: String) {
        modelFor(language).clear()
        synchronized(modelsLock) { files[language] }?.delete()
    }
}
