package com.nuvio.tv.data.local

enum class AudioTrackPreferenceScope {
    EPISODE,
    SERIES,
}

data class PersistedAudioTrackPreference(
    val language: String?,
    val name: String?,
)

data class ScopedAudioTrackPreference(
    val preference: PersistedAudioTrackPreference,
    val scope: AudioTrackPreferenceScope,
)

internal fun supportsEpisodeAudioPreference(contentType: String?, videoId: String?): Boolean =
    !videoId.isNullOrBlank() && (
        contentType.equals("series", ignoreCase = true) ||
            contentType.equals("tv", ignoreCase = true)
    )

internal fun resolveScopedAudioPreference(
    episodePreference: PersistedAudioTrackPreference?,
    seriesPreference: PersistedAudioTrackPreference?,
): ScopedAudioTrackPreference? = when {
    episodePreference != null -> ScopedAudioTrackPreference(
        preference = episodePreference,
        scope = AudioTrackPreferenceScope.EPISODE,
    )
    seriesPreference != null -> ScopedAudioTrackPreference(
        preference = seriesPreference,
        scope = AudioTrackPreferenceScope.SERIES,
    )
    else -> null
}
