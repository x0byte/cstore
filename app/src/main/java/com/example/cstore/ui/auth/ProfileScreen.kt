package com.example.cstore.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Divider
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ProfileScreen(viewModel: AuthViewModel, onSignOut: () -> Unit) {
    val profile by viewModel.profile.collectAsState()
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("My Profile", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))

        when (state) {
            is AuthUiState.Loading -> {
                CircularProgressIndicator()
            }
            is AuthUiState.Error -> {
                Text((state as AuthUiState.Error).message, color = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(12.dp))
                Button(onClick = {
                    val uid = viewModel.currentUserUid()
                    if (uid != null) viewModel.loadUserProfile(uid)
                }) { Text("Retry") }
            }
            else -> {
                Surface(tonalElevation = 1.dp, shadowElevation = 0.dp) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Email: ${profile?.email ?: "-"}", style = MaterialTheme.typography.bodyLarge)
                        Spacer(Modifier.height(8.dp))
                        Text("UID: ${profile?.uid ?: "-"}", style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(8.dp))
                        Text("Joined: ${profile?.createdAt ?: "-"}", style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(16.dp))
                        Divider()
                    }
                }
                Spacer(Modifier.height(16.dp))
                Button(onClick = onSignOut) { Text("Sign Out") }
            }
        }
    }
}


