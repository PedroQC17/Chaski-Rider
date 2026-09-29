
package com.example.chaskirider.ui.navigation

import com.example.chaskirider.R
import androidx.compose.ui.res.stringResource
import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.*
import androidx.navigation.NavHostController
import com.example.chaskirider.ui.platform.GoogleSignInClient
import com.example.chaskirider.di.AppContainer
import com.example.chaskirider.domain.model.*
import com.example.chaskirider.ui.components.AppBottomBar
import com.example.chaskirider.ui.screens.auth.*
import com.example.chaskirider.ui.screens.home.*
import com.example.chaskirider.ui.screens.notifications.*
import com.example.chaskirider.ui.screens.onboarding.*
import com.example.chaskirider.ui.screens.profile.*
import com.example.chaskirider.ui.screens.profile.documents.DocumentsViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
fun AppNavigation(navController: NavHostController = rememberNavController(), authViewModel: AuthViewModel = viewModel { AuthViewModel(AppContainer.textProvider, AppContainer.authRepository, AppContainer.riderSession) }) {

    val state by authViewModel.uiState.collectAsStateWithLifecycle()

    val profileViewModel: RiderProfileViewModel = viewModel { RiderProfileViewModel(AppContainer.textProvider, AppContainer.riderProfileRepository, AppContainer.riderSession) }
    val profileState by profileViewModel.uiState.collectAsStateWithLifecycle()
    val documentsViewModel: DocumentsViewModel = viewModel { DocumentsViewModel(AppContainer.textProvider, AppContainer.documentRepository, AppContainer.riderSession) }
    val documentsState by documentsViewModel.uiState.collectAsStateWithLifecycle()
    val notificationsViewModel: NotificationsViewModel = viewModel { NotificationsViewModel(AppContainer.notificationsRepository) }
    val notificationsState by notificationsViewModel.uiState.collectAsStateWithLifecycle()
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

    LaunchedEffect(state.initialized, user?.id, user?.status, route) {
        if (!state.initialized) return@LaunchedEffect
        if (user != null && (route == Screen.Access.route || route == Screen.EmailLogin.route)) {
            navController.navigate(destination(user)) { popUpTo(navController.graph.id) { inclusive = true }; launchSingleTop = true }
        } else if (user == null && route != null && route !in listOf(Screen.Access.route, Screen.EmailLogin.route)) {
            navController.navigate(Screen.Access.route) { popUpTo(navController.graph.id) { inclusive = true } }
        }
    }

    val mainRoutes = listOf(Screen.Home.route, Screen.Profile.route)
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
                                catch (_: GetCredentialCancellationException) {  }
                                catch (e: CancellationException) { throw e }
                                catch (_: Exception) { authViewModel.reportError(context.getString(R.string.text_no_se_pudo_iniciar_sesion_con_google)) }
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
                            profileViewModel.saveStep1PersonalData(n,l,d,p,t) { navController.navigate(Screen.OnboardingStep2.route) }
                        }, isLoading = profileState.isLoading, errorMessage = profileState.errorMessage)
                }
            }
            composable(Screen.OnboardingStep2.route) {
                user?.let {
                    OnboardingStep2Screen(currentVehicle = it.vehicleType,
                        onNavigateBack = { profileViewModel.clearError(); navController.navigate(Screen.OnboardingStep1.route) { launchSingleTop = true } },
                        onContinueClick = { vehicle -> profileViewModel.saveStep2VehicleType(vehicle) { navController.navigate(Screen.OnboardingStep3.route) } },
                        isLoading = profileState.isLoading, errorMessage = profileState.errorMessage)
                }
            }
            composable(Screen.OnboardingStep3.route) {
                user?.let {
                    OnboardingStep3Screen(vehicleType = it.vehicleType, initialBankInfo = it.bankInfo,
                        onNavigateBack = { profileViewModel.clearError(); documentsViewModel.clearError(); navController.navigate(Screen.OnboardingStep2.route) { launchSingleTop = true } },
                        onDocumentPick = documentsViewModel::uploadDocument,
                        onDocumentView = { type -> documentsViewModel.openDocument(type) { uri ->
                            try { context.startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(uri, context.contentResolver.getType(uri)).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)) }
                            catch (_: Exception) { documentsViewModel.reportError(context.getString(R.string.text_no_hay_una_aplicacion_disponible_para_abrir)) }
                        } },
                        onSaveBank = profileViewModel::saveBank,
                        onFinishRegistrationClick = { b,h,a,c -> profileViewModel.finishRegistration(b,h,a,c) {
                            navController.navigate(Screen.RegistrationStatus.route) { popUpTo(navController.graph.id) { inclusive = true } }
                        } },
                        documentsMap = documentsState.documentsMap, isLoading = profileState.isLoading || documentsState.isLoading,
                        errorMessage = documentsState.errorMessage ?: profileState.errorMessage, successMessage = profileState.message)
                }
            }
            composable(Screen.RegistrationStatus.route) {
                user?.let {
                    RegistrationStatusScreen(user = it,
                        onResumeRegistrationClick = { profileViewModel.clearError(); navController.navigate(Screen.OnboardingStep1.route) },
                        onGoToHomeClick = { if (it.status == RegistrationStatus.APPROVED && it.isEnabled) navController.navigate(Screen.Home.route) },
                        onLogoutClick = { authViewModel.logout() },
                        onRefresh = authViewModel::checkCurrentUser,
                        onConfigurePassword = { authViewModel.clearError(); passwordDialog = true },
                        isLoading = state.isLoading, errorMessage = state.errorMessage)
                }
            }
            composable(Screen.Home.route) {
                user?.let {
                    HomeScreen(user = it,
                        onAvailabilityChange = { available -> profileViewModel.setAvailability(available) },
                        onError = profileViewModel::reportError,
                        onConfigurePassword = { passwordDialog = true; authViewModel.clearError() },
                        onNotificationsClick = { navController.navigate(Screen.Notifications.route) },
                        onLogout = { authViewModel.logout() },
                        isLoading = profileState.isLoading, errorMessage = profileState.errorMessage,
                        unreadCount = notificationsState.unreadCount)
                }
            }
            composable(Screen.Profile.route) {
                user?.let {
                    ProfileScreen(user = it,
                        onPersonalDataClick = { profileViewModel.clearError(); navController.navigate(Screen.ProfilePersonalData.route) },
                        onVehicleClick = { profileViewModel.clearError(); navController.navigate(Screen.ProfileVehicle.route) },
                        onDocumentsClick = { profileViewModel.clearError(); navController.navigate(Screen.ProfileDocuments.route) },
                        onNotificationsClick = { profileViewModel.clearError(); navController.navigate(Screen.Notifications.route) },
                        onLogoutClick = { authViewModel.logout() })
                }
            }
            composable(Screen.ProfilePersonalData.route) {
                user?.let {
                    ProfilePersonalDataScreen(user = it,
                        onNavigateBack = { profileViewModel.clearError(); navController.popBackStack() },
                        onSaveClick = { n, l, d, p ->
                            profileViewModel.updatePersonalData(n, l, d, p) { navController.popBackStack() }
                        },
                        isLoading = profileState.isLoading, errorMessage = profileState.errorMessage)
                }
            }
            composable(Screen.ProfileVehicle.route) {
                user?.let {
                    ProfileVehicleScreen(user = it,
                        onNavigateBack = { profileViewModel.clearError(); navController.popBackStack() },
                        onSaveClick = { vehicle ->
                            profileViewModel.updateVehicle(vehicle) { navController.popBackStack() }
                        },
                        isLoading = profileState.isLoading, errorMessage = profileState.errorMessage)
                }
            }
            composable(Screen.ProfileDocuments.route) {
                user?.let {
                    ProfileDocumentsScreen(user = it,
                        onPrepareCapture = documentsViewModel::prepareCapture,
                        documentsMap = documentsState.documentsMap,
                        onNavigateBack = { profileViewModel.clearError(); documentsViewModel.clearError(); navController.popBackStack() },
                        onDocumentPick = documentsViewModel::uploadDocument,
                        onDocumentView = { type -> documentsViewModel.openDocument(type) { uri ->
                            try { context.startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(uri, context.contentResolver.getType(uri)).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)) }
                            catch (_: Exception) { documentsViewModel.reportError(context.getString(R.string.text_no_hay_una_aplicacion_disponible_para_abrir)) }
                        } },
                        isLoading = profileState.isLoading || documentsState.isLoading, errorMessage = documentsState.errorMessage ?: profileState.errorMessage)
                }
            }
            composable(Screen.Notifications.route) {
                LaunchedEffect(Unit) { notificationsViewModel.markSeen() }
                NotificationsScreen(state = notificationsState, onNavigateBack = { navController.popBackStack() })
            }

        }
    }
    LaunchedEffect(state.initialized, user?.id) {
        if (state.initialized && user == null) {
            try { google.clear(context) } catch (e: CancellationException) { throw e } catch (_: Exception) { }
        }
    }


    if (recovery) AlertDialog(onDismissRequest = { if (!state.isLoading) recovery = false },
        title = { Text(stringResource(R.string.text_recuperar_contrasena)) },
        text = { Column {
            Text(stringResource(R.string.text_ingresa_el_correo_con_el_que_configuraste))
            OutlinedTextField(resetEmail, { resetEmail = it }, label = { Text(stringResource(R.string.text_correo_electronico)) }, singleLine = true)
            state.message?.let { Text(it) }; state.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        } },
        confirmButton = { TextButton(onClick = { authViewModel.sendPasswordResetEmail(resetEmail) }, enabled = !state.isLoading) { Text(stringResource(R.string.text_enviar_enlace)) } },
        dismissButton = { TextButton(onClick = { recovery = false; authViewModel.clearError() }) { Text(stringResource(R.string.text_cerrar)) } })
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
