package com.example.chaskirider.di

import android.content.Context
import com.example.chaskirider.data.text.AndroidTextProvider
import com.example.chaskirider.domain.text.TextProvider
import com.example.chaskirider.data.notifications.NotificationsInitializer
import com.example.chaskirider.data.documents.RiderDocumentDataSource
import com.example.chaskirider.data.remote.RiderProfileRemoteDataSource
import com.example.chaskirider.data.repository.*
import com.example.chaskirider.data.session.RiderSessionStore
import com.example.chaskirider.domain.repository.*

object AppContainer {
    private lateinit var applicationContext: Context
    fun initialize(context: Context) {
        applicationContext = context.applicationContext
        NotificationsInitializer().initialize(applicationContext)
    }
    private val sessionStore = RiderSessionStore()
    val riderSession: RiderSession = sessionStore
    val textProvider: TextProvider by lazy { AndroidTextProvider(applicationContext) }
    private val profiles by lazy { RiderProfileRemoteDataSource(textProvider, sessionStore) }
    private val documents by lazy { RiderDocumentDataSource(textProvider, applicationContext, profiles) }
    val authRepository: AuthRepository by lazy { AuthRepositoryImpl(textProvider, profiles, sessionStore) }
    val riderProfileRepository: RiderProfileRepository by lazy { RiderProfileRepositoryImpl(textProvider, profiles) }
    val documentRepository: DocumentRepository by lazy { DocumentRepositoryImpl(documents) }
    val notificationsRepository: NotificationsRepository by lazy { NotificationsRepositoryImpl() }
}
