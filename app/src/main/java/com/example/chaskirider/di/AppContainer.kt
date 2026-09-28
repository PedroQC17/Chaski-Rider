package com.example.chaskirider.di

import android.content.Context
import com.example.chaskirider.data.repository.AuthRepositoryImpl
import com.example.chaskirider.domain.repository.AuthRepository

object AppContainer {
    private lateinit var applicationContext: Context
    fun initialize(context: Context) { applicationContext = context.applicationContext }
    val authRepository: AuthRepository by lazy { AuthRepositoryImpl(applicationContext) }
}
