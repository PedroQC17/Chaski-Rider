package com.example.chaskirider.navigation
import com.example.chaskirider.navigation.components.WorkSidebar

import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.unit.dp
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
import androidx.compose.foundation.verticalScroll
import androidx.core.content.ContextCompat
import androidx.credentials.exceptions.GetCredentialCancellationException
import coil.compose.AsyncImage
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.createSavedStateHandle
import com.example.chaskirider.ui.components.ChaskiDialog
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.example.chaskirider.R
import com.example.chaskirider.di.AppContainer
import com.example.chaskirider.feature.rider.domain.Rider
import com.example.chaskirider.feature.rider.domain.RiderOnboardingService
import com.example.chaskirider.feature.rider.domain.RiderStatus
import com.example.chaskirider.feature.rider.ui.platform.GoogleSignInClient
import com.example.chaskirider.feature.rider.ui.screens.PasswordSetupDialog
import com.example.chaskirider.feature.rider.ui.screens.SessionLoadingScreen
import com.example.chaskirider.feature.rider.ui.viewmodel.AuthViewModel
import com.example.chaskirider.feature.rider.ui.viewmodel.ProfilePhotoViewModel
import com.example.chaskirider.feature.rider.ui.viewmodel.RiderOnboardingReviewViewModel
import com.example.chaskirider.feature.rider.ui.viewmodel.RiderOnboardingViewModel
import com.example.chaskirider.feature.rider.ui.viewmodel.RiderProfileViewModel
import com.example.chaskirider.feature.rider.ui.navigation.authGraph
import com.example.chaskirider.feature.rider.ui.navigation.onboardingGraph
import com.example.chaskirider.feature.rider.ui.navigation.profileGraph
import com.example.chaskirider.dashboard.viewmodel.HomeViewModel
import com.example.chaskirider.dashboard.navigation.homeGraph
import com.example.chaskirider.feature.orders.ui.viewmodel.OrdersViewModel
import com.example.chaskirider.feature.orders.ui.navigation.ordersGraph
import com.example.chaskirider.feature.support.ui.viewmodel.SupportViewModel
import com.example.chaskirider.feature.support.domain.SupportOrderContext
import com.example.chaskirider.feature.support.ui.navigation.supportGraph
import com.example.chaskirider.feature.earnings.ui.viewmodel.EarningsViewModel
import com.example.chaskirider.feature.earnings.ui.navigation.earningsGraph
import com.example.chaskirider.feature.documents.ui.viewmodel.DocumentsViewModel
import com.example.chaskirider.feature.documents.ui.screens.DocumentPreviewHost
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect

