package com.example.chaskirider.ui.screens.initial

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chaskirider.R
import com.example.chaskirider.ui.theme.Orange

@Preview(showBackground = true)
@Composable
fun InitialScreenPreview() {
    InitialScreen()
}

// PANTALLA INICIO
@Composable
fun InitialScreen(
    onNavigateToLogin: () -> Unit = {},
    onNavigateToRegister: () -> Unit = {}
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ChaskiLogo()

        RiderIlustration()

        Spacer(modifier = Modifier.weight(1f))
        WelcomeText()
        Spacer(modifier = Modifier.weight(1f))
        OnboardingActions(
            onNavigateToLogin = onNavigateToLogin,
            onNavigateToRegister = onNavigateToRegister
        )
    }
}

// Logo imagen
@Composable
fun ChaskiLogo() {
    val image = painterResource(R.drawable.chaski_rider_logo_transparent)

    Image(
        painter = image,
        contentDescription = "chaski_rider_logo",
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .padding(top = 20.dp)
            .height(200.dp)
            .width(300.dp)
    )
}

// Imagenes desplazables
@Composable
fun RiderIlustration() {
    val images = listOf(
        R.drawable.image_one,
        R.drawable.image_two,
        R.drawable.image_three
    )

    val pageState = rememberPagerState(
        initialPage = 0,
        pageCount = { 3 }
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        HorizontalPager(
            state = pageState,
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp)
        ) { page ->
            Image(
                painter = painterResource(images[page]),
                contentDescription = "imagenes de repartidores de chaski riders",
                contentScale = ContentScale.Crop
            )
        }

        // Puntos o indicadores de las imagenes
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 8.dp)
        ) {
            repeat(3) { index ->
                Text(
                    text = if (pageState.currentPage == index) "●" else "○",
                    fontSize = 20.sp,
                    color = Orange
                )
            }
        }
    }
}

// Texto de bienvenida completa
@Composable
fun WelcomeText() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Bienvenido a",
            fontSize = 28.sp
        )
        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = buildAnnotatedString {
                withStyle(
                    style = SpanStyle(
                        color = Color.Black,
                        fontWeight = FontWeight.Bold
                    )
                ) {
                    append("Chaski")
                }

                withStyle(
                    style = SpanStyle(
                        color = Orange,
                        fontWeight = FontWeight.Bold
                    )
                ) {
                    append(" Rider")
                }
            },
            fontSize = 48.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "La app para repartidores de Chaski Food.\nConecta, entrega y gana.",
            fontSize = 20.sp,
            fontStyle = FontStyle.Italic,
            color = Color.Gray,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold
        )
    }
}

// Agrupa los botones e indicador de la pagina
@Composable
fun OnboardingActions(
    onNavigateToLogin: () -> Unit = {},
    onNavigateToRegister: () -> Unit = {}
) {
    val textSignin = "Registrarse"
    val textLogin = "Iniciar Sesión"

    Row(
        modifier = Modifier
            .padding(bottom = 20.dp)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        Button(
            onClick = onNavigateToLogin,
            modifier = Modifier.height(55.dp)
        ) {
            Text(
                text = textLogin,
                fontSize = 16.sp
            )
        }

        Button(
            onClick = onNavigateToRegister,
            modifier = Modifier.height(55.dp)
        ) {
            Text(
                text = textSignin,
                fontSize = 16.sp
            )
        }
    }
}
