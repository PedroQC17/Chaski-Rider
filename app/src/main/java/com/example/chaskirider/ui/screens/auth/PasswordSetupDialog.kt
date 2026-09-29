package com.example.chaskirider.ui.screens.auth

import com.example.chaskirider.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.text.input.PasswordVisualTransformation
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
    val text_configurar_contrasena = stringResource(R.string.text_configurar_contrasena)
    val text_contrasena_minimo_8_caracteres = stringResource(R.string.text_contrasena_minimo_8_caracteres)
    val text_confirmar_contrasena = stringResource(R.string.text_confirmar_contrasena)
    val text_guardar = stringResource(R.string.text_guardar)
    val text_cerrar = stringResource(R.string.text_cerrar)

    var password by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = { if (!isLoading) onDismiss() },
        title = { Text(text_configurar_contrasena) },
        text = { Column {
            Text(stringResource(R.string.text_podras_ingresar_con_value_y_esta_contrasena, email))
            OutlinedTextField(password, { password = it }, label = { Text(text_contrasena_minimo_8_caracteres) },
                singleLine = true, visualTransformation = PasswordVisualTransformation(), enabled = !isLoading)
            OutlinedTextField(confirmation, { confirmation = it }, label = { Text(text_confirmar_contrasena) },
                singleLine = true, visualTransformation = PasswordVisualTransformation(), enabled = !isLoading)
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            message?.let { Text(it) }
        } },
        confirmButton = { TextButton(onClick = { onSave(password, confirmation) },
            enabled = !isLoading && message == null) { Text(text_guardar) } },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !isLoading) { Text(text_cerrar) } })
}
