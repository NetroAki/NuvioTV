package com.nuvio.tv.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nuvio.tv.R
import com.nuvio.tv.ui.theme.NuvioTheme

@Composable
internal fun ServerAssistSettingsContent(
    initialFocusRequester: FocusRequester? = null,
    viewModel: ServerAssistSettingsViewModel =
        androidx.hilt.navigation.compose
            .hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showEndpointDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.status) {
        if (uiState.status == ServerAssistStatus.Available) {
            showEndpointDialog = false
        }
    }

    val status =
        when (val current = uiState.status) {
            ServerAssistStatus.Idle -> {
                if (uiState.enabled) {
                    uiState.endpoint
                } else {
                    stringResource(R.string.server_assist_status_disabled)
                }
            }

            ServerAssistStatus.Checking -> {
                stringResource(R.string.server_assist_status_checking)
            }

            ServerAssistStatus.Available -> {
                uiState.probe
                    ?.let { probe ->
                        stringResource(
                            R.string.server_assist_status_ready,
                            probe.serverVersion,
                            probe.logicalCpus,
                        )
                    }.orEmpty()
            }

            is ServerAssistStatus.Error -> {
                stringResource(
                    R.string.server_assist_status_error,
                    current.message,
                )
            }
        }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(NuvioTheme.spacing.md),
    ) {
        SettingsDetailHeader(
            title = stringResource(R.string.server_assist_title),
            subtitle = stringResource(R.string.server_assist_subtitle),
        )
        SettingsGroupCard(modifier = Modifier.fillMaxWidth()) {
            SettingsActionRow(
                title = stringResource(R.string.server_assist_configure),
                subtitle = stringResource(R.string.server_assist_configure_subtitle),
                value =
                    uiState.endpoint.ifBlank {
                        stringResource(R.string.server_assist_not_configured)
                    },
                onClick = { showEndpointDialog = true },
                modifier = initialFocusRequester?.let { Modifier.focusRequester(it) } ?: Modifier,
            )
            SettingsActionRow(
                title = stringResource(R.string.server_assist_test),
                subtitle = stringResource(R.string.server_assist_test_subtitle),
                value = status,
                onClick = viewModel::testConnection,
                enabled = uiState.endpoint.isNotBlank() && uiState.status != ServerAssistStatus.Checking,
            )
            if (uiState.enabled) {
                SettingsActionRow(
                    title = stringResource(R.string.server_assist_disable),
                    subtitle = stringResource(R.string.server_assist_disable_subtitle),
                    onClick = viewModel::disable,
                )
            }
        }
    }

    if (showEndpointDialog) {
        ServerAssistEndpointDialog(
            currentValue = uiState.endpoint,
            checking = uiState.status == ServerAssistStatus.Checking,
            errorMessage = (uiState.status as? ServerAssistStatus.Error)?.message,
            onConnect = viewModel::saveAndConnect,
            onDismiss = { showEndpointDialog = false },
        )
    }
}
