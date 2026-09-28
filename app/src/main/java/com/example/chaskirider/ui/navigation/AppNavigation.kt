// HU03 - Parte 1 - cambios en este archivo:
// 1. El NavHost ahora vive dentro de un Scaffold con bottom bar (AppBottomBar),
//    visible solo en las rutas Home y Profile; contentWindowInsets en 0 para no
//    duplicar insets (las pantallas ya usan systemBarsPadding/statusBarsPadding).
// 2. Nueva ruta Screen.Profile que muestra ProfileScreen (en esta parte solo
//    Cerrar sesión está habilitado; el resto se habilita en las Partes 2-4).
// 3. En el Home se agregó el botón "Mi perfil".
// HU03 - Parte 2 - cambios en este archivo:
// 4. Nueva ruta Screen.ProfilePersonalData con ProfilePersonalDataScreen y
//    ProfileScreen.onPersonalDataClick conectado a updatePersonalData.
// HU03 - Parte 3 - cambios en este archivo:
// 5. Nueva ruta Screen.ProfileVehicle con ProfileVehicleScreen y
//    ProfileScreen.onVehicleClick conectado a updateVehicle.
// HU03 - Parte 4 - cambios en este archivo:
// 6. Nueva ruta Screen.ProfileDocuments con ProfileDocumentsScreen; reutiliza
//    uploadDocument/openDocument (cámara + picker) y se conecta
//    ProfileScreen.onDocumentsClick.
// HU04 - Parte 2 - cambios en este archivo:
// 7. El Home deja de ser una columna de texto y pasa a HomeScreen (saludo,
//    chip de estado, tarjeta de disponibilidad con slider, permiso de
//    ubicación al activarse); conecta setAvailability/reportError y conserva
//    Configurar contraseña y Cerrar sesión.
// HU04 - Parte 3 - cambios en este archivo:
// 8. Con cada sesión activa se suscribe el dispositivo al topic FCM
//    "rider_{uid}" para recibir pushes dirigidos a este repartidor.
// HU04 - Parte 4 - cambios en este archivo:
// 9. Nueva ruta Screen.Notifications con NotificationsScreen; se conecta la
//    campana del Home y la fila "Notificaciones" del perfil. Al tocar una
//    notificación push se abre MainActivity (NEW_TASK|CLEAR_TASK) y el
//    routing existente deja en Home con sesión o en Access sin ella.
// HU06 - Parte 3 - cambios en este archivo:
// 10. Nuevo OrdersViewModel (inyectado desde AppContainer) vinculado al
//     usuario actual; cuando llega una oferta se navega sola a la ruta
//     Screen.Offer y al aceptar/rechazar/expirar se regresa (popBackStack).
// HU06 - Parte 4 - cambios en este archivo:
// 11. Nueva ruta Screen.Orders (pestaña Pedidos, visible en la bottom bar) y
//     HomeScreen recibe ordersState + onSimulateOffer para el estado mock.
package com.example.chaskirider.ui.navigation

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.*
import androidx.navigation.NavHostController
import com.example.chaskirider.data.auth.GoogleSignInClient
import com.example.chaskirider.di.AppContainer
import com.example.chaskirider.domain.model.*
import com.example.chaskirider.ui.components.AppBottomBar
import com.example.chaskirider.ui.screens.auth.*
import com.example.chaskirider.ui.screens.home.*
import com.example.chaskirider.ui.screens.notifications.*
import com.example.chaskirider.ui.screens.onboarding.*
import com.example.chaskirider.ui.screens.orders.*
import com.example.chaskirider.ui.screens.profile.*
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
fun AppNavigation(navController: NavHostController = rememberNavController(), authViewModel: AuthViewModel = viewModel()) {
    val state by authViewModel.uiState.collectAsState()
    // HU06 - Parte 3: ViewModel de pedidos con el repositorio mock (AppContainer).
    val ordersViewModel: OrdersViewModel = viewModel { OrdersViewModel(AppContainer.orderRepository) }
    val ordersState by ordersViewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val google = remember { GoogleSignInClient() }
    var googleBusy by remember { mutableStateOf(false) }
    var recovery by remember { mutableStateOf(false) }
    var passwordDialog by remember { mutableStateOf(false) }
    var resetEmail by remember { mutableStateOf("") }
    val entry by navController.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    val user = state.currentUser

    // Session restoration and sign-in use the same routing logic. Never infer a new account on read failure.
    LaunchedEffect(state.initialized, user?.id, user?.status, route) {
        if (!state.initialized) return@LaunchedEffect
        if (user != null && (route == Screen.Access.route || route == Screen.EmailLogin.route)) {
            navController.navigate(destination(user)) { popUpTo(navController.graph.id) { inclusive = true }; launchSingleTop = true }
        } else if (user == null && route != null && route !in listOf(Screen.Access.route, Screen.EmailLogin.route)) {
            navController.navigate(Screen.Access.route) { popUpTo(navController.graph.id) { inclusive = true } }
        }
    }

    val mainRoutes = listOf(Screen.Home.route, Screen.Profile.route, Screen.Orders.route)
    Scaffold(
        bottomBar = {
            if (route in mainRoutes) AppBottomBar(currentRoute = route, onNavigate = { target ->
                navController.navigate(target) { launchSingleTop = true }
            })
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { padding ->
        NavHost(navController, startDestination = Screen.Access.route, modifier = Modifier.padding(padding)) {
            composable(Screen.Access.route) {
                AccessScreen(
                    onGoogleSignInClick = {
                        if (!googleBusy && !state.isLoading) {
                            googleBusy = true
                            authViewModel.clearError()
                            scope.launch {
                                try { authViewModel.loginWithGoogle(google.getIdToken(context)) }
                                catch (_: GetCredentialCancellationException) { /* User dismissed the picker. */ }
                                catch (e: CancellationException) { throw e }
                                catch (_: Exception) { authViewModel.reportError("No se pudo iniciar sesión con Google. Revisa la conexión y la configuración de Firebase.") }
                                finally { googleBusy = false }
                            }
                        }
                    },
                    onEmailLoginClick = { authViewModel.clearError(); navController.navigate(Screen.EmailLogin.route) },
                    onAccountRecoveryClick = { authViewModel.clearError(); recovery = true },
                    isLoading = state.isLoading || googleBusy, errorMessage = state.errorMessage
                )
            }
            composable(Screen.EmailLogin.route) {
                EmailLoginScreen(onNavigateBack = { authViewModel.clearError(); navController.popBackStack() },
                    onLoginClick = authViewModel::loginWithEmail,
                    onPasswordResetRequest = authViewModel::sendPasswordResetEmail,
                    isLoading = state.isLoading, errorMessage = state.errorMessage,
                    successMessage = state.message)
            }
            composable(Screen.OnboardingStep1.route) {
                user?.let {
                    OnboardingStep1Screen(user = it,
                        onNavigateBack = { authViewModel.logout() },
                        onContinueClick = { n, l, d, p, t ->
                            authViewModel.saveStep1PersonalData(n,l,d,p,t) { navController.navigate(Screen.OnboardingStep2.route) }
                        }, isLoading = state.isLoading, errorMessage = state.errorMessage)
                }
            }
            composable(Screen.OnboardingStep2.route) {
                user?.let {
                    OnboardingStep2Screen(currentVehicle = it.vehicleType,
                        onNavigateBack = { authViewModel.clearError(); navController.navigate(Screen.OnboardingStep1.route) { launchSingleTop = true } },
                        onContinueClick = { vehicle -> authViewModel.saveStep2VehicleType(vehicle) { navController.navigate(Screen.OnboardingStep3.route) } },
                        isLoading = state.isLoading, errorMessage = state.errorMessage)
                }
            }
            composable(Screen.OnboardingStep3.route) {
                user?.let {
                    OnboardingStep3Screen(vehicleType = it.vehicleType, initialBankInfo = it.bankInfo,
                        onNavigateBack = { authViewModel.clearError(); navController.navigate(Screen.OnboardingStep2.route) { launchSingleTop = true } },
                        onDocumentPick = authViewModel::uploadDocument,
                        onDocumentView = { type -> authViewModel.openDocument(type) { uri ->
                            try { context.startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(uri, context.contentResolver.getType(uri)).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)) }
                            catch (_: Exception) { authViewModel.reportError("No hay una aplicación disponible para abrir este archivo") }
                        } },
                        onSaveBank = authViewModel::saveBank,
                        onFinishRegistrationClick = { b,h,a,c -> authViewModel.finishRegistration(b,h,a,c) {
                            navController.navigate(Screen.RegistrationStatus.route) { popUpTo(navController.graph.id) { inclusive = true } }
                        } },
                        documentsMap = state.documentsMap, isLoading = state.isLoading,
                        errorMessage = state.errorMessage, successMessage = state.message)
                }
            }
            composable(Screen.RegistrationStatus.route) {
                user?.let {
                    RegistrationStatusScreen(user = it,
                        onResumeRegistrationClick = { authViewModel.clearError(); navController.navigate(Screen.OnboardingStep1.route) },
                        onGoToHomeClick = { if (it.status == RegistrationStatus.APPROVED && it.isEnabled) navController.navigate(Screen.Home.route) },
                        onLogoutClick = { authViewModel.logout() },
                        onRefresh = authViewModel::checkCurrentUser,
                        onConfigurePassword = { authViewModel.clearError(); passwordDialog = true },
                        isLoading = state.isLoading, errorMessage = state.errorMessage)
                }
            }
            composable(Screen.Home.route) {
                // HU04: Home con disponibilidad. El módulo de pedidos sigue sin existir.
                user?.let {
                    HomeScreen(user = it,
                        onAvailabilityChange = { available -> authViewModel.setAvailability(available) },
                        onError = authViewModel::reportError,
                        onConfigurePassword = { passwordDialog = true; authViewModel.clearError() },
                        onNotificationsClick = { navController.navigate(Screen.Notifications.route) },
                        onLogout = { authViewModel.logout() },
                        isLoading = state.isLoading, errorMessage = state.errorMessage,
                        ordersState = ordersState, onSimulateOffer = { ordersViewModel.simulateOffer() })
                }
            }
            composable(Screen.Profile.route) {
                user?.let {
                    ProfileScreen(user = it,
                        onPersonalDataClick = { authViewModel.clearError(); navController.navigate(Screen.ProfilePersonalData.route) },
                        onVehicleClick = { authViewModel.clearError(); navController.navigate(Screen.ProfileVehicle.route) },
                        onDocumentsClick = { authViewModel.clearError(); navController.navigate(Screen.ProfileDocuments.route) },
                        onNotificationsClick = { authViewModel.clearError(); navController.navigate(Screen.Notifications.route) },
                        onLogoutClick = { authViewModel.logout() })
                }
            }
            composable(Screen.ProfilePersonalData.route) {
                user?.let {
                    ProfilePersonalDataScreen(user = it,
                        onNavigateBack = { authViewModel.clearError(); navController.popBackStack() },
                        onSaveClick = { n, l, d, p ->
                            authViewModel.updatePersonalData(n, l, d, p) { navController.popBackStack() }
                        },
                        isLoading = state.isLoading, errorMessage = state.errorMessage)
                }
            }
            composable(Screen.ProfileVehicle.route) {
                user?.let {
                    ProfileVehicleScreen(user = it,
                        onNavigateBack = { authViewModel.clearError(); navController.popBackStack() },
                        onSaveClick = { vehicle ->
                            authViewModel.updateVehicle(vehicle) { navController.popBackStack() }
                        },
                        isLoading = state.isLoading, errorMessage = state.errorMessage)
                }
            }
            composable(Screen.ProfileDocuments.route) {
                user?.let {
                    ProfileDocumentsScreen(user = it,
                        documentsMap = state.documentsMap,
                        onNavigateBack = { authViewModel.clearError(); navController.popBackStack() },
                        onDocumentPick = authViewModel::uploadDocument,
                        onDocumentView = { type -> authViewModel.openDocument(type) { uri ->
                            try { context.startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(uri, context.contentResolver.getType(uri)).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)) }
                            catch (_: Exception) { authViewModel.reportError("No hay una aplicación disponible para abrir este archivo") }
                        } },
                        isLoading = state.isLoading, errorMessage = state.errorMessage)
                }
            }
            composable(Screen.Notifications.route) {
                NotificationsScreen(onNavigateBack = { navController.popBackStack() })
            }
            composable(Screen.Offer.route) {
                OfferScreen(
                    state = ordersState,
                    onAccept = { ordersViewModel.accept() },
                    onReject = { ordersViewModel.reject() }
                )
            }
            composable(Screen.Orders.route) {
                OrdersScreen(state = ordersState)
            }
        }
    }
    LaunchedEffect(user?.id) {
        if (state.initialized && user == null) {
            try { google.clear(context) } catch (e: CancellationException) { throw e } catch (_: Exception) { }
        }
    }
    // HU04 - Parte 3: pushes dirigidos a este repartidor (topic por uid).
    LaunchedEffect(user?.id) {
        if (state.initialized && user != null) {
            try { FirebaseMessaging.getInstance().subscribeToTopic("rider_${user.id}") }
            catch (e: CancellationException) { throw e } catch (_: Exception) { }
        }
    }
    // HU06 - Parte 3: vincula el estado del rider al motor de ofertas.
    LaunchedEffect(user?.id, user?.isAvailable) { ordersViewModel.bindUser(user) }
    // Navega solo a la oferta cuando llega; y regresa al decidir (aceptar/
    // rechazar) o cuando la oferta expira.
    LaunchedEffect(ordersState.status, route) {
        when {
            ordersState.status == OrderUiStatus.OFFER_ACTIVE && route != Screen.Offer.route ->
                navController.navigate(Screen.Offer.route) { launchSingleTop = true }
            route == Screen.Offer.route && ordersState.status != OrderUiStatus.OFFER_ACTIVE ->
                navController.popBackStack()
        }
    }
    if (recovery) AlertDialog(onDismissRequest = { if (!state.isLoading) recovery = false },
        title = { Text("Recuperar contraseña") },
        text = { Column {
            Text("Ingresa el correo con el que configuraste tu contraseña.")
            OutlinedTextField(resetEmail, { resetEmail = it }, label = { Text("Correo electrónico") }, singleLine = true)
            state.message?.let { Text(it) }; state.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        } },
        confirmButton = { TextButton(onClick = { authViewModel.sendPasswordResetEmail(resetEmail) }, enabled = !state.isLoading) { Text("Enviar enlace") } },
        dismissButton = { TextButton(onClick = { recovery = false; authViewModel.clearError() }) { Text("Cerrar") } })
    if (passwordDialog && user != null) PasswordSetupDialog(
        email = user.email, isLoading = state.isLoading, error = state.errorMessage, message = state.message,
        onSave = authViewModel::linkPassword, onDismiss = { passwordDialog = false; authViewModel.clearError() })
}

private fun destination(user: RiderUser): String = when (user.status) {
    RegistrationStatus.INCOMPLETE -> when (RegistrationValidation.nextStep(user)) {
        1 -> Screen.OnboardingStep1.route
        2 -> Screen.OnboardingStep2.route
        else -> Screen.OnboardingStep3.route
    }
    RegistrationStatus.APPROVED -> if (user.isEnabled) Screen.Home.route else Screen.RegistrationStatus.route
    else -> Screen.RegistrationStatus.route
}
