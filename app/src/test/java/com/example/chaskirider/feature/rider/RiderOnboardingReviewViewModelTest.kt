package com.example.chaskirider.feature.rider
import androidx.lifecycle.ViewModelStore
import com.example.chaskirider.feature.documents.domain.DocumentRepository
import com.example.chaskirider.feature.documents.domain.DocumentSnapshot
import com.example.chaskirider.feature.rider.domain.Rider
import com.example.chaskirider.feature.rider.domain.RiderStatus
import com.example.chaskirider.feature.rider.domain.RiderSession
import com.example.chaskirider.feature.rider.domain.RiderRepository
import com.example.chaskirider.core.text.*
import com.example.chaskirider.feature.rider.ui.viewmodel.RiderOnboardingReviewViewModel
import com.example.chaskirider.feature.rider.ui.viewmodel.RiderOnboardingReviewStage
import com.example.chaskirider.feature.rider.ui.viewmodel.RiderOnboardingReviewUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class RiderOnboardingReviewViewModelTest {
    private val store = ViewModelStore()
    private val session = object : RiderSession {
        override val user = MutableStateFlow<Rider?>(Rider(id = "ana", status = RiderStatus.PENDING_REVIEW))
    }
    private var result: Result<Rider?> = Result.success(session.user.value)
    private var calls = 0
    private val repository = object : FakeRiderRepository() {
        override suspend fun getCurrentUser(): Result<Rider?> {
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
    private fun model() = RiderOnboardingReviewViewModel(repository, documents, session, texts)
        .also { store.put("review", it) }

    @Test fun failedRefreshRetainsDecisionAndRetryLoadsCorrection() = runTest {
        val vm = model(); advanceUntilIdle()
        result = Result.failure(IllegalStateException("offline"))
        vm.refresh(); advanceUntilIdle()
        assertEquals(RiderOnboardingReviewStage.PENDING, vm.uiState.value.stage)
        assertEquals("offline", vm.uiState.value.errorMessage)
        assertFalse(vm.uiState.value.canEdit)
        result = Result.success(session.user.value!!.copy(status = RiderStatus.NEEDS_CORRECTION,
            rejectionReason = "Corrige el DNI"))
        vm.refresh(); advanceUntilIdle()
        assertEquals(RiderOnboardingReviewStage.CORRECTION, vm.uiState.value.stage)
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
        session.user.value = Rider(id = "bob", status = RiderStatus.REJECTED)
        advanceUntilIdle()
        assertEquals("bob", vm.uiState.value.user!!.id)
        assertEquals(RiderOnboardingReviewStage.REJECTED, vm.uiState.value.stage)
        assertNull(vm.uiState.value.errorMessage); assertFalse(vm.uiState.value.isRefreshing)
    }
    @Test fun onlyEditableStatesCanResumeAndOnlyApprovedCanWork() {
        for (status in RiderStatus.entries) {
            val state = RiderOnboardingReviewUiState(Rider(status = status))
            assertEquals(status in listOf(RiderStatus.INCOMPLETE, RiderStatus.NEEDS_CORRECTION), state.canEdit)
            assertEquals(status == RiderStatus.APPROVED, state.canWork)
        }
        assertFalse(RiderOnboardingReviewUiState().canEdit)
    }
    @Test fun disabledAccountOverridesPendingAndApprovedRiderOnboarding() {
        for (status in RiderStatus.entries) {
            val state = RiderOnboardingReviewUiState(Rider(status = status, isEnabled = false))
            assertEquals(RiderOnboardingReviewStage.DISABLED, state.stage)
            assertFalse(state.canEdit); assertFalse(state.canWork)
        }
    }
}
