package com.example.chaskirider.ui.screens.auth

import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chaskirider.R
import com.example.chaskirider.ui.components.AppleLogoIcon
import com.example.chaskirider.ui.components.GoogleLogoIcon
import com.example.chaskirider.ui.theme.*

@Composable
fun AccessScreen(
    onGoogleSignInClick: () -> Unit = {},
    onEmailLoginClick: () -> Unit = {},
    onAccountRecoveryClick: () -> Unit = {},
    isLoading: Boolean = false,
    errorMessage: String? = null
) {
    val text_tu_proxima_ruta_empieza_aqui = stringResource(R.string.text_tu_proxima_ruta_empieza_aqui)
    val text_chaski = stringResource(R.string.text_chaski)
    val text_rider = stringResource(R.string.text_rider)
    val text_conecta_entrega_y_gana_nempieza_tu_registro = stringResource(R.string.text_conecta_entrega_y_gana_nempieza_tu_registro)
    val text_continuar_con_google = stringResource(R.string.text_continuar_con_google)
    val text_continuar_con_apple = stringResource(R.string.text_continuar_con_apple)
    val text_apple_no_disponible = stringResource(R.string.text_apple_no_disponible)
    val text_o_usa_tu_correo = stringResource(R.string.text_o_usa_tu_correo)
    val text_ingresar_con_correo = stringResource(R.string.text_ingresar_con_correo)
    val text_olvidaste_tu_contrasena = stringResource(R.string.text_olvidaste_tu_contrasena)

    Column(
        Modifier.fillMaxSize().background(Color.White).systemBarsPadding()
            .verticalScroll(rememberScrollState()).padding(horizontal = 28.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(painterResource(R.drawable.image_one), null,
            Modifier.fillMaxWidth().heightIn(max = 210.dp).aspectRatio(1.5f),
            contentScale = ContentScale.Fit)
        Spacer(Modifier.height(12.dp))
        Text(text_tu_proxima_ruta_empieza_aqui, color = Orange, fontSize = 11.sp,
            fontWeight = FontWeight.Bold, letterSpacing = 1.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.Center) {
            Text(text_chaski, fontSize = 32.sp, fontWeight = FontWeight.Bold, color = TextDark)
            Text(text_rider, fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Orange)
        }
        Spacer(Modifier.height(10.dp))
        Text(text_conecta_entrega_y_gana_nempieza_tu_registro,
            color = TextMuted, fontSize = 14.sp, lineHeight = 21.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(28.dp))
        OutlinedButton(onClick = onGoogleSignInClick, enabled = !isLoading,
            modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, BorderLight),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextDark)) {
            GoogleLogoIcon(Modifier.size(22.dp))
            Spacer(Modifier.width(12.dp))
            Text(text_continuar_con_google, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, BorderLight),
            colors = ButtonDefaults.outlinedButtonColors(disabledContentColor = TextMuted,
                disabledContainerColor = Color(0xFFF7F7F7))) {
            AppleLogoIcon(Modifier.size(22.dp))
            Spacer(Modifier.width(12.dp))
            Text(text_continuar_con_apple)
        }
        Spacer(Modifier.height(6.dp))
        Text(text_apple_no_disponible, fontSize = 11.sp, color = TextMuted)
        Spacer(Modifier.height(18.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            HorizontalDivider(Modifier.weight(1f), color = BorderLight)
            Text(text_o_usa_tu_correo, fontSize = 12.sp, color = TextMuted)
            HorizontalDivider(Modifier.weight(1f), color = BorderLight)
        }
        Spacer(Modifier.height(18.dp))
        Button(onClick = onEmailLoginClick, enabled = !isLoading,
            modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Orange)) {
            Icon(Icons.Default.Email, null, Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
            Text(text_ingresar_con_correo, fontWeight = FontWeight.SemiBold)
        }
        if (isLoading) {
            Spacer(Modifier.height(16.dp))
            CircularProgressIndicator(Modifier.size(24.dp), color = Orange, strokeWidth = 2.dp)
        }
        errorMessage?.let {
            Spacer(Modifier.height(12.dp))
            Text(it, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center, fontSize = 13.sp)
        }
        TextButton(onClick = onAccountRecoveryClick, enabled = !isLoading) {
            Text(text_olvidaste_tu_contrasena, color = TextMuted, fontSize = 13.sp)
        }
    }
}
