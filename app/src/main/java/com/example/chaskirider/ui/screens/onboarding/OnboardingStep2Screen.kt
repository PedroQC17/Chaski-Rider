package com.example.chaskirider.ui.screens.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import com.example.chaskirider.R
import com.example.chaskirider.domain.model.VehicleType
import com.example.chaskirider.ui.components.OnboardingHeader
import com.example.chaskirider.ui.theme.BackgroundLight
import com.example.chaskirider.ui.theme.BorderLight
import com.example.chaskirider.ui.theme.ChaskiRiderTheme
import com.example.chaskirider.ui.theme.Orange
import com.example.chaskirider.ui.theme.TextDark
import com.example.chaskirider.ui.theme.TextMuted

@Preview(name = "Paso 2: Tipo de Vehículo", showBackground = true, showSystemUi = true)
@Composable
fun OnboardingStep2ScreenPreview() {
    ChaskiRiderTheme {
        OnboardingStep2Screen(
            currentVehicle = VehicleType.MOTORCYCLE
        )
    }
}

@Composable
fun OnboardingStep2Screen(
    currentVehicle: VehicleType = VehicleType.NONE,
    onNavigateBack: () -> Unit = {},
    onContinueClick: (vehicleType: VehicleType) -> Unit = {},
    isLoading: Boolean = false,
    errorMessage: String? = null
) {
    var selectedVehicle by rememberSaveable { mutableStateOf(currentVehicle) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight)
            .systemBarsPadding().imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 12.dp)
    ) {
        OnboardingHeader(
            currentStep = 2,
            onNavigateBack = onNavigateBack
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Tipo de vehículo",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = TextDark
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Selecciona el vehículo con el que realizarás tus repartos",
            fontSize = 14.sp,
            color = TextMuted
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Tarjeta Bicicleta
        VehicleOptionCard(
            title = "Bicicleta",
            imageRes = R.drawable.vehicle_bicycle,
            isSelected = selectedVehicle == VehicleType.BICYCLE,
            onClick = { selectedVehicle = VehicleType.BICYCLE }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Tarjeta Motocicleta
        VehicleOptionCard(
            title = "Motocicleta",
            imageRes = R.drawable.vehicle_motorcycle,
            isSelected = selectedVehicle == VehicleType.MOTORCYCLE,
            onClick = { selectedVehicle = VehicleType.MOTORCYCLE }
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

        Spacer(modifier = Modifier.height(36.dp))

        // Botón Continuar
        Button(
            onClick = { onContinueClick(selectedVehicle) },
            enabled = !isLoading && selectedVehicle != VehicleType.NONE,
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
                    text = "Continuar",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun VehicleOptionCard(
    title: String,
    imageRes: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) Orange else BorderLight
    val backgroundColor = if (isSelected) Color(0xFFFFF7F2) else Color.White

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor, RoundedCornerShape(16.dp))
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Image(
                painter = painterResource(imageRes),
                contentDescription = title,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .size(110.dp)
                    .padding(end = 12.dp)
            )

            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark,
                modifier = Modifier.weight(1f)
            )

            // Selector Círculo Check
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(28.dp)
                    .background(
                        color = if (isSelected) Orange else Color.White,
                        shape = CircleShape
                    )
                    .border(
                        width = if (isSelected) 0.dp else 1.dp,
                        color = if (isSelected) Color.Transparent else BorderLight,
                        shape = CircleShape
                    )
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Seleccionado",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
