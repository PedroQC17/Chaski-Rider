package com.example.chaskirider

import androidx.lifecycle.ViewModelStore
import com.example.chaskirider.domain.model.*
import com.example.chaskirider.domain.repository.*
import com.example.chaskirider.domain.text.*
import com.example.chaskirider.ui.screens.onboarding.*
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
    private val profiles = Profiles()
    private val auth = object : AuthRepository {
        override suspend fun loginWithEmail(email: String, pass: String) = error("unused")
        override suspend fun loginWithGoogle(idToken: String) = error("unused")
        override suspend fun sendPasswordResetEmail(email: String) = Result.success(Unit)
        override suspend fun linkPassword(password: String): Result<Unit> {
            session.user.value = session.user.value!!.copy(hasPassword = true)
            return Result.success(Unit)
        }
        override suspend fun logout() { session.user.value = null }
        override suspend fun getCurrentUser() = Result.success(session.user.value)
    }
    private val texts = object : TextProvider {
        override fun get(key: TextKey) = key.name
        override fun resolveError(message: String?, fallback: TextKey) = message ?: fallback.name
    }
    @Before fun setup() { Dispatchers.setMain(StandardTestDispatcher()) }
    @After fun teardown() { store.clear(); Dispatchers.resetMain() }
    private fun model() = RegistrationViewModel(auth, profiles, session, texts, drafts).also { store.put("registration", it) }

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
        assertEquals(0, profiles.vehicleCalls); assertFalse(navigated)
        drafts.failWrite = false
        vm.retryDraft(); advanceUntilIdle()
        vm.saveVehicle { navigated = true }; advanceUntilIdle()
        assertTrue(navigated); assertEquals(1, profiles.vehicleCalls)
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
        assertEquals(1, profiles.vehicleCalls); assertEquals(1, callbacks)
    }

    @Test fun switchingAccountCancelsPendingSaveAndNavigation() = runTest {
        val vm = model(); advanceUntilIdle()
        vm.selectVehicle(VehicleType.BICYCLE); advanceUntilIdle()
        var navigated = false
        vm.saveVehicle { navigated = true }; runCurrent()
        session.user.value = RiderUser(id = "bob")
        advanceUntilIdle()
        assertFalse(navigated)
        assertEquals(VehicleType.NONE, session.user.value!!.vehicleType)
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
        session.user.value = RiderUser(id = "ana", hasPassword = true, vehicleType = VehicleType.BICYCLE,
            dniFrontUrl = "front", dniBackUrl = "back", bankStatementUrl = "bank")
        val vm = model(); advanceUntilIdle()
        vm.changeBank(BankField.BANK, "BCP")
        vm.changeBank(BankField.HOLDER, "Ana")
        vm.changeBank(BankField.CCI, "00219100123456780123")
        advanceUntilIdle()
        profiles.failSubmit = true
        var callbacks = 0
        vm.submit { callbacks++ }; advanceUntilIdle()
        assertEquals(0, callbacks); assertNotNull(drafts.load("ana"))
        assertEquals("server unavailable", vm.uiState.value.errorMessage)
        profiles.failSubmit = false
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
        assertFalse(vm.uiState.value.editable); assertEquals(0, profiles.vehicleCalls)
        assertNull(drafts.load("ana"))
    }

    @Test fun observedUploadedDocumentMustBeReplacedBeforeResubmission() = runTest {
        session.user.value = RiderUser(id = "ana", hasPassword = true, vehicleType = VehicleType.BICYCLE,
            status = RegistrationStatus.NEEDS_CORRECTION, rejectionReason = "Reemplaza el DNI",
            dniFrontUrl = "front", dniBackUrl = "back", bankStatementUrl = "bank",
            bankInfo = BankInfo("BCP", "Ana", "123", "00219100123456780123"),
            documentReviews = mapOf("dniFront" to DocumentReview(DocumentReviewStatus.OBSERVED, "Borrosa")))
        val vm = model(); advanceUntilIdle()
        vm.submit { fail("Must not navigate") }; advanceUntilIdle()
        assertEquals(TextKey.REGISTRATION_REPLACE_OBSERVED.name, vm.uiState.value.errorMessage)
        assertEquals(0, profiles.submitCalls)
        session.user.value = session.user.value!!.copy(dniFrontUrl = "new-front",
            documentReviews = mapOf("dniFront" to DocumentReview()))
        advanceUntilIdle()
        var submitted = false
        vm.submit { submitted = true }; advanceUntilIdle()
        assertTrue(submitted); assertEquals(1, profiles.submitCalls)
    }

    private inner class Profiles : RiderProfileRepository {
        var vehicleCalls = 0
        var submitCalls = 0
        var failSubmit = false
        private fun update(user: RiderUser): Result<RiderUser> {
            session.user.value = user
            return Result.success(user)
        }
        override suspend fun savePersonalData(name: String, lastName: String, dni: String, phone: String, terms: Boolean) =
            update(session.user.value!!.copy(name = name, lastName = lastName, dni = dni, phone = phone, termsAccepted = terms))
        override suspend fun saveVehicle(vehicle: VehicleType): Result<RiderUser> {
            vehicleCalls++; delay(50)
            return update(session.user.value!!.copy(vehicleType = vehicle))
        }
        override suspend fun saveBankInfo(bank: BankInfo) = update(session.user.value!!.copy(bankInfo = bank))
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
