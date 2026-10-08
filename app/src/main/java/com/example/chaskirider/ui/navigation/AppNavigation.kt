package com.example.chaskirider.ui.navigation

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.credentials.exceptions.GetCredentialCancellationException
import coil.compose.AsyncImage
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.createSavedStateHandle
import com.example.chaskirider.ui.screens.profile.photo.*
import com.example.chaskirider.ui.components.ChaskiDialog
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.example.chaskirider.R
import com.example.chaskirider.data.orders.OfferNotifier
import com.example.chaskirider.di.AppContainer
import com.example.chaskirider.domain.model.*
import com.example.chaskirider.ui.platform.GoogleSignInClient
import com.example.chaskirider.ui.screens.auth.*
import com.example.chaskirider.ui.screens.home.*
import com.example.chaskirider.ui.screens.notifications.*
import com.example.chaskirider.ui.screens.onboarding.*
import com.example.chaskirider.ui.screens.orders.OrdersRoute
import com.example.chaskirider.ui.screens.orders.OrdersViewModel
import com.example.chaskirider.ui.screens.profile.*
import com.example.chaskirider.ui.screens.profile.documents.DocumentsViewModel
import com.example.chaskirider.ui.screens.profile.documents.DocumentPreviewHost
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
fun AppNavigation(demoRequest: Long = 0, navController: NavHostController = rememberNavController(), authViewModel: AuthViewModel = viewModel { AuthViewModel(AppContainer.textProvider, AppContainer.authRepository, AppContainer.riderSession) }) {

    val state by authViewModel.uiState.collectAsStateWithLifecycle()
    val homeViewModel: HomeViewModel = viewModel { HomeViewModel(AppContainer.riderProfileRepository,
        AppContainer.locationRepository, AppContainer.textProvider, AppContainer.offerRepository,
        AppContainer.riderSession) }
    val homeState by homeViewModel.uiState.collectAsStateWithLifecycle()
    val drawerState = rememberDrawerState(DrawerValue.Closed)

    val profileViewModel: RiderProfileViewModel = viewModel { RiderProfileViewModel(AppContainer.textProvider, AppContainer.riderProfileRepository, AppContainer.riderSession) }
    val personalViewModel: PersonalRegistrationViewModel = viewModel { PersonalRegistrationViewModel(
        AppContainer.authRepository, AppContainer.riderProfileRepository, AppContainer.riderSession, AppContainer.textProvider) }
    val personalState by personalViewModel.uiState.collectAsStateWithLifecycle()
    val profileState by profileViewModel.uiState.collectAsStateWithLifecycle()
    val documentsViewModel: DocumentsViewModel = viewModel { DocumentsViewModel(AppContainer.textProvider, AppContainer.documentRepository, AppContainer.riderSession, AppContainer.documentPreviewRepository) }
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
    // HU03: captura de documentos con la cámara (permiso bajo demanda + vista previa antes de subir).
    var pendingCameraType by remember { mutableStateOf<String?>(null) }
    val documentCamera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture(), documentsViewModel::captured)
    fun openDocumentCamera(type: String) {
        documentsViewModel.prepareCapture(type) { uri ->
            try { documentCamera.launch(uri) }
            catch (_: android.content.ActivityNotFoundException) { documentsViewModel.discardCapture(); documentsViewModel.reportError(context.getString(R.string.profile_photo_camera_error)) }
            catch (_: SecurityException) { documentsViewModel.discardCapture(); documentsViewModel.reportError(context.getString(R.string.profile_photo_camera_error)) }
        }
    }
    val documentCameraPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) pendingCameraType?.let(::openDocumentCamera)
        else documentsViewModel.reportError(context.getString(R.string.text_permiso_de_camara_denegado_puedes_elegir_un))
    }
    val entry by navController.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    val user = state.currentUser

    if (!state.initialized) {
        SessionLoadingScreen(state, authViewModel::checkCurrentUser)
        return
    }

    val photoViewModel: ProfilePhotoViewModel = viewModel { ProfilePhotoViewModel(AppContainer.profilePhotoRepository,
        AppContainer.riderSession, AppContainer.textProvider, createSavedStateHandle()) }
    val photoState by photoViewModel.uiState.collectAsStateWithLifecycle()
    val needsPhoto = user?.status == RegistrationStatus.APPROVED && user.isEnabled && user.profilePhotoPath.isBlank()

    LaunchedEffect(state.initialized, user?.id, user?.status, user?.isEnabled, user?.profilePhotoPath, route) {
        if (needsPhoto && route != Screen.ProfilePhoto.route) {
            navController.navigate(Screen.ProfilePhoto.route) { popUpTo(navController.graph.id) { inclusive = true }; launchSingleTop = true }
        } else if (user != null && route == Screen.ProfilePhoto.route && !needsPhoto) {
            navController.navigate(destination(user)) { popUpTo(navController.graph.id) { inclusive = true }; launchSingleTop = true }
        } else if (user?.status == RegistrationStatus.APPROVED && route?.startsWith("onboarding_") == true) {
            navController.navigate(destination(user)) { popUpTo(navController.graph.id) { inclusive = true }; launchSingleTop = true }
        } else if (route == Screen.Startup.route) {
            navController.navigate(user?.let(::destination) ?: Screen.Access.route) {
                popUpTo(Screen.Startup.route) { inclusive = true }; launchSingleTop = true
            }
        } else if (user != null && (route == Screen.Access.route || route == Screen.EmailLogin.route)) {
            navController.navigate(destination(user)) { popUpTo(navController.graph.id) { inclusive = true }; launchSingleTop = true }
        } else if (user == null && route != null && route !in listOf(Screen.Access.route, Screen.EmailLogin.route)) {
            navController.navigate(Screen.Access.route) { popUpTo(navController.graph.id) { inclusive = true } }
        }
    }

    val workArea = user?.status == RegistrationStatus.APPROVED && user.isEnabled && !needsPhoto
    // HU04: las ofertas (notificación de tiempo y navegación) solo existen conectado.
    val offersActive = workArea && user?.isAvailable == true
    val realOrdersViewModel: OrdersViewModel = viewModel(key = "real_orders") {
        OrdersViewModel(AppContainer.realOfferRepository, AppContainer.riderSession)
    }
    val demoOrdersViewModel: OrdersViewModel = viewModel(key = "demo_orders") {
        OrdersViewModel(AppContainer.demoOfferRepository, AppContainer.riderSession)
    }
    val offerNotifier = remember { OfferNotifier(context.applicationContext) }
    LaunchedEffect(realOrdersViewModel, demoOrdersViewModel, offersActive) {
        if (!offersActive) { offerNotifier.update(null, 0); return@LaunchedEffect }
        launch {
            realOrdersViewModel.uiState.collect { orders ->
                if (orders.snapshot?.offer != null) {
                    offerNotifier.update(orders.snapshot?.offer?.id, orders.secondsLeft)
                }
            }
        }
        launch {
            demoOrdersViewModel.uiState.collect { orders ->
                if (orders.snapshot?.offer != null) {
                    offerNotifier.update(orders.snapshot?.offer?.id, orders.secondsLeft)
                }
            }
        }
    }
    LaunchedEffect(demoRequest, offersActive) {
        if (demoRequest > 0 && offersActive && com.example.chaskirider.BuildConfig.DEBUG) {
            navController.navigate(Screen.DemoOrders.route) { launchSingleTop = true }
        }
    }
    LaunchedEffect(workArea) { if (!workArea) drawerState.close() }
    ModalNavigationDrawer(drawerState = drawerState, gesturesEnabled = workArea,
        drawerContent = {
            if (workArea) WorkSidebar(
                route = route,
                unread = notificationsState.unreadCount,
                busy = homeState.isUpdatingAvailability || state.isLoading || profileState.isLoading || documentsState.isLoading,
                onNavigate = { target -> scope.launch {
                    drawerState.close()
                    navController.navigate(target) { popUpTo(Screen.Home.route); launchSingleTop = true }
                } },
                onLogout = { scope.launch { drawerState.close(); authViewModel.logout() } }
            )
        }) {
    Scaffold(contentWindowInsets = WindowInsets(0, 0, 0, 0)) { padding ->
        NavHost(navController, startDestination = Screen.Startup.route, modifier = Modifier.padding(padding)) {
            composable(Screen.Startup.route) { SessionLoadingScreen(state, authViewModel::checkCurrentUser) }
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
                    OnboardingStep1Screen(state = personalState,
                        onChange = personalViewModel::change, onTermsChange = personalViewModel::acceptTerms,
                        onNavigateBack = { authViewModel.logout() },
                        onContinue = { personalViewModel.save { navController.navigate(Screen.OnboardingStep2.route) } })
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
                        onDocumentCamera = { type ->
                            pendingCameraType = type
                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) openDocumentCamera(type)
                            else documentCameraPermission.launch(Manifest.permission.CAMERA)
                        },
                        onDocumentView = documentsViewModel::openDocument,
                        onFinishRegistrationClick = { b,h,a,c -> profileViewModel.finishRegistration(b,h,a,c) {
                            navController.navigate(Screen.RegistrationStatus.route) { popUpTo(navController.graph.id) { inclusive = true } }
                        } },
                        documentsMap = documentsState.documentsMap, isLoading = profileState.isLoading || documentsState.isLoading,
                        errorMessage = documentsState.errorMessage ?: profileState.errorMessage)
                }
            }
            composable(Screen.RegistrationStatus.route) {
                user?.let {
                    RegistrationStatusScreen(user = it,
                        onResumeRegistrationClick = { profileViewModel.clearError(); navController.navigate(Screen.OnboardingStep1.route) },
                        onGoToHomeClick = { if (it.status == RegistrationStatus.APPROVED && it.isEnabled) navController.navigate(destination(it)) },
                        onLogoutClick = { authViewModel.logout() },
                        onRefresh = authViewModel::checkCurrentUser,
                        isLoading = state.isLoading, errorMessage = state.errorMessage)
                }
            }
            composable(Screen.ProfilePhoto.route) {
                if (needsPhoto) ProfilePhotoRoute(photoViewModel, onLogout = authViewModel::logout)
            }
            composable(Screen.Orders.route) {
                if (workArea)
                    OrdersRoute(
                        viewModel = realOrdersViewModel,
                        onMenu = { scope.launch { drawerState.open() } },
                        isDemo = false
                    )
            }
            composable(Screen.DemoOrders.route) {
                if (workArea)
                    OrdersRoute(
                        viewModel = demoOrdersViewModel,
                        onMenu = { scope.launch { drawerState.open() } },
                        isDemo = true
                    )
            }
            composable(Screen.Home.route) {
                if (workArea) HomeRoute(homeViewModel, onOpenMenu = { scope.launch { drawerState.open() } })
            }
            composable(Screen.Profile.route) {
                user?.let {
                    ProfileScreen(user = it, photoUri = photoState.profileUri,
                        onOpenMenu = { scope.launch { drawerState.open() } },
                        onPersonalDataClick = { profileViewModel.clearError(); navController.navigate(Screen.ProfilePersonalData.route) },
                        onVehicleClick = { profileViewModel.clearError(); navController.navigate(Screen.ProfileVehicle.route) },
                        onDocumentsClick = { profileViewModel.clearError(); navController.navigate(Screen.ProfileDocuments.route) },
                        onLogoutClick = { authViewModel.logout() })
                }
            }
            composable(Screen.ProfilePersonalData.route) {
                user?.let {
                    ProfilePersonalDataScreen(user = it,
                        onNavigateBack = { profileViewModel.clearError(); navController.popBackStack() },
                        onRequestChange = profileViewModel::showChangeNotice,
                        onConfigurePassword = { authViewModel.clearError(); passwordDialog = true })
                }
            }
            composable(Screen.ProfileVehicle.route) {
                user?.let {
                    ProfileVehicleScreen(user = it,
                        onNavigateBack = { profileViewModel.clearError(); navController.popBackStack() },
                        onRequestChange = profileViewModel::showChangeNotice)
                }
            }
            composable(Screen.ProfileDocuments.route) {
                user?.let {
                    ProfileDocumentsScreen(user = it,
                        onNavigateBack = { profileViewModel.clearError(); documentsViewModel.clearError(); navController.popBackStack() },
                        onDocumentView = documentsViewModel::openDocument,
                        onRequestChange = profileViewModel::showChangeNotice)
                }
            }
            composable(Screen.Notifications.route) {
                LaunchedEffect(Unit) { notificationsViewModel.markSeen() }
                NotificationsScreen(state = notificationsState, onOpenMenu = { scope.launch { drawerState.open() } })
            }

        }
    }
    }
    LaunchedEffect(state.initialized, user?.id) {
        if (user == null) {
            try { google.clear(context) } catch (e: CancellationException) { throw e } catch (_: Exception) { }
        }
    }


    documentsState.preview?.let { preview ->
        DocumentPreviewHost(preview, documentsViewModel::dismissPreview, documentsViewModel::showPage)
    }
    documentsState.captureUri?.let { capture ->
        AlertDialog(onDismissRequest = documentsViewModel::discardCapture,
            title = { Text(stringResource(R.string.document_preview)) },
            text = { AsyncImage(model = capture, contentDescription = null, modifier = Modifier.fillMaxWidth()) },
            confirmButton = { TextButton(onClick = documentsViewModel::confirmCapture) { Text(stringResource(R.string.profile_photo_save)) } },
            dismissButton = { TextButton(onClick = documentsViewModel::discardCapture) { Text(stringResource(R.string.text_descartar)) } })
    }
    if (profileState.changeNoticeVisible) ChaskiDialog(
        title = stringResource(R.string.profile_change_title), onDismiss = profileViewModel::dismissChangeNotice,
        content = { Text(stringResource(R.string.profile_change_body)) },
        confirm = { Button(onClick = profileViewModel::dismissChangeNotice) { Text(stringResource(R.string.profile_change_close)) } })
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
        hasPassword = user.hasPassword, isLoading = state.isLoading, error = state.errorMessage, message = state.message,
        onSave = authViewModel::linkPassword, onDismiss = { passwordDialog = false; authViewModel.clearError() })
}

private fun destination(user: RiderUser): String = when (user.status) {
    RegistrationStatus.INCOMPLETE -> when (RegistrationValidation.nextStep(user)) {
        1 -> Screen.OnboardingStep1.route
        2 -> Screen.OnboardingStep2.route
        else -> Screen.OnboardingStep3.route
    }
    RegistrationStatus.APPROVED -> when {
        !user.isEnabled -> Screen.RegistrationStatus.route
        user.profilePhotoPath.isBlank() -> Screen.ProfilePhoto.route
        else -> Screen.Home.route
    }
    else -> Screen.RegistrationStatus.route
}
