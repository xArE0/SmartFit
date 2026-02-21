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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.example.project_smartfit.data.WorkoutSession
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val DarkBg = Color(0xFF0D0D0D)
private val CardBg = Color(0xFF1A1A1A)
private val NeonGreen = Color(0xFF00E676)
private val TextWhite = Color(0xFFE0E0E0)
private val TextGray = Color(0xFF9E9E9E)

@Composable
fun HistoryScreen(navController: NavController) {
    val context = LocalContext.current
    val repo = remember { WorkoutRepository.getInstance(context) }

    val filterOptions = listOf("Today", "This Week", "This Month", "All Time")
    var selectedFilter by remember { mutableIntStateOf(0) }

    val sessions = remember(selectedFilter) {
        when (selectedFilter) {
            0 -> repo.getTodaySessions()
            1 -> repo.getThisWeekSessions()
            2 -> repo.getThisMonthSessions()
            else -> repo.getAllSessions()
        }
    }

    // Group sessions by date
    val dateFormat = SimpleDateFormat("EEEE, MMM d", Locale.getDefault())
    val grouped = remember(sessions) {
        sessions.groupBy { dateFormat.format(Date(it.timestamp)) }
    }

    Scaffold(
        bottomBar = { BottomNavBar(navController) },
        containerColor = DarkBg
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // ── Header ────────────────────────────────────────────
            Text(
                "Workout History",
                color = TextWhite,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 4.dp)
            )

            Text(
                "${sessions.size} sessions • ${sessions.sumOf { it.reps }} total reps",
                color = TextGray,
                fontSize = 13.sp,
                modifier = Modifier.padding(start = 20.dp, bottom = 12.dp)
            )

            // ── Filter Chips ──────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                filterOptions.forEachIndexed { index, label ->
                    FilterChip(
                        selected = selectedFilter == index,
                        onClick = { selectedFilter = index },
                        label = {
                            Text(
                                label,
                                fontSize = 12.sp,
                                fontWeight = if (selectedFilter == index) FontWeight.Bold else FontWeight.Normal
                            )
                        },
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
                            selected = selectedFilter == index
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ── Session List ──────────────────────────────────────
            if (sessions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.FitnessCenter,
                            contentDescription = null,
                            tint = TextGray,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "No sessions found",
                            color = TextGray,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            "Complete a workout to see it here!",
                            color = TextGray.copy(alpha = 0.7f),
                            fontSize = 13.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    grouped.forEach { (dateLabel, daySessions) ->
                        item {
                            Text(
                                dateLabel,
                                color = TextGray,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp, start = 4.dp)
                            )
                        }
                        items(daySessions, key = { it.id }) { session ->
                            SessionCard(session) {
                                navController.navigate(NavSessionDetail(session.id))
                            }
                        }
                    }
                    item { Spacer(modifier = Modifier.height(8.dp)) }
                }
            }
        }
    }
}

// ── Session Card ─────────────────────────────────────────────────────

@Composable
private fun SessionCard(session: WorkoutSession, onClick: () -> Unit) {
    val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
    val exerciseDisplay = session.exercise.split(" ")
        .joinToString(" ") { it.replaceFirstChar { c -> c.uppercaseChar() } }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(CardBg)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Exercise info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                exerciseDisplay,
                color = TextWhite,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Schedule,
                    contentDescription = null,
                    tint = TextGray,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    timeFormat.format(Date(session.timestamp)),
                    color = TextGray,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    if (session.holdSeconds > 0) "${session.holdSeconds}s hold"
                    else "${session.reps} reps",
                    color = NeonGreen.copy(alpha = 0.8f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.width(12.dp))
                val mins = session.durationSeconds / 60
                val secs = session.durationSeconds % 60
                Text(
                    "${mins}m ${secs}s",
                    color = TextGray,
                    fontSize = 12.sp
                )
            }
        }

        // Form score badge
        FormScoreBadge(session.formScore)
    }
}
