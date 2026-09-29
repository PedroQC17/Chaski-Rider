package com.example.chaskirider.di

import android.content.Context
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
    private val profiles by lazy { RiderProfileRemoteDataSource(sessionStore) }
    private val documents by lazy { RiderDocumentDataSource(applicationContext, profiles) }
    val authRepository: AuthRepository by lazy { AuthRepositoryImpl(profiles, sessionStore) }
    val riderProfileRepository: RiderProfileRepository by lazy { RiderProfileRepositoryImpl(profiles) }
    val documentRepository: DocumentRepository by lazy { DocumentRepositoryImpl(documents) }
    val notificationsRepository: NotificationsRepository by lazy { NotificationsRepositoryImpl() }
}
