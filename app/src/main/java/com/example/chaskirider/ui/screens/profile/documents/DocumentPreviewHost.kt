package com.example.chaskirider.ui.screens.profile.documents

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.SubcomposeAsyncImage
import com.example.chaskirider.R

@Composable
fun DocumentPreviewHost(uri: Uri, onDismiss: () -> Unit, onError: (String) -> Unit) {
    val context = LocalContext.current
    val mime = remember(uri) { context.contentResolver.getType(uri) }
    if (mime == "application/pdf") {
        LaunchedEffect(uri) {
            try {
                context.startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(uri, mime)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))
            } catch (_: android.content.ActivityNotFoundException) {
                onError(context.getString(R.string.text_no_hay_una_aplicacion_disponible_para_abrir))
            } catch (_: SecurityException) {
                onError(context.getString(R.string.text_no_se_pudo_abrir_el_archivo))
            } finally { onDismiss() }
        }
        return
    }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize().systemBarsPadding().padding(16.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.document_preview), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.text_cerrar)) }
                }
                SubcomposeAsyncImage(model = uri, contentDescription = stringResource(R.string.document_preview),
                    modifier = Modifier.fillMaxWidth().weight(1f), contentScale = ContentScale.Fit,
                    loading = { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() } },
                    error = { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(stringResource(R.string.text_no_se_pudo_abrir_el_archivo))
                    } })
            }
        }
    }
}
