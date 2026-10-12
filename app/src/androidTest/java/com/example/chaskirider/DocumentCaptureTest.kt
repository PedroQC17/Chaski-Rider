package com.example.chaskirider

import android.net.Uri
import androidx.lifecycle.ViewModelStore
import com.example.chaskirider.domain.model.*
import com.example.chaskirider.domain.repository.*
import com.example.chaskirider.domain.text.*
import com.example.chaskirider.ui.screens.profile.documents.DocumentsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class DocumentCaptureTest {
    private val store = ViewModelStore()
    private val uri = Uri.parse("content://example/registration_documents/capture.jpg")
    private val session = object : RiderSession {
        override val user = MutableStateFlow<RiderUser?>(RiderUser(id = "ana"))
    }
    private val drafts = object : RegistrationDraftRepository {
        private val captures = mutableMapOf<String, PendingDocumentCapture>()
        override suspend fun load(uid: String): RegistrationDraft? = null
        override suspend fun save(draft: RegistrationDraft) = Unit
        override suspend fun clear(uid: String) = Unit
        override suspend fun loadCapture(uid: String) = captures[uid]
        override suspend fun saveCapture(capture: PendingDocumentCapture) { captures[capture.uid] = capture }
        override suspend fun clearCapture(uid: String) { captures.remove(uid) }
    }
    private var uploadFails = true
    private var deleted = false
    private val repository = object : DocumentRepository {
        override suspend fun createCapture() = Result.success(uri)
        override suspend fun hasCapture(uri: Uri) = Result.success(!deleted)
        override suspend fun deleteCapture(uri: Uri): Result<Unit> { deleted = true; return Result.success(Unit) }
        override suspend fun uploadDocument(docType: String, uri: Uri): Result<RiderUser> =
            if (uploadFails) Result.failure(IllegalStateException("offline"))
            else Result.success(session.user.value!!.copy(dniFrontUrl = "uploaded"))
        override suspend fun getDocument(docType: String) = Result.success(uri)
    }
    private val texts = object : TextProvider {
        override fun get(key: TextKey) = key.name
        override fun resolveError(message: String?, fallback: TextKey) = message ?: fallback.name
    }
    private val previews = object : DocumentPreviewRepository {
        override suspend fun render(uri: Uri, page: Int): Result<DocumentPage> = error("unused")
    }
    @Before fun setup() { Dispatchers.setMain(StandardTestDispatcher()) }
    @After fun teardown() { store.clear(); Dispatchers.resetMain() }
    private fun model() = DocumentsViewModel(texts, repository, session, previews, drafts).also { store.put("documents", it) }

    @Test fun recreatedViewModelRecoversCaptureAndFailedUploadKeepsItForRetry() = runTest {
        val vm = model(); advanceUntilIdle()
        var cameraOpened = false
        vm.prepareCapture("dniFront") { cameraOpened = true }; advanceUntilIdle()
        assertTrue(cameraOpened)
        assertNotNull(drafts.loadCapture("ana"))
        store.clear()
        val restored = model(); advanceUntilIdle()
        assertEquals(uri, restored.uiState.value.captureUri)
        restored.confirmCapture(); advanceUntilIdle()
        assertEquals("offline", restored.uiState.value.errorMessage)
        assertNotNull(drafts.loadCapture("ana")); assertFalse(deleted)
        uploadFails = false
        restored.confirmCapture(); advanceUntilIdle()
        assertNull(drafts.loadCapture("ana")); assertTrue(deleted)
        assertNull(restored.uiState.value.captureUri)
        assertEquals(DocumentUploadState.UPLOADED, restored.uiState.value.documentsMap["dniFront"]!!.uploadState)
    }

    @Test fun accountSwitchHidesCaptureAndReturningOwnerCanDiscardIt() = runTest {
        drafts.saveCapture(PendingDocumentCapture("ana", "dniFront", uri.toString()))
        val vm = model(); advanceUntilIdle()
        assertEquals(uri, vm.uiState.value.captureUri)
        session.user.value = RiderUser(id = "bob"); advanceUntilIdle()
        assertNull(vm.uiState.value.captureUri)
        assertNotNull(drafts.loadCapture("ana"))
        session.user.value = RiderUser(id = "ana"); advanceUntilIdle()
        vm.discardCapture(); advanceUntilIdle()
        assertNull(drafts.loadCapture("ana")); assertTrue(deleted)
    }
}
