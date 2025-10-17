package com.example.cstore

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cstore.ui.auth.AuthUiState
import com.example.cstore.ui.auth.AuthViewModel
import com.example.cstore.ui.auth.ForgotPasswordScreen
import com.example.cstore.ui.auth.LoginScreen
import com.example.cstore.ui.auth.ProfileScreen
import com.example.cstore.ui.auth.ProfileViewModel
import com.example.cstore.ui.auth.SignUpScreen
import com.example.cstore.ui.home.HomeScreen
import com.example.cstore.ui.home.HomeViewModel
import com.example.cstore.ui.listing.CreateListingScreen
import com.example.cstore.ui.listing.ItemDetailScreen
import com.example.cstore.ui.listing.ItemDetailViewModel
import com.example.cstore.ui.map.MapScreen
import com.example.cstore.ui.navigation.BottomNavBar
import com.example.cstore.ui.search.SearchScreen
import com.example.cstore.ui.chat.ChatListScreen
import com.example.cstore.ui.chat.ChatScreen
import com.example.cstore.ui.theme.CstoreTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.cstore.data.events.EventRepository
import com.example.cstore.workers.EventRefreshWorker
import com.google.android.gms.auth.api.signin.GoogleSignIn
import java.util.concurrent.TimeUnit
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Schedule periodic event refresh worker (runs every 6 hours)
        scheduleEventRefresh()
        
        setContent { CstoreTheme { App() } }
    }
    
    private fun scheduleEventRefresh() {
        val workRequest = PeriodicWorkRequestBuilder<EventRefreshWorker>(
            repeatInterval = 6,
            repeatIntervalTimeUnit = TimeUnit.HOURS
        ).build()
        
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "event_refresh",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }
}

@Composable
fun App() {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = viewModel()
    val homeViewModel: HomeViewModel = viewModel()
    val profileViewModel: ProfileViewModel = viewModel()

    val state by authViewModel.uiState.collectAsState()

    val context = LocalContext.current
    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestIdToken(context.getString(R.string.default_web_client_id))
        .requestEmail()
        .build()
    val googleSignInClient = GoogleSignIn.getClient(context, gso)

    // Auto-load bundled events CSV once
    LaunchedEffect(Unit) {
        EventRepository.autoLoadBundled(context)
    }

    val googleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task: Task<com.google.android.gms.auth.api.signin.GoogleSignInAccount> =
            GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val idToken = account.idToken
            if (idToken != null) {
                authViewModel.signInWithGoogle(idToken)
            } else {
                authViewModel.reportError("Google sign-in returned null idToken")
            }
        } catch (e: ApiException) {
            authViewModel.reportError("Google sign-in failed: ${e.statusCode}")
        } catch (e: Exception) {
            authViewModel.reportError(e.message ?: "Google sign-in failed")
        }
    }

    NavHost(navController = navController, startDestination = "login") {

        composable("login") {
            val context = LocalContext.current

            LoginScreen(
                state = state,
                onSignIn = { email, password, rememberMe ->
                    authViewModel.signIn(email, password, rememberMe, context)
                },
                onSignUpNavigate = { navController.navigate("signup") },
                onGoogleClick = { googleLauncher.launch(googleSignInClient.signInIntent) },
                onForgotPassword = { navController.navigate("forgot_password") },
                onSuccess = {
                    authViewModel.currentUserUid()?.let { authViewModel.loadUserProfile(it) }
                    navController.navigate("home") { popUpTo("login") { inclusive = true } }
                }
            )
        }

        composable("forgot_password") {
            ForgotPasswordScreen(
                viewModel = authViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable("signup") {
            SignUpScreen(
                state = state,
                onSignUp = { email, password -> authViewModel.signUp(email, password) },
                onLoginNavigate = { navController.popBackStack() },
                onSuccess = {
                    authViewModel.currentUserUid()?.let { authViewModel.loadUserProfile(it) }
                    navController.navigate("home") { popUpTo("login") { inclusive = true } }
                }
            )
        }

        composable("home") {
            Scaffold(bottomBar = { BottomNavBar(navController) }) { innerPadding ->
                HomeScreen(
                    viewModel = homeViewModel,
                    onCreateListing = { navController.navigate("create_listing") },
                    onProfile = { navController.navigate("profile") },
                    onItemClick = { listingId -> navController.navigate("item_detail/$listingId") },
                    onChats = { navController.navigate("chat_list") },
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }

        composable("search") {
            Scaffold(bottomBar = { BottomNavBar(navController) }) { innerPadding ->
                SearchScreen(
                    onItemClick = { listingId -> navController.navigate("item_detail/$listingId") },
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }

        composable("create_listing") {
            Scaffold(bottomBar = { BottomNavBar(navController) }) { innerPadding ->
                CreateListingScreen(
                    viewModel = authViewModel,
                    onSaved = { navController.navigate("home") },
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }

        composable("map") {
            Scaffold(bottomBar = { BottomNavBar(navController) }) { innerPadding ->
                MapScreen(
                    onItemClick = { listingId -> navController.navigate("item_detail/$listingId") },
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }

        composable("profile") {
            Scaffold(bottomBar = { BottomNavBar(navController) }) { innerPadding ->
                ProfileScreen(
                    authViewModel = authViewModel,
                    profileViewModel = profileViewModel,
                    onSignOut = {
                        authViewModel.signOut()
                        navController.navigate("login") { popUpTo("home") { inclusive = true } }
                    },
                    onCreateListing = { navController.navigate("create_listing") },
                    onItemClick = { listingId -> navController.navigate("item_detail/$listingId") },
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }

        composable(
            "item_detail/{listingId}",
            arguments = listOf(navArgument("listingId") { type = NavType.StringType })
        ) { backStackEntry ->
            val listingId = backStackEntry.arguments?.getString("listingId") ?: ""
            val itemDetailViewModel: ItemDetailViewModel = viewModel()

            ItemDetailScreen(
                listingId = listingId,
                viewModel = itemDetailViewModel,
                onBack = { navController.popBackStack() },
                onRequestItem = { /* TODO */ },
                onChatWithOwner = { ownerId ->
                    // Navigate to chat with listing context
                    navController.navigate("chat/$ownerId?listingId=$listingId")
                },
                onShareItem = { /* TODO */ }
            )
        }

        composable("chat_list") {
            ChatListScreen(
                authViewModel = authViewModel,
                onChatSelected = { otherUserId, otherEmail ->
                    navController.navigate("chat/$otherUserId")
                }
            )
        }

        composable(
            "chat/{otherUserId}?listingId={listingId}",
            arguments = listOf(
                navArgument("otherUserId") { type = NavType.StringType },
                navArgument("listingId") { 
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val otherUserId = backStackEntry.arguments?.getString("otherUserId") ?: ""
            val listingId = backStackEntry.arguments?.getString("listingId")
            
            ChatScreen(
                authViewModel = authViewModel,
                otherUserId = otherUserId,
                listingId = listingId
            )
        }
    }
}