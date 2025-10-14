package com.example.cstore.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.graphics.vector.ImageVector

sealed class BottomNavItem(
    val route: String,
    val icon: ImageVector,
    val label: String
) {
    object Home : BottomNavItem("home", Icons.Default.Home, "Home")
    object Add : BottomNavItem("create_listing", Icons.Default.Add, "Add")
    object Map : BottomNavItem("map", Icons.Default.Map, "Map")
    object Profile : BottomNavItem("profile", Icons.Default.Person, "Profile")
}
