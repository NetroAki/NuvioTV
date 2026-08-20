@file:OptIn(ExperimentalTvMaterial3Api::class)

package com.nuvio.tv.ui.screens.player

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.nuvio.tv.R
import com.nuvio.tv.data.local.AudioTrackPreferenceScope
import com.nuvio.tv.ui.screens.detail.requestFocusAfterFrames
import com.nuvio.tv.ui.theme.NuvioTheme
import kotlinx.coroutines.delay

@Composable
internal fun AudioSelectionOverlay(
    visible: Boolean,
    tracks: List<TrackInfo>,
    selectedIndex: Int,
    audioDelayMs: Int,
    audioAmplificationDb: Int,
    isAmplificationAvailable: Boolean,
    centerMixLevelDb: Int,
    isCenterMixAvailable: Boolean,
    persistAmplification: Boolean,
    preferenceScope: AudioTrackPreferenceScope,
    showPreferenceScope: Boolean,
    onTrackSelected: (Int, AudioTrackPreferenceScope) -> Unit,
    onAudioDelayChange: (Int) -> Unit,
    onAmplificationChange: (Int) -> Unit,
    onCenterMixLevelChange: (Int) -> Unit,
    onPersistAmplificationChange: (Boolean) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tracksFocusRequester = remember { FocusRequester() }
    val delayMinusFocusRequester = remember { FocusRequester() }
    val delayPlusFocusRequester = remember { FocusRequester() }
    val ampMinusFocusRequester = remember { FocusRequester() }
    val ampPlusFocusRequester = remember { FocusRequester() }
    val centerMinusFocusRequester = remember { FocusRequester() }
    val centerPlusFocusRequester = remember { FocusRequester() }
    val persistFocusRequester = remember { FocusRequester() }
    val listState = rememberLazyListState()
    val currentDelayMs = audioDelayMs.coerceIn(AUDIO_DELAY_MIN_MS, AUDIO_DELAY_MAX_MS)
    val canDecreaseDelay = currentDelayMs > AUDIO_DELAY_MIN_MS
    val canIncreaseDelay = currentDelayMs < AUDIO_DELAY_MAX_MS
    val currentDb = audioAmplificationDb.coerceIn(AUDIO_AMPLIFICATION_MIN_DB, AUDIO_AMPLIFICATION_MAX_DB)
    val canDecreaseAmp = isAmplificationAvailable && currentDb > AUDIO_AMPLIFICATION_MIN_DB
    val canIncreaseAmp = isAmplificationAvailable && currentDb < AUDIO_AMPLIFICATION_MAX_DB
    val currentCenterMixDb = centerMixLevelDb.coerceIn(CENTER_MIX_LEVEL_MIN_DB, CENTER_MIX_LEVEL_MAX_DB)
    val canDecreaseCenterMix = isCenterMixAvailable && currentCenterMixDb > CENTER_MIX_LEVEL_MIN_DB
    val canIncreaseCenterMix = isCenterMixAvailable && currentCenterMixDb < CENTER_MIX_LEVEL_MAX_DB

    var lastFocusedAudioIndex by rememberSaveable { mutableStateOf<Int?>(null) }
    var selectedPreferenceScope by rememberSaveable { mutableStateOf(preferenceScope) }
    var pendingControlFocusTarget by rememberSaveable {
        mutableStateOf<AudioControlFocusTarget?>(null)
    }

    LaunchedEffect(visible, tracks, selectedIndex) {
        if (!visible) return@LaunchedEffect
        selectedPreferenceScope = preferenceScope

        if (tracks.isNotEmpty()) {
            val selectedListIndex = tracks.indexOfFirst { it.index == selectedIndex }.takeIf { it >= 0 } ?: 0
            listState.scrollToItem(selectedListIndex)
            delay(120)
            runCatching { tracksFocusRequester.requestFocus() }
        } else {
            delay(120)
            val initialControlsFocusRequester = when {
                canDecreaseDelay -> delayMinusFocusRequester
                canIncreaseDelay -> delayPlusFocusRequester
                canDecreaseAmp -> ampMinusFocusRequester
                canIncreaseAmp -> ampPlusFocusRequester
                canDecreaseCenterMix -> centerMinusFocusRequester
                canIncreaseCenterMix -> centerPlusFocusRequester
                else -> persistFocusRequester
            }
            runCatching { initialControlsFocusRequester.requestFocus() }
        }
    }

    LaunchedEffect(visible, pendingControlFocusTarget, audioAmplificationDb, isAmplificationAvailable) {
        if (!visible) return@LaunchedEffect
        val target = pendingControlFocusTarget ?: return@LaunchedEffect
        val targetCanFocus = when (target) {
            AudioControlFocusTarget.DelayMinus -> canDecreaseDelay
            AudioControlFocusTarget.DelayPlus -> canIncreaseDelay
            AudioControlFocusTarget.AmpMinus -> canDecreaseAmp
            AudioControlFocusTarget.AmpPlus -> canIncreaseAmp
            AudioControlFocusTarget.CenterMinus -> canDecreaseCenterMix
            AudioControlFocusTarget.CenterPlus -> canIncreaseCenterMix
            AudioControlFocusTarget.Persist -> true
        }
        if (!targetCanFocus) return@LaunchedEffect
        val controlFocusRequester = when (target) {
            AudioControlFocusTarget.DelayMinus -> delayMinusFocusRequester
            AudioControlFocusTarget.DelayPlus -> delayPlusFocusRequester
            AudioControlFocusTarget.AmpMinus -> ampMinusFocusRequester
            AudioControlFocusTarget.AmpPlus -> ampPlusFocusRequester
            AudioControlFocusTarget.CenterMinus -> centerMinusFocusRequester
            AudioControlFocusTarget.CenterPlus -> centerPlusFocusRequester
            AudioControlFocusTarget.Persist -> persistFocusRequester
        }
        delay(80)
        controlFocusRequester.requestFocusAfterFrames(frames = 2)
        delay(200)
        runCatching { controlFocusRequester.requestFocus() }
        pendingControlFocusTarget = null
    }

    PlayerOverlayScaffold(
        visible = visible,
        onDismiss = onDismiss,
        modifier = modifier,
        captureKeys = false,
        contentPadding = PaddingValues(start = 44.dp, end = 44.dp, top = 28.dp, bottom = 64.dp)
    ) {
        Column(
            modifier = Modifier
                .width(724.dp)
                .align(Alignment.BottomStart)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.Bottom
        ) {
            Text(
                text = stringResource(R.string.audio_dialog_title),
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                modifier = Modifier.padding(bottom = NuvioTheme.spacing.sm)
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(NuvioTheme.spacing.md),
                verticalAlignment = Alignment.Top,
                modifier = Modifier.padding(bottom = NuvioTheme.spacing.sm)
            ) {
                Column(modifier = Modifier.width(444.dp)) {
                    if (showPreferenceScope) {
                        AudioPreferenceScopeSelector(
                            selectedScope = selectedPreferenceScope,
                            onScopeSelected = { selectedPreferenceScope = it },
                        )
                    }
                    AudioTracksContent(
                        tracks = tracks,
                        selectedIndex = selectedIndex,
                        listState = listState,
                        initialFocusRequester = tracksFocusRequester,
                        rightFocusRequester = when {
                            canDecreaseDelay -> delayMinusFocusRequester
                            canIncreaseDelay -> delayPlusFocusRequester
                            canDecreaseAmp -> ampMinusFocusRequester
                            canIncreaseAmp -> ampPlusFocusRequester
                            canDecreaseCenterMix -> centerMinusFocusRequester
                            canIncreaseCenterMix -> centerPlusFocusRequester
                            else -> persistFocusRequester
                        },
                        onTrackFocused = { lastFocusedAudioIndex = it },
                        onTrackSelected = { index -> onTrackSelected(index, selectedPreferenceScope) }
                    )
                }
                Column(modifier = Modifier.width(268.dp)) {
                    AudioControlsContent(
                        audioDelayMs = audioDelayMs,
                        audioAmplificationDb = audioAmplificationDb,
                        isAmplificationAvailable = isAmplificationAvailable,
                        centerMixLevelDb = centerMixLevelDb,
                        isCenterMixAvailable = isCenterMixAvailable,
                        persistAmplification = persistAmplification,
                        delayMinusFocusRequester = delayMinusFocusRequester,
                        delayPlusFocusRequester = delayPlusFocusRequester,
                        ampMinusFocusRequester = ampMinusFocusRequester,
                        ampPlusFocusRequester = ampPlusFocusRequester,
                        centerMinusFocusRequester = centerMinusFocusRequester,
                        centerPlusFocusRequester = centerPlusFocusRequester,
                        persistFocusRequester = persistFocusRequester,
                        leftFocusRequester = tracksFocusRequester,
                        onAudioDelayChange = onAudioDelayChange,
                        onAmplificationChange = { nextDb, focusTarget ->
                            pendingControlFocusTarget = focusTarget
                            onAmplificationChange(nextDb)
                        },
                        onCenterMixLevelChange = onCenterMixLevelChange,
                        onPersistAmplificationChange = onPersistAmplificationChange
                    )
                }
            }
        }
    }
}
