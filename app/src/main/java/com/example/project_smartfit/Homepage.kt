package com.example.project_smartfit

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.YuvImage
import android.util.Log
import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.example.project_smartfit.pose.overlay.PoseStatistics
import java.io.ByteArrayOutputStream
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


/**
 * Exercise detection screen with camera feed, rep counter, and scoring
 */
@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ExerciseDetectionScreen(
    exerciseType: ExerciseType,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    val viewModel = remember {
        ViewModelProvider(
            context as androidx.lifecycle.ViewModelStoreOwner,
            PoseDetectionViewModelFactory(context)
        )[PoseDetectionViewModel::class.java]
    }

    val cameraPermission = rememberPermissionState(android.Manifest.permission.CAMERA)
    val detectionState by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.startExerciseTracking(exerciseType)
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopExerciseTracking()
        }
    }

    if (cameraPermission.status.isGranted) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top bar
            TopAppBar(
                title = { Text(exerciseType.name.replace("_", " ")) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.Home, contentDescription = "Back")
                    }
                }
            )

            Box(modifier = Modifier
                .fillMaxSize()
                .weight(1f)) {
                var previewViewRef by remember { mutableStateOf<PreviewView?>(null) }

                if (detectionState.isModelLoaded) {
                    CameraPreviewWithOverlay(
                        context = context,
                        lifecycleOwner = lifecycleOwner,
                        onPreviewViewReady = { previewView ->
                            previewViewRef = previewView
                        },
                        onFrameAnalyzed = { bitmap ->
                            viewModel.processCameraFrame(bitmap)
                        }
                    )

                    // Overlay with pose visualization
                    if (detectionState.currentPerson != null && previewViewRef != null &&
                        previewViewRef!!.width > 0 && previewViewRef!!.height > 0
                    ) {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .zIndex(1f)
                        ) {
                            Canvas(modifier = Modifier.matchParentSize()) {
                                with(PoseVisualization) {
                                    drawPoseWithFeatures(
                                        person = detectionState.currentPerson!!,
                                        features = detectionState.postureFeatures,
                                        canvasWidth = size.width,
                                        canvasHeight = size.height,
                                        bitmapWidth = detectionState.bitmapSize.first,
                                        bitmapHeight = detectionState.bitmapSize.second,
                                        highlightIssues = true
                                    )
                                }
                            }
                        }
                    }

                    // Exercise stats overlay
                    ExerciseStatsOverlay(
                        exerciseType = exerciseType,
                        exerciseState = detectionState.exerciseState,
                        fps = detectionState.fps,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(16.dp)
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Loading models...")
                        }
                    }
                }
            }
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Camera permission is required")
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { cameraPermission.launchPermissionRequest() }) {
                Text("Grant Permission")
            }
        }
    }
}

/**
 * Helper composable for camera preview with Android View
 */
@Composable
fun CameraPreviewWithOverlay(
    context: Context,
    lifecycleOwner: LifecycleOwner,
    onPreviewViewReady: (PreviewView) -> Unit,
    onFrameAnalyzed: (Bitmap) -> Unit
) {
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            PreviewView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }
        },
        update = { previewView ->
            onPreviewViewReady(previewView)
            startCamera(
                context = context,
                lifecycleOwner = lifecycleOwner,
                previewView = previewView,
                onFrameAnalyzed = onFrameAnalyzed
            )
        }
    )
}

/**
 * Exercise statistics overlay showing rep count and scoring with progress bar
 */
