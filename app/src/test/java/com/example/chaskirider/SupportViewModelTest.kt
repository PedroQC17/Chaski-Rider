package com.example.chaskirider

import com.example.chaskirider.domain.model.RiderUser
import com.example.chaskirider.domain.orders.OrderProblemReason
import com.example.chaskirider.domain.repository.RiderSession
import com.example.chaskirider.domain.support.*
import com.example.chaskirider.ui.screens.support.SupportViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SupportViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val session = object : RiderSession { override val user = MutableStateFlow<RiderUser?>(RiderUser(id = "alice")) }
    private val saved = mutableMapOf<String, SupportDraft>()
    private val tickets = MutableStateFlow<List<SupportTicket>>(emptyList())
    private var failSave = false
    private var failCreate = false
    private var createGate: CompletableDeferred<Unit>? = null
    private val created = mutableListOf<Pair<String, SupportDraft>>()
    private val replies = mutableListOf<String>()
    private var failReply = false
    private val conversation = MutableStateFlow<List<SupportMessage>>(emptyList())
    private val store = object : SupportDraftStore {
        override suspend fun load(uid: String) = saved[uid]
        override suspend fun save(uid: String, draft: SupportDraft) {
            if (failSave) error("disk full")
            saved[uid] = draft
        }
        override suspend fun clear(uid: String, draftId: String) { if (saved[uid]?.id == draftId) saved.remove(uid) }
    }
    private val repository = object : SupportRepository {
        override fun observe(uid: String): Flow<List<SupportTicket>> = tickets
        override fun observeMessages(uid: String, ticketId: String): Flow<List<SupportMessage>> = conversation
        override suspend fun create(uid: String, draft: SupportDraft): String {
            createGate?.await()
            if (failCreate) error("offline")
            created += uid to draft
            tickets.value = listOf(SupportTicket(draft.id, draft.topic!!, 1000))
            return draft.id
        }
        override suspend fun reply(uid: String, ticketId: String, messageId: String, body: String) {
            if (failReply) error("offline")
            replies += messageId
        }
    }
    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun cleanup() { Dispatchers.resetMain() }
    private fun form(vm: SupportViewModel) {
        vm.selectCategory(SupportCategory.APP); vm.selectTopic(SupportTopic.LOCATION); vm.openForm()
        vm.changeBody("Mi ubicación no se actualiza en el mapa")
    }

    @Test fun validationDoesNotAllowBlankOrOversizedMessages() {
        assertFalse(SupportValidation.validBody("         "))
        assertFalse(SupportValidation.validBody("x".repeat(2001)))
        assertTrue(SupportValidation.validBody("x".repeat(10)))
        assertFalse(SupportValidation.validReply(" "))
    }

    @Test fun draftSurvivesNewViewModelAndDoesNotLeakAcrossAccounts() = runTest(dispatcher) {
        val vm = SupportViewModel(repository, store, session); runCurrent()
        form(vm); runCurrent()
        val restored = SupportViewModel(repository, store, session); runCurrent()
        assertEquals(vm.uiState.value.draft, restored.uiState.value.draft)
        assertTrue(restored.uiState.value.formOpen)
        session.user.value = RiderUser(id = "bob"); runCurrent()
        assertEquals("", vm.uiState.value.draft!!.body)
        assertTrue(vm.uiState.value.tickets.isEmpty())
        session.user.value = RiderUser(id = "alice"); runCurrent()
        assertEquals("Mi ubicación no se actualiza en el mapa", vm.uiState.value.draft!!.body)
    }

    @Test fun failedSubmissionKeepsDraftIdAndRetryCannotDoubleSubmit() = runTest(dispatcher) {
        val vm = SupportViewModel(repository, store, session); runCurrent(); form(vm); runCurrent()
        val id = vm.uiState.value.draft!!.id
        failCreate = true; vm.createTicket(); runCurrent()
        assertTrue(vm.uiState.value.formOpen)
        assertEquals(id, vm.uiState.value.draft!!.id)
        assertEquals(id, saved["alice"]!!.id)
        failCreate = false; createGate = CompletableDeferred()
        vm.createTicket(); runCurrent(); vm.createTicket(); runCurrent()
        createGate!!.complete(Unit); runCurrent()
        assertEquals(1, created.size)
        assertEquals(id, created.single().second.id)
        assertNull(saved["alice"])
        assertEquals(id, vm.uiState.value.selectedTicketId)
    }

    @Test fun diskFailureDoesNotCreateTicketAndKeepsText() = runTest(dispatcher) {
        val vm = SupportViewModel(repository, store, session); runCurrent(); form(vm); runCurrent()
        failSave = true; vm.createTicket(); runCurrent()
        assertTrue(created.isEmpty())
        assertNotNull(vm.uiState.value.error)
        assertTrue(vm.uiState.value.draft!!.body.isNotBlank())
    }

    @Test fun orderProblemIsLinkedWithoutOverwritingAnotherDraft() = runTest(dispatcher) {
        val vm = SupportViewModel(repository, store, session); runCurrent(); form(vm); runCurrent()
        val original = vm.uiState.value.draft
        val order = SupportOrderContext("order-1", "Local", "Recojo", "Destino", true, "problem-1")
        vm.openForOrder(order, OrderProblemReason.CUSTOMER_UNREACHABLE, "El cliente no responde"); runCurrent()
        assertEquals(original, vm.uiState.value.draft)
        assertNotNull(vm.uiState.value.notice)
        vm.discardDraft(); runCurrent()
        vm.openForOrder(order, OrderProblemReason.CUSTOMER_UNREACHABLE, "El cliente no responde"); runCurrent()
        assertEquals(order, vm.uiState.value.draft!!.order)
        assertEquals(SupportTopic.CUSTOMER_UNREACHABLE, vm.uiState.value.draft!!.topic)
        assertTrue(vm.uiState.value.draft!!.order!!.pickedUp)
    }

    @Test fun accountSwitchCancelsPendingTicketCreation() = runTest(dispatcher) {
        val vm = SupportViewModel(repository, store, session); runCurrent(); form(vm); runCurrent()
        createGate = CompletableDeferred(); vm.createTicket(); runCurrent()
        session.user.value = RiderUser(id = "bob"); runCurrent()
        createGate!!.complete(Unit); runCurrent()
        assertTrue(created.isEmpty())
        assertEquals("", vm.uiState.value.draft!!.body)
        assertNotNull(saved["alice"])
    }

    @Test fun retryingDraftSaveNeverReplacesUnsavedTextWithOlderDiskContent() = runTest(dispatcher) {
        val vm = SupportViewModel(repository, store, session); runCurrent(); form(vm); runCurrent()
        failSave = true; vm.changeBody("Este es el texto mas reciente que necesito conservar"); runCurrent()
        vm.refresh(); runCurrent()
        assertEquals("Este es el texto mas reciente que necesito conservar", vm.uiState.value.draft!!.body)
        failSave = false; vm.refresh(); runCurrent()
        assertEquals(vm.uiState.value.draft, saved["alice"])
    }

    @Test fun replyFailurePreservesDraftAndClosedTicketsBlockFurtherMessages() = runTest(dispatcher) {
        tickets.value = listOf(SupportTicket("ticket-1", SupportTopic.LOCATION, 1000))
        val vm = SupportViewModel(repository, store, session); runCurrent()
        vm.openTicket("ticket-1"); runCurrent()
        vm.changeReply("Estoy en el destino"); runCurrent()
        val id = vm.uiState.value.reply!!.id
        failReply = true; vm.sendReply(); runCurrent()
        assertEquals("Estoy en el destino", vm.uiState.value.reply!!.body)
        assertEquals(id, saved["alice/reply/ticket-1"]!!.id)
        failReply = false; vm.sendReply(); runCurrent()
        assertEquals(listOf(id), replies)
        assertEquals("", vm.uiState.value.reply!!.body)
        conversation.value = listOf(SupportMessage("support-1", SupportSender.SUPPORT, "Confirma el destino", 2000))
        runCurrent(); assertEquals(SupportSender.SUPPORT, vm.uiState.value.conversation.single().sender)
        tickets.value = tickets.value.map { it.copy(status = SupportTicketStatus.CLOSED) }; runCurrent()
        vm.changeReply("Otra consulta"); runCurrent(); vm.sendReply(); runCurrent()
        assertEquals(1, replies.size)
    }

    @Test fun editingTextAfterServerConfirmationUsesANewMessageId() = runTest(dispatcher) {
        tickets.value = listOf(SupportTicket("ticket-1", SupportTopic.LOCATION, 1000))
        val vm = SupportViewModel(repository, store, session); runCurrent()
        vm.openTicket("ticket-1"); runCurrent(); vm.changeReply("Primer mensaje"); runCurrent()
        val oldId = vm.uiState.value.reply!!.id
        failReply = true; vm.sendReply(); runCurrent()
        conversation.value = listOf(SupportMessage(oldId, SupportSender.RIDER, "Primer mensaje", 2000)); runCurrent()
        vm.changeReply("Ahora necesito agregar otra informacion"); runCurrent()
        failReply = false; vm.sendReply(); runCurrent()
        assertEquals(1, replies.size)
        assertNotEquals(oldId, replies.single())
    }

    @Test fun editingAPreviouslyRegisteredTicketDoesNotReuseItsId() = runTest(dispatcher) {
        val vm = SupportViewModel(repository, store, session); runCurrent(); form(vm); runCurrent()
        val old = vm.uiState.value.draft!!
        failCreate = true; vm.createTicket(); runCurrent()
        tickets.value = listOf(SupportTicket(old.id, old.topic!!, 1000,
            messages = listOf(SupportMessage("initial", SupportSender.RIDER, old.body, 1000)))); runCurrent()
        vm.changeBody("Quiero registrar una consulta distinta a la anterior"); runCurrent()
        failCreate = false; vm.createTicket(); runCurrent()
        assertEquals(1, created.size)
        assertNotEquals(old.id, created.single().second.id)
    }
}
