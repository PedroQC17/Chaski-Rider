package com.example.chaskirider.ui.screens.profile.photo

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.chaskirider.R
import com.example.chaskirider.ui.theme.*

@Composable
fun ProfilePhotoScreen(state: ProfilePhotoUiState, onCapture: () -> Unit, onSave: () -> Unit, onLogout: () -> Unit, onOpenSettings: () -> Unit) {
    Surface(color = Color.White) {
        Column(Modifier.fillMaxSize().systemBarsPadding().verticalScroll(rememberScrollState()).padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Text(stringResource(R.string.app_name), color = Orange, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            Surface(Modifier.size(220.dp), shape = CircleShape, color = Color(0xFFFFEEE5), border = BorderStroke(2.dp, Color(0xFFFFCCB5))) {
                if (state.previewUri != null) AsyncImage(state.previewUri, stringResource(R.string.profile_photo),
                    modifier = Modifier.fillMaxSize().clip(CircleShape), contentScale = ContentScale.Crop)
                else Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Person, null, Modifier.size(112.dp), tint = Orange) }
            }
            Text(stringResource(R.string.profile_photo_title), style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Text(stringResource(R.string.profile_photo_body), color = TextMuted, textAlign = TextAlign.Center)
            state.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center) }
            if (state.permissionDenied) TextButton(onClick = onOpenSettings) { Text(stringResource(R.string.profile_photo_settings)) }
            Button(onClick = if (state.previewUri == null) onCapture else onSave, enabled = !state.isLoading,
                modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(16.dp)) {
                if (state.isLoading) CircularProgressIndicator(Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
                else Text(stringResource(if (state.previewUri == null) R.string.profile_photo_capture else R.string.profile_photo_save))
            }
            if (state.previewUri != null) OutlinedButton(onClick = onCapture, enabled = !state.isLoading,
                modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(16.dp)) {
                Text(stringResource(R.string.profile_photo_retake))
            }
            TextButton(onClick = onLogout, enabled = !state.isLoading) { Text(stringResource(R.string.text_cerrar_sesion), color = TextMuted) }
        }
    }
}
