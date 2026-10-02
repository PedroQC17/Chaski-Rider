package com.example.chaskirider.ui.screens.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.chaskirider.R
import com.example.chaskirider.ui.components.PasswordField
import com.example.chaskirider.ui.components.OnboardingHeader
import com.example.chaskirider.ui.theme.*

@Composable
fun OnboardingStep1Screen(
    state: PersonalRegistrationUiState,
    onChange: (PersonalField, String) -> Unit,
    onTermsChange: (Boolean) -> Unit,
    onNavigateBack: () -> Unit,
    onContinue: () -> Unit
) {
    Surface(color = BackgroundLight) {
        Column(Modifier.fillMaxSize().systemBarsPadding().imePadding()
            .verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OnboardingHeader(1, onNavigateBack = { if (!state.isLoading) onNavigateBack() })
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.text_datos_personales), style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold)
            PersonalInput(state.name, R.string.text_nombres, !state.isLoading, { onChange(PersonalField.NAME, it) })
            PersonalInput(state.lastName, R.string.text_apellidos, !state.isLoading, { onChange(PersonalField.LAST_NAME, it) })
            PersonalInput(state.dni, R.string.text_dni_8_digitos, !state.isLoading,
                { onChange(PersonalField.DNI, it) }, KeyboardType.Number)
            PersonalInput(state.phone, R.string.text_numero_de_celular, !state.isLoading,
                { onChange(PersonalField.PHONE, it) }, KeyboardType.Phone)
            OutlinedTextField(state.email, {}, readOnly = true, label = { Text(stringResource(R.string.text_correo_electronico)) },
                singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
            if (state.needsPassword) {
                PasswordField(state.password, { onChange(PersonalField.PASSWORD, it) },
                    stringResource(R.string.registration_new_password), enabled = !state.isLoading)
                PasswordField(state.confirmation, { onChange(PersonalField.CONFIRMATION, it) },
                    stringResource(R.string.text_confirmar_contrasena), enabled = !state.isLoading, imeAction = ImeAction.Done)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(state.termsAccepted, onTermsChange, enabled = !state.isLoading,
                    colors = CheckboxDefaults.colors(checkedColor = Orange))
                Text(stringResource(R.string.registration_terms), style = MaterialTheme.typography.bodySmall)
            }
            state.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Button(onClick = onContinue, enabled = !state.isLoading,
                modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Orange)) {
                if (state.isLoading) CircularProgressIndicator(Modifier.size(22.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                else Text(stringResource(R.string.text_continuar))
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun PersonalInput(value: String, label: Int, enabled: Boolean, onChange: (String) -> Unit,
    keyboard: KeyboardType = KeyboardType.Text) {
    OutlinedTextField(value, onChange, label = { Text(stringResource(label)) }, enabled = enabled,
        singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
        keyboardOptions = KeyboardOptions(keyboardType = keyboard, imeAction = ImeAction.Next))
}

@Preview(showBackground = true)
@Composable
private fun PersonalRegistrationPreview() {
    ChaskiRiderTheme { OnboardingStep1Screen(PersonalRegistrationUiState(email = "ana@example.com"), { _, _ -> }, {}, {}, {}) }
}
