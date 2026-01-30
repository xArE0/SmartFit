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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.ViewModelProvider
import com.example.project_smartfit.domain.model.ExerciseType
import com.example.project_smartfit.presentation.components.CameraPreviewWithOverlay
import com.example.project_smartfit.presentation.components.ExerciseStatsOverlay
import com.example.project_smartfit.presentation.components.PoseVisualization
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
