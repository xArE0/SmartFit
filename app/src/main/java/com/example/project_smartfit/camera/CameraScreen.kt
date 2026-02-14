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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import com.example.project_smartfit.ml.ExerciseClassifier
import com.example.project_smartfit.ml.ExerciseFeedbackEngine
import com.example.project_smartfit.ml.PoseLandmarkerHelper
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import java.util.concurrent.Executors

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
        Text(
            text = "Camera Permission Required",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "SmartFit needs camera access to detect your exercise form.",
            color = Color.Gray,
            fontSize = 14.sp,
            modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
        )
        Button(onClick = onRequestPermission) {
            Text("Grant Permission")
        }
        Button(
            onClick = onBack,
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Text("Go Back")
        }
    }
}

@Composable
private fun CameraContent(navController: NavController) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // ── State ────────────────────────────────────────────────────────
    var poseResult by remember { mutableStateOf<PoseLandmarkerResult?>(null) }
    var imageWidth by remember { mutableIntStateOf(0) }
    var imageHeight by remember { mutableIntStateOf(0) }
    var classificationResult by remember {
        mutableStateOf<ExerciseClassifier.ClassificationResult?>(null)
    }
    var feedbackState by remember {
        mutableStateOf(ExerciseFeedbackEngine.Feedback.NONE)
    }
    var currentLandmarks by remember {
        mutableStateOf<List<NormalizedLandmark>?>(null)
    }
    var useFrontCamera by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // ── ML helpers ───────────────────────────────────────────────────
    val exerciseClassifier = remember { ExerciseClassifier(context) }

    // Throttle feedback to avoid flickering — only update every N frames
    var feedbackFrameCounter by remember { mutableIntStateOf(0) }
    val FEEDBACK_UPDATE_INTERVAL = 10

    val poseLandmarkerHelper = remember {
        PoseLandmarkerHelper(
            context = context,
            listener = object : PoseLandmarkerHelper.LandmarkerListener {
                override fun onResults(
                    result: PoseLandmarkerResult,
                    imgHeight: Int,
                    imgWidth: Int
                ) {
                    // Update pose overlay immediately
                    poseResult = result
                    imageWidth = imgWidth
                    imageHeight = imgHeight

                    // Feed landmarks to classifier
                    val landmarks = result.landmarks().firstOrNull()
                    if (landmarks != null && landmarks.isNotEmpty()) {
                        currentLandmarks = landmarks

                        exerciseClassifier.addFrameAndClassify(landmarks) { classification ->
                            classificationResult = classification
                        }

                        // Throttled feedback
                        val currentResult = classificationResult
                        if (currentResult != null && currentResult.isConfident) {
                            feedbackFrameCounter++
                            if (feedbackFrameCounter >= FEEDBACK_UPDATE_INTERVAL) {
                                feedbackFrameCounter = 0
                                feedbackState = ExerciseFeedbackEngine.analyse(
                                    currentResult.label,
                                    landmarks
                                )
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
            cameraExecutor.shutdown()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {

        // ── Camera Preview ───────────────────────────────────────────
        // key(useFrontCamera) forces full recreation when camera flips,
        // so the CameraProvider properly rebinds to the new lens.
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
                                lifecycleOwner,
                                cameraSelector,
                                preview,
                                imageAnalysis
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
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            IconButton(onClick = { useFrontCamera = !useFrontCamera }) {
                Icon(
                    Icons.Default.Cameraswitch,
                    contentDescription = "Switch Camera",
                    tint = Color.White
                )
            }
        }

        // ── Bottom HUD ───────────────────────────────────────────────
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(24.dp)
                .fillMaxWidth()
                .background(
                    Color.Black.copy(alpha = 0.75f),
                    RoundedCornerShape(16.dp)
                )
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val result = classificationResult

            if (result == null) {
                // Still waiting for first classification — no frame counter, just a message
                Text(
                    text = "Perform an exercise…",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Model will identify your exercise live",
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            } else {
                // ── Classification result ────────────────────────────
                Text(
                    text = result.displayLabel,
                    color = if (result.isConfident) Color(0xFF00E676) else Color(0xFFFFD54F),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Confidence: ${(result.confidence * 100).toInt()}%",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 14.sp
                )

                // Confidence bar
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .background(Color.DarkGray, RoundedCornerShape(3.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(result.confidence.coerceIn(0f, 1f))
                            .height(6.dp)
                            .background(
                                if (result.isConfident) Color(0xFF00E676)
                                else Color(0xFFFFD54F),
                                RoundedCornerShape(3.dp)
                            )
                    )
                }

                // ── Form feedback ────────────────────────────────────
                if (result.isConfident && feedbackState.tips.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (feedbackState.isGoodForm) Color(0xFF1B5E20).copy(alpha = 0.6f)
                                else Color(0xFFE65100).copy(alpha = 0.6f),
                                RoundedCornerShape(8.dp)
                            )
                            .padding(10.dp)
                    ) {
                        feedbackState.tips.forEach { tip ->
                            Text(
                                text = tip,
                                color = Color.White,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(vertical = 1.dp)
                            )
                        }
                    }
                }

                if (!result.isConfident) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Low confidence — try a clearer angle",
                        color = Color(0xFFFFD54F),
                        fontSize = 12.sp
                    )
                }

                // Live indicator + reset
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(Color(0xFF00E676), RoundedCornerShape(4.dp))
                    )
                    Text(
                        text = "  LIVE",
                        color = Color(0xFF00E676),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        exerciseClassifier.resetBuffer()
                        classificationResult = null
                        feedbackState = ExerciseFeedbackEngine.Feedback.NONE
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White.copy(alpha = 0.15f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Reset",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "  Try Another Exercise",
                        color = Color.White,
                        fontSize = 13.sp
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
