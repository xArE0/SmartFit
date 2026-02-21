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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.project_smartfit.data.WorkoutRepository
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val DarkBg = Color(0xFF0D0D0D)
private val CardBg = Color(0xFF1A1A1A)
private val NeonGreen = Color(0xFF00E676)
private val TextWhite = Color(0xFFE0E0E0)
private val TextGray = Color(0xFF9E9E9E)

@Composable
fun ProgressScreen(navController: NavController) {
    val context = LocalContext.current
    val repo = remember { WorkoutRepository.getInstance(context) }
    val allSessions = remember { repo.getAllSessions() }
    val stats = remember { repo.getStats() }

    // Calculate weekly streak from stats
    val weeklyStreak = stats.currentStreak

    // Calendar heat map data (last 28 days)
    val calendarData = remember {
        val cal = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val sessionCountByDate = allSessions.groupBy { dateFormat.format(Date(it.timestamp)) }
            .mapValues { it.value.size }
        val days = mutableListOf<Pair<String, Int>>() // date label, count
        for (i in 27 downTo 0) {
            val tempCal = Calendar.getInstance()
            tempCal.add(Calendar.DAY_OF_YEAR, -i)
            val dayStr = dateFormat.format(tempCal.time)
            val shortLabel = SimpleDateFormat("d", Locale.getDefault()).format(tempCal.time)
            days.add(shortLabel to (sessionCountByDate[dayStr] ?: 0))
        }
        days
    }

    Scaffold(
        bottomBar = { BottomNavBar(navController) },
        containerColor = DarkBg
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
        ) {
            // ── Header ─────────────────────────────────────────
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Progress",
                    color = TextWhite,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Your fitness journey",
                    color = TextGray,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
                )
            }

            // ── Stats Row ──────────────────────────────────────
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatCard(
                        icon = Icons.Default.FitnessCenter,
                        value = "${stats.totalWorkouts}",
                        label = "Workouts",
                        color = NeonGreen,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        icon = Icons.Default.CheckCircle,
                        value = "${stats.totalReps}",
                        label = "Total Reps",
                        color = Color(0xFF64B5F6),
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        icon = Icons.Default.LocalFireDepartment,
                        value = "$weeklyStreak",
                        label = "Day Streak",
                        color = Color(0xFFFF9800),
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            // ── Calendar Heat Map ──────────────────────────────
            item {
                Text(
                    "LAST 28 DAYS",
                    color = TextGray,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // 7 columns x 4 rows grid
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    for (row in 0 until 4) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            for (col in 0 until 7) {
                                val idx = row * 7 + col
                                if (idx < calendarData.size) {
                                    val (dayLabel, count) = calendarData[idx]
                                    val bgColor = when {
                                        count >= 3 -> NeonGreen
                                        count == 2 -> NeonGreen.copy(alpha = 0.6f)
                                        count == 1 -> NeonGreen.copy(alpha = 0.3f)
                                        else -> Color(0xFF2A2A2A)
                                    }
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(36.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(bgColor),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            dayLabel,
                                            color = if (count > 0) Color.Black else TextGray,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                } else {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // ── Average Form Score ─────────────────────────────
            item {
                val avgForm = stats.avgFormScore
                if (allSessions.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(CardBg)
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Average Form Score", color = TextGray, fontSize = 12.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "$avgForm%",
                                    color = when {
                                        avgForm >= 80 -> NeonGreen
                                        avgForm >= 60 -> Color(0xFFFFD600)
                                        else -> Color(0xFFFF5252)
                                    },
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Icon(
                                Icons.Default.EmojiEvents,
                                null,
                                tint = NeonGreen.copy(alpha = 0.3f),
                                modifier = Modifier.size(48.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }

            // ── Session History ─────────────────────────────────
            item {
                Text(
                    "WORKOUT HISTORY",
                    color = TextGray,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            if (allSessions.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(CardBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No workouts yet.\nStart your first one!",
                            color = TextGray,
                            fontSize = 14.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                items(allSessions.take(50)) { session ->
                    val dateStr = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
                        .format(Date(session.timestamp))
                    val exerciseDisplay = session.exercise.split(" ")
                        .joinToString(" ") { w -> w.replaceFirstChar { it.uppercaseChar() } }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(CardBg)
                            .clickable {
                                navController.navigate(NavSessionDetail(session.id))
                            }
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    exerciseDisplay,
                                    color = TextWhite,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(dateStr, color = TextGray, fontSize = 11.sp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    if (session.reps > 0) {
                                        Text("${session.reps} reps", color = TextGray, fontSize = 11.sp)
                                    } else {
                                        Text("${session.holdSeconds}s hold", color = TextGray, fontSize = 11.sp)
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Icon(Icons.Default.Timer, null, tint = TextGray, modifier = Modifier.size(12.dp))
                                    Text(" ${session.durationSeconds}s", color = TextGray, fontSize = 11.sp)
                                }
                            }

                            // Form score circle
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            session.formScore >= 80 -> NeonGreen.copy(alpha = 0.15f)
                                            session.formScore >= 60 -> Color(0xFFFFD600).copy(alpha = 0.15f)
                                            else -> Color(0xFFFF5252).copy(alpha = 0.15f)
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "${session.formScore}",
                                    color = when {
                                        session.formScore >= 80 -> NeonGreen
                                        session.formScore >= 60 -> Color(0xFFFFD600)
                                        else -> Color(0xFFFF5252)
                                    },
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun StatCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(CardBg)
            .padding(14.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Icon(icon, null, tint = color, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(value, color = TextWhite, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(2.dp))
            Text(label, color = TextGray, fontSize = 11.sp)
        }
    }
}
