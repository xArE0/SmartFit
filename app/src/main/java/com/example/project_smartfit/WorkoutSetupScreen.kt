package com.example.project_smartfit

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.project_smartfit.data.ExercisePlan
import com.example.project_smartfit.data.WorkoutPlanGenerator
import com.google.gson.Gson

private val DarkBg = Color(0xFF0D0D0D)
private val CardBg = Color(0xFF1A1A1A)
private val NeonGreen = Color(0xFF00E676)
private val TextWhite = Color(0xFFE0E0E0)
private val TextGray = Color(0xFF9E9E9E)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WorkoutSetupScreen(navController: NavController) {
    val context = LocalContext.current
    val sessionManager = remember { SessionManager.getInstance(context) }
    val gson = remember { Gson() }

    var step by remember { mutableIntStateOf(0) }
    var selectedMuscle by remember { mutableStateOf("") }
    var selectedDifficulty by remember { mutableStateOf("") }
    var selectedDuration by remember { mutableStateOf("") }
    var generatedPlan by remember { mutableStateOf<ExercisePlan?>(null) }

    val fitnessGoal = sessionManager.getFitnessGoal()

    Scaffold(containerColor = DarkBg) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            // ── Top Bar ──────────────────────────────────────────────
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = {
                    if (step > 0) step-- else navController.popBackStack()
                }) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TextWhite
                    )
                }
                Text(
                    "Workout Setup",
                    color = TextWhite,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "${step + 1}/4",
                    color = NeonGreen,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // ── Progress bar ─────────────────────────────────────────
            LinearProgressIndicator(
                progress = { (step + 1) / 4f },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = NeonGreen,
                trackColor = CardBg
            )

            Spacer(modifier = Modifier.height(8.dp))

            // ── Steps ────────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                when (step) {
                    0 -> MuscleGroupStep(selected = selectedMuscle) { selectedMuscle = it }
                    1 -> DifficultyStep(selected = selectedDifficulty) { selectedDifficulty = it }
                    2 -> DurationStep(selected = selectedDuration) { selectedDuration = it }
                    3 -> PlanPreviewStep(
                        plan = generatedPlan,
                        onRegenerate = {
                            generatedPlan = WorkoutPlanGenerator.generate(
                                muscleGroup = selectedMuscle,
                                difficulty = selectedDifficulty,
                                duration = selectedDuration,
                                fitnessGoal = fitnessGoal
                            )
                        },
                        onStart = {
                            val planJson = gson.toJson(generatedPlan)
                            navController.navigate(NavExerciseCamera(planJson))
                        }
                    )
                }
            }

            // ── Bottom Button ────────────────────────────────────────
            if (step < 3) {
                val canProceed = when (step) {
                    0 -> selectedMuscle.isNotBlank()
                    1 -> selectedDifficulty.isNotBlank()
                    2 -> selectedDuration.isNotBlank()
                    else -> true
                }

                Button(
                    onClick = {
                        step++
                        if (step == 3) {
                            generatedPlan = WorkoutPlanGenerator.generate(
                                muscleGroup = selectedMuscle,
                                difficulty = selectedDifficulty,
                                duration = selectedDuration,
                                fitnessGoal = fitnessGoal
                            )
                        }
                    },
                    enabled = canProceed,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonGreen,
                        contentColor = Color.Black,
                        disabledContainerColor = CardBg,
                        disabledContentColor = TextGray
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        "Continue",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

// ── Step 1: Muscle Group Selection ──────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MuscleGroupStep(selected: String, onSelect: (String) -> Unit) {
    Text(
        "What do you want to train?",
        color = TextWhite,
        fontSize = 26.sp,
        fontWeight = FontWeight.Bold
    )
    Text(
        "Choose your target muscle group",
        color = TextGray,
        fontSize = 14.sp,
        modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
    )

    val groups = listOf(
        "Chest" to Icons.Default.FitnessCenter,
        "Arms" to Icons.Default.FitnessCenter,
        "Legs" to Icons.AutoMirrored.Filled.DirectionsRun,
        "Abs/Core" to Icons.Default.SelfImprovement,
        "Back" to Icons.Default.FitnessCenter,
        "Full Body" to Icons.Default.FlashOn
    )

    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        groups.forEach { (name, icon) ->
            val isSelected = selected == name
            SelectionCard(
                label = name,
                icon = icon,
                isSelected = isSelected,
                onClick = { onSelect(name) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

// ── Step 2: Difficulty Selection ────────────────────────────────────────

@Composable
private fun DifficultyStep(selected: String, onSelect: (String) -> Unit) {
    Text(
        "Choose your intensity",
        color = TextWhite,
        fontSize = 26.sp,
        fontWeight = FontWeight.Bold
    )
    Text(
        "We'll adjust reps, sets, and rest times",
        color = TextGray,
        fontSize = 14.sp,
        modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
    )

    val difficulties = listOf(
        Triple("Beginner", "Lower reps • Longer rest • Perfect for starting", Icons.Default.SelfImprovement),
        Triple("Intermediate", "Balanced challenge • Moderate rest", Icons.Default.FitnessCenter),
        Triple("Advanced", "High intensity • Short rest • Push your limits", Icons.Default.FlashOn)
    )

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        difficulties.forEach { (name, desc, icon) ->
            DetailCard(
                title = name,
                description = desc,
                icon = icon,
                isSelected = selected == name,
                onClick = { onSelect(name) }
            )
        }
    }
}

// ── Step 3: Duration Selection ──────────────────────────────────────────

@Composable
private fun DurationStep(selected: String, onSelect: (String) -> Unit) {
    Text(
        "How long do you have?",
        color = TextWhite,
        fontSize = 26.sp,
        fontWeight = FontWeight.Bold
    )
    Text(
        "We'll pick the right number of exercises",
        color = TextGray,
        fontSize = 14.sp,
        modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
    )

    val durations = listOf(
        Triple("Quick", "10–15 min • 3 exercises", Icons.Default.Speed),
        Triple("Medium", "20–30 min • 4 exercises", Icons.Default.AccessTime),
        Triple("Intense", "40–60 min • 5 exercises", Icons.Default.FlashOn)
    )

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        durations.forEach { (name, desc, icon) ->
            DetailCard(
                title = name,
                description = desc,
                icon = icon,
                isSelected = selected == name,
                onClick = { onSelect(name) }
            )
        }
    }
}

// ── Step 4: Plan Preview ────────────────────────────────────────────────

@Composable
private fun PlanPreviewStep(
    plan: ExercisePlan?,
    onRegenerate: () -> Unit,
    onStart: () -> Unit
) {
    if (plan == null) return

    Text(
        "Your Workout Plan",
        color = TextWhite,
        fontSize = 26.sp,
        fontWeight = FontWeight.Bold
    )

    Row(
        modifier = Modifier.padding(top = 4.dp, bottom = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(plan.description, color = TextGray, fontSize = 13.sp, modifier = Modifier.weight(1f))
    }

    // ── Plan summary badges ──────────────────────────────────────
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(CardBg)
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        PlanBadge("Exercises", "${plan.exercises.size}")
        PlanBadge("Est. Time", "~${plan.estimatedMinutes} min")
        PlanBadge("Difficulty", plan.difficulty)
    }

    Spacer(modifier = Modifier.height(20.dp))

    // ── Exercise list ────────────────────────────────────────────
    plan.exercises.forEachIndexed { index, exercise ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(CardBg)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Number badge
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(NeonGreen.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "${index + 1}",
                    color = NeonGreen,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    exercise.name.replaceFirstChar { it.uppercaseChar() },
                    color = TextWhite,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                val target = if (exercise.targetHoldSeconds > 0) {
                    "${exercise.targetHoldSeconds}s hold"
                } else {
                    "${exercise.targetReps} reps"
                }
                Text(
                    "$target × ${exercise.sets} sets • ${exercise.restSeconds}s rest",
                    color = TextGray,
                    fontSize = 12.sp
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(24.dp))

    // ── Action buttons ───────────────────────────────────────────
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedButton(
            onClick = onRegenerate,
            modifier = Modifier
                .weight(1f)
                .height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonGreen),
            border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                brush = androidx.compose.ui.graphics.SolidColor(NeonGreen.copy(alpha = 0.5f))
            )
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Regenerate", fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }

        Button(
            onClick = onStart,
            modifier = Modifier
                .weight(1f)
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = NeonGreen,
                contentColor = Color.Black
            ),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Start", fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
    }

    Spacer(modifier = Modifier.height(16.dp))
}

// ── Reusable Components ─────────────────────────────────────────────────

@Composable
private fun SelectionCard(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg = if (isSelected) NeonGreen.copy(alpha = 0.12f) else CardBg
    val borderColor = if (isSelected) NeonGreen else Color.Transparent
    val iconTint = if (isSelected) NeonGreen else TextGray
    val textColor = if (isSelected) TextWhite else TextGray

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .border(1.5.dp, borderColor, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 20.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, contentDescription = label, tint = iconTint, modifier = Modifier.size(28.dp))
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            label,
            color = textColor,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun DetailCard(
    title: String,
    description: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bg = if (isSelected) NeonGreen.copy(alpha = 0.10f) else CardBg
    val borderColor = if (isSelected) NeonGreen else Color.Transparent

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .border(1.5.dp, borderColor, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(NeonGreen.copy(alpha = if (isSelected) 0.2f else 0.08f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (isSelected) NeonGreen else TextGray,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = TextWhite, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Text(description, color = TextGray, fontSize = 12.sp, lineHeight = 16.sp)
        }
        if (isSelected) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                tint = NeonGreen,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun PlanBadge(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = NeonGreen, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        Text(label, color = TextGray, fontSize = 11.sp)
    }
}
