package com.simutrade.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.simutrade.data.repository.AuthRepository
import com.simutrade.data.repository.AuthResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AuthUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val success: Boolean = false
)

class AuthViewModel : ViewModel() {

    private val repository = AuthRepository()

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    // 🔹 Helper para actualizar estado
    private fun updateState(
        isLoading: Boolean? = null,
        error: String? = null,
        success: Boolean? = null
    ) {
        _uiState.value = _uiState.value.copy(
            isLoading = isLoading ?: _uiState.value.isLoading,
            error = error,
            success = success ?: _uiState.value.success
        )
    }

    fun login(email: String, password: String) {

        if (email.isBlank() || password.isBlank()) {
            updateState(error = "Completa todos los campos")
            return
        }

        viewModelScope.launch {
            updateState(isLoading = true, error = null, success = false)

            when (val result = repository.login(email, password)) {
                is AuthResult.Success -> {
                    updateState(isLoading = false, success = true)
                }

                is AuthResult.Error -> {
                    updateState(isLoading = false, error = result.message)
                }
            }
        }
    }

    fun register(email: String, password: String, username: String) {

        if (email.isBlank() || password.isBlank() || username.isBlank()) {
            updateState(error = "Completa todos los campos")
            return
        }

        viewModelScope.launch {
            updateState(isLoading = true, error = null, success = false)

            when (val result = repository.register(email, password, username)) {
                is AuthResult.Success -> {
                    updateState(isLoading = false, success = true)
                }

                is AuthResult.Error -> {
                    updateState(isLoading = false, error = result.message)
                }
            }
        }
    }

    fun clearError() {
        updateState(error = null)
    }

    fun clearSuccess() {
        updateState(success = false)
    }

    fun logout() {
        repository.logout()
    }
}