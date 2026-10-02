package com.example.chaskirider.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp
import com.example.chaskirider.R
import com.example.chaskirider.ui.theme.*

@Composable
fun PasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    enabled: Boolean = true,
    imeAction: ImeAction = ImeAction.Next
) {
    var visible by remember { mutableStateOf(false) }
    val toggleDescription = stringResource(if (visible) R.string.password_hide else R.string.password_show)
    OutlinedTextField(value = value, onValueChange = onValueChange,
        label = { Text(label) }, enabled = enabled, singleLine = true,
        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Orange, unfocusedBorderColor = BorderLight),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = imeAction),
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            IconButton(onClick = { visible = !visible }, enabled = enabled,
                modifier = Modifier.semantics { contentDescription = toggleDescription }) {
                if (visible) EyeOffIcon(color = TextMuted) else EyeIcon(color = TextMuted)
            }
        })
}
