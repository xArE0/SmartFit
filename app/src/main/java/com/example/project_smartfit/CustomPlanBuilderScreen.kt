package com.example.project_smartfit

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.project_smartfit.data.CustomPlanRepository
import com.example.project_smartfit.data.ExerciseDatabase
import com.example.project_smartfit.data.ExercisePlan
import com.example.project_smartfit.data.PlanExercise
import com.google.gson.Gson

private val DarkBg = Color(0xFF0D0D0D)
private val CardBg = Color(0xFF1A1A1A)
private val NeonGreen = Color(0xFF00E676)

@Composable
fun CustomPlanBuilderScreen(navController: NavController) {
    val context = LocalContext.current
    val repo = remember { CustomPlanRepository.getInstance(context) }
    val allExercises = remember { ExerciseDatabase.getAll() }
    val gson = remember { Gson() }

    // ── State ────────────────────────────────────────────────────────
    var step by remember { mutableIntStateOf(1) } // 1 = select exercises, 2 = configure
    val selectedExerciseIds = remember { mutableStateListOf<String>() }
    val builtExercises = remember { mutableStateListOf<PlanExercise>() }
    var planName by remember { mutableStateOf("My Custom Plan") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ── Top bar ──────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    if (step == 2) step = 1 else navController.popBackStack()
                }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                }
                Text(
                    if (step == 1) "Select Exercises" else "Configure Plan",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            when (step) {
                // ── Step 1: Select exercises ─────────────────────
                1 -> {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        itemsIndexed(allExercises) { _, exercise ->
                            val isSelected = exercise.id in selectedExerciseIds
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        if (isSelected) NeonGreen.copy(alpha = 0.15f) else CardBg,
                                        RoundedCornerShape(12.dp)
                                    )
                                    .border(
                                        width = if (isSelected) 2.dp else 0.dp,
                                        color = if (isSelected) NeonGreen else Color.Transparent,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable {
                                        if (isSelected) selectedExerciseIds.remove(exercise.id)
                                        else selectedExerciseIds.add(exercise.id)
                                    }
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (isSelected) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = NeonGreen,
                                        modifier = Modifier.size(24.dp)
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .border(2.dp, Color.Gray, RoundedCornerShape(12.dp))
                                    )
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        exercise.displayName,
                                        color = Color.White,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        exercise.targetMuscles.joinToString(", "),
                                        color = Color.Gray,
                                        fontSize = 12.sp
                                    )
                                }
                                IconButton(onClick = {
                                    navController.navigate(NavExerciseInfo(exercise.id))
                                }) {
                                    Icon(Icons.Default.Info, "Info", tint = Color.Gray)
                                }
                            }
                        }
                    }

                    // Continue button
                    Button(
                        onClick = {
                            builtExercises.clear()
                            selectedExerciseIds.forEach { id ->
                                val isHold = id == "plank"
                                builtExercises.add(
                                    PlanExercise(
                                        name = id,
                                        targetReps = if (isHold) 0 else 10,
                                        targetHoldSeconds = if (isHold) 30 else 0,
                                        sets = 3,
                                        restSeconds = 30
                                    )
                                )
                            }
                            step = 2
                        },
                        enabled = selectedExerciseIds.isNotEmpty(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonGreen,
                            disabledContainerColor = Color.DarkGray
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(
                            "Configure (${selectedExerciseIds.size} selected)",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }

                // ── Step 2: Configure each exercise ──────────────
                2 -> {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        itemsIndexed(
                            items = builtExercises.toList(),
                            key = { index, ex -> "${ex.name}_$index" }
                        ) { index, exercise ->
                            val displayName = exercise.name.split(" ")
                                .joinToString(" ") { w -> w.replaceFirstChar { it.uppercaseChar() } }
                            val isHold = exercise.targetHoldSeconds > 0

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(CardBg, RoundedCornerShape(14.dp))
                                    .padding(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        displayName,
                                        color = NeonGreen,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Row {
                                        if (index > 0) {
                                            IconButton(onClick = {
                                                val item = builtExercises.removeAt(index)
                                                builtExercises.add(index - 1, item)
                                            }, modifier = Modifier.size(32.dp)) {
                                                Icon(Icons.Default.ArrowUpward, "Up", tint = Color.Gray, modifier = Modifier.size(18.dp))
                                            }
                                        }
                                        if (index < builtExercises.size - 1) {
                                            IconButton(onClick = {
                                                val item = builtExercises.removeAt(index)
                                                builtExercises.add(index + 1, item)
                                            }, modifier = Modifier.size(32.dp)) {
                                                Icon(Icons.Default.ArrowDownward, "Down", tint = Color.Gray, modifier = Modifier.size(18.dp))
                                            }
                                        }
                                        IconButton(onClick = {
                                            builtExercises.removeAt(index)
                                            if (builtExercises.isEmpty()) step = 1
                                        }, modifier = Modifier.size(32.dp)) {
                                            Icon(Icons.Default.Delete, "Remove", tint = Color(0xFFE53935), modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Reps / Hold slider
                                if (isHold) {
                                    SliderRow(
                                        label = "Hold",
                                        value = exercise.targetHoldSeconds,
                                        range = 10f..120f,
                                        suffix = "s",
                                        onValueChange = { newVal ->
                                            builtExercises[index] = exercise.copy(targetHoldSeconds = newVal)
                                        }
                                    )
                                } else {
                                    SliderRow(
                                        label = "Reps",
                                        value = exercise.targetReps,
                                        range = 4f..30f,
                                        suffix = "",
                                        onValueChange = { newVal ->
                                            builtExercises[index] = exercise.copy(targetReps = newVal)
                                        }
                                    )
                                }

                                SliderRow(
                                    label = "Sets",
                                    value = exercise.sets,
                                    range = 1f..6f,
                                    suffix = "",
                                    onValueChange = { newVal ->
                                        builtExercises[index] = exercise.copy(sets = newVal)
                                    }
                                )

                                SliderRow(
                                    label = "Rest",
                                    value = exercise.restSeconds,
                                    range = 10f..90f,
                                    suffix = "s",
                                    onValueChange = { newVal ->
                                        builtExercises[index] = exercise.copy(restSeconds = newVal)
                                    }
                                )
                            }
                        }
                    }

                    // ── Bottom actions ────────────────────────────
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Save plan
                        OutlinedButton(
                            onClick = {
                                val plan = ExercisePlan(
                                    id = "custom_${System.currentTimeMillis()}",
                                    name = planName,
                                    description = "Custom plan with ${builtExercises.size} exercises",
                                    difficulty = "Custom",
                                    estimatedMinutes = builtExercises.sumOf {
                                        ((it.targetReps * 3 + it.targetHoldSeconds) * it.sets + it.restSeconds * (it.sets - 1)) / 60 + 1
                                    },
                                    exercises = builtExercises.toList()
                                )
                                repo.savePlan(plan)
                                navController.popBackStack()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp),
                            shape = RoundedCornerShape(14.dp),
                            border = androidx.compose.foundation.BorderStroke(2.dp, NeonGreen)
                        ) {
                            Icon(Icons.Default.Save, null, tint = NeonGreen, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Save", color = NeonGreen, fontWeight = FontWeight.Bold)
                        }

                        // Start now
                        Button(
                            onClick = {
                                val plan = ExercisePlan(
                                    id = "custom_${System.currentTimeMillis()}",
                                    name = planName,
                                    description = "Custom plan",
                                    difficulty = "Custom",
                                    estimatedMinutes = 0,
                                    exercises = builtExercises.toList()
                                )
                                repo.savePlan(plan)
                                val planJsonStr = gson.toJson(plan)
                                navController.navigate(NavExerciseCamera(planJsonStr))
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, null, tint = Color.Black, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Start", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SliderRow(
    label: String,
    value: Int,
    range: ClosedFloatingPointRange<Float>,
    suffix: String,
    onValueChange: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "$label: $value$suffix",
            color = Color.White,
            fontSize = 13.sp,
            modifier = Modifier.width(90.dp)
        )
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.toInt()) },
            valueRange = range,
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(
                thumbColor = NeonGreen,
                activeTrackColor = NeonGreen,
                inactiveTrackColor = Color.DarkGray
            )
        )
    }
}
