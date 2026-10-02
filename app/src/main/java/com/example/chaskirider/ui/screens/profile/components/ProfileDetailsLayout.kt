package com.example.chaskirider.ui.screens.profile.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.chaskirider.R
import com.example.chaskirider.ui.components.AppHeader

@Composable
fun ProfileDetailsLayout(title: String, onBack: () -> Unit, onRequestChange: () -> Unit,
    content: @Composable ColumnScope.() -> Unit) {
    Surface(color = Color.White) {
        Column(Modifier.fillMaxSize().systemBarsPadding().verticalScroll(rememberScrollState()).padding(24.dp, 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)) {
            AppHeader(title, onBack)
            content()
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onRequestChange, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp), shape = RoundedCornerShape(14.dp)) {
                Text(stringResource(R.string.profile_request_change))
            }
        }
    }
}

@Composable
fun ProfileReadOnlyField(value: String, label: String) {
    OutlinedTextField(value, {}, label = { Text(label) }, readOnly = true,
        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true)
}
