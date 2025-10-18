package com.example.cstore.ui.listing

import android.Manifest
import android.annotation.SuppressLint
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import com.example.cstore.ml.ReceiptOCRProcessor
import com.example.cstore.ui.auth.AuthViewModel
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.launch
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import android.graphics.BitmapFactory
import android.provider.MediaStore
import android.location.Geocoder
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource






@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateListingScreen(
    viewModel: AuthViewModel,
    repository: ListingRepository = ListingRepository(),
    onSaved: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val fusedClient: FusedLocationProviderClient = remember {
        LocationServices.getFusedLocationProviderClient(context)
    }
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
    var isScanning by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val categories = listOf("Clothing", "Electronics", "Books", "Furniture", "Other", "Home", "Sports", "Outdoor")
    var expanded by remember { mutableStateOf(false) }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        imageUri = uri
    }
    
    val receiptPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            isScanning = true
            scope.launch {
                try {
                    val bitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                    val result = ReceiptOCRProcessor.processImage(bitmap)
                    result.fold(
                        onSuccess = { data ->
                            data.itemName?.let { title = it }
                            data.price?.let { priceText = it.toString() }
                            data.category?.let { category = it }
                            data.description?.let { description = it }
                            isScanning = false
                        },
                        onFailure = { e ->
                            error = "OCR failed: ${e.message}"
                            isScanning = false
                        }
                    )
                } catch (e: Exception) {
                    error = "Failed to process image: ${e.message}"
                    isScanning = false
                }
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

        android.util.Log.d("LocationDebug", "Permission result - Fine: $fineLocationGranted, Coarse: $coarseLocationGranted")

        if (fineLocationGranted || coarseLocationGranted) {
            @SuppressLint("MissingPermission")
            run {
                val cancellationTokenSource = CancellationTokenSource()

                fusedClient.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    cancellationTokenSource.token
                ).addOnSuccessListener { loc ->
                    if (loc != null) {
                        android.util.Log.d("LocationDebug", "Current location: ${loc.latitude}, ${loc.longitude}")

                        latitude = loc.latitude
                        longitude = loc.longitude

                        scope.launch(Dispatchers.IO) {
                            try {
                                val geocoder = Geocoder(context, Locale.getDefault())

                                @Suppress("DEPRECATION")
                                val addresses = geocoder.getFromLocation(loc.latitude, loc.longitude, 1)

                                if (!addresses.isNullOrEmpty()) {
                                    val address = addresses[0]
                                    val parts = mutableListOf<String>()
                                    address.thoroughfare?.let { parts.add(it) }
                                    address.locality?.let { parts.add(it) }
                                    address.adminArea?.let { parts.add(it) }
                                    withContext(Dispatchers.Main) {
                                        locationName = if (parts.isNotEmpty()) {
                                            parts.joinToString(", ")
                                        } else {
                                            "Unknown location"
                                        }
                                        android.util.Log.d("LocationDebug", "Address resolved: $locationName")
                                    }
                                } else {
                                    withContext(Dispatchers.Main) {
                                        locationName = "Lat: ${loc.latitude}, Lon: ${loc.longitude}"
                                    }
                                }
                            } catch (e: Exception) {
                                android.util.Log.e("LocationDebug", "Geocoding error: ${e.message}", e)
                                withContext(Dispatchers.Main) {
                                    locationName = "Lat: ${loc.latitude}, Lon: ${loc.longitude}"
                                }
                            }
                        }

                    } else {
                        android.util.Log.w("LocationDebug", "Current location is null, trying lastLocation...")

                        fusedClient.lastLocation.addOnSuccessListener { lastLoc ->
                            if (lastLoc != null) {
                                latitude = lastLoc.latitude
                                longitude = lastLoc.longitude
                                locationName = "Lat: ${lastLoc.latitude}, Lon: ${lastLoc.longitude}"
                            } else {
                                error = "Could not get location. Please enter manually."
                            }
                        }.addOnFailureListener { e ->
                            android.util.Log.e("LocationDebug", "lastLocation failed: ${e.message}", e)
                            error = "Location error: ${e.message}"
                        }
                    }
                }.addOnFailureListener { e ->
                    android.util.Log.e("LocationDebug", "getCurrentLocation failed: ${e.message}", e)
                    error = "Location error: ${e.message}"
                }
            }

        } else {
            android.util.Log.w("LocationDebug", "Location permission denied")
            error = "Location permission denied. Please enter location manually."
        }
    }

    fun requestLocation() {
        android.util.Log.d("LocationDebug", "Requesting location permissions...")
        permissionLauncher.launch(arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ))
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Add New Listing", 
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Share your items with the community",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
            }

            // Scan Receipt Button
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Quick Fill with OCR",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Scan a receipt or price tag to auto-fill details",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = { receiptPicker.launch("image/*") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isScanning && !isSaving,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        if (isScanning) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Scanning...")
                        } else {
                            Text("📸 Scan Receipt / Price Tag")
                        }
                    }
                }
            }

            // Basic Info Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Basic Information", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(12.dp))
                    
                    OutlinedTextField(
                        value = title, 
                        onValueChange = { title = it }, 
                        label = { Text("Title") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )
                    Spacer(Modifier.height(12.dp))
                    
                    OutlinedTextField(
                        value = description, 
                        onValueChange = { description = it }, 
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        minLines = 3
                    )
                }
            }

            // Category & Pricing Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Category & Pricing", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(12.dp))
                    
                    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                        OutlinedTextField(
                            value = category,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Category") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier.fillMaxWidth().menuAnchor(),
                            shape = RoundedCornerShape(8.dp)
                        )
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            categories.forEach { item ->
                                DropdownMenuItem(text = { Text(item) }, onClick = { category = item; expanded = false })
                            }
                        }
                    }
                    
                    Spacer(Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Mark as donation", style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.width(8.dp))
                        Switch(checked = isDonation, onCheckedChange = { isDonation = it })
                    }
                    
                    if (!isDonation) {
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = priceText,
                            onValueChange = { priceText = it.filter { ch -> ch.isDigit() || ch == '.' } },
                            label = { Text("Price ($)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }
            }

            // Location Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Location", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(12.dp))
                    
                    OutlinedTextField(
                        value = locationName,
                        onValueChange = { locationName = it },
                        label = { Text("Enter your city or suburb") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )
                    Spacer(Modifier.height(12.dp))
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Button(
                            onClick = { requestLocation() },
                            shape = RoundedCornerShape(8.dp)
                        ) { 
                            Text("Use my location") 
                        }
                        Spacer(Modifier.width(12.dp))
                        if (latitude != null && longitude != null) {
                            Text(
                                "✓ Location found", 
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // Image Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Image", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(12.dp))
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Button(
                            onClick = { imagePicker.launch("image/*") },
                            shape = RoundedCornerShape(8.dp)
                        ) { 
                            Text("Pick image") 
                        }
                        Spacer(Modifier.width(12.dp))
                        if (imageUri != null) {
                            Text(
                                "✓ Image selected", 
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    
                    if (imageUri != null) {
                        Spacer(Modifier.height(12.dp))
                        AsyncImage(
                            model = imageUri, 
                            contentDescription = null, 
                            modifier = Modifier
                                .size(120.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                    }
                }
            }

            // Submit Button
            Button(
                onClick = {
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
                        imageUrl = null,
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
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isSaving) { 
                    CircularProgressIndicator(modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Creating...")
                } else { 
                    Text("Create Listing") 
                }
            }

            if (error != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        error!!, 
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }
    }
}


