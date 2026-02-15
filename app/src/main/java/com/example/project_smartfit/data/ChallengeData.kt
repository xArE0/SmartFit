package com.example.project_smartfit.data

/**
 * One day within a challenge.
 */
data class ChallengeDay(
    val day: Int,
    val isRestDay: Boolean = false,
    val exercises: List<PlanExercise> = emptyList()
)

/**
 * A multi-day fitness challenge.
 */
data class Challenge(
    val id: String,
    val name: String,
    val description: String,
    val totalDays: Int,
    val difficulty: String,
    val days: List<ChallengeDay>
)

/**
 * Pre-built challenges.
 */
object Challenges {

    // ── helpers ───────────────────────────────────────────────────────
    private fun restDay(d: Int) = ChallengeDay(day = d, isRestDay = true)

    private fun chestDay(d: Int, reps: Int, sets: Int) = ChallengeDay(
        day = d, exercises = listOf(
            PlanExercise("push-up", targetReps = reps, sets = sets, restSeconds = 30),
            PlanExercise("plank", targetHoldSeconds = 20 + d, sets = 2, restSeconds = 20)
        )
    )

    private fun absDay(d: Int, reps: Int, sets: Int) = ChallengeDay(
        day = d, exercises = listOf(
            PlanExercise("plank", targetHoldSeconds = 20 + d * 2, sets = sets, restSeconds = 20),
            PlanExercise("leg raises", targetReps = reps, sets = sets, restSeconds = 25),
            PlanExercise("russian twist", targetReps = reps, sets = sets, restSeconds = 20)
        )
    )

    private fun fatBurnDay(d: Int, reps: Int, sets: Int) = ChallengeDay(
        day = d, exercises = listOf(
            PlanExercise("squat", targetReps = reps, sets = sets, restSeconds = 20),
            PlanExercise("push-up", targetReps = reps, sets = sets, restSeconds = 20),
            PlanExercise("russian twist", targetReps = reps, sets = sets, restSeconds = 15),
            PlanExercise("plank", targetHoldSeconds = 20 + d, sets = 2, restSeconds = 15)
        )
    )

    private fun beginnerDay(d: Int, reps: Int, sets: Int) = ChallengeDay(
        day = d, exercises = listOf(
            PlanExercise("squat", targetReps = reps, sets = sets, restSeconds = 30),
            PlanExercise("push-up", targetReps = (reps * 0.6).toInt().coerceAtLeast(4), sets = sets, restSeconds = 30),
            PlanExercise("plank", targetHoldSeconds = 15 + d * 2, sets = 2, restSeconds = 20)
        )
    )

    // ── 30-Day Chest Challenge ───────────────────────────────────────
    private val chest30 = Challenge(
        id = "chest_30",
        name = "30-Day Chest Challenge",
        description = "Build chest strength and endurance with progressive push-up training",
        totalDays = 30,
        difficulty = "Intermediate",
        days = (1..30).map { d ->
            if (d % 7 == 0) restDay(d)
            else chestDay(d, reps = 8 + d / 2, sets = if (d < 15) 3 else 4)
        }
    )

    // ── 30-Day Abs Transformation ────────────────────────────────────
    private val abs30 = Challenge(
        id = "abs_30",
        name = "30-Day Abs Transformation",
        description = "Sculpt your core with planks, leg raises, and russian twists",
        totalDays = 30,
        difficulty = "Intermediate",
        days = (1..30).map { d ->
            if (d % 7 == 0) restDay(d)
            else absDay(d, reps = 8 + d / 3, sets = if (d < 15) 2 else 3)
        }
    )

    // ── 21-Day Fat Burn ──────────────────────────────────────────────
    private val fatBurn21 = Challenge(
        id = "fat_burn_21",
        name = "21-Day Fat Burn",
        description = "High-intensity full body workouts to maximize calorie burn",
        totalDays = 21,
        difficulty = "Advanced",
        days = (1..21).map { d ->
            if (d % 7 == 0) restDay(d)
            else fatBurnDay(d, reps = 10 + d / 2, sets = 3)
        }
    )

    // ── 14-Day Beginner ──────────────────────────────────────────────
    private val beginner14 = Challenge(
        id = "beginner_14",
        name = "Beginner 14-Day Fitness Plan",
        description = "Ease into fitness with gentle, progressive daily workouts",
        totalDays = 14,
        difficulty = "Beginner",
        days = (1..14).map { d ->
            if (d % 7 == 0) restDay(d)
            else beginnerDay(d, reps = 6 + d, sets = 2)
        }
    )

    val all: List<Challenge> = listOf(chest30, abs30, fatBurn21, beginner14)

    fun getById(id: String): Challenge? = all.find { it.id == id }
}
