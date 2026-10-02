package com.example.chaskirider.ui.screens.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.chaskirider.R
import com.example.chaskirider.ui.components.*

@Composable
fun PasswordSetupDialog(hasPassword: Boolean, isLoading: Boolean, error: String?, message: String?,
    onSave: (String, String) -> Unit, onDismiss: () -> Unit) {
    var password by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    ChaskiDialog(title = stringResource(if (hasPassword) R.string.password_change else R.string.text_configurar_contrasena),
        onDismiss = { if (!isLoading) onDismiss() }, content = {
            if (message == null) {
                PasswordField(password, { password = it }, stringResource(R.string.registration_new_password), enabled = !isLoading)
                PasswordField(confirmation, { confirmation = it }, stringResource(R.string.text_confirmar_contrasena), enabled = !isLoading, imeAction = ImeAction.Done)
            }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            message?.let { Text(it) }
        }, confirm = {
            Button(onClick = { if (message == null) onSave(password, confirmation) else onDismiss() },
                enabled = !isLoading, shape = RoundedCornerShape(12.dp)) {
                if (isLoading) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                else Text(stringResource(if (message == null) R.string.text_guardar else R.string.text_cerrar))
            }
        }, dismiss = {
            if (message == null) TextButton(onClick = onDismiss, enabled = !isLoading) { Text(stringResource(R.string.text_cerrar)) }
        })
}
