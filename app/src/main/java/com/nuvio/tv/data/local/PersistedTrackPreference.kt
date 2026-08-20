package com.nuvio.tv.data.local

import com.nuvio.tv.ui.screens.player.PlayerRuntimeController

data class PersistedTrackPreference(
    val subtitleType: String?,
    val subtitleLanguage: String?,
    val subtitleName: String?,
    val subtitleTrackId: String?,
    val subtitleIsForced: Boolean? = null,
    val addonSubtitleId: String?,
    val addonSubtitleUrl: String?,
    val addonSubtitleAddonName: String?,
    val audioLanguage: String?,
    val audioName: String?,
    val audioTrackId: String?,
)

internal fun PersistedTrackPreference.toTrackPreference(): PlayerRuntimeController.TrackPreference? {
    val audio = if (audioLanguage != null || audioName != null || audioTrackId != null) {
        PlayerRuntimeController.RememberedTrackSelection(
            language = audioLanguage,
            name = audioName,
            trackId = audioTrackId,
        )
    } else {
        null
    }

    val subtitle = when (subtitleType) {
        "INTERNAL" -> PlayerRuntimeController.RememberedSubtitleSelection.Internal(
            track = PlayerRuntimeController.RememberedTrackSelection(
                language = subtitleLanguage,
                name = subtitleName,
                trackId = subtitleTrackId,
                isForcedHint = subtitleIsForced,
            ),
        )
        "ADDON" -> PlayerRuntimeController.RememberedSubtitleSelection.Addon(
            id = addonSubtitleId ?: "",
            url = addonSubtitleUrl ?: "",
            language = subtitleLanguage ?: "",
            addonName = addonSubtitleAddonName ?: "",
        )
        "DISABLED" -> PlayerRuntimeController.RememberedSubtitleSelection.Disabled
        else -> null
    }

    if (audio == null && subtitle == null) return null
    return PlayerRuntimeController.TrackPreference(audio = audio, subtitle = subtitle)
}
