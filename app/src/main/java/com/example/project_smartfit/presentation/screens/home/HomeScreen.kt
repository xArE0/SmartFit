package com.example.project_smartfit.presentation.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.project_smartfit.data.local.SessionManager
import com.example.project_smartfit.domain.model.ExerciseType
import com.example.project_smartfit.presentation.components.*
import com.example.project_smartfit.presentation.navigation.*
import com.example.project_smartfit.presentation.screens.exercise.ExerciseDetectionScreen
import com.example.project_smartfit.presentation.theme.*
import androidx.compose.animation.core.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    sessionManager: SessionManager
) {
    var selectedExercise by remember { mutableStateOf<ExerciseType?>(null) }
    var showCamera by remember { mutableStateOf(false) }
    val name = sessionManager.getEmail()?.substringBefore("@")?.replaceFirstChar { it.uppercase() } ?: "User"

    if (showCamera && selectedExercise != null) {
        ExerciseDetectionScreen(
            exerciseType = selectedExercise!!,
            onBack = {
                showCamera = false
                selectedExercise = null
            }
        )
    } else {
        LightAuroraBackground(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                containerColor = Color.Transparent,
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                "SmartFit",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Slate900
                            )
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent
                        ),
                        actions = {
                            IconButton(onClick = { navController.navigate(NavProfile) }) {
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = "Profile",
                                    tint = GovBlue
                                )
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
                        .padding(horizontal = 20.dp)
                ) {
                    // Header Section
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp)
                    ) {
                        Text(
                            text = "Hello, $name! 👋",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = Slate900
                        )
                        Text(
                            text = "Ready for your daily posture check?",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Slate600
                        )
                    }

                    // Exercise Grid
                    val exercises = listOf(
                        ExerciseInfo("💪", "Pushup", "Upper body form", ExerciseType.PUSHUP),
                        ExerciseInfo("🏋️", "Squat", "Lower body alignment", ExerciseType.SQUAT),
                        ExerciseInfo("📏", "Plank", "Core stability", ExerciseType.PLANK),
                        ExerciseInfo("🤸", "Jumping Jacks", "Coordination", ExerciseType.JUMPING_JACKS),
                        ExerciseInfo("🎯", "Dumbbell Curl", "Arm flexion", ExerciseType.DUMBBELL_CURL)
                    )

                    androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                        columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(2),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(exercises.size) { index ->
                            val exercise = exercises[index]
                            AnimatedExerciseTile(
                                exercise = exercise,
                                delay = index * 100,
                                onClick = {
                                    selectedExercise = exercise.type
                                    showCamera = true
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Quick Action or Logout (Styled as a ghost button or similar)
                    TextButton(
                        onClick = {
                            sessionManager.clearSession()
                            navController.navigate(NavLogin) {
                                popUpTo(0)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        colors = ButtonDefaults.textButtonColors(contentColor = ErrorRed)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ExitToApp, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Sign Out", fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }
    }
}

data class ExerciseInfo(
    val emoji: String,
    val title: String,
    val description: String,
    val type: ExerciseType
)

@Composable
fun AnimatedExerciseTile(
    exercise: ExerciseInfo,
    delay: Int,
    onClick: () -> Unit
) {
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(delay.toLong())
        isVisible = true
    }

    val animatedScale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0.8f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "tile_scale"
    )

    val animatedAlpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(600),
        label = "tile_alpha"
    )

    Box(
        modifier = Modifier
            .graphicsLayer(
                scaleX = animatedScale,
                scaleY = animatedScale,
                alpha = animatedAlpha
            )
            .clickable(onClick = onClick)
    ) {
        GlassCard(
            variant = GlassCardVariant.Light,
            cornerRadius = 20.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(GovBlue.copy(alpha = 0.1f), Color.Transparent)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(exercise.emoji, fontSize = 40.sp)
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = exercise.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Slate900,
                    textAlign = TextAlign.Center
                )
                
                Text(
                    text = exercise.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate500,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }
        }
    }
}
