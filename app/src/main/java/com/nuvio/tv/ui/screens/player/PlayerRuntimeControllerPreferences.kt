package com.nuvio.tv.ui.screens.player

import com.nuvio.tv.data.local.AudioTrackPreferenceScope
import com.nuvio.tv.data.local.PersistedAudioTrackPreference
import com.nuvio.tv.data.local.supportsEpisodeAudioPreference
import com.nuvio.tv.data.local.toTrackPreference
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal data class LoadedPlaybackTrackPreference(
    val preference: PlayerRuntimeController.TrackPreference?,
    val audioScope: AudioTrackPreferenceScope,
)

internal suspend fun PlayerRuntimeController.loadTrackPreferenceForPlayback(
    contentId: String,
): LoadedPlaybackTrackPreference {
    val stored = trackPreferenceDataStore.load(contentId)?.toTrackPreference()
    val scopedAudio = trackPreferenceDataStore.audio.load(contentId, currentVideoId)
    val audioSelection = scopedAudio?.preference?.let { preference ->
        PlayerRuntimeController.RememberedTrackSelection(
            language = preference.language,
            name = preference.name,
            trackId = null,
        )
    }
    val preference = when {
        stored != null -> stored.copy(audio = audioSelection ?: stored.audio)
        audioSelection != null -> PlayerRuntimeController.TrackPreference(audio = audioSelection)
        else -> null
    }
    val audioScope = scopedAudio?.scope ?: AudioTrackPreferenceScope.SERIES
    return LoadedPlaybackTrackPreference(preference = preference, audioScope = audioScope)
}

internal fun PlayerRuntimeController.rememberAudioSelection(
    trackIndex: Int,
    preferenceScope: AudioTrackPreferenceScope,
) {
    val selectedTrack = _uiState.value.audioTracks.getOrNull(trackIndex) ?: return
    logSwitchTrace(
        stage = "user-remember-audio",
        message = "trackIndex=$trackIndex scope=$preferenceScope " +
            "lang=${selectedTrack.language} name=${selectedTrack.name} id=${selectedTrack.trackId}",
    )
    val basePreference = currentTrackPreferenceForPersistence()
    hasExplicitAudioPreferenceForPlayback = true
    clearPendingEngineSwitchTrackPreference()
    persistedTrackPreference = null
    rememberedTrackPreference = basePreference.copy(
        audio = PlayerRuntimeController.RememberedTrackSelection(
            language = selectedTrack.language,
            name = selectedTrack.name,
            trackId = null,
        ),
    )
    persistAudioTrackPreference(selectedTrack, preferenceScope)
}

private fun PlayerRuntimeController.persistAudioTrackPreference(
    selectedTrack: TrackInfo,
    requestedScope: AudioTrackPreferenceScope,
) {
    val id = contentId ?: return
    val supportsEpisodeScope = supportsEpisodeAudioPreference(contentType, currentVideoId)
    val effectiveScope = if (supportsEpisodeScope) requestedScope else AudioTrackPreferenceScope.SERIES
    _uiState.update { it.copy(audioPreferenceScope = effectiveScope) }
    scope.launch {
        trackPreferenceDataStore.audio.save(
            contentId = id,
            videoId = currentVideoId,
            scope = effectiveScope,
            preference = PersistedAudioTrackPreference(
                language = selectedTrack.language,
                name = selectedTrack.name,
            ),
        )
    }
}

internal fun PlayerRuntimeController.resetTrackPreferencesForEpisodeChange() {
    rememberedTrackPreference = null
    persistedTrackPreference = null
    hasExplicitAudioPreferenceForPlayback = false
    hasAppliedRememberedAudioSelection = false
    _uiState.update { it.copy(audioPreferenceScope = AudioTrackPreferenceScope.SERIES) }
}
