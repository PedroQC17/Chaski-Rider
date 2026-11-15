package com.example.chaskirider.feature.documents.data.remote

import com.example.chaskirider.core.firebase.FirebaseRoot

object DocumentRoutes {
    fun file(uid: String, type: String, id: String) = "${FirebaseRoot.riderDoc(uid)}/documents/$type/$id"
    fun filePrefix(uid: String, type: String) = "${FirebaseRoot.riderDoc(uid)}/documents/$type/"
}
