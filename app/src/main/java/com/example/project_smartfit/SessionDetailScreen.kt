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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.example.project_smartfit.data.WorkoutRepository
import androidx.navigation.NavController
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val DarkBg = Color(0xFF0D0D0D)
private val CardBg = Color(0xFF1A1A1A)
private val NeonGreen = Color(0xFF00E676)
private val TextWhite = Color(0xFFE0E0E0)
private val TextGray = Color(0xFF9E9E9E)

@Composable
fun SessionDetailScreen(navController: NavController, sessionId: String) {
    val context = LocalContext.current
    val repo = remember { WorkoutRepository.getInstance(context) }
    val session = remember { repo.getSessionById(sessionId) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
    ) {
        // ── Top bar ───────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, top = 40.dp, end = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = TextWhite)
            }
            Text(
                "Session Report",
                color = TextWhite,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }

        if (session == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Session not found", color = TextGray, fontSize = 16.sp)
            }
            return
        }

        val exerciseDisplay = session.exercise.split(" ")
            .joinToString(" ") { it.replaceFirstChar { c -> c.uppercaseChar() } }
        val dateFormat = SimpleDateFormat("EEEE, MMM d 'at' h:mm a", Locale.getDefault())

        Column(modifier = Modifier.padding(horizontal = 20.dp)) {

            Spacer(modifier = Modifier.height(16.dp))

            // ── Exercise Name + Date ──────────────────────────────
            Text(
                exerciseDisplay,
                color = NeonGreen,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                dateFormat.format(Date(session.timestamp)),
                color = TextGray,
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // ── Form Score Ring ────────────────────────────────────
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                val scoreColor = when {
                    session.formScore >= 80 -> NeonGreen
                    session.formScore >= 50 -> Color(0xFFFFD600)
                    else -> Color(0xFFFF5252)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        scoreColor.copy(alpha = 0.2f),
                                        scoreColor.copy(alpha = 0.05f)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "${session.formScore}",
                            color = scoreColor,
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        "Form Score",
                        color = TextGray,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Stats Grid ────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DetailStatCard(
                    icon = Icons.Default.Repeat,
                    value = if (session.holdSeconds > 0) "${session.holdSeconds}s" else "${session.reps}",
                    label = if (session.holdSeconds > 0) "Hold Time" else "Reps",
                    modifier = Modifier.weight(1f)
                )
                DetailStatCard(
                    icon = Icons.Default.AccessTime,
                    value = "${session.durationSeconds / 60}m ${session.durationSeconds % 60}s",
                    label = "Duration",
                    modifier = Modifier.weight(1f)
                )
                DetailStatCard(
                    icon = Icons.Default.FitnessCenter,
                    value = exerciseDisplay.take(8),
                    label = "Exercise",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Form Tips ─────────────────────────────────────────
            Text(
                "Form Feedback",
                color = TextWhite,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(12.dp))

            if (session.formTips.isEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(NeonGreen.copy(alpha = 0.1f))
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = NeonGreen,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        "Perfect form throughout! No corrections needed.",
                        color = NeonGreen,
                        fontSize = 14.sp
                    )
                }
            } else {
                session.formTips.forEach { tip ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFE65100).copy(alpha = 0.1f))
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color(0xFFFF9800),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            tip,
                            color = TextWhite,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Personalized AI Feedback ─────────────────────────────
            Text(
                "AI Insights",
                color = TextWhite,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(12.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(CardBg)
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // ── Strengths ────────────────────────────────────────
                val strengths = buildList {
                    if (session.formScore >= 80) add("Excellent form consistency — you maintained great posture throughout")
                    if (session.formScore >= 60 && session.formTips.isEmpty()) add("Clean execution with zero form corrections needed")
                    if (session.reps >= 15 || session.holdSeconds >= 40) add("Strong endurance — you pushed past the average range")
                    if (session.durationSeconds >= 120) add("Great workout duration — consistent effort over ${session.durationSeconds / 60}+ minutes")
                    if (isEmpty()) add("Good effort completing the session — keep building consistency")
                }

                Text("💪 Strengths", color = NeonGreen, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                strengths.forEach { strength ->
                    Text("• $strength", color = TextWhite, fontSize = 13.sp, lineHeight = 18.sp)
                }

                // ── Areas to Improve ─────────────────────────────────
                val improvements = buildList {
                    if (session.formScore < 60) add("Focus on slowing down your movements — quality over quantity")
                    if (session.formScore < 40) add("Consider reducing reps and prioritizing correct form")
                    session.formTips.take(2).forEach { tip ->
                        add("Work on: ${tip.lowercase().removeSuffix(".")}")
                    }
                    if (session.durationSeconds < 60) add("Try to extend your workout duration gradually")
                    if (isEmpty()) add("Maintain your current form — small improvements come from consistency")
                }

                Text("🎯 Areas to Improve", color = Color(0xFFFFD600), fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                improvements.forEach { item ->
                    Text("• $item", color = TextWhite, fontSize = 13.sp, lineHeight = 18.sp)
                }

                // ── Recommendations ──────────────────────────────────
                val recommendations = buildList {
                    if (session.formScore >= 80) {
                        add("You're ready to increase difficulty — try more reps or shorter rest")
                    } else {
                        add("Repeat this exercise next session to build muscle memory")
                    }
                    when {
                        session.exercise.contains("squat", true) -> add("Pair with leg raises for a complete lower body workout")
                        session.exercise.contains("push", true) -> add("Complement with lateral raises for balanced upper body")
                        session.exercise.contains("plank", true) -> add("Try russian twists to target obliques next")
                        session.exercise.contains("curl", true) -> add("Add lateral raises for complete arm development")
                        else -> add("Try a Full Body plan next for balanced training")
                    }
                }

                Text("🧠 Next Session", color = Color(0xFF64B5F6), fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                recommendations.forEach { rec ->
                    Text("• $rec", color = TextWhite, fontSize = 13.sp, lineHeight = 18.sp)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun DetailStatCard(
    icon: ImageVector,
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(CardBg)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, contentDescription = label, tint = NeonGreen, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.height(8.dp))
        Text(value, color = TextWhite, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text(label, color = TextGray, fontSize = 11.sp)
    }
}
