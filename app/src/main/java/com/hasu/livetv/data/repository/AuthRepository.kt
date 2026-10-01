package com.hasu.livetv.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser as FirebaseAuthUser
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.hasu.livetv.data.model.FirebaseUser
import kotlinx.coroutines.tasks.await

class AuthRepository {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    private val usersCollection = firestore.collection("users")

    fun currentUser(): FirebaseAuthUser? {
        return auth.currentUser
    }

    fun isLoggedIn(): Boolean {
        return auth.currentUser != null
    }

    suspend fun signup(
        name: String,
        email: String,
        password: String
    ): Result<FirebaseUser> {
        return try {
            val cleanName = name.trim()
            val cleanEmail = email.trim().lowercase()

            require(cleanName.isNotEmpty()) {
                "Name is required"
            }

            require(cleanEmail.isNotEmpty()) {
                "Email is required"
            }

            require(password.length >= 6) {
                "Password must be at least 6 characters"
            }

            val authResult = auth
                .createUserWithEmailAndPassword(
                    cleanEmail,
                    password
                )
                .await()

            val user = authResult.user
                ?: throw Exception("Could not create account")

            val now = System.currentTimeMillis()

            val userData = hashMapOf(
                "uid" to user.uid,
                "name" to cleanName,
                "email" to cleanEmail,
                "photoUrl" to "",
                "role" to "user",
                "createdAt" to now,
                "lastLoginAt" to now
            )

            usersCollection
                .document(user.uid)
                .set(userData)
                .await()

            Result.success(
                FirebaseUser(
                    uid = user.uid,
                    name = cleanName,
                    email = cleanEmail,
                    photoUrl = "",
                    role = "user",
                    createdAt = now,
                    lastLoginAt = now
                )
            )

        } catch (e: Exception) {
            Result.failure(
                Exception(
                    readableError(e)
                )
            )
        }
    }

    suspend fun login(
        email: String,
        password: String
    ): Result<FirebaseUser> {
        return try {
            val cleanEmail = email.trim().lowercase()

            require(cleanEmail.isNotEmpty()) {
                "Email is required"
            }

            require(password.isNotEmpty()) {
                "Password is required"
            }

            val authResult = auth
                .signInWithEmailAndPassword(
                    cleanEmail,
                    password
                )
                .await()

            val user = authResult.user
                ?: throw Exception("Login failed")

            val userDocument = usersCollection
                .document(user.uid)
                .get()
                .await()

            val now = System.currentTimeMillis()

            if (!userDocument.exists()) {

                val newUser = hashMapOf(
                    "uid" to user.uid,
                    "name" to (user.displayName ?: ""),
                    "email" to (user.email ?: cleanEmail),
                    "photoUrl" to (user.photoUrl?.toString() ?: ""),
                    "role" to "user",
                    "createdAt" to now,
                    "lastLoginAt" to now
                )

                usersCollection
                    .document(user.uid)
                    .set(newUser)
                    .await()

                return Result.success(
                    FirebaseUser(
                        uid = user.uid,
                        name = user.displayName ?: "",
                        email = user.email ?: cleanEmail,
                        photoUrl = user.photoUrl?.toString() ?: "",
                        role = "user",
                        createdAt = now,
                        lastLoginAt = now
                    )
                )
            }

            usersCollection
                .document(user.uid)
                .update(
                    "lastLoginAt",
                    FieldValue.serverTimestamp()
                )
                .await()

            val data = userDocument.data ?: emptyMap()

            Result.success(
                FirebaseUser(
                    uid = user.uid,
                    name = data["name"] as? String ?: "",
                    email = data["email"] as? String ?: cleanEmail,
                    photoUrl = data["photoUrl"] as? String ?: "",
                    role = data["role"] as? String ?: "user",
                    createdAt = (data["createdAt"] as? Number)?.toLong() ?: 0L,
                    lastLoginAt = now
                )
            )

        } catch (e: Exception) {
            Result.failure(
                Exception(
                    readableError(e)
                )
            )
        }
    }

    suspend fun getCurrentUser(): Result<FirebaseUser?> {
        return try {

            val firebaseUser = auth.currentUser
                ?: return Result.success(null)

            val snapshot = usersCollection
                .document(firebaseUser.uid)
                .get()
                .await()

            if (!snapshot.exists()) {
                return Result.success(
                    FirebaseUser(
                        uid = firebaseUser.uid,
                        name = firebaseUser.displayName ?: "",
                        email = firebaseUser.email ?: "",
                        photoUrl = firebaseUser.photoUrl?.toString() ?: "",
                        role = "user"
                    )
                )
            }

            val data = snapshot.data ?: emptyMap()

            Result.success(
                FirebaseUser(
                    uid = firebaseUser.uid,
                    name = data["name"] as? String ?: "",
                    email = data["email"] as? String
                        ?: firebaseUser.email.orEmpty(),
                    photoUrl = data["photoUrl"] as? String ?: "",
                    role = data["role"] as? String ?: "user",
                    createdAt = (data["createdAt"] as? Number)?.toLong() ?: 0L,
                    lastLoginAt = (data["lastLoginAt"] as? Number)?.toLong() ?: 0L
                )
            )

        } catch (e: Exception) {
            Result.failure(
                Exception(
                    readableError(e)
                )
            )
        }
    }

    fun logout() {
        auth.signOut()
    }

    private fun readableError(exception: Exception): String {

        val message = exception.message.orEmpty()

        return when {
            message.contains("already in use", true) ->
                "This email is already registered."

            message.contains("badly formatted", true) ->
                "Please enter a valid email address."

            message.contains("password is invalid", true) ->
                "Incorrect password."

            message.contains("no user record", true) ->
                "No account found with this email."

            message.contains("network", true) ->
                "Network error. Please check your internet connection."

            message.contains("too many requests", true) ->
                "Too many attempts. Please try again later."

            message.contains("requires-recent-login", true) ->
                "Please login again and retry."

            else ->
                message.ifBlank {
                    "Something went wrong. Please try again."
                }
        }
    }
}