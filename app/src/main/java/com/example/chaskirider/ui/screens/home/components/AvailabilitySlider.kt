package com.example.chaskirider.ui.screens.home.components

import com.example.chaskirider.R
import androidx.compose.ui.res.stringResource
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chaskirider.ui.theme.Orange
import com.example.chaskirider.ui.theme.SuccessGreen
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun AvailabilitySlider(
    available: Boolean,
    enabled: Boolean,
    onToggle: () -> Unit
) {
    val text_desliza_para_desconectar = stringResource(R.string.text_desliza_para_desconectar)
    val text_desliza_para_estar_disponible = stringResource(R.string.text_desliza_para_estar_disponible)
    val text_desconectar = stringResource(R.string.text_desconectar)
    val text_conectar = stringResource(R.string.text_conectar)

    val thumbSize = 44.dp
    val scope = rememberCoroutineScope()
    var innerWidth by remember { mutableIntStateOf(0) }
    val maxOffset = with(androidx.compose.ui.platform.LocalDensity.current) {
        (innerWidth - thumbSize.toPx()).coerceAtLeast(0f)
    }

    var dragPx by remember { mutableFloatStateOf(0f) }
    var snapJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    val trackColor = if (available) SuccessGreen else Orange

    LaunchedEffect(available) { snapJob?.cancel(); dragPx = 0f }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(trackColor)
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .padding(6.dp)
                .onSizeChanged { innerWidth = it.width }
                .pointerInput(available, enabled, maxOffset) {
                    detectDragGestures(
                        onDragStart = { snapJob?.cancel() },
                        onDrag = { change, amount ->
                            change.consume()
                            dragPx = (dragPx + amount.x).coerceIn(0f, maxOffset)
                        },
                        onDragEnd = {
                            if (enabled && maxOffset > 0f && dragPx >= maxOffset * 0.85f) {
                                onToggle()
                            }
                            snapJob = scope.launch {
                                animate(
                                    initialValue = dragPx,
                                    targetValue = 0f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessMedium
                                    )
                                ) { value, _ -> dragPx = value }
                            }
                        }
                    )
                }
        ) {
            Text(
                text = if (available) text_desliza_para_desconectar else text_desliza_para_estar_disponible,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 56.dp, end = 12.dp)
            )
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset { IntOffset(dragPx.roundToInt(), 0) }
                    .size(thumbSize)
                    .background(Color.White, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = if (available) text_desconectar else text_conectar,
                    tint = trackColor,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}
