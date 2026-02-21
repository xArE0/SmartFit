package com.example.project_smartfit.camera

import android.Manifest
import android.util.Log
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import com.example.project_smartfit.NavExerciseCamera
import com.example.project_smartfit.NavHomepage
import com.example.project_smartfit.NavWorkoutSummary
import com.example.project_smartfit.data.ExercisePlan
import com.example.project_smartfit.data.PlanExercise
import com.example.project_smartfit.data.WorkoutRepository
import com.example.project_smartfit.data.WorkoutSession
import com.example.project_smartfit.ml.ExerciseClassifier
import com.example.project_smartfit.ml.ExerciseFeedbackEngine
import com.example.project_smartfit.ml.PoseLandmarkerHelper
import com.example.project_smartfit.ml.RepCounter
import com.example.project_smartfit.ml.VoiceFeedbackManager
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.gson.Gson
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import kotlinx.coroutines.delay
import java.util.concurrent.Executors

// ── Phase of the workout ────────────────────────────────────────────
private enum class WorkoutPhase {
    SCANNING,           // Free mode: detect any exercise
    LOCKED_IN,          // Free mode: counting reps for detected exercise
    PLAN_ACTIVE,        // Plan mode: doing current exercise
    PLAN_REST,          // Plan mode: rest timer between sets/exercises
    PLAN_TRANSITION,    // Plan mode: "Great! Next: …" overlay
    PLAN_COMPLETE       // Plan mode: all exercises done
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraScreen(navController: NavController, planJson: String = "") {
    val cameraPermission = rememberPermissionState(Manifest.permission.CAMERA)

    LaunchedEffect(Unit) {
        if (!cameraPermission.status.isGranted) {
            cameraPermission.launchPermissionRequest()
        }
    }

    if (cameraPermission.status.isGranted) {
        CameraContent(navController, planJson)
    } else {
        PermissionDeniedContent(
            onRequestPermission = { cameraPermission.launchPermissionRequest() },
            onBack = { navController.popBackStack() }
        )
    }
}

@Composable
private fun PermissionDeniedContent(
    onRequestPermission: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Camera Permission Required", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text(
            "SmartFit needs camera access to detect your exercise form.",
            color = Color.Gray, fontSize = 14.sp,
            modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
        )
        Button(onClick = onRequestPermission) { Text("Grant Permission") }
        Button(onClick = onBack, modifier = Modifier.padding(top = 8.dp)) { Text("Go Back") }
    }
}

