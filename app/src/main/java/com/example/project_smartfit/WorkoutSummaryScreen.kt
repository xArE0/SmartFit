package com.example.project_smartfit

import androidx.compose.animation.animateContentSize
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.project_smartfit.data.WorkoutSession
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

private val DarkBg = Color(0xFF0D0D0D)
private val CardBg = Color(0xFF1A1A1A)
private val CardBgLight = Color(0xFF222222)
private val NeonGreen = Color(0xFF00E676)
private val AccentBlue = Color(0xFF64B5F6)
private val WarnOrange = Color(0xFFFF9800)
private val ErrorRed = Color(0xFFE53935)

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

    // Aggregate all tip counts across all exercises
    val aggregatedTipCounts: Map<String, Int> = remember(sessions) {
        val merged = mutableMapOf<String, Int>()
        sessions.forEach { s ->
            s.formTipCounts.forEach { (tip, count) ->
                merged[tip] = (merged[tip] ?: 0) + count
            }
        }
        merged
    }
    val totalMistakes = aggregatedTipCounts.values.sum()

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
                        iconTint = AccentBlue
                    )
                    SummaryStatCard(
                        icon = Icons.Default.CheckCircle,
                        value = "$avgFormScore%",
                        label = "Form Score",
                        iconTint = if (avgFormScore >= 80) NeonGreen else WarnOrange
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // ── Most Common Mistakes (aggregated) ────────────────────
            if (aggregatedTipCounts.isNotEmpty()) {
                item {
                    MostCommonMistakesCard(aggregatedTipCounts, totalMistakes)
                    Spacer(modifier = Modifier.height(24.dp))
                }
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

            itemsIndexed(sessions) { _, session ->
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

// ── Most Common Mistakes Card ────────────────────────────────────────

@Composable
private fun MostCommonMistakesCard(
    tipCounts: Map<String, Int>,
    totalMistakes: Int
) {
    val sortedTips = remember(tipCounts) {
        tipCounts.entries.sortedByDescending { it.value }
    }
    val topTips = sortedTips.take(5)
    val maxCount = topTips.firstOrNull()?.value ?: 1

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardBg, RoundedCornerShape(16.dp))
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.TrendingUp,
                contentDescription = null,
                tint = WarnOrange,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "Most Common Mistakes",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            "$totalMistakes total form corrections detected",
            color = Color.Gray,
            fontSize = 12.sp
        )
        Spacer(modifier = Modifier.height(16.dp))

        topTips.forEach { (tip, count) ->
            MistakeBarRow(
                tip = tip,
                count = count,
                fraction = count.toFloat() / maxCount,
                barColor = when {
                    count >= maxCount * 0.8f -> ErrorRed
                    count >= maxCount * 0.4f -> WarnOrange
                    else -> AccentBlue
                }
            )
            Spacer(modifier = Modifier.height(10.dp))
        }

        if (sortedTips.size > 5) {
            Text(
                "+${sortedTips.size - 5} more",
                color = Color.Gray,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun MistakeBarRow(
    tip: String,
    count: Int,
    fraction: Float,
    barColor: Color
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = tip,
                color = Color.White,
                fontSize = 13.sp,
                modifier = Modifier.weight(1f),
                maxLines = 2
            )
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .background(barColor.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "${count}×",
                    color = barColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { fraction },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp)),
            color = barColor,
            trackColor = barColor.copy(alpha = 0.1f),
            strokeCap = StrokeCap.Round,
        )
    }
}

// ── Stat Card ────────────────────────────────────────────────────────

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

// ── Exercise Result Card (expandable) ────────────────────────────────

@Composable
private fun ExerciseResultCard(session: WorkoutSession) {
    val displayName = session.exercise.split(" ")
        .joinToString(" ") { w -> w.replaceFirstChar { it.uppercaseChar() } }

    val hasMistakes = session.formTipCounts.isNotEmpty()
    var expanded by remember { mutableStateOf(hasMistakes) }

    val sortedTips = remember(session.formTipCounts) {
        session.formTipCounts.entries.sortedByDescending { it.value }
    }
    val totalTipCount = session.formTipCounts.values.sum()
    val maxCount = sortedTips.firstOrNull()?.value ?: 1

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardBg, RoundedCornerShape(12.dp))
            .animateContentSize()
            .clickable { expanded = !expanded }
            .padding(16.dp)
    ) {
        // ── Top row: score circle + name + metrics ──────────────────
        Row(verticalAlignment = Alignment.CenterVertically) {
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

            // Expand/collapse indicator
            if (hasMistakes || session.formTips.isNotEmpty()) {
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = Color.Gray,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // ── Expanded: mistake breakdown ──────────────────────────────
        if (expanded) {
            Spacer(modifier = Modifier.height(14.dp))

            if (hasMistakes) {
                // Mistake summary line
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.WarningAmber,
                        contentDescription = null,
                        tint = WarnOrange,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "$totalTipCount form corrections • ${sortedTips.size} unique issue${if (sortedTips.size != 1) "s" else ""}",
                        color = WarnOrange,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))

                // Individual mistake rows
                sortedTips.forEach { (tip, count) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .background(CardBgLight, RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = when {
                                count >= maxCount * 0.8f -> ErrorRed
                                count >= maxCount * 0.4f -> WarnOrange
                                else -> AccentBlue
                            },
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = tip,
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 13.sp,
                            modifier = Modifier.weight(1f),
                            maxLines = 2
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${count}×",
                            color = when {
                                count >= maxCount * 0.8f -> ErrorRed
                                count >= maxCount * 0.4f -> WarnOrange
                                else -> AccentBlue
                            },
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else if (session.formTips.isNotEmpty()) {
                // Fallback: old sessions that only have formTips (no counts)
                Text(
                    "Form tips:",
                    color = Color.Gray,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                session.formTips.forEach { tip ->
                    Text(
                        "• $tip",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 13.sp,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }

            // Perfect form message
            if (!hasMistakes && session.formTips.isEmpty() && session.formScore >= 80) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🎉", fontSize = 18.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Perfect form! Keep it up!",
                        color = NeonGreen,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
