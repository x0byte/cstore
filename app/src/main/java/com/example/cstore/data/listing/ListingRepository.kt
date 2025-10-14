package com.example.cstore.data.listing

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class ListingRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val listings = firestore.collection("listings")

    suspend fun uploadListing(listing: Listing): Result<Unit> {
        return try {
            val docRef = if (listing.id.isBlank()) listings.document() else listings.document(listing.id)
            val data = mapOf(
                "id" to docRef.id,
                "title" to listing.title,
                "description" to listing.description,
                "category" to listing.category,
                "price" to listing.price,
                "isDonation" to listing.isDonation,
                "localImageUri" to listing.localImageUri,
                "userId" to listing.userId,
                "locationName" to listing.locationName,
                "latitude" to listing.latitude,
                "longitude" to listing.longitude,
                "createdAt" to listing.createdAt,
                "availableOn" to listing.availableOn
            )
            docRef.set(data).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAllListings(): Result<List<Listing>> {
        return try {
            val snapshot = listings.get().await()
            val list = snapshot.toObjects(Listing::class.java)
            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getListingById(listingId: String): Result<Listing?> {
        return try {
            val snapshot = listings.document(listingId).get().await()
            if (snapshot.exists()) {
                val listing = snapshot.toObject(Listing::class.java)
                Result.success(listing)
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getListingsByUserId(userId: String): Result<List<Listing>> {
        return try {
            val snapshot = listings.whereEqualTo("userId", userId).get().await()
            val list = snapshot.toObjects(Listing::class.java)
            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}


