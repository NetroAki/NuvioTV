package com.nuvio.tv.ui.screens.settings

import android.view.KeyEvent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.Text
import com.nuvio.tv.R
import com.nuvio.tv.ui.components.NuvioDialog
import com.nuvio.tv.ui.theme.NuvioTheme

@Composable
internal fun ServerAssistEndpointDialog(
    currentValue: String,
    checking: Boolean,
    errorMessage: String?,
    onConnect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var value by remember(currentValue) { mutableStateOf(currentValue) }
    var inputFocused by remember { mutableStateOf(false) }
    val inputFocusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current

    NuvioDialog(
        onDismiss = onDismiss,
        title = stringResource(R.string.server_assist_dialog_title),
        subtitle = stringResource(R.string.server_assist_dialog_subtitle),
        width = 700.dp,
    ) {
        Card(
            onClick = { inputFocusRequester.requestFocus() },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .onFocusChanged { inputFocused = it.isFocused || it.hasFocus },
            colors =
                CardDefaults.colors(
                    containerColor = NuvioTheme.colors.BackgroundElevated,
                    focusedContainerColor = NuvioTheme.colors.BackgroundElevated,
                ),
            border =
                CardDefaults.border(
                    border =
                        Border(
                            border = BorderStroke(NuvioTheme.spacing.hairline, NuvioTheme.colors.Border),
                            shape = RoundedCornerShape(10.dp),
                        ),
                    focusedBorder =
                        Border(
                            border = BorderStroke(NuvioTheme.spacing.xxs, NuvioTheme.colors.FocusRing),
                            shape = RoundedCornerShape(10.dp),
                        ),
                ),
            shape = CardDefaults.shape(RoundedCornerShape(10.dp)),
            scale = CardDefaults.scale(focusedScale = 1f),
        ) {
            Box(modifier = Modifier.padding(horizontal = 14.dp, vertical = NuvioTheme.spacing.md)) {
                BasicTextField(
                    value = value,
                    onValueChange = { value = it },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .focusRequester(inputFocusRequester)
                            .onKeyEvent { event ->
                                event.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_DPAD_CENTER &&
                                    event.nativeKeyEvent.action == KeyEvent.ACTION_DOWN
                            },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { keyboard?.hide() }),
                    textStyle =
                        MaterialTheme.typography.bodyMedium.copy(
                            color = NuvioTheme.colors.TextPrimary,
                        ),
                    cursorBrush =
                        SolidColor(
                            if (inputFocused) NuvioTheme.colors.Primary else androidx.compose.ui.graphics.Color.Transparent,
                        ),
                    decorationBox = { content ->
                        if (value.isBlank()) {
                            Text(
                                text = stringResource(R.string.server_assist_dialog_placeholder),
                                style = MaterialTheme.typography.bodyMedium,
                                color = NuvioTheme.colors.TextTertiary,
                            )
                        }
                        content()
                    },
                )
            }
        }
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
                    if (checking) {
                        stringResource(R.string.server_assist_status_checking)
                    } else {
                        stringResource(R.string.server_assist_connect)
                    },
                onClick = { onConnect(value) },
                primary = true,
                enabled = value.isNotBlank() && !checking,
            )
        }
    }
}
