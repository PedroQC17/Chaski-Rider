package com.example.chaskirider.feature.documents.data.local

import com.example.chaskirider.feature.documents.domain.DocumentPolicy

import com.example.chaskirider.core.text.TextProvider
import com.example.chaskirider.core.text.TextKey
import android.content.Context
import android.net.Uri
import com.example.chaskirider.core.platform.FileProviderUris
import com.example.chaskirider.feature.user.domain.RiderUserService
import com.example.chaskirider.feature.documents.data.remote.DocumentRemote
import com.google.firebase.auth.*
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await
import java.io.File
import java.util.UUID
import com.example.chaskirider.core.firebase.firebaseResult

class RiderDocumentDataSource(
    private val texts: TextProvider,
    private val context: Context,
    private val remote: DocumentRemote,
    private val refreshUser: suspend () -> Unit,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()
) {
    suspend fun load() = firebaseResult(texts) { remote.load() }

    // Private durable file: cache eviction must not discard an unconfirmed registration capture.
    suspend fun createCapture() = firebaseResult(texts) {
        val uid = auth.currentUser?.uid ?: error(texts.get(TextKey.TEXT_NO_HAY_UNA_SESION_ACTIVA))
        withContext(Dispatchers.IO) {
            val dir = File(context.filesDir, "registration_documents/$uid").apply { mkdirs() }
            FileProviderUris.uriFor(context, File.createTempFile("capture-", ".jpg", dir))
        }
    }

    suspend fun hasCapture(uri: Uri) = firebaseResult(texts) {
        requireCapture(uri)
        withContext(Dispatchers.IO) {
            try { context.contentResolver.openInputStream(uri)?.use { it.read() != -1 } ?: false }
            catch (_: java.io.FileNotFoundException) { false }
        }
    }

    suspend fun deleteCapture(uri: Uri) = firebaseResult(texts) {
        requireCapture(uri)
        withContext(Dispatchers.IO) { context.contentResolver.delete(uri, null, null) }
        Unit
    }

    private fun requireCapture(uri: Uri) {
        require(FileProviderUris.belongsTo(context, uri) &&
            uri.pathSegments.firstOrNull() == "registration_documents")
    }

    suspend fun uploadDocument(docType: String, uri: Uri) = firebaseResult(texts) {
        val uid = auth.currentUser?.uid ?: error(texts.get(TextKey.TEXT_NO_HAY_UNA_SESION_ACTIVA))
        require(docType in listOf("dniFront", "dniBack", "bankStatement", "driverLicense", "soat"))
        val mime = context.contentResolver.getType(uri)
        require(mime in DocumentPolicy.mimeTypes) { texts.get(TextKey.TEXT_SELECCIONA_UN_PDF_JPG_O_PNG) }
        val temp = withContext(Dispatchers.IO) {
            val file = File.createTempFile("rider-upload-", ".tmp", context.cacheDir)
            try {
                context.contentResolver.openInputStream(uri).use { input ->
                    requireNotNull(input) { texts.get(TextKey.TEXT_NO_SE_PUDO_ABRIR_EL_ARCHIVO) }
                    file.outputStream().use { output ->
                        val buffer = ByteArray(8192)
                        var total = 0L
                        while (true) {
                            val count = input.read(buffer)
                            if (count < 0) break
                            total += count
                            require(total <= DocumentPolicy.MAX_FILE_BYTES) { texts.get(TextKey.TEXT_EL_ARCHIVO_SUPERA_LOS_10_MB) }
                            output.write(buffer, 0, count)
                        }
                        require(total > 0) { texts.get(TextKey.TEXT_EL_ARCHIVO_ESTA_VACIO) }
                    }
                }
                file
            } catch (e: Exception) { file.delete(); throw e }
        }
        try {

            val path = "riders/$uid/documents/$docType/${UUID.randomUUID()}"
            remote.upload(Uri.fromFile(temp), docType, requireNotNull(mime), path)
            refreshUser()
        } finally { temp.delete() }
    }

    suspend fun getDocument(docType: String) = firebaseResult(texts) {
        val uid = auth.currentUser?.uid ?: error(texts.get(TextKey.TEXT_NO_HAY_UNA_SESION_ACTIVA))
        val path = remote.path(docType)
        require(path.startsWith("riders/$uid/documents/$docType/")) { texts.get(TextKey.TEXT_VUELVE_A_SUBIR_ESTE_DOCUMENTO_PARA_CONSULTARLO) }
        val ref = storage.reference.child(path)
        val metadata = ref.metadata.await()
        val extension = when (metadata.contentType) { "application/pdf" -> ".pdf"; "image/png" -> ".png"; else -> ".jpg" }
        val directory = File(context.cacheDir, "documents").apply { mkdirs() }
        val file = File.createTempFile("document-", extension, directory)
        ref.getFile(file).await()
        check(auth.currentUser?.uid == uid) { texts.get(TextKey.TEXT_LA_SESION_CAMBIO_VUELVE_A_INGRESAR) }
        FileProviderUris.uriFor(context, file)
    }
}
