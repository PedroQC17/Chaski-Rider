package com.example.chaskirider.di
import com.example.chaskirider.feature.notifications.data.remote.RiderTopicSubscription

import com.example.chaskirider.core.network.DeviceConnectivity
import com.example.chaskirider.feature.chat.data.ChatRepositoryImpl
import com.example.chaskirider.feature.chat.data.local.RoomChatDraftStore
import com.example.chaskirider.feature.chat.domain.ChatDraftStore
import com.example.chaskirider.feature.chat.domain.OrderChatRepository
import com.example.chaskirider.feature.documents.data.DocumentPreviewRepositoryImpl
import com.example.chaskirider.feature.orders.data.local.RoomCustomerContactStore
import com.example.chaskirider.feature.orders.data.OrderSyncRepositoryImpl
import com.example.chaskirider.feature.orders.domain.CustomerContactStore
import com.example.chaskirider.feature.orders.domain.OrderSyncRepository
import com.example.chaskirider.feature.support.data.FirebaseAssistantRepository
import com.example.chaskirider.feature.support.data.SupportRepositoryImpl
import com.example.chaskirider.feature.support.data.local.RoomSupportDraftStore
import com.example.chaskirider.feature.support.domain.AssistantRepository
import com.example.chaskirider.feature.support.domain.SupportDraftStore
import com.example.chaskirider.feature.support.domain.SupportRepository
import com.example.chaskirider.feature.user.data.local.RoomRegistrationDraftRepository
import com.example.chaskirider.data.local.openChaskiDatabase
import android.content.Context
import com.example.chaskirider.core.platform.location.DeviceLocationRepository
import com.example.chaskirider.core.text.AndroidTextProvider
import com.example.chaskirider.core.text.TextProvider
import com.example.chaskirider.feature.notifications.data.remote.NotificationsInitializer
import com.example.chaskirider.feature.documents.data.local.RiderDocumentDataSource
import com.example.chaskirider.feature.documents.data.remote.DocumentRemote
import com.example.chaskirider.feature.earnings.data.EarningsRepositoryImpl
import com.example.chaskirider.feature.orders.data.local.DemoOrderCatalog
import com.example.chaskirider.feature.orders.data.remote.DeliveryPinRemote
import com.example.chaskirider.feature.orders.data.OrderRepositoryImpl
import com.example.chaskirider.platform.OrderSyncWorker
import com.example.chaskirider.feature.orders.data.local.RoomDemoOrderStore
import com.example.chaskirider.feature.user.data.remote.RiderUserRemote
import com.example.chaskirider.feature.user.data.RiderUserRepositoryImpl
import com.example.chaskirider.feature.documents.data.DocumentRepositoryImpl
import com.example.chaskirider.feature.notifications.data.NotificationsRepositoryImpl
import com.example.chaskirider.feature.user.data.local.RiderUserSessionStore
import com.example.chaskirider.feature.earnings.domain.EarningsRepository
import com.example.chaskirider.feature.orders.domain.OfferRepository
import com.example.chaskirider.core.platform.location.LocationRepository
import com.example.chaskirider.feature.user.domain.RiderSession
import com.example.chaskirider.feature.bank.data.BankRepositoryImpl
import com.example.chaskirider.feature.bank.data.remote.BankRemote
import com.example.chaskirider.feature.bank.domain.BankRepository
import com.example.chaskirider.feature.vehicle.data.VehicleRepositoryImpl
import com.example.chaskirider.feature.vehicle.data.remote.VehicleRemote
import com.example.chaskirider.feature.vehicle.domain.VehicleRepository
import com.example.chaskirider.feature.user.domain.RiderUserRepository
import com.example.chaskirider.feature.user.domain.RegistrationDraftRepository
import com.example.chaskirider.feature.documents.domain.DocumentRepository
import com.example.chaskirider.feature.documents.domain.DocumentPreviewRepository
import com.example.chaskirider.feature.notifications.domain.NotificationsRepository

object AppContainer {
    private lateinit var applicationContext: Context
    fun initialize(context: Context) {
        val firstInitialization = !::applicationContext.isInitialized
        applicationContext = context.applicationContext
        NotificationsInitializer().initialize(applicationContext)
        if (firstInitialization) com.google.firebase.auth.FirebaseAuth.getInstance().addAuthStateListener { auth ->
            auth.currentUser?.uid?.let { com.example.chaskirider.platform.OrderSyncWorker.schedule(applicationContext, it) }
        }
    }
    private val riderTopics by lazy { com.example.chaskirider.feature.notifications.data.remote.RiderTopicSubscription() }
    private val sessionStore = RiderUserSessionStore { uid -> riderTopics.bind(uid) }
    val riderSession: RiderSession = sessionStore
    val orderChanges = kotlinx.coroutines.flow.MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val offerNotifier by lazy { com.example.chaskirider.platform.OfferNotifier(applicationContext) }
    private val chaskiDatabase by lazy { openChaskiDatabase(applicationContext) }
    val registrationDraftRepository: RegistrationDraftRepository by lazy {
        com.example.chaskirider.feature.user.data.local.RoomRegistrationDraftRepository(chaskiDatabase.registrationDrafts())
    }

    val customerAbsenceService by lazy {
        com.example.chaskirider.feature.orders.data.remote.FirebaseCustomerAbsence(chaskiDatabase.orders()) {
            OrderSyncWorker.schedule(applicationContext, it)
        }
    }
    private val connectivity by lazy { com.example.chaskirider.core.network.DeviceConnectivity(applicationContext) }
    val orderSyncRepository: com.example.chaskirider.feature.orders.domain.OrderSyncRepository by lazy {
        com.example.chaskirider.feature.orders.data.OrderSyncRepositoryImpl(applicationContext, chaskiDatabase.orders(), connectivity)
    }
    val localEarningsRepository by lazy {
        EarningsRepositoryImpl(
            com.example.chaskirider.feature.orders.data.local.RoomEarningsOrderSource(chaskiDatabase.orders())
        )
    }
    val earningsRepository: EarningsRepository by lazy {
        localEarningsRepository
    }

    // Repositorio de demostraciÃ³n (Datos simulados locales para test)
    val demoOfferRepository: OfferRepository by lazy {
        OrderRepositoryImpl(
            store = RoomDemoOrderStore(chaskiDatabase.orders()) {
                OrderSyncWorker.schedule(applicationContext, it)
            },
            currentUid = { riderSession.user.value?.id },
            catalog = {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    applicationContext.assets.open("demo_orders_lima.json").bufferedReader().use {
                        DemoOrderCatalog.parse(it.readText())
                    }
                }
            },
            verifyPin = DeliveryPinRemote::verify,
            resolveAbsence = customerAbsenceService::resolve
        )
    }

    val offerRepository: OfferRepository by lazy {
        demoOfferRepository
    }
    val supportRepository: com.example.chaskirider.feature.support.domain.SupportRepository by lazy {
        com.example.chaskirider.feature.support.data.SupportRepositoryImpl()
    }
    val assistantRepository: com.example.chaskirider.feature.support.domain.AssistantRepository by lazy {
        com.example.chaskirider.feature.support.data.FirebaseAssistantRepository(supportRepository, customerAbsenceService::awaitSync)
    }
    val orderChatRepository: com.example.chaskirider.feature.chat.domain.OrderChatRepository by lazy {
        com.example.chaskirider.feature.chat.data.ChatRepositoryImpl()
    }
    val chatDraftStore: com.example.chaskirider.feature.chat.domain.ChatDraftStore by lazy {
        com.example.chaskirider.feature.chat.data.local.RoomChatDraftStore(chaskiDatabase.chatDrafts())
    }
    val supportDraftStore: com.example.chaskirider.feature.support.domain.SupportDraftStore by lazy {
        com.example.chaskirider.feature.support.data.local.RoomSupportDraftStore(chaskiDatabase.supportDrafts())
    }
    val customerContactStore: com.example.chaskirider.feature.orders.domain.CustomerContactStore by lazy {
        com.example.chaskirider.feature.orders.data.local.RoomCustomerContactStore(chaskiDatabase.contacts())
    }
    val textProvider: TextProvider by lazy { AndroidTextProvider(applicationContext) }
    private val profiles by lazy { RiderUserRemote(textProvider, sessionStore, online = connectivity::isOnline) }
    private val documents by lazy {
        RiderDocumentDataSource(textProvider, applicationContext,
            DocumentRemote(textProvider, online = connectivity::isOnline), refreshRiderUser)
    }
    val riderUserRepository: RiderUserRepository by lazy {
        RiderUserRepositoryImpl(applicationContext, textProvider, profiles, sessionStore, notificationsRepository)
    }
    private val refreshRiderUser: suspend () -> Unit = { riderUserRepository.refresh() }
    private val currentUid: () -> String? = {
        com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
    }
    private val bankLocal by lazy {
        com.example.chaskirider.feature.bank.data.local.RoomBankLocal(chaskiDatabase.bankCache())
    }
    val bankRepository: BankRepository by lazy {
        BankRepositoryImpl(textProvider, BankRemote(textProvider), bankLocal, currentUid, refreshRiderUser)
    }
    private val vehicleLocal by lazy {
        com.example.chaskirider.feature.vehicle.data.local.RoomVehicleLocal(chaskiDatabase.vehicleCache())
    }
    val vehicleRepository: VehicleRepository by lazy {
        VehicleRepositoryImpl(textProvider, VehicleRemote(textProvider), vehicleLocal, currentUid, refreshRiderUser)
    }
    val locationRepository: LocationRepository by lazy { DeviceLocationRepository(applicationContext) }
    val documentPreviewRepository: DocumentPreviewRepository by lazy { com.example.chaskirider.feature.documents.data.DocumentPreviewRepositoryImpl(applicationContext, textProvider) }
    val documentRepository: DocumentRepository by lazy { DocumentRepositoryImpl(documents) }
    val notificationsRepository: NotificationsRepository by lazy { NotificationsRepositoryImpl() }
}


