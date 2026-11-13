package com.example.chaskirider.feature.support.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chaskirider.feature.chat.domain.ChatDraft
import com.example.chaskirider.feature.chat.domain.ChatDraftStore
import com.example.chaskirider.feature.user.domain.RiderSession
import com.example.chaskirider.feature.support.domain.SupportTicketStatus
import com.example.chaskirider.feature.support.domain.SupportMessage
import com.example.chaskirider.feature.support.domain.AssistantRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.UUID

data class AssistantUiState(
    val messages: List<SupportMessage> = emptyList(), val draft: ChatDraft? = null,
    val loading: Boolean = true, val busy: Boolean = false, val error: String? = null,
    val mode: String? = null, val needsHuman: Boolean = false, val closed: Boolean = false,
    val canResolve: Boolean = false, val ticketId: String? = null
)

class AssistantViewModel(private val repository: AssistantRepository, private val drafts: ChatDraftStore,
    private val session: RiderSession, private val finishOrder: suspend (String) -> Unit) : ViewModel() {
    private val mutable = MutableStateFlow(AssistantUiState())
    val uiState = mutable.asStateFlow()
    private val order = MutableStateFlow<String?>(null)
    private var owner: String? = null
    private var currentOrder: String? = null
    private var writes: Job? = null
    private var action: Job? = null
    private fun key(uid: String, id: String) = "assistant:$uid:$id"
    private fun matches(uid: String, id: String) = session.user.value?.id == uid && currentOrder == id && owner == uid
    init {
        viewModelScope.launch {
            combine(session.user.map { it?.id }.distinctUntilChanged(), order) { uid, id -> uid to id }.collectLatest { (uid, id) ->
                action?.cancel(); writes?.join()
                owner = uid; currentOrder = id; mutable.value = AssistantUiState()
                if (uid == null || id == null) return@collectLatest
                try {
                    val draft = drafts.load(key(uid, id)) ?: ChatDraft(UUID.randomUUID().toString(), "")
                    if (matches(uid, id)) mutable.update { it.copy(draft = draft, loading = false) }
                    coroutineScope {
                        launch {
                            repository.messages(uid, id).collect { messages ->
                                if (matches(uid, id)) mutable.update { it.copy(messages = messages, mode = messages.lastOrNull { m -> m.mode != null }?.mode) }
                            }
                        }
                        repository.ticket(uid, id).collect { ticket ->
                            if (matches(uid, id)) mutable.update { it.copy(needsHuman = ticket?.needsHuman == true,
                                closed = ticket?.status == SupportTicketStatus.CLOSED, ticketId = ticket?.id) }
                        }
                    }
                } catch (error: CancellationException) { throw error }
                catch (_: Exception) { if (matches(uid, id)) mutable.update { it.copy(loading = false, error = "No se pudo recuperar la conversación. Cierra y vuelve a abrir el asistente.") } }
            }
        }
    }
    fun bind(id: String) { order.value = id }
    fun changeBody(body: String) {
        val uid = owner ?: return
        val id = currentOrder ?: return
        if (mutable.value.busy || mutable.value.loading || !matches(uid, id)) return
        val draft = ChatDraft(UUID.randomUUID().toString(), body.take(1000))
        mutable.update { it.copy(draft = draft) }
        writes?.cancel()
        writes = viewModelScope.launch { try { drafts.save(key(uid, id), draft) }
            catch (error: CancellationException) { throw error }
            catch (_: Exception) { if (matches(uid, id)) mutable.update { it.copy(error = "No se pudo guardar el borrador.") } } }
    }
    fun send(human: Boolean = false) {
        val uid = owner ?: return
        val id = currentOrder ?: return
        val draft = mutable.value.draft ?: return
        if (!matches(uid, id) || mutable.value.busy || mutable.value.closed || mutable.value.loading) return
        val sent = if (human) ChatDraft(UUID.randomUUID().toString(), draft.body.ifBlank { "Necesito hablar con un asesor humano" }) else draft
        if (sent.body.isBlank()) return
        mutable.update { it.copy(busy = true, error = null) }
        action = viewModelScope.launch {
            try {
                writes?.join()
                drafts.save(key(uid, id), sent)
                val reply = repository.send(uid, id, sent.id, sent.body, human)
                drafts.clear(key(uid, id), sent)
                if (matches(uid, id)) mutable.update { it.copy(draft = ChatDraft(UUID.randomUUID().toString(), ""),
                    mode = reply.mode, needsHuman = reply.needsHuman, canResolve = reply.canResolve, ticketId = reply.ticketId) }
            } catch (error: CancellationException) { throw error }
            catch (_: Exception) { if (matches(uid, id)) mutable.update { it.copy(error = "No se pudo enviar. Conservamos tu mensaje para reintentar; también puedes solicitar un asesor humano.") } }
            finally { if (matches(uid, id)) mutable.update { it.copy(busy = false) } }
        }
    }
    fun resolve(onResolved: () -> Unit) {
        val uid = owner ?: return
        val id = currentOrder ?: return
        if (!matches(uid, id) || mutable.value.busy || !mutable.value.canResolve || mutable.value.needsHuman) return
        mutable.update { it.copy(busy = true, error = null) }
        action = viewModelScope.launch {
            try { finishOrder(id); if (matches(uid, id)) onResolved() }
            catch (error: CancellationException) { throw error }
            catch (_: Exception) { if (matches(uid, id)) mutable.update { it.copy(error = "No se confirmó la resolución. Revisa los requisitos o consulta con un asesor.") } }
            finally { if (matches(uid, id)) mutable.update { it.copy(busy = false) } }
        }
    }
}
