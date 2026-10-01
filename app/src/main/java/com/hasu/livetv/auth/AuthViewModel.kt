package com.hasu.livetv.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.hasu.livetv.data.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AuthViewModel(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val userRepository: UserRepository = UserRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        AuthUiState(
            isLoggedIn = auth.currentUser != null
        )
    )

    val uiState: StateFlow<AuthUiState> =
        _uiState.asStateFlow()

    fun login(
        email: String,
        password: String
    ) {
        if (email.isBlank()) {
            _uiState.value = AuthUiState(
                error = "Email is required"
            )
            return
        }

        if (password.isBlank()) {
            _uiState.value = AuthUiState(
                error = "Password is required"
            )
            return
        }

        viewModelScope.launch {

            _uiState.value = AuthUiState(
                isLoading = true
            )

            try {

                auth.signInWithEmailAndPassword(
                    email.trim(),
                    password
                ).await()

                userRepository.updateLastLogin()

                _uiState.value = AuthUiState(
                    isLoggedIn = true,
                    message = "Login successful"
                )

            } catch (e: Exception) {

                _uiState.value = AuthUiState(
                    error = firebaseError(e)
                )
            }
        }
    }

    fun signup(
        name: String,
        email: String,
        password: String
    ) {

        if (name.isBlank()) {
            _uiState.value = AuthUiState(
                error = "Name is required"
            )
            return
        }

        if (email.isBlank()) {
            _uiState.value = AuthUiState(
                error = "Email is required"
            )
            return
        }

        if (password.length < 6) {
            _uiState.value = AuthUiState(
                error = "Password must be at least 6 characters"
            )
            return
        }

        viewModelScope.launch {

            _uiState.value = AuthUiState(
                isLoading = true
            )

            try {

                auth.createUserWithEmailAndPassword(
                    email.trim(),
                    password
                ).await()

                userRepository.createUserProfile(
                    name = name.trim(),
                    email = email.trim()
                )

                _uiState.value = AuthUiState(
                    isLoggedIn = true,
                    message = "Account created successfully"
                )

            } catch (e: Exception) {

                _uiState.value = AuthUiState(
                    error = firebaseError(e)
                )
            }
        }
    }

    fun logout() {
        auth.signOut()

        _uiState.value = AuthUiState(
            isLoggedIn = false
        )
    }

    private fun firebaseError(
        exception: Exception
    ): String {

        val message = exception.message ?: ""

        return when {

            message.contains(
                "email address is badly formatted",
                ignoreCase = true
            ) ->
                "Invalid email address"

            message.contains(
                "password is invalid",
                ignoreCase = true
            ) ->
                "Wrong password"

            message.contains(
                "no user record",
                ignoreCase = true
            ) ->
                "Account not found"

            message.contains(
                "email address is already in use",
                ignoreCase = true
            ) ->
                "Email is already registered"

            message.contains(
                "network",
                ignoreCase = true
            ) ->
                "Network error. Check your internet."

            else ->
                message.ifBlank {
                    "Authentication failed"
                }
        }
    }
}