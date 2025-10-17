package com.example.cstore.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.cstore.ui.components.ListingCard
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onCreateListing: () -> Unit,
    onProfile: () -> Unit,
    onItemClick: (String) -> Unit = {},
    onChats: () -> Unit,                 // NEW
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val weatherState by viewModel.weatherState.collectAsState()
    val contextState by viewModel.contextState.collectAsState()

    // Load listings when screen is first displayed
    LaunchedEffect(Unit) {
        viewModel.loadListings()
        // Load weather for Melbourne as default (you can change this to user's location)
        viewModel.loadWeather(-37.8136, 144.9631, "dd6f05f68644e8fa202315bd4704d451")
    }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("CircularStore") },
                    actions = {
                        // Chats
                        IconButton(onClick = onChats) {
                            Icon(imageVector = Icons.Filled.Chat, contentDescription = "Chats")
                        }
                        // Add listing
                        IconButton(onClick = onCreateListing) {
                            Icon(imageVector = Icons.Filled.Add, contentDescription = "Add listing")
                        }
                        // Profile
                        IconButton(onClick = onProfile) {
                            Icon(imageVector = Icons.Filled.Person, contentDescription = "Profile")
                        }
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
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 2.dp)
                    ) {
                        // Weather Card
                        item {
                            WeatherCard(weatherState = weatherState, viewModel = viewModel)
                        }
                        
                        // Listings
                        items(listings) { item ->
                            val badges = contextState.listingContexts[item.id]?.badges ?: emptyList()
                            ListingCard(
                                listing = item,
                                badges = badges,
                                modifier = Modifier.fillMaxWidth(),
                                onClick = { onItemClick(item.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WeatherCard(
    weatherState: WeatherUiState,
    viewModel: HomeViewModel
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (weatherState) {
                is WeatherUiState.Loading -> {
                    Text(
                        text = "🌤️ Loading weather...",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                is WeatherUiState.Success -> {
                    val weather = weatherState.weather
                    Text(
                        text = "Weather in ${weather.cityName}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "${weather.temperature.toInt()}°C • ${weather.condition}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = viewModel.getRecommendation(weather.temperature),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
                is WeatherUiState.Error -> {
                    Text(
                        text = "🌤️ Weather unavailable",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Browse all categories!",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}


