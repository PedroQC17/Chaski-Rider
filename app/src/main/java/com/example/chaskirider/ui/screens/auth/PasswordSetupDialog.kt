package com.example.chaskirider.ui.screens.auth

import com.example.chaskirider.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.example.chaskirider.ui.components.PasswordField
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import com.example.chaskirider.ui.theme.ChaskiRiderTheme

@Preview(name = "Diálogo Configurar Contraseña", showBackground = true)
@Composable
fun PasswordSetupDialogPreview() {
    ChaskiRiderTheme {
        PasswordSetupDialog(
            email = "usuario@example.com",
            isLoading = false,
            error = null,
            message = null,
            onSave = { _, _ -> },
            onDismiss = {}
        )
    }
}

@Composable
fun PasswordSetupDialog(email: String, isLoading: Boolean, error: String?, message: String?,
    onSave: (String, String) -> Unit, onDismiss: () -> Unit) {
    var password by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = { if (!isLoading) onDismiss() },
        title = { Text(stringResource(R.string.text_configurar_contrasena)) },
        text = { Column {
            Text(stringResource(R.string.text_podras_ingresar_con_value_y_esta_contrasena, email))
            PasswordField(password, { password = it }, stringResource(R.string.text_contrasena_minimo_8_caracteres), enabled = !isLoading)
            PasswordField(confirmation, { confirmation = it }, stringResource(R.string.text_confirmar_contrasena),
                enabled = !isLoading, imeAction = ImeAction.Done)
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            message?.let { Text(it) }
        } },
        confirmButton = { TextButton(onClick = { onSave(password, confirmation) },
            enabled = !isLoading && message == null) { Text(stringResource(R.string.text_guardar)) } },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !isLoading) { Text(stringResource(R.string.text_cerrar)) } })
}
