package com.example.project_smartfit

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController

/**
 * Example Page 1: Workout Monitoring
 *
 * Shows how to use pose detection during a workout session.
 * Real-time posture feedback while performing exercises.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutMonitoringPage(navController: NavController) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    // Create ViewModel instance
    val vm = remember {
        ViewModelProvider(
            context as androidx.lifecycle.ViewModelStoreOwner,
            PoseDetectionViewModelFactory(context)
        )[PoseDetectionViewModel::class.java]
    }

    val state by vm.state.collectAsState()

    // Initialize session
    LaunchedEffect(Unit) {
        vm.startSession()
    }

    DisposableEffect(Unit) {
        onDispose {
            vm.endSession()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Workout Monitoring") }
            )
        },
        bottomBar = {
            BottomNavBar(navController)
        }
    ) { innerPadding ->
        if (state.isModelLoaded) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Camera section
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black)
                ) {
                    CameraScreen(viewModel = vm)
                }

                // Posture metrics
                if (state.postureFeatures != null) {
                    PostureMetricsCard(features = state.postureFeatures!!)
                }

                // Feedback section
                FeedbackSection(state = state)

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { vm.reset() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Reset")
                    }
                    Button(
                        onClick = { navController.popBackStack() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Back")
                    }
                }
            }
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Loading model...")
            }
        }
    }
}

/**
 * Example Page 2: Posture Analysis Report
 *
 * Shows detailed posture analysis with recommendations.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostureAnalysisPage(navController: NavController) {
    val context = LocalContext.current

    val vm = remember {
        ViewModelProvider(
            context as androidx.lifecycle.ViewModelStoreOwner,
            PoseDetectionViewModelFactory(context)
        )[PoseDetectionViewModel::class.java]
    }

    val state by vm.state.collectAsState()

    LaunchedEffect(Unit) {
        vm.startSession()
    }

    DisposableEffect(Unit) {
        onDispose {
            vm.endSession()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Posture Analysis") }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Camera with small preview
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black)
            ) {
                CameraScreen(viewModel = vm)
            }

            // Full analysis report
            if (state.postureFeatures != null) {
                PostureAnalysisReport(
                    features = state.postureFeatures!!,
                    modifier = Modifier.fillMaxWidth()
                )

                // Recommendations
                RecommendationsCard(features = state.postureFeatures!!)
            }

            Button(
                onClick = { navController.popBackStack() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Done")
            }
        }
    }
}

/**
 * Example Page 3: Quick Pose Check
 *
 * Simple, minimal interface for quick posture checks.
 */
@Composable
fun QuickPoseCheckPage(navController: NavController) {
    val context = LocalContext.current

    val vm = remember {
        ViewModelProvider(
            context as androidx.lifecycle.ViewModelStoreOwner,
            PoseDetectionViewModelFactory(context)
        )[PoseDetectionViewModel::class.java]
    }

    val state by vm.state.collectAsState()

    LaunchedEffect(Unit) {
        vm.startSession()
    }

    DisposableEffect(Unit) {
        onDispose {
            vm.endSession()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Full-screen camera
        CameraScreen(viewModel = vm)

        // Large score overlay
        if (state.postureFeatures != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black.copy(alpha = 0.7f))
                    .padding(32.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Detected Joints",
                        fontSize = 16.sp,
                        color = Color.LightGray
                    )
                    Text(
                        text = "${state.postureFeatures!!.detectedJointCount}",
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Cyan
                    )
                    Text(
                        text = "joints detected",
                        fontSize = 14.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }

        // Close button
        Button(
            onClick = { navController.popBackStack() },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        ) {
            Text("Close")
        }
    }
}

/**
 * Reusable card component for posture metrics
 */
@Composable
private fun PostureMetricsCard(features: PostureFeatures) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.DarkGray.copy(alpha = 0.5f))
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Overall score with progress bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Detected Joints")
                Text(
                    "${features.detectedJointCount}",
                    fontWeight = FontWeight.Bold,
                    color = Color.Cyan
                )
            }

            // Key metrics
            MetricRow("Neck Flexion", "${String.format("%.1f", features.neckFlexion)}°")
            MetricRow("Shoulder Level", "${String.format("%.1f", features.shoulderLevel)}%")
            MetricRow("Spine Alignment", "${String.format("%.1f", features.spineAlignment)}%")
        }
    }
}

/**
 * Reusable feedback component
 */
@Composable
private fun FeedbackSection(state: PoseDetectionState) {
    if (state.postureFeatures == null) return

    val features = state.postureFeatures
    val feedback = mutableListOf<String>()

    // Generate feedback based on features
    if (kotlin.math.abs(features.neckFlexion) > 20f) {
        feedback.add("⚠ Straighten your neck - reduce forward lean")
    }
    if (features.shoulderLevel > 15f) {
        feedback.add("⚠ Level your shoulders")
    }
    if (kotlin.math.abs(features.torsoLean) > 25f) {
        feedback.add("⚠ Reduce forward trunk lean")
    }
    if (features.spineAlignment < 60f) {
        feedback.add("⚠ Improve spine alignment")
    }

    if (feedback.isNotEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFFFE4B5).copy(alpha = 0.3f))
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Feedback", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                feedback.forEach { msg ->
                    Text(msg, fontSize = 12.sp, color = Color.White)
                }
            }
        }
    }
}

/**
 * Recommendations based on posture analysis
 */
@Composable
private fun RecommendationsCard(features: PostureFeatures) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Blue.copy(alpha = 0.2f))
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Key Measurements", fontWeight = FontWeight.Bold, fontSize = 14.sp)

            Text("• Neck Flexion: ${String.format("%.1f", features.neckFlexion)}°", fontSize = 12.sp)
            Text("• Torso Lean: ${String.format("%.1f", features.torsoLean)}°", fontSize = 12.sp)
            Text("• Left Arm: ${String.format("%.0f", features.leftArmFlexion)}°", fontSize = 12.sp)
            Text("• Right Arm: ${String.format("%.0f", features.rightArmFlexion)}°", fontSize = 12.sp)
            Text("• Left Knee: ${String.format("%.0f", features.leftKneeFlexion)}°", fontSize = 12.sp)
            Text("• Right Knee: ${String.format("%.0f", features.rightKneeFlexion)}°", fontSize = 12.sp)
        }
    }
}

/**
 * Simple metric row component
 */
@Composable
private fun MetricRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 12.sp, color = Color.LightGray)
        Text(value, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

private fun getScoreColor(score: Float): Color {
    return when {
        score >= 85 -> Color.Green
        score >= 70 -> Color(0xFF90EE90)
        score >= 50 -> Color(0xFFFFD700)
        else -> Color.Red
    }
}

