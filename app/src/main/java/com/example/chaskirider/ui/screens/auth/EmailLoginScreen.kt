package com.example.chaskirider.ui.screens.auth

import com.example.chaskirider.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import com.example.chaskirider.ui.components.EyeIcon
import com.example.chaskirider.ui.components.EyeOffIcon
import com.example.chaskirider.ui.theme.BackgroundLight
import com.example.chaskirider.ui.theme.BorderLight
import com.example.chaskirider.ui.theme.ChaskiRiderTheme
import com.example.chaskirider.ui.theme.Orange
import com.example.chaskirider.ui.theme.TextDark
import com.example.chaskirider.ui.theme.TextMuted

@Preview(name = "Ingreso con Correo", showBackground = true, showSystemUi = true)
@Composable
fun EmailLoginScreenPreview() {
    ChaskiRiderTheme {
        EmailLoginScreen()
    }
}

@Composable
fun EmailLoginScreen(
    onNavigateBack: () -> Unit = {},
    onLoginClick: (email: String, pass: String) -> Unit = { _, _ -> },
    onPasswordResetRequest: (email: String) -> Unit = {},
    isLoading: Boolean = false,
    errorMessage: String? = null,
    successMessage: String? = null
) {
    val text_volver = stringResource(R.string.text_volver)
    val text_ingresar_con_correo = stringResource(R.string.text_ingresar_con_correo)
    val text_ingresa_tus_credenciales_registradas_para_acceder_a = stringResource(R.string.text_ingresa_tus_credenciales_registradas_para_acceder_a)
    val text_correo_electronico = stringResource(R.string.text_correo_electronico)
    val text_correo = stringResource(R.string.text_correo)
    val text_contrasena = stringResource(R.string.text_contrasena)
    val text_olvidaste_tu_contrasena = stringResource(R.string.text_olvidaste_tu_contrasena)
    val text_iniciar_sesion = stringResource(R.string.text_iniciar_sesion)
    val text_recuperar_contrasena = stringResource(R.string.text_recuperar_contrasena)
    val text_ingresa_tu_correo_registrado_para_enviarte_un = stringResource(R.string.text_ingresa_tu_correo_registrado_para_enviarte_un)
    val text_enviar_enlace = stringResource(R.string.text_enviar_enlace)
    val text_cancelar = stringResource(R.string.text_cancelar)

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    var showResetDialog by remember { mutableStateOf(false) }
    var resetEmail by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight)
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 12.dp)
    ) {

        IconButton(
            onClick = onNavigateBack,
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = text_volver,
                tint = TextDark
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = text_ingresar_con_correo,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = TextDark
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = text_ingresa_tus_credenciales_registradas_para_acceder_a,
            fontSize = 14.sp,
            color = TextMuted
        )

        Spacer(modifier = Modifier.height(28.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text(text_correo_electronico) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Email,
                    contentDescription = text_correo,
                    tint = TextMuted
                )
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            ),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Orange,
                unfocusedBorderColor = BorderLight
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text(text_contrasena) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = text_contrasena,
                    tint = TextMuted
                )
            },
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    if (passwordVisible) {
                        EyeOffIcon(color = TextMuted)
                    } else {
                        EyeIcon(color = TextMuted)
                    }
                }
            },
            singleLine = true,
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Orange,
                unfocusedBorderColor = BorderLight
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(enabled = !isLoading, onClick = {
                resetEmail = email
                showResetDialog = true
            }) {
                Text(
                    text = text_olvidaste_tu_contrasena,
                    fontSize = 13.sp,
                    color = Orange,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        successMessage?.let { Text(it, color = TextDark, fontSize = 13.sp) }
        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = errorMessage,
                color = Color.Red,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        Button(
            onClick = { onLoginClick(email.trim(), password) },
            enabled = !isLoading && email.isNotBlank() && password.isNotBlank(),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = Orange,
                contentColor = Color.White
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.padding(4.dp)
                )
            } else {
                Text(
                    text = text_iniciar_sesion,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text(text_recuperar_contrasena) },
            text = {
                Column {
                    Text(
                        text = text_ingresa_tu_correo_registrado_para_enviarte_un,
                        fontSize = 14.sp,
                        color = TextMuted
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = resetEmail,
                        onValueChange = { resetEmail = it },
                        label = { Text(text_correo_electronico) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (resetEmail.isNotBlank()) {
                            onPasswordResetRequest(resetEmail.trim())
                            showResetDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Orange)
                ) {
                    Text(text_enviar_enlace)
                }
            },
            dismissButton = {
                TextButton(enabled = !isLoading, onClick = { showResetDialog = false }) {
                    Text(text_cancelar)
                }
            }
        )
    }
}
