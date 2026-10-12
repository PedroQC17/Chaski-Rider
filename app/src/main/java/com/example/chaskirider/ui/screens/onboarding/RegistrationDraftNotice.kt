package com.example.chaskirider.ui.screens.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.chaskirider.R

@Composable
fun RegistrationDraftNotice(status: DraftStatus, onRetry: () -> Unit) {
    if (status == DraftStatus.SAVED) return
    Column(Modifier.fillMaxWidth()) {
        Text(stringResource(when (status) {
            DraftStatus.LOADING -> R.string.registration_draft_loading
            DraftStatus.SAVING -> R.string.registration_draft_saving
            else -> R.string.registration_draft_error
        }), style = MaterialTheme.typography.bodySmall,
            color = if (status == DraftStatus.ERROR) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
        if (status == DraftStatus.ERROR) TextButton(onClick = onRetry) { Text(stringResource(R.string.retry)) }
    }
}
