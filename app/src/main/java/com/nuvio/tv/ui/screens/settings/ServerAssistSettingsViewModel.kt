package com.nuvio.tv.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nuvio.tv.data.local.ServerAssistSettingsDataStore
import com.nuvio.tv.data.remote.ServerAssistClient
import com.nuvio.tv.data.remote.ServerAssistProbe
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ServerAssistSettingsViewModel
    @Inject
    constructor(
        private val settingsDataStore: ServerAssistSettingsDataStore,
        private val client: ServerAssistClient,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(ServerAssistSettingsUiState())
        val uiState: StateFlow<ServerAssistSettingsUiState> = _uiState.asStateFlow()
        private var probeJob: Job? = null

        init {
            viewModelScope.launch {
                settingsDataStore.settings.collect { settings ->
                    _uiState.update {
                        it.copy(
                            enabled = settings.enabled,
                            endpoint = settings.endpoint.orEmpty(),
                        )
                    }
                }
            }
        }

        fun saveAndConnect(endpoint: String) {
            probe(endpoint, saveOnSuccess = true)
        }

        fun testConnection() {
            val endpoint = uiState.value.endpoint
            if (endpoint.isBlank()) {
                _uiState.update { it.copy(status = ServerAssistStatus.Error("Configure a server endpoint first")) }
                return
            }
            probe(endpoint, saveOnSuccess = false)
        }

        fun disable() {
            probeJob?.cancel()
            viewModelScope.launch {
                settingsDataStore.disable()
                _uiState.update { it.copy(status = ServerAssistStatus.Idle, probe = null) }
            }
        }

        private fun probe(
            endpoint: String,
            saveOnSuccess: Boolean,
        ) {
            probeJob?.cancel()
            probeJob =
                viewModelScope.launch {
                    _uiState.update { it.copy(status = ServerAssistStatus.Checking) }
                    client
                        .probe(endpoint)
                        .onSuccess { probe ->
                            if (saveOnSuccess) {
                                settingsDataStore.save(probe.endpoint, enabled = true)
                            }
                            _uiState.update {
                                it.copy(
                                    endpoint = probe.endpoint,
                                    enabled = if (saveOnSuccess) true else it.enabled,
                                    status = ServerAssistStatus.Available,
                                    probe = probe,
                                )
                            }
                        }.onFailure { error ->
                            _uiState.update {
                                it.copy(
                                    status =
                                        ServerAssistStatus.Error(
                                            error.message ?: "Server connection failed",
                                        ),
                                    probe = null,
                                )
                            }
                        }
                }
        }
    }

data class ServerAssistSettingsUiState(
    val enabled: Boolean = false,
    val endpoint: String = "",
    val status: ServerAssistStatus = ServerAssistStatus.Idle,
    val probe: ServerAssistProbe? = null,
)

sealed interface ServerAssistStatus {
    data object Idle : ServerAssistStatus

    data object Checking : ServerAssistStatus

    data object Available : ServerAssistStatus

    data class Error(
        val message: String,
    ) : ServerAssistStatus
}
