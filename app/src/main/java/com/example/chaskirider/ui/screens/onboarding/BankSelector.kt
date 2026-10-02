package com.example.chaskirider.ui.screens.onboarding

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.chaskirider.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BankSelector(value: String, enabled: Boolean, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val banks = listOf("BCP" to R.string.bank_bcp, "INTERBANK" to R.string.bank_interbank, "BBVA" to R.string.bank_bbva)
    ExposedDropdownMenuBox(expanded = expanded && enabled, onExpandedChange = { if (enabled) expanded = it }) {
        OutlinedTextField(value = banks.firstOrNull { it.first == value }?.let { stringResource(it.second) }.orEmpty(),
            onValueChange = {}, readOnly = true, enabled = enabled,
            label = { Text(stringResource(R.string.text_banco)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded && enabled) },
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled).fillMaxWidth())
        ExposedDropdownMenu(expanded = expanded && enabled, onDismissRequest = { expanded = false }) {
            banks.forEach { (id, label) ->
                DropdownMenuItem(text = { Text(stringResource(label)) }, onClick = { onSelect(id); expanded = false })
            }
        }
    }
}
