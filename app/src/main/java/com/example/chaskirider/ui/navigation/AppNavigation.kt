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
import com.example.chaskirider.domain.model.*
import com.example.chaskirider.ui.screens.auth.*
import com.example.chaskirider.ui.screens.onboarding.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
fun AppNavigation(navController: NavHostController = rememberNavController(), authViewModel: AuthViewModel = viewModel()) {
    val state by authViewModel.uiState.collectAsState()
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

    NavHost(navController, startDestination = Screen.Access.route) {
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
            // The project has no orders screen yet. Do not invent operational functionality.
            Column(Modifier.fillMaxSize().systemBarsPadding().padding(24.dp)) {
                Text("Mi cuenta", style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(20.dp))
                Text(if (user?.status == RegistrationStatus.APPROVED && user.isEnabled)
                    "Tu registro está aprobado. El módulo de pedidos aún no está implementado en este proyecto."
                    else "Tu cuenta aún no está habilitada para recibir pedidos.")
                TextButton(onClick = { passwordDialog = true; authViewModel.clearError() }) { Text("Configurar contraseña") }
                TextButton(onClick = { authViewModel.logout() }) { Text("Cerrar sesión") }
            }
        }
    }
    LaunchedEffect(user?.id) {
        if (state.initialized && user == null) {
            try { google.clear(context) } catch (e: CancellationException) { throw e } catch (_: Exception) { }
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
