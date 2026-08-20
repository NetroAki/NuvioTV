package com.nuvio.tv.data.local

import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.nuvio.tv.core.profile.ProfileManager
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

@Singleton
class AudioTrackPreferenceDataStore @Inject constructor(
    private val factory: ProfileDataStoreFactory,
    private val profileManager: ProfileManager,
) {
    private companion object {
        const val FEATURE = "track_preference"
        const val SERIES_LANGUAGE = "audio_lang"
        const val SERIES_NAME = "audio_name"
        const val LEGACY_TRACK_ID = "audio_track_id"
        const val EPISODE_LANGUAGE = "episode_audio_lang"
        const val EPISODE_NAME = "episode_audio_name"
    }

    private fun store() = factory.get(profileManager.activeProfileId.value, FEATURE)

    suspend fun save(
        contentId: String,
        videoId: String?,
        scope: AudioTrackPreferenceScope,
        preference: PersistedAudioTrackPreference,
    ) {
        store().edit { values ->
            if (scope == AudioTrackPreferenceScope.EPISODE && !videoId.isNullOrBlank()) {
                values.set(EPISODE_LANGUAGE, videoId, preference.language)
                values.set(EPISODE_NAME, videoId, preference.name)
            } else {
                values.set(SERIES_LANGUAGE, contentId, preference.language)
                values.set(SERIES_NAME, contentId, preference.name)
                values.set(LEGACY_TRACK_ID, contentId, null)
                videoId?.takeIf { it.isNotBlank() }?.let { id ->
                    values.set(EPISODE_LANGUAGE, id, null)
                    values.set(EPISODE_NAME, id, null)
                }
            }
        }
    }

    suspend fun load(contentId: String, videoId: String?): ScopedAudioTrackPreference? {
        val values = store().data.first()
        val episodePreference = videoId
            ?.takeIf { it.isNotBlank() }
            ?.let { id -> values.audioPreference(EPISODE_LANGUAGE, EPISODE_NAME, id) }
        val seriesPreference = values.audioPreference(SERIES_LANGUAGE, SERIES_NAME, contentId)
        return resolveScopedAudioPreference(episodePreference, seriesPreference)
    }

    private fun MutablePreferences.set(field: String, id: String, value: String?) {
        val key = stringPreferencesKey("$field|$id")
        if (value != null) this[key] = value else remove(key)
    }

    private fun androidx.datastore.preferences.core.Preferences.audioPreference(
        languageField: String,
        nameField: String,
        id: String,
    ): PersistedAudioTrackPreference? {
        val preference = PersistedAudioTrackPreference(
            language = this[stringPreferencesKey("$languageField|$id")],
            name = this[stringPreferencesKey("$nameField|$id")],
        )
        return preference.takeIf { it.language != null || it.name != null }
    }
}
