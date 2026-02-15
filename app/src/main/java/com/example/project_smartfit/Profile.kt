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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.project_smartfit.data.WorkoutRepository

private val DarkBg = Color(0xFF0D0D0D)
private val CardBg = Color(0xFF1A1A1A)
private val NeonGreen = Color(0xFF00E676)
private val TextWhite = Color(0xFFE0E0E0)
private val TextGray = Color(0xFF9E9E9E)

@Composable
fun Profile(
    navController: NavController,
    sessionManager: SessionManager
) {
    val context = LocalContext.current
    val repo = remember { WorkoutRepository.getInstance(context) }
    val stats = remember { repo.getStats() }
    val weeklyReps = remember { repo.getWeeklyReps() }

    val email = sessionManager.getEmail() ?: "User"
    val name = email.substringBefore("@")

    // Period report
    val periodOptions = listOf("This Week", "This Month", "All Time")
    var selectedPeriod by remember { mutableIntStateOf(0) }
    val report = remember(selectedPeriod) {
        val sessions = when (selectedPeriod) {
            0 -> repo.getThisWeekSessions()
            1 -> repo.getThisMonthSessions()
            else -> repo.getAllSessions()
        }
        repo.generateReport(periodOptions[selectedPeriod], sessions)
    }

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
            // ── Profile Header ────────────────────────────────────
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(NeonGreen.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        tint = NeonGreen,
                        modifier = Modifier.size(30.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        name.replaceFirstChar { it.uppercaseChar() },
                        color = TextWhite,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(email, color = TextGray, fontSize = 13.sp)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── All-Time Stats ────────────────────────────────────
            Text(
                "All-Time Stats",
                color = TextWhite,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ProfileStatCard(Icons.Default.FitnessCenter, "${stats.totalWorkouts}", "Workouts", Modifier.weight(1f))
                ProfileStatCard(Icons.Default.Repeat, "${stats.totalReps}", "Reps", Modifier.weight(1f))
                ProfileStatCard(Icons.Default.LocalFireDepartment, "${stats.currentStreak}", "Streak", Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ProfileStatCard(Icons.Default.Star, stats.favoriteExercise.take(10), "Favorite", Modifier.weight(1f))
                ProfileStatCard(Icons.Default.EmojiEvents, "${stats.avgFormScore}%", "Avg Form", Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Weekly Activity Chart ─────────────────────────────
            Text(
                "Weekly Activity",
                color = TextWhite,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(12.dp))
            WeeklyChart(weeklyReps)

            Spacer(modifier = Modifier.height(24.dp))

            // ── Period Report ─────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Performance Report",
                    color = TextWhite,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Icon(Icons.Default.BarChart, null, tint = NeonGreen, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                periodOptions.forEachIndexed { index, label ->
                    FilterChip(
                        selected = selectedPeriod == index,
                        onClick = { selectedPeriod = index },
                        label = { Text(label, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = NeonGreen.copy(alpha = 0.15f),
                            selectedLabelColor = NeonGreen,
                            containerColor = CardBg,
                            labelColor = TextGray
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = Color.Transparent,
                            selectedBorderColor = NeonGreen.copy(alpha = 0.4f),
                            enabled = true,
                            selected = selectedPeriod == index
                        )
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))

            // Report card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(CardBg)
                    .padding(20.dp)
            ) {
                if (report.totalWorkouts == 0) {
                    Text(
                        "No data for ${report.periodLabel}",
                        color = TextGray,
                        fontSize = 14.sp
                    )
                } else {
                    ReportRow("Workouts", "${report.totalWorkouts}")
                    ReportRow("Total Reps", "${report.totalReps}")
                    ReportRow("Time", "${report.totalMinutes} minutes")
                    ReportRow("Avg Form", "${report.avgFormScore}%")
                    ReportRow("Most Done", report.bestExercise.replaceFirstChar { it.uppercaseChar() })
                    ReportRow(
                        "Exercises",
                        report.exercisesDone.joinToString(", ") { it.replaceFirstChar { c -> c.uppercaseChar() } }
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // ── Logout ────────────────────────────────────────────
            Button(
                onClick = {
                    sessionManager.clearSession()
                    navController.navigate(NavLogin) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2A2A2A)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Logout,
                    contentDescription = null,
                    tint = Color(0xFFFF5252),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Logout",
                    color = Color(0xFFFF5252),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// ── Profile Stat Card ────────────────────────────────────────────────

@Composable
private fun ProfileStatCard(
    icon: ImageVector,
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(CardBg)
            .padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, contentDescription = label, tint = NeonGreen, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.height(6.dp))
        Text(value, color = TextWhite, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text(label, color = TextGray, fontSize = 11.sp)
    }
}

// ── Weekly Bar Chart ─────────────────────────────────────────────────

@Composable
private fun WeeklyChart(reps: List<Int>) {
    val maxReps = (reps.maxOrNull() ?: 1).coerceAtLeast(1)
    val dayLabels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(CardBg)
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom
    ) {
        reps.forEachIndexed { index, count ->
            val fraction = (count.toFloat() / maxReps).coerceIn(0f, 1f)
            val barHeight = (fraction * 80).coerceAtLeast(4f)

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(36.dp)
            ) {
                if (count > 0) {
                    Text(
                        "$count",
                        color = NeonGreen,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .width(20.dp)
                        .height(barHeight.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            if (count > 0) NeonGreen.copy(alpha = 0.7f)
                            else Color(0xFF2A2A2A)
                        )
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    if (index < dayLabels.size) dayLabels[index] else "",
                    color = TextGray,
                    fontSize = 10.sp
                )
            }
        }
    }
}

// ── Report Row ───────────────────────────────────────────────────────

@Composable
private fun ReportRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = TextGray, fontSize = 14.sp)
        Text(
            value,
            color = TextWhite,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}