package com.example.chaskirider.ui.screens.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.chaskirider.R
import com.example.chaskirider.domain.model.RiderUser
import com.example.chaskirider.ui.screens.profile.components.*

@Composable
fun ProfilePersonalDataScreen(user: RiderUser, onNavigateBack: () -> Unit, onRequestChange: () -> Unit, onConfigurePassword: () -> Unit) {
    ProfileDetailsLayout(stringResource(R.string.text_datos_personales), onNavigateBack, onRequestChange) {
        ProfileReadOnlyField(user.name, stringResource(R.string.text_nombres))
        ProfileReadOnlyField(user.lastName, stringResource(R.string.text_apellidos))
        ProfileReadOnlyField(user.dni, stringResource(R.string.dni_label))
        ProfileReadOnlyField(user.phone, stringResource(R.string.text_numero_de_celular))
        ProfileReadOnlyField(user.email, stringResource(R.string.text_correo_electronico))
        Button(onClick = onConfigurePassword, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(14.dp)) {
            Text(stringResource(if (user.hasPassword) R.string.password_change else R.string.text_configurar_contrasena))
        }
    }
}
