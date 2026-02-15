package com.example.project_smartfit.data

/**
 * Generates custom workout plans based on user profile and preferences.
 *
 * Uses the 7 exercises the ML classifier supports, mapped to muscle groups.
 * Adjusts reps/sets/rest based on difficulty, fitness goal, and duration.
 */
object WorkoutPlanGenerator {

    // ── Exercise → Muscle‐Group mapping ─────────────────────────────────

    private data class ExerciseInfo(
        val name: String,
        val muscleGroups: Set<String>,
        val isHoldExercise: Boolean = false
    )

    private val exercises = listOf(
        ExerciseInfo("push-up",       setOf("Chest", "Arms", "Full Body")),
        ExerciseInfo("hammer curl",   setOf("Arms", "Full Body")),
        ExerciseInfo("lateral raise", setOf("Arms", "Back", "Full Body")),
        ExerciseInfo("squat",         setOf("Legs", "Full Body")),
        ExerciseInfo("leg raises",    setOf("Abs/Core", "Legs", "Full Body")),
        ExerciseInfo("plank",         setOf("Abs/Core", "Full Body"), isHoldExercise = true),
        ExerciseInfo("russian twist", setOf("Abs/Core", "Full Body"))
    )

    // ── Duration → exercise count ───────────────────────────────────────

    private fun exerciseCountForDuration(duration: String): Int = when (duration) {
        "Quick"   -> 3
        "Medium"  -> 4
        "Intense" -> 5
        else      -> 4
    }

    // ── Base reps/hold per difficulty ────────────────────────────────────

    private data class DifficultyProfile(
        val baseReps: Int,
        val baseHold: Int,
        val baseSets: Int,
        val baseRest: Int
    )

    private fun difficultyProfile(difficulty: String) = when (difficulty) {
        "Beginner"     -> DifficultyProfile(baseReps = 8,  baseHold = 20, baseSets = 2, baseRest = 40)
        "Intermediate" -> DifficultyProfile(baseReps = 12, baseHold = 35, baseSets = 3, baseRest = 30)
        "Advanced"     -> DifficultyProfile(baseReps = 16, baseHold = 50, baseSets = 4, baseRest = 20)
        else           -> DifficultyProfile(baseReps = 10, baseHold = 30, baseSets = 3, baseRest = 30)
    }

    // ── Goal‐based adjustments ──────────────────────────────────────────

    private data class GoalModifier(
        val repMultiplier: Float,
        val setMultiplier: Float,
        val restMultiplier: Float
    )

    private fun goalModifier(goal: String) = when (goal) {
        "Fat Loss"        -> GoalModifier(repMultiplier = 1.2f, setMultiplier = 1.0f, restMultiplier = 0.7f)
        "Muscle Gain"     -> GoalModifier(repMultiplier = 1.0f, setMultiplier = 1.3f, restMultiplier = 1.0f)
        "Strength"        -> GoalModifier(repMultiplier = 0.7f, setMultiplier = 1.5f, restMultiplier = 1.3f)
        "General Fitness" -> GoalModifier(repMultiplier = 1.0f, setMultiplier = 1.0f, restMultiplier = 1.0f)
        else              -> GoalModifier(repMultiplier = 1.0f, setMultiplier = 1.0f, restMultiplier = 1.0f)
    }

    // ── Estimated total time ────────────────────────────────────────────

    private fun estimateMinutes(exercises: List<PlanExercise>): Int {
        var totalSec = 0
        for (ex in exercises) {
            val workSec = if (ex.targetHoldSeconds > 0) ex.targetHoldSeconds else (ex.targetReps * 3)
            totalSec += (workSec + ex.restSeconds) * ex.sets
        }
        return (totalSec / 60).coerceAtLeast(5)
    }

    // ── Public API ──────────────────────────────────────────────────────

    /**
     * Generate a custom workout plan.
     *
     * @param muscleGroup   One of: Chest, Arms, Legs, Abs/Core, Back, Full Body
     * @param difficulty    One of: Beginner, Intermediate, Advanced
     * @param duration      One of: Quick, Medium, Intense
     * @param fitnessGoal   One of: Fat Loss, Muscle Gain, Strength, General Fitness
     */
    fun generate(
        muscleGroup: String,
        difficulty: String,
        duration: String,
        fitnessGoal: String
    ): ExercisePlan {
        val dp = difficultyProfile(difficulty)
        val gm = goalModifier(fitnessGoal)
        val count = exerciseCountForDuration(duration)

        // 1. Filter exercises for the chosen muscle group
        val filtered = exercises.filter { muscleGroup in it.muscleGroups }

        // 2. Pick exercises (shuffle for variety, cap at count)
        val selected = filtered.shuffled().take(count).let { picked ->
            // If not enough exercises for this group, pad with Full Body ones
            if (picked.size < count) {
                val extras = exercises.filter { it !in picked }.shuffled()
                    .take(count - picked.size)
                picked + extras
            } else picked
        }

        // 3. Build plan exercises with adjusted reps/sets/rest
        val planExercises = selected.map { ex ->
            if (ex.isHoldExercise) {
                PlanExercise(
                    name = ex.name,
                    targetReps = 0,
                    targetHoldSeconds = (dp.baseHold * gm.repMultiplier).toInt(),
                    sets = (dp.baseSets * gm.setMultiplier).toInt().coerceAtLeast(1),
                    restSeconds = (dp.baseRest * gm.restMultiplier).toInt().coerceAtLeast(10)
                )
            } else {
                PlanExercise(
                    name = ex.name,
                    targetReps = (dp.baseReps * gm.repMultiplier).toInt().coerceAtLeast(4),
                    targetHoldSeconds = 0,
                    sets = (dp.baseSets * gm.setMultiplier).toInt().coerceAtLeast(1),
                    restSeconds = (dp.baseRest * gm.restMultiplier).toInt().coerceAtLeast(10)
                )
            }
        }

        val estMin = estimateMinutes(planExercises)

        return ExercisePlan(
            id = "generated_${System.currentTimeMillis()}",
            name = "$muscleGroup — $difficulty",
            description = "Custom $duration plan for $fitnessGoal. " +
                    "${planExercises.size} exercises, ~$estMin min.",
            difficulty = difficulty,
            estimatedMinutes = estMin,
            exercises = planExercises
        )
    }
}
