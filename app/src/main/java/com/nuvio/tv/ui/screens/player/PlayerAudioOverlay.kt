package com.nuvio.tv.ui.screens.player

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.nuvio.tv.data.local.supportsEpisodeAudioPreference

@Composable
internal fun PlayerAudioOverlay(
    uiState: PlayerUiState,
    onEvent: (PlayerEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    AudioSelectionOverlay(
        visible = uiState.showAudioOverlay,
        tracks = uiState.audioTracks,
        selectedIndex = uiState.selectedAudioTrackIndex,
        audioDelayMs = uiState.audioDelayMs,
        audioAmplificationDb = uiState.audioAmplificationDb,
        isAmplificationAvailable = uiState.isAudioAmplificationAvailable,
        persistAmplification = uiState.persistAudioAmplification,
        centerMixLevelDb = uiState.centerMixLevelDb,
        isCenterMixAvailable = uiState.isCenterMixAvailable,
        preferenceScope = uiState.audioPreferenceScope,
        showPreferenceScope = supportsEpisodeAudioPreference(
            contentType = uiState.contentType,
            videoId = uiState.currentVideoId,
        ),
        onTrackSelected = { index, scope ->
            onEvent(PlayerEvent.OnSelectAudioTrack(index, scope))
        },
        onAudioDelayChange = { onEvent(PlayerEvent.OnSetAudioDelayMs(it)) },
        onAmplificationChange = { onEvent(PlayerEvent.OnSetAudioAmplificationDb(it)) },
        onPersistAmplificationChange = {
            onEvent(PlayerEvent.OnSetPersistAudioAmplification(it))
        },
        onCenterMixLevelChange = { onEvent(PlayerEvent.OnSetCenterMixLevelDb(it)) },
        onDismiss = { onEvent(PlayerEvent.OnDismissTransientOverlay) },
        modifier = modifier,
    )
}
