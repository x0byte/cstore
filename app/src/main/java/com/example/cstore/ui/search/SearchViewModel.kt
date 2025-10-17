package com.example.cstore.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cstore.data.listing.Listing
import com.example.cstore.data.listing.ListingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class SearchUiState {
    object Loading : SearchUiState()
    data class Success(val listings: List<Listing>) : SearchUiState()
    data class Error(val message: String) : SearchUiState()
}

class SearchViewModel(
    private val repository: ListingRepository = ListingRepository()
) : ViewModel() {


    private val _uiState = MutableStateFlow<SearchUiState>(SearchUiState.Loading)
    val uiState: StateFlow<SearchUiState> = _uiState


    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory

    private val _sortBy = MutableStateFlow("Most Recent")
    val sortBy: StateFlow<String> = _sortBy


    fun search(
        query: String = _searchQuery.value,
        category: String = _selectedCategory.value,
        sortBy: String = _sortBy.value
    ) {
        // Update search parameters
        _searchQuery.value = query
        _selectedCategory.value = category
        _sortBy.value = sortBy

        // Show loading state
        _uiState.value = SearchUiState.Loading


        viewModelScope.launch {
            try {

                val result = repository.searchListings(
                    query = query,
                    category = if (category == "All") null else category
                )


                if (result.isSuccess) {
                    val listings = result.getOrNull() ?: emptyList()
                    val sortedListings = repository.sortListings(listings, sortBy)
                    _uiState.value = SearchUiState.Success(sortedListings)
                } else {
                    // Handle failure case
                    val error = result.exceptionOrNull()?.message ?: "Search failed"
                    _uiState.value = SearchUiState.Error(error)
                }
            } catch (e: Exception) {

                _uiState.value = SearchUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    // Clear all filters and search again
    fun clearFilters() {
        _searchQuery.value = ""
        _selectedCategory.value = "All"
        _sortBy.value = "Most Recent"
        search()
    }
}