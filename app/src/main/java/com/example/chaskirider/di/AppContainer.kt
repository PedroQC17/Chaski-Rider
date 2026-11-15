package com.example.chaskirider.di
import com.example.chaskirider.feature.notifications.data.remote.RiderTopicSubscription

import com.example.chaskirider.core.network.DeviceConnectivity
import com.example.chaskirider.feature.chat.data.ChatRepositoryImpl
import com.example.chaskirider.feature.chat.data.local.RoomChatMessageStore
import com.example.chaskirider.feature.chat.domain.ChatDraftStore
import com.example.chaskirider.feature.chat.domain.OrderChatRepository
import com.example.chaskirider.feature.documents.data.DocumentPreviewRepositoryImpl
import com.example.chaskirider.feature.orders.data.local.RoomCustomerContactStore
import com.example.chaskirider.feature.orders.data.OrderSyncRepositoryImpl
import com.example.chaskirider.feature.orders.domain.CustomerContactStore
import com.example.chaskirider.feature.orders.domain.OrderSyncRepository
import com.example.chaskirider.feature.support.data.FirebaseAssistantRepository
import com.example.chaskirider.feature.support.data.SupportRepositoryImpl
import com.example.chaskirider.feature.support.data.local.StoredSupportDraftStore
import com.example.chaskirider.feature.support.domain.AssistantRepository
import com.example.chaskirider.feature.support.domain.SupportDraftStore
import com.example.chaskirider.feature.support.domain.SupportRepository
import com.example.chaskirider.feature.rider.data.local.StoredRiderOnboardingDraftRepository
import com.example.chaskirider.database.openChaskiDatabase
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
import com.example.chaskirider.feature.orders.data.local.StoredOrderSessionStore
import com.example.chaskirider.feature.rider.data.remote.RiderRemote
import com.example.chaskirider.feature.rider.data.RiderRepositoryImpl
import com.example.chaskirider.feature.documents.data.DocumentRepositoryImpl
import com.example.chaskirider.feature.notifications.data.NotificationsRepositoryImpl
import com.example.chaskirider.feature.rider.data.local.RiderSessionStore
import com.example.chaskirider.feature.earnings.domain.EarningsRepository
import com.example.chaskirider.feature.orders.domain.OfferRepository
import com.example.chaskirider.core.platform.location.LocationRepository
import com.example.chaskirider.feature.rider.domain.RiderSession
import com.example.chaskirider.feature.bank.data.BankRepositoryImpl
import com.example.chaskirider.feature.bank.data.remote.BankRemote
import com.example.chaskirider.feature.bank.domain.BankRepository
import com.example.chaskirider.feature.rider.data.VehicleRepositoryImpl
import com.example.chaskirider.feature.rider.data.remote.VehicleRemote
import com.example.chaskirider.feature.rider.domain.VehicleRepository
import com.example.chaskirider.feature.rider.domain.RiderRepository
import com.example.chaskirider.feature.rider.domain.RiderOnboardingDraftRepository
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
    private val sessionStore = RiderSessionStore { uid -> riderTopics.bind(uid) }
    val riderSession: RiderSession = sessionStore
    val orderChanges = kotlinx.coroutines.flow.MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val offerNotifier by lazy { com.example.chaskirider.platform.OfferNotifier(applicationContext) }
    private val chaskiDatabase by lazy { openChaskiDatabase(applicationContext) }
    val registrationDraftRepository: RiderOnboardingDraftRepository by lazy {
        com.example.chaskirider.feature.rider.data.local.StoredRiderOnboardingDraftRepository(applicationContext)
    }
    private val riderStore by lazy {
        com.example.chaskirider.feature.rider.data.local.RiderStore(chaskiDatabase.riders())
    }
    private val bankAccountStore by lazy {
        com.example.chaskirider.feature.bank.data.local.BankAccountStore(chaskiDatabase.banks())
    }
    private val documentStore by lazy {
        com.example.chaskirider.feature.documents.data.local.DocumentStore(chaskiDatabase.documents())
    }
    private val supportStore by lazy {
        com.example.chaskirider.feature.support.data.local.SupportStore(chaskiDatabase.support())
    }
    private val earningsStore by lazy {
        com.example.chaskirider.feature.earnings.data.local.EarningsStore(chaskiDatabase.earnings())
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
            com.example.chaskirider.feature.orders.data.local.RoomEarningsOrderSource(chaskiDatabase.orders()),
            store = earningsStore
        )
    }
    val earningsRepository: EarningsRepository by lazy {
        localEarningsRepository
    }

    // Repositorio de demostraciÃ³n (Datos simulados locales para test)
    val demoOfferRepository: OfferRepository by lazy {
        OrderRepositoryImpl(
            store = StoredOrderSessionStore(chaskiDatabase.orders()) {
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
        com.example.chaskirider.feature.support.data.SupportRepositoryImpl(store = supportStore)
    }
    val assistantRepository: com.example.chaskirider.feature.support.domain.AssistantRepository by lazy {
        com.example.chaskirider.feature.support.data.FirebaseAssistantRepository(supportRepository, customerAbsenceService::awaitSync)
    }
    val orderChatRepository: com.example.chaskirider.feature.chat.domain.OrderChatRepository by lazy {
        com.example.chaskirider.feature.chat.data.ChatRepositoryImpl()
    }
    val chatDraftStore: com.example.chaskirider.feature.chat.domain.ChatDraftStore by lazy {
        com.example.chaskirider.feature.chat.data.local.RoomChatMessageStore(chaskiDatabase.chatMessages())
    }
    val supportDraftStore: com.example.chaskirider.feature.support.domain.SupportDraftStore by lazy {
        com.example.chaskirider.feature.support.data.local.StoredSupportDraftStore(applicationContext)
    }
    val customerContactStore: com.example.chaskirider.feature.orders.domain.CustomerContactStore by lazy {
        com.example.chaskirider.feature.orders.data.local.RoomCustomerContactStore(chaskiDatabase.orderContacts())
    }
    val textProvider: TextProvider by lazy { AndroidTextProvider(applicationContext) }
    private val profiles by lazy { RiderRemote(textProvider, sessionStore, online = connectivity::isOnline) }
    private val documents by lazy {
        RiderDocumentDataSource(textProvider, applicationContext,
            DocumentRemote(textProvider, online = connectivity::isOnline), refreshRider, documentStore)
    }
    val riderUserRepository: RiderRepository by lazy {
        RiderRepositoryImpl(applicationContext, textProvider, profiles, sessionStore, notificationsRepository, riderStore)
    }
    private val refreshRider: suspend () -> Unit = { riderUserRepository.refresh() }
    private val currentUid: () -> String? = {
        com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
    }
    val bankRepository: BankRepository by lazy {
        BankRepositoryImpl(textProvider, BankRemote(textProvider), bankAccountStore, currentUid, refreshRider)
    }
    val vehicleRepository: VehicleRepository by lazy {
        VehicleRepositoryImpl(textProvider, VehicleRemote(textProvider), refreshRider)
    }
    val locationRepository: LocationRepository by lazy { DeviceLocationRepository(applicationContext) }
    val documentPreviewRepository: DocumentPreviewRepository by lazy { com.example.chaskirider.feature.documents.data.DocumentPreviewRepositoryImpl(applicationContext, textProvider) }
    val documentRepository: DocumentRepository by lazy { DocumentRepositoryImpl(documents) }
    val notificationsRepository: NotificationsRepository by lazy {
        NotificationsRepositoryImpl(chaskiDatabase.notifications())
    }
}


