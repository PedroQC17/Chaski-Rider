package com.example.chaskirider.ui.screens.auth

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
    var password by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = { if (!isLoading) onDismiss() },
        title = { Text("Configurar contraseña") },
        text = { Column {
            Text("Podrás ingresar con $email y esta contraseña.")
            OutlinedTextField(password, { password = it }, label = { Text("Contraseña (mínimo 8 caracteres)") },
                singleLine = true, visualTransformation = PasswordVisualTransformation(), enabled = !isLoading)
            OutlinedTextField(confirmation, { confirmation = it }, label = { Text("Confirmar contraseña") },
                singleLine = true, visualTransformation = PasswordVisualTransformation(), enabled = !isLoading)
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            message?.let { Text(it) }
        } },
        confirmButton = { TextButton(onClick = { onSave(password, confirmation) },
            enabled = !isLoading && message == null) { Text("Guardar") } },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !isLoading) { Text("Cerrar") } })
}
