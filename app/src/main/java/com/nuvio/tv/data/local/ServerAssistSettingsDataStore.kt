package com.nuvio.tv.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.nuvio.tv.core.serverassist.ServerAssistEndpoint
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.serverAssistDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "server_assist_settings",
)

@Singleton
class ServerAssistSettingsDataStore
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
    ) {
        private val enabledKey = booleanPreferencesKey("enabled")
        private val endpointKey = stringPreferencesKey("endpoint")

        val settings: Flow<ServerAssistSettings> =
            context.serverAssistDataStore.data.map { preferences ->
                ServerAssistSettings(
                    enabled = preferences[enabledKey] ?: false,
                    endpoint = preferences[endpointKey],
                )
            }

        suspend fun save(
            endpoint: String,
            enabled: Boolean,
        ) {
            val normalized = ServerAssistEndpoint.normalize(endpoint).getOrThrow()
            context.serverAssistDataStore.edit { preferences ->
                preferences[endpointKey] = normalized
                preferences[enabledKey] = enabled
            }
        }

        suspend fun disable() {
            context.serverAssistDataStore.edit { preferences ->
                preferences[enabledKey] = false
            }
        }
    }

data class ServerAssistSettings(
    val enabled: Boolean,
    val endpoint: String?,
)
