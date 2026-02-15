package com.example.project_smartfit

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState

data class BottomNavItem(
    val title: String,
    val icon: ImageVector,
    val route: Any
)

private val NavGreen = Color(0xFF00E676)
private val NavDarkBg = Color(0xFF121212)
private val NavGray = Color(0xFF757575)

@Composable
fun BottomNavBar(navController: NavController) {
    val items = listOf(
        BottomNavItem("Home", Icons.Default.Home, NavHomepage),
        BottomNavItem("Workout", Icons.Default.FitnessCenter, NavWorkout),
        BottomNavItem("Explore", Icons.Default.Search, NavExplore),
        BottomNavItem("Progress", Icons.Default.BarChart, NavProgress),
        BottomNavItem("Profile", Icons.Default.Person, NavProfile)
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar(
        containerColor = NavDarkBg,
        contentColor = NavGreen,
        tonalElevation = 0.dp
    ) {
        items.forEach { item ->
            val isSelected = currentRoute?.contains(item.route::class.qualifiedName ?: "") == true

            val scale by animateFloatAsState(
                targetValue = if (isSelected) 1.15f else 1.0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                ),
                label = "navScale"
            )

            val iconColor by animateColorAsState(
                targetValue = if (isSelected) NavGreen else NavGray,
                label = "navIconColor"
            )

            NavigationBarItem(
                icon = {
                    Icon(
                        item.icon,
                        contentDescription = item.title,
                        tint = iconColor,
                        modifier = Modifier
                            .size(24.dp)
                            .scale(scale)
                    )
                },
                label = {
                    Text(
                        item.title,
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = iconColor
                    )
                },
                selected = isSelected,
                onClick = {
                    if (!isSelected) {
                        navController.navigate(item.route) {
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = NavGreen,
                    selectedTextColor = NavGreen,
                    unselectedIconColor = NavGray,
                    unselectedTextColor = NavGray,
                    indicatorColor = NavGreen.copy(alpha = 0.12f)
                )
            )
        }
    }
}
