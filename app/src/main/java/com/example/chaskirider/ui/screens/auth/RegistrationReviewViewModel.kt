package com.example.chaskirider.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chaskirider.domain.repository.AuthRepository
import com.example.chaskirider.domain.repository.RiderSession
import com.example.chaskirider.domain.text.TextKey
import com.example.chaskirider.domain.text.TextProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class RegistrationReviewViewModel(
    private val repository: AuthRepository,
    private val session: RiderSession,
    private val texts: TextProvider
) : ViewModel() {
    private val state = MutableStateFlow(RegistrationReviewUiState(user = session.user.value))
    val uiState = state.asStateFlow()
    private var refreshJob: Job? = null

    init {
        viewModelScope.launch {
            session.user.collect { user ->
                if (state.value.user?.id != user?.id) {
                    refreshJob?.cancel()
                    state.value = RegistrationReviewUiState(user = user)
                } else state.update { it.copy(user = user) }
            }
        }
    }

    fun refresh() {
        val owner = session.user.value?.id ?: return
        if (state.value.isRefreshing) return
        state.update { it.copy(isRefreshing = true, errorMessage = null) }
        refreshJob = viewModelScope.launch {
            try {
                val user = repository.getCurrentUser().getOrThrow()
                if (session.user.value?.id == owner && user?.id == owner)
                    state.update { it.copy(user = user) }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                if (session.user.value?.id == owner) state.update { it.copy(errorMessage =
                    texts.resolveError(e.message, TextKey.TEXT_NO_SE_PUDO_COMPLETAR_LA_OPERACION)) }
            } finally {
                if (session.user.value?.id == owner) state.update { it.copy(isRefreshing = false) }
            }
        }
    }
}
