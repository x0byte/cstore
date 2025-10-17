package com.example.cstore.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cstore.data.auth.AuthRepository
import com.example.cstore.data.user.UserRepository
import com.example.cstore.data.user.UserProfile
import java.util.Date
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class AuthUiState {
    data object Idle : AuthUiState()
    data object Loading : AuthUiState()
    data class Success(val uid: String) : AuthUiState()
    data class Error(val message: String) : AuthUiState()
    data class ProfileLoaded(val profile: UserProfile) : AuthUiState()
}

class AuthViewModel(
    private val repository: AuthRepository = AuthRepository(),
    private val userRepository: UserRepository = UserRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState

    private val _profile = MutableStateFlow<UserProfile?>(null)
    val profile: StateFlow<UserProfile?> = _profile

    fun signUp(email: String, password: String) {
        val validationError = validate(email, password)
        if (validationError != null) {
            _uiState.value = AuthUiState.Error(validationError)
            return
        }
        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            val result = repository.signUpWithEmail(email.trim(), password)
            result.onSuccess { uid ->
                // Attempt to create Firestore profile; do not block success if it fails
                val profileResult = userRepository.createUserProfile(uid, email.trim())
                profileResult.onSuccess {
                    loadUserProfile(uid)
                    _uiState.value = AuthUiState.Success(uid)
                }.onFailure {
                    _uiState.value = AuthUiState.Error("Profile creation failed, but sign-up succeeded: ${it.message}")
                }
            }.onFailure { e ->
                _uiState.value = AuthUiState.Error(e.message ?: "Sign up failed")
            }
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
            result.onSuccess { uid ->
                loadUserProfile(uid)
                _uiState.value = AuthUiState.Success(uid)
            }.onFailure { e ->
                _uiState.value = AuthUiState.Error(e.message ?: "Sign in failed")
            }
        }
    }

    fun signInWithGoogle(idToken: String) {
        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            val result = repository.signInWithGoogle(idToken)
            result.onSuccess { uid ->
                loadUserProfile(uid)
                _uiState.value = AuthUiState.Success(uid)
            }.onFailure { e ->
                _uiState.value = AuthUiState.Error(e.message ?: "Google sign-in failed")
            }
        }
    }

    fun signOut() {
        repository.signOut()
        _uiState.value = AuthUiState.Idle
    }

    fun currentUserEmail(): String? = repository.getCurrentUserEmail()
    fun currentUserUid(): String? = repository.getCurrentUserUid()

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

    fun loadUserProfile(uid: String) {
        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            val result = userRepository.getUserProfile(uid)
            result.onSuccess { profile ->
                if (profile != null) {
                    _profile.value = profile
                    _uiState.value = AuthUiState.ProfileLoaded(profile)
                } else {
                    val email = repository.getCurrentUserEmail()
                    if (!email.isNullOrBlank()) {
                        val created = userRepository.createUserProfile(uid, email)
                        created.onSuccess {
                            val newProfile = UserProfile(uid = uid, email = email, createdAt = Date())
                            _profile.value = newProfile
                            _uiState.value = AuthUiState.ProfileLoaded(newProfile)
                        }.onFailure { e ->
                            _uiState.value = AuthUiState.Error(e.message ?: "Failed to create profile")
                        }
                    } else {
                        _uiState.value = AuthUiState.Error("No email available for profile")
                    }
                }
            }.onFailure { e ->
                _uiState.value = AuthUiState.Error(e.message ?: "Failed to load profile")
            }
        }
    }

    suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        return repository.sendPasswordResetEmail(email)
    }
}


