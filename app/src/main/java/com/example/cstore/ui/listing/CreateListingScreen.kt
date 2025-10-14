package com.example.cstore.ui.listing

import android.Manifest
import android.annotation.SuppressLint
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.cstore.data.listing.Listing
import com.example.cstore.data.listing.ListingRepository
import com.example.cstore.ui.auth.AuthViewModel
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.launch
import androidx.compose.material3.ExperimentalMaterial3Api

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateListingScreen(
    viewModel: AuthViewModel,
    repository: ListingRepository = ListingRepository(),
    onSaved: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var isDonation by remember { mutableStateOf(false) }
    var priceText by remember { mutableStateOf("") }
    var locationName by remember { mutableStateOf("") }
    var latitude by remember { mutableStateOf<Double?>(null) }
    var longitude by remember { mutableStateOf<Double?>(null) }
    var availableOn by remember { mutableStateOf<Long?>(null) }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var isSaving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val categories = listOf("Clothing", "Electronics", "Books", "Furniture", "Other")
    var expanded by remember { mutableStateOf(false) }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        imageUri = uri
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* result ignored; we attempt getLastLocation after */ }

    val fusedClient: FusedLocationProviderClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    @SuppressLint("MissingPermission")
    fun requestLocation() {
        permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        fusedClient.lastLocation.addOnSuccessListener { loc ->
            if (loc != null) {
                latitude = loc.latitude
                longitude = loc.longitude
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Add New Listing", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))

        OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") })
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") })
        Spacer(Modifier.height(8.dp))

        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
            OutlinedTextField(
                value = category,
                onValueChange = {},
                readOnly = true,
                label = { Text("Category") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier.menuAnchor()
            )
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                categories.forEach { item ->
                    DropdownMenuItem(text = { Text(item) }, onClick = { category = item; expanded = false })
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Mark as donation")
            Spacer(Modifier.width(8.dp))
            Switch(checked = isDonation, onCheckedChange = { isDonation = it })
        }
        if (!isDonation) {
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = priceText,
                onValueChange = { priceText = it.filter { ch -> ch.isDigit() || ch == '.' } },
                label = { Text("Price") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
        }

        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = locationName,
            onValueChange = { locationName = it },
            label = { Text("Enter your city or suburb") }
        )
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Button(onClick = { requestLocation() }) { Text("Use my location") }
            Spacer(Modifier.width(12.dp))
            Text(text = if (latitude != null && longitude != null) "(${latitude}, ${longitude})" else "")
        }

        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Button(onClick = { imagePicker.launch("image/*") }) { Text("Pick image") }
            Spacer(Modifier.width(12.dp))
            AsyncImage(model = imageUri, contentDescription = null, modifier = Modifier.size(96.dp))
        }

        Spacer(Modifier.height(8.dp))
        // Simple available date: leave null or set to now (extend later)
        Button(onClick = {
            error = null
            if (title.isBlank() || description.isBlank()) { error = "Title and description required"; return@Button }
            if (!isDonation) {
                val p = priceText.toDoubleOrNull(); if (p == null || p <= 0.0) { error = "Price must be > 0"; return@Button }
            }
            if (locationName.isBlank() && (latitude == null || longitude == null)) { error = "Location required"; return@Button }

            isSaving = true
            val listing = Listing(
                title = title.trim(),
                description = description.trim(),
                category = category,
                price = priceText.toDoubleOrNull(),
                isDonation = isDonation,
                localImageUri = imageUri?.toString(),
                userId = viewModel.currentUserUid() ?: "",
                locationName = locationName.trim(),
                latitude = latitude,
                longitude = longitude,
                availableOn = availableOn
            )

            scope.launch {
                val result = repository.uploadListing(listing)
                isSaving = false
                if (result.isSuccess) onSaved() else error = result.exceptionOrNull()?.message
            }
        }) {
            if (isSaving) { CircularProgressIndicator() } else { Text("Submit") }
        }

        if (error != null) {
            Spacer(Modifier.height(8.dp))
            Text(error!!, color = MaterialTheme.colorScheme.error)
        }
    }
}


