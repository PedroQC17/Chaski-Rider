package com.example.chaskirider.di

import android.content.Context
import com.example.chaskirider.data.location.DeviceLocationRepository
import com.example.chaskirider.data.text.AndroidTextProvider
import com.example.chaskirider.domain.text.TextProvider
import com.example.chaskirider.data.notifications.NotificationsInitializer
import com.example.chaskirider.data.documents.RiderDocumentDataSource
import com.example.chaskirider.data.earnings.LocalEarningsRepository
import com.example.chaskirider.data.orders.DemoOrderCatalog
import com.example.chaskirider.data.orders.LocalDemoOfferRepository
import com.example.chaskirider.data.orders.OrderSyncWorker
import com.example.chaskirider.data.orders.RealOfferRepository
import com.example.chaskirider.data.orders.RoomDemoOrderStore
import com.example.chaskirider.data.orders.openOrderDatabase
import com.example.chaskirider.data.remote.RiderProfileRemoteDataSource
import com.example.chaskirider.data.repository.*
import com.example.chaskirider.data.session.RiderSessionStore
import com.example.chaskirider.domain.earnings.EarningsRepository
import com.example.chaskirider.domain.orders.OfferRepository
import com.example.chaskirider.domain.repository.*

object AppContainer {
    private lateinit var applicationContext: Context
    fun initialize(context: Context) {
        val firstInitialization = !::applicationContext.isInitialized
        applicationContext = context.applicationContext
        NotificationsInitializer().initialize(applicationContext)
        if (firstInitialization) com.google.firebase.auth.FirebaseAuth.getInstance().addAuthStateListener { auth ->
            auth.currentUser?.uid?.let { com.example.chaskirider.data.orders.OrderSyncWorker.schedule(applicationContext, it) }
        }
    }
    private val sessionStore = RiderSessionStore()
    val riderSession: RiderSession = sessionStore
    val orderChanges = kotlinx.coroutines.flow.MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val offerNotifier by lazy { com.example.chaskirider.data.orders.OfferNotifier(applicationContext) }
    private val registrationDatabase by lazy {
        androidx.room.Room.databaseBuilder(applicationContext,
            com.example.chaskirider.data.local.RegistrationDatabase::class.java, "registration.db").build()
    }
    val registrationDraftRepository: RegistrationDraftRepository by lazy {
        com.example.chaskirider.data.local.RoomRegistrationDraftRepository(registrationDatabase.drafts())
    }
    
    // Repositorio real de ofertas (Nube / Firebase)
    val realOfferRepository: OfferRepository by lazy {
        RealOfferRepository()
    }

    val localEarningsRepository by lazy {
        LocalEarningsRepository()
    }
    val earningsRepository: EarningsRepository by lazy {
        localEarningsRepository
    }

    // Repositorio de demostración (Datos simulados locales para test)
    val demoOfferRepository: OfferRepository by lazy {
        val database = openOrderDatabase(applicationContext)
        LocalDemoOfferRepository(
            store = RoomDemoOrderStore(database.orders()) {
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
            onOrderCompleted = { orderId, merchantName, totalCents ->
                localEarningsRepository.recordCompletedOrder(
                    orderId = orderId,
                    merchantName = merchantName,
                    baseFareCents = (totalCents * 0.85).toInt(),
                    tipCents = (totalCents * 0.15).toInt()
                )
            }
        )
    }

    val offerRepository: OfferRepository by lazy {
        demoOfferRepository
    }
    val supportRepository: com.example.chaskirider.domain.support.SupportRepository by lazy {
        com.example.chaskirider.data.support.FirebaseSupportRepository()
    }
    val orderChatRepository: com.example.chaskirider.domain.chat.OrderChatRepository by lazy {
        com.example.chaskirider.data.chat.FirebaseOrderChatRepository()
    }
    val chatDraftStore: com.example.chaskirider.domain.chat.ChatDraftStore by lazy {
        val database = androidx.room.Room.databaseBuilder(applicationContext,
            com.example.chaskirider.data.chat.ChatDraftDatabase::class.java, "chat_drafts.db").build()
        com.example.chaskirider.data.chat.RoomChatDraftStore(database.drafts())
    }
    val supportDraftStore: com.example.chaskirider.domain.support.SupportDraftStore by lazy {
        val database = androidx.room.Room.databaseBuilder(applicationContext,
            com.example.chaskirider.data.support.SupportDraftDatabase::class.java, "support_drafts.db").build()
        com.example.chaskirider.data.support.RoomSupportDraftStore(database.drafts())
    }
    val textProvider: TextProvider by lazy { AndroidTextProvider(applicationContext) }
    private val profiles by lazy { RiderProfileRemoteDataSource(textProvider, sessionStore) }
    private val documents by lazy { RiderDocumentDataSource(textProvider, applicationContext, profiles) }
    val authRepository: AuthRepository by lazy { AuthRepositoryImpl(textProvider, profiles, sessionStore) }
    val riderProfileRepository: RiderProfileRepository by lazy { RiderProfileRepositoryImpl(textProvider, profiles) }
    val profilePhotoRepository: ProfilePhotoRepository by lazy { com.example.chaskirider.data.profile.ProfilePhotoRepositoryImpl(applicationContext, textProvider, profiles) }
    val locationRepository: LocationRepository by lazy { DeviceLocationRepository(applicationContext) }
    val documentPreviewRepository: DocumentPreviewRepository by lazy { com.example.chaskirider.data.documents.DocumentPreviewRepositoryImpl(applicationContext, textProvider) }
    val documentRepository: DocumentRepository by lazy { DocumentRepositoryImpl(documents) }
    val notificationsRepository: NotificationsRepository by lazy { NotificationsRepositoryImpl() }
}
