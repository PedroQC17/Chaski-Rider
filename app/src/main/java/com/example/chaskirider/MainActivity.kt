package com.example.chaskirider

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.chaskirider.di.AppContainer
import com.example.chaskirider.navigation.AppNavigation
import com.example.chaskirider.ui.theme.ChaskiRiderTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppContainer.initialize(applicationContext)
        enableEdgeToEdge()
        setContent {
            ChaskiRiderTheme {
                AppNavigation()
            }
        }
    }
}
