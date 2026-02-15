package com.example.project_smartfit

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.project_smartfit.data.CustomPlanRepository
import com.example.project_smartfit.data.ExercisePlans
import com.google.gson.Gson

private val DarkBg = Color(0xFF0D0D0D)
private val CardBg = Color(0xFF1A1A1A)
private val NeonGreen = Color(0xFF00E676)
private val TextWhite = Color(0xFFE0E0E0)
private val TextGray = Color(0xFF9E9E9E)

@Composable
fun WorkoutHubScreen(navController: NavController) {
    val context = LocalContext.current
    val gson = remember { Gson() }
    val customPlanRepo = remember { CustomPlanRepository.getInstance(context) }
    val customPlans = remember { customPlanRepo.getAllPlans() }

    Scaffold(
        bottomBar = { BottomNavBar(navController) },
        containerColor = DarkBg
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            // ── Header ─────────────────────────────────────────
            Text(
                "Workout",
                color = TextWhite,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Choose how you want to train",
                color = TextGray,
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
            )

            // ── Mode Cards ─────────────────────────────────────
            ModeCard(
                title = "Free Mode",
                subtitle = "No plan — just start exercising and the AI will detect your moves",
                icon = Icons.Default.DirectionsRun,
                gradient = listOf(Color(0xFF1B5E20), Color(0xFF2E7D32)),
                iconTint = NeonGreen,
                onClick = { navController.navigate(NavExerciseCamera()) }
            )

            Spacer(modifier = Modifier.height(12.dp))

            ModeCard(
                title = "Build a Plan",
                subtitle = "Configure exercises, reps & sets — or let the AI generate one for you",
                icon = Icons.Default.FitnessCenter,
                gradient = listOf(Color(0xFF0D47A1), Color(0xFF1565C0)),
                iconTint = Color(0xFF64B5F6),
                onClick = { navController.navigate(NavWorkoutSetup) }
            )

            Spacer(modifier = Modifier.height(12.dp))

            ModeCard(
                title = "Challenges",
                subtitle = "Join multi-day fitness challenges and track your streak",
                icon = Icons.Default.LocalFireDepartment,
                gradient = listOf(Color(0xFFBF360C), Color(0xFFD84315)),
                iconTint = Color(0xFFFF9800),
                onClick = { navController.navigate(NavChallenges) }
            )

            // ── Quick Plans ────────────────────────────────────
            Spacer(modifier = Modifier.height(28.dp))
            Text(
                "QUICK START",
                color = TextGray,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Pre-built plans
            ExercisePlans.all.take(3).forEach { plan ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(CardBg)
                        .clickable {
                            navController.navigate(NavExerciseCamera(gson.toJson(plan)))
                        }
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(plan.name, color = TextWhite, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                            Text(
                                "${plan.exercises.size} exercises · ~${plan.estimatedMinutes} min",
                                color = TextGray,
                                fontSize = 12.sp
                            )
                        }
                        Icon(Icons.Default.PlayArrow, null, tint = NeonGreen, modifier = Modifier.size(24.dp))
                    }
                }
            }

            // ── Custom Plans ───────────────────────────────────
            if (customPlans.isNotEmpty()) {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    "MY CUSTOM PLANS",
                    color = TextGray,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(12.dp))

                customPlans.forEach { plan ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(CardBg)
                            .clickable {
                                navController.navigate(NavExerciseCamera(gson.toJson(plan)))
                            }
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(plan.name, color = TextWhite, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                                Text("${plan.exercises.size} exercises", color = TextGray, fontSize = 12.sp)
                            }
                            Icon(Icons.Default.PlayArrow, null, tint = Color(0xFF64B5F6), modifier = Modifier.size(24.dp))
                        }
                    }
                }
            }

            // ── Build Custom ───────────────────────────────────
            Spacer(modifier = Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(NeonGreen.copy(alpha = 0.10f))
                    .clickable { navController.navigate(NavCustomPlanBuilder) }
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "+ Build Custom Plan",
                    color = NeonGreen,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ModeCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    gradient: List<Color>,
    iconTint: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Brush.linearGradient(gradient))
            .clickable(onClick = onClick)
            .padding(24.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    title,
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    subtitle,
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            }
        }
    }
}
