package com.example.chaskirider.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chaskirider.R
import com.example.chaskirider.ui.theme.TextDark
import com.example.chaskirider.ui.theme.TextMuted

@Composable
fun AppHeader(
    title: String,
    onNavigate: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    navigationIcon: ImageVector = Icons.AutoMirrored.Filled.ArrowBack,
    navigationDescription: String = stringResource(R.string.text_volver)
) {
    Row(modifier.fillMaxWidth().heightIn(min = 56.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onNavigate, modifier = Modifier.size(48.dp)) {
            Icon(navigationIcon, navigationDescription, tint = TextDark)
        }
        Column(Modifier.weight(1f).padding(start = 8.dp, top = 4.dp, bottom = 4.dp)) {
            Text(title, fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.Bold, color = TextDark)
            subtitle?.let {
                Text(it, fontSize = 13.sp, lineHeight = 18.sp, color = TextMuted)
            }
        }
    }
}
