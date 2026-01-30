package com.example.project_smartfit.presentation.screens.exercise

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import com.example.project_smartfit.presentation.components.PostureAnalysisReport
import com.example.project_smartfit.presentation.components.RecommendationsCard

/**
 * Posture Analysis Report Screen
 *
 * Shows detailed posture analysis with recommendations.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostureAnalysisScreen(navController: NavController) {
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
                Text("Done", color = Color.White)
            }
        }
    }
}
