package com.nuvio.tv.data.local

import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StremioSyncStateStore
    @Inject
    constructor(
        private val factory: ProfileDataStoreFactory,
    ) {
        suspend fun getLibraryKeys(profileId: Int): Set<String> =
            factory
                .get(profileId, FEATURE)
                .data
                .first()[LIBRARY_KEYS]
                .orEmpty()

        suspend fun setLibraryKeys(
            profileId: Int,
            keys: Set<String>,
        ) {
            factory.get(profileId, FEATURE).edit { preferences ->
                preferences[LIBRARY_KEYS] = keys
            }
        }

        private companion object {
            const val FEATURE = "stremio_sync_state"
            val LIBRARY_KEYS = stringSetPreferencesKey("library_keys")
        }
    }
