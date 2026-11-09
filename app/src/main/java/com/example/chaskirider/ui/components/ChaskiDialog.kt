package com.example.chaskirider.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.chaskirider.ui.theme.*

@Composable
fun ChaskiDialog(title: String, onDismiss: () -> Unit, content: @Composable () -> Unit,
    confirm: @Composable () -> Unit, dismiss: @Composable () -> Unit = {}) {
    AlertDialog(onDismissRequest = onDismiss, containerColor = Color.White, tonalElevation = 0.dp,
        shape = RoundedCornerShape(24.dp), titleContentColor = TextDark, textContentColor = TextMuted,
        title = { Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) },
        text = { Column(Modifier.heightIn(max = androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp.dp * 0.55f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) { content() } },
        confirmButton = confirm, dismissButton = dismiss)
}
