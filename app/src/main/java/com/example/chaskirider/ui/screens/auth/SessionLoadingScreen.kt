package com.example.chaskirider.ui.screens.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.chaskirider.R
import com.example.chaskirider.ui.theme.Orange

@Composable
fun SessionLoadingScreen(state: AuthUiState, onRetry: () -> Unit) {
    Surface(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().systemBarsPadding().padding(32.dp),
            verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold, color = Orange)
            Spacer(Modifier.height(28.dp))
            if (state.isLoading || state.errorMessage == null) {
                CircularProgressIndicator(Modifier.size(28.dp), color = Orange, strokeWidth = 2.dp)
            } else {
                Text(state.errorMessage, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.error)
                TextButton(onClick = onRetry) { Text(stringResource(R.string.retry)) }
            }
        }
    }
}
