package com.example.chaskirider.feature.vehicle.data.remote

import com.example.chaskirider.core.firebase.getWithCacheFallback
import com.example.chaskirider.core.text.TextKey
import com.example.chaskirider.core.text.TextProvider
import com.example.chaskirider.feature.vehicle.domain.VehicleType
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await

class VehicleRemote(
    private val texts: TextProvider,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val functions: FirebaseFunctions = FirebaseFunctions.getInstance("us-central1")
) {
    suspend fun load(): VehicleType {
        val uid = requireUid()
        val value = db.collection("riders").document(uid).getWithCacheFallback().getString("vehicleType")
        return VehicleType.entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: VehicleType.NONE
    }

    suspend fun save(vehicle: VehicleType): VehicleType {
        requireUid()
        functions.getHttpsCallable("riderRegistration")
            .call(mapOf("action" to "vehicle", "vehicleType" to vehicle.name)).await()
        return load()
    }

    private fun requireUid(): String =
        auth.currentUser?.uid ?: error(texts.get(TextKey.TEXT_NO_HAY_UNA_SESION_ACTIVA))
}
