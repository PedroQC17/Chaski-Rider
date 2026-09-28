package com.example.chaskirider.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
fun GoogleLogoIcon(modifier: Modifier = Modifier.size(24.dp)) {
    androidx.compose.foundation.Image(androidx.compose.ui.res.painterResource(com.example.chaskirider.R.drawable.ic_google), null, modifier)
}

@Composable
fun AppleLogoIcon(modifier: Modifier = Modifier.size(24.dp)) {
    androidx.compose.foundation.Image(androidx.compose.ui.res.painterResource(com.example.chaskirider.R.drawable.ic_apple), null, modifier)
}
@Composable
fun EyeIcon(modifier: Modifier = Modifier.size(24.dp), color: Color = Color.Gray) {
    Canvas(modifier = modifier) {
        val width = size.width
        drawCircle(
            color = color,
            radius = width / 4f,
            style = Stroke(width = width / 10f)
        )
    }
}

@Composable
fun EyeOffIcon(modifier: Modifier = Modifier.size(24.dp), color: Color = Color.Gray) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        drawLine(
            color = color,
            start = Offset(width * 0.2f, height * 0.2f),
            end = Offset(width * 0.8f, height * 0.8f),
            strokeWidth = width / 10f
        )
    }
}

@Composable
fun ChevronRightIcon(modifier: Modifier = Modifier.size(20.dp), color: Color = Color.Gray) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val path = Path().apply {
            moveTo(width * 0.3f, height * 0.2f)
            lineTo(width * 0.7f, height * 0.5f)
            lineTo(width * 0.3f, height * 0.8f)
        }
        drawPath(path = path, color = color, style = Stroke(width = width / 8f))
    }
}
