package com.example.chaskirider.ui.screens.onboarding

import com.example.chaskirider.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import com.example.chaskirider.domain.model.RiderUser
import com.example.chaskirider.ui.components.OnboardingHeader
import com.example.chaskirider.ui.theme.BackgroundLight
import com.example.chaskirider.ui.theme.BorderLight
import com.example.chaskirider.ui.theme.ChaskiRiderTheme
import com.example.chaskirider.ui.theme.Orange
import com.example.chaskirider.ui.theme.TextDark
import com.example.chaskirider.ui.theme.TextMuted

@Preview(name = "Paso 1: Datos Personales", showBackground = true, showSystemUi = true)
@Composable
fun OnboardingStep1ScreenPreview() {
    ChaskiRiderTheme {
        OnboardingStep1Screen(
            user = RiderUser(
                id = "123",
                name = "Ana",
                lastName = "García",
                email = "ana.garcia@example.com",
                phone = "+51987654321",
                dni = "12345678",
                termsAccepted = true
            )
        )
    }
}

@Composable
fun OnboardingStep1Screen(
    user: RiderUser,
    onNavigateBack: () -> Unit = {},
    onContinueClick: (
        name: String,
        lastName: String,
        dni: String,
        phone: String,
        termsAccepted: Boolean
    ) -> Unit = { _, _, _, _, _ -> },
    isLoading: Boolean = false,
    errorMessage: String? = null
) {
    var name by rememberSaveable { mutableStateOf(user.name) }
    var lastName by rememberSaveable { mutableStateOf(user.lastName) }
    var dni by rememberSaveable { mutableStateOf(user.dni) }
    var phone by rememberSaveable { mutableStateOf(user.phone.replace("+51", "")) }
    var termsAccepted by rememberSaveable { mutableStateOf(user.termsAccepted) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight)
            .systemBarsPadding().imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 12.dp)
    ) {
        OnboardingHeader(
            currentStep = 1,
            onNavigateBack = onNavigateBack
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResource(R.string.text_datos_personales),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = TextDark
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text(stringResource(R.string.text_nombres)) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = stringResource(R.string.text_nombres),
                    tint = TextMuted
                )
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Orange,
                unfocusedBorderColor = BorderLight
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = lastName,
            onValueChange = { lastName = it },
            label = { Text(stringResource(R.string.text_apellidos)) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = stringResource(R.string.text_apellidos),
                    tint = TextMuted
                )
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Orange,
                unfocusedBorderColor = BorderLight
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = dni,
            onValueChange = {
                if (it.length <= 8 && it.all { char -> char.isDigit() }) {
                    dni = it
                }
            },
            label = { Text(stringResource(R.string.text_dni_8_digitos)) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = stringResource(R.string.dni_label),
                    tint = TextMuted
                )
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Next
            ),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Orange,
                unfocusedBorderColor = BorderLight
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = phone,
            onValueChange = {
                if (it.length <= 9 && it.all { char -> char.isDigit() }) {
                    phone = it
                }
            },
            label = { Text(stringResource(R.string.text_numero_de_celular)) },
            leadingIcon = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = 12.dp, end = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = stringResource(R.string.text_celular),
                        tint = TextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = stringResource(R.string.text_51),
                        color = TextDark,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Phone,
                imeAction = ImeAction.Done
            ),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Orange,
                unfocusedBorderColor = BorderLight
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = user.email,
            onValueChange = {},
            readOnly = true,
            enabled = false,
            label = { Text(stringResource(R.string.text_correo_electronico_cuenta_autenticada)) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Email,
                    contentDescription = stringResource(R.string.text_correo),
                    tint = TextMuted
                )
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                disabledBorderColor = BorderLight,
                disabledTextColor = TextDark,
                disabledLabelColor = TextMuted
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(20.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFFFF7F2), RoundedCornerShape(12.dp))
                .border(1.dp, Color(0xFFFFE0D6), RoundedCornerShape(12.dp))
                .clickable { termsAccepted = !termsAccepted }
                .padding(12.dp)
        ) {
            Checkbox(
                checked = termsAccepted,
                onCheckedChange = { termsAccepted = it },
                colors = CheckboxDefaults.colors(
                    checkedColor = Orange,
                    uncheckedColor = TextMuted
                )
            )

            Text(
                text = buildAnnotatedString {
                    append(stringResource(R.string.text_al_registrarte_aceptas_los))
                    withStyle(SpanStyle(color = Orange, fontWeight = FontWeight.Bold)) {
                        append(stringResource(R.string.text_terminos_y_condiciones))
                    }
                    append(stringResource(R.string.text_y_la))
                    withStyle(SpanStyle(color = Orange, fontWeight = FontWeight.Bold)) {
                        append(stringResource(R.string.text_politica_de_privacidad))
                    }
                    append(stringResource(R.string.text_period))
                },
                fontSize = 12.sp,
                color = TextDark,
                lineHeight = 16.sp
            )
        }

        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = errorMessage,
                color = Color.Red,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        Button(
            onClick = {
                onContinueClick(
                    name.trim(),
                    lastName.trim(),
                    dni.trim(),
                    phone.trim(),
                    termsAccepted
                )
            },
            enabled = !isLoading &&
                    name.isNotBlank() &&
                    lastName.isNotBlank() &&
                    dni.length == 8 &&
                    phone.length == 9 &&
                    termsAccepted,
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
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Text(
                    text = stringResource(R.string.text_continuar),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
