package com.example.chaskirider

import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.chaskirider.feature.chat.domain.*
import com.example.chaskirider.feature.user.domain.RiderUser
import com.example.chaskirider.feature.user.domain.RiderSession
import com.example.chaskirider.feature.support.domain.SupportMessage
import com.example.chaskirider.feature.support.domain.SupportTicket
import com.example.chaskirider.feature.support.domain.AssistantReply
import com.example.chaskirider.feature.support.domain.AssistantRepository
import com.example.chaskirider.feature.support.ui.viewmodel.AssistantViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class AssistantViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val holder = ViewModelStore()
    private val session = object : RiderSession { override val user = MutableStateFlow<RiderUser?>(RiderUser(id = "alice")) }
    private val saved = mutableMapOf<String, ChatDraft>()
    private var fail = false
    private var allowed = false
    private var resolved = 0
    private val ids = mutableListOf<String>()
    private val drafts = object : ChatDraftStore {
        override suspend fun load(key: String) = saved[key]
        override suspend fun save(key: String, draft: ChatDraft) { saved[key] = draft }
        override suspend fun clear(key: String, draft: ChatDraft) { if (saved[key] == draft) saved.remove(key) }
    }
    private val repository = object : AssistantRepository {
        override fun messages(uid: String, orderId: String) = flowOf(emptyList<SupportMessage>())
        override fun ticket(uid: String, orderId: String) = flowOf<SupportTicket?>(null)
        override suspend fun send(uid: String, orderId: String, messageId: String, body: String, human: Boolean): AssistantReply {
            ids += messageId
            if (fail) error("offline")
            return AssistantReply(if (human) "HUMAN_HANDOFF" else "GEMINI", human, allowed && !human, "ticket")
        }
    }
    private fun model() = ViewModelProvider(holder, viewModelFactory { initializer {
        AssistantViewModel(repository, drafts, session) { resolved++ }
    } })[AssistantViewModel::class.java]
    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun cleanup() { holder.clear(); Dispatchers.resetMain() }

    @Test fun failedSendPreservesDraftAndRetryUsesTheSameId() = runTest {
        val vm = model();vm.bind("one");runCurrent()
        vm.changeBody("¿Qué pasos faltan?");runCurrent()
        fail = true;vm.send();runCurrent()
        assertEquals("¿Qué pasos faltan?", saved["assistant:alice:one"]?.body)
        fail = false;vm.send();runCurrent()
        assertEquals(ids[0], ids[1]);assertNull(saved["assistant:alice:one"])
        assertFalse(vm.uiState.value.canResolve);assertEquals(0,resolved)
    }

    @Test fun accountAndOrderChangesNeverReuseAnotherDraft() = runTest {
        val vm = model();vm.bind("one");runCurrent()
        vm.changeBody("Mi borrador");runCurrent()
        vm.bind("two");runCurrent();assertEquals("",vm.uiState.value.draft?.body)
        vm.bind("one");runCurrent();assertEquals("Mi borrador",vm.uiState.value.draft?.body)
        session.user.value = RiderUser(id = "bob");runCurrent()
        assertEquals("",vm.uiState.value.draft?.body)
        assertEquals("Mi borrador",saved["assistant:alice:one"]?.body)
    }

    @Test fun resolvingRequiresServerEligibilityAndHumanHandoffDisablesIt() = runTest {
        val vm = model();vm.bind("one");runCurrent()
        vm.resolve {};runCurrent();assertEquals(0,resolved)
        allowed = true;vm.changeBody("Revisa mis requisitos");runCurrent();vm.send();runCurrent()
        assertTrue(vm.uiState.value.canResolve)
        vm.send(human = true);runCurrent()
        assertTrue(vm.uiState.value.needsHuman);assertFalse(vm.uiState.value.canResolve)
        vm.resolve {};runCurrent();assertEquals(0,resolved)
    }
}
