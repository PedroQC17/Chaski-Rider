package com.example.chaskirider.feature.earnings.data.remote

import com.example.chaskirider.core.firebase.FirebaseRoot

object EarningsRoutes {
    fun settlements(uid: String) = "${FirebaseRoot.riderDoc(uid)}/settlements"
    fun connectionSessions(uid: String) = "${FirebaseRoot.riderDoc(uid)}/connectionSessions"
}
