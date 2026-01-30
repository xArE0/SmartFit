package com.example.project_smartfit.presentation.screens.exercise

import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.ViewModelProvider
import com.example.project_smartfit.domain.model.ExerciseType
import com.example.project_smartfit.presentation.components.*
import com.example.project_smartfit.presentation.theme.*
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

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

    LightAuroraBackground(modifier = Modifier.fillMaxSize()) {
        if (cameraPermission.status.isGranted) {
            Scaffold(
                containerColor = Color.Transparent,
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                exerciseType.name.replace("_", " "),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Slate900
                            )
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent
                        ),
                        navigationIcon = {
                            IconButton(onClick = onBack) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = GovBlue
                                )
                            }
                        }
                    )
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(bottom = 16.dp, start = 16.dp, end = 16.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.Black)
                ) {
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

                        // Overlay for pose visualization
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
                                .align(Alignment.TopEnd)
                                .padding(16.dp)
                        )
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = GovGreenLight)
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("Initializing Vision Engine...", color = Color.White)
                            }
                        }
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                GlassCard(
                    variant = GlassCardVariant.Light,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            "Camera Access Required",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "We need camera access to analyze your posture and count reps.",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = Slate600
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = { cameraPermission.launchPermissionRequest() },
                            colors = ButtonDefaults.buttonColors(containerColor = GovBlue)
                        ) {
                            Text("Grant Permission")
                        }
                    }
                }
            }
        }
    }
}
