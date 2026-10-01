package com.hasu.livetv.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hasu.livetv.data.model.FirebaseUser
import com.hasu.livetv.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class AuthState {

    data object Loading : AuthState()

    data object LoggedOut : AuthState()

    data class LoggedIn(
        val user: FirebaseUser
    ) : AuthState()

    data class Error(
        val message: String
    ) : AuthState()
}

class AuthViewModel(
    private val repository: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _state =
        MutableStateFlow<AuthState>(AuthState.Loading)

    val state: StateFlow<AuthState> =
        _state.asStateFlow()

    init {
        checkSession()
    }

    fun checkSession() {

        viewModelScope.launch {

            if (!repository.isLoggedIn()) {
                _state.value = AuthState.LoggedOut
                return@launch
            }

            repository.getCurrentUser()
                .onSuccess { user ->

                    if (user == null) {
                        _state.value = AuthState.LoggedOut
                    } else {
                        _state.value =
                            AuthState.LoggedIn(user)
                    }
                }
                .onFailure {
                    _state.value =
                        AuthState.Error(
                            it.message ?: "Unable to load account"
                        )
                }
        }
    }

    fun login(
        email: String,
        password: String
    ) {

        if (email.isBlank() || password.isBlank()) {
            _state.value =
                AuthState.Error(
                    "Email and password are required."
                )
            return
        }

        viewModelScope.launch {

            _state.value = AuthState.Loading

            repository
                .login(email, password)
                .onSuccess {
                    _state.value =
                        AuthState.LoggedIn(it)
                }
                .onFailure {
                    _state.value =
                        AuthState.Error(
                            it.message
                                ?: "Login failed."
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
            _state.value =
                AuthState.Error("Please enter your name.")
            return
        }

        if (email.isBlank()) {
            _state.value =
                AuthState.Error("Please enter your email.")
            return
        }

        if (password.length < 6) {
            _state.value =
                AuthState.Error(
                    "Password must be at least 6 characters."
                )
            return
        }

        viewModelScope.launch {

            _state.value = AuthState.Loading

            repository
                .signup(
                    name = name,
                    email = email,
                    password = password
                )
                .onSuccess {
                    _state.value =
                        AuthState.LoggedIn(it)
                }
                .onFailure {
                    _state.value =
                        AuthState.Error(
                            it.message
                                ?: "Signup failed."
                        )
                }
        }
    }

    fun logout() {
        repository.logout()
        _state.value = AuthState.LoggedOut
    }

    fun clearError() {
        if (_state.value is AuthState.Error) {
            _state.value = AuthState.LoggedOut
        }
    }
}