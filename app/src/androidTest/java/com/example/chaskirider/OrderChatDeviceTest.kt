package com.example.chaskirider

import androidx.activity.ComponentActivity
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.*
import com.example.chaskirider.data.chat.*
import com.example.chaskirider.data.orders.*
import com.example.chaskirider.domain.chat.*
import com.example.chaskirider.domain.model.RiderUser
import com.example.chaskirider.domain.orders.*
import com.example.chaskirider.domain.repository.RiderSession
import com.example.chaskirider.ui.screens.chat.*
import com.example.chaskirider.ui.theme.ChaskiRiderTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import com.google.gson.Gson
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.tasks.await
import org.junit.Assert.*
import org.junit.Assume.assumeNotNull
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.util.UUID
import java.util.concurrent.TimeUnit

class OrderChatDeviceTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun draftDatabaseRestoresChannelsAndDoesNotDeleteEditedText() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "chat-draft-restore-test.db"
        context.deleteDatabase(name)
        var database = Room.databaseBuilder(context, ChatDraftDatabase::class.java, name).build()
        val first = ChatDraft("message", "Mensaje pendiente")
        try {
            var store = RoomChatDraftStore(database.drafts())
            store.save("alice/order/MERCHANT", first)
            store.save("alice/order/CUSTOMER", ChatDraft("customer", "Otro canal"))
            database.close()
            database = Room.databaseBuilder(context, ChatDraftDatabase::class.java, name).build()
            store = RoomChatDraftStore(database.drafts())
            assertEquals(first, store.load("alice/order/MERCHANT"))
            assertNull(store.load("bob/order/MERCHANT"))
            val edited = first.copy(body = "Texto editado más reciente")
            store.save("alice/order/MERCHANT", edited)
            store.clear("alice/order/MERCHANT", first)
            assertEquals(edited, store.load("alice/order/MERCHANT"))
            assertEquals("Otro canal", store.load("alice/order/CUSTOMER")!!.body)
        } finally { database.close(); context.deleteDatabase(name) }
    }

    @Test fun realChatRestoresMessagesReceivesReplyAndClosesChannelsByStage() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val user = FirebaseAuth.getInstance().currentUser
        assumeNotNull(user)
        val uid = user!!.uid
        val id = UUID.randomUUID().toString()
        val orders = openOrderDatabase(context)
        val draftName = "chat-device-test.db"
        context.deleteDatabase(draftName)
        val draftDb = Room.databaseBuilder(context, ChatDraftDatabase::class.java, draftName).build()
        val drafts = RoomChatDraftStore(draftDb.drafts())
        val repo = FirebaseOrderChatRepository()
        val session = object : RiderSession { override val user = MutableStateFlow<RiderUser?>(RiderUser(id = uid)) }
        var vm = ChatViewModel(repo, drafts, session)
        var displayed by mutableStateOf(vm)
        val now = System.currentTimeMillis()
        val point = RoutePoint(-12.046, -77.037)
        val record = OrderRecord(id, uid, OrderStatus.ACCEPTED, "Prueba tecnica de chat", point, point, now, null, null, null)
        val quote = DeliveryQuote(listOf(DeliveryStop("stop", id, "Lima", point)), 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, emptyList())
        val snapshot = OfferSnapshot(now, false, quote, null, point)
        val manager = WorkManager.getInstance(context)
        val name = "chat-device-$id"
        val remote = FirebaseFirestore.getInstance().collection("riders").document(uid).collection("deliveryRecords").document(id)
        val report = File(context.getExternalFilesDir(null), "chat-verification.json")
        suspend fun sync(value: OrderRecord, revision: Long) {
            orders.orders().saveRecord(OrderRecordEntity(uid, id, Gson().toJson(value), revision, true))
            manager.enqueueUniqueWork(name, ExistingWorkPolicy.REPLACE,
                OneTimeWorkRequestBuilder<OrderSyncWorker>().setInputData(workDataOf("uid" to uid)).build())
                .result.get(10, TimeUnit.SECONDS)
            withTimeout(60_000) { while (orders.orders().record(uid, id)!!.pending) delay(250) }
        }
        try {
            sync(record, 1)
            compose.setContent { ChaskiRiderTheme {
                val state by displayed.uiState.collectAsStateWithLifecycle()
                ChatDialog(state, displayed::closeChat, displayed::selectChannel, displayed::updateInputText,
                    displayed::onSpeechRecognized, displayed::sendMessage, displayed::retry)
            } }
            compose.runOnUiThread { vm.syncOrderState(snapshot); vm.openChat() }
            compose.waitUntil(15_000) { !vm.uiState.value.loading }
            assertTrue(vm.uiState.value.messages.isEmpty())
            compose.onNodeWithTag("order_chat_input").performTextInput("Estoy en la entrada del negocio")
            compose.onNodeWithContentDescription("Enviar").performClick()
            compose.waitUntil(20_000) { !vm.uiState.value.busy && vm.uiState.value.messages.size == 1 }
            val sent = vm.uiState.value.messages.single()
            repo.send(uid, id, ChatChannel.MERCHANT, sent.id, sent.text)
            assertEquals(1, remote.collection("chats").document("MERCHANT").collection("messages").get(Source.SERVER).await().size())
            compose.runOnUiThread { vm.closeChat() }
            vm = ChatViewModel(repo, drafts, session)
            compose.runOnUiThread { displayed = vm; vm.syncOrderState(snapshot); vm.openChat() }
            compose.waitUntil(15_000) { !vm.uiState.value.loading && vm.uiState.value.messages.size == 1 }
            report.writeText("""{"uid":"$uid","orderId":"$id","phase":"WAITING_REPLY","result":"STARTED"}""")
            compose.waitUntil(90_000) { vm.uiState.value.messages.any { it.senderRole == ChatSenderRole.MERCHANT } }
            compose.waitUntil(10_000) {
                compose.onAllNodesWithText("El pedido está listo para recoger").fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithText("El pedido está listo para recoger").assertIsDisplayed()
            File(context.getExternalFilesDir(null), "order-chat-conversation.png").outputStream().use {
                InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
                    .compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
            }
            val picked = record.copy(status = OrderStatus.ON_THE_WAY, arrivedAt = now + 1, pickedUpAt = now + 2)
            sync(picked, 2)
            compose.runOnUiThread { vm.syncOrderState(snapshot.copy(pickedUp = true, pickedUpAt = now + 2)) }
            assertFalse(vm.uiState.value.isMerchantChannelEnabled)
            try { repo.send(uid, id, ChatChannel.MERCHANT, UUID.randomUUID().toString(), "No debe enviarse"); fail() }
            catch (_: IllegalStateException) { }
            compose.runOnUiThread { vm.selectChannel(ChatChannel.CUSTOMER) }
            compose.waitUntil(15_000) { !vm.uiState.value.loading }
            compose.onNodeWithTag("order_chat_input").performTextInput("Voy hacia el destino")
            compose.onNodeWithContentDescription("Enviar").performClick()
            compose.waitUntil(20_000) { !vm.uiState.value.busy && vm.uiState.value.messages.size == 1 }
            sync(picked.copy(status = OrderStatus.DELIVERED, deliveredAt = now + 3), 3)
            compose.runOnUiThread { vm.syncOrderState(snapshot.copy(pickedUp = true, isDelivered = true)) }
            compose.onNodeWithTag("order_chat_input").assertDoesNotExist()
            assertFalse(vm.uiState.value.isCustomerChannelEnabled)
            report.writeText("""{"uid":"$uid","orderId":"$id","result":"PASSED"}""")
        } finally {
            compose.runOnUiThread { vm.closeChat() }
            manager.cancelUniqueWork(name).result.get(10, TimeUnit.SECONDS)
            delay(300)
            draftDb.close(); orders.close(); context.deleteDatabase(draftName)
        }
    }
}
