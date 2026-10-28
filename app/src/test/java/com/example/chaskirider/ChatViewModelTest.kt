package com.example.chaskirider

import com.example.chaskirider.domain.chat.*
import com.example.chaskirider.domain.model.RiderUser
import com.example.chaskirider.domain.orders.*
import com.example.chaskirider.domain.repository.RiderSession
import com.example.chaskirider.ui.screens.chat.ChatViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.After
import org.junit.Test

class ChatViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val session = object : RiderSession { override val user = MutableStateFlow<RiderUser?>(RiderUser(id = "alice")) }
    private val saved = mutableMapOf<String, ChatDraft>()
    private val drafts = object : ChatDraftStore {
        override suspend fun load(key: String) = saved[key]
        override suspend fun save(key: String, draft: ChatDraft) { saved[key] = draft }
        override suspend fun clear(key: String, draft: ChatDraft) { if (saved[key] == draft) saved.remove(key) }
    }
    private val conversations = mutableMapOf<String, MutableStateFlow<ChatConversation>>()
    private var fail = false
    private var gate: CompletableDeferred<Unit>? = null
    private val sent = mutableListOf<String>()
    private val repo = object : OrderChatRepository {
        override fun observe(uid: String, orderId: String, channel: ChatChannel) = conversations.getOrPut("$uid/$orderId/$channel") {
            MutableStateFlow(ChatConversation(emptyList())) }
        override suspend fun send(uid: String, orderId: String, channel: ChatChannel, messageId: String, body: String) {
            gate?.await()
            if (fail) error("offline")
            sent += messageId
        }
    }
    private val point = RoutePoint(-12.0, -77.0)
    private fun snapshot(id: String = "order") = OfferSnapshot(1000, false,
        DeliveryQuote(listOf(DeliveryStop("stop", id, "Lima", point)), 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, emptyList()), null, point)
    private fun model() = ChatViewModel(repo, drafts, session).also { it.syncOrderState(snapshot()); it.openChat() }
    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun cleanup() { Dispatchers.resetMain() }

    @Test fun draftsSurviveNewViewModelAndStaySeparatedByChannelAndAccount() = runTest(dispatcher) {
        val vm = model(); runCurrent()
        vm.updateInputText("Estoy en la entrada"); runCurrent()
        vm.selectChannel(ChatChannel.CUSTOMER); runCurrent()
        assertEquals("", vm.uiState.value.inputText)
        vm.updateInputText("Voy hacia tu dirección"); runCurrent()
        val restored = model(); runCurrent()
        assertEquals("Estoy en la entrada", restored.uiState.value.inputText)
        session.user.value = RiderUser(id = "bob"); runCurrent()
        assertTrue(restored.uiState.value.messages.isEmpty())
        assertFalse(restored.uiState.value.isOpen)
        restored.syncOrderState(snapshot()); restored.openChat(); runCurrent()
        assertEquals("", restored.uiState.value.inputText)
    }

    @Test fun failedSendKeepsDraftAndIdWhileSuccessClearsIt() = runTest(dispatcher) {
        val vm = model(); runCurrent()
        vm.updateInputText("Confirma el punto de recojo"); runCurrent()
        val initial = saved.values.single()
        fail = true; vm.sendMessage(); runCurrent()
        assertEquals(initial, saved.values.single())
        assertNotNull(vm.uiState.value.error)
        fail = false; vm.sendMessage(); vm.sendMessage(); runCurrent()
        assertEquals(listOf(initial.id), sent)
        assertTrue(saved.isEmpty())
        assertEquals("", vm.uiState.value.inputText)
    }

    @Test fun merchantClosesAfterPickupAndCustomerAfterDeliveryEvenWithDirectCalls() = runTest(dispatcher) {
        val vm = model(); runCurrent()
        vm.updateInputText("Pedido del local"); runCurrent()
        vm.syncOrderState(snapshot().copy(pickedUp = true, pickedUpAt = 2000))
        vm.sendMessage(); runCurrent()
        assertTrue(sent.isEmpty())
        vm.selectChannel(ChatChannel.CUSTOMER); runCurrent()
        vm.updateInputText("Llegaré a tu dirección"); runCurrent()
        vm.sendMessage(); runCurrent()
        assertEquals(1, sent.size)
        vm.updateInputText("No debe enviarse"); runCurrent()
        vm.syncOrderState(snapshot().copy(pickedUp = true, isDelivered = true))
        vm.sendMessage(); runCurrent()
        assertEquals(1, sent.size)
    }

    @Test fun changingAccountCancelsInFlightSendAndDoesNotExposeOldMessages() = runTest(dispatcher) {
        val vm = model(); runCurrent()
        vm.updateInputText("Mensaje de Alice"); runCurrent()
        gate = CompletableDeferred(); vm.sendMessage(); runCurrent()
        session.user.value = RiderUser(id = "bob"); runCurrent()
        gate!!.complete(Unit); runCurrent()
        assertTrue(sent.isEmpty())
        assertEquals("", vm.uiState.value.inputText)
        assertNotNull(saved["alice/order/MERCHANT"])
    }

    @Test fun dictationIsReviewedAndRealIncomingMessagesAreObserved() = runTest(dispatcher) {
        val vm = model(); runCurrent()
        vm.onSpeechRecognized("Estoy afuera"); runCurrent()
        assertEquals("Estoy afuera", vm.uiState.value.inputText)
        assertTrue(sent.isEmpty())
        val message = ChatMessage("remote", "order", ChatChannel.MERCHANT, "Local", ChatSenderRole.MERCHANT, "Ya está listo", 1000)
        conversations["alice/order/MERCHANT"]!!.value = ChatConversation(listOf(message))
        runCurrent()
        assertEquals(listOf(message), vm.uiState.value.messages)
        vm.syncOrderState(null)
        assertTrue(vm.uiState.value.messages.isEmpty())
    }
}
