package com.example.cstore.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cstore.data.listing.Listing
import com.example.cstore.data.listing.ListingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

sealed class HomeUiState {
    data object Loading : HomeUiState()
    data class Success(val listings: List<Listing>) : HomeUiState()
    data class Error(val message: String) : HomeUiState()
}

class HomeViewModel(
    private val repository: ListingRepository = ListingRepository()
) : ViewModel() {
    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState

    // Don't auto-load on init to prevent crashes before auth

    fun loadListings() {
        _uiState.value = HomeUiState.Loading
        viewModelScope.launch {
            try {
                val result = withTimeout(5000) { repository.getAllListings() }
                _uiState.value = result.fold(
                    onSuccess = { list -> 
                        // Filter out any invalid listings
                        val validListings = list.filter { it.title.isNotBlank() }
                        HomeUiState.Success(validListings)
                    },
                    onFailure = { e -> 
                        HomeUiState.Error("Failed to load listings: ${e.message}")
                    }
                )
            } catch (e: Exception) {
                _uiState.value = HomeUiState.Error("Request failed: ${e.message}")
            }
        }
    }
}


