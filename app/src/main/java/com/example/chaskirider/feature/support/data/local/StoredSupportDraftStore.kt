package com.example.chaskirider.feature.support.data.local

import android.content.Context
import com.example.chaskirider.feature.support.domain.SupportDraft
import com.example.chaskirider.feature.support.domain.SupportDraftStore
import com.google.gson.Gson

/** Borrador de soporte fuera de Room: persiste entre procesos sin ser una entidad. */
class StoredSupportDraftStore(context: Context) : SupportDraftStore {
    private val preferences = context.applicationContext
        .getSharedPreferences("support_drafts", Context.MODE_PRIVATE)
    private val gson = Gson()

    override suspend fun load(uid: String): SupportDraft? =
        preferences.getString(key(uid), null)?.let {
            runCatching { gson.fromJson(it, SupportDraft::class.java) }.getOrNull()
        }

    override suspend fun save(uid: String, draft: SupportDraft) {
        preferences.edit().putString(key(uid), gson.toJson(draft)).commit()
    }

    override suspend fun clear(uid: String, draftId: String) {
        val saved = load(uid) ?: return
        if (saved.id == draftId) preferences.edit().remove(key(uid)).commit()
    }

    private fun key(uid: String) = "draft_$uid"
}
