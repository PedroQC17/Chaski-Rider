package com.example.chaskirider.feature.user.data.remote

import com.example.chaskirider.core.text.TextKey
import com.example.chaskirider.core.text.TextProvider
import com.example.chaskirider.feature.user.data.local.RiderUserSessionStore
import com.example.chaskirider.feature.user.domain.RegistrationStatus
import com.example.chaskirider.feature.user.domain.RiderUser
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await

@IgnoreExtraProperties
private data class RiderUserDto(
    val name: String = "",
    val lastName: String = "",
    val dni: String = "",
    val phone: String = "",
    val profilePhotoPath: String = "",
    val vehicleType: String = "",
    val status: String = "",
    val rejectionReason: String = "",
    val submissionVersion: Long = 0,
    val currentStep: Int = 1,
    val termsAccepted: Boolean = false,
    @get:PropertyName("isAvailable") val isAvailable: Boolean = false,
    @get:PropertyName("isEnabled") val isEnabled: Boolean = true
) {
    fun toDomain(user: FirebaseUser): RiderUser = RiderUser(
        id = user.uid,
        name = name,
        lastName = lastName,
        dni = dni,
        phone = phone,
        email = user.email.orEmpty(),
        profilePhotoPath = profilePhotoPath,
        hasPassword = user.providerData.any { it.providerId == EmailAuthProvider.PROVIDER_ID },
        vehicleTypeId = vehicleType.ifBlank { null },
        status = RegistrationStatus.entries.firstOrNull { it.name.equals(status, ignoreCase = true) }
            ?: RegistrationStatus.INCOMPLETE,
        rejectionReason = rejectionReason,
        submissionVersion = submissionVersion,
        currentStep = currentStep,
        termsAccepted = termsAccepted,
        isAvailable = isAvailable,
        isEnabled = isEnabled
    )
}

class RiderUserRemote(
    private val texts: TextProvider,
    private val session: RiderUserSessionStore,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val functions: FirebaseFunctions = FirebaseFunctions.getInstance("us-central1"),
    private val online: () -> Boolean = { true }
) {
    suspend fun load(): RiderUser {
        val user = auth.currentUser ?: error(texts.get(TextKey.TEXT_NO_HAY_UNA_SESION_ACTIVA))

        val reference = db.collection("riders").document(user.uid)
        val snapshot = if (!online()) reference.get(Source.CACHE).await() else try {
            kotlinx.coroutines.withTimeout(8_000) { reference.get(Source.SERVER).await() }
        } catch (_: kotlinx.coroutines.TimeoutCancellationException) {
            reference.get(Source.CACHE).await()
        } catch (error: com.google.firebase.firestore.FirebaseFirestoreException) {
            if (error.code !in listOf(com.google.firebase.firestore.FirebaseFirestoreException.Code.UNAVAILABLE,
                    com.google.firebase.firestore.FirebaseFirestoreException.Code.DEADLINE_EXCEEDED)) throw error
            reference.get(Source.CACHE).await()
        }
        check(!snapshot.metadata.isFromCache || snapshot.exists()) { "Conéctate a internet para cargar tu perfil por primera vez." }
        val rider = snapshot.toObject(RiderUserDto::class.java)?.toDomain(user)
            ?: RiderUser(id = user.uid, email = user.email.orEmpty(),
                name = user.displayName?.substringBefore(" ").orEmpty(),
                lastName = user.displayName?.substringAfter(" ", "").orEmpty(),
                hasPassword = user.providerData.any { it.providerId == EmailAuthProvider.PROVIDER_ID })
        check(auth.currentUser?.uid == user.uid) { texts.get(TextKey.TEXT_LA_SESION_CAMBIO_VUELVE_A_INGRESAR) }
        session.update(rider)
        return rider
    }

    suspend fun mutate(action: String, payload: Map<String, Any> = emptyMap(), expectedUid: String? = null): RiderUser {
        val uid = auth.currentUser?.uid ?: error(texts.get(TextKey.TEXT_NO_HAY_UNA_SESION_ACTIVA))
        check(expectedUid == null || expectedUid == uid) { texts.get(TextKey.TEXT_LA_SESION_CAMBIO_VUELVE_A_INGRESAR) }
        functions.getHttpsCallable("riderRegistration").call(payload + ("action" to action)).await()
        check(auth.currentUser?.uid == uid) { texts.get(TextKey.TEXT_LA_SESION_CAMBIO_VUELVE_A_INGRESAR) }
        return load()
    }
}