@Composable
fun ExerciseStatsOverlay(
    exerciseType: ExerciseType,
    exerciseState: ExerciseState?,
    fps: Int,
    modifier: Modifier = Modifier
) {
    androidx.compose.material3.Card(
        modifier = modifier,
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = Color.Black.copy(alpha = 0.7f)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Rep count
            if (exerciseState != null && exerciseType != ExerciseType.PLANK) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Reps:",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White
                    )
                    Text(
                        text = exerciseState.repCount.toString(),
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.Green
                    )
                }
            }

            // Current state
            if (exerciseState != null) {
                Text(
                    text = "State: ${exerciseState.lastFrameState.uppercase()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White
                )
            }

            // Score with progress bar

            // FPS
            Text(
                text = "FPS: $fps",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraScreen(viewModel: PoseDetectionViewModel? = null) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    // Create or use provided ViewModel
    val vm = viewModel ?: remember {
        ViewModelProvider(
            context as androidx.lifecycle.ViewModelStoreOwner,
            PoseDetectionViewModelFactory(context)
        )[PoseDetectionViewModel::class.java]
    }

    val cameraPermission = rememberPermissionState(android.Manifest.permission.CAMERA)
    val detectionState by vm.state.collectAsState()

    // Initialize session
    LaunchedEffect(Unit) {
        vm.startSession()
    }

    DisposableEffect(Unit) {
        onDispose {
            vm.endSession()
        }
    }

    if (cameraPermission.status.isGranted) {
        Box(modifier = Modifier.fillMaxSize()) {
            var previewViewRef by remember { mutableStateOf<PreviewView?>(null) }

            if (detectionState.isModelLoaded) {
                CameraPreviewWithOverlay(
                    context = context,
                    lifecycleOwner = lifecycleOwner,
                    onPreviewViewReady = { previewView ->
                        previewViewRef = previewView
                    },
                    onFrameAnalyzed = { bitmap ->
                        vm.processCameraFrame(bitmap)
                    }
                )

                // Overlay Canvas for pose visualization
                if (
                    detectionState.currentPerson != null &&
                    previewViewRef != null &&
                    previewViewRef!!.width > 0 &&
                    previewViewRef!!.height > 0
                ) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .zIndex(1f)
                    ) {
                        Canvas(
                            modifier = Modifier
                                .matchParentSize()
                        ) {
                            with(PoseVisualization) {
                                drawPoseWithFeatures(
                                    person = detectionState.currentPerson!!,
                                    features = detectionState.postureFeatures,
                                    canvasWidth = size.width,
                                    canvasHeight = size.height,
                                    bitmapWidth = detectionState.bitmapSize.first,
                                    bitmapHeight = detectionState.bitmapSize.second,
                                    highlightIssues = true
                                )
                            }
                        }
                    }
                }

                // Statistics overlay
                PoseStatistics(
                    person = detectionState.currentPerson,
                    fps = detectionState.fps,
                    elapsedTimeMs = detectionState.elapsedTimeMs,
                    postureFeatures = detectionState.postureFeatures,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                )
            } else {
                // Loading state
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Loading MoveNet model...")
                    }
                }
            }
        }
    } else {
        // Permission not granted
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Camera permission is required for pose detection",
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { cameraPermission.launchPermissionRequest() }) {
                Text("Grant Camera Permission")
            }
        }
    }
}

private fun startCamera(
    context: Context,
    lifecycleOwner: LifecycleOwner,
    previewView: PreviewView,
    onFrameAnalyzed: (Bitmap) -> Unit
) {
    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
    val executor: ExecutorService = Executors.newSingleThreadExecutor()

    cameraProviderFuture.addListener({
        val cameraProvider = cameraProviderFuture.get()

        val preview = Preview.Builder()
            .build()
            .also { it.setSurfaceProvider(previewView.surfaceProvider) }

        val imageAnalyzer = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also { analysis ->
                analysis.setAnalyzer(executor) { imageProxy ->
                    try {
                        val bitmap = imageProxyToBitmap(imageProxy)
                        onFrameAnalyzed(bitmap)
                    } catch (e: Exception) {
                        Log.e("CameraX", "Error processing frame", e)
                    } finally {
                        imageProxy.close()
                    }
                }
            }

        val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageAnalyzer
            )
        } catch (exc: Exception) {
            Log.e("CameraX", "Use case binding failed", exc)
        }
    }, ContextCompat.getMainExecutor(context))
}

