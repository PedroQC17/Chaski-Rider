
package com.example.chaskirider

import android.os.Bundle
import android.content.Intent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import com.example.chaskirider.data.orders.OfferNotifier
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.chaskirider.ui.navigation.AppNavigation
import com.example.chaskirider.ui.theme.ChaskiRiderTheme

class MainActivity : ComponentActivity() {
    private var demoRequest by mutableStateOf(0L)
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra(OfferNotifier.EXTRA_DEMO, false)) {
            demoRequest++
            intent.removeExtra(OfferNotifier.EXTRA_DEMO)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        com.example.chaskirider.di.AppContainer.initialize(applicationContext)
        enableEdgeToEdge()
        if (intent.getBooleanExtra(OfferNotifier.EXTRA_DEMO, false)) {
            demoRequest++
            intent.removeExtra(OfferNotifier.EXTRA_DEMO)
        }

        setContent {
            ChaskiRiderTheme {
                AppNavigation(demoRequest = demoRequest)
            }
        }
    }
}
