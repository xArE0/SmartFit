package com.example.project_smartfit.data

/**
 * A single exercise within a plan, with its target metrics.
 */
data class PlanExercise(
    val name: String,
    val targetReps: Int = 0,       // 0 → use holdSeconds instead
    val targetHoldSeconds: Int = 0,
    val sets: Int = 3,
    val restSeconds: Int = 30
)

/**
 * A curated workout plan with a difficulty level and a list of exercises.
 */
data class ExercisePlan(
    val id: String,
    val name: String,
    val description: String,
    val difficulty: String,        // "Beginner", "Intermediate", "Advanced"
    val estimatedMinutes: Int,
    val exercises: List<PlanExercise>
)

/**
 * Pre-built exercise plan templates.
 */
object ExercisePlans {

    val all: List<ExercisePlan> = listOf(
        ExercisePlan(
            id = "beginner_full_body",
            name = "Beginner Full Body",
            description = "A balanced introduction to all major movement patterns. Perfect for getting started.",
            difficulty = "Beginner",
            estimatedMinutes = 20,
            exercises = listOf(
                PlanExercise("squat", targetReps = 10, sets = 3, restSeconds = 30),
                PlanExercise("push-up", targetReps = 8, sets = 3, restSeconds = 30),
                PlanExercise("plank", targetHoldSeconds = 30, sets = 3, restSeconds = 20),
                PlanExercise("leg raises", targetReps = 10, sets = 2, restSeconds = 30)
            )
        ),
        ExercisePlan(
            id = "core_blast",
            name = "Core Blast",
            description = "Target your abs and core stability with this focused routine.",
            difficulty = "Intermediate",
            estimatedMinutes = 15,
            exercises = listOf(
                PlanExercise("plank", targetHoldSeconds = 45, sets = 3, restSeconds = 20),
                PlanExercise("leg raises", targetReps = 12, sets = 3, restSeconds = 25),
                PlanExercise("russian twist", targetReps = 15, sets = 3, restSeconds = 20)
            )
        ),
        ExercisePlan(
            id = "upper_body_power",
            name = "Upper Body Power",
            description = "Build strength in your arms, shoulders, and chest.",
            difficulty = "Intermediate",
            estimatedMinutes = 25,
            exercises = listOf(
                PlanExercise("push-up", targetReps = 12, sets = 4, restSeconds = 30),
                PlanExercise("hammer curl", targetReps = 12, sets = 3, restSeconds = 30),
                PlanExercise("lateral raise", targetReps = 10, sets = 3, restSeconds = 25),
                PlanExercise("plank", targetHoldSeconds = 40, sets = 2, restSeconds = 20)
            )
        ),
        ExercisePlan(
            id = "leg_day",
            name = "Leg Day",
            description = "Strengthen your lower body with squats and targeted leg work.",
            difficulty = "Intermediate",
            estimatedMinutes = 20,
            exercises = listOf(
                PlanExercise("squat", targetReps = 15, sets = 4, restSeconds = 30),
                PlanExercise("leg raises", targetReps = 12, sets = 3, restSeconds = 25),
                PlanExercise("plank", targetHoldSeconds = 45, sets = 2, restSeconds = 20),
                PlanExercise("russian twist", targetReps = 12, sets = 3, restSeconds = 25)
            )
        )
    )
}
