/*
 * Copyright (C) 2021-2025 The FlorisBoard Contributors
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

package dev.patrickgold.florisboard.ime.nlp

import android.content.Context
import android.os.SystemClock
import android.util.LruCache
import dev.patrickgold.florisboard.app.FlorisPreferenceStore
import dev.patrickgold.florisboard.clipboardManager
import dev.patrickgold.florisboard.editorInstance
import dev.patrickgold.florisboard.ime.clipboard.provider.ClipboardItem
import dev.patrickgold.florisboard.ime.clipboard.provider.ItemType
import dev.patrickgold.florisboard.ime.core.Subtype
import dev.patrickgold.florisboard.dictate.MaLanguage
import dev.patrickgold.florisboard.dictate.nlp.MaNgram
import dev.patrickgold.florisboard.ime.editor.EditorContent
import dev.patrickgold.florisboard.ime.editor.EditorRange
import dev.patrickgold.florisboard.ime.dictionary.DictionaryManager
import dev.patrickgold.florisboard.ime.dictionary.UserDictionaryEntry
import dev.patrickgold.florisboard.ime.media.emoji.EmojiSuggestionProvider
import dev.patrickgold.florisboard.ime.nlp.han.HanShapeBasedLanguageProvider
import dev.patrickgold.florisboard.ime.nlp.latin.LatinLanguageProvider
import dev.patrickgold.florisboard.keyboardManager
import dev.patrickgold.florisboard.lib.util.NetworkUtils
import dev.patrickgold.florisboard.subtypeManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.florisboard.lib.kotlin.guardedByLock
import org.florisboard.lib.kotlin.collectLatestIn
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.properties.Delegates
import dev.patrickgold.florisboard.ime.text.key.KeyCode

private const val BLANK_STR_PATTERN = "^\\s*$"

// Frequency stored for a word learned from the suggestion strip (issue #241) — the maximum, matching what
// the user dictionary settings screen assigns to a hand-added word.
private const val USER_DICTIONARY_FREQ = 255

class NlpManager(context: Context) {
    private val blankStrRegex = Regex(BLANK_STR_PATTERN)

    private val prefs by FlorisPreferenceStore
    private val clipboardManager by context.clipboardManager()
    private val editorInstance by context.editorInstance()
    private val keyboardManager by context.keyboardManager()
    private val subtypeManager by context.subtypeManager()

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val emojiSuggestionProvider = EmojiSuggestionProvider(context)
    private val providers = guardedByLock {
        mapOf(
            LatinLanguageProvider.ProviderId to ProviderInstanceWrapper(LatinLanguageProvider(context)),
            HanShapeBasedLanguageProvider.ProviderId to ProviderInstanceWrapper(HanShapeBasedLanguageProvider(context)),
        )
    }
    // lock unnecessary because values constant
    private val providersForceSuggestionOn = mutableMapOf<String, Boolean>()

    private val internalSuggestionsGuard = Mutex()
    private var internalSuggestions by Delegates.observable(SystemClock.uptimeMillis() to listOf<SuggestionCandidate>()) { _, _, _ ->
        scope.launch { assembleCandidates() }
    }

    private val _activeCandidatesFlow = MutableStateFlow(listOf<SuggestionCandidate>())
    val activeCandidatesFlow = _activeCandidatesFlow.asStateFlow()
    inline var activeCandidates
        get() = activeCandidatesFlow.value
        private set(v) {
            _activeCandidatesFlow.value = v
        }

    val debugOverlaySuggestionsInfos = LruCache<Long, Pair<String, SpellingResult>>(10)
    var debugOverlayVersion = MutableStateFlow(0)

    init {
        clipboardManager.primaryClipFlow.collectLatestIn(scope) {
            assembleCandidates()
        }
        prefs.suggestion.enabled.asFlow().collectLatestIn(scope) {
            assembleCandidates()
        }
        prefs.emoji.suggestionEnabled.asFlow().collectLatestIn(scope) {
            assembleCandidates()
        }
        subtypeManager.activeSubtypeFlow.collectLatestIn(scope) { subtype ->
            preload(subtype)
        }
    }

    /**
     * Gets the punctuation rule from the currently active subtype and returns it. Falls back to a default one if the
     * subtype does not exist or defines an invalid punctuation rule.
     *
     * @return The punctuation rule or a fallback.
     */
    fun getActivePunctuationRule(): PunctuationRule {
        return getPunctuationRule(subtypeManager.activeSubtype)
    }

    /**
     * Gets the punctuation rule from the given subtype and returns it. Falls back to a default one if the subtype does
     * not exist or defines an invalid punctuation rule.
     *
     * @return The punctuation rule or a fallback.
     */
    fun getPunctuationRule(subtype: Subtype): PunctuationRule {
        return keyboardManager.resources.punctuationRules.value[subtype.punctuationRule] ?: PunctuationRule.Fallback
    }

    private suspend fun getSpellingProvider(subtype: Subtype): SpellingProvider {
        return providers.withLock { it[subtype.nlpProviders.spelling] }?.provider as? SpellingProvider
            ?: FallbackNlpProvider
    }

    private suspend fun getSuggestionProvider(subtype: Subtype): SuggestionProvider {
        return providers.withLock { it[subtype.nlpProviders.suggestion] }?.provider as? SuggestionProvider
            ?: FallbackNlpProvider
    }

    fun preload(subtype: Subtype) {
        scope.launch {
            emojiSuggestionProvider.preload(subtype)
            providers.withLock { providers ->
                subtype.nlpProviders.forEach { _, providerId ->
                    providers[providerId]?.let { provider ->
                        provider.createIfNecessary()
                        provider.preload(subtype)
                    }
                }
            }
        }
    }

    /**
     * Spell wrapper helper which calls the spelling provider and returns the result. Coroutine management must be done
     * by the source spell checker service.
     */
    suspend fun spell(
        subtype: Subtype,
        word: String,
        precedingWords: List<String>,
        followingWords: List<String>,
        maxSuggestionCount: Int,
    ): SpellingResult {
        return getSpellingProvider(subtype).spell(
            subtype = subtype,
            word = word,
            precedingWords = precedingWords,
            followingWords = followingWords,
            maxSuggestionCount = maxSuggestionCount,
            allowPossiblyOffensive = true,
            isPrivateSession = keyboardManager.activeState.isIncognitoMode,
        )
    }

    suspend fun determineLocalComposing(
        textBeforeSelection: CharSequence, breakIterators: BreakIteratorGroup, localLastCommitPosition: Int
    ): EditorRange {
        return getSuggestionProvider(subtypeManager.activeSubtype).determineLocalComposing(
            subtypeManager.activeSubtype, textBeforeSelection, breakIterators, localLastCommitPosition
        )
    }

    fun providerForcesSuggestionOn(subtype: Subtype): Boolean {
        // Using a cache because I have no idea how fast the runBlocking is
        return providersForceSuggestionOn.getOrPut(subtype.nlpProviders.suggestion) {
            runBlocking {
                getSuggestionProvider(subtype).forcesSuggestionOn
            }
        }
    }

    fun isSuggestionOn(): Boolean =
        prefs.suggestion.enabled.get()
            || prefs.emoji.suggestionEnabled.get()
            || providerForcesSuggestionOn(subtypeManager.activeSubtype)

    // Set by a glide-typing commit: the word commit itself triggers one resetSuggestions → suggest() that
    // would immediately wipe the just-shown glide alternatives. This one-shot flag makes that next suggest()
    // a no-op so the alternatives stay in the strip until the user's next input (issue #127).
    @Volatile
    private var holdNextSuggest = false

    fun suggest(subtype: Subtype, content: EditorContent) {
        if (holdNextSuggest) {
            holdNextSuggest = false
            return
        }
        val reqTime = SystemClock.uptimeMillis()
        scope.launch {
            val emojiSuggestions = when {
                prefs.emoji.suggestionEnabled.get() -> {
                    emojiSuggestionProvider.suggest(
                        subtype = subtype,
                        content = content,
                        maxCandidateCount = prefs.emoji.suggestionCandidateMaxCount.get(),
                        allowPossiblyOffensive = true,
                        isPrivateSession = keyboardManager.activeState.isIncognitoMode,
                    )
                }
                else -> emptyList()
            }
            val suggestions = when {
                emojiSuggestions.isNotEmpty() && prefs.emoji.suggestionType.get().prefix.isNotEmpty() -> {
                    emptyList()
                }
                else -> {
                    getSuggestionProvider(subtype).suggest(
                        subtype = subtype,
                        content = content,
                        maxCandidateCount = 8,
                        allowPossiblyOffensive = true,
                        isPrivateSession = keyboardManager.activeState.isIncognitoMode,
                    )
                }
            }
            // The personal model goes in front of the shipped one.
            //
            // It knows nothing about language in general and a great deal about the words written on
            // this phone, which is exactly the set the shipped dictionary is missing: place names,
            // project names, the jargon of one trade. Where it has an answer at all it is the better
            // answer, and where it has none it returns nothing and costs nothing.
            //
            // Duplicates are dropped rather than shown twice: the same word arriving from both is
            // the common case, not the exception.
            val personal = MaNgram.predict(
                textBeforeCursor = content.textBeforeSelection,
                currentWord = content.currentWordText,
                isIncognito = keyboardManager.activeState.isIncognitoMode,
            ).map { hit ->
                WordSuggestionCandidate(
                    text = hit.word,
                    secondaryText = null,
                    confidence = 0.9,
                    isEligibleForAutoCommit = false,
                    sourceProvider = null,
                )
            }
            // THE SHIPPED DICTIONARY ONLY SPEAKS WHEN IT IS SPEAKING HIS LANGUAGE.
            //
            // This is the bug he photographed: the badge said HR and the row offered "what",
            // "existing", "are", "for", "to", "and", "in". None of those came from the personal
            // model — that one was split by language and is innocent. They came from the shipped
            // dictionary, which is chosen per subtype and falls back to an English one when the
            // subtype has no dictionary of its own.
            //
            // So a provider whose locale does not match the badge is DROPPED ENTIRELY. Not ranked
            // lower, dropped. **A wrong-language suggestion is not a weaker answer, it is a wrong
            // one**, and an English row under a Croatian badge is worse than an empty row: the empty
            // row tells the truth, which is that nothing here knows Croatian yet.
            //
            // What is left when it is dropped is his own Croatian model, which is the thing that
            // learns as he writes. That is the honest state of it, and it fills.
            val badge = MaLanguage.active()
            val providerLanguage = subtype.primaryLocale.language.lowercase()
            val providerAgrees = providerLanguage == badge
            internalSuggestionsGuard.withLock {
                if (internalSuggestions.first < reqTime) {
                    internalSuggestions = reqTime to buildList {
                        addAll(emojiSuggestions)
                        addAll(personal)
                        if (providerAgrees) {
                            val seen = personal.map { it.text.toString().lowercase() }.toSet()
                            addAll(suggestions.filter { it.text.toString().lowercase() !in seen })
                        }
                    }
                }
            }
        }
    }

    fun suggestDirectly(suggestions: List<SuggestionCandidate>, holdNext: Boolean = false) {
        val reqTime = SystemClock.uptimeMillis()
        holdNextSuggest = holdNext
        runBlocking {
            internalSuggestions = reqTime to suggestions
        }
    }

    fun clearSuggestions() {
        val reqTime = SystemClock.uptimeMillis()
        runBlocking {
            internalSuggestions = reqTime to emptyList()
        }
    }

    fun getAutoCommitCandidate(): SuggestionCandidate? {
        return activeCandidates.firstOrNull { it.isEligibleForAutoCommit }
    }

    /** Outcome of [addToUserDictionary], so the caller knows what (if anything) to tell the user. */
    enum class AddToDictionaryResult { ADDED, ALREADY_PRESENT, UNAVAILABLE }

    /**
     * Adds [candidate]'s word to the personal dictionary for [subtype]'s language (issue #241) and re-runs
     * the suggestions so it is treated as known from the very next keystroke — which is the point of the
     * feature: [LatinLanguageProvider] consults the user dictionary in `isKnownWord`, so a learned word is
     * never autocorrected again.
     *
     * Stored at the maximum frequency, matching what the settings screen uses when a word is added by hand.
     */
    fun addToUserDictionary(subtype: Subtype, candidate: SuggestionCandidate): AddToDictionaryResult {
        val word = candidate.text.toString().trim()
        if (word.isEmpty()) return AddToDictionaryResult.UNAVAILABLE
        val dao = DictionaryManager.default().florisUserDictionaryDao()
            ?: return AddToDictionaryResult.UNAVAILABLE // the personal dictionary is switched off
        val locale = subtype.primaryLocale
        return runCatching {
            if (dao.queryExactFuzzyLocale(word, locale).isNotEmpty()) {
                AddToDictionaryResult.ALREADY_PRESENT
            } else {
                dao.insert(
                    UserDictionaryEntry(
                        id = 0,
                        word = word,
                        freq = USER_DICTIONARY_FREQ,
                        locale = locale.localeTag(),
                        shortcut = null,
                    )
                )
                scope.launch { suggest(subtypeManager.activeSubtype, editorInstance.activeContent) }
                AddToDictionaryResult.ADDED
            }
        }.getOrDefault(AddToDictionaryResult.UNAVAILABLE)
    }

    fun removeSuggestion(subtype: Subtype, candidate: SuggestionCandidate): Boolean {
        return runBlocking { candidate.sourceProvider?.removeSuggestion(subtype, candidate) == true }.also { result ->
            if (result) {
                scope.launch {
                    // Need to re-trigger the suggestions algorithm. Only word candidates reach this
                    // now: the clipboard branch went with the clipboard suggestion itself.
                    suggest(subtypeManager.activeSubtype, editorInstance.activeContent)
                }
            }
        }
    }

    fun getListOfWords(subtype: Subtype): List<String> {
        return runBlocking { getSuggestionProvider(subtype).getListOfWords(subtype) }
    }

    fun getFrequencyForWord(subtype: Subtype, word: String): Double {
        return runBlocking { getSuggestionProvider(subtype).getFrequencyForWord(subtype, word) }
    }

    private fun assembleCandidates() {
        runBlocking {
            val candidates = when {
                // Word suggestions only. The clipboard used to elbow its way in here as a
                // candidate chip, and it is gone: the copy buckets are the clipboard now, they have
                // their own strip above the keyboard, and two clipboards on one screen is what
                // produced a suggestion row drawn in two layers on top of itself.
                isSuggestionOn() -> {
                    buildList {
                        internalSuggestionsGuard.withLock {
                            addAll(internalSuggestions.second)
                        }
                    }
                }
                else -> emptyList()
            }
            activeCandidates = candidates
            autoExpandCollapseSmartbarActions(candidates, NlpInlineAutofill.suggestions.value)
        }
    }

    fun autoExpandCollapseSmartbarActions(list1: List<*>?, list2: List<*>?) {
        if (!prefs.smartbar.enabled.get()) {// || !prefs.smartbar.sharedActionsAutoExpandCollapse.get()) {
            return
        }
        // TODO: this is a mess and needs to be cleaned up in v0.5 with the NLP development
        /*if (keyboardManager.inputEventDispatcher.isRepeatableCodeLastDown()
            && !keyboardManager.inputEventDispatcher.isPressed(KeyCode.DELETE)
            && !keyboardManager.inputEventDispatcher.isPressed(KeyCode.FORWARD_DELETE)
            || keyboardManager.activeState.isActionsOverflowVisible
        ) {
            return // We do not auto switch if a repeatable action key was last pressed or if the actions overflow
                   // menu is visible to prevent annoying UI changes
        }*/
        // WHILE DELETING, THE BAR HOLDS STILL.
        //
        // He deletes a word, the suggestions empty at the space, the bar expands to the action row;
        // one more character and a suggestion returns, so it collapses again. Held down, backspace
        // does that several times a second and **the whole keyboard walks up and down while the text
        // above it jumps with it.** His words: it makes him dizzy.
        //
        // FlorisBoard wrote this exact guard and left it commented out above, with a TODO. The
        // reasoning in it was right — do not auto-switch while a repeatable key is down, because the
        // user is holding a key rather than finishing a word — so it is turned on rather than
        // reinvented.
        //
        // Narrower than theirs: theirs also bailed while the overflow menu was open, which is a
        // different problem and not one he has. This asks one question — is a repeat running — and
        // a repeat is exactly when the answer changes fastest and matters least.
        //
        // Not a delay or a smoothing. **The bar does not move at all while the key is down**, and it
        // settles once when he lets go, which is the moment he is ready to read it.
        val dispatcher = keyboardManager.inputEventDispatcher
        if (dispatcher.isPressed(KeyCode.DELETE) || dispatcher.isPressed(KeyCode.FORWARD_DELETE)) {
            return
        }
        val isSelection = editorInstance.activeContent.selection.isSelectionMode
        val isExpanded = list1.isNullOrEmpty() && list2.isNullOrEmpty() || isSelection
        // Only write when the expanded state actually changes. This runs on every keystroke (via
        // assembleCandidates); the state usually stays the same while typing a word, so the guard avoids
        // two redundant pref writes per character that would otherwise bounce the Smartbar flows into a
        // recomposition (and schedule a datastore persist) each time — a contributor to the typing jank.
        if (prefs.smartbar.sharedActionsExpanded.get() != isExpanded) {
            scope.launch {
                prefs.smartbar.sharedActionsExpandWithAnimation.set(false)
                prefs.smartbar.sharedActionsExpanded.set(isExpanded)
            }
        }
    }

    fun addToDebugOverlay(word: String, info: SpellingResult) {
        debugOverlaySuggestionsInfos.put(System.currentTimeMillis(), word to info)
        debugOverlayVersion.update { it + 1 }
    }

    fun clearDebugOverlay() {
        debugOverlaySuggestionsInfos.evictAll()
        debugOverlayVersion.update { it + 1 }
    }

    private class ProviderInstanceWrapper(val provider: NlpProvider) {
        private var isInstanceAlive = AtomicBoolean(false)

        suspend fun createIfNecessary() {
            if (!isInstanceAlive.getAndSet(true)) provider.create()
        }

        suspend fun preload(subtype: Subtype) {
            provider.preload(subtype)
        }

        suspend fun destroyIfNecessary() {
            if (isInstanceAlive.getAndSet(true)) provider.destroy()
        }
    }

}
