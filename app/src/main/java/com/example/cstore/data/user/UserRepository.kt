package com.example.cstore.data.user

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import java.util.Date

data class UserProfile(
    val uid: String = "",
    val email: String = "",
    val createdAt: Date = Date()
)

class UserRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val usersCollection = firestore.collection("users")

    suspend fun createUserProfile(uid: String, email: String): Result<Unit> {
        return try {
            val data = mapOf(
                "uid" to uid,
                "email" to email,
                "createdAt" to Timestamp.now()
            )
            usersCollection.document(uid).set(data, SetOptions.merge()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserProfile(uid: String): Result<UserProfile?> {
        return try {
            val snapshot = usersCollection.document(uid).get().await()
            if (!snapshot.exists()) return Result.success(null)
            val email = snapshot.getString("email") ?: ""
            val ts = snapshot.getTimestamp("createdAt") ?: Timestamp.now()
            val profile = UserProfile(uid = uid, email = email, createdAt = ts.toDate())
            Result.success(profile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}


