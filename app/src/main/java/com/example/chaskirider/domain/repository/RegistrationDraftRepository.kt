package com.example.chaskirider.domain.repository

import com.example.chaskirider.domain.model.PendingDocumentCapture
import com.example.chaskirider.domain.model.RegistrationDraft

interface RegistrationDraftRepository {
    suspend fun load(uid: String): RegistrationDraft?
    suspend fun save(draft: RegistrationDraft)
    suspend fun clear(uid: String)
    suspend fun loadCapture(uid: String): PendingDocumentCapture?
    suspend fun saveCapture(capture: PendingDocumentCapture)
    suspend fun clearCapture(uid: String)
}