@Composable
private fun CameraContent(navController: NavController, planJson: String) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val gson = remember { Gson() }

    // ── Deserialize plan (if any) ────────────────────────────────────
    val plan: ExercisePlan? = remember(planJson) {
        if (planJson.isNotBlank()) {
            try { gson.fromJson(planJson, ExercisePlan::class.java) } catch (_: Exception) { null }
        } else null
    }
    val hasPlan = plan != null
    val planExercises: List<PlanExercise> = plan?.exercises ?: emptyList()

    // ── Core state ───────────────────────────────────────────────────
    var poseResult by remember { mutableStateOf<PoseLandmarkerResult?>(null) }
    var imageWidth by remember { mutableIntStateOf(0) }
    var imageHeight by remember { mutableIntStateOf(0) }
    var useFrontCamera by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // ── Workout flow state ───────────────────────────────────────────
    var workoutPhase by remember {
        mutableStateOf(if (hasPlan) WorkoutPhase.PLAN_ACTIVE else WorkoutPhase.SCANNING)
    }
    var lockedExercise by remember { mutableStateOf(if (hasPlan) planExercises.firstOrNull()?.name ?: "" else "") }
    var lockedDisplayLabel by remember {
        mutableStateOf(
            if (hasPlan) planExercises.firstOrNull()?.name?.split(" ")
                ?.joinToString(" ") { w -> w.replaceFirstChar { it.uppercaseChar() } } ?: ""
            else ""
        )
    }

    // ── Plan mode state ──────────────────────────────────────────────
    var currentPlanIndex by remember { mutableIntStateOf(0) }
    var currentSet by remember { mutableIntStateOf(1) }
    var restCountdown by remember { mutableIntStateOf(0) }
    var transitionMessage by remember { mutableStateOf("") }

    // Track per-exercise sessions for summary
    val completedSessions = remember { mutableListOf<WorkoutSession>() }

    // ── Scanning / convergence state ─────────────────────────────────
    val classificationVotes = remember { mutableMapOf<String, Int>() }
    var scanStartTimeMs by remember { mutableLongStateOf(0L) }
    var currentLabel by remember { mutableStateOf("") }
    // Best-guess label during scanning — used for speculative rep counting
    var candidateLabel by remember { mutableStateOf("") }

    // ── Workout state ────────────────────────────────────────────────
    var repCount by remember { mutableIntStateOf(0) }
    var holdSeconds by remember { mutableIntStateOf(0) }
    var feedbackState by remember { mutableStateOf(ExerciseFeedbackEngine.Feedback.NONE) }
    var feedbackFrameCounter by remember { mutableIntStateOf(0) }

    // ── Plan mode: wrong-exercise detection ──────────────────────────
    // Classifier runs every WRONG_EXERCISE_CHECK_INTERVAL frames in plan mode
    // to verify the user is doing the correct exercise.
    var wrongExerciseFrameCounter by remember { mutableIntStateOf(0) }
    var wrongExerciseStrikes by remember { mutableIntStateOf(0) }
    var wrongExerciseWarning by remember { mutableStateOf("") }

    // ── Session tracking for saving reports ───────────────────────────
    val workoutRepo = remember { WorkoutRepository.getInstance(context) }
    var sessionStartMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var goodFormFrames by remember { mutableIntStateOf(0) }
    var totalFormFrames by remember { mutableIntStateOf(0) }
    val collectedTips = remember { mutableSetOf<String>() }
    val tipCountMap = remember { mutableMapOf<String, Int>() }

    // ── ML helpers ───────────────────────────────────────────────────
    val exerciseClassifier = remember { ExerciseClassifier(context) }
    val repCounter = remember { RepCounter() }
    val voiceFeedback = remember { VoiceFeedbackManager(context) }

    val FEEDBACK_VOICE_INTERVAL = 15
    val CONVERGENCE_THRESHOLD = 3
    val MIN_SCAN_TIME_MS = 3_000L
    val MAX_SCAN_TIME_MS = 10_000L
    val WRONG_EXERCISE_CHECK_INTERVAL = 90 // frames (~3s at 30fps)
    val WRONG_EXERCISE_STRIKES_NEEDED = 2  // consecutive wrong checks before warning

    // ── Pose visibility gate (fixes erratic overlay bug) ─────────────
    var isPoseVisible by remember { mutableStateOf(true) }
    var poseHiddenSince by remember { mutableLongStateOf(0L) }
    var lastSpokenTip by remember { mutableStateOf("") }
    var lastTipTimeMs by remember { mutableLongStateOf(0L) }
    val POSE_HIDDEN_DEBOUNCE_MS = 500L
    val TIP_COOLDOWN_MS = 8_000L

    /**
     * Check that key landmarks are visible enough for accurate tracking.
     * Returns false if the user's body isn't fully in frame.
     */
    fun isPoseValid(
        landmarks: List<com.google.mediapipe.tasks.components.containers.NormalizedLandmark>,
        exercise: String
    ): Boolean {
        val REQUIRED_VIS = 0.65f
        val coreIndices = listOf(11, 12, 23, 24) // shoulders + hips always needed
        val exerciseExtra = when (exercise) {
            "squat" -> listOf(25, 26, 27, 28)
            "push-up" -> listOf(13, 14, 15, 16)
            "hammer curl", "lateral raise" -> listOf(13, 14, 15, 16)
            "plank" -> listOf(25, 26, 27, 28)
            "leg raises" -> listOf(23, 24, 25, 26)
            "russian twist" -> listOf(11, 12, 13, 14)
            else -> emptyList()
        }
        val required = (coreIndices + exerciseExtra).distinct()
        return required.all { idx ->
            idx < landmarks.size &&
                    (landmarks[idx].visibility().orElse(0f)) >= REQUIRED_VIS
        }
    }

    // ── Helper: save current exercise session ────────────────────────
    fun saveCurrentExercise(): WorkoutSession {
        val durationSec = ((System.currentTimeMillis() - sessionStartMs) / 1000).toInt()
        val formScore = if (totalFormFrames > 0)
            ((goodFormFrames.toFloat() / totalFormFrames) * 100).toInt()
        else 100

        val session = WorkoutSession(
            exercise = lockedExercise,
            reps = repCount,
            holdSeconds = holdSeconds,
            durationSeconds = durationSec,
            formScore = formScore,
            formTips = collectedTips.toList(),
            formTipCounts = tipCountMap.toMap()
        )
        workoutRepo.saveSession(session)
        completedSessions.add(session)
        return session
    }

    // ── Helper: reset exercise state for next exercise ───────────────
    fun resetForNextExercise() {
        repCount = 0
        holdSeconds = 0
        feedbackState = ExerciseFeedbackEngine.Feedback.NONE
        feedbackFrameCounter = 0
        sessionStartMs = System.currentTimeMillis()
        goodFormFrames = 0
        totalFormFrames = 0
        collectedTips.clear()
        tipCountMap.clear()
        repCounter.reset()
        voiceFeedback.reset()
        wrongExerciseFrameCounter = 0
        wrongExerciseStrikes = 0
        wrongExerciseWarning = ""
    }

    // ── Helper: advance to next exercise in plan ─────────────────────
    fun advancePlan() {
        val currentExercise = planExercises[currentPlanIndex]
        if (currentSet < currentExercise.sets) {
            // More sets remaining → rest then same exercise
            currentSet++
            restCountdown = currentExercise.restSeconds
            workoutPhase = WorkoutPhase.PLAN_REST
            resetForNextExercise()
        } else {
            // All sets done for this exercise → save and move to next
            saveCurrentExercise()
            currentSet = 1
            if (currentPlanIndex < planExercises.size - 1) {
                val nextExercise = planExercises[currentPlanIndex + 1]
                transitionMessage = "Next: ${nextExercise.name.split(" ")
                    .joinToString(" ") { w -> w.replaceFirstChar { it.uppercaseChar() } }}"
                workoutPhase = WorkoutPhase.PLAN_TRANSITION
            } else {
                workoutPhase = WorkoutPhase.PLAN_COMPLETE
            }
        }
    }

    // ── Plan rest timer ──────────────────────────────────────────────
    LaunchedEffect(workoutPhase, restCountdown) {
        if (workoutPhase == WorkoutPhase.PLAN_REST && restCountdown > 0) {
            delay(1000L)
            restCountdown--
        } else if (workoutPhase == WorkoutPhase.PLAN_REST && restCountdown <= 0) {
            // Rest done → resume exercise
            workoutPhase = WorkoutPhase.PLAN_ACTIVE
            voiceFeedback.announceExercise("Set $currentSet")
        }
    }

    // ── Plan transition timer (3s) ───────────────────────────────────
    LaunchedEffect(workoutPhase) {
        if (workoutPhase == WorkoutPhase.PLAN_TRANSITION) {
            voiceFeedback.announceExercise(transitionMessage)
            delay(3000L)
            currentPlanIndex++
            val nextExercise = planExercises[currentPlanIndex]
            lockedExercise = nextExercise.name
            lockedDisplayLabel = nextExercise.name.split(" ")
                .joinToString(" ") { w -> w.replaceFirstChar { it.uppercaseChar() } }
            resetForNextExercise()
            workoutPhase = WorkoutPhase.PLAN_ACTIVE
            voiceFeedback.announceExercise(lockedDisplayLabel)
        }
    }

    // ── Plan complete → navigate to summary ──────────────────────────
    LaunchedEffect(workoutPhase) {
        if (workoutPhase == WorkoutPhase.PLAN_COMPLETE) {
            voiceFeedback.announceExercise("Workout complete! Great job!")
            delay(1500L)
            val summaryJson = gson.toJson(completedSessions.toList())
            navController.navigate(NavWorkoutSummary(summaryJson)) {
                popUpTo(NavExerciseCamera::class) { inclusive = true }
            }
        }
    }

    // ── Announce first exercise in plan ──────────────────────────────
    LaunchedEffect(hasPlan) {
        if (hasPlan && lockedDisplayLabel.isNotEmpty()) {
            voiceFeedback.announceExercise(lockedDisplayLabel)
        }
    }

    val poseLandmarkerHelper = remember {
        PoseLandmarkerHelper(
            context = context,
            listener = object : PoseLandmarkerHelper.LandmarkerListener {
                override fun onResults(
                    result: PoseLandmarkerResult,
                    imgHeight: Int,
                    imgWidth: Int
                ) {
                    poseResult = result
                    imageWidth = imgWidth
                    imageHeight = imgHeight

                    val landmarks = result.landmarks().firstOrNull() ?: return
                    if (landmarks.isEmpty()) return

                    when (workoutPhase) {
                        WorkoutPhase.SCANNING -> {
                            // ── Speculative rep counting ───────────────────────────────
                            // Run repCounter every frame using the current best-guess label
                            // so reps done during the scan window are NOT lost at lock-in.
                            if (candidateLabel.isNotEmpty()) {
                                val speculativeRep = repCounter.update(candidateLabel, landmarks)
                                repCount = repCounter.repCount
                                holdSeconds = repCounter.holdSeconds
                                if (speculativeRep) voiceFeedback.announceRep(repCount)
                            }

                            exerciseClassifier.addFrameAndClassify(landmarks) { classification ->
                                if (classification.isConfident) {
                                    val label = classification.label
                                    currentLabel = classification.displayLabel

                                    val votes = classificationVotes.getOrDefault(label, 0) + 1
                                    classificationVotes[label] = votes

                                    // Update candidate label to the current top-voted exercise
                                    val topCandidate = classificationVotes.maxByOrNull { it.value }?.key ?: label
                                    if (topCandidate != candidateLabel) {
                                        // Exercise guess changed — reset speculative count
                                        candidateLabel = topCandidate
                                        repCounter.reset()
                                        repCount = 0
                                    }

                                    if (scanStartTimeMs == 0L) {
                                        scanStartTimeMs = System.currentTimeMillis()
                                    }

                                    val elapsed = System.currentTimeMillis() - scanStartTimeMs

                                    val shouldLock = (votes >= CONVERGENCE_THRESHOLD && elapsed >= MIN_SCAN_TIME_MS) ||
                                            (elapsed >= MAX_SCAN_TIME_MS && classificationVotes.isNotEmpty())

                                    if (shouldLock) {
                                        val best = classificationVotes.maxByOrNull { it.value }
                                        if (best != null) {
                                            lockedExercise = best.key
                                            lockedDisplayLabel = best.key.split(" ")
                                                .joinToString(" ") { w -> w.replaceFirstChar { it.uppercaseChar() } }
                                            workoutPhase = WorkoutPhase.LOCKED_IN

                                            // ── Carry speculative reps forward if the label matches ──
                                            // Only reset if the locked exercise differs from what we
                                            // were speculatively counting — otherwise keep the count.
                                            if (lockedExercise != candidateLabel) {
                                                repCounter.reset()
                                                repCount = 0
                                            }
                                            // candidateLabel stays set — no need to clear

                                            sessionStartMs = System.currentTimeMillis()
                                            goodFormFrames = 0
                                            totalFormFrames = 0
                                            collectedTips.clear()
                                            tipCountMap.clear()
                                            voiceFeedback.announceExercise(lockedDisplayLabel)
                                        }
                                    }
                                }
                            }
                        }

                        WorkoutPhase.LOCKED_IN, WorkoutPhase.PLAN_ACTIVE -> {
                            // ── Pose visibility gate ───────────────────
                            val poseOk = isPoseValid(landmarks, lockedExercise)
                            if (!poseOk) {
                                if (poseHiddenSince == 0L) poseHiddenSince = System.currentTimeMillis()
                                val hiddenFor = System.currentTimeMillis() - poseHiddenSince
                                if (hiddenFor >= POSE_HIDDEN_DEBOUNCE_MS) {
                                    isPoseVisible = false
                                }
                                return  // skip all ML processing this frame
                            }
                            // Pose is visible again
                            poseHiddenSince = 0L
                            isPoseVisible = true

                            // Rep counting
                            val newRep = repCounter.update(lockedExercise, landmarks)
                            repCount = repCounter.repCount
                            holdSeconds = repCounter.holdSeconds

                            if (newRep) {
                                voiceFeedback.announceRep(repCount)
                            }

                            // Plank hold announcements
                            if (lockedExercise == "plank") {
                                voiceFeedback.announceHold(holdSeconds)
                            }

                            // Throttled form feedback with cooldown
                            feedbackFrameCounter++
                            if (feedbackFrameCounter >= FEEDBACK_VOICE_INTERVAL) {
                                feedbackFrameCounter = 0
                                // Pass repCounter.phase so feedback engine can gate phase-specific tips
                                val fb = ExerciseFeedbackEngine.analyse(
                                    lockedExercise, landmarks, repCounter.phase
                                )
                                feedbackState = fb

                                totalFormFrames++
                                if (fb.isGoodForm) goodFormFrames++
                                if (!fb.isGoodForm) {
                                    collectedTips.addAll(fb.tips)
                                    fb.tips.forEach { tip ->
                                        tipCountMap[tip] = (tipCountMap[tip] ?: 0) + 1
                                    }
                                }

                                if (!fb.isGoodForm && fb.tips.isNotEmpty()) {
                                    val tip = fb.tips.first()
                                    val now = System.currentTimeMillis()
                                    if (tip != lastSpokenTip || (now - lastTipTimeMs) >= TIP_COOLDOWN_MS) {
                                        voiceFeedback.speakTip(tip)
                                        lastSpokenTip = tip
                                        lastTipTimeMs = now
                                    }
                                }
                            }

                            // ── Plan mode: wrong-exercise detector ────────────────────
                            // Instead of running the classifier every frame (wasteful), we
                            // run it once every WRONG_EXERCISE_CHECK_INTERVAL frames to
                            // verify the user is actually doing the planned exercise.
                            if (workoutPhase == WorkoutPhase.PLAN_ACTIVE) {
                                wrongExerciseFrameCounter++
                                if (wrongExerciseFrameCounter >= WRONG_EXERCISE_CHECK_INTERVAL) {
                                    wrongExerciseFrameCounter = 0
                                    exerciseClassifier.addFrameAndClassify(landmarks) { classification ->
                                        if (classification.isConfident &&
                                            classification.confidence >= 0.60f &&
                                            classification.label != lockedExercise
                                        ) {
                                            wrongExerciseStrikes++
                                            if (wrongExerciseStrikes >= WRONG_EXERCISE_STRIKES_NEEDED) {
                                                wrongExerciseWarning =
                                                    "Wrong exercise? Expected: $lockedDisplayLabel"
                                            }
                                        } else {
                                            // User is doing the right thing — clear warning
                                            wrongExerciseStrikes = 0
                                            wrongExerciseWarning = ""
                                        }
                                    }
                                }
                            }

                            // ── Plan mode: auto-advance when target reached ──
                            if (workoutPhase == WorkoutPhase.PLAN_ACTIVE && currentPlanIndex < planExercises.size) {
                                val target = planExercises[currentPlanIndex]
                                val targetReached = if (target.targetHoldSeconds > 0) {
                                    holdSeconds >= target.targetHoldSeconds
                                } else {
                                    repCount >= target.targetReps
                                }
                                if (targetReached) {
                                    advancePlan()
                                }
                            }
                        }

                        else -> { /* PLAN_REST, PLAN_TRANSITION, PLAN_COMPLETE — no ML processing */ }
                    }
                }

                override fun onError(error: String) {
                    errorMessage = error
                }
            }
        )
    }

    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    // ── Cleanup ──────────────────────────────────────────────────────
    DisposableEffect(Unit) {
        onDispose {
            poseLandmarkerHelper.close()
            exerciseClassifier.close()
            voiceFeedback.shutdown()
            cameraExecutor.shutdown()
        }
    }

    // ── UI ───────────────────────────────────────────────────────────
    Box(modifier = Modifier.fillMaxSize()) {

        // Camera Preview
        key(useFrontCamera) {
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx).apply {
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                        implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                    }

                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()

                        val preview = Preview.Builder()
                            .build()
                            .also { it.surfaceProvider = previewView.surfaceProvider }

                        val imageAnalysis = ImageAnalysis.Builder()
                            .setTargetResolution(Size(480, 640))
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                            .build()
                            .also { analysis ->
                                analysis.setAnalyzer(cameraExecutor) { imageProxy: ImageProxy ->
                                    poseLandmarkerHelper.detectAsync(imageProxy)
                                    imageProxy.close()
                                }
                            }

                        val cameraSelector = if (useFrontCamera) {
                            CameraSelector.DEFAULT_FRONT_CAMERA
                        } else {
                            CameraSelector.DEFAULT_BACK_CAMERA
                        }

                        try {
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner, cameraSelector, preview, imageAnalysis
                            )
                        } catch (e: Exception) {
                            Log.e("CameraScreen", "Camera bind failed", e)
                        }
                    }, ContextCompat.getMainExecutor(ctx))

                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // Pose overlay
        PoseOverlayView(
            result = poseResult,
            imageWidth = imageWidth,
            imageHeight = imageHeight,
            isFrontCamera = useFrontCamera,
            isPoseVisible = isPoseVisible,
            modifier = Modifier.fillMaxSize()
        )

        // ── "Position yourself" overlay when pose is not visible ───────
        if (!isPoseVisible && (workoutPhase == WorkoutPhase.LOCKED_IN || workoutPhase == WorkoutPhase.PLAN_ACTIVE)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center)
                    .padding(horizontal = 32.dp)
                    .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(16.dp))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("⚠️", fontSize = 32.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Position your full body in frame",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Tracking paused",
                        color = Color(0xFFFF9800),
                        fontSize = 13.sp
                    )
                }
            }
        }

        // ── Wrong-exercise warning overlay (plan mode) ────────────────
        if (wrongExerciseWarning.isNotEmpty() && workoutPhase == WorkoutPhase.PLAN_ACTIVE) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(start = 24.dp, top = 56.dp, end = 24.dp)
                    .background(Color(0xFFE65100).copy(alpha = 0.90f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "⚠️ $wrongExerciseWarning",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }

        // ── Animation overlay (plan mode + free locked-in mode) ──────
        if (lockedExercise.isNotEmpty() &&
            (workoutPhase == WorkoutPhase.LOCKED_IN ||
             workoutPhase == WorkoutPhase.PLAN_ACTIVE ||
             workoutPhase == WorkoutPhase.PLAN_REST)) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(top = 48.dp, end = 12.dp)
            ) {
                ExerciseAnimationPlayer(
                    exerciseId = lockedExercise,
                    modifier = Modifier
                        .size(100.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
            }
        }

        // ── Top bar ──────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
            }
            // Plan progress indicator
            if (hasPlan) {
                Text(
                    text = "${currentPlanIndex + 1}/${planExercises.size}",
                    color = Color(0xFF00E676),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
            if (workoutPhase == WorkoutPhase.SCANNING) {
                IconButton(onClick = { useFrontCamera = !useFrontCamera }) {
                    Icon(Icons.Default.Cameraswitch, "Switch Camera", tint = Color.White)
                }
            } else {
                Spacer(modifier = Modifier.size(48.dp))
            }
        }

        // ── Bottom HUD ───────────────────────────────────────────────
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 24.dp, vertical = 24.dp)
                .fillMaxWidth()
                .background(
                    Color.Black.copy(alpha = 0.80f),
                    RoundedCornerShape(16.dp)
                )
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (workoutPhase) {
                WorkoutPhase.SCANNING -> {
                    ScanningHud(currentLabel)
                }

                WorkoutPhase.LOCKED_IN -> {
                    WorkoutHud(
                        exerciseName = lockedDisplayLabel,
                        isPlank = lockedExercise == "plank",
                        repCount = repCount,
                        holdSeconds = holdSeconds,
                        feedback = feedbackState,
                        targetReps = 0,
                        targetHold = 0,
                        setInfo = null,
                        onEndExercise = {
                            val session = saveCurrentExercise()
                            // Free mode: go directly to summary screen
                            val summaryJson = gson.toJson(listOf(session))
                            navController.navigate(NavWorkoutSummary(summaryJson)) {
                                popUpTo(NavExerciseCamera::class) { inclusive = true }
                            }
                        }
                    )
                }

                WorkoutPhase.PLAN_ACTIVE -> {
                    val target = planExercises.getOrNull(currentPlanIndex)
                    WorkoutHud(
                        exerciseName = lockedDisplayLabel,
                        isPlank = lockedExercise == "plank",
                        repCount = repCount,
                        holdSeconds = holdSeconds,
                        feedback = feedbackState,
                        targetReps = target?.targetReps ?: 0,
                        targetHold = target?.targetHoldSeconds ?: 0,
                        setInfo = "Set $currentSet of ${target?.sets ?: 1}",
                        onEndExercise = {
                            // Manual skip to next
                            advancePlan()
                        }
                    )
                }

                WorkoutPhase.PLAN_REST -> {
                    RestHud(
                        secondsLeft = restCountdown,
                        nextLabel = lockedDisplayLabel,
                        setInfo = "Set $currentSet of ${planExercises.getOrNull(currentPlanIndex)?.sets ?: 1}",
                        onSkip = {
                            restCountdown = 0
                        }
                    )
                }

                WorkoutPhase.PLAN_TRANSITION -> {
                    TransitionHud(message = transitionMessage)
                }

                WorkoutPhase.PLAN_COMPLETE -> {
                    Text(
                        "🎉 Workout Complete!",
                        color = Color(0xFF00E676),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Saving results…",
                        color = Color.Gray,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }

        // Error message
        errorMessage?.let { msg ->
            Text(
                text = msg,
                color = Color.Red,
                fontSize = 12.sp,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 80.dp)
                    .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                    .padding(8.dp)
            )
        }
    }
}

