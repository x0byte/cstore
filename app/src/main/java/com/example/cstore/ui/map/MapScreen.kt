package com.example.cstore.ui.map

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cstore.data.listing.Listing
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import androidx.compose.material3.Button


@Composable
fun MapScreen(
    viewModel: MapViewModel = viewModel(),
    onItemClick: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    var selectedListing by remember { mutableStateOf<Listing?>(null) }

    // Load listings when screen is first displayed
    LaunchedEffect(Unit) {
        viewModel.loadListings()
    }

    Box(modifier = modifier.fillMaxSize()) {
        val currentState = state
        when (currentState) {
            is MapUiState.Loading -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                    Text("Loading map...", style = MaterialTheme.typography.bodyLarge)
                }
            }
            is MapUiState.Error -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(currentState.message, color = MaterialTheme.colorScheme.error)
                }
            }
            is MapUiState.Success -> {
                // Debug: Show how many listings we have
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Google Maps - ${currentState.listings.size} items with locations",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(Modifier.height(8.dp))
                }

                MapContent(
                    listings = currentState.listings,
                    onMarkerClick = { listing -> selectedListing = listing },
                    modifier = Modifier.fillMaxSize()
                )
                
                // Show preview popup when a marker is selected
                selectedListing?.let { listing ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        MarkerPreview(
                            listing = listing,
                            onItemClick = { itemId ->
                                selectedListing = null
                                onItemClick(itemId)
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MapContent(
    listings: List<Listing>,
    onMarkerClick: (Listing) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    var searchError by remember { mutableStateOf<String?>(null) }
    fun performSearch(query: String, googleMap: GoogleMap?) {
        if (query.isBlank() || googleMap == null) return

        isSearching = true
        searchError = null

        val placesClient = com.google.android.libraries.places.api.Places.createClient(context)
        val request = com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest.builder()
            .setQuery(query)
            .setLocationBias(
                com.google.android.libraries.places.api.model.RectangularBounds.newInstance(
                    com.google.android.gms.maps.model.LatLng(-37.9, 144.8),  // Southwest
                    com.google.android.gms.maps.model.LatLng(-37.7, 145.1)   // Northeast
                )
            )
            .build()

        placesClient.findAutocompletePredictions(request)
            .addOnSuccessListener { response ->
                isSearching = false

                if (response.autocompletePredictions.isNotEmpty()) {
                    val prediction = response.autocompletePredictions[0]
                    val placeId = prediction.placeId
                    val placeFields = listOf(
                        com.google.android.libraries.places.api.model.Place.Field.LAT_LNG,
                        com.google.android.libraries.places.api.model.Place.Field.NAME
                    )

                    val fetchRequest = com.google.android.libraries.places.api.net.FetchPlaceRequest.newInstance(placeId, placeFields)

                    placesClient.fetchPlace(fetchRequest)
                        .addOnSuccessListener { fetchResponse ->
                            val place = fetchResponse.place
                            val latLng = place.latLng

                            if (latLng != null) {
                                googleMap.animateCamera(
                                    com.google.android.gms.maps.CameraUpdateFactory.newLatLngZoom(latLng, 14f)
                                )
                                android.util.Log.d("MapSearch", "Found: ${place.name} at $latLng")
                            }
                        }
                        .addOnFailureListener { e ->
                            searchError = "Failed to get place details: ${e.message}"
                            android.util.Log.e("MapSearch", "Fetch place failed", e)
                        }
                } else {
                    searchError = "No results found for \"$query\""
                }
            }
            .addOnFailureListener { e ->
                isSearching = false
                searchError = "Search failed: ${e.message}"
                android.util.Log.e("MapSearch", "Search failed", e)
            }
    }

    var googleMapInstance by remember { mutableStateOf<GoogleMap?>(null) }

    if (listings.isEmpty()) {
        // Show empty state
        Column(
            modifier = modifier.padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "🗺️ No items with location data",
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Create listings with location to see them on the map",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    } else {
        // Show Google Maps with markers and search
        Box(modifier = modifier.fillMaxSize()) {
            // Google Maps using AndroidView
            AndroidView(
                factory = { context ->
                    MapView(context).apply {
                        onCreate(null)
                        getMapAsync { googleMap ->
                            googleMapInstance = googleMap
                            // Configure map
                            googleMap.uiSettings.isZoomControlsEnabled = true
                            googleMap.uiSettings.isMyLocationButtonEnabled = false
                            
                            // Add markers for each listing
                            listings.forEach { listing ->
                                if (listing.latitude != null && listing.longitude != null) {
                                    println("🗺️ Adding Google Maps marker for: ${listing.title} at (${listing.latitude}, ${listing.longitude})")
                                    
                                    val marker = googleMap.addMarker(
                                        MarkerOptions()
                                            .position(LatLng(listing.latitude!!, listing.longitude!!))
                                            .title(listing.title)
                                            .snippet(listing.locationName)
                                    )
                                    
                                    // Set click listener for marker
                                    googleMap.setOnMarkerClickListener { clickedMarker ->
                                        if (clickedMarker == marker) {
                                            onMarkerClick(listing)
                                            true
                                        } else {
                                            false
                                        }
                                    }
                                }
                            }
                            
                            // Center map on first item or Melbourne
                            if (listings.isNotEmpty()) {
                                val firstListing = listings.first()
                                if (firstListing.latitude != null && firstListing.longitude != null) {
                                    val firstLocation = LatLng(firstListing.latitude!!, firstListing.longitude!!)
                                    googleMap.animateCamera(
                                        CameraUpdateFactory.newLatLngZoom(firstLocation, 12f)
                                    )
                                } else {
                                    // Default to Melbourne
                                    val melbourne = LatLng(-37.8136, 144.9631)
                                    googleMap.animateCamera(
                                        CameraUpdateFactory.newLatLngZoom(melbourne, 10f)
                                    )
                                }
                            } else {
                                // Default to Melbourne
                                val melbourne = LatLng(-37.8136, 144.9631)
                                googleMap.animateCamera(
                                    CameraUpdateFactory.newLatLngZoom(melbourne, 10f)
                                )
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
            
            // Search bar at top

            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search places in Melbourne...") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            leadingIcon = {
                                if (isSearching) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(Icons.Default.Search, contentDescription = "Search")
                                }
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = {
                                        searchQuery = ""
                                        searchError = null
                                    }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                                    }
                                }
                            },
                            keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                                onSearch = { performSearch(searchQuery, googleMapInstance) }
                            ),
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                imeAction = androidx.compose.ui.text.input.ImeAction.Search
                            ),
                            enabled = !isSearching
                        )

                        Spacer(Modifier.width(8.dp))

                        Button(
                            onClick = { performSearch(searchQuery, googleMapInstance) },
                            enabled = searchQuery.isNotEmpty() && !isSearching
                        ) {
                            Text("Search")
                        }
                    }
                }
            }


            // Simple item count at bottom
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            ) {
                androidx.compose.material3.Card(
                    colors = androidx.compose.material3.CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                    )
                ) {
                    Text(
                        text = "📍 ${listings.size} items on map",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }
    }
}