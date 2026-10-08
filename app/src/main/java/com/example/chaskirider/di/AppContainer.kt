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
        applicationContext = context.applicationContext
        NotificationsInitializer().initialize(applicationContext)
    }
    private val sessionStore = RiderSessionStore()
    val riderSession: RiderSession = sessionStore
    
    // Repositorio real de ofertas (Nube / Firebase)
    val realOfferRepository: OfferRepository by lazy {
        RealOfferRepository()
    }

    // Repositorio de demostración (Datos simulados locales para test)
    val demoOfferRepository: OfferRepository by lazy {
        LocalDemoOfferRepository()
    }

    val offerRepository: OfferRepository by lazy {
        realOfferRepository
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