// ── Scanning HUD ─────────────────────────────────────────────────────

@Composable
private fun ScanningHud(currentLabel: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(20.dp),
            color = Color(0xFF64B5F6),
            strokeWidth = 2.dp
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = "Detecting exercise…",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium
        )
    }
    if (currentLabel.isNotEmpty()) {
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Seeing: $currentLabel",
            color = Color(0xFF64B5F6),
            fontSize = 13.sp
        )
    }
    Spacer(modifier = Modifier.height(4.dp))
    Text(
        text = "Start your exercise to lock in",
        color = Color.Gray,
        fontSize = 11.sp
    )
}

// ── Workout HUD ──────────────────────────────────────────────────────

@Composable
private fun WorkoutHud(
    exerciseName: String,
    isPlank: Boolean,
    repCount: Int,
    holdSeconds: Int,
    feedback: ExerciseFeedbackEngine.Feedback,
    targetReps: Int,
    targetHold: Int,
    setInfo: String?,
    onEndExercise: () -> Unit
) {
    // Exercise name
    Text(
        text = exerciseName,
        color = Color(0xFF00E676),
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold
    )

    // Set info
    if (setInfo != null) {
        Text(
            text = setInfo,
            color = Color(0xFF64B5F6),
            fontSize = 13.sp,
            modifier = Modifier.padding(top = 2.dp)
        )
    }

    Spacer(modifier = Modifier.height(8.dp))

    // Rep count or hold timer with target
    if (isPlank) {
        val mins = holdSeconds / 60
        val secs = holdSeconds % 60
        Text(
            text = "%d:%02d".format(mins, secs),
            color = Color.White,
            fontSize = 48.sp,
            fontWeight = FontWeight.Bold
        )
        if (targetHold > 0) {
            Text(
                text = "Target: ${targetHold}s",
                color = Color(0xFF64B5F6),
                fontSize = 12.sp
            )
        } else {
            Text(text = "Hold time", color = Color.Gray, fontSize = 12.sp)
        }
    } else {
        Text(
            text = if (targetReps > 0) "$repCount / $targetReps" else "$repCount",
            color = Color.White,
            fontSize = 56.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Reps",
            color = Color.Gray,
            fontSize = 14.sp
        )
    }

    // Form feedback
    if (feedback.tips.isNotEmpty()) {
        Spacer(modifier = Modifier.height(10.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    if (feedback.isGoodForm) Color(0xFF1B5E20).copy(alpha = 0.6f)
                    else Color(0xFFE65100).copy(alpha = 0.6f),
                    RoundedCornerShape(8.dp)
                )
                .padding(10.dp)
        ) {
            feedback.tips.forEach { tip ->
                Text(
                    text = tip,
                    color = Color.White,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(vertical = 1.dp)
                )
            }
        }
    }

    // End / Skip exercise button
    Spacer(modifier = Modifier.height(14.dp))
    Button(
        onClick = onEndExercise,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFFE53935)
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            Icons.Default.Stop,
            contentDescription = "End",
            tint = Color.White,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = if (targetReps > 0 || targetHold > 0) "Skip Exercise" else "End Exercise",
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

// ── Rest HUD ─────────────────────────────────────────────────────────

@Composable
private fun RestHud(
    secondsLeft: Int,
    nextLabel: String,
    setInfo: String,
    onSkip: () -> Unit
) {
    Text(
        text = "Rest",
        color = Color(0xFF64B5F6),
        fontSize = 18.sp,
        fontWeight = FontWeight.SemiBold
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = "$secondsLeft",
        color = Color.White,
        fontSize = 56.sp,
        fontWeight = FontWeight.Bold
    )
    Text(
        text = "seconds",
        color = Color.Gray,
        fontSize = 14.sp
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = "Up next: $nextLabel • $setInfo",
        color = Color(0xFF00E676),
        fontSize = 13.sp
    )
    Spacer(modifier = Modifier.height(14.dp))
    Button(
        onClick = onSkip,
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("Skip Rest", color = Color.Black, fontWeight = FontWeight.Bold)
    }
}

// ── Transition HUD ───────────────────────────────────────────────────

@Composable
private fun TransitionHud(message: String) {
    Text(
        text = "✅ Exercise Complete!",
        color = Color(0xFF00E676),
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold
    )
    Spacer(modifier = Modifier.height(12.dp))
    Text(
        text = message,
        color = Color.White,
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold
    )
    Spacer(modifier = Modifier.height(8.dp))
    CircularProgressIndicator(
        modifier = Modifier.size(24.dp),
        color = Color(0xFF00E676),
        strokeWidth = 2.dp
    )
}
