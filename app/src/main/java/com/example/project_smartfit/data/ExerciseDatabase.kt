package com.example.project_smartfit.data

/**
 * Static info for every exercise the ML model supports.
 * Used by ExerciseInfoScreen and the animation player.
 */
data class ExerciseInfo(
    val id: String,
    val displayName: String,
    val animationFile: String,          // filename inside assets/animation/
    val targetMuscles: List<String>,
    val difficulty: String,
    val instructions: List<String>,
    val commonMistakes: List<String>,
    val breathingTechnique: String
)

object ExerciseDatabase {

    private val exercises = listOf(
        ExerciseInfo(
            id = "push-up",
            displayName = "Push-Up",
            animationFile = "pushup.mp4",
            targetMuscles = listOf("Chest", "Triceps", "Core", "Shoulders"),
            difficulty = "Beginner",
            instructions = listOf(
                "Start in a high plank with hands slightly wider than shoulders",
                "Keep your body in a straight line from head to heels",
                "Lower your chest until it nearly touches the floor",
                "Push back up to the starting position with control"
            ),
            commonMistakes = listOf(
                "Sagging hips — engage your core to keep body straight",
                "Flaring elbows too wide — keep them at ~45° angle",
                "Not going low enough — aim for chest near the floor",
                "Locking elbows at the top — keep a slight bend"
            ),
            breathingTechnique = "Inhale as you lower your body. Exhale as you push up."
        ),
        ExerciseInfo(
            id = "squat",
            displayName = "Squat",
            animationFile = "squat.mp4",
            targetMuscles = listOf("Quadriceps", "Glutes", "Hamstrings", "Core"),
            difficulty = "Beginner",
            instructions = listOf(
                "Stand with feet shoulder-width apart, toes slightly out",
                "Push your hips back and bend knees as if sitting in a chair",
                "Lower until thighs are parallel to the floor",
                "Drive through your heels to stand back up"
            ),
            commonMistakes = listOf(
                "Knees caving inward — push them out over your toes",
                "Leaning too far forward — keep chest up and back straight",
                "Heels lifting off the ground — shift weight to heels",
                "Not reaching parallel — go deeper if mobility allows"
            ),
            breathingTechnique = "Inhale as you squat down. Exhale as you stand up."
        ),
        ExerciseInfo(
            id = "plank",
            displayName = "Plank",
            animationFile = "plank.mp4",
            targetMuscles = listOf("Core", "Shoulders", "Back", "Glutes"),
            difficulty = "Beginner",
            instructions = listOf(
                "Place forearms on the ground, elbows under shoulders",
                "Extend legs back, balancing on toes",
                "Keep body in a straight line from head to heels",
                "Hold the position while breathing steadily"
            ),
            commonMistakes = listOf(
                "Hips sagging — squeeze glutes and engage core",
                "Hips too high — lower to create a straight line",
                "Looking up — keep neck neutral, gaze at floor",
                "Holding breath — breathe normally throughout"
            ),
            breathingTechnique = "Breathe steadily — inhale for 3 counts, exhale for 3 counts. Do not hold your breath."
        ),
        ExerciseInfo(
            id = "leg raises",
            displayName = "Leg Raises",
            animationFile = "legraises.mp4",
            targetMuscles = listOf("Lower Abs", "Hip Flexors", "Core"),
            difficulty = "Intermediate",
            instructions = listOf(
                "Lie flat on your back with legs straight",
                "Place hands under your hips or by your sides",
                "Raise both legs to 90° while keeping them straight",
                "Lower slowly without letting feet touch the floor"
            ),
            commonMistakes = listOf(
                "Lower back arching — press lower back into the floor",
                "Using momentum — lift and lower with control",
                "Bending knees — keep legs as straight as possible",
                "Dropping legs too fast — control the descent"
            ),
            breathingTechnique = "Exhale as you raise your legs. Inhale as you lower them."
        ),
        ExerciseInfo(
            id = "russian twist",
            displayName = "Russian Twist",
            animationFile = "russian-twist.mp4",
            targetMuscles = listOf("Obliques", "Core", "Hip Flexors"),
            difficulty = "Intermediate",
            instructions = listOf(
                "Sit on the floor with knees bent, feet slightly elevated",
                "Lean back slightly to engage core",
                "Clasp hands together or hold a weight",
                "Rotate torso side to side, touching the ground each side"
            ),
            commonMistakes = listOf(
                "Moving only arms — rotate your entire torso",
                "Rounding back excessively — keep chest lifted",
                "Going too fast — use controlled, deliberate movements",
                "Holding breath — breathe with each rotation"
            ),
            breathingTechnique = "Exhale as you twist to each side. Inhale as you pass through center."
        ),
        ExerciseInfo(
            id = "hammer curl",
            displayName = "Hammer Curl",
            animationFile = "hammer_curl.mp4",
            targetMuscles = listOf("Biceps", "Brachialis", "Forearms"),
            difficulty = "Beginner",
            instructions = listOf(
                "Stand with dumbbells at your sides, palms facing inward",
                "Keep upper arms stationary against your body",
                "Curl the weights up by bending at the elbows",
                "Lower with control back to starting position"
            ),
            commonMistakes = listOf(
                "Swinging body for momentum — keep torso still",
                "Moving elbows forward — pin upper arms to sides",
                "Curling too fast — use a 2-second up, 2-second down tempo",
                "Gripping too tight — maintain a firm but relaxed grip"
            ),
            breathingTechnique = "Exhale as you curl up. Inhale as you lower the weight."
        ),
        ExerciseInfo(
            id = "lateral raise",
            displayName = "Lateral Raise",
            animationFile = "lateral_raise.mp4",
            targetMuscles = listOf("Lateral Deltoids", "Traps", "Shoulders"),
            difficulty = "Beginner",
            instructions = listOf(
                "Stand with dumbbells at your sides, slight lean forward",
                "Raise both arms out to the sides until shoulder height",
                "Keep a slight bend in your elbows",
                "Lower slowly back to starting position"
            ),
            commonMistakes = listOf(
                "Using too much weight — start light for proper form",
                "Shrugging shoulders — keep them down and relaxed",
                "Raising arms too high — stop at shoulder level",
                "Swinging the weights — use controlled movement"
            ),
            breathingTechnique = "Exhale as you raise your arms. Inhale as you lower them."
        )
    )

    /** Lookup by exercise ID (e.g. "push-up", "squat") */
    fun getInfo(exerciseId: String): ExerciseInfo? =
        exercises.find { it.id.equals(exerciseId, ignoreCase = true) }

    /** All exercises */
    fun getAll(): List<ExerciseInfo> = exercises

    /** Map exercise ID → asset file name */
    fun animationAsset(exerciseId: String): String {
        return getInfo(exerciseId)?.animationFile ?: "pushup.mp4"
    }
}
