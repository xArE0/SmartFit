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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import com.example.project_smartfit.ml.ExerciseClassifier
import com.example.project_smartfit.ml.ExerciseFeedbackEngine
import com.example.project_smartfit.ml.PoseLandmarkerHelper
import com.example.project_smartfit.ml.RepCounter
import com.example.project_smartfit.ml.VoiceFeedbackManager
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import java.util.concurrent.Executors

// ── Phase of the workout ────────────────────────────────────────────
private enum class WorkoutPhase { SCANNING, LOCKED_IN }

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraScreen(navController: NavController) {
    val cameraPermission = rememberPermissionState(Manifest.permission.CAMERA)

    LaunchedEffect(Unit) {
        if (!cameraPermission.status.isGranted) {
            cameraPermission.launchPermissionRequest()
        }
    }

    if (cameraPermission.status.isGranted) {
        CameraContent(navController)
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
private fun CameraContent(navController: NavController) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // ── Core state ───────────────────────────────────────────────────
    var poseResult by remember { mutableStateOf<PoseLandmarkerResult?>(null) }
    var imageWidth by remember { mutableIntStateOf(0) }
    var imageHeight by remember { mutableIntStateOf(0) }
    var useFrontCamera by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // ── Workout flow state ───────────────────────────────────────────
    var workoutPhase by remember { mutableStateOf(WorkoutPhase.SCANNING) }
    var lockedExercise by remember { mutableStateOf("") }
    var lockedDisplayLabel by remember { mutableStateOf("") }

    // ── Scanning / convergence state ─────────────────────────────────
    // Collect classification votes; lock in when one dominates
    val classificationVotes = remember { mutableMapOf<String, Int>() }
    var scanStartTimeMs by remember { mutableLongStateOf(0L) }
    var currentLabel by remember { mutableStateOf("") }

    // ── Workout state ────────────────────────────────────────────────
    var repCount by remember { mutableIntStateOf(0) }
    var holdSeconds by remember { mutableIntStateOf(0) }
    var feedbackState by remember { mutableStateOf(ExerciseFeedbackEngine.Feedback.NONE) }
    var feedbackFrameCounter by remember { mutableIntStateOf(0) }

    // ── ML helpers ───────────────────────────────────────────────────
    val exerciseClassifier = remember { ExerciseClassifier(context) }
    val repCounter = remember { RepCounter() }
    val voiceFeedback = remember { VoiceFeedbackManager(context) }

    val FEEDBACK_VOICE_INTERVAL = 15  // frames between voice feedback checks
    val CONVERGENCE_THRESHOLD = 3     // votes needed to lock in
    val MIN_SCAN_TIME_MS = 3_000L     // minimum 3 seconds scanning
    val MAX_SCAN_TIME_MS = 10_000L    // force lock at 10 seconds if any votes

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
                            // Feed to classifier for exercise detection
                            exerciseClassifier.addFrameAndClassify(landmarks) { classification ->
                                if (classification.isConfident) {
                                    val label = classification.label
                                    currentLabel = classification.displayLabel

                                    val votes = classificationVotes.getOrDefault(label, 0) + 1
                                    classificationVotes[label] = votes

                                    if (scanStartTimeMs == 0L) {
                                        scanStartTimeMs = System.currentTimeMillis()
                                    }

                                    val elapsed = System.currentTimeMillis() - scanStartTimeMs

                                    // Lock in if enough votes AND minimum time passed
                                    val shouldLock = (votes >= CONVERGENCE_THRESHOLD && elapsed >= MIN_SCAN_TIME_MS) ||
                                            (elapsed >= MAX_SCAN_TIME_MS && classificationVotes.isNotEmpty())

                                    if (shouldLock) {
                                        // Pick the label with most votes
                                        val best = classificationVotes.maxByOrNull { it.value }
                                        if (best != null) {
                                            lockedExercise = best.key
                                            lockedDisplayLabel = best.key.split(" ")
                                                .joinToString(" ") { w -> w.replaceFirstChar { it.uppercaseChar() } }
                                            workoutPhase = WorkoutPhase.LOCKED_IN
                                            repCounter.reset()
                                            voiceFeedback.announceExercise(lockedDisplayLabel)
                                            Log.d("CameraScreen", "Locked in: $lockedExercise (${best.value} votes)")
                                        }
                                    }
                                }
                            }
                        }

                        WorkoutPhase.LOCKED_IN -> {
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

                            // Throttled form feedback
                            feedbackFrameCounter++
                            if (feedbackFrameCounter >= FEEDBACK_VOICE_INTERVAL) {
                                feedbackFrameCounter = 0
                                val fb = ExerciseFeedbackEngine.analyse(lockedExercise, landmarks)
                                feedbackState = fb

                                // Voice only the first tip (most important)
                                if (!fb.isGoodForm && fb.tips.isNotEmpty()) {
                                    voiceFeedback.speakTip(fb.tips.first())
                                }
                            }
                        }
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

        // Camera Preview — key forces rebind on camera switch
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
            modifier = Modifier.fillMaxSize()
        )

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
            if (workoutPhase == WorkoutPhase.SCANNING) {
                IconButton(onClick = { useFrontCamera = !useFrontCamera }) {
                    Icon(Icons.Default.Cameraswitch, "Switch Camera", tint = Color.White)
                }
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
                // ── SCANNING PHASE ───────────────────────────────
                WorkoutPhase.SCANNING -> {
                    ScanningHud(currentLabel)
                }
                // ── LOCKED-IN PHASE ──────────────────────────────
                WorkoutPhase.LOCKED_IN -> {
                    WorkoutHud(
                        exerciseName = lockedDisplayLabel,
                        isPlank = lockedExercise == "plank",
                        repCount = repCount,
                        holdSeconds = holdSeconds,
                        feedback = feedbackState,
                        onEndExercise = {
                            // Reset everything for a new scan
                            workoutPhase = WorkoutPhase.SCANNING
                            lockedExercise = ""
                            lockedDisplayLabel = ""
                            classificationVotes.clear()
                            scanStartTimeMs = 0L
                            currentLabel = ""
                            repCount = 0
                            holdSeconds = 0
                            feedbackState = ExerciseFeedbackEngine.Feedback.NONE
                            feedbackFrameCounter = 0
                            exerciseClassifier.resetBuffer()
                            repCounter.reset()
                            voiceFeedback.reset()
                        }
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
    onEndExercise: () -> Unit
) {
    // Exercise name
    Text(
        text = exerciseName,
        color = Color(0xFF00E676),
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold
    )

    Spacer(modifier = Modifier.height(8.dp))

    // Rep count or hold timer
    if (isPlank) {
        val mins = holdSeconds / 60
        val secs = holdSeconds % 60
        Text(
            text = "%d:%02d".format(mins, secs),
            color = Color.White,
            fontSize = 48.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Hold time",
            color = Color.Gray,
            fontSize = 12.sp
        )
    } else {
        Text(
            text = "$repCount",
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

    // End exercise button
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
            text = "End Exercise",
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
