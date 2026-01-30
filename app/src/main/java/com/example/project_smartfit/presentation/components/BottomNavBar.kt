package com.example.project_smartfit.presentation.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.project_smartfit.presentation.navigation.NavHome
import com.example.project_smartfit.presentation.navigation.NavProfile

data class BottomNavItem(
    val title: String,
    val icon: ImageVector,
    val route: Any
)

@Composable
fun BottomNavBar(navController: NavController) {
    val items = listOf(
        BottomNavItem("Home", Icons.Default.Home, NavHome),
        BottomNavItem("Profile", Icons.Default.Person, NavProfile)
    )

    val navBackStackEntry = navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry.value?.destination?.route

    NavigationBar {
        items.forEach { item ->
            NavigationBarItem(
                icon = { Icon(item.icon, contentDescription = item.title) },
                label = { Text(item.title) },
                selected = currentRoute == item.route.toString(),
                onClick = {
                    if (currentRoute != item.route.toString()) {
                        navController.navigate(item.route)
                    }
                }
            )
        }
    }
}
