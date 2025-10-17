package com.example.cstore.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.graphics.vector.ImageVector

sealed class BottomNavItem(
    val route: String,
    val icon: ImageVector,
    val label: String
) {
    object Home : BottomNavItem("home", Icons.Default.Home, "Home")
    //object Chat : BottomNavItem("chats", Icons.Default.MailOutline, "Chats")
    object Add : BottomNavItem("create_listing", Icons.Default.Add, "Add")
    object Map : BottomNavItem("map", Icons.Default.LocationOn, "Map")
    object Profile : BottomNavItem("profile", Icons.Default.Person, "Profile")
}
