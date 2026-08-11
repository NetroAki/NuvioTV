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
import com.nuvio.tv.ui.screens.account.StremioAccountDialog
import com.nuvio.tv.ui.theme.NuvioTheme

@Composable
internal fun StremioAccountSettingsContent(
    initialFocusRequester: FocusRequester? = null,
    viewModel: StremioAccountSettingsViewModel =
        androidx.hilt.navigation.compose
            .hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showConnectDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.status) {
        if (uiState.status is StremioSyncStatus.Complete) showConnectDialog = false
    }

    val statusText =
        when (val status = uiState.status) {
            StremioSyncStatus.Idle -> {
                null
            }

            StremioSyncStatus.Working -> {
                stringResource(R.string.stremio_account_syncing)
            }

            is StremioSyncStatus.Complete -> {
                stringResource(R.string.stremio_account_sync_complete, status.importedAddons)
            }

            is StremioSyncStatus.Error -> {
                stringResource(R.string.stremio_account_sync_error, status.message)
            }
        }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(NuvioTheme.spacing.md),
    ) {
        SettingsDetailHeader(
            title = stringResource(R.string.stremio_account_title),
            subtitle = stringResource(R.string.stremio_account_subtitle),
        )
        SettingsGroupCard(modifier = Modifier.fillMaxWidth()) {
            if (uiState.connected) {
                SettingsActionRow(
                    title = stringResource(R.string.stremio_account_sync_now),
                    subtitle = stringResource(R.string.stremio_account_sync_now_subtitle),
                    value = statusText ?: uiState.email,
                    onClick = viewModel::syncNow,
                    enabled = uiState.status != StremioSyncStatus.Working,
                    modifier = initialFocusRequester?.let { Modifier.focusRequester(it) } ?: Modifier,
                )
                SettingsActionRow(
                    title = stringResource(R.string.stremio_account_disconnect),
                    subtitle = stringResource(R.string.stremio_account_disconnect_subtitle),
                    onClick = viewModel::disconnect,
                    enabled = uiState.status != StremioSyncStatus.Working,
                )
            } else {
                SettingsActionRow(
                    title = stringResource(R.string.stremio_account_connect),
                    subtitle = stringResource(R.string.stremio_account_connect_subtitle),
                    value = statusText,
                    onClick = { showConnectDialog = true },
                    modifier = initialFocusRequester?.let { Modifier.focusRequester(it) } ?: Modifier,
                )
            }
        }
    }

    if (showConnectDialog) {
        StremioAccountDialog(
            working = uiState.status == StremioSyncStatus.Working,
            errorMessage = (uiState.status as? StremioSyncStatus.Error)?.message,
            onConnect = viewModel::connect,
            onDismiss = { showConnectDialog = false },
        )
    }
}
