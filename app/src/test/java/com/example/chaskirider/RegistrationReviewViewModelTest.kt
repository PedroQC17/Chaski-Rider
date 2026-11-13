package com.example.chaskirider

import androidx.lifecycle.ViewModelStore
import com.example.chaskirider.feature.documents.domain.DocumentRepository
import com.example.chaskirider.feature.documents.domain.DocumentSnapshot
import com.example.chaskirider.feature.user.domain.RiderUser
import com.example.chaskirider.feature.user.domain.RegistrationStatus
import com.example.chaskirider.feature.user.domain.RiderSession
import com.example.chaskirider.feature.user.domain.RiderUserRepository
import com.example.chaskirider.core.text.*
import com.example.chaskirider.feature.user.ui.viewmodel.RegistrationReviewViewModel
import com.example.chaskirider.feature.user.ui.viewmodel.RegistrationReviewStage
import com.example.chaskirider.feature.user.ui.viewmodel.RegistrationReviewUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class RegistrationReviewViewModelTest {
    private val store = ViewModelStore()
    private val session = object : RiderSession {
        override val user = MutableStateFlow<RiderUser?>(RiderUser(id = "ana", status = RegistrationStatus.PENDING_REVIEW))
    }
    private var result: Result<RiderUser?> = Result.success(session.user.value)
    private var calls = 0
    private val repository = object : FakeRiderUserRepository() {
        override suspend fun getCurrentUser(): Result<RiderUser?> {
            calls++; delay(100)
            result.getOrNull()?.let { session.user.value = it }
            return result
        }
    }
    private val texts = object : TextProvider {
        override fun get(key: TextKey) = key.name
        override fun resolveError(message: String?, fallback: TextKey) = message ?: fallback.name
    }
    @Before fun setup() { Dispatchers.setMain(StandardTestDispatcher()) }
    @After fun teardown() { store.clear(); Dispatchers.resetMain() }
    private val documents = object : DocumentRepository {
        override suspend fun load() = Result.success(DocumentSnapshot())
        override suspend fun createCapture() = error("unused")
        override suspend fun hasCapture(uri: android.net.Uri) = error("unused")
        override suspend fun deleteCapture(uri: android.net.Uri) = error("unused")
        override suspend fun uploadDocument(docType: String, uri: android.net.Uri) = error("unused")
        override suspend fun getDocument(docType: String) = error("unused")
    }
    private fun model() = RegistrationReviewViewModel(repository, documents, session, texts)
        .also { store.put("review", it) }

    @Test fun failedRefreshRetainsDecisionAndRetryLoadsCorrection() = runTest {
        val vm = model(); advanceUntilIdle()
        result = Result.failure(IllegalStateException("offline"))
        vm.refresh(); advanceUntilIdle()
        assertEquals(RegistrationReviewStage.PENDING, vm.uiState.value.stage)
        assertEquals("offline", vm.uiState.value.errorMessage)
        assertFalse(vm.uiState.value.canEdit)
        result = Result.success(session.user.value!!.copy(status = RegistrationStatus.NEEDS_CORRECTION,
            rejectionReason = "Corrige el DNI"))
        vm.refresh(); advanceUntilIdle()
        assertEquals(RegistrationReviewStage.CORRECTION, vm.uiState.value.stage)
        assertEquals("Corrige el DNI", vm.uiState.value.user!!.rejectionReason)
        assertTrue(vm.uiState.value.canEdit); assertNull(vm.uiState.value.errorMessage)
    }
    @Test fun duplicateRefreshOnlyQueriesOnce() = runTest {
        val vm = model(); advanceUntilIdle()
        vm.refresh(); vm.refresh(); advanceUntilIdle()
        assertEquals(1, calls); assertFalse(vm.uiState.value.isRefreshing)
    }
    @Test fun accountSwitchCancelsOldRefreshAndRemovesItsError() = runTest {
        val vm = model(); advanceUntilIdle()
        result = Result.failure(IllegalStateException("old account error"))
        vm.refresh(); runCurrent()
        session.user.value = RiderUser(id = "bob", status = RegistrationStatus.REJECTED)
        advanceUntilIdle()
        assertEquals("bob", vm.uiState.value.user!!.id)
        assertEquals(RegistrationReviewStage.REJECTED, vm.uiState.value.stage)
        assertNull(vm.uiState.value.errorMessage); assertFalse(vm.uiState.value.isRefreshing)
    }
    @Test fun onlyEditableStatesCanResumeAndOnlyApprovedCanWork() {
        for (status in RegistrationStatus.entries) {
            val state = RegistrationReviewUiState(RiderUser(status = status))
            assertEquals(status in listOf(RegistrationStatus.INCOMPLETE, RegistrationStatus.NEEDS_CORRECTION), state.canEdit)
            assertEquals(status == RegistrationStatus.APPROVED, state.canWork)
        }
        assertFalse(RegistrationReviewUiState().canEdit)
    }
    @Test fun disabledAccountOverridesPendingAndApprovedRegistration() {
        for (status in RegistrationStatus.entries) {
            val state = RegistrationReviewUiState(RiderUser(status = status, isEnabled = false))
            assertEquals(RegistrationReviewStage.DISABLED, state.stage)
            assertFalse(state.canEdit); assertFalse(state.canWork)
        }
    }
}
