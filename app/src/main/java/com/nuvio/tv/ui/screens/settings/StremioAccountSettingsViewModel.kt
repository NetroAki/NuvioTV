package com.nuvio.tv.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nuvio.tv.core.sync.stremio.StremioAddonImporter
import com.nuvio.tv.data.local.StremioSessionStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StremioAccountSettingsViewModel
    @Inject
    constructor(
        private val sessionStore: StremioSessionStore,
        private val importer: StremioAddonImporter,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(StremioAccountSettingsUiState())
        val uiState: StateFlow<StremioAccountSettingsUiState> = _uiState.asStateFlow()

        init {
            viewModelScope.launch {
                sessionStore.state.collect { session ->
                    _uiState.update { current ->
                        current.copy(
                            connected = session.connected,
                            email = session.email,
                            status = if (session.connected) current.status else StremioSyncStatus.Idle,
                        )
                    }
                }
            }
        }

        fun connect(
            email: String,
            password: String,
        ) {
            if (uiState.value.status == StremioSyncStatus.Working) return
            viewModelScope.launch {
                _uiState.update { it.copy(status = StremioSyncStatus.Working) }
                importer
                    .connectAndImport(email, password)
                    .onSuccess { result ->
                        _uiState.update {
                            it.copy(status = StremioSyncStatus.Complete(result.importedAddons))
                        }
                    }.onFailure { error ->
                        _uiState.update {
                            it.copy(status = StremioSyncStatus.Error(error.userFacingMessage()))
                        }
                    }
            }
        }

        fun syncNow() {
            if (uiState.value.status == StremioSyncStatus.Working) return
            viewModelScope.launch {
                _uiState.update { it.copy(status = StremioSyncStatus.Working) }
                importer
                    .refresh()
                    .onSuccess { result ->
                        _uiState.update {
                            it.copy(status = StremioSyncStatus.Complete(result.importedAddons))
                        }
                    }.onFailure { error ->
                        _uiState.update {
                            it.copy(status = StremioSyncStatus.Error(error.userFacingMessage()))
                        }
                    }
            }
        }

        fun disconnect() {
            if (uiState.value.status == StremioSyncStatus.Working) return
            viewModelScope.launch {
                _uiState.update { it.copy(status = StremioSyncStatus.Working) }
                importer
                    .disconnect()
                    .onFailure { error ->
                        _uiState.update {
                            it.copy(status = StremioSyncStatus.Error(error.userFacingMessage()))
                        }
                    }
            }
        }

        private fun Throwable.userFacingMessage(): String = message?.takeIf { it.isNotBlank() } ?: "Stremio account request failed"
    }

data class StremioAccountSettingsUiState(
    val connected: Boolean = false,
    val email: String? = null,
    val status: StremioSyncStatus = StremioSyncStatus.Idle,
)

sealed interface StremioSyncStatus {
    data object Idle : StremioSyncStatus

    data object Working : StremioSyncStatus

    data class Complete(
        val importedAddons: Int,
    ) : StremioSyncStatus

    data class Error(
        val message: String,
    ) : StremioSyncStatus
}
