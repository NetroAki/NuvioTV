package com.nuvio.tv.ui.screens.player

import java.util.Locale

internal data class AudioTrackPreference(
    val languages: List<String>,
    val label: String? = null,
)

internal object AudioTrackSelectionPolicy {
    private val commentaryPattern = Regex("\\b(commentary|director(?:'s)? comments?|cast commentary)\\b", RegexOption.IGNORE_CASE)
    private val descriptionPattern =
        Regex(
            "\\b(audio description|audio described|descriptive audio|described video|visual(?:ly)? impaired|audesc)\\b",
            RegexOption.IGNORE_CASE,
        )
    private val presentationNoise =
        Regex(
            "\\b(aac|ac-?3|e-?ac-?3|dts(?:-hd)?|truehd|opus|flac|stereo|mono|surround|atmos|[257]\\.?1|\\d+\\s*ch)\\b",
            RegexOption.IGNORE_CASE,
        )

    fun select(
        tracks: List<TrackInfo>,
        preference: AudioTrackPreference,
    ): Int? {
        if (tracks.isEmpty()) return null
        val normalizedLanguages = preference.languages.map(::normalizeLanguage).filter(String::isNotEmpty)
        val preferredLabel = normalizeLabel(preference.label)
        return tracks
            .map { track -> track to score(track, normalizedLanguages, preferredLabel) }
            .sortedWith(compareByDescending<Pair<TrackInfo, Int>> { it.second }.thenBy { semanticKey(it.first) })
            .first()
            .first
            .index
    }

    fun isCommentary(track: TrackInfo): Boolean = commentaryPattern.containsMatchIn(track.name)

    fun isAudioDescription(track: TrackInfo): Boolean = descriptionPattern.containsMatchIn(track.name)

    private fun score(
        track: TrackInfo,
        languages: List<String>,
        preferredLabel: String,
    ): Int {
        val language = normalizeLanguage(track.language)
        val languageRank = languages.indexOfFirst { preferred -> languageMatches(language, preferred) }
        val languageScore = if (languageRank >= 0) 1_000 - (languageRank * 100) else 0
        val label = normalizeLabel(track.name)
        val labelScore =
            when {
                preferredLabel.isEmpty() -> 0
                label == preferredLabel -> 250
                label.contains(preferredLabel) || preferredLabel.contains(label) -> 120
                else -> 0
            }
        val accessibilityPenalty =
            when {
                isCommentary(track) -> 10_000
                isAudioDescription(track) -> 9_000
                else -> 0
            }
        return languageScore + labelScore - accessibilityPenalty
    }

    private fun semanticKey(track: TrackInfo): String =
        listOf(normalizeLanguage(track.language), normalizeLabel(track.name), track.codec.orEmpty())
            .joinToString("|")

    private fun normalizeLabel(value: String?): String =
        value
            .orEmpty()
            .lowercase()
            .replace(presentationNoise, " ")
            .replace(Regex("\\s+"), " ")
            .trim()

    private fun normalizeLanguage(value: String?): String {
        val language =
            value
                .orEmpty()
                .trim()
                .lowercase()
                .replace('_', '-')
                .substringBefore('-')
        if (language.isEmpty() || language == "und") return ""
        return runCatching { Locale.forLanguageTag(language).isO3Language.lowercase() }.getOrDefault(language)
    }

    private fun languageMatches(
        actual: String,
        preferred: String,
    ): Boolean = actual.isNotEmpty() && preferred.isNotEmpty() && actual == preferred
}
