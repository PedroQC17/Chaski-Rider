package com.example.chaskirider.feature.user.ui.viewmodel

import com.example.chaskirider.feature.bank.domain.BankInfo
import com.example.chaskirider.feature.bank.domain.BankPolicy
import com.example.chaskirider.feature.bank.domain.BankRepository
import com.example.chaskirider.feature.vehicle.domain.VehiclePolicy
import com.example.chaskirider.feature.vehicle.domain.VehicleRepository
import com.example.chaskirider.feature.documents.domain.DocumentRepository
import com.example.chaskirider.feature.documents.domain.DocumentSnapshot

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chaskirider.feature.user.domain.RiderUser
import com.example.chaskirider.feature.user.domain.RegistrationStatus
import com.example.chaskirider.feature.user.domain.RegistrationDraft
import com.example.chaskirider.feature.user.domain.RiderUserPolicy
import com.example.chaskirider.feature.user.domain.RiderUserService
import com.example.chaskirider.feature.vehicle.domain.VehicleType
import com.example.chaskirider.feature.user.domain.RiderSession
import com.example.chaskirider.feature.user.domain.RiderUserRepository
import com.example.chaskirider.feature.user.domain.RegistrationDraftRepository
import com.example.chaskirider.core.text.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*

/** One owner for the three registration forms. Server state and local drafts remain distinct. */
class RegistrationViewModel(
    private val users: RiderUserRepository,
    private val bank: BankRepository,
    private val vehicle: VehicleRepository,
    private val documents: DocumentRepository,
    private val session: RiderSession,
    private val texts: TextProvider,
    private val drafts: RegistrationDraftRepository
) : ViewModel() {
    private val state = MutableStateFlow(RegistrationUiState())
    val uiState = state.asStateFlow()
    private var uid: String? = null
    private var draft: RegistrationDraft? = null
    private var actionJob: Job? = null
    private var loadJob: Job? = null
    private var docsJob: Job? = null
    private var documentsSnapshot: DocumentSnapshot? = null
    private var revision = 0L
    private data class Write(val uid: String, val draft: RegistrationDraft?, val revision: Long,
        val done: CompletableDeferred<Boolean> = CompletableDeferred())
    private val writes = Channel<Write>(Channel.UNLIMITED)
    private var latestWrite: Write? = null

    init {
        // Serial writes prevent an older keystroke or a delayed write from overwriting a newer draft/deletion.
        viewModelScope.launch {
            for (write in writes) {
                val success = try {
                    if (write.draft == null) drafts.clear(write.uid) else drafts.save(write.draft)
                    true
                } catch (e: CancellationException) { throw e }
                catch (_: Exception) { false }
                if (uid == write.uid && revision == write.revision) {
                    state.update { it.copy(draftStatus = if (success) DraftStatus.SAVED else DraftStatus.ERROR) }
                }
                write.done.complete(success)
            }
        }
        viewModelScope.launch { session.user.collect(::acceptUser) }
    }

    private fun canEdit(user: RiderUser) = user.isEnabled && user.status in
        listOf(RegistrationStatus.INCOMPLETE, RegistrationStatus.NEEDS_CORRECTION)

    private fun acceptUser(user: RiderUser?) {
        if (uid != user?.id) {
            actionJob?.cancel(); loadJob?.cancel()
            uid = user?.id; draft = null
            state.value = RegistrationUiState()
            if (user != null) restore(user)
            return
        }
        if (user == null) return
        state.update { it.copy(email = user.email, editable = canEdit(user), needsPassword = !user.hasPassword,
            registrationStatus = user.status, reviewReason = user.rejectionReason,
            password = if (user.hasPassword) "" else it.password,
            confirmation = if (user.hasPassword) "" else it.confirmation) }
        refreshDocuments(user.id)
        if (!canEdit(user) && draft != null) {
            draft = null
            enqueue(user.id, null)
        }
    }

    private fun refreshDocuments(owner: String) {
        docsJob?.cancel()
        docsJob = viewModelScope.launch {
            documents.load().onSuccess { loaded ->
                if (uid != owner) return@launch
                documentsSnapshot = loaded
                state.update { it.copy(documentReviews = loaded.reviews) }
            }
        }
    }

    private fun restore(user: RiderUser) {
        loadJob = viewModelScope.launch {
            // A quick logout/login must wait for any preceding edits for this account.
            latestWrite?.done?.await()
            try {
                val saved = if (canEdit(user)) drafts.load(user.id) else null
                val current = session.user.value ?: return@launch
                if (current.id != user.id) return@launch
                if (!canEdit(current)) {
                    drafts.clear(current.id)
                    state.value = RegistrationUiState(ready = true, email = current.email,
                        needsPassword = !current.hasPassword, draftStatus = DraftStatus.SAVED)
                    return@launch
                }
                val savedBank = bank.load().getOrNull() ?: BankInfo()
                val restored = (saved ?: RegistrationDraft(current.id)).mergeWith(current, savedBank)
                draft = restored
                state.value = RegistrationUiState(name = restored.name, lastName = restored.lastName,
                    dni = restored.dni, phone = restored.phone, termsAccepted = restored.termsAccepted,
                    email = current.email, needsPassword = !current.hasPassword,
                    registrationStatus = current.status, reviewReason = current.rejectionReason,
                    vehicle = restored.vehicle, bank = restored.bank, ready = true, editable = true,
                    draftStatus = DraftStatus.SAVED)
                refreshDocuments(current.id)
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) {
                state.update { it.copy(draftStatus = DraftStatus.ERROR, ready = false) }
            }
        }
    }

    private fun enqueue(owner: String, value: RegistrationDraft?) {
        revision++
        val write = Write(owner, value, revision)
        latestWrite = write
        state.update { it.copy(draftStatus = DraftStatus.SAVING) }
        writes.trySend(write)
    }

    private fun persist(value: RegistrationDraft) {
        draft = value
        enqueue(value.uid, value)
    }

    fun retryDraft() {
        if (state.value.isLoading) return
        val current = session.user.value ?: return
        if (!state.value.ready) {
            loadJob?.cancel()
            state.update { it.copy(draftStatus = DraftStatus.LOADING) }
            restore(current)
        } else draft?.let(::persist)
    }

    fun clearError() { state.update { it.copy(errorMessage = null) } }

    fun change(field: PersonalField, value: String) {
        if (!state.value.controlsEnabled) return
        val updated = when (field) {
            PersonalField.NAME -> state.value.copy(name = value.take(150))
            PersonalField.LAST_NAME -> state.value.copy(lastName = value.take(150))
            PersonalField.DNI -> state.value.copy(dni = value.filter { it in '0'..'9' }.take(8))
            PersonalField.PHONE -> state.value.copy(phone = value.filter { it in '0'..'9' }.take(9))
            PersonalField.PASSWORD -> state.value.copy(password = value)
            PersonalField.CONFIRMATION -> state.value.copy(confirmation = value)
        }
        state.value = updated.copy(errorMessage = null)
        if (field == PersonalField.PASSWORD || field == PersonalField.CONFIRMATION) return
        draft?.let { persist(it.copy(name = updated.name, lastName = updated.lastName,
            dni = updated.dni, phone = updated.phone, personalDirty = true)) }
    }

    fun acceptTerms(value: Boolean) {
        if (!state.value.controlsEnabled) return
        state.update { it.copy(termsAccepted = value, errorMessage = null) }
        draft?.let { persist(it.copy(termsAccepted = value, personalDirty = true)) }
    }

    fun selectVehicle(value: VehicleType) {
        if (!state.value.controlsEnabled || value !in listOf(VehicleType.BICYCLE, VehicleType.MOTORCYCLE)) return
        state.update { it.copy(vehicle = value, errorMessage = null) }
        draft?.let { persist(it.copy(vehicle = value, vehicleDirty = true)) }
    }

    fun changeBank(field: BankField, value: String) {
        if (!state.value.controlsEnabled) return
        val bank = when (field) {
            BankField.BANK -> state.value.bank.copy(bankName = value)
            BankField.HOLDER -> state.value.bank.copy(holderName = value.take(150))
            BankField.ACCOUNT -> state.value.bank.copy(accountNumber = value.take(40))
            BankField.CCI -> state.value.bank.copy(cci = value.filter { it in '0'..'9' }.take(20))
        }
        state.update { it.copy(bank = bank, errorMessage = null) }
        draft?.let { persist(it.copy(bank = bank, bankDirty = true)) }
    }

    private fun error(key: TextKey) { state.update { it.copy(errorMessage = texts.get(key)) } }

    private fun action(onSuccess: () -> Unit, block: suspend () -> Unit) {
        if (!state.value.controlsEnabled) return
        val owner = uid ?: return
        state.update { it.copy(isLoading = true, errorMessage = null) }
        actionJob = viewModelScope.launch {
            try {
                if (latestWrite?.done?.await() == false) return@launch
                block()
                if (uid == owner) { state.update { it.copy(isLoading = false) }; onSuccess() }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                if (uid == owner) state.update { it.copy(errorMessage = texts.resolveError(e.message,
                    TextKey.TEXT_NO_SE_PUDO_COMPLETAR_LA_OPERACION)) }
            } finally { if (uid == owner) state.update { it.copy(isLoading = false) } }
        }
    }

    fun savePersonal(onSuccess: () -> Unit) {
        val form = state.value
        val invalid = RiderUserPolicy.personalError(form.name, form.lastName, form.dni, form.phone, form.termsAccepted)
            ?: if (form.needsPassword && (form.password.length < 8 || form.password != form.confirmation))
                TextKey.TEXT_USA_AL_MENOS_8_CARACTERES_Y_CONFIRMA else null
        if (invalid != null) { error(invalid); return }
        action(onSuccess) {
            if (form.needsPassword) {
                users.linkPassword(form.password).getOrThrow()
                state.update { it.copy(needsPassword = false, password = "", confirmation = "") }
            }
            val user = users.savePersonalData(form.name, form.lastName, form.dni, form.phone, form.termsAccepted).getOrThrow()
            val bankInfo = bank.load().getOrNull() ?: BankInfo()
            draft?.let { persist(it.copy(personalDirty = false).mergeWith(user, bankInfo)) }
        }
    }

    fun saveVehicle(onSuccess: () -> Unit) {
        val selected = state.value.vehicle
        if (!VehiclePolicy.isSelected(selected)) {
            error(TextKey.TEXT_SELECCIONA_UN_VEHICULO); return
        }
        action(onSuccess) {
            vehicle.save(selected).getOrThrow()
            val bankInfo = bank.load().getOrNull() ?: BankInfo()
            session.user.value?.let { user ->
                draft?.let { persist(it.copy(vehicleDirty = false).mergeWith(user, bankInfo)) }
            }
        }
    }

    fun submit(onSuccess: () -> Unit) {
        val user = session.user.value ?: return
        val bankInfo = state.value.bank
        BankPolicy.error(bankInfo)?.let { error(it); return }
        // Required documents follow the vehicle already confirmed by the server.
        val vehicleType = VehicleType.fromId(user.vehicleTypeId)
        if (!VehiclePolicy.isSelected(vehicleType)) {
            error(TextKey.TEXT_SELECCIONA_UN_VEHICULO); return
        }
        val required = VehiclePolicy.requiredDocuments(vehicleType)
        action(onSuccess) {
            val docs = documents.load().getOrThrow()
            documentsSnapshot = docs
            if (required.any { !docs.exists(it) })
                throw IllegalStateException(texts.get(TextKey.TEXT_SUBE_TODOS_LOS_DOCUMENTOS_OBLIGATORIOS_ANTES_DE))
            if (required.any { docs.isObserved(it) })
                throw IllegalStateException(texts.get(TextKey.REGISTRATION_REPLACE_OBSERVED))
            bank.save(bankInfo).getOrThrow()
            users.submitRegistration().getOrThrow()
            draft = null
            enqueue(user.id, null)
        }
    }
}
