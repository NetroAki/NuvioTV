@file:OptIn(androidx.tv.material3.ExperimentalTvMaterial3Api::class)

package com.nuvio.tv.ui.screens.player

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.tv.material3.FilterChip
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.nuvio.tv.R
import com.nuvio.tv.data.local.AudioTrackPreferenceScope
import com.nuvio.tv.ui.theme.NuvioTheme

@Composable
internal fun AudioPreferenceScopeSelector(
    selectedScope: AudioTrackPreferenceScope,
    onScopeSelected: (AudioTrackPreferenceScope) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(NuvioTheme.spacing.xs),
        modifier = Modifier.padding(bottom = NuvioTheme.spacing.xs),
    ) {
        Text(
            text = stringResource(R.string.audio_preference_scope_label),
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.72f),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(NuvioTheme.spacing.sm)) {
            ScopeChip(
                label = stringResource(R.string.audio_preference_scope_episode),
                selected = selectedScope == AudioTrackPreferenceScope.EPISODE,
                onClick = { onScopeSelected(AudioTrackPreferenceScope.EPISODE) },
            )
            ScopeChip(
                label = stringResource(R.string.audio_preference_scope_series),
                selected = selectedScope == AudioTrackPreferenceScope.SERIES,
                onClick = { onScopeSelected(AudioTrackPreferenceScope.SERIES) },
            )
        }
    }
}

@Composable
private fun ScopeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(selected = selected, onClick = onClick) {
        Text(text = label)
    }
}
