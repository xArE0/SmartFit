package com.example.project_smartfit

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.project_smartfit.data.WorkoutSession
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

private val DarkBg = Color(0xFF0D0D0D)
private val CardBg = Color(0xFF1A1A1A)
private val NeonGreen = Color(0xFF00E676)

@Composable
fun WorkoutSummaryScreen(navController: NavController, summaryJson: String) {
    val gson = remember { Gson() }
    val sessions: List<WorkoutSession> = remember(summaryJson) {
        try {
            val type = object : TypeToken<List<WorkoutSession>>() {}.type
            gson.fromJson(summaryJson, type)
        } catch (_: Exception) { emptyList() }
    }

    val totalReps = sessions.sumOf { it.reps }
    val totalDuration = sessions.sumOf { it.durationSeconds }
    val avgFormScore = if (sessions.isNotEmpty()) sessions.sumOf { it.formScore } / sessions.size else 0

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .padding(top = 48.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── Header ───────────────────────────────────────────────
            item {
                Icon(
                    Icons.Default.EmojiEvents,
                    contentDescription = null,
                    tint = Color(0xFFFFD700),
                    modifier = Modifier.size(72.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Workout Complete!",
                    color = NeonGreen,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Great job! Here's your summary",
                    color = Color.Gray,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Spacer(modifier = Modifier.height(24.dp))
            }

            // ── Stats row ────────────────────────────────────────────
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    SummaryStatCard(
                        icon = Icons.Default.FitnessCenter,
                        value = "$totalReps",
                        label = "Total Reps",
                        iconTint = NeonGreen
                    )
                    SummaryStatCard(
                        icon = Icons.Default.Timer,
                        value = "${totalDuration / 60}m ${totalDuration % 60}s",
                        label = "Duration",
                        iconTint = Color(0xFF64B5F6)
                    )
                    SummaryStatCard(
                        icon = Icons.Default.CheckCircle,
                        value = "$avgFormScore%",
                        label = "Form Score",
                        iconTint = if (avgFormScore >= 80) NeonGreen else Color(0xFFFF9800)
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // ── Per-exercise breakdown ───────────────────────────────
            item {
                Text(
                    "Exercise Breakdown",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                )
            }

            items(sessions) { session ->
                ExerciseResultCard(session)
                Spacer(modifier = Modifier.height(10.dp))
            }

            // ── Done button ──────────────────────────────────────────
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = {
                        navController.navigate(NavHomepage) {
                            popUpTo(NavHomepage) { inclusive = true }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        "Done",
                        color = Color.Black,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryStatCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String,
    iconTint: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .background(CardBg, RoundedCornerShape(14.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(28.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(value, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text(label, color = Color.Gray, fontSize = 11.sp)
    }
}

@Composable
private fun ExerciseResultCard(session: WorkoutSession) {
    val displayName = session.exercise.split(" ")
        .joinToString(" ") { w -> w.replaceFirstChar { it.uppercaseChar() } }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardBg, RoundedCornerShape(12.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Form score circle
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        colors = if (session.formScore >= 80)
                            listOf(Color(0xFF00E676), Color(0xFF00BFA5))
                        else listOf(Color(0xFFFF9800), Color(0xFFE65100))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "${session.formScore}",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(displayName, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Row {
                if (session.reps > 0) {
                    Text("${session.reps} reps", color = Color.Gray, fontSize = 13.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                }
                if (session.holdSeconds > 0) {
                    Text("${session.holdSeconds}s hold", color = Color.Gray, fontSize = 13.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                }
                Text("${session.durationSeconds}s", color = Color.Gray, fontSize = 13.sp)
            }
        }
    }
}
