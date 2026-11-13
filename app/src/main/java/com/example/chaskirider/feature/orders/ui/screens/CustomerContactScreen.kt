package com.example.chaskirider.feature.orders.ui.screens
import com.example.chaskirider.feature.support.ui.screens.OrderHelpPage

import com.example.chaskirider.feature.orders.ui.viewmodel.CustomerContactViewModel
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.chaskirider.BuildConfig
import com.example.chaskirider.platform.DevicePhoneCalls

@Composable
fun CustomerContactDialog(viewModel: CustomerContactViewModel, orderId: String, customerPhone: String?,
    onClose: () -> Unit, onSupport: () -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var testPhone by rememberSaveable(orderId) { mutableStateOf("") }
    var phoneError by remember(orderId) { mutableStateOf<String?>(null) }
    val phone = state.contact?.phone ?: customerPhone ?: testPhone
    val validPhone = Regex("\\+?[0-9]{9,15}").matches(phone)
    val calls = remember(context) { DevicePhoneCalls(context.applicationContext) }
    val permissions = arrayOf(Manifest.permission.CALL_PHONE, Manifest.permission.READ_PHONE_STATE)
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        if (permissions.all { result[it] == true || ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED })
            viewModel.call(calls, phone)
        else phoneError = "Necesitamos permiso para iniciar y detectar el intento de llamada. Puedes solicitar ayuda si no puedes llamar."
    }
    fun call() {
        phoneError = null
        if (permissions.all { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED })
            viewModel.call(calls, phone)
        else permissionLauncher.launch(permissions)
    }
    val contact = state.contact
    val waited = state.waitedMillis
    OrderHelpPage("No puedo comunicarme con el cliente", onClose) {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Registra tu llegada y llama al cliente. La primera llamada inicia los 10 minutos de espera.")
                if (state.loading || state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                (state.error ?: phoneError)?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                if (contact == null) {
                    Button(onClick = viewModel::arrive, enabled = !state.loading && !state.busy) { Text("Registrar llegada al cliente") }
                    Text("Necesitas una ubicación reciente a menos de 100 metros del destino.")
                } else {
                    val remaining = ((600_000 - waited).coerceAtLeast(0) + 999) / 1000
                    Text(if (contact.firstCallAt == null) "Llegada registrada · Espera sin iniciar"
                        else "Tiempo restante: %02d:%02d".format(remaining / 60, remaining % 60),
                        style = MaterialTheme.typography.titleMedium)
                    Text("Intentos registrados: ${if (contact.secondCallAt != null) 2 else if (contact.firstCallAt != null) 1 else 0} de 2")
                    if (customerPhone == null && BuildConfig.DEBUG) {
                        OutlinedTextField(value = phone, onValueChange = {
                            testPhone = it.filter { c -> c.isDigit() || c == '+' }.take(16)
                        }, label = { Text("Teléfono de pruebas") }, enabled = !state.busy && contact.firstCallAt == null,
                            supportingText = { Text("Solo se usa en este pedido de prueba.") })
                    }
                    if (!validPhone) Text("El pedido aún no tiene un teléfono de contacto disponible.")
                    if (contact.secondCallAt == null) {
                        Button(onClick = ::call, enabled = !state.busy && validPhone && contact.canCallAgain(waited)) {
                            Text(if (contact.firstCallAt == null) "Llamar al cliente" else "Realizar segundo intento")
                        }
                        if (contact.firstCallAt != null && waited < 180_000)
                            Text("El segundo intento estará disponible tres minutos después del primero.")
                    }
                    
                    Button(onClick = onSupport, enabled = !state.busy && contact.canContactSupport(waited)) { Text("Hablar con soporte") }
                    Text(if (state.busy) "Comprobando los registros. Espera un momento…"
                        else "Al finalizar por cliente ausente recibes el pago de la ruta sin propina.")
                }
            }
        }

    if (contact?.showReminder(waited) == true) {
        AlertDialog(onDismissRequest = {}, title = { Text("Intenta también por WhatsApp") },
            text = {
                Column(Modifier.heightIn(max = androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp.dp * 0.5f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Prueba comunicarte con el cliente por WhatsApp mientras termina la espera.")
                    TextButton(enabled = validPhone, onClick = {
                        val digits = phone.filter(Char::isDigit).let { if (it.length == 9) "51$it" else it }
                        try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$digits"))) }
                        catch (_: Exception) { phoneError = "No se pudo abrir WhatsApp." }
                    }) { Text("Abrir chat · $phone") }
                }
            }, confirmButton = { TextButton(onClick = viewModel::acknowledgeReminder, enabled = !state.busy) { Text("Entendido") } })
    }
}
