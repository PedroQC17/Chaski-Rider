package com.example.chaskirider

import android.net.Uri
import androidx.lifecycle.ViewModelStore
import com.example.chaskirider.feature.user.domain.RiderUser
import com.example.chaskirider.feature.user.domain.RegistrationStatus
import com.example.chaskirider.feature.user.domain.RegistrationDraft
import com.example.chaskirider.feature.user.domain.PendingDocumentCapture
import com.example.chaskirider.feature.bank.domain.BankInfo
import com.example.chaskirider.feature.bank.domain.BankRepository
import com.example.chaskirider.feature.vehicle.domain.VehicleRepository
import com.example.chaskirider.feature.vehicle.domain.VehicleType
import com.example.chaskirider.feature.documents.domain.DocumentRepository
import com.example.chaskirider.feature.documents.domain.DocumentSnapshot
import com.example.chaskirider.feature.documents.domain.DocumentReviewStatus
import com.example.chaskirider.feature.documents.domain.DocumentReview
import com.example.chaskirider.feature.user.domain.RiderSession
import com.example.chaskirider.feature.user.domain.RiderUserRepository
import com.example.chaskirider.feature.user.domain.RegistrationDraftRepository
import com.example.chaskirider.core.text.*
import com.example.chaskirider.feature.user.ui.viewmodel.RegistrationViewModel
import com.example.chaskirider.feature.user.ui.viewmodel.DraftStatus
import com.example.chaskirider.feature.user.ui.viewmodel.PersonalField
import com.example.chaskirider.feature.user.ui.viewmodel.BankField
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class RegistrationViewModelTest {
    private val store = ViewModelStore()
    private val session = object : RiderSession {
        override val user = MutableStateFlow<RiderUser?>(RiderUser(id = "ana", email = "ana@example.com"))
    }
    private val drafts = MemoryDrafts()
    private val repository = Repository()
    private val texts = object : TextProvider {
        override fun get(key: TextKey) = key.name
        override fun resolveError(message: String?, fallback: TextKey) = message ?: fallback.name
    }
    @Before fun setup() { Dispatchers.setMain(StandardTestDispatcher()) }
    @After fun teardown() { store.clear(); Dispatchers.resetMain() }
    private fun model() = RegistrationViewModel(repository, banks, vehicles, documents, session, texts, drafts)
        .also { store.put("registration", it) }

    @Test fun recreationRestoresAllThreeFormsButNeverPasswords() = runTest {
        val vm = model(); advanceUntilIdle()
        vm.change(PersonalField.NAME, "Ana")
        vm.change(PersonalField.PASSWORD, "not-persisted")
        vm.change(PersonalField.CONFIRMATION, "not-persisted")
        vm.acceptTerms(true)
        vm.selectVehicle(VehicleType.MOTORCYCLE)
        vm.changeBank(BankField.BANK, "BCP")
        vm.changeBank(BankField.CCI, "00219100123456780123")
        advanceUntilIdle()
        store.clear()
        val restored = model(); advanceUntilIdle()
        with(restored.uiState.value) {
            assertEquals("Ana", name); assertTrue(termsAccepted)
            assertEquals(VehicleType.MOTORCYCLE, vehicle)
            assertEquals("00219100123456780123", bank.cci)
            assertEquals("", password); assertEquals("", confirmation)
            assertEquals(DraftStatus.SAVED, draftStatus)
        }
    }

    @Test fun slowWritesKeepNewestValueAndRemainIsolatedAcrossAccounts() = runTest {
        drafts.writeDelay = 100
        val vm = model(); advanceUntilIdle()
        vm.change(PersonalField.NAME, "A"); vm.change(PersonalField.NAME, "Ana")
        session.user.value = RiderUser(id = "bob", name = "Roberto")
        advanceUntilIdle()
        assertEquals("Roberto", vm.uiState.value.name)
        vm.change(PersonalField.NAME, "Beto"); advanceUntilIdle()
        session.user.value = RiderUser(id = "ana"); advanceUntilIdle()
        assertEquals("Ana", vm.uiState.value.name)
        assertEquals("Beto", drafts.load("bob")!!.name)
    }

    @Test fun diskFailureBlocksRemoteSaveUntilRetry() = runTest {
        val vm = model(); advanceUntilIdle()
        drafts.failWrite = true
        vm.selectVehicle(VehicleType.BICYCLE); advanceUntilIdle()
        var navigated = false
        vm.saveVehicle { navigated = true }; advanceUntilIdle()
        assertEquals(DraftStatus.ERROR, vm.uiState.value.draftStatus)
        assertEquals(0, vehicles.vehicleCalls); assertFalse(navigated)
        drafts.failWrite = false
        vm.retryDraft(); advanceUntilIdle()
        vm.saveVehicle { navigated = true }; advanceUntilIdle()
        assertTrue(navigated); assertEquals(1, vehicles.vehicleCalls)
    }

    @Test fun failedReadDoesNotOverwriteExistingDraftAndCanBeRetried() = runTest {
        drafts.save(RegistrationDraft("ana", name = "Ana local", personalDirty = true))
        drafts.failRead = true
        val vm = model(); advanceUntilIdle()
        vm.change(PersonalField.NAME, "Overwrite")
        assertFalse(vm.uiState.value.ready)
        drafts.failRead = false
        vm.retryDraft(); advanceUntilIdle()
        assertEquals("Ana local", vm.uiState.value.name)
    }

    @Test fun duplicateContinueOnlySendsOneRequest() = runTest {
        val vm = model(); advanceUntilIdle()
        vm.selectVehicle(VehicleType.BICYCLE); advanceUntilIdle()
        var callbacks = 0
        vm.saveVehicle { callbacks++ }; vm.saveVehicle { callbacks++ }
        advanceUntilIdle()
        assertEquals(1, vehicles.vehicleCalls); assertEquals(1, callbacks)
    }

    @Test fun switchingAccountCancelsPendingSaveAndNavigation() = runTest {
        val vm = model(); advanceUntilIdle()
        vm.selectVehicle(VehicleType.BICYCLE); advanceUntilIdle()
        var navigated = false
        vm.saveVehicle { navigated = true }; runCurrent()
        session.user.value = RiderUser(id = "bob")
        advanceUntilIdle()
        assertFalse(navigated)
        assertEquals(VehicleType.NONE, VehicleType.fromId(session.user.value!!.vehicleTypeId))
        assertEquals(VehicleType.NONE, vm.uiState.value.vehicle)
        assertEquals(VehicleType.BICYCLE, drafts.load("ana")!!.vehicle)
    }

    @Test fun linkedPasswordIsClearedAndServerConfirmedPersonalDataReplacesDraft() = runTest {
        val vm = model(); advanceUntilIdle()
        vm.change(PersonalField.NAME, "Ana"); vm.change(PersonalField.LAST_NAME, "Perez")
        vm.change(PersonalField.DNI, "12345678"); vm.change(PersonalField.PHONE, "987654321")
        vm.change(PersonalField.PASSWORD, "password123"); vm.change(PersonalField.CONFIRMATION, "password123")
        vm.acceptTerms(true); advanceUntilIdle()
        var saved = false
        vm.savePersonal { saved = true }; advanceUntilIdle()
        assertTrue(saved); assertFalse(vm.uiState.value.needsPassword)
        assertEquals("", vm.uiState.value.password); assertEquals("", vm.uiState.value.confirmation)
        assertFalse(drafts.load("ana")!!.personalDirty)
        assertEquals("Ana", session.user.value!!.name)
    }

    @Test fun rejectedSubmissionRetainsDraftAndOnlyServerSuccessClearsIt() = runTest {
        session.user.value = RiderUser(id = "ana", hasPassword = true, vehicleTypeId = VehicleType.BICYCLE.name)
        val vm = model(); advanceUntilIdle()
        vm.changeBank(BankField.BANK, "BCP")
        vm.changeBank(BankField.HOLDER, "Ana")
        vm.changeBank(BankField.CCI, "00219100123456780123")
        advanceUntilIdle()
        repository.failSubmit = true
        var callbacks = 0
        vm.submit { callbacks++ }; advanceUntilIdle()
        assertEquals(0, callbacks); assertNotNull(drafts.load("ana"))
        assertEquals("server unavailable", vm.uiState.value.errorMessage)
        repository.failSubmit = false
        vm.submit { callbacks++ }; advanceUntilIdle()
        assertEquals(1, callbacks); assertNull(drafts.load("ana"))
        assertEquals(RegistrationStatus.PENDING_REVIEW, session.user.value!!.status)
        assertFalse(vm.uiState.value.editable)
    }

    @Test fun approvedAccountDiscardsOldDraftAndCannotEditRegistration() = runTest {
        drafts.save(RegistrationDraft("ana", name = "Stale", personalDirty = true))
        session.user.value = RiderUser(id = "ana", status = RegistrationStatus.APPROVED)
        val vm = model(); advanceUntilIdle()
        vm.change(PersonalField.NAME, "Forbidden"); advanceUntilIdle()
        assertFalse(vm.uiState.value.editable); assertNull(drafts.load("ana"))
    }

    @Test fun definitiveRejectionClosesRegistrationAndDiscardsLocalDraft() = runTest {
        drafts.save(RegistrationDraft("ana", name = "Stale", personalDirty = true))
        session.user.value = RiderUser(id = "ana", status = RegistrationStatus.REJECTED)
        val vm = model(); advanceUntilIdle()
        vm.selectVehicle(VehicleType.BICYCLE); vm.saveVehicle { fail("Must not navigate") }
        advanceUntilIdle()
        assertFalse(vm.uiState.value.editable); assertEquals(0, vehicles.vehicleCalls)
        assertNull(drafts.load("ana"))
    }

    @Test fun observedUploadedDocumentMustBeReplacedBeforeResubmission() = runTest {
        session.user.value = RiderUser(id = "ana", hasPassword = true,
            vehicleTypeId = VehicleType.BICYCLE.name,
            status = RegistrationStatus.NEEDS_CORRECTION, rejectionReason = "Reemplaza el DNI")
        banks.saved = BankInfo("BCP", "Ana", "123", "00219100123456780123")
        documents.snapshot = documents.snapshot.copy(
            reviews = mapOf("dniFront" to DocumentReview(DocumentReviewStatus.OBSERVED, "Borrosa")))
        val vm = model(); advanceUntilIdle()
        vm.submit { fail("Must not navigate") }; advanceUntilIdle()
        assertEquals(TextKey.REGISTRATION_REPLACE_OBSERVED.name, vm.uiState.value.errorMessage)
        assertEquals(0, repository.submitCalls)
        documents.snapshot = documents.snapshot.copy(reviews = mapOf("dniFront" to DocumentReview()))
        session.user.value = session.user.value!!.copy(submissionVersion = 1)
        advanceUntilIdle()
        var submitted = false
        vm.submit { submitted = true }; advanceUntilIdle()
        assertTrue(submitted); assertEquals(1, repository.submitCalls)
    }

    private val documents = object : DocumentRepository {
        var snapshot = DocumentSnapshot(
            paths = mapOf("dniFront" to "front", "dniBack" to "back", "bankStatement" to "bank"))
        override suspend fun load() = Result.success(snapshot)
        override suspend fun createCapture() = error("unused")
        override suspend fun hasCapture(uri: Uri) = error("unused")
        override suspend fun deleteCapture(uri: Uri) = error("unused")
        override suspend fun uploadDocument(docType: String, uri: Uri) = error("unused")
        override suspend fun getDocument(docType: String) = error("unused")
    }

    private val banks = object : BankRepository {
        var saved = BankInfo()
        override suspend fun load() = Result.success(saved)
        override suspend fun save(bank: BankInfo): Result<BankInfo> { saved = bank; return Result.success(bank) }
    }

    private val vehicles = object : VehicleRepository {
        var vehicleCalls = 0
        override suspend fun load() = Result.success(VehicleType.NONE)
        override suspend fun save(vehicle: VehicleType): Result<VehicleType> {
            vehicleCalls++; delay(50)
            session.user.value = session.user.value!!.copy(vehicleTypeId = vehicle.name)
            return Result.success(vehicle)
        }
    }

    private inner class Repository : FakeRiderUserRepository() {
        var submitCalls = 0
        var failSubmit = false
        private fun update(user: RiderUser): Result<RiderUser> {
            session.user.value = user
            return Result.success(user)
        }
        override suspend fun linkPassword(password: String): Result<Unit> {
            session.user.value = session.user.value!!.copy(hasPassword = true)
            return Result.success(Unit)
        }
        override suspend fun getCurrentUser() = Result.success(session.user.value)
        override suspend fun savePersonalData(name: String, lastName: String, dni: String, phone: String, terms: Boolean) =
            update(session.user.value!!.copy(name = name, lastName = lastName, dni = dni, phone = phone, termsAccepted = terms))
        override suspend fun submitRegistration(): Result<RiderUser> {
            submitCalls++
            return if (failSubmit) Result.failure(IllegalStateException("server unavailable"))
                else update(session.user.value!!.copy(status = RegistrationStatus.PENDING_REVIEW))
        }
        override suspend fun setAvailability(available: Boolean) = error("unused")
    }
}

internal class MemoryDrafts : RegistrationDraftRepository {
    private val drafts = mutableMapOf<String, RegistrationDraft>()
    private val captures = mutableMapOf<String, PendingDocumentCapture>()
    var failWrite = false
    var failRead = false
    var writeDelay = 0L
    override suspend fun load(uid: String): RegistrationDraft? {
        check(!failRead); return drafts[uid]
    }
    override suspend fun save(draft: RegistrationDraft) {
        delay(writeDelay); check(!failWrite); drafts[draft.uid] = draft
    }
    override suspend fun clear(uid: String) { drafts.remove(uid) }
    override suspend fun loadCapture(uid: String) = captures[uid]
    override suspend fun saveCapture(capture: PendingDocumentCapture) { captures[capture.uid] = capture }
    override suspend fun clearCapture(uid: String) { captures.remove(uid) }
}
