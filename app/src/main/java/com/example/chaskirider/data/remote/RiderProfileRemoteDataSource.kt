package com.example.chaskirider.data.remote

import com.example.chaskirider.domain.model.*
import com.google.firebase.auth.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await

import com.example.chaskirider.data.session.RiderSessionStore

class RiderProfileRemoteDataSource(
    private val session: RiderSessionStore,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val functions: FirebaseFunctions = FirebaseFunctions.getInstance("us-central1")
) {
    suspend fun load(): RiderUser {
        val user = auth.currentUser ?: error("No hay una sesión activa")
        
        val snapshot = db.collection("riders").document(user.uid).get(Source.SERVER).await()
        val rider = snapshot.toObject(RiderUser::class.java)?.copy(id = user.uid, email = user.email.orEmpty())
            ?: RiderUser(id = user.uid, email = user.email.orEmpty(),
                name = user.displayName?.substringBefore(" ").orEmpty(),
                lastName = user.displayName?.substringAfter(" ", "").orEmpty())
        check(auth.currentUser?.uid == user.uid) { "La sesión cambió. Vuelve a ingresar." }
        session.update(rider)
        return rider
    }

    suspend fun mutate(action: String, payload: Map<String, Any> = emptyMap()): RiderUser {
        val uid = auth.currentUser?.uid ?: error("No hay una sesión activa")
        functions.getHttpsCallable("riderRegistration").call(payload + ("action" to action)).await()
        check(auth.currentUser?.uid == uid) { "La sesión cambió. Vuelve a ingresar." }
        return load()
    }
}
