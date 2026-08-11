package com.nuvio.tv.ui.screens.account

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.nuvio.tv.R
import com.nuvio.tv.ui.components.NuvioDialog
import com.nuvio.tv.ui.screens.settings.SettingsDialogActionButton
import com.nuvio.tv.ui.screens.settings.SettingsDialogActionRow
import com.nuvio.tv.ui.theme.NuvioTheme

@Composable
internal fun StremioAccountDialog(
    working: Boolean,
    errorMessage: String?,
    onConnect: (email: String, password: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    NuvioDialog(
        onDismiss = onDismiss,
        title = stringResource(R.string.stremio_account_dialog_title),
        subtitle = stringResource(R.string.stremio_account_dialog_subtitle),
        width = 660.dp,
    ) {
        InputField(
            value = email,
            onValueChange = { email = it },
            placeholder = stringResource(R.string.stremio_account_email_placeholder),
            keyboardType = KeyboardType.Email,
            imeAction = ImeAction.Next,
        )
        InputField(
            value = password,
            onValueChange = { password = it },
            placeholder = stringResource(R.string.stremio_account_password_placeholder),
            keyboardType = KeyboardType.Password,
            isPassword = true,
        )
        if (errorMessage != null) {
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodySmall,
                color = NuvioTheme.colors.Error,
            )
        }
        SettingsDialogActionRow {
            SettingsDialogActionButton(
                text = stringResource(R.string.action_cancel),
                onClick = onDismiss,
            )
            SettingsDialogActionButton(
                text =
                    if (working) {
                        stringResource(R.string.stremio_account_connecting)
                    } else {
                        stringResource(R.string.stremio_account_connect)
                    },
                onClick = { onConnect(email, password) },
                primary = true,
                enabled = email.isNotBlank() && password.isNotEmpty() && !working,
            )
        }
    }
}
