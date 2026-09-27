package com.example.chaskirider.ui.screens.initial

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chaskirider.R
import com.example.chaskirider.ui.theme.BackgroundLight
import com.example.chaskirider.ui.theme.BorderLight
import com.example.chaskirider.ui.theme.Orange
import com.example.chaskirider.ui.theme.TextDark
import com.example.chaskirider.ui.theme.TextMuted
import kotlinx.coroutines.delay

@Preview(showBackground = true)
@Composable
fun InitialScreenPreview() {
    InitialScreen()
}

// PANTALLA INICIO OPTIMIZADA Y ESTILIZADA
@Composable
fun InitialScreen(
    onNavigateToLogin: () -> Unit = {},
    onNavigateToRegister: () -> Unit = {}
) {
    val images = listOf(
        R.drawable.image_one,
        R.drawable.image_two,
        R.drawable.image_three
    )

    val pageState = rememberPagerState(
        initialPage = 0,
        pageCount = { images.size }
    )

    // TRANSICIÓN AUTOMÁTICA MÁS SUAVE Y PAUSADA (Cada 5s con animación fluida de 1.2s)
    LaunchedEffect(pageState) {
        while (true) {
            delay(5000)
            val nextPage = (pageState.currentPage + 1) % images.size
            pageState.animateScrollToPage(
                page = nextPage,
                animationSpec = tween(
                    durationMillis = 1200,
                    easing = FastOutSlowInEasing
                )
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight)
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // SECCIÓN SUPERIOR: Logo, Ilustración y Texto de bienvenida
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            // 1. Logo Chaski Rider
            Image(
                painter = painterResource(R.drawable.chaski_rider_logo_transparent),
                contentDescription = "Chaski Rider Logo",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth(0.55f)
                    .heightIn(max = 85.dp)
                    .padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 2. Carrusel de Ilustraciones del Repartidor
            HorizontalPager(
                state = pageState,
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .aspectRatio(1.15f)
                    .heightIn(max = 240.dp)
            ) { page ->
                Image(
                    painter = painterResource(images[page]),
                    contentDescription = "Ilustración Chaski Rider",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3. Textos de Bienvenida
            Text(
                text = "Bienvenido a",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = TextDark
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = buildAnnotatedString {
                    withStyle(
                        style = SpanStyle(
                            color = TextDark,
                            fontWeight = FontWeight.Bold
                        )
                    ) {
                        append("Chaski ")
                    }
                    withStyle(
                        style = SpanStyle(
                            color = Orange,
                            fontWeight = FontWeight.Bold
                        )
                    ) {
                        append("Rider")
                    }
                },
                fontSize = 30.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "La app para repartidores de Chaski Food.\nConecta, entrega y gana.",
                fontSize = 14.sp,
                color = TextMuted,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp,
                modifier = Modifier.padding(horizontal = 12.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // SECCIÓN INFERIOR: Botones y Enlace de Recuperación
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Botón Primario Naranja (Iniciar sesión)
            Button(
                onClick = onNavigateToLogin,
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Orange,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text(
                    text = "Iniciar sesión",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Botón Secundario Bordeado (Registrarme)
            OutlinedButton(
                onClick = onNavigateToRegister,
                shape = CircleShape,
                border = BorderStroke(1.dp, BorderLight),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = TextDark
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text(
                    text = "Registrarme",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Enlace "He perdido mi cuenta" subrayado
            TextButton(
                onClick = { /* TODO: Funcionalidad de recuperación de cuenta */ },
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Text(
                    text = "He perdido mi cuenta",
                    fontSize = 14.sp,
                    color = TextMuted,
                    style = TextStyle(textDecoration = TextDecoration.Underline)
                )
            }
        }
    }
}
