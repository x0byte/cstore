package com.example.cstore.ui.search

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cstore.ui.components.ListingCard

// Main Search Screen Composable
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: SearchViewModel = viewModel(),
    onItemClick: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Collect state from ViewModel
    val uiState by viewModel.uiState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val sortBy by viewModel.sortBy.collectAsState()

    //Remember state for UI
    var showFilters by remember { mutableStateOf(false) }

    // Categories
    val categories = listOf("All", "Clothing", "Electronics", "Books", "Furniture", "Other")
    val sortOptions = listOf("Most Recent", "Price: Low to High", "Price: High to Low")

    // Load data when screen opens
    LaunchedEffect(Unit) {
        viewModel.search()
    }

    // Scaffold with TopAppBar
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Search Items") }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            // TextField for search input
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { newQuery ->
                    viewModel.search(query = newQuery)
                },
                placeholder = { Text("Search items...") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "Search")
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.search(query = "") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(Modifier.height(12.dp))

            // Filter and Sort chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Category filter chip
                FilterChip(
                    selected = selectedCategory != "All",
                    onClick = { showFilters = !showFilters },
                    label = { Text("Category: $selectedCategory") }
                )

                // Sort filter chip
                FilterChip(
                    selected = sortBy != "Most Recent",
                    onClick = { /* TODO: Show sort dialog */ },
                    label = { Text("Sort: $sortBy") }
                )

                // Clear filters button
                if (selectedCategory != "All" || searchQuery.isNotEmpty()) {
                    TextButton(onClick = { viewModel.clearFilters() }) {
                        Text("Clear")
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Show/hide filter options
            if (showFilters) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Category", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))

                        // Loop through categories
                        categories.forEach { category ->
                            FilterChip(
                                selected = selectedCategory == category,
                                onClick = {
                                    viewModel.search(category = category)
                                    showFilters = false
                                },
                                label = { Text(category) },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(Modifier.height(4.dp))
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            // Handle different UI states
            when (val state = uiState) {
                is SearchUiState.Loading -> {
                    // Show loading indicator
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                is SearchUiState.Error -> {
                    // Show error message
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = state.message,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(Modifier.height(8.dp))
                            Button(onClick = { viewModel.search() }) {
                                Text("Retry")
                            }
                        }
                    }
                }

                is SearchUiState.Success -> {
                    // Show search results
                    if (state.listings.isEmpty()) {
                        // Empty state
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("No items found")
                                Spacer(Modifier.height(8.dp))
                                if (searchQuery.isNotEmpty() || selectedCategory != "All") {
                                    TextButton(onClick = { viewModel.clearFilters() }) {
                                        Text("Clear filters")
                                    }
                                }
                            }
                        }
                    } else {
                        // Show results count
                        Text(
                            "${state.listings.size} items found",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(Modifier.height(8.dp))

                        //  LazyColumn for scrollable list
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(state.listings) { listing ->
                                ListingCard(
                                    listing = listing,
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = { onItemClick(listing.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}