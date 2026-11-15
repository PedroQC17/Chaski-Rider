package com.example.chaskirider.feature.rider.data.remote

import com.example.chaskirider.core.firebase.FirebaseRoot

object RiderRoutes {
    fun doc(uid: String) = FirebaseRoot.riderDoc(uid)
    fun profilePhoto(uid: String, id: String) = "${doc(uid)}/profilePhoto/$id"
    fun profilePhotoPrefix(uid: String) = "${doc(uid)}/profilePhoto/"
}
