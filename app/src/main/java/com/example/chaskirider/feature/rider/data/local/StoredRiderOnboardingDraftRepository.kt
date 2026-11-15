package com.example.chaskirider.feature.rider.data.local

import android.content.Context
import com.example.chaskirider.feature.rider.domain.PendingDocumentCapture
import com.example.chaskirider.feature.rider.domain.RiderOnboardingDraft
import com.example.chaskirider.feature.rider.domain.RiderOnboardingDraftRepository
import com.google.gson.Gson

/** Borrador del onboarding fuera de Room: persiste entre procesos sin ser una entidad. */
class StoredRiderOnboardingDraftRepository(context: Context) : RiderOnboardingDraftRepository {
    private val preferences = context.applicationContext
        .getSharedPreferences("rider_onboarding", Context.MODE_PRIVATE)
    private val gson = Gson()

    override suspend fun load(uid: String): RiderOnboardingDraft? =
        preferences.getString(draftKey(uid), null)?.let {
            runCatching { gson.fromJson(it, RiderOnboardingDraft::class.java) }.getOrNull()
        }

    override suspend fun save(draft: RiderOnboardingDraft) {
        preferences.edit().putString(draftKey(draft.uid), gson.toJson(draft)).commit()
    }

    override suspend fun clear(uid: String) {
        preferences.edit().remove(draftKey(uid)).commit()
    }

    override suspend fun loadCapture(uid: String): PendingDocumentCapture? =
        preferences.getString(captureKey(uid), null)?.let {
            runCatching { gson.fromJson(it, PendingDocumentCapture::class.java) }.getOrNull()
        }

    override suspend fun saveCapture(capture: PendingDocumentCapture) {
        preferences.edit().putString(captureKey(capture.uid), gson.toJson(capture)).commit()
    }

    override suspend fun clearCapture(uid: String) {
        preferences.edit().remove(captureKey(uid)).commit()
    }

    private fun draftKey(uid: String) = "draft_$uid"
    private fun captureKey(uid: String) = "capture_$uid"
}
