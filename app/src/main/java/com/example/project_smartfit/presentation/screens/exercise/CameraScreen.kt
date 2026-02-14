package com.example.project_smartfit.presentation.screens.exercise

import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.ViewModelProvider
import com.example.project_smartfit.presentation.components.CameraPreviewWithOverlay
import com.example.project_smartfit.presentation.components.MediaPipePoseDrawer
import com.example.project_smartfit.domain.evaluation.ExerciseProfiles
import com.example.project_smartfit.domain.evaluation.LandmarkGroup
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

/**
 * Generic camera screen for MediaPipe pose detection visualization
 */
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

                // Overlay Canvas for MediaPipe pose skeleton
                val currentPose = detectionState.currentPoseFrame
                if (currentPose != null && 
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
                            modifier = Modifier.matchParentSize()
                        ) {
                            with(MediaPipePoseDrawer) {
                                // Extract error joints if available
                                val errorJoints = detectionState.formResult?.errors
                                    ?.flatMap { it.affectedJoints }
                                    ?: emptyList()
                                
                                // Filter landmarks based on exercise type
                                val profile = ExerciseProfiles.getProfile(detectionState.selectedExercise)
                                val relevantIndices = LandmarkGroup.indicesFor(profile.relevantLandmarkGroups)
                                
                                drawPoseWithFeedback(
                                    poseFrame = currentPose,
                                    canvasWidth = size.width,
                                    canvasHeight = size.height,
                                    isFormCorrect = detectionState.isFormCorrect,
                                    errorJoints = errorJoints,
                                    minVisibility = 0.5f,
                                    relevantIndices = relevantIndices
                                )
                            }
                        }
                    }
                }

                // Statistics overlay - top right
                Card(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .zIndex(2f),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "FPS: ${detectionState.fps}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "Visibility: ${String.format("%.0f", detectionState.averageVisibility * 100)}%",
                            style = MaterialTheme.typography.bodySmall
                        )
                        if (detectionState.isTrackingActive) {
                            Text(
                                text = "Reps: ${detectionState.repCount}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            } else {
                // Loading state
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Loading MediaPipe Pose model...")
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
