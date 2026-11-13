package com.example.chaskirider.feature.vehicle.ui.screens

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
import com.example.chaskirider.feature.user.domain.RiderUser
import com.example.chaskirider.feature.vehicle.domain.VehicleType
import com.example.chaskirider.feature.user.ui.components.*

@Composable
fun ProfileVehicleScreen(user: RiderUser, onNavigateBack: () -> Unit, onRequestChange: () -> Unit) {
    ProfileDetailsLayout(stringResource(R.string.text_vehiculos), onNavigateBack, onRequestChange) {
        val vehicle = VehicleType.fromId(user.vehicleTypeId)
        val label = when (vehicle) {
            VehicleType.BICYCLE -> R.string.text_bicicleta
            VehicleType.MOTORCYCLE -> R.string.text_motocicleta
            VehicleType.CAR -> R.string.text_automovil
            else -> R.string.text_sin_vehiculo
        }
        if (vehicle in listOf(VehicleType.BICYCLE, VehicleType.MOTORCYCLE)) {
            Image(painterResource(if (vehicle == VehicleType.BICYCLE) R.drawable.vehicle_bicycle else R.drawable.vehicle_motorcycle),
                null, Modifier.size(180.dp).align(Alignment.CenterHorizontally))
        }
        ProfileReadOnlyField(stringResource(label), stringResource(R.string.text_tipo_de_vehiculo))
    }
}
