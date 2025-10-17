package com.example.cstore.ui.components

import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import androidx.compose.ui.res.painterResource
import com.example.cstore.data.listing.Listing

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ListingCard(
    listing: Listing,
    badges: List<String> = emptyList(),
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = modifier.then(
            if (onClick != null) {
                Modifier.clickable { onClick() }
            } else {
                Modifier
            }
        )
    ) {
        // Try to load remote imageUrl, fallback to localImageUri, then placeholder
        AsyncImage(
            model = listing.imageUrl?.takeIf { it.isNotBlank() } ?: listing.localImageUri,
            contentDescription = "Item image",
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
            contentScale = ContentScale.Crop,
            error = painterResource(android.R.drawable.ic_menu_gallery),
            placeholder = painterResource(android.R.drawable.ic_menu_gallery)
        )
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = listing.title.ifBlank { "Untitled" }, 
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "${listing.category.ifBlank { "Other" }} • ${listing.locationName.ifBlank { "Unknown" }}", 
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(Modifier.height(6.dp))
            val priceText = if (listing.isDonation) "Free" else listing.price?.let { "$${it}" } ?: "Price TBD"
            Text(
                text = priceText, 
                style = MaterialTheme.typography.labelLarge, 
                color = MaterialTheme.colorScheme.primary
            )
            if (badges.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    badges.forEach { badge ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.padding(0.dp)
                        ) {
                            Text(
                                text = badge,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}


