package com.example.chaskirider

import android.app.Application
import com.example.chaskirider.di.AppContainer

class ChaskiRiderApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AppContainer.initialize(this)
    }
}
