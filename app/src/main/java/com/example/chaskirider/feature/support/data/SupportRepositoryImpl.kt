package com.example.chaskirider.feature.support.data

import com.example.chaskirider.core.firebase.FirebaseRoot
import com.example.chaskirider.feature.support.data.local.SupportStore
import com.example.chaskirider.feature.support.data.remote.SupportRemote
import com.example.chaskirider.feature.support.domain.AssistantReply
import com.example.chaskirider.feature.support.domain.AssistantRepository
import com.example.chaskirider.feature.support.domain.SupportDraft
import com.example.chaskirider.feature.support.domain.SupportRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout

class SupportRepositoryImpl(
    private val remote: SupportRemote = SupportRemote(),
    private val store: SupportStore? = null
) : SupportRepository {
    override fun observe(uid: String) = remote.observe(uid).onEach { tickets ->
        store?.let { local ->
            local.saveTickets(uid, tickets)
            tickets.forEach { local.saveMessages(uid, it.id, it.messages) }
        }
    }

    override fun observeMessages(uid: String, ticketId: String) =
        remote.observeMessages(uid, ticketId).onEach { messages ->
            store?.saveMessages(uid, ticketId, messages)
        }

    override suspend fun create(uid: String, draft: SupportDraft) = remote.create(uid, draft)
    override suspend fun reply(uid: String, ticketId: String, messageId: String, body: String) =
        remote.reply(uid, ticketId, messageId, body)
}

class FirebaseAssistantRepository(private val support: SupportRepository,
    private val awaitSync: suspend (String, String) -> Unit = { _, _ -> }) : AssistantRepository {
    override fun messages(uid: String, orderId: String) = support.observeMessages(uid, "absence_$orderId")
    override fun ticket(uid: String, orderId: String) = support.observe(uid).map { rows -> rows.firstOrNull { it.id == "absence_$orderId" } }
    override suspend fun send(uid: String, orderId: String, messageId: String, body: String, human: Boolean): AssistantReply {
        check(FirebaseAuth.getInstance().currentUser?.uid == uid)
        awaitSync(uid, orderId)
        val result = withTimeout(55_000) {
            FirebaseFunctions.getInstance(FirebaseRoot.REGION).getHttpsCallable(FirebaseRoot.Callables.RIDER_ASSISTANT).call(mapOf(
                "action" to if (human) "human" else "send", "orderId" to orderId, "messageId" to messageId, "body" to body)).await().data as Map<*, *>
        }
        check(FirebaseAuth.getInstance().currentUser?.uid == uid)
        return AssistantReply(result["mode"] as String, result["needsHuman"] == true, result["canResolve"] == true, result["ticketId"] as String)
    }
}
