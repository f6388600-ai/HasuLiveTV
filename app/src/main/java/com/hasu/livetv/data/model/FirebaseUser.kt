package com.hasu.livetv.data.model

data class FirebaseUser(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val photoUrl: String = "",
    val role: String = "user",
    val createdAt: Long = 0L,
    val lastLoginAt: Long = 0L
)