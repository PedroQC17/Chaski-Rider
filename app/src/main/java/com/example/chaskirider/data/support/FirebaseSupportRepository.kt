package com.example.chaskirider.data.support

import com.example.chaskirider.domain.support.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout

class FirebaseSupportRepository(private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()) : SupportRepository {
    private fun tickets(uid: String) = db.collection("riders").document(uid).collection("supportTickets")
    private fun requireUser(uid: String) { check(auth.currentUser?.uid == uid) { "La sesión cambió" } }

    override fun observe(uid: String) = callbackFlow {
        requireUser(uid)
        val listener = tickets(uid).orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, error ->
                if (error != null) close(error)
                else if (snapshot != null && auth.currentUser?.uid == uid) {
                    try { trySend(snapshot.documents.map(::ticket)) }
                    catch (e: Exception) { close(e) }
                }
            }
        awaitClose { listener.remove() }
    }

    override fun observeMessages(uid: String, ticketId: String) = callbackFlow {
        requireUser(uid)
        val listener = tickets(uid).document(ticketId).collection("messages").orderBy("createdAt")
            .addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, error ->
                if (error != null) close(error)
                else if (snapshot != null && auth.currentUser?.uid == uid) {
                    try { trySend(snapshot.documents.map { doc -> SupportMessage(doc.id,
                        SupportSender.valueOf(requireNotNull(doc.getString("sender"))), requireNotNull(doc.getString("body")),
                        doc.getTimestamp("createdAt")?.toDate()?.time ?: 0, doc.metadata.hasPendingWrites()) }) }
                    catch (e: Exception) { close(e) }
                }
            }
        awaitClose { listener.remove() }
    }

    override suspend fun create(uid: String, draft: SupportDraft): String = withTimeout(15_000) {
        requireUser(uid)
        val topic = requireNotNull(draft.topic)
        require(SupportValidation.validBody(draft.body))
        val ref = tickets(uid).document(draft.id)
        val order = draft.order?.let { mapOf("orderId" to it.orderId, "merchantName" to it.merchantName,
            "pickupAddress" to it.pickupAddress, "deliveryAddress" to it.deliveryAddress,
            "pickedUp" to it.pickedUp, "problemId" to it.problemId,
            "orderStatus" to (it.orderStatus ?: if (it.pickedUp) com.example.chaskirider.domain.orders.OrderStatus.ON_THE_WAY
                else com.example.chaskirider.domain.orders.OrderStatus.ACCEPTED).name, "problemCategory" to it.problemCategory?.name) }
        val location = draft.location?.let { mapOf("latitude" to it.latitude, "longitude" to it.longitude, "capturedAt" to it.capturedAt) }
        db.runTransaction { transaction ->
            val existing = transaction.get(ref)
            if (existing.exists()) {
                check(existing.getString("topic") == topic.name && existing.getString("body") == draft.body.trim()
                    && existing.get("order") == order && existing.get("location") == location) { "Este ticket ya fue registrado con otro contenido" }
            } else transaction.set(ref, mapOf("topic" to topic.name, "category" to topic.category.name,
                "body" to draft.body.trim(), "order" to order, "location" to location,
                "priority" to if (topic == SupportTopic.EMERGENCY) "URGENT" else "NORMAL",
                "status" to "OPEN", "createdAt" to FieldValue.serverTimestamp()))
            draft.id
        }.await()
    }

    override suspend fun reply(uid: String, ticketId: String, messageId: String, body: String): Unit = withTimeout(15_000) {
        requireUser(uid)
        require(SupportValidation.validReply(body))
        val parent = tickets(uid).document(ticketId)
        val message = parent.collection("messages").document(messageId)
        db.runTransaction { transaction ->
            val ticket = transaction.get(parent)
            val existing = transaction.get(message)
            if (existing.exists()) check(existing.getString("body") == body.trim() && existing.getString("sender") == "RIDER")
            else {
                check(ticket.getString("status") == "OPEN") { "El ticket está cerrado" }
                transaction.set(message, mapOf("sender" to "RIDER", "body" to body.trim(), "createdAt" to FieldValue.serverTimestamp()))
            }
            Unit
        }.await()
    }

    private fun ticket(doc: DocumentSnapshot): SupportTicket {
        val order = (doc.get("order") as? Map<*, *>)?.let { SupportOrderContext(
            it["orderId"] as String, it["merchantName"] as String, it["pickupAddress"] as String,
            it["deliveryAddress"] as String, it["pickedUp"] as Boolean, it["problemId"] as? String,
            (it["orderStatus"] as? String)?.let(com.example.chaskirider.domain.orders.OrderStatus::valueOf)
                ?: if (it["pickedUp"] == true) com.example.chaskirider.domain.orders.OrderStatus.ON_THE_WAY
                else com.example.chaskirider.domain.orders.OrderStatus.ACCEPTED,
            (it["problemCategory"] as? String)?.let(com.example.chaskirider.domain.orders.OrderProblemReason::valueOf)) }
        val time = doc.getTimestamp("createdAt")?.toDate()?.time ?: 0
        val pending = doc.metadata.hasPendingWrites()
        val location = (doc.get("location") as? Map<*, *>)?.let { SupportLocation(
            (it["latitude"] as Number).toDouble(), (it["longitude"] as Number).toDouble(), (it["capturedAt"] as Number).toLong()) }
        return SupportTicket(doc.id, SupportTopic.valueOf(requireNotNull(doc.getString("topic"))), time,
            SupportTicketStatus.valueOf(requireNotNull(doc.getString("status"))), order,
            listOf(SupportMessage("initial", SupportSender.RIDER, requireNotNull(doc.getString("body")), time, pending)), pending, location)
    }
}
