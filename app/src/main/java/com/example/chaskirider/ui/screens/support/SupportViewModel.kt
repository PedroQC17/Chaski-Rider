package com.example.chaskirider.ui.screens.support

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chaskirider.domain.orders.OrderProblemReason
import com.example.chaskirider.domain.repository.RiderSession
import com.example.chaskirider.domain.support.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.UUID

class SupportViewModel(private val repository: SupportRepository, private val drafts: SupportDraftStore,
    private val session: RiderSession,
    private val locations: com.example.chaskirider.domain.repository.LocationRepository? = null) : ViewModel() {
    private val mutable = MutableStateFlow(SupportUiState())
    val uiState = mutable.asStateFlow()
    private var uid: String? = null
    private var writes: Job? = null
    private var action: Job? = null
    private var ticketLoad: Job? = null
    private var conversationJob: Job? = null
    private val refreshes = MutableStateFlow(0)
    private var pendingOrder: Triple<SupportOrderContext, OrderProblemReason?, String?>? = null
    private var pendingOrderUid: String? = null

    init {
        viewModelScope.launch {
            combine(session.user.map { it?.id }.distinctUntilChanged(), refreshes) { id, _ -> id }.collectLatest { userId ->
                action?.cancel(); ticketLoad?.cancel(); conversationJob?.cancel()
                val previous = mutable.value.takeIf { uid == userId }
                uid = userId
                mutable.value = previous?.copy(error = null) ?: SupportUiState()
                if (userId == null) return@collectLatest
                try {
                    writes?.join()
                    val draft = previous?.draft ?: drafts.load(userId) ?: SupportDraft(newId())
                    if (previous?.draft != null) drafts.save(userId, draft)
                    mutable.update { if (previous?.draft != null) it.copy(loading = false) else
                        it.copy(loading = false, draft = draft, topic = draft.topic,
                            category = draft.topic?.category, formOpen = draft.body.isNotBlank()) }
                    val pending = pendingOrder
                    pendingOrder = null
                    if (pending != null && pendingOrderUid == userId) openForOrder(pending.first, pending.second, pending.third)
                    if (previous?.selectedTicketId != null) openTicket(previous.selectedTicketId)
                } catch (e: CancellationException) { throw e }
                catch (_: Exception) {
                    mutable.update { it.copy(loading = false, error = "No se pudo recuperar el borrador. Vuelve a abrir Ayuda.") }
                }
                repository.observe(userId).catch {
                    mutable.update { it.copy(error = "No se pudieron consultar tus tickets. Comprueba tu conexión y vuelve a intentar.") }
                }.collect { tickets -> mutable.update { it.copy(tickets = tickets) } }
            }
        }
    }

    fun refresh() { if (!mutable.value.busy) refreshes.update { it + 1 } }

    fun selectCategory(category: SupportCategory) { mutable.update { it.copy(category = category, topic = null, notice = null) } }
    fun selectTopic(topic: SupportTopic) { mutable.update { it.copy(topic = topic, notice = null) } }
    fun openForm() {
        val state = mutable.value
        val topic = state.topic ?: return
        val draft = state.draft ?: return
        if (state.busy) return
        if (draft.body.isNotBlank() && draft.topic != topic) {
            mutable.update { it.copy(formOpen = true, topic = draft.topic, category = draft.topic?.category,
                notice = "Recuperamos tu borrador anterior. Puedes enviarlo o descartarlo para crear otro.") }
        } else updateDraft(draft.copy(topic = topic, order = if (topic.category == SupportCategory.ORDER)
            draft.order ?: state.availableOrder else null, location = null))
        mutable.update { it.copy(formOpen = true) }
    }

    fun setAvailableOrder(order: SupportOrderContext?) { mutable.update { it.copy(availableOrder = order) } }
    fun openForOrder(order: SupportOrderContext, reason: OrderProblemReason?, detail: String?) {
        setAvailableOrder(order)
        val draft = mutable.value.draft
        if (draft == null) {
            pendingOrder = Triple(order, reason, detail)
            pendingOrderUid = session.user.value?.id
            return
        }
        val topic = when (reason) {
            OrderProblemReason.LOCAL_CLOSED -> SupportTopic.LOCAL_CLOSED
            OrderProblemReason.CUSTOMER_UNREACHABLE -> SupportTopic.CUSTOMER_UNREACHABLE
            OrderProblemReason.ADDRESS_PROBLEM -> SupportTopic.ADDRESS_PROBLEM
            else -> SupportTopic.ORDER_OTHER
        }
        if (draft.body.isNotBlank() && (draft.order?.orderId != order.orderId || draft.order?.problemId != order.problemId)) {
            mutable.update { it.copy(formOpen = true, topic = draft.topic, category = draft.topic?.category,
                notice = "Tienes un borrador anterior. Puedes enviarlo o descartarlo antes de reportar este pedido.") }
            return
        }
        updateDraft(draft.copy(topic = topic, order = order, body = draft.body.ifBlank { detail.orEmpty() }))
        mutable.update { it.copy(category = SupportCategory.ORDER, topic = topic, formOpen = true) }
    }

    fun changeBody(body: String) { mutable.value.draft?.let { updateDraft(it.copy(body = body.take(2000))) } }
    fun prepareEmergency() {
        val owner = uid ?: return
        val draft = mutable.value.draft ?: return
        if (mutable.value.busy) return
        if (draft.body.isNotBlank() && draft.topic != SupportTopic.EMERGENCY) {
            mutable.update { it.copy(formOpen = true, topic = draft.topic, category = draft.topic?.category,
                notice = "Conservamos tu borrador. Envíalo o descártalo antes de abrir el caso de seguridad.") }
            return
        }
        mutable.update { it.copy(busy = true, error = null) }
        action = viewModelScope.launch {
            try {
                val point = locations?.currentLocation()?.getOrNull()
                if (uid != owner) return@launch
                mutable.update { it.copy(busy = false) }
                updateDraft(draft.copy(topic = SupportTopic.EMERGENCY, order = mutable.value.availableOrder,
                    location = point?.let { SupportLocation(it.latitude, it.longitude, System.currentTimeMillis()) }))
                mutable.update { it.copy(topic = SupportTopic.EMERGENCY, category = SupportCategory.SAFETY,
                    formOpen = true, notice = if (point == null) "No pudimos obtener una ubicación reciente. Puedes describir dónde estás y enviar el caso sin coordenadas."
                    else "Ubicación adjunta. Describe lo ocurrido y confirma el envío del caso urgente.") }
            } catch (error: CancellationException) { throw error }
            finally { if (uid == owner) mutable.update { it.copy(busy = false) } }
        }
    }
    fun linkOrder(link: Boolean) { mutable.value.draft?.let { updateDraft(it.copy(order = if (link) mutable.value.availableOrder else null)) } }
    private fun updateDraft(draft: SupportDraft) {
        if (mutable.value.busy) return
        val userId = uid ?: return
        mutable.update { it.copy(draft = draft, error = null) }
        save(userId, draft)
    }

    private fun save(key: String, draft: SupportDraft) {
        val previous = writes
        writes = viewModelScope.launch {
            previous?.join()
            try { drafts.save(key, draft) }
            catch (e: CancellationException) { throw e }
            catch (_: Exception) {
                if (uid != null && (key == uid || key.startsWith("$uid/reply/"))) mutable.update {
                    it.copy(error = "No se pudo guardar el borrador. Mantén esta pantalla abierta y vuelve a intentar.")
                }
            }
        }
    }

    fun discardDraft() {
        if (mutable.value.busy) return
        updateDraft(SupportDraft(newId(), topic = mutable.value.topic))
        mutable.update { it.copy(notice = null) }
    }

    fun createTicket() {
        val state = mutable.value
        var draft = state.draft ?: return
        val userId = uid ?: return
        if (state.busy || draft.topic == null || !SupportValidation.validBody(draft.body)) return
        val registered = state.tickets.firstOrNull { it.id == draft.id && !it.pending }
        if (registered != null && (registered.topic != draft.topic || registered.order != draft.order || registered.location != draft.location ||
                registered.messages.firstOrNull()?.body != draft.body.trim())) {
            draft = draft.copy(id = newId())
            mutable.update { it.copy(draft = draft) }
        }
        mutable.update { it.copy(busy = true, error = null) }
        action = viewModelScope.launch {
            try {
                writes?.join()
                drafts.save(userId, draft)
                val id = repository.create(userId, draft)
                drafts.clear(userId, draft.id)
                if (uid == userId) {
                    mutable.update { it.copy(draft = SupportDraft(newId()), formOpen = false, topic = null,
                        category = null, notice = null) }
                    openTicket(id)
                }
            } catch (_: TimeoutCancellationException) {
                if (uid == userId) mutable.update { it.copy(error = "Firebase no confirmó el envío a tiempo. Tu borrador se conserva; revisa Mis tickets antes de reintentar.") }
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { if (uid == userId) mutable.update { it.copy(error = "No se pudo completar la operación. Tu borrador se conserva; revisa Mis tickets antes de reintentar.") } }
            finally { if (uid == userId) mutable.update { it.copy(busy = false) } }
        }
    }

    fun openTicket(id: String) {
        val userId = uid ?: return
        mutable.update { it.copy(selectedTicketId = id, reply = null, replyLoading = true, conversation = emptyList(), error = null, notice = null) }
        conversationJob?.cancel()
        conversationJob = viewModelScope.launch {
            repository.observeMessages(userId, id).catch {
                if (uid == userId) mutable.update { it.copy(error = "No se pudieron consultar los mensajes. Vuelve a abrir el ticket.") }
            }.collect { messages -> if (uid == userId && mutable.value.selectedTicketId == id) mutable.update { it.copy(conversation = messages) } }
        }
        ticketLoad?.cancel()
        ticketLoad = viewModelScope.launch {
            try {
                writes?.join()
                val reply = drafts.load(replyKey(userId, id)) ?: SupportDraft(newId())
                if (uid == userId && mutable.value.selectedTicketId == id) mutable.update { it.copy(reply = reply, replyLoading = false) }
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { if (uid == userId) mutable.update { it.copy(replyLoading = false, error = "No se pudo recuperar tu mensaje. Vuelve a abrir el ticket.") } }
        }
    }

    fun changeReply(body: String) {
        val state = mutable.value
        val userId = uid ?: return
        val id = state.selectedTicketId ?: return
        val reply = state.reply ?: return
        if (state.busy) return
        val updated = reply.copy(body = body.take(2000))
        mutable.update { it.copy(reply = updated, error = null) }
        save(replyKey(userId, id), updated)
    }

    fun sendReply() {
        val state = mutable.value
        val userId = uid ?: return
        val id = state.selectedTicketId ?: return
        var reply = state.reply ?: return
        if (state.busy || !SupportValidation.validReply(reply.body) || state.tickets.firstOrNull { it.id == id }?.status != SupportTicketStatus.OPEN) return
        val sent = state.conversation.firstOrNull { it.id == reply.id && !it.pending && it.sender == SupportSender.RIDER }
        if (sent != null && sent.body != reply.body.trim()) {
            reply = reply.copy(id = newId())
            mutable.update { it.copy(reply = reply) }
        }
        mutable.update { it.copy(busy = true, error = null) }
        action = viewModelScope.launch {
            try {
                writes?.join()
                drafts.save(replyKey(userId, id), reply)
                repository.reply(userId, id, reply.id, reply.body)
                drafts.clear(replyKey(userId, id), reply.id)
                if (uid == userId && mutable.value.selectedTicketId == id) mutable.update { it.copy(reply = SupportDraft(newId())) }
            } catch (_: TimeoutCancellationException) {
                if (uid == userId) mutable.update { it.copy(error = "Firebase no confirmó el mensaje a tiempo. El texto se conserva; revisa la conversación antes de reintentar.") }
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { if (uid == userId) mutable.update { it.copy(error = "No se pudo completar la operación. El texto se conserva; revisa la conversación antes de reintentar.") } }
            finally { if (uid == userId) mutable.update { it.copy(busy = false) } }
        }
    }

    fun back(): Boolean {
        val state = mutable.value
        if (state.busy) return true
        return when {
            state.selectedTicketId != null -> { conversationJob?.cancel(); ticketLoad?.cancel(); mutable.update { it.copy(selectedTicketId = null, reply = null, conversation = emptyList(), error = null) }; true }
            state.formOpen -> { mutable.update { it.copy(formOpen = false, error = null, notice = null) }; true }
            state.topic != null -> { mutable.update { it.copy(topic = null) }; true }
            state.category != null -> { mutable.update { it.copy(category = null) }; true }
            else -> false
        }
    }

    private fun replyKey(uid: String, ticket: String) = "$uid/reply/$ticket"
    private fun newId() = UUID.randomUUID().toString()
}
