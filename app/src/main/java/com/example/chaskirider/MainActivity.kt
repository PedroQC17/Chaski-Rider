// HU04 - Parte 3: se crea el canal de notificaciones de FCM y se suscribe el
// dispositivo al topic general "riders" al iniciar; la suscripción por
// usuario (rider_{uid}) se hace desde AppNavigation cuando hay sesión.
package com.example.chaskirider

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.chaskirider.data.notifications.ChaskiFirebaseMessagingService
import com.example.chaskirider.ui.navigation.AppNavigation
import com.example.chaskirider.ui.theme.ChaskiRiderTheme
import com.google.firebase.messaging.FirebaseMessaging

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        com.example.chaskirider.di.AppContainer.initialize(applicationContext)
        enableEdgeToEdge()

        ChaskiFirebaseMessagingService.ensureChannel(this)
        FirebaseMessaging.getInstance().subscribeToTopic(ChaskiFirebaseMessagingService.TOPIC_RIDERS)

        setContent {
            ChaskiRiderTheme {
                AppNavigation()
            }
        }
    }
}
