package com.example.chaskirider.feature.orders.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.chaskirider.R

@Composable
internal fun OrdersTopHeader(onMenu: () -> Unit) {
    Surface(
        Modifier.statusBarsPadding().padding(16.dp),
        shape = RoundedCornerShape(22.dp),
        shadowElevation = 4.dp
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 4.dp)) {
            IconButton(onClick = onMenu) { Icon(Icons.Default.Menu, stringResource(R.string.orders_menu)) }
            Text(
                stringResource(R.string.text_pedidos),
                Modifier.padding(end = 12.dp),
                fontWeight = FontWeight.Bold
            )
        }
    }
}
