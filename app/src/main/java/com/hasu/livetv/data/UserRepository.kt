package com.hasu.livetv.data

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class UserRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    suspend fun createUserProfile(
        name: String,
        email: String,
        photoUrl: String = "",
        role: String = "user"
    ) {
        val user = auth.currentUser
            ?: throw Exception("User is not logged in")

        val data = hashMapOf(
            "uid" to user.uid,
            "name" to name,
            "email" to email,
            "photoUrl" to photoUrl,
            "role" to role,
            "createdAt" to Timestamp.now(),
            "lastLoginAt" to Timestamp.now()
        )

        firestore
            .collection("users")
            .document(user.uid)
            .set(data)
            .await()
    }

    suspend fun updateLastLogin() {
        val user = auth.currentUser ?: return

        firestore
            .collection("users")
            .document(user.uid)
            .update(
                "lastLoginAt",
                Timestamp.now()
            )
            .await()
    }

    suspend fun getUserProfile(): Map<String, Any>? {
        val user = auth.currentUser ?: return null

        return firestore
            .collection("users")
            .document(user.uid)
            .get()
            .await()
            .data
    }

    suspend fun getUserRole(): String {
        return getUserProfile()
            ?.get("role")
            ?.toString()
            ?: "user"
    }
}