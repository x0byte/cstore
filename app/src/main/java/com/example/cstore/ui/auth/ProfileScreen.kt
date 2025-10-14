package com.example.cstore.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Divider
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.cstore.ui.components.ListingCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    authViewModel: AuthViewModel, 
    profileViewModel: ProfileViewModel,
    onSignOut: () -> Unit, 
    onCreateListing: (() -> Unit)? = null,
    onItemClick: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val profileState by profileViewModel.uiState.collectAsState()

    // Load profile when screen is first displayed
    LaunchedEffect(Unit) {
        val uid = authViewModel.currentUserUid()
        if (uid != null) {
            profileViewModel.loadUserProfile(uid)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Text("My Profile", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))

        val currentState = profileState
        when (currentState) {
            is ProfileUiState.Loading -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(8.dp))
                    Text("Loading profile...")
                }
            }
            is ProfileUiState.Error -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(currentState.message, color = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = {
                        val uid = authViewModel.currentUserUid()
                        if (uid != null) profileViewModel.loadUserProfile(uid)
                    }) { Text("Retry") }
                }
            }
            is ProfileUiState.Success -> {
                // Profile Info Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Email: ${currentState.profile.email}", style = MaterialTheme.typography.bodyLarge)
                        Spacer(Modifier.height(8.dp))
                        Text("UID: ${currentState.profile.uid}", style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(8.dp))
                        Text("Joined: ${currentState.profile.createdAt}", style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(16.dp))
                        Divider()
                        Spacer(Modifier.height(16.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Button(onClick = { onCreateListing?.invoke() }) { Text("Create Listing") }
                            Spacer(Modifier.width(12.dp))
                            Button(onClick = onSignOut) { Text("Sign Out") }
                        }
                    }
                }
                
                Spacer(Modifier.height(16.dp))
                
                // My Items Section
                Text("My Items (${currentState.userListings.size})", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                
                if (currentState.userListings.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("No items yet", style = MaterialTheme.typography.bodyLarge)
                            Spacer(Modifier.height(8.dp))
                            Text("Create your first listing to get started!", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(currentState.userListings) { listing ->
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


