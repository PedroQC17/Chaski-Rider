package com.example.chaskirider.ui.screens.profile

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.chaskirider.R
import com.example.chaskirider.domain.model.*
import com.example.chaskirider.ui.screens.profile.components.*

@Composable
fun ProfileVehicleScreen(user: RiderUser, onNavigateBack: () -> Unit, onRequestChange: () -> Unit) {
    ProfileDetailsLayout(stringResource(R.string.text_vehiculos), onNavigateBack, onRequestChange) {
        val label = when (user.vehicleType) {
            VehicleType.BICYCLE -> R.string.text_bicicleta
            VehicleType.MOTORCYCLE -> R.string.text_motocicleta
            VehicleType.CAR -> R.string.text_automovil
            else -> R.string.text_sin_vehiculo
        }
        if (user.vehicleType in listOf(VehicleType.BICYCLE, VehicleType.MOTORCYCLE)) {
            Image(painterResource(if (user.vehicleType == VehicleType.BICYCLE) R.drawable.vehicle_bicycle else R.drawable.vehicle_motorcycle),
                null, Modifier.size(180.dp).align(Alignment.CenterHorizontally))
        }
        ProfileReadOnlyField(stringResource(label), stringResource(R.string.text_tipo_de_vehiculo))
    }
}
