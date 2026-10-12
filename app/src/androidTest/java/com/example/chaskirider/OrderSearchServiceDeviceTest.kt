package com.example.chaskirider

import android.app.NotificationManager
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.example.chaskirider.data.orders.OrderSearchService
import com.example.chaskirider.data.session.RiderSessionStore
import com.example.chaskirider.di.AppContainer
import com.example.chaskirider.domain.model.RegistrationStatus
import com.example.chaskirider.domain.model.RiderUser
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class OrderSearchServiceDeviceTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun searchContinuesBehindHomeAndStopsWhenRiderDisconnects() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        fun shell(command: String) { instrumentation.uiAutomation.executeShellCommand(command).use {
            android.os.ParcelFileDescriptor.AutoCloseInputStream(it).use { stream -> stream.readBytes() }
        } }
        shell("pm grant ${context.packageName} android.permission.ACCESS_COARSE_LOCATION")
        shell("pm grant ${context.packageName} android.permission.ACCESS_FINE_LOCATION")
        shell("pm grant ${context.packageName} android.permission.POST_NOTIFICATIONS")
        AppContainer.initialize(context)
        val session = AppContainer.riderSession as RiderSessionStore
        val rider = RiderUser(id = "background-device-test", status = RegistrationStatus.APPROVED,
            isAvailable = true, profilePhotoPath = "local-test-photo")
        val manager = context.getSystemService(NotificationManager::class.java)
        try {
            session.update(rider)
            compose.runOnUiThread { OrderSearchService.start(compose.activity) }
            withTimeout(30_000) {
                while (manager.activeNotifications.none { it.id == 2008 }) delay(100)
            }
            shell("input keyevent 3")
            withTimeout(30_000) { AppContainer.orderChanges.first() }
            assertTrue(manager.activeNotifications.any { it.id == 2008 })
            session.update(rider.copy(isAvailable = false))
            withTimeout(10_000) {
                while (manager.activeNotifications.any { it.id == 2008 }) delay(100)
            }
        } finally {
            OrderSearchService.stop(context)
            session.update(null)
        }
    }
}
