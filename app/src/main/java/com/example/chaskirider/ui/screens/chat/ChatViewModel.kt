package com.example.chaskirider.ui.screens.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chaskirider.domain.chat.*
import com.example.chaskirider.domain.orders.OfferSnapshot
import com.example.chaskirider.domain.repository.RiderSession
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.UUID

class ChatViewModel(private val repository: OrderChatRepository, private val drafts: ChatDraftStore,
    private val session: RiderSession) : ViewModel() {
    private val mutable = MutableStateFlow(ChatUiState())
    val uiState = mutable.asStateFlow()
    private var uid = session.user.value?.id
    private var orderId: String? = null
    private var reader: Job? = null
    private var sender: Job? = null
    private var writes: Job? = null
    private var draft = ChatDraft(newId())
    private var loadedKey: String? = null

    init {
        viewModelScope.launch {
            session.user.map { it?.id }.distinctUntilChanged().collect { id ->
                if (id != uid) {
                    uid = id; orderId = null; loadedKey = null
                    reader?.cancel(); sender?.cancel()
                    draft = ChatDraft(newId())
                    mutable.value = ChatUiState()
                }
            }
        }
    }

    private fun newId() = UUID.randomUUID().toString()
    private fun key(): String? = uid?.let { user -> orderId?.let { "$user/$it/${mutable.value.activeChannel.name}" } }
    private fun canSend(): Boolean = key() != null && if (mutable.value.activeChannel == ChatChannel.MERCHANT)
        mutable.value.isMerchantChannelEnabled else mutable.value.isCustomerChannelEnabled

    fun syncOrderState(snapshot: OfferSnapshot?) {
        val id = snapshot?.batch?.stops?.singleOrNull()?.orderId
        if (id != orderId) {
            reader?.cancel(); sender?.cancel(); loadedKey = null
            orderId = id; draft = ChatDraft(newId()); mutable.value = ChatUiState()
        }
        mutable.update { it.copy(isMerchantChannelEnabled = id != null && snapshot?.pickedUp == false && !snapshot.isDelivered,
            isCustomerChannelEnabled = id != null && snapshot?.isDelivered == false) }
        if (!canSend()) { sender?.cancel(); mutable.update { it.copy(busy = false) } }
    }

    fun openChat() {
        if (key() == null) return
        mutable.update { it.copy(isOpen = true) }
        loadChannel()
    }
    fun closeChat() {
        mutable.update { it.copy(isOpen = false, isListeningSpeech = false) }
        reader?.cancel()
    }
    fun selectChannel(channel: ChatChannel) {
        if (mutable.value.busy || channel == mutable.value.activeChannel) return
        mutable.update { it.copy(activeChannel = channel, inputText = "", messages = emptyList(), error = null) }
        loadChannel()
    }
    fun retry() { if (!mutable.value.busy) loadChannel() }

    private fun loadChannel() {
        val owner = uid ?: return
        val order = orderId ?: return
        val channel = mutable.value.activeChannel
        val target = key() ?: return
        val previous = draft.takeIf { loadedKey == target }
        reader?.cancel()
        mutable.update { it.copy(loading = true, error = null, fromCache = false) }
        reader = viewModelScope.launch {
            try {
                writes?.join()
                val restored = previous ?: drafts.load(target) ?: ChatDraft(newId())
                if (target != key()) return@launch
                draft = restored; loadedKey = target
                mutable.update { it.copy(inputText = restored.body) }
                repository.observe(owner, order, channel).collect { conversation ->
                    if (target == key()) mutable.update { it.copy(messages = conversation.messages,
                        fromCache = conversation.fromCache, loading = false) }
                }
            } catch (error: CancellationException) { throw error }
            catch (_: Exception) {
                if (target == key()) mutable.update { it.copy(loading = false,
                    error = "No se pudo cargar la conversación. Revisa tu conexión y vuelve a intentar.") }
            }
        }
    }

    private fun saveDraft(target: String, value: ChatDraft) {
        val previous = writes
        writes = viewModelScope.launch {
            previous?.join()
            try { drafts.save(target, value) }
            catch (error: CancellationException) { throw error }
            catch (_: Exception) { if (key() == target) mutable.update { it.copy(error = "No se pudo guardar el borrador en este dispositivo.") } }
        }
    }

    fun updateInputText(text: String) {
        if (!canSend() || mutable.value.busy || mutable.value.loading) return
        val target = key() ?: return
        draft = draft.copy(body = text.take(2000))
        mutable.update { it.copy(inputText = draft.body, error = null) }
        saveDraft(target, draft)
    }
    fun onSpeechRecognized(text: String) {
        if (text.isBlank()) return
        updateInputText(listOf(mutable.value.inputText, text).filter { it.isNotBlank() }.joinToString(" "))
        mutable.update { it.copy(isListeningSpeech = false) }
    }
    fun setListeningSpeech(listening: Boolean) { mutable.update { it.copy(isListeningSpeech = listening, speechError = null) } }
    fun setSpeechError(error: String) { mutable.update { it.copy(isListeningSpeech = false, speechError = error) } }

    fun sendMessage() {
        if (!canSend() || mutable.value.busy || mutable.value.loading || draft.body.trim().isEmpty()) return
        val owner = uid ?: return
        val order = orderId ?: return
        val channel = mutable.value.activeChannel
        val target = key() ?: return
        val confirmed = mutable.value.messages.firstOrNull { it.id == draft.id }
        if (confirmed != null && confirmed.text != draft.body.trim()) draft = draft.copy(id = newId())
        val sending = draft
        mutable.update { it.copy(busy = true, error = null) }
        sender = viewModelScope.launch {
            try {
                writes?.join()
                drafts.save(target, sending)
                if (key() != target || !canSend()) return@launch
                repository.send(owner, order, channel, sending.id, sending.body)
                drafts.clear(target, sending)
                if (key() == target) {
                    draft = ChatDraft(newId())
                    mutable.update { it.copy(inputText = "") }
                }
            } catch (_: TimeoutCancellationException) {
                if (key() == target) mutable.update { it.copy(error = "No se confirmó el envío. Tu borrador se conserva; revisa la conversación antes de reintentar.") }
            } catch (error: CancellationException) { throw error }
            catch (_: Exception) {
                if (key() == target) mutable.update { it.copy(error = "No se pudo completar el envío. Tu borrador se conserva; comprueba la conexión y la etapa del pedido.") }
            } finally { if (key() == target) mutable.update { it.copy(busy = false) } }
        }
    }
}
