package com.example.project_smartfit.presentation.screens.exercise

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import com.example.project_smartfit.presentation.components.BottomNavBar
import com.example.project_smartfit.presentation.components.FeedbackSection
import com.example.project_smartfit.presentation.components.PostureMetricsCard

/**
 * Workout Monitoring Screen
 *
 * Shows how to use pose detection during a workout session.
 * Real-time posture feedback while performing exercises.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutMonitoringScreen(navController: NavController) {
    val context = LocalContext.current

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
