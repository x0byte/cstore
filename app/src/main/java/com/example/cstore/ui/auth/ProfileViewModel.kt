package com.example.cstore.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cstore.data.listing.Listing
import com.example.cstore.data.listing.ListingRepository
import com.example.cstore.data.user.UserProfile
import com.example.cstore.data.user.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class ProfileUiState {
    data object Loading : ProfileUiState()
    data class Success(val profile: UserProfile, val userListings: List<Listing>) : ProfileUiState()
    data class Error(val message: String) : ProfileUiState()
}

class ProfileViewModel(
    private val userRepository: UserRepository = UserRepository(),
    private val listingRepository: ListingRepository = ListingRepository()
) : ViewModel() {
    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val uiState: StateFlow<ProfileUiState> = _uiState

    fun loadUserProfile(uid: String) {
        _uiState.value = ProfileUiState.Loading
        viewModelScope.launch {
            try {
                val profileResult = userRepository.getUserProfile(uid)
                val listingsResult = listingRepository.getListingsByUserId(uid)
                
                profileResult.fold(
                    onSuccess = { profile ->
                        listingsResult.fold(
                            onSuccess = { listings ->
                                _uiState.value = ProfileUiState.Success(
                                    profile = profile ?: UserProfile(),
                                    userListings = listings
                                )
                            },
                            onFailure = { e ->
                                _uiState.value = ProfileUiState.Error("Failed to load listings: ${e.message}")
                            }
                        )
                    },
                    onFailure = { e ->
                        _uiState.value = ProfileUiState.Error("Failed to load profile: ${e.message}")
                    }
                )
            } catch (e: Exception) {
                _uiState.value = ProfileUiState.Error("Unexpected error: ${e.message}")
            }
        }
    }
}