// Simple ImageProxy to Bitmap conversion
private fun imageProxyToBitmap(imageProxy: ImageProxy): Bitmap {
    val yBuffer = imageProxy.planes[0].buffer
    val uBuffer = imageProxy.planes[1].buffer
    val vBuffer = imageProxy.planes[2].buffer

    val ySize = yBuffer.remaining()
    val uSize = uBuffer.remaining()
    val vSize = vBuffer.remaining()

    val nv21 = ByteArray(ySize + uSize + vSize)

    // U and V are swapped for NV21 format
    yBuffer.get(nv21, 0, ySize)
    vBuffer.get(nv21, ySize, vSize)
    uBuffer.get(nv21, ySize + vSize, uSize)

    val yuvImage = YuvImage(
        nv21,
        ImageFormat.NV21,
        imageProxy.width,
        imageProxy.height,
        null
    )

    val out = ByteArrayOutputStream()
    yuvImage.compressToJpeg(Rect(0, 0, imageProxy.width, imageProxy.height), 100, out)
    val imageBytes = out.toByteArray()
    val bitmap = android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)

    // Apply rotation and mirroring for front camera
    val matrix = Matrix().apply {
        postRotate(imageProxy.imageInfo.rotationDegrees.toFloat())
        // Mirror for front camera
        postScale(-1f, 1f, bitmap.width / 2f, bitmap.height / 2f)
    }

    return Bitmap.createBitmap(
        bitmap,
        0,
        0,
        bitmap.width,
        bitmap.height,
        matrix,
        true
    )
}


@OptIn(ExperimentalMaterial3Api::class)
@Suppress("UNUSED_EXPRESSION")
@Composable
fun Homepage(
    navController: NavController,
    sessionManager: SessionManager
) {
    var selectedExercise by remember { mutableStateOf<ExerciseType?>(null) }
    var showCamera by remember { mutableStateOf(false) }
    val name = sessionManager.getEmail() ?: "User"

    if (showCamera && selectedExercise != null) {
        // Show exercise detection screen
        ExerciseDetectionScreen(
            exerciseType = selectedExercise!!,
            onBack = {
                showCamera = false
                selectedExercise = null
            }
        )
    } else {
        // Homepage with exercise selection
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("SmartFit Posture Trainer") },
                    navigationIcon = {
                        IconButton(onClick = { navController.navigate(NavProfile) }) {
                            Icon(Icons.Default.Home, contentDescription = "Profile")
                        }
                    }
                )
            },
            bottomBar = {
                BottomNavBar(navController)
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Welcome section
                Text(
                    text = "Welcome back, $name! 👋",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Text(
                    text = "Select an exercise to analyze your form",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Exercise cards grid
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Pushup
                    ExerciseCard(
                        emoji = "💪",
                        title = "Pushup",
                        description = "Upper body strength with form analysis",
                        isSelected = selectedExercise == ExerciseType.PUSHUP,
                        onClick = {
                            selectedExercise = ExerciseType.PUSHUP
                            showCamera = true
                        }
                    )

                    // Squat
                    ExerciseCard(
                        emoji = "🏋️",
                        title = "Squat",
                        description = "Lower body strength and alignment",
                        isSelected = selectedExercise == ExerciseType.SQUAT,
                        onClick = {
                            selectedExercise = ExerciseType.SQUAT
                            showCamera = true
                        }
                    )

                    // Plank
                    ExerciseCard(
                        emoji = "📏",
                        title = "Plank",
                        description = "Core stability and body alignment",
                        isSelected = selectedExercise == ExerciseType.PLANK,
                        onClick = {
                            selectedExercise = ExerciseType.PLANK
                            showCamera = true
                        }
                    )

                    // Jumping Jacks
                    ExerciseCard(
                        emoji = "🤸",
                        title = "Jumping Jacks",
                        description = "Full body coordination and rhythm",
                        isSelected = selectedExercise == ExerciseType.JUMPING_JACKS,
                        onClick = {
                            selectedExercise = ExerciseType.JUMPING_JACKS
                            showCamera = true
                        }
                    )

                    // Dumbbell Curl
                    ExerciseCard(
                        emoji = "🎯",
                        title = "Dumbbell Curl",
                        description = "Arm flexion and shoulder stability",
                        isSelected = selectedExercise == ExerciseType.DUMBBELL_CURL,
                        onClick = {
                            selectedExercise = ExerciseType.DUMBBELL_CURL
                            showCamera = true
                        }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Logout button
                Button(
                    onClick = {
                        sessionManager.clearSession()
                        navController.navigate(NavLogin)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = Color.Red
                    )
                ) {
                    Text("Logout")
                }
            }
        }
    }
}

/**
 * Reusable exercise selection card
 */
@Composable
private fun ExerciseCard(
    emoji: String,
    title: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    androidx.compose.material3.Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Emoji
            Text(
                text = emoji,
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.size(48.dp)
            )

            // Content
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }

            // Arrow
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = "Start",
                    tint = Color.Gray
                )
            }
        }
    }
}