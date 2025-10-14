package com.example.cstore.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.cstore.ui.components.ListingCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onCreateListing: () -> Unit,
    onProfile: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    // Load listings when screen is first displayed
    LaunchedEffect(Unit) {
        viewModel.loadListings()
    }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("CircularStore") },
                    actions = {
                        TextButton(onClick = onCreateListing) { Text("Add") }
                        TextButton(onClick = onProfile) { Text("Profile") }
                    }
                )
            },
            modifier = modifier
        ) { inner ->
        when (val ui = state) {
            is HomeUiState.Loading -> {
                Column(
                    Modifier.fillMaxSize().padding(inner),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) { Text("Loading…") }
            }
            is HomeUiState.Error -> {
                Column(
                    Modifier.fillMaxSize().padding(inner),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(ui.message, color = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = { viewModel.loadListings() }) { Text("Retry") }
                }
            }
            is HomeUiState.Success -> {
                val listings = ui.listings
                if (listings.isEmpty()) {
                    Column(
                        Modifier.fillMaxSize().padding(inner),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("No items yet")
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = onCreateListing) { Text("Create your first listing") }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(inner),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(16.dp)
                    ) {
                        items(listings) { item ->
                            ListingCard(listing = item, modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            }
        }
    }
}


