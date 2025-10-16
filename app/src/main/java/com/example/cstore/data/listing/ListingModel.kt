package com.example.cstore.data.listing

data class Listing(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val category: String = "",
    val price: Double? = null,
    val isDonation: Boolean = false,
    val localImageUri: String? = null,
    val imageUrl: String? = null,
    val userId: String = "",
    val locationName: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val availableOn: Long? = null
)


