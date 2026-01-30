package com.example.project_smartfit.presentation.screens.exercise

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
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
 * Quick Pose Check Screen
 *
 * Simple, minimal interface for quick posture checks.
 */
@Composable
fun QuickPoseCheckScreen(navController: NavController) {
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
            Text("Close", color = Color.White)
        }
    }
}
