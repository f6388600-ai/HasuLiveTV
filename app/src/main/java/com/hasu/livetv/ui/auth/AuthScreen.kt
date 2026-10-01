package com.hasu.livetv.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

@Composable
fun AuthScreen(
    viewModel: AuthViewModel,
    onLoggedIn: () -> Unit
) {

    val state by viewModel.state.collectAsState()

    var signupMode by remember {
        mutableStateOf(false)
    }

    var name by remember {
        mutableStateOf("")
    }

    var email by remember {
        mutableStateOf("")
    }

    var password by remember {
        mutableStateOf("")
    }

    LaunchedEffect(state) {
        if (state is AuthState.LoggedIn) {
            onLoggedIn()
        }
    }

    val loading =
        state is AuthState.Loading

    val error =
        (state as? AuthState.Error)?.message

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(
                rememberScrollState()
            )
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        Text(
            text = "Hasu Live TV",
            style = MaterialTheme.typography.headlineLarge
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        Text(
            text = if (signupMode)
                "Create your account"
            else
                "Welcome back"
        )

        Spacer(
            modifier = Modifier.height(28.dp)
        )

        if (signupMode) {

            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                },
                modifier = Modifier.fillMaxWidth(),
                label = {
                    Text("Name")
                },
                singleLine = true,
                enabled = !loading
            )

            Spacer(
                modifier = Modifier.height(12.dp)
            )
        }

        OutlinedTextField(
            value = email,
            onValueChange = {
                email = it
            },
            modifier = Modifier.fillMaxWidth(),
            label = {
                Text("Email")
            },
            singleLine = true,
            enabled = !loading
        )

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        OutlinedTextField(
            value = password,
            onValueChange = {
                password = it
            },
            modifier = Modifier.fillMaxWidth(),
            label = {
                Text("Password")
            },
            singleLine = true,
            visualTransformation =
                PasswordVisualTransformation(),
            enabled = !loading
        )

        if (error != null) {

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            Text(
                text = error,
                color = MaterialTheme.colorScheme.error
            )
        }

        Spacer(
            modifier = Modifier.height(20.dp)
        )

        Button(
            onClick = {

                if (signupMode) {
                    viewModel.signup(
                        name = name,
                        email = email,
                        password = password
                    )
                } else {
                    viewModel.login(
                        email = email,
                        password = password
                    )
                }

            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !loading
        ) {

            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.height(20.dp)
                )
            } else {
                Text(
                    if (signupMode)
                        "Create Account"
                    else
                        "Login"
                )
            }
        }

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        OutlinedButton(
            onClick = {
                if (!loading) {
                    signupMode = !signupMode
                    viewModel.clearError()
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !loading
        ) {

            Text(
                if (signupMode)
                    "Back to Login"
                else
                    "Create New Account"
            )
        }

        Spacer(
            modifier = Modifier.height(4.dp)
        )

        TextButton(
            onClick = {
                if (!loading) {
                    signupMode = !signupMode
                    viewModel.clearError()
                }
            },
            enabled = !loading
        ) {

            Text(
                if (signupMode)
                    "Already have an account? Login"
                else
                    "Don't have an account? Sign up"
            )
        }
    }
}