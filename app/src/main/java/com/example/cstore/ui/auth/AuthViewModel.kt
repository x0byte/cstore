package com.example.cstore.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cstore.data.auth.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class AuthUiState {
    data object Idle : AuthUiState()
    data object Loading : AuthUiState()
    data class Success(val uid: String) : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}

class AuthViewModel(
    private val repository: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState

    fun signUp(email: String, password: String) {
        val validationError = validate(email, password)
        if (validationError != null) {
            _uiState.value = AuthUiState.Error(validationError)
            return
        }
        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            val result = repository.signUpWithEmail(email.trim(), password)
            _uiState.value = result.fold(
                onSuccess = { uid -> AuthUiState.Success(uid) },
                onFailure = { e -> AuthUiState.Error(e.message ?: "Sign up failed") }
            )
        }
    }

    fun signIn(email: String, password: String) {
        val validationError = validateBasic(email, password)
        if (validationError != null) {
            _uiState.value = AuthUiState.Error(validationError)
            return
        }
        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            val result = repository.signInWithEmail(email.trim(), password)
            _uiState.value = result.fold(
                onSuccess = { uid -> AuthUiState.Success(uid) },
                onFailure = { e -> AuthUiState.Error(e.message ?: "Sign in failed") }
            )
        }
    }

    fun signInWithGoogle(idToken: String) {
        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            val result = repository.signInWithGoogle(idToken)
            _uiState.value = result.fold(
                onSuccess = { uid -> AuthUiState.Success(uid) },
                onFailure = { e -> AuthUiState.Error(e.message ?: "Google sign-in failed") }
            )
        }
    }

    fun signOut() {
        repository.signOut()
        _uiState.value = AuthUiState.Idle
    }

    fun currentUserEmail(): String? = repository.getCurrentUserEmail()

    fun reportError(message: String) {
        _uiState.value = AuthUiState.Error(message)
    }

    private fun validate(email: String, password: String): String? {
        if (!isValidEmail(email)) return "Invalid email format"
        if (password.length < 8) return "Password must be at least 8 characters"
        return null
    }

    private fun validateBasic(email: String, password: String): String? {
        if (!isValidEmail(email)) return "Invalid email format"
        if (password.isBlank()) return "Password is required"
        return null
    }

    private fun isValidEmail(email: String): Boolean {
        val trimmed = email.trim()
        return trimmed.isNotEmpty() && android.util.Patterns.EMAIL_ADDRESS.matcher(trimmed).matches()
    }
}


