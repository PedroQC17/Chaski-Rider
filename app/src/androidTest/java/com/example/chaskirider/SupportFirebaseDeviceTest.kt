package com.example.chaskirider

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.example.chaskirider.feature.support.data.SupportRepositoryImpl
import com.example.chaskirider.feature.support.data.local.SupportDraftDatabase
import com.example.chaskirider.feature.support.data.local.RoomSupportDraftStore
import com.example.chaskirider.feature.user.domain.RiderUser
import com.example.chaskirider.feature.orders.domain.*
import com.example.chaskirider.feature.user.domain.RiderSession
import com.example.chaskirider.feature.support.domain.SupportOrderContext
import com.example.chaskirider.feature.support.domain.SupportTicketStatus
import com.example.chaskirider.feature.support.domain.SupportSender
import com.example.chaskirider.feature.support.ui.screens.SupportRoute
import com.example.chaskirider.feature.support.ui.viewmodel.SupportViewModel
import com.example.chaskirider.ui.theme.ChaskiRiderTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import org.junit.Assert.*
import org.junit.Assume.assumeNotNull
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.util.UUID

class SupportFirebaseDeviceTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun realTicketKeepsContextReceivesResponseAndCloses() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val user = FirebaseAuth.getInstance().currentUser
        assumeNotNull(user)
        val uid = user!!.uid
        val database = Room.inMemoryDatabaseBuilder(context, SupportDraftDatabase::class.java).build()
        val session = object : RiderSession { override val user = MutableStateFlow<RiderUser?>(RiderUser(id = uid)) }
        val vm = SupportViewModel(SupportRepositoryImpl(), RoomSupportDraftStore(database.drafts()), session)
        val report = File(context.getExternalFilesDir(null), "support-verification.json")
        try {
            compose.setContent { ChaskiRiderTheme { SupportRoute(vm, {}) } }
            compose.waitUntil(20_000) { !vm.uiState.value.loading }
            val order = SupportOrderContext(UUID.randomUUID().toString(), "Prueba técnica HU13", "Recojo de prueba",
                "Destino de prueba", true, UUID.randomUUID().toString(), OrderStatus.ON_THE_WAY, OrderProblemReason.CUSTOMER_UNREACHABLE)
            compose.runOnUiThread { vm.openForOrder(order, OrderProblemReason.CUSTOMER_UNREACHABLE, "Prueba técnica HU13: el cliente no responde") }
            compose.onNodeWithText("Crear ticket").performScrollTo().performClick()
            compose.waitUntil(30_000) { vm.uiState.value.selectedTicketId != null && !vm.uiState.value.busy && !vm.uiState.value.replyLoading }
            val id = vm.uiState.value.selectedTicketId!!
            val remote = FirebaseFirestore.getInstance().document("riders/$uid/supportTickets/$id")
            val record = remote.get(Source.SERVER).await()
            assertEquals("ON_THE_WAY", record.getString("order.orderStatus"))
            assertEquals("CUSTOMER_UNREACHABLE", record.getString("order.problemCategory"))
            report.writeText("""{"uid":"$uid","ticketId":"$id","phase":"WAITING_REPLY"}""")
            compose.waitUntil(90_000) { vm.uiState.value.conversation.any { it.sender == SupportSender.SUPPORT } }
            compose.onNodeWithText("Recibimos tu caso. Mantén el pedido activo.").performScrollTo().assertIsDisplayed()
            compose.onNodeWithTag("support_reply").performScrollTo().performTextInput("Sigo esperando en el destino")
            compose.onNodeWithText("Enviar mensaje").performScrollTo().performClick()
            compose.waitUntil(20_000) { !vm.uiState.value.busy && vm.uiState.value.conversation.any { it.sender == SupportSender.RIDER } }
            report.writeText("""{"uid":"$uid","ticketId":"$id","phase":"WAITING_CLOSE"}""")
            compose.waitUntil(90_000) { vm.uiState.value.tickets.firstOrNull { it.id == id }?.status == SupportTicketStatus.CLOSED }
            compose.onNodeWithText("Enviar mensaje").assertDoesNotExist()
            File(context.getExternalFilesDir(null), "support-real-closed.png").outputStream().use {
                InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot().compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
            }
            assertEquals(OrderStatus.ON_THE_WAY, vm.uiState.value.tickets.first { it.id == id }.order!!.orderStatus)
            report.writeText("""{"uid":"$uid","ticketId":"$id","result":"PASSED"}""")
        } finally { session.user.value = null; compose.waitForIdle(); database.close() }
    }
}
