package com.example.cstore

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.cstore.ui.theme.CstoreTheme
import com.example.cstore.ui.auth.AuthUiState
import com.example.cstore.ui.auth.AuthViewModel
import com.example.cstore.ui.auth.LoginScreen
import com.example.cstore.ui.auth.SignUpScreen
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CstoreTheme {
                App()
            }
        }
    }
}

@Composable
fun App() {
    val navController = rememberNavController()
    val viewModel = AuthViewModel()
    val state by viewModel.uiState.collectAsState()

    val context = LocalContext.current
    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestIdToken(context.getString(R.string.default_web_client_id))
        .requestEmail()
        .build()
    val googleSignInClient = GoogleSignIn.getClient(context, gso)

    val googleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task: Task<com.google.android.gms.auth.api.signin.GoogleSignInAccount> =
            GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val idToken = account.idToken
            if (idToken != null) {
                viewModel.signInWithGoogle(idToken)
            } else {
                viewModel.reportError("Google sign-in returned null idToken")
            }
        } catch (e: ApiException) {
            viewModel.reportError("Google sign-in failed: ${e.statusCode}")
        } catch (e: Exception) {
            viewModel.reportError(e.message ?: "Google sign-in failed")
        }
    }

    NavHost(navController = navController, startDestination = "login") {
        composable("login") {
            LoginScreen(
                state = state,
                onSignIn = { email, password -> viewModel.signIn(email, password) },
                onSignUpNavigate = { navController.navigate("signup") },
                onGoogleClick = { googleLauncher.launch(googleSignInClient.signInIntent) },
                onSuccess = { navController.navigate("home") { popUpTo("login") { inclusive = true } } }
            )
        }
        composable("signup") {
            SignUpScreen(
                state = state,
                onSignUp = { email, password -> viewModel.signUp(email, password) },
                onLoginNavigate = { navController.popBackStack() },
                onSuccess = { navController.navigate("home") { popUpTo("login") { inclusive = true } } }
            )
        }
        composable("home") {
            Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                Home(
                    email = viewModel.currentUserEmail() ?: "",
                    onSignOut = {
                        viewModel.signOut()
                        navController.navigate("login") { popUpTo("home") { inclusive = true } }
                    },
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
    }
}

@Composable
fun Home(email: String, onSignOut: () -> Unit, modifier: Modifier = Modifier) {
    Text(text = "Welcome, $email", modifier = modifier)
    Button(onClick = onSignOut) { Text("Sign out") }
}