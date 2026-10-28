package com.example.chaskirider.data.chat

import com.example.chaskirider.domain.chat.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout

class FirebaseOrderChatRepository(private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()) : OrderChatRepository {
    private fun order(uid: String, orderId: String) = db.collection("riders").document(uid)
        .collection("deliveryRecords").document(orderId)
    private fun messages(uid: String, orderId: String, channel: ChatChannel) = order(uid, orderId)
        .collection("chats").document(channel.name).collection("messages")

    override fun observe(uid: String, orderId: String, channel: ChatChannel) = callbackFlow {
        check(auth.currentUser?.uid == uid)
        val listener = messages(uid, orderId, channel).orderBy("createdAt")
            .addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, error ->
                if (error != null) close(error)
                else if (snapshot != null && auth.currentUser?.uid == uid) {
                    try {
                        val result = snapshot.documents.map { doc ->
                            val role = ChatSenderRole.valueOf(requireNotNull(doc.getString("senderRole")))
                            check(doc.getString("orderId") == orderId && doc.getString("channel") == channel.name)
                            ChatMessage(doc.id, orderId, channel, when (role) {
                                ChatSenderRole.RIDER -> "Tú"
                                ChatSenderRole.MERCHANT -> "Establecimiento"
                                ChatSenderRole.CUSTOMER -> "Cliente"
                            }, role, requireNotNull(doc.getString("text")), doc.getTimestamp("createdAt")?.toDate()?.time ?: 0)
                        }
                        trySend(ChatConversation(result, snapshot.metadata.isFromCache))
                    } catch (error: Exception) { close(error) }
                }
            }
        awaitClose { listener.remove() }
    }

    override suspend fun send(uid: String, orderId: String, channel: ChatChannel, messageId: String, body: String): Unit = withTimeout(15_000) {
        check(auth.currentUser?.uid == uid)
        require(body.trim().length in 1..2000)
        val parent = order(uid, orderId)
        val ref = messages(uid, orderId, channel).document(messageId)
        db.runTransaction { tx ->
            val record = tx.get(parent)
            val existing = tx.get(ref)
            check(auth.currentUser?.uid == uid)
            if (existing.exists()) {
                check(existing.getString("text") == body.trim() && existing.getString("senderId") == uid &&
                    existing.getString("senderRole") == "RIDER") { "El mensaje ya existe con otro contenido" }
            } else {
                val stages = if (channel == ChatChannel.MERCHANT) listOf("ACCEPTED", "AT_MERCHANT")
                    else listOf("ACCEPTED", "AT_MERCHANT", "ON_THE_WAY")
                check(record.getString("status") in stages) { "El canal ya no permite enviar mensajes" }
                tx.set(ref, mapOf("orderId" to orderId, "channel" to channel.name, "senderId" to uid,
                    "senderRole" to "RIDER", "text" to body.trim(), "createdAt" to FieldValue.serverTimestamp()))
            }
            Unit
        }.await()
    }
}
