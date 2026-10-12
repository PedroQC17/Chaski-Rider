package com.example.chaskirider

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.example.chaskirider.data.support.*
import com.example.chaskirider.domain.model.RiderUser
import com.example.chaskirider.domain.repository.RiderSession
import com.example.chaskirider.domain.support.*
import com.example.chaskirider.ui.screens.support.*
import com.example.chaskirider.ui.theme.ChaskiRiderTheme
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File

class SupportDeviceTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private fun screenshot(name: String) {
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        File(context.getExternalFilesDir(null), name).outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test fun roomRestoresDraftAndReplyAfterReopenAndIsolatesUsers() = runBlocking {
        val name = "support-drafts-device-test.db"
        context.deleteDatabase(name)
        var db = Room.databaseBuilder(context, SupportDraftDatabase::class.java, name).build()
        try {
            var store = RoomSupportDraftStore(db.drafts())
            val order = SupportOrderContext("order-1", "Local", "Recojo", "Destino", true, "problem-1")
            val draft = SupportDraft("draft-1", SupportTopic.CUSTOMER_UNREACHABLE, "El cliente no responde al contacto", order)
            store.save("alice", draft)
            store.save("alice/reply/ticket-1", SupportDraft("message-1", body = "Estoy esperando en el destino"))
            db.close()
            db = Room.databaseBuilder(context, SupportDraftDatabase::class.java, name).build()
            store = RoomSupportDraftStore(db.drafts())
            assertEquals(draft, store.load("alice"))
            assertEquals("Estoy esperando en el destino", store.load("alice/reply/ticket-1")!!.body)
            assertNull(store.load("bob"))
            store.clear("alice", "different-id")
            assertNotNull(store.load("alice"))
            store.clear("alice", "draft-1")
            assertNull(store.load("alice"))
        } finally { db.close(); context.deleteDatabase(name) }
    }

    @Test fun riderCanCreateTicketReadSupportReplyAndSeeClosedState() {
        val db = Room.inMemoryDatabaseBuilder(context, SupportDraftDatabase::class.java).build()
        val session = object : RiderSession { override val user = MutableStateFlow<RiderUser?>(RiderUser(id = "support-device-test")) }
        val tickets = MutableStateFlow<List<SupportTicket>>(emptyList())
        val messages = MutableStateFlow<List<SupportMessage>>(emptyList())
        val repository = object : SupportRepository {
            override fun observe(uid: String): Flow<List<SupportTicket>> = tickets
            override fun observeMessages(uid: String, ticketId: String): Flow<List<SupportMessage>> = messages
            override suspend fun create(uid: String, draft: SupportDraft): String {
                tickets.value = listOf(SupportTicket(draft.id, draft.topic!!, System.currentTimeMillis(), order = draft.order,
                    messages = listOf(SupportMessage("initial", SupportSender.RIDER, draft.body, System.currentTimeMillis()))))
                return draft.id
            }
            override suspend fun reply(uid: String, ticketId: String, messageId: String, body: String) {
                messages.value += SupportMessage(messageId, SupportSender.RIDER, body, System.currentTimeMillis())
            }
        }
        val vm = SupportViewModel(repository, RoomSupportDraftStore(db.drafts()), session)
        try {
            compose.setContent { ChaskiRiderTheme { SupportRoute(vm, {}) } }
            compose.waitUntil(15_000) { !vm.uiState.value.loading }
            screenshot("support-home.png")
            compose.onNodeWithText("Un pedido").performClick()
            compose.onNodeWithText("El cliente no responde").performClick()
            compose.onNodeWithText("Necesito más ayuda").performScrollTo().performClick()
            compose.onNodeWithTag("support_body").performTextInput("El cliente no responde y estoy en el destino")
            screenshot("support-form.png")
            compose.onNodeWithText("Crear ticket").performScrollTo().performClick()
            compose.waitUntil(15_000) { vm.uiState.value.selectedTicketId != null && !vm.uiState.value.replyLoading }
            compose.onNodeWithText("Abierto").assertExists()
            compose.onNodeWithTag("support_reply").performScrollTo().performTextInput("Sigo esperando en la entrada")
            compose.onNodeWithText("Enviar mensaje").performScrollTo().performClick()
            compose.waitUntil(15_000) { messages.value.size == 1 && !vm.uiState.value.busy }
            messages.value += SupportMessage("support-1", SupportSender.SUPPORT, "Recibimos tu consulta. Confirma los datos del destino.", System.currentTimeMillis())
            compose.onNodeWithText("Recibimos tu consulta. Confirma los datos del destino.").performScrollTo().assertIsDisplayed()
            screenshot("support-conversation.png")
            tickets.value = tickets.value.map { it.copy(status = SupportTicketStatus.CLOSED) }
            compose.onNodeWithText("Este ticket está cerrado. Puedes abrir otro desde Ayuda si necesitas asistencia.").performScrollTo().assertIsDisplayed()
            compose.onNodeWithText("Enviar mensaje").assertDoesNotExist()
        } finally { session.user.value = null; compose.waitForIdle(); db.close() }
    }
}
