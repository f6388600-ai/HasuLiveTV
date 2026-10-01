package com.hasu.livetv.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/* =========================================================
   LOGIN SCREEN
   ========================================================= */

@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    onSignup: () -> Unit,
    onSuccess: () -> Unit
) {
    var email by remember {
        mutableStateOf("")
    }

    var password by remember {
        mutableStateOf("")
    }

    var passwordVisible by remember {
        mutableStateOf(false)
    }

    val state by viewModel.uiState.collectAsStateCompat()

    LaunchedEffect(state.isLoggedIn) {
        if (state.isLoggedIn) {
            onSuccess()
        }
    }

    AuthBackground {
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

            AuthLogo()

            Spacer(
                modifier = Modifier.height(18.dp)
            )

            Text(
                text = "Welcome Back",
                fontSize = 30.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )

            Spacer(
                modifier = Modifier.height(6.dp)
            )

            Text(
                text = "Login to continue to Hasu Live TV",
                color = Color.White.copy(alpha = 0.65f)
            )

            Spacer(
                modifier = Modifier.height(28.dp)
            )

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(
                        RoundedCornerShape(26.dp)
                    ),
                color = Color.White.copy(alpha = 0.07f),
                tonalElevation = 0.dp
            ) {

                Column(
                    modifier = Modifier.padding(20.dp)
                ) {

                    OutlinedTextField(
                        value = email,
                        onValueChange = {
                            email = it
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = {
                            Text("Email")
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Email,
                                contentDescription = null
                            )
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email
                        )
                    )

                    Spacer(
                        modifier = Modifier.height(14.dp)
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = {
                            password = it
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = {
                            Text("Password")
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null
                            )
                        },
                        trailingIcon = {
                            IconButton(
                                onClick = {
                                    passwordVisible =
                                        !passwordVisible
                                }
                            ) {
                                Icon(
                                    imageVector =
                                        if (passwordVisible)
                                            Icons.Default.VisibilityOff
                                        else
                                            Icons.Default.Visibility,
                                    contentDescription = null
                                )
                            }
                        },
                        visualTransformation =
                            if (passwordVisible)
                                VisualTransformation.None
                            else
                                PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password
                        )
                    )

                    if (state.error != null) {

                        Spacer(
                            modifier = Modifier.height(12.dp)
                        )

                        AuthMessage(
                            text = state.error!!,
                            error = true
                        )
                    }

                    if (state.message != null) {

                        Spacer(
                            modifier = Modifier.height(12.dp)
                        )

                        AuthMessage(
                            text = state.message!!,
                            error = false
                        )
                    }

                    Spacer(
                        modifier = Modifier.height(20.dp)
                    )

                    Button(
                        onClick = {
                            viewModel.login(
                                email = email,
                                password = password
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        enabled = !state.isLoading,
                        shape = RoundedCornerShape(16.dp)
                    ) {

                        if (state.isLoading) {

                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                strokeWidth = 2.dp
                            )

                        } else {

                            Text(
                                text = "Login",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(
                        modifier = Modifier.height(12.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement =
                            Arrangement.Center,
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        Text(
                            text = "Don't have an account?",
                            color = Color.White.copy(
                                alpha = 0.65f
                            )
                        )

                        TextButton(
                            onClick = onSignup
                        ) {
                            Text(
                                text = "Sign Up",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(
                modifier = Modifier.height(22.dp)
            )

            Text(
                text = "HASU LIVE TV",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.45f)
            )
        }
    }
}


/* =========================================================
   SIGNUP SCREEN
   ========================================================= */

@Composable
fun SignupScreen(
    viewModel: AuthViewModel,
    onLogin: () -> Unit,
    onSuccess: () -> Unit
) {
    var name by remember {
        mutableStateOf("")
    }

    var email by remember {
        mutableStateOf("")
    }

    var password by remember {
        mutableStateOf("")
    }

    var confirmPassword by remember {
        mutableStateOf("")
    }

    var passwordVisible by remember {
        mutableStateOf(false)
    }

    var confirmPasswordVisible by remember {
        mutableStateOf(false)
    }

    var localError by remember {
        mutableStateOf<String?>(null)
    }

    val state by viewModel.uiState.collectAsStateCompat()

    LaunchedEffect(state.isLoggedIn) {
        if (state.isLoggedIn) {
            onSuccess()
        }
    }

    AuthBackground {

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

            AuthLogo()

            Spacer(
                modifier = Modifier.height(18.dp)
            )

            Text(
                text = "Create Account",
                fontSize = 30.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )

            Spacer(
                modifier = Modifier.height(6.dp)
            )

            Text(
                text = "Create your Hasu Live TV account",
                color = Color.White.copy(alpha = 0.65f)
            )

            Spacer(
                modifier = Modifier.height(28.dp)
            )

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(
                        RoundedCornerShape(26.dp)
                    ),
                color = Color.White.copy(alpha = 0.07f),
                tonalElevation = 0.dp
            ) {

                Column(
                    modifier = Modifier.padding(20.dp)
                ) {

                    OutlinedTextField(
                        value = name,
                        onValueChange = {
                            name = it
                            localError = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = {
                            Text("Name")
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null
                            )
                        }
                    )

                    Spacer(
                        modifier = Modifier.height(14.dp)
                    )

                    OutlinedTextField(
                        value = email,
                        onValueChange = {
                            email = it
                            localError = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = {
                            Text("Email")
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Email,
                                contentDescription = null
                            )
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email
                        )
                    )

                    Spacer(
                        modifier = Modifier.height(14.dp)
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            localError = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = {
                            Text("Password")
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null
                            )
                        },
                        trailingIcon = {
                            IconButton(
                                onClick = {
                                    passwordVisible =
                                        !passwordVisible
                                }
                            ) {
                                Icon(
                                    imageVector =
                                        if (passwordVisible)
                                            Icons.Default.VisibilityOff
                                        else
                                            Icons.Default.Visibility,
                                    contentDescription = null
                                )
                            }
                        },
                        visualTransformation =
                            if (passwordVisible)
                                VisualTransformation.None
                            else
                                PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password
                        )
                    )

                    Spacer(
                        modifier = Modifier.height(14.dp)
                    )

                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = {
                            confirmPassword = it
                            localError = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = {
                            Text("Confirm Password")
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null
                            )
                        },
                        trailingIcon = {
                            IconButton(
                                onClick = {
                                    confirmPasswordVisible =
                                        !confirmPasswordVisible
                                }
                            ) {
                                Icon(
                                    imageVector =
                                        if (confirmPasswordVisible)
                                            Icons.Default.VisibilityOff
                                        else
                                            Icons.Default.Visibility,
                                    contentDescription = null
                                )
                            }
                        },
                        visualTransformation =
                            if (confirmPasswordVisible)
                                VisualTransformation.None
                            else
                                PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password
                        )
                    )

                    val error =
                        localError ?: state.error

                    if (error != null) {

                        Spacer(
                            modifier = Modifier.height(12.dp)
                        )

                        AuthMessage(
                            text = error,
                            error = true
                        )
                    }

                    if (state.message != null) {

                        Spacer(
                            modifier = Modifier.height(12.dp)
                        )

                        AuthMessage(
                            text = state.message!!,
                            error = false
                        )
                    }

                    Spacer(
                        modifier = Modifier.height(20.dp)
                    )

                    Button(
                        onClick = {

                            if (name.isBlank()) {
                                localError =
                                    "Name is required"
                                return@Button
                            }

                            if (email.isBlank()) {
                                localError =
                                    "Email is required"
                                return@Button
                            }

                            if (password.length < 6) {
                                localError =
                                    "Password must be at least 6 characters"
                                return@Button
                            }

                            if (password != confirmPassword) {
                                localError =
                                    "Passwords do not match"
                                return@Button
                            }

                            localError = null

                            viewModel.signup(
                                name = name,
                                email = email,
                                password = password
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        enabled = !state.isLoading,
                        shape = RoundedCornerShape(16.dp)
                    ) {

                        if (state.isLoading) {

                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                strokeWidth = 2.dp
                            )

                        } else {

                            Text(
                                text = "Create Account",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(
                        modifier = Modifier.height(12.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement =
                            Arrangement.Center,
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        Text(
                            text = "Already have an account?",
                            color = Color.White.copy(
                                alpha = 0.65f
                            )
                        )

                        TextButton(
                            onClick = onLogin
                        ) {
                            Text(
                                text = "Login",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(
                modifier = Modifier.height(22.dp)
            )

            Text(
                text = "HASU LIVE TV",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.45f)
            )
        }
    }
}


/* =========================================================
   BACKGROUND
   ========================================================= */

@Composable
private fun AuthBackground(
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    listOf(
                        Color(0xFF07080D),
                        Color(0xFF101326),
                        Color(0xFF17102A)
                    )
                )
            )
    ) {
        content()
    }
}


/* =========================================================
   LOGO
   ========================================================= */

@Composable
private fun AuthLogo() {
    Box(
        modifier = Modifier
            .size(82.dp)
            .clip(
                RoundedCornerShape(24.dp)
            )
            .background(
                Brush.linearGradient(
                    listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.secondary
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {

        Text(
            text = "H",
            fontSize = 42.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White
        )
    }
}


/* =========================================================
   MESSAGE
   ========================================================= */

@Composable
private fun AuthMessage(
    text: String,
    error: Boolean
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = if (error) {
            Color(0xFFFF5252).copy(alpha = 0.12f)
        } else {
            Color(0xFF4CAF50).copy(alpha = 0.12f)
        }
    ) {

        Text(
            text = text,
            modifier = Modifier.padding(12.dp),
            color = if (error) {
                Color(0xFFFF8A80)
            } else {
                Color(0xFF81C784)
            },
            fontSize = 13.sp
        )
    }
}


/* =========================================================
   STATE FLOW COLLECTOR
   ========================================================= */

@Composable
private fun AuthViewModel.uiState.collectAsStateCompat():
        androidx.compose.runtime.State<AuthUiState> {

    return this.uiState.collectAsState()
}