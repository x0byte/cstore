package com.example.cstore.ui.listing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cstore.data.listing.Listing
import com.example.cstore.data.listing.ListingRepository
import com.example.cstore.data.user.UserProfile
import com.example.cstore.data.user.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class ItemDetailUiState {
    data object Loading : ItemDetailUiState()
    data class Success(val listing: Listing, val ownerProfile: UserProfile?) : ItemDetailUiState()
    data class Error(val message: String) : ItemDetailUiState()
}

class ItemDetailViewModel(
    private val listingRepository: ListingRepository = ListingRepository(),
    private val userRepository: UserRepository = UserRepository()
) : ViewModel() {
    private val _uiState = MutableStateFlow<ItemDetailUiState>(ItemDetailUiState.Loading)
    val uiState: StateFlow<ItemDetailUiState> = _uiState

    fun loadItemDetails(listingId: String) {
        _uiState.value = ItemDetailUiState.Loading
        viewModelScope.launch {
            try {
                val listingResult = listingRepository.getListingById(listingId)
                listingResult.fold(
                    onSuccess = { listing ->
                        if (listing != null) {
                            // Load owner profile
                            val ownerResult = userRepository.getUserProfile(listing.userId)
                            ownerResult.fold(
                                onSuccess = { ownerProfile ->
                                    _uiState.value = ItemDetailUiState.Success(listing, ownerProfile)
                                },
                                onFailure = { e ->
                                    // Still show listing even if owner profile fails to load
                                    _uiState.value = ItemDetailUiState.Success(listing, null)
                                }
                            )
                        } else {
                            _uiState.value = ItemDetailUiState.Error("Item not found")
                        }
                    },
                    onFailure = { e ->
                        _uiState.value = ItemDetailUiState.Error("Failed to load item: ${e.message}")
                    }
                )
            } catch (e: Exception) {
                _uiState.value = ItemDetailUiState.Error("Request failed: ${e.message}")
            }
        }
    }
}
