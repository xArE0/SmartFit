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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import com.example.project_smartfit.data.WorkoutRepository
import com.example.project_smartfit.data.ChallengeRepository
import com.example.project_smartfit.data.Challenges
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val DarkBg = Color(0xFF0D0D0D)
private val CardBg = Color(0xFF1A1A1A)
private val NeonGreen = Color(0xFF00E676)
private val AccentTeal = Color(0xFF00BFA5)
private val TextWhite = Color(0xFFE0E0E0)
private val TextGray = Color(0xFF9E9E9E)

@Composable
fun Homepage(
    navController: NavController,
    sessionManager: SessionManager
) {
    val context = LocalContext.current
    val repo = remember { WorkoutRepository.getInstance(context) }
    val todaySessions = remember { repo.getTodaySessions() }
    val stats = remember { repo.getStats() }
    val latestSession = remember { repo.getAllSessions().firstOrNull() }
    val challengeRepo = remember { ChallengeRepository.getInstance(context) }
    val enrolledChallenges = remember { Challenges.all.filter { challengeRepo.isEnrolled(it.id) } }

    val name = sessionManager.getName().substringBefore("@") ?: "User"
    val dateFormat = SimpleDateFormat("EEEE, MMM d", Locale.getDefault())
    val today = dateFormat.format(Date())

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
            // ── Greeting ──────────────────────────────────────────
            Text(
                text = "Hey, $name",
                color = TextWhite,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = today,
                color = TextGray,
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 2.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // ── Today's Stats Row ─────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatPill(
                    icon = Icons.Default.DirectionsRun,
                    value = "${todaySessions.size}",
                    label = "Workouts",
                    modifier = Modifier.weight(1f)
                )
                StatPill(
                    icon = Icons.Default.RepeatOne,
                    value = "${todaySessions.sumOf { it.reps }}",
                    label = "Reps",
                    modifier = Modifier.weight(1f)
                )
                StatPill(
                    icon = Icons.Default.LocalFireDepartment,
                    value = "${stats.currentStreak}",
                    label = "Day Streak",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // ── Start Workout Button ──────────────────────────────
            Button(
                onClick = { navController.navigate(NavWorkoutSetup) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NeonGreen
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Start Workout",
                    color = Color.Black,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // ── Latest Session Card ───────────────────────────────
            if (latestSession != null) {
                Text(
                    "Latest Session",
                    color = TextGray,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    CardBg,
                                    Color(0xFF1E2A1E)
                                )
                            )
                        )
                        .clickable {
                            navController.navigate(NavSessionDetail(latestSession.id))
                        }
                        .padding(20.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = latestSession.exercise.split(" ")
                                    .joinToString(" ") { it.replaceFirstChar { c -> c.uppercaseChar() } },
                                color = NeonGreen,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                            FormScoreBadge(latestSession.formScore)
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(20.dp)
                        ) {
                            Column {
                                Text(
                                    if (latestSession.holdSeconds > 0) "${latestSession.holdSeconds}s" else "${latestSession.reps}",
                                    color = TextWhite,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    if (latestSession.holdSeconds > 0) "Hold" else "Reps",
                                    color = TextGray,
                                    fontSize = 12.sp
                                )
                            }
                            Column {
                                val mins = latestSession.durationSeconds / 60
                                val secs = latestSession.durationSeconds % 60
                                Text(
                                    "${mins}m ${secs}s",
                                    color = TextWhite,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text("Duration", color = TextGray, fontSize = 12.sp)
                            }
                        }
                    }
                }
            } else {
                // Empty state
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(CardBg)
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.FitnessCenter,
                            contentDescription = null,
                            tint = TextGray,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "No workouts yet",
                            color = TextGray,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            "Start your first workout to see stats here",
                            color = TextGray.copy(alpha = 0.7f),
                            fontSize = 13.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// ── Stat Pill ────────────────────────────────────────────────────────

@Composable
private fun StatPill(
    icon: ImageVector,
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(CardBg)
            .padding(vertical = 16.dp, horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = NeonGreen,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            value,
            color = TextWhite,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            label,
            color = TextGray,
            fontSize = 11.sp
        )
    }
}

// ── Form Score Badge ─────────────────────────────────────────────────

@Composable
fun FormScoreBadge(score: Int) {
    val color = when {
        score >= 80 -> NeonGreen
        score >= 50 -> Color(0xFFFFD600)
        else -> Color(0xFFFF5252)
    }
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(color.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            "$score",
            color = color,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
