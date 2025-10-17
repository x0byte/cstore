package com.example.cstore.ui.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp

@Composable
fun LoginScreen(
    state: AuthUiState,
    onSignIn: (String, String) -> Unit,
    onSignUpNavigate: () -> Unit,
    onGoogleClick: () -> Unit,
    onSuccess: () -> Unit,
    onForgotPassword: () -> Unit = {}
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }

    LaunchedEffect(state) {
        if (state is AuthUiState.Success) onSuccess()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Welcome back", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(20.dp))
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            singleLine = true
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            singleLine = true,
            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation()
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = { onSignIn(email, password) }, enabled = state !is AuthUiState.Loading) {
            if (state is AuthUiState.Loading) {
                CircularProgressIndicator(modifier = Modifier.width(18.dp).height(18.dp))
                Spacer(Modifier.width(8.dp))
            }
            Text("Sign In")
        }

        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onForgotPassword) {
            Text("Forgot Password?")
        }



        if (state is AuthUiState.Error) {
            Spacer(Modifier.height(8.dp))
            Text(state.message, color = MaterialTheme.colorScheme.error)
        }
        Spacer(Modifier.height(16.dp))
        Divider()
        Spacer(Modifier.height(16.dp))
        GoogleButton(onClick = onGoogleClick)
        Spacer(Modifier.height(12.dp))
        TextButton(onClick = onSignUpNavigate) { Text("Don't have an account? Sign up") }
    }
}

@Composable
private fun GoogleButton(onClick: () -> Unit) {
    Button(onClick = onClick) {
        // Placeholder for Google logo to keep code dependency-free
        Image(
            painter = painterResource(android.R.drawable.ic_dialog_email),
            contentDescription = "Google",
            modifier = Modifier.height(18.dp).width(18.dp),
            contentScale = ContentScale.Fit
        )
        Spacer(Modifier.width(8.dp))
        Text("Sign in with Google")
    }
}


