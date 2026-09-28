// HU04 - Parte 1: se implementa setAvailability() con la acción "availability"
// de la Cloud Function riderRegistration (nombre de acción propuesto; si el
// backend usa otro, se ajusta aquí).
package com.example.chaskirider.data.repository

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.chaskirider.domain.model.*
import com.example.chaskirider.domain.repository.AuthRepository
import com.google.firebase.auth.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Source
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await
import java.io.File
import java.util.UUID

class AuthRepositoryImpl(
    private val context: Context,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val storage: FirebaseStorage = FirebaseStorage.getInstance(),
    private val functions: FirebaseFunctions = FirebaseFunctions.getInstance("us-central1")
) : AuthRepository {
    private suspend fun <T> safe(block: suspend () -> T): Result<T> = try {
        Result.success(block())
    } catch (e: CancellationException) { throw e
    } catch (e: Exception) {
        val message = when (e) {
            is FirebaseFirestoreException -> when (e.code) {
                FirebaseFirestoreException.Code.UNAVAILABLE ->
                    "No se pudo consultar tu perfil en Firestore. Comprueba la conexión y que Cloud Firestore esté habilitado en Firebase."
                FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                    "Firebase no permite consultar tu perfil. Revisa que la API de Firestore esté habilitada y las reglas publicadas."
                else -> "No se pudo cargar tu perfil de Firestore. Reintenta."
            }
            is FirebaseAuthInvalidCredentialsException, is FirebaseAuthInvalidUserException -> "Correo o contraseña incorrectos"
            is FirebaseAuthUserCollisionException -> "La credencial ya pertenece a otra cuenta. Ingresa con su método original."
            is FirebaseAuthRecentLoginRequiredException -> "Por seguridad, cierra sesión y vuelve a ingresar antes de configurar la contraseña."
            is FirebaseAuthWeakPasswordException -> "La contraseña no cumple la política de seguridad."
            is FirebaseFunctionsException -> when (e.code) {
                FirebaseFunctionsException.Code.NOT_FOUND, FirebaseFunctionsException.Code.UNIMPLEMENTED ->
                    "Falta desplegar el servicio de registro en Firebase. Tu sesión sigue activa."
                FirebaseFunctionsException.Code.UNAVAILABLE -> "No se pudo conectar con el servidor. Reintenta."
                else -> e.message ?: "No se pudo guardar la información"
            }
            else -> e.message ?: "No se pudo completar la operación. Reintenta."
        }
        Result.failure(IllegalStateException(message, e))
    }

    private suspend fun profile(): RiderUser {
        val user = auth.currentUser ?: error("No hay una sesión activa")
        // A failed server read must not turn an approved/existing user into a new profile.
        val snapshot = db.collection("riders").document(user.uid).get(Source.SERVER).await()
        return snapshot.toObject(RiderUser::class.java)?.copy(id = user.uid, email = user.email.orEmpty())
            ?: RiderUser(id = user.uid, email = user.email.orEmpty(),
                name = user.displayName?.substringBefore(" ").orEmpty(),
                lastName = user.displayName?.substringAfter(" ", "").orEmpty())
    }

    override suspend fun loginWithGoogle(idToken: String) = safe {
        auth.signInWithCredential(GoogleAuthProvider.getCredential(idToken, null)).await()
        profile()
    }
    override suspend fun loginWithEmail(email: String, pass: String) = safe {
        auth.signInWithEmailAndPassword(email.trim(), pass).await()
        profile()
    }
    override suspend fun getCurrentUser(): Result<RiderUser?> = safe {
        if (auth.currentUser == null) null else profile()
    }
    override suspend fun sendPasswordResetEmail(email: String) = safe {
        auth.sendPasswordResetEmail(email.trim()).await()
        Unit
    }
    override suspend fun linkPassword(password: String) = safe {
        val user = auth.currentUser ?: error("No hay una sesión activa")
        require(user.providerData.none { it.providerId == EmailAuthProvider.PROVIDER_ID }) {
            "Ya tienes una contraseña configurada. Usa la recuperación para cambiarla."
        }
        user.linkWithCredential(EmailAuthProvider.getCredential(
            user.email ?: error("La cuenta no tiene correo"), password)).await()
        Unit
    }
    private suspend fun mutate(action: String, payload: Map<String, Any> = emptyMap()): RiderUser {
        functions.getHttpsCallable("riderRegistration").call(payload + ("action" to action)).await()
        return profile()
    }
    override suspend fun savePersonalData(name: String, lastName: String, dni: String, phone: String, terms: Boolean) = safe {
        mutate("personal", mapOf("name" to name.trim(), "lastName" to lastName.trim(), "dni" to dni.trim(),
            "phone" to RegistrationValidation.normalizePhone(phone), "termsAccepted" to terms))
    }
    override suspend fun saveVehicle(vehicle: VehicleType) = safe { mutate("vehicle", mapOf("vehicleType" to vehicle.name)) }
    override suspend fun saveBankInfo(bank: BankInfo) = safe {
        mutate("bank", mapOf("bankInfo" to mapOf("bankName" to bank.bankName.trim(),
            "holderName" to bank.holderName.trim(), "accountNumber" to bank.accountNumber.trim(), "cci" to bank.cci.trim())))
    }
    override suspend fun submitRegistration() = safe { mutate("submit") }

    override suspend fun uploadDocument(docType: String, uri: Uri) = safe {
        val uid = auth.currentUser?.uid ?: error("No hay una sesión activa")
        require(docType in listOf("dniFront", "dniBack", "bankStatement", "driverLicense", "soat"))
        val mime = context.contentResolver.getType(uri)
        require(mime in RegistrationValidation.mimeTypes) { "Selecciona un PDF, JPG o PNG" }
        val temp = withContext(Dispatchers.IO) {
            val file = File.createTempFile("rider-upload-", ".tmp", context.cacheDir)
            try {
                context.contentResolver.openInputStream(uri).use { input ->
                    requireNotNull(input) { "No se pudo abrir el archivo" }
                    file.outputStream().use { output ->
                        val buffer = ByteArray(8192)
                        var total = 0L
                        while (true) {
                            val count = input.read(buffer)
                            if (count < 0) break
                            total += count
                            require(total <= RegistrationValidation.MAX_FILE_BYTES) { "El archivo supera los 10 MB" }
                            output.write(buffer, 0, count)
                        }
                        require(total > 0) { "El archivo está vacío" }
                    }
                }
                file
            } catch (e: Exception) { file.delete(); throw e }
        }
        try {
            // Immutable object names prevent a replacement from changing a submitted document.
            val path = "riders/$uid/documents/$docType/${UUID.randomUUID()}"
            val metadata = StorageMetadata.Builder().setContentType(mime).build()
            storage.reference.child(path).putFile(Uri.fromFile(temp), metadata).await()
            mutate("document", mapOf("docType" to docType, "path" to path))
        } finally { temp.delete() }
    }

    override suspend fun getDocument(docType: String) = safe {
        val user = profile()
        val path = RegistrationValidation.documentPaths(user)[docType].orEmpty()
        require(path.startsWith("riders/${user.id}/documents/$docType/")) { "Vuelve a subir este documento para consultarlo de forma privada" }
        val ref = storage.reference.child(path)
        val metadata = ref.metadata.await()
        val extension = when (metadata.contentType) { "application/pdf" -> ".pdf"; "image/png" -> ".png"; else -> ".jpg" }
        val directory = File(context.cacheDir, "documents").apply { mkdirs() }
        val file = File.createTempFile("document-", extension, directory)
        ref.getFile(file).await()
        FileProvider.getUriForFile(context, "${context.packageName}.files", file)
    }
    override suspend fun logout() { auth.signOut() }
    override suspend fun setAvailability(available: Boolean) = safe {
        mutate("availability", mapOf("isAvailable" to available))
    }
}
