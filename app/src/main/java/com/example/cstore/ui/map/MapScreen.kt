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
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
                        text = "🗺️ Google Maps - ${currentState.listings.size} items with locations",
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
        // Show Google Maps with markers
        Box(modifier = modifier.fillMaxSize()) {
            // Google Maps using AndroidView
            AndroidView(
                factory = { context ->
                    MapView(context).apply {
                        onCreate(null)
                        getMapAsync { googleMap ->
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
            
            // Overlay with item count
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
            ) {
                androidx.compose.material3.Card(
                    colors = androidx.compose.material3.CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "🗺️ ${listings.size} items on map",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Google Maps",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            // Show items list at bottom
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                androidx.compose.material3.Card(
                    colors = androidx.compose.material3.CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Items with locations:",
                            style = MaterialTheme.typography.titleSmall
                        )
                        
                        listings.take(3).forEach { listing ->
                            if (listing.latitude != null && listing.longitude != null) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "📍",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = listing.title,
                                            style = MaterialTheme.typography.bodyMedium,
                                            maxLines = 1
                                        )
                                        Text(
                                            text = listing.locationName,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                        
                        if (listings.size > 3) {
                            Text(
                                text = "... and ${listings.size - 3} more",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}