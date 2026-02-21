package com.example.project_smartfit

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.project_smartfit.data.ChallengeRepository
import com.example.project_smartfit.data.Challenges
import com.example.project_smartfit.data.ExercisePlan
import com.google.gson.Gson

private val DarkBg = Color(0xFF0D0D0D)
private val CardBg = Color(0xFF1A1A1A)
private val NeonGreen = Color(0xFF00E676)
private val CompletedGreen = Color(0xFF2E7D32)
private val TodayBlue = Color(0xFF1565C0)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChallengeDetailScreen(navController: NavController, challengeId: String) {
    val context = LocalContext.current
    val challengeRepo = remember { ChallengeRepository.getInstance(context) }
    val challenge = remember { Challenges.getById(challengeId) }
    val gson = remember { Gson() }

    if (challenge == null) {
        Box(Modifier.fillMaxSize().background(DarkBg), contentAlignment = Alignment.Center) {
            Text("Challenge not found", color = Color.White)
        }
        return
    }

    val completedDays = remember { challengeRepo.getCompletedDays(challengeId) }
    val currentDay = remember { challengeRepo.getCurrentDay(challengeId) }
    val progress = remember { challengeRepo.getProgress(challengeId) }
    val streak = remember { challengeRepo.getStreak(challengeId) }

    val todayData = challenge.days.find { it.day == currentDay }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
    ) {
        // ── Top bar ──────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
            }
            Text(
                challenge.name,
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // ── Progress header ──────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .background(CardBg, RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Day $currentDay of ${challenge.totalDays}", color = NeonGreen, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text("🔥 $streak day streak", color = Color(0xFFFF9800), fontSize = 14.sp)
            }
            Spacer(modifier = Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = NeonGreen,
                trackColor = Color.DarkGray,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                "${(progress * 100).toInt()}% complete",
                color = Color.Gray,
                fontSize = 12.sp,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.End
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ── Calendar grid ────────────────────────────────────────
        Text(
            "Progress Calendar",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
        )

        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            for (day in 1..challenge.totalDays) {
                val completed = day in completedDays
                val isToday = day == currentDay
                val isRestDay = challenge.days.find { it.day == day }?.isRestDay == true

                val bgColor = when {
                    completed -> CompletedGreen
                    isToday -> TodayBlue
                    else -> CardBg
                }
                val borderColor = when {
                    isToday -> Color(0xFF42A5F5)
                    else -> Color.Transparent
                }

                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(bgColor)
                        .border(
                            width = if (isToday) 2.dp else 0.dp,
                            color = borderColor,
                            shape = RoundedCornerShape(8.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (completed) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "Done",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    } else if (isRestDay) {
                        Text("R", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    } else {
                        Text(
                            "$day",
                            color = if (isToday) Color.White else Color.Gray,
                            fontSize = 13.sp,
                            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ── Today's workout ──────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        ) {
            Text(
                "Day $currentDay",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(10.dp))

            if (todayData == null || currentDay > challenge.totalDays) {
                Text("🎉 Challenge Complete!", color = NeonGreen, fontSize = 16.sp)
            } else if (todayData.isRestDay) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CardBg, RoundedCornerShape(14.dp))
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.Hotel, null, tint = Color(0xFF64B5F6), modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Rest Day", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text("Take it easy and recover", color = Color.Gray, fontSize = 13.sp)
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = {
                        challengeRepo.completeDay(challengeId, currentDay)
                        navController.popBackStack()
                        navController.navigate(NavChallengeDetail(challengeId))
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF64B5F6)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Mark Rest Day Complete", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            } else {
                // Exercise list
                todayData.exercises.forEach { exercise ->
                    val displayName = exercise.name.split(" ")
                        .joinToString(" ") { w -> w.replaceFirstChar { it.uppercaseChar() } }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .background(CardBg, RoundedCornerShape(10.dp))
                            .clickable { navController.navigate(NavExerciseInfo(exercise.name)) }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("•", color = NeonGreen, fontSize = 18.sp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(displayName, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                            val detail = if (exercise.targetHoldSeconds > 0)
                                "${exercise.targetHoldSeconds}s hold × ${exercise.sets} sets"
                            else
                                "${exercise.targetReps} reps × ${exercise.sets} sets"
                            Text(detail, color = Color.Gray, fontSize = 12.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Start workout button
                Button(
                    onClick = {
                        val plan = ExercisePlan(
                            id = "challenge_${challengeId}_day_$currentDay",
                            name = "${challenge.name} – Day $currentDay",
                            description = "Challenge workout",
                            difficulty = challenge.difficulty,
                            estimatedMinutes = 0,
                            exercises = todayData.exercises
                        )
                        val planJson = gson.toJson(plan)
                        // Mark day complete, then start
                        challengeRepo.completeDay(challengeId, currentDay)
                        navController.navigate(NavExerciseCamera(planJson))
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, null, tint = Color.Black, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Start Today's Workout",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}
