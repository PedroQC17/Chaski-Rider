package com.example.chaskirider.ui.screens.home.components

import com.example.chaskirider.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chaskirider.ui.theme.DangerRed
import com.example.chaskirider.ui.theme.TextDark
import kotlinx.coroutines.launch

@Composable
fun NotificationsBell(count: Int, onClick: () -> Unit) {
    Box(contentAlignment = Alignment.TopEnd) {
        Icon(
            imageVector = Icons.Default.Notifications,
            contentDescription = stringResource(R.string.text_notificaciones),
            tint = TextDark,
            modifier = Modifier
                .size(26.dp)
                .clickable { onClick() }
        )
        if (count > 0) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .offset(x = 12.dp, y = (-6).dp)
                    .size(16.dp)
                    .background(DangerRed, CircleShape)
            ) {
                Text(
                    text = if (count > 9) stringResource(R.string.text_9) else count.toString(),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}
