package com.example.chaskirider.feature.support.data.remote

import com.example.chaskirider.core.firebase.FirebaseRoot

object SupportRoutes {
    fun tickets(uid: String) = "${FirebaseRoot.riderDoc(uid)}/supportTickets"
    fun messages(uid: String, ticketId: String) = "${tickets(uid)}/$ticketId/messages"
}
