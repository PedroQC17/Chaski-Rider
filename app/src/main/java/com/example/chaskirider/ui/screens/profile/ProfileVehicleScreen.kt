
package com.example.chaskirider.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chaskirider.domain.model.RiderUser
import com.example.chaskirider.domain.model.VehicleType
import com.example.chaskirider.ui.screens.onboarding.VehicleOptionCard
import com.example.chaskirider.ui.theme.BackgroundLight
import com.example.chaskirider.ui.theme.ChaskiRiderTheme
import com.example.chaskirider.ui.theme.Orange
import com.example.chaskirider.ui.theme.TextDark
import com.example.chaskirider.ui.theme.TextMuted

@Preview(name = "Perfil - Vehículos", showBackground = true, showSystemUi = true)
@Composable
fun ProfileVehicleScreenPreview() {
    ChaskiRiderTheme {
        ProfileVehicleScreen(
            user = RiderUser(
                id = "123",
                name = "Pedro",
                lastName = "Quincho Cordova",
                vehicleType = VehicleType.MOTORCYCLE
            )
        )
    }
}

@Composable
fun ProfileVehicleScreen(
    user: RiderUser,
    onNavigateBack: () -> Unit = {},
    onSaveClick: (vehicleType: VehicleType) -> Unit = {},
    isLoading: Boolean = false,
    errorMessage: String? = null
) {
    var selectedVehicle by rememberSaveable { mutableStateOf(user.vehicleType) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight)
            .systemBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Volver",
                    tint = TextDark
                )
            }
            Text(
                text = "Vehículos",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Vinculado a tu cuenta: ${vehicleLabel(user.vehicleType)}",
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = Orange
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Selecciona el vehículo con el que realizarás tus repartos",
            fontSize = 14.sp,
            color = TextMuted
        )

        Spacer(modifier = Modifier.height(20.dp))

        VehicleOptionCard(
            title = "Bicicleta",
            imageRes = com.example.chaskirider.R.drawable.vehicle_bicycle,
            isSelected = selectedVehicle == VehicleType.BICYCLE,
            onClick = { selectedVehicle = VehicleType.BICYCLE }
        )

        Spacer(modifier = Modifier.height(16.dp))

        VehicleOptionCard(
            title = "Motocicleta",
            imageRes = com.example.chaskirider.R.drawable.vehicle_motorcycle,
            isSelected = selectedVehicle == VehicleType.MOTORCYCLE,
            onClick = { selectedVehicle = VehicleType.MOTORCYCLE }
        )

        Spacer(modifier = Modifier.height(16.dp))

        VehicleOptionCard(
            title = "Automóvil",
            imageRes = com.example.chaskirider.R.drawable.vehicle_car,
            isSelected = selectedVehicle == VehicleType.CAR,
            onClick = { selectedVehicle = VehicleType.CAR }
        )

        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = errorMessage,
                color = Color.Red,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        Button(
            onClick = { onSaveClick(selectedVehicle) },
            enabled = !isLoading &&
                    selectedVehicle != VehicleType.NONE &&
                    selectedVehicle != user.vehicleType,
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
                    text = "Guardar cambios",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

private fun vehicleLabel(vehicle: VehicleType): String = when (vehicle) {
    VehicleType.BICYCLE -> "Bicicleta"
    VehicleType.MOTORCYCLE -> "Motocicleta"
    VehicleType.CAR -> "Automóvil"
    VehicleType.NONE -> "Sin vehículo"
}
