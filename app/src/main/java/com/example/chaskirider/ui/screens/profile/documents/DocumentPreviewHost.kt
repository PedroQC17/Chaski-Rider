package com.example.chaskirider.ui.screens.profile.documents

import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.SubcomposeAsyncImage
import com.example.chaskirider.R
import com.example.chaskirider.ui.theme.*

@Composable
fun DocumentPreviewHost(state: DocumentPreviewUiState, onDismiss: () -> Unit, onPage: (Int) -> Unit) {
    val label = when (state.type) {
        "dniFront" -> R.string.text_dni_frente
        "dniBack" -> R.string.text_dni_reverso
        "driverLicense" -> R.string.text_licencia_de_conducir
        "soat" -> R.string.text_soat
        "bankStatement" -> R.string.text_estado_de_cuenta
        else -> R.string.document_preview
    }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxWidth().padding(16.dp).fillMaxHeight(.86f), shape = RoundedCornerShape(24.dp), color = Color.White) {
            Column(Modifier.padding(16.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(label), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, stringResource(R.string.text_cerrar), tint = Orange) }
                }
                HorizontalDivider(color = BorderLight)
                Box(Modifier.weight(1f).fillMaxWidth().clipToBounds(), contentAlignment = Alignment.Center) {
                    when {
                        state.loading -> CircularProgressIndicator(color = Orange)
                        state.error -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(stringResource(R.string.text_no_se_pudo_abrir_el_archivo))
                            TextButton(onClick = { onPage(state.page?.index ?: 0) }) { Text(stringResource(R.string.retry)) }
                        }
                        state.page != null -> {
                            var zoom by remember(state.page.imageUri) { mutableFloatStateOf(1f) }
                            var offset by remember(state.page.imageUri) { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
                            SubcomposeAsyncImage(model = state.page.imageUri, contentDescription = stringResource(label),
                                modifier = Modifier.fillMaxSize().pointerInput(state.page.imageUri) {
                                    detectTransformGestures { _, pan, scale, _ ->
                                        zoom = (zoom * scale).coerceIn(1f, 4f)
                                        val next = offset + pan
                                        val maxX = size.width * (zoom - 1) / 2
                                        val maxY = size.height * (zoom - 1) / 2
                                        offset = androidx.compose.ui.geometry.Offset(next.x.coerceIn(-maxX, maxX), next.y.coerceIn(-maxY, maxY))
                                    }
                                }.graphicsLayer { scaleX = zoom; scaleY = zoom; translationX = offset.x; translationY = offset.y },
                                contentScale = ContentScale.Fit,
                                loading = { CircularProgressIndicator(Modifier.size(28.dp), color = Orange) },
                                error = { Text(stringResource(R.string.text_no_se_pudo_abrir_el_archivo)) })
                        }
                    }
                }
                state.page?.takeIf { it.count > 1 }?.let { page ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { onPage(page.index - 1) }, enabled = !state.loading && page.index > 0) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.preview_previous))
                        }
                        Text(stringResource(R.string.preview_pages, page.index + 1, page.count))
                        IconButton(onClick = { onPage(page.index + 1) }, enabled = !state.loading && page.index + 1 < page.count) {
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, stringResource(R.string.preview_next))
                        }
                    }
                }
            }
        }
    }
}
