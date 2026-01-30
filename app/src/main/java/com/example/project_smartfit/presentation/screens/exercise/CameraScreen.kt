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
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import com.example.project_smartfit.presentation.components.CameraPreviewWithOverlay
import com.example.project_smartfit.presentation.components.PoseStatistics
import com.example.project_smartfit.presentation.components.PoseVisualization
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

/**
 * Generic camera screen for pose detection visualization
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
