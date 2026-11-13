package com.example.chaskirider.feature.orders.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import com.example.chaskirider.core.platform.FileProviderUris
import java.io.File

/** Persistencia del borrador de entrega (foto, nota y su pedido) entre procesos. */
internal class DeliveryDraft(private val handle: SavedStateHandle) {
    fun pendingUri(): Uri? = savedUri(KEY_PENDING)
    fun evidenceUri(): Uri? = savedUri(KEY_EVIDENCE)
    fun note(): String? = handle.get(KEY_NOTE)
    fun boundOrderId(): String? = handle.get(KEY_ORDER)

    fun ensureOwner(owner: String?) {
        if (owner == null || handle.get<String>(KEY_UID) != owner) clear()
    }

    fun clear() {
        listOf(KEY_PENDING, KEY_EVIDENCE, KEY_NOTE, KEY_UID, KEY_ORDER)
            .forEach { handle.remove<String>(it) }
    }

    fun newCapture(context: Context): Uri {
        val dir = File(context.cacheDir, "captures").apply { mkdirs() }
        val file = File(dir, "evidence_${System.currentTimeMillis()}.jpg")
        val uri = FileProviderUris.uriFor(context, file)
        handle[KEY_PENDING] = uri.toString()
        return uri
    }

    fun confirmPending(fallback: Uri?): Uri? {
        val pending = fallback ?: savedUri(KEY_PENDING)
        return if (pending != null) {
            handle[KEY_EVIDENCE] = pending.toString()
            handle.remove<String>(KEY_PENDING)
            pending
        } else {
            handle.remove<String>(KEY_PENDING)
            null
        }
    }

    fun discardPending() = handle.remove<String>(KEY_PENDING)

    fun saveEvidence(uri: Uri) {
        handle[KEY_EVIDENCE] = uri.toString()
        handle.remove<String>(KEY_PENDING)
    }

    fun clearEvidence() {
        handle.remove<String>(KEY_PENDING)
        handle.remove<String>(KEY_EVIDENCE)
    }

    fun saveNote(owner: String, orderId: String, note: String) {
        handle[KEY_UID] = owner
        handle[KEY_ORDER] = orderId
        handle[KEY_NOTE] = note
    }

    fun bind(owner: String?, orderId: String) {
        handle[KEY_UID] = owner
        handle[KEY_ORDER] = orderId
    }

    private fun savedUri(key: String) = handle.get<String>(key)?.let(Uri::parse)

    private companion object {
        const val KEY_PENDING = "pending_capture"
        const val KEY_EVIDENCE = "evidence_photo"
        const val KEY_NOTE = "delivery_note"
        const val KEY_UID = "delivery_draft_uid"
        const val KEY_ORDER = "delivery_draft_order"
    }
}
