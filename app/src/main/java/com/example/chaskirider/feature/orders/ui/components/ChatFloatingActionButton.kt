package com.example.chaskirider.feature.orders.ui.components

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.chaskirider.ui.theme.Orange

@Composable
internal fun BoxScope.ChatFloatingActionButton(onClick: () -> Unit) {
    ExtendedFloatingActionButton(
        onClick = onClick,
        modifier = Modifier
            .align(Alignment.TopEnd)
            .statusBarsPadding()
            .padding(top = 76.dp, end = 16.dp),
        containerColor = Orange,
        contentColor = Color.White,
        shape = RoundedCornerShape(22.dp),
        elevation = FloatingActionButtonDefaults.elevation(4.dp),
        icon = {
            Icon(
                imageVector = Icons.Default.Email,
                contentDescription = "Chat del Pedido",
                modifier = Modifier.size(20.dp)
            )
        },
        text = {
            Text(
                text = "Chat",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.labelLarge
            )
        }
    )
}
