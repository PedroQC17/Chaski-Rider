package com.example.chaskirider.data.documents

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.chaskirider.domain.model.*
import com.google.firebase.auth.*
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await
import java.io.File
import java.util.UUID

import com.example.chaskirider.data.remote.RiderProfileRemoteDataSource
import com.example.chaskirider.data.remote.firebaseResult

class RiderDocumentDataSource(
    private val context: Context,
    private val profiles: RiderProfileRemoteDataSource,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()
) {
    suspend fun createCaptureUri(): Result<Uri> = firebaseResult {
        withContext(Dispatchers.IO) {
            val directory = File(context.cacheDir, "captures").apply { mkdirs() }
            val file = File.createTempFile("capture-", ".jpg", directory)
            FileProvider.getUriForFile(context, "${context.packageName}.files", file)
        }
    }
    suspend fun uploadDocument(docType: String, uri: Uri) = firebaseResult {
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
            
            val path = "riders/$uid/documents/$docType/${UUID.randomUUID()}"
            val metadata = StorageMetadata.Builder().setContentType(mime).build()
            storage.reference.child(path).putFile(Uri.fromFile(temp), metadata).await()
            profiles.mutate("document", mapOf("docType" to docType, "path" to path))
        } finally { temp.delete() }
    }

    suspend fun getDocument(docType: String) = firebaseResult {
        val user = profiles.load()
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
}
