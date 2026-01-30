package com.example.project_smartfit.presentation.screens.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.project_smartfit.data.local.SessionManager
import com.example.project_smartfit.domain.model.ExerciseType
import com.example.project_smartfit.presentation.components.BottomNavBar
import com.example.project_smartfit.presentation.navigation.NavLogin
import com.example.project_smartfit.presentation.navigation.NavProfile
import com.example.project_smartfit.presentation.screens.exercise.ExerciseDetectionScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    sessionManager: SessionManager
) {
    var selectedExercise by remember { mutableStateOf<ExerciseType?>(null) }
    var showCamera by remember { mutableStateOf(false) }
    val name = sessionManager.getEmail() ?: "User"

    if (showCamera && selectedExercise != null) {
        // Show exercise detection screen
        ExerciseDetectionScreen(
            exerciseType = selectedExercise!!,
            onBack = {
                showCamera = false
                selectedExercise = null
            }
        )
    } else {
        // Homepage with exercise selection
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("SmartFit Posture Trainer") },
                    navigationIcon = {
                        IconButton(onClick = { navController.navigate(NavProfile) }) {
                            Icon(Icons.Default.Home, contentDescription = "Profile")
                        }
                    }
                )
            },
            bottomBar = {
                BottomNavBar(navController)
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Welcome section
                Text(
                    text = "Welcome back, $name! 👋",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Text(
                    text = "Select an exercise to analyze your form",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Exercise cards grid
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Pushup
                    ExerciseCard(
                        emoji = "💪",
                        title = "Pushup",
                        description = "Upper body strength with form analysis",
                        isSelected = selectedExercise == ExerciseType.PUSHUP,
                        onClick = {
                            selectedExercise = ExerciseType.PUSHUP
                            showCamera = true
                        }
                    )

                    // Squat
                    ExerciseCard(
                        emoji = "🏋️",
                        title = "Squat",
                        description = "Lower body strength and alignment",
                        isSelected = selectedExercise == ExerciseType.SQUAT,
                        onClick = {
                            selectedExercise = ExerciseType.SQUAT
                            showCamera = true
                        }
                    )

                    // Plank
                    ExerciseCard(
                        emoji = "📏",
                        title = "Plank",
                        description = "Core stability and body alignment",
                        isSelected = selectedExercise == ExerciseType.PLANK,
                        onClick = {
                            selectedExercise = ExerciseType.PLANK
                            showCamera = true
                        }
                    )

                    // Jumping Jacks
                    ExerciseCard(
                        emoji = "🤸",
                        title = "Jumping Jacks",
                        description = "Full body coordination and rhythm",
                        isSelected = selectedExercise == ExerciseType.JUMPING_JACKS,
                        onClick = {
                            selectedExercise = ExerciseType.JUMPING_JACKS
                            showCamera = true
                        }
                    )

                    // Dumbbell Curl
                    ExerciseCard(
                        emoji = "🎯",
                        title = "Dumbbell Curl",
                        description = "Arm flexion and shoulder stability",
                        isSelected = selectedExercise == ExerciseType.DUMBBELL_CURL,
                        onClick = {
                            selectedExercise = ExerciseType.DUMBBELL_CURL
                            showCamera = true
                        }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Logout button
                Button(
                    onClick = {
                        sessionManager.clearSession()
                        navController.navigate(NavLogin)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = Color.Red
                    )
                ) {
                    Text("Logout", color = Color.White)
                }
            }
        }
    }
}

/**
 * Reusable exercise selection card
 */
@Composable
private fun ExerciseCard(
    emoji: String,
    title: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    androidx.compose.material3.Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Emoji
            Text(
                text = emoji,
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.size(48.dp)
            )

            // Content
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }

            // Arrow
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = "Start",
                    tint = Color.Gray
                )
            }
        }
    }
}
