package com.example.project_smartfit.presentation.screens.exercise

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import com.example.project_smartfit.presentation.components.*
import com.example.project_smartfit.presentation.theme.*
import com.example.project_smartfit.presentation.screens.exercise.CameraScreen
import com.example.project_smartfit.domain.model.FeatureVector

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

    LightAuroraBackground(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            "Posture Analysis",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Slate900
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    ),
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Camera Preview Section
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.Black)
                ) {
                    CameraScreen(viewModel = vm)
                }

                // Stats / Analysis Section
                if (state.currentFeatures != null) {
                    GlassCard(
                        variant = GlassCardVariant.Light,
                        cornerRadius = 24.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Text(
                                "Live Feedback",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Slate900
                            )
                            
                            // Display key metrics from current features
                            PostureMetricsDisplay(
                                features = state.currentFeatures!!,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    // Recommendations based on form errors
                    if (state.currentErrors.isNotEmpty()) {
                        GlassCard(
                            variant = GlassCardVariant.Accent,
                            cornerRadius = 24.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    "Recommendations",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate900
                                )
                                state.currentErrors.forEach { error ->
                                    Text(
                                        "• $error",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Slate700
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // Loading State inside a card
                    GlassCard(
                        variant = GlassCardVariant.Light,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(color = GovBlue)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Analyzing Posture...", color = Slate600)
                        }
                    }
                }

                Button(
                    onClick = { navController.popBackStack() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(bottom = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GovBlue)
                ) {
                    Text("Complete Session", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun PostureMetricsDisplay(
    features: FeatureVector,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Overall visibility
        MetricRow(
            label = "Visibility",
            value = "${(features.overallVisibility * 100).toInt()}%",
            isGood = features.overallVisibility > 0.7f
        )

        // Back angle
        if (features.backAngle > 0) {
            MetricRow(
                label = "Back Alignment",
                value = "${features.backAngle.toInt()}°",
                isGood = features.backAngle > 150f
            )
        }

        // Hip depth (for squats)
        if (features.hipDepthRatio > 0) {
            MetricRow(
                label = "Hip Depth",
                value = String.format("%.2f", features.hipDepthRatio),
                isGood = features.hipDepthRatio < 0.8f
            )
        }
    }
}

@Composable
private fun MetricRow(
    label: String,
    value: String,
    isGood: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = Slate700
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = if (isGood) Color(0xFF10B981) else Color(0xFFEF4444)
        )
    }
}
