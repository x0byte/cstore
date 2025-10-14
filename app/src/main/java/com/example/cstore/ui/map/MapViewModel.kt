package com.example.cstore.ui.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cstore.data.listing.Listing
import com.example.cstore.data.listing.ListingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class MapUiState {
    data object Loading : MapUiState()
    data class Success(val listings: List<Listing>) : MapUiState()
    data class Error(val message: String) : MapUiState()
}

class MapViewModel(
    private val listingRepository: ListingRepository = ListingRepository()
) : ViewModel() {
    private val _uiState = MutableStateFlow<MapUiState>(MapUiState.Loading)
    val uiState: StateFlow<MapUiState> = _uiState

    fun loadListings() {
        _uiState.value = MapUiState.Loading
        viewModelScope.launch {
            try {
                val result = listingRepository.getAllListings()
                result.fold(
                    onSuccess = { listings ->
                        // Filter listings that have valid coordinates
                        val validListings = listings.filter { listing ->
                            listing.latitude != null && listing.longitude != null
                        }
                        _uiState.value = MapUiState.Success(validListings)
                    },
                    onFailure = { e ->
                        _uiState.value = MapUiState.Error("Failed to load listings: ${e.message}")
                    }
                )
            } catch (e: Exception) {
                _uiState.value = MapUiState.Error("Request failed: ${e.message}")
            }
        }
    }
}
