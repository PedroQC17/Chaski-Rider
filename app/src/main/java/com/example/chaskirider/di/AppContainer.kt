package com.example.chaskirider.di

import android.content.Context
import com.example.chaskirider.data.location.DeviceLocationRepository
import com.example.chaskirider.data.text.AndroidTextProvider
import com.example.chaskirider.domain.text.TextProvider
import com.example.chaskirider.data.notifications.NotificationsInitializer
import com.example.chaskirider.data.documents.RiderDocumentDataSource
import com.example.chaskirider.data.orders.LocalDemoOfferRepository
import com.example.chaskirider.data.orders.RealOfferRepository
import com.example.chaskirider.data.remote.RiderProfileRemoteDataSource
import com.example.chaskirider.data.repository.*
import com.example.chaskirider.data.session.RiderSessionStore
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

    // Repositorio de demostración (Datos simulados locales para test)
    val demoOfferRepository: OfferRepository by lazy {
        val database = com.example.chaskirider.data.orders.openOrderDatabase(applicationContext)
        LocalDemoOfferRepository(com.example.chaskirider.data.orders.RoomDemoOrderStore(database.orders()) {
            com.example.chaskirider.data.orders.OrderSyncWorker.schedule(applicationContext, it)
        },
            { riderSession.user.value?.id }, {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    applicationContext.assets.open("demo_orders_lima.json").bufferedReader().use {
                        com.example.chaskirider.data.orders.DemoOrderCatalog.parse(it.readText())
                    }
                }
            })
    }

    val offerRepository: OfferRepository by lazy {
        demoOfferRepository
    }
    val supportRepository: com.example.chaskirider.domain.support.SupportRepository by lazy {
        com.example.chaskirider.data.support.FirebaseSupportRepository()
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
