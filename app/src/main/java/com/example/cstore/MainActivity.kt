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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.NavType
import com.example.cstore.ui.theme.CstoreTheme
import com.example.cstore.ui.auth.AuthUiState
import com.example.cstore.ui.auth.AuthViewModel
import com.example.cstore.ui.auth.LoginScreen
import com.example.cstore.ui.auth.SignUpScreen
import com.example.cstore.ui.auth.ProfileScreen
import com.example.cstore.ui.auth.ProfileViewModel
import com.example.cstore.ui.listing.CreateListingScreen
import com.example.cstore.ui.home.HomeScreen
import com.example.cstore.ui.home.HomeViewModel
import com.example.cstore.ui.listing.ItemDetailScreen
import com.example.cstore.ui.listing.ItemDetailViewModel
import com.example.cstore.ui.map.MapScreen
import com.example.cstore.ui.navigation.BottomNavBar
import com.example.cstore.ui.chat.ChatScreen // Add chat
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.example.cstore.ui.chat.ChatListScreen
import android.net.Uri


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
    val homeViewModel = remember { HomeViewModel() }
    val profileViewModel = remember { ProfileViewModel() }

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
                onSuccess = {
                    viewModel.currentUserUid()?.let { viewModel.loadUserProfile(it) }
                    navController.navigate("home") { popUpTo("login") { inclusive = true } }
                }
            )
        }
        composable("signup") {
            SignUpScreen(
                state = state,
                onSignUp = { email, password -> viewModel.signUp(email, password) },
                onLoginNavigate = { navController.popBackStack() },
                onSuccess = {
                    viewModel.currentUserUid()?.let { viewModel.loadUserProfile(it) }
                    navController.navigate("home") { popUpTo("login") { inclusive = true } }
                }
            )
        }
        composable("home") {
            Scaffold(
                bottomBar = { BottomNavBar(navController) }
            ) { innerPadding ->
                HomeScreen(
                    viewModel = homeViewModel,
                    onCreateListing = { navController.navigate("create_listing") },
                    onProfile = { navController.navigate("profile") },
                    onItemClick = { listingId -> navController.navigate("item_detail/$listingId") },
                    onChats = { navController.navigate("chats") },
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
        composable("create_listing") {
            Scaffold(
                bottomBar = { BottomNavBar(navController) }
            ) { innerPadding ->
                CreateListingScreen(
                    viewModel = viewModel,
                    onSaved = { navController.navigate("home") },
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
        composable("map") {
            Scaffold(
                bottomBar = { BottomNavBar(navController) }
            ) { innerPadding ->
                MapScreen(
                    onItemClick = { listingId -> navController.navigate("item_detail/$listingId") },
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
        composable("profile") {
            Scaffold(
                bottomBar = { BottomNavBar(navController) }
            ) { innerPadding ->
                ProfileScreen(
                    authViewModel = viewModel,
                    profileViewModel = profileViewModel,
                    onSignOut = {
                        viewModel.signOut()
                        navController.navigate("login") { popUpTo("home") { inclusive = true } }
                    },
                    onCreateListing = { navController.navigate("create_listing") },
                    onItemClick = { listingId -> navController.navigate("item_detail/$listingId") },
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
        //composable("chat") {
        //    Scaffold(bottomBar = { BottomNavBar(navController) }) { innerPadding ->
        //       ChatScreen(modifier = Modifier.padding(innerPadding))
        //}

        composable(
            "chat/{otherUserId}/{otherEmail}",
            arguments = listOf(navArgument("otherUserId") { type = NavType.StringType },
                navArgument("otherEmail")  { type = NavType.StringType })
        ) { backStackEntry ->
            val otherUserId = backStackEntry.arguments?.getString("otherUserId") ?: return@composable
            val otherEmail  = backStackEntry.arguments?.getString("otherEmail")  ?: ""
            ChatScreen(
                authViewModel = viewModel,  // reuse your AuthViewModel
                otherUserId = otherUserId
            )
        }

        composable(
            "chat/{otherUserId}",
            arguments = listOf(
                navArgument("otherUserId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val otherUserId = backStackEntry.arguments?.getString("otherUserId") ?: return@composable
            ChatScreen(
                authViewModel = viewModel,
                otherUserId = otherUserId
            )
        }

        composable("chats") {
                ChatListScreen(
                    onChatSelected = { otherUserId, otherEmail ->
                        val id   = Uri.encode(otherUserId)
                        val mail = Uri.encode(otherEmail)
                        navController.navigate("chat/$id/$mail")
                    }
                )
        }




        composable(
            "item_detail/{listingId}",
            arguments = listOf(navArgument("listingId") { type = NavType.StringType })
        ) { backStackEntry ->
            val listingId = backStackEntry.arguments?.getString("listingId") ?: ""
            val itemDetailViewModel = remember { ItemDetailViewModel() }
            
            ItemDetailScreen(
                listingId = listingId,
                viewModel = itemDetailViewModel,
                onBack = { navController.popBackStack() },
                onRequestItem = { /* Handle request item */ },
                onChatWithOwner = { ownerId ->
                    navController.navigate("chat/$ownerId")
                },
                onShareItem = { /* Handle share */ }
            )
        }
    }
}

// Home removed; navigating directly to Profile per requirements