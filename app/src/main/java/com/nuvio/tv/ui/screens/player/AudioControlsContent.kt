@file:OptIn(androidx.tv.material3.ExperimentalTvMaterial3Api::class)

package com.nuvio.tv.ui.screens.player

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.nuvio.tv.R
import com.nuvio.tv.ui.theme.NuvioTheme
import java.util.Locale

@Composable
internal fun AudioControlsContent(
    audioDelayMs: Int,
    audioAmplificationDb: Int,
    isAmplificationAvailable: Boolean,
    centerMixLevelDb: Int,
    isCenterMixAvailable: Boolean,
    persistAmplification: Boolean,
    delayMinusFocusRequester: FocusRequester,
    delayPlusFocusRequester: FocusRequester,
    ampMinusFocusRequester: FocusRequester,
    ampPlusFocusRequester: FocusRequester,
    centerMinusFocusRequester: FocusRequester,
    centerPlusFocusRequester: FocusRequester,
    persistFocusRequester: FocusRequester,
    leftFocusRequester: FocusRequester,
    onAudioDelayChange: (Int) -> Unit,
    onAmplificationChange: (Int, AudioControlFocusTarget) -> Unit,
    onCenterMixLevelChange: (Int) -> Unit,
    onPersistAmplificationChange: (Boolean) -> Unit
) {
    val currentDelayMs = audioDelayMs.coerceIn(AUDIO_DELAY_MIN_MS, AUDIO_DELAY_MAX_MS)
    val canDecreaseDelay = currentDelayMs > AUDIO_DELAY_MIN_MS
    val canIncreaseDelay = currentDelayMs < AUDIO_DELAY_MAX_MS
    val currentDb = audioAmplificationDb.coerceIn(AUDIO_AMPLIFICATION_MIN_DB, AUDIO_AMPLIFICATION_MAX_DB)
    val canDecreaseAmp = isAmplificationAvailable && currentDb > AUDIO_AMPLIFICATION_MIN_DB
    val canIncreaseAmp = isAmplificationAvailable && currentDb < AUDIO_AMPLIFICATION_MAX_DB
    val currentCenterMixDb = centerMixLevelDb.coerceIn(CENTER_MIX_LEVEL_MIN_DB, CENTER_MIX_LEVEL_MAX_DB)
    val canDecreaseCenterMix = isCenterMixAvailable && currentCenterMixDb > CENTER_MIX_LEVEL_MIN_DB
    val canIncreaseCenterMix = isCenterMixAvailable && currentCenterMixDb < CENTER_MIX_LEVEL_MAX_DB

    val firstDelayFocusRequester = if (canDecreaseDelay) {
        delayMinusFocusRequester
    } else {
        delayPlusFocusRequester
    }
    val firstAmpFocusRequester = when {
        canDecreaseAmp -> ampMinusFocusRequester
        canIncreaseAmp -> ampPlusFocusRequester
        canDecreaseCenterMix -> centerMinusFocusRequester
        canIncreaseCenterMix -> centerPlusFocusRequester
        else -> persistFocusRequester
    }
    val firstCenterFocusRequester = when {
        canDecreaseCenterMix -> centerMinusFocusRequester
        canIncreaseCenterMix -> centerPlusFocusRequester
        else -> persistFocusRequester
    }
    val delayPlusLeftFocusRequester = if (canDecreaseDelay) {
        delayMinusFocusRequester
    } else {
        leftFocusRequester
    }
    val ampPlusLeftFocusRequester = if (canDecreaseAmp) {
        ampMinusFocusRequester
    } else {
        leftFocusRequester
    }
    val centerPlusLeftFocusRequester = if (canDecreaseCenterMix) {
        centerMinusFocusRequester
    } else {
        leftFocusRequester
    }
    val persistLeftFocusRequester = when {
        canIncreaseCenterMix -> centerPlusFocusRequester
        canDecreaseCenterMix -> centerMinusFocusRequester
        canIncreaseAmp -> ampPlusFocusRequester
        canDecreaseAmp -> ampMinusFocusRequester
        else -> leftFocusRequester
    }
    val centerHelperText = if (isCenterMixAvailable) {
        stringResource(R.string.audio_center_mix_help)
    } else {
        stringResource(R.string.audio_center_mix_unavailable)
    }

    val amplificationHelperText = when {
        !isAmplificationAvailable -> stringResource(R.string.audio_mix_unavailable)
        persistAmplification -> stringResource(
            R.string.audio_mix_range_saved,
            AUDIO_AMPLIFICATION_MIN_DB,
            AUDIO_AMPLIFICATION_MAX_DB
        )
        else -> stringResource(
            R.string.audio_mix_range,
            AUDIO_AMPLIFICATION_MIN_DB,
            AUDIO_AMPLIFICATION_MAX_DB
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = NuvioTheme.spacing.xs, bottom = NuvioTheme.spacing.sm),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        AdjustmentSection(
            title = stringResource(R.string.audio_delay_label),
            valueText = formatAudioDelay(currentDelayMs),
            helperText = stringResource(
                R.string.audio_delay_range,
                AUDIO_DELAY_MIN_MS / 1000f,
                AUDIO_DELAY_MAX_MS / 1000f
            ),
            canDecrease = canDecreaseDelay,
            canIncrease = canIncreaseDelay,
            minusFocusRequester = delayMinusFocusRequester,
            plusFocusRequester = delayPlusFocusRequester,
            minusLeftFocusRequester = leftFocusRequester,
            plusLeftFocusRequester = delayPlusLeftFocusRequester,
            upFocusRequester = null,
            downFocusRequester = firstAmpFocusRequester,
            onDecrease = {
                val nextDelayMs = currentDelayMs - AUDIO_DELAY_STEP_MS
                onAudioDelayChange(nextDelayMs)
                if (nextDelayMs <= AUDIO_DELAY_MIN_MS && canIncreaseDelay) {
                    runCatching { delayPlusFocusRequester.requestFocus() }
                }
            },
            onIncrease = {
                val nextDelayMs = currentDelayMs + AUDIO_DELAY_STEP_MS
                onAudioDelayChange(nextDelayMs)
                if (nextDelayMs >= AUDIO_DELAY_MAX_MS && canDecreaseDelay) {
                    runCatching { delayMinusFocusRequester.requestFocus() }
                }
            }
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            AdjustmentSection(
                title = stringResource(R.string.audio_mix_label),
                valueText = stringResource(R.string.audio_mix_value_db, currentDb),
                helperText = amplificationHelperText,
                canDecrease = canDecreaseAmp,
                canIncrease = canIncreaseAmp,
                minusFocusRequester = ampMinusFocusRequester,
                plusFocusRequester = ampPlusFocusRequester,
                minusLeftFocusRequester = leftFocusRequester,
                plusLeftFocusRequester = ampPlusLeftFocusRequester,
                upFocusRequester = firstDelayFocusRequester,
                downFocusRequester = firstCenterFocusRequester,
                onDecrease = {
                    val nextDb = currentDb - 1
                    val target = if (nextDb <= AUDIO_AMPLIFICATION_MIN_DB && canIncreaseAmp) {
                        AudioControlFocusTarget.AmpPlus
                    } else {
                        AudioControlFocusTarget.AmpMinus
                    }
                    onAmplificationChange(nextDb, target)
                    if (nextDb <= AUDIO_AMPLIFICATION_MIN_DB && canIncreaseAmp) {
                        runCatching { ampPlusFocusRequester.requestFocus() }
                    }
                },
                onIncrease = {
                    val nextDb = currentDb + 1
                    val target = if (nextDb >= AUDIO_AMPLIFICATION_MAX_DB && canDecreaseAmp) {
                        AudioControlFocusTarget.AmpMinus
                    } else {
                        AudioControlFocusTarget.AmpPlus
                    }
                    onAmplificationChange(nextDb, target)
                    if (nextDb >= AUDIO_AMPLIFICATION_MAX_DB && canDecreaseAmp) {
                        runCatching { ampMinusFocusRequester.requestFocus() }
                    }
                }
            )

            AdjustmentSection(
                title = stringResource(R.string.audio_center_mix_label),
                valueText = stringResource(R.string.audio_center_mix_value_db, currentCenterMixDb),
                helperText = centerHelperText,
                canDecrease = canDecreaseCenterMix,
                canIncrease = canIncreaseCenterMix,
                minusFocusRequester = centerMinusFocusRequester,
                plusFocusRequester = centerPlusFocusRequester,
                minusLeftFocusRequester = leftFocusRequester,
                plusLeftFocusRequester = centerPlusLeftFocusRequester,
                upFocusRequester = firstAmpFocusRequester,
                downFocusRequester = persistFocusRequester,
                onDecrease = {
                    val nextDb = currentCenterMixDb - 1
                    onCenterMixLevelChange(nextDb)
                    if (nextDb <= CENTER_MIX_LEVEL_MIN_DB && canIncreaseCenterMix) {
                        runCatching { centerPlusFocusRequester.requestFocus() }
                    }
                },
                onIncrease = {
                    val nextDb = currentCenterMixDb + 1
                    onCenterMixLevelChange(nextDb)
                    if (nextDb >= CENTER_MIX_LEVEL_MAX_DB && canDecreaseCenterMix) {
                        runCatching { centerMinusFocusRequester.requestFocus() }
                    }
                }
            )

            Card(
                onClick = { onPersistAmplificationChange(!persistAmplification) },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(persistFocusRequester)
                    .focusProperties {
                        left = persistLeftFocusRequester
                        up = firstCenterFocusRequester
                    },
                colors = CardDefaults.colors(
                    containerColor = if (persistAmplification) NuvioTheme.colors.Secondary else Color.Transparent,
                    focusedContainerColor = if (persistAmplification) NuvioTheme.colors.Secondary else Color.Transparent
                ),
                shape = CardDefaults.shape(RoundedCornerShape(NuvioTheme.radii.md)),
                border = CardDefaults.border(
                    border = Border(
                        border = BorderStroke(NuvioTheme.spacing.xxs, Color.Transparent),
                        shape = RoundedCornerShape(NuvioTheme.radii.md)
                    ),
                    focusedBorder = Border(
                        border = BorderStroke(NuvioTheme.spacing.xxs, NuvioTheme.colors.FocusRing),
                        shape = RoundedCornerShape(NuvioTheme.radii.md)
                    )
                ),
                scale = CardDefaults.scale(focusedScale = 1f, pressedScale = 1f)
            ) {
                Text(
                    text = if (persistAmplification) {
                        stringResource(R.string.audio_mix_persist_on)
                    } else {
                        stringResource(R.string.audio_mix_persist_off)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (persistAmplification) NuvioTheme.colors.OnSecondary else Color.White,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                )
            }
        }
    }
}

@Composable
private fun AdjustmentSection(
    title: String,
    valueText: String,
    helperText: String,
    canDecrease: Boolean,
    canIncrease: Boolean,
    minusFocusRequester: FocusRequester,
    plusFocusRequester: FocusRequester,
    minusLeftFocusRequester: FocusRequester,
    plusLeftFocusRequester: FocusRequester,
    upFocusRequester: FocusRequester?,
    downFocusRequester: FocusRequester?,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(NuvioTheme.spacing.xs)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.92f)
        )

        Text(
            text = valueText,
            style = MaterialTheme.typography.titleMedium,
            color = Color.White
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            StepCard(
                icon = Icons.Default.Remove,
                enabled = canDecrease,
                focusRequester = minusFocusRequester,
                leftFocusRequester = minusLeftFocusRequester,
                rightFocusRequester = if (canIncrease) plusFocusRequester else FocusRequester.Default,
                upFocusRequester = upFocusRequester,
                downFocusRequester = downFocusRequester,
                onClick = onDecrease
            )
            StepCard(
                icon = Icons.Default.Add,
                enabled = canIncrease,
                focusRequester = plusFocusRequester,
                leftFocusRequester = plusLeftFocusRequester,
                upFocusRequester = upFocusRequester,
                downFocusRequester = downFocusRequester,
                onClick = onIncrease
            )
        }

        Text(
            text = helperText,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.66f)
        )
    }
}

@Composable
private fun StepCard(
    icon: ImageVector,
    enabled: Boolean,
    focusRequester: FocusRequester,
    leftFocusRequester: FocusRequester,
    rightFocusRequester: FocusRequester = FocusRequester.Default,
    upFocusRequester: FocusRequester? = null,
    downFocusRequester: FocusRequester? = null,
    onClick: () -> Unit
) {
    Card(
        onClick = {
            if (enabled) {
                onClick()
            }
        },
        modifier = Modifier
            .width(NuvioTheme.spacing.huge)
            .focusRequester(focusRequester)
            .focusProperties {
                canFocus = enabled
                left = leftFocusRequester
                right = rightFocusRequester
                upFocusRequester?.let { up = it }
                downFocusRequester?.let { down = it }
            },
        colors = CardDefaults.colors(
            containerColor = if (enabled) Color.Transparent else Color.White.copy(alpha = 0.06f),
            focusedContainerColor = if (enabled) Color.Transparent else Color.White.copy(alpha = 0.06f)
        ),
        shape = CardDefaults.shape(RoundedCornerShape(NuvioTheme.radii.md)),
        border = CardDefaults.border(
            border = Border(
                border = BorderStroke(NuvioTheme.spacing.xxs, if (enabled) Color.White.copy(alpha = 0.18f) else Color.Transparent),
                shape = RoundedCornerShape(NuvioTheme.radii.md)
            ),
            focusedBorder = Border(
                border = BorderStroke(NuvioTheme.spacing.xxs, if (enabled) NuvioTheme.colors.FocusRing else Color.Transparent),
                shape = RoundedCornerShape(NuvioTheme.radii.md)
            )
        ),
        scale = CardDefaults.scale(focusedScale = 1f, pressedScale = 1f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (enabled) Color.White else Color.White.copy(alpha = 0.35f)
            )
        }
    }
}

internal enum class AudioControlFocusTarget {
    DelayMinus,
    DelayPlus,
    AmpMinus,
    AmpPlus,
    CenterMinus,
    CenterPlus,
    Persist
}

private fun formatAudioDelay(delayMs: Int): String {
    return if (delayMs == 0) {
        String.format(Locale.US, "%.3fs", 0f)
    } else {
        String.format(Locale.US, "%+.3fs", delayMs / 1000f)
    }
}