@Composable
fun AppNavigation(navController: NavHostController = rememberNavController(), authViewModel: AuthViewModel = viewModel { AuthViewModel(AppContainer.textProvider, AppContainer.riderUserRepository, AppContainer.riderSession) }) {

    val state by authViewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val homeViewModel: HomeViewModel = viewModel { HomeViewModel(AppContainer.riderUserRepository,
        AppContainer.locationRepository, AppContainer.textProvider, AppContainer.offerRepository,
        AppContainer.riderSession, com.example.chaskirider.platform.DemoDemandForecast(context.applicationContext.assets)::zones) }
    val homeState by homeViewModel.uiState.collectAsStateWithLifecycle()
    val drawerState = rememberDrawerState(DrawerValue.Closed)

    val profileViewModel: RiderProfileViewModel = viewModel { RiderProfileViewModel(AppContainer.riderSession) }
    val personalViewModel: RiderOnboardingViewModel = viewModel { RiderOnboardingViewModel(
        AppContainer.riderUserRepository, AppContainer.bankRepository, AppContainer.vehicleRepository,
        AppContainer.documentRepository, AppContainer.riderSession, AppContainer.textProvider,
        AppContainer.registrationDraftRepository) }
    val personalState by personalViewModel.uiState.collectAsStateWithLifecycle()
    val reviewViewModel: RiderOnboardingReviewViewModel = viewModel { RiderOnboardingReviewViewModel(
        AppContainer.riderUserRepository, AppContainer.documentRepository, AppContainer.riderSession,
        AppContainer.textProvider) }
    val reviewState by reviewViewModel.uiState.collectAsStateWithLifecycle()
    val profileState by profileViewModel.uiState.collectAsStateWithLifecycle()
    val documentsViewModel: DocumentsViewModel = viewModel { DocumentsViewModel(AppContainer.textProvider, AppContainer.documentRepository, AppContainer.riderSession, AppContainer.documentPreviewRepository,
        AppContainer.registrationDraftRepository) }
    val documentsState by documentsViewModel.uiState.collectAsStateWithLifecycle()
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

    val photoViewModel: ProfilePhotoViewModel = viewModel { ProfilePhotoViewModel(AppContainer.riderUserRepository,
        AppContainer.riderSession, AppContainer.textProvider, createSavedStateHandle()) }
    val photoState by photoViewModel.uiState.collectAsStateWithLifecycle()
    val needsPhoto = user?.status == RiderStatus.APPROVED && user.isEnabled && user.profilePhotoPath.isBlank()

    LaunchedEffect(state.initialized, user?.id, user?.status, user?.isEnabled, user?.profilePhotoPath, route) {
        if (needsPhoto && route != Screen.ProfilePhoto.route) {
            navController.navigate(Screen.ProfilePhoto.route) { popUpTo(navController.graph.id) { inclusive = true }; launchSingleTop = true }
        } else if (user != null && route == Screen.ProfilePhoto.route && !needsPhoto) {
            navController.navigate(destination(user)) { popUpTo(navController.graph.id) { inclusive = true }; launchSingleTop = true }
        } else if (user != null && (!user.isEnabled || user.status !in listOf(RiderStatus.INCOMPLETE, RiderStatus.NEEDS_CORRECTION)) && route?.startsWith("onboarding_") == true) {
            navController.navigate(destination(user)) { popUpTo(navController.graph.id) { inclusive = true }; launchSingleTop = true }
        } else if (user != null && (user.status != RiderStatus.APPROVED || !user.isEnabled) && route in listOf(
                Screen.Home.route, Screen.Orders.route, Screen.Profile.route,
                Screen.ProfilePersonalData.route, Screen.ProfileVehicle.route, Screen.ProfileDocuments.route,
                Screen.Earnings.route)) {
            navController.navigate(Screen.RiderStatus.route) { popUpTo(navController.graph.id) { inclusive = true }; launchSingleTop = true }
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

    val workArea = user?.status == RiderStatus.APPROVED && user.isEnabled && !needsPhoto
    // HU04: las ofertas (notificación de tiempo y navegación) solo existen conectado.
    val offersActive = workArea && user?.isAvailable == true
    val demoOrdersViewModel: OrdersViewModel = viewModel(key = "demo_orders") {
        OrdersViewModel(AppContainer.demoOfferRepository, AppContainer.riderSession, automaticSearch = false,
            synchronization = AppContainer.orderSyncRepository, evidenceAnalyzer = AppContainer.evidenceAnalyzer)
    }
    val ordersState by demoOrdersViewModel.uiState.collectAsStateWithLifecycle()
    val orderSnapshot = ordersState.snapshot
    val showDemand = workArea && orderSnapshot?.offer == null &&
        (orderSnapshot == null || orderSnapshot.batch == null || orderSnapshot.isCompleted)
    val trackingOrderId = ordersState.snapshot?.batch?.stops?.singleOrNull()?.orderId
    val shouldTrack = workArea && ordersState.snapshot?.pickedUp == true && ordersState.snapshot?.isCompleted != true
    val trackingUserId = user?.id
    androidx.lifecycle.compose.LifecycleStartEffect(trackingUserId, trackingOrderId, shouldTrack) {
        if (shouldTrack && trackingOrderId != null && trackingUserId != null)
            com.example.chaskirider.platform.OrderTrackingService.start(context, trackingUserId, trackingOrderId)
        else com.example.chaskirider.platform.OrderTrackingService.stop(context)
        onStopOrDispose { }
    }
    val supportViewModel: SupportViewModel = viewModel {
        SupportViewModel(AppContainer.supportRepository, AppContainer.supportDraftStore, AppContainer.riderSession,
            AppContainer.locationRepository)
    }
    val earningsViewModel: EarningsViewModel = viewModel {
        EarningsViewModel(AppContainer.earningsRepository, AppContainer.riderSession)
    }
    fun activeSupportOrder(): SupportOrderContext? {
        val snapshot = ordersState.snapshot ?: return null
        val stop = snapshot.batch?.stops?.firstOrNull() ?: return null
        if (snapshot.isCompleted) return null
        return SupportOrderContext(stop.orderId, snapshot.merchantName.orEmpty(), snapshot.merchantAddress.orEmpty(),
            stop.address.orEmpty(), snapshot.pickedUp, snapshot.problem?.id,
            com.example.chaskirider.feature.orders.domain.OrderRecord.from(user?.id.orEmpty(), snapshot)!!.status,
            snapshot.problem?.reason)
    }
    fun openOrderSupport() {
        val order = activeSupportOrder() ?: return
        val problem = ordersState.snapshot?.problem
        supportViewModel.openForOrder(order, problem?.reason, problem?.detail)
        navController.navigate(Screen.Support.route) { launchSingleTop = true }
    }
    fun openOrderHelp(reason: com.example.chaskirider.feature.orders.domain.OrderProblemReason) {
        val order = activeSupportOrder() ?: return
        supportViewModel.openForOrder(order, reason, null)
        navController.navigate(Screen.Support.route) { launchSingleTop = true }
    }
    fun openSupportTicket(id: String) {
        supportViewModel.openTicket(id)
        navController.navigate(Screen.Support.route) { launchSingleTop = true }
    }
    LaunchedEffect(ordersState.snapshot) { supportViewModel.setAvailableOrder(activeSupportOrder()) }
    val searchingForOffers = offersActive && ordersState.snapshot?.batch == null
    androidx.lifecycle.compose.LifecycleStartEffect(demoOrdersViewModel, searchingForOffers, user?.id) {
        demoOrdersViewModel.setForeground(true)
        if (searchingForOffers) com.example.chaskirider.platform.OrderSearchService.start(context)
        else com.example.chaskirider.platform.OrderSearchService.stop(context)
        onStopOrDispose { demoOrdersViewModel.setForeground(false) }
    }
    androidx.lifecycle.compose.LifecycleStartEffect(demoOrdersViewModel, "order_updates") {
        val job = scope.launch { AppContainer.orderChanges.collect { demoOrdersViewModel.refresh() } }
        onStopOrDispose { job.cancel() }
    }
    LaunchedEffect(ordersState.snapshot?.offer?.id, ordersState.snapshot?.batch, route, workArea, offersActive) {
        val snapshot = ordersState.snapshot
        if (workArea && route == Screen.Home.route && ((snapshot?.offer != null && offersActive) || (snapshot?.batch != null && !snapshot.isCompleted))) {
            navController.navigate(Screen.Orders.route) { launchSingleTop = true }
        }
    }
    LaunchedEffect(route, showDemand, orderSnapshot?.batch) {
        if (route == Screen.Orders.route && showDemand && orderSnapshot?.batch == null) {
            navController.navigate(Screen.Home.route) {
                popUpTo(Screen.Orders.route) { inclusive = true }
                launchSingleTop = true
            }
        }
    }
    LaunchedEffect(workArea) { if (!workArea) drawerState.close() }
    ModalNavigationDrawer(drawerState = drawerState, gesturesEnabled = false,
        drawerContent = {
            if (workArea) WorkSidebar(
                route = route,
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
            authGraph(
                navController = navController,
                state = state,
                busy = state.isLoading || googleBusy,
                viewModel = authViewModel,
                onGoogleSignIn = {
                    if (!googleBusy && !state.isLoading) {
                        googleBusy = true
                        authViewModel.clearError()
                        scope.launch {
                            try { authViewModel.loginWithGoogle(google.getIdToken(context)) }
                            catch (_: GetCredentialCancellationException) { }
                            catch (e: CancellationException) { throw e }
                            catch (_: Exception) { authViewModel.reportError(context.getString(R.string.text_no_se_pudo_iniciar_sesion_con_google)) }
                            finally { googleBusy = false }
                        }
                    }
                },
                onAccountRecovery = { recovery = true }
            )
            onboardingGraph(
                navController = navController,
                user = user,
                personalState = personalState,
                reviewState = reviewState,
                documentsState = documentsState,
                personalViewModel = personalViewModel,
                documentsViewModel = documentsViewModel,
                reviewViewModel = reviewViewModel,
                photoViewModel = photoViewModel,
                needsPhoto = needsPhoto,
                onLogout = authViewModel::logout,
                onDocumentCamera = { type ->
                    pendingCameraType = type
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) openDocumentCamera(type)
                    else documentCameraPermission.launch(Manifest.permission.CAMERA)
                },
                onSupport = { navController.navigate(Screen.Support.route) { launchSingleTop = true } },
                onGoHome = { user?.let { navController.navigate(destination(it)) } }
            )
            ordersGraph(
                workArea = workArea,
                viewModel = demoOrdersViewModel,
                onMenu = { scope.launch { drawerState.open() } },
                onSupport = ::openOrderSupport,
                onOrderHelp = ::openOrderHelp,
                onTicket = ::openSupportTicket
            )
            homeGraph(
                workArea = workArea,
                viewModel = homeViewModel,
                showDemand = showDemand,
                onOpenMenu = { scope.launch { drawerState.open() } }
            )
            profileGraph(
                navController = navController,
                user = user,
                photoUri = photoState.profileUri,
                profileViewModel = profileViewModel,
                documentsState = documentsState,
                documentsViewModel = documentsViewModel,
                onOpenMenu = { scope.launch { drawerState.open() } },
                onLogout = authViewModel::logout,
                onSupport = { navController.navigate(Screen.Support.route) { launchSingleTop = true } },
                onPasswordSetup = { authViewModel.clearError(); passwordDialog = true }
            )
            supportGraph(
                user = user,
                viewModel = supportViewModel,
                onBack = { navController.popBackStack() }
            )
            earningsGraph(
                workArea = workArea,
                viewModel = earningsViewModel,
                onMenu = { scope.launch { drawerState.open() } }
            )
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
            text = { androidx.compose.foundation.layout.Column(androidx.compose.ui.Modifier.heightIn(max = androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp.dp * 0.55f).verticalScroll(androidx.compose.foundation.rememberScrollState())) {
                AsyncImage(model = capture, contentDescription = null, modifier = Modifier.fillMaxWidth())
                if (documentsState.isLoading) LinearProgressIndicator(Modifier.fillMaxWidth())
                documentsState.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            } },
            confirmButton = { TextButton(onClick = documentsViewModel::confirmCapture, enabled = !documentsState.isLoading) { Text(stringResource(R.string.profile_photo_save)) } },
            dismissButton = { TextButton(onClick = documentsViewModel::discardCapture, enabled = !documentsState.isLoading) { Text(stringResource(R.string.text_descartar)) } })
    }
    if (profileState.changeNoticeVisible) ChaskiDialog(
        title = stringResource(R.string.profile_change_title), onDismiss = profileViewModel::dismissChangeNotice,
        content = { Text(stringResource(R.string.profile_change_body)) },
        confirm = { Button(onClick = {
            profileViewModel.dismissChangeNotice()
            navController.navigate(Screen.Support.route) { launchSingleTop = true }
        }) { Text(stringResource(R.string.profile_change_help)) } })
    if (recovery) AlertDialog(onDismissRequest = { if (!state.isLoading) recovery = false },
        title = { Text(stringResource(R.string.text_recuperar_contrasena)) },
        text = { androidx.compose.foundation.layout.Column(androidx.compose.ui.Modifier.heightIn(max = androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp.dp * 0.55f).verticalScroll(androidx.compose.foundation.rememberScrollState())) {
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

private fun destination(user: Rider): String = if (!user.isEnabled) Screen.RiderStatus.route else when (user.status) {
    RiderStatus.INCOMPLETE -> when (RiderOnboardingService.nextStep(user)) {
        1 -> Screen.OnboardingStep1.route
        2 -> Screen.OnboardingStep2.route
        else -> Screen.OnboardingStep3.route
    }
    RiderStatus.APPROVED -> when {
        !user.isEnabled -> Screen.RiderStatus.route
        user.profilePhotoPath.isBlank() -> Screen.ProfilePhoto.route
        else -> Screen.Home.route
    }
    else -> Screen.RiderStatus.route
}
