package com.example.chaskirider.feature.documents.data.remote

import android.net.Uri
import com.example.chaskirider.core.firebase.getWithCacheFallback
import com.example.chaskirider.core.text.TextKey
import com.example.chaskirider.core.text.TextProvider
import com.example.chaskirider.feature.documents.domain.DocumentPolicy
import com.example.chaskirider.feature.documents.domain.DocumentReview
import com.example.chaskirider.feature.documents.domain.DocumentReviewStatus
import com.example.chaskirider.feature.documents.domain.DocumentSnapshot
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import kotlinx.coroutines.tasks.await

class DocumentRemote(
    private val texts: TextProvider,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val storage: FirebaseStorage = FirebaseStorage.getInstance(),
    private val functions: FirebaseFunctions = FirebaseFunctions.getInstance("us-central1"),
    private val online: () -> Boolean = { true }
) {
    suspend fun upload(uri: Uri, docType: String, mimeType: String, path: String) {
        val uid = requireUid()
        val metadata = StorageMetadata.Builder().setContentType(mimeType).build()
        storage.reference.child(path).putFile(uri, metadata).await()
        functions.getHttpsCallable("riderRegistration")
            .call(mapOf("action" to "document", "docType" to docType, "path" to path)).await()
        check(auth.currentUser?.uid == uid) { texts.get(TextKey.TEXT_LA_SESION_CAMBIO_VUELVE_A_INGRESAR) }
    }

    suspend fun path(docType: String): String {
        val uid = requireUid()
        return db.collection("riders").document(uid)
            .getWithCacheFallback(online())
            .getString(DocumentPolicy.field(docType)).orEmpty()
    }

    suspend fun load(): DocumentSnapshot {
        val uid = requireUid()
        val document = db.collection("riders").document(uid).getWithCacheFallback(online())
        val paths = DocumentPolicy.types.associateWith { document.getString(DocumentPolicy.field(it)).orEmpty() }
        val reviews = (document.get("documentReviews") as? Map<*, *>).orEmpty()
            .mapNotNull { (key, value) ->
                val type = key as? String ?: return@mapNotNull null
                val raw = value as? Map<*, *> ?: return@mapNotNull null
                val status = (raw["status"] as? String)
                    ?.let { name -> DocumentReviewStatus.entries.firstOrNull { it.name.equals(name, ignoreCase = true) } }
                    ?: return@mapNotNull null
                type to DocumentReview(status, raw["reason"] as? String ?: "")
            }.toMap()
        return DocumentSnapshot(paths, reviews)
    }

    private fun requireUid(): String =
        auth.currentUser?.uid ?: error(texts.get(TextKey.TEXT_NO_HAY_UNA_SESION_ACTIVA))
}
