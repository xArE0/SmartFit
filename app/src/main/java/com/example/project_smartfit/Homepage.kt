package com.example.project_smartfit

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import android.view.ViewGroup
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.NavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.sqrt

data class KeyPoint(
    val x: Float,
    val y: Float,
    val score: Float
)

data class Person(
    val keyPoints: List<KeyPoint>,
    val score: Float
)

class PoseDetector(private val context: Context) {
    private var interpreter: Interpreter? = null
    private var inputImageWidth: Int = 0
    private var inputImageHeight: Int = 0
    private var outputShape: IntArray? = null

    companion object {
        private const val TAG = "PoseDetector"
        private const val MODEL_FILENAME = "movenet_singlepose_lightning.tflite"

        // MoveNet keypoint names
        val keyPointNames = arrayOf(
            "nose", "left_eye", "right_eye", "left_ear", "right_ear",
            "left_shoulder", "right_shoulder", "left_elbow", "right_elbow",
            "left_wrist", "right_wrist", "left_hip", "right_hip",
            "left_knee", "right_knee", "left_ankle", "right_ankle"
        )

        // Connections for skeleton visualization
        val bodyConnections = arrayOf(
            Pair(0, 1), Pair(0, 2), Pair(1, 3), Pair(2, 4), // Head
            Pair(5, 6), // Shoulders
            Pair(5, 7), Pair(7, 9), // Left arm
            Pair(6, 8), Pair(8, 10), // Right arm
            Pair(5, 11), Pair(6, 12), // Torso
            Pair(11, 12), // Hips
            Pair(11, 13), Pair(13, 15), // Left leg
            Pair(12, 14), Pair(14, 16) // Right leg
        )
    }

    fun initialize(): Boolean {
        return try {
            val model = FileUtil.loadMappedFile(context, MODEL_FILENAME)
            val options = Interpreter.Options().apply {
                setNumThreads(4)
                setUseNNAPI(true)
            }

            interpreter = Interpreter(model, options)

            // Get input shape
            val inputShape = interpreter!!.getInputTensor(0).shape()
            inputImageHeight = inputShape[1]
            inputImageWidth = inputShape[2]

            // Get output shape
            outputShape = interpreter!!.getOutputTensor(0).shape()

            Log.d(TAG, "Model initialized successfully")
            Log.d(TAG, "Input shape: ${inputShape.contentToString()}")
            Log.d(TAG, "Output shape: ${outputShape!!.contentToString()}")

            true
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing pose detector", e)
            false
        }
    }

    fun detectPose(bitmap: Bitmap): Person? {
        return try {
            val tensorImage = TensorImage.fromBitmap(bitmap)

            val imageProcessor = ImageProcessor.Builder()
                .add(ResizeOp(inputImageHeight, inputImageWidth, ResizeOp.ResizeMethod.BILINEAR))
                .build()

            val processedImage = imageProcessor.process(tensorImage)

            // Prepare output buffer
            val outputArray = Array(1) { Array(1) { Array(17) { FloatArray(3) } } }

            // Run inference
            interpreter?.run(processedImage.buffer, outputArray)

            // Parse results
            val keyPoints = mutableListOf<KeyPoint>()
            val originalHeight = bitmap.height.toFloat()
            val originalWidth = bitmap.width.toFloat()

            for (i in 0 until 17) {
                val y = outputArray[0][0][i][0] * originalHeight
                val x = outputArray[0][0][i][1] * originalWidth
                val score = outputArray[0][0][i][2]

                keyPoints.add(KeyPoint(x, y, score))
            }

            // Calculate overall confidence
            val avgScore = keyPoints.map { it.score }.average().toFloat()

            Person(keyPoints, avgScore)

        } catch (e: Exception) {
            Log.e(TAG, "Error during pose detection", e)
            null
        }
    }

    fun close() {
        interpreter?.close()
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val cameraPermission = rememberPermissionState(android.Manifest.permission.CAMERA)

    var currentPerson by remember { mutableStateOf<Person?>(null) }
    var previewSize by remember { mutableStateOf(Pair(0, 0)) }
    var poseDetector by remember { mutableStateOf<PoseDetector?>(null) }
    var isModelLoaded by remember { mutableStateOf(false) }

    // Initialize pose detector
    LaunchedEffect(Unit) {
        val detector = PoseDetector(context)
        if (detector.initialize()) {
            poseDetector = detector
            isModelLoaded = true
        }
    }

    // Clean up
    DisposableEffect(Unit) {
        onDispose {
            poseDetector?.close()
        }
    }

    if (cameraPermission.status.isGranted) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (isModelLoaded) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { context ->
                        PreviewView(context).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            scaleType = PreviewView.ScaleType.FILL_START
                        }
                    },
                    update = { previewView ->
                        startCamera(
                            context = context,
                            lifecycleOwner = lifecycleOwner,
                            previewView = previewView,
                            poseDetector = poseDetector,
                            onPoseDetected = { person, width, height ->
                                currentPerson = person
                                previewSize = Pair(width, height)
                            }
                        )
                    }
                )

                // Pose overlay
                if (currentPerson != null && previewSize.first > 0 && previewSize.second > 0) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawPose(currentPerson!!, size.width, size.height, previewSize.first, previewSize.second)
                    }
                }

                // Statistics overlay
                PoseStatistics(
                    person = currentPerson,
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
    context: android.content.Context,
    lifecycleOwner: LifecycleOwner,
    previewView: PreviewView,
    poseDetector: PoseDetector?,
    onPoseDetected: (Person?, Int, Int) -> Unit
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
                    poseDetector?.let { detector ->
                        val bitmap = imageProxy.toBitmap()
                        val person = detector.detectPose(bitmap)

                        onPoseDetected(person, bitmap.width, bitmap.height)
                    }
                    imageProxy.close()
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

// Extension function to convert ImageProxy to Bitmap
private fun ImageProxy.toBitmap(): Bitmap {
    val buffer = planes[0].buffer
    val bytes = ByteArray(buffer.remaining())
    buffer.get(bytes)
    return android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        ?: Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
}

private fun DrawScope.drawPose(
    person: Person,
    canvasWidth: Float,
    canvasHeight: Float,
    imageWidth: Int,
    imageHeight: Int
) {
    val scaleX = canvasWidth / imageWidth
    val scaleY = canvasHeight / imageHeight

    // Draw keypoints
    person.keyPoints.forEachIndexed { index, keyPoint ->
        if (keyPoint.score > 0.3) {
            val x = keyPoint.x * scaleX
            val y = keyPoint.y * scaleY

            drawCircle(
                color = Color.Red,
                radius = 8f,
                center = androidx.compose.ui.geometry.Offset(x, y)
            )

            // Draw keypoint labels
            drawContext.canvas.nativeCanvas.apply {
                drawText(
                    PoseDetector.keyPointNames[index],
                    x + 10f,
                    y - 10f,
                    android.graphics.Paint().apply {
                        color = android.graphics.Color.WHITE
                        textSize = 24f
                        isAntiAlias = true
                    }
                )
            }
        }
    }

    // Draw skeleton connections
    PoseDetector.bodyConnections.forEach { connection ->
        val startKeyPoint = person.keyPoints[connection.first]
        val endKeyPoint = person.keyPoints[connection.second]

        if (startKeyPoint.score > 0.3 && endKeyPoint.score > 0.3) {
            val startX = startKeyPoint.x * scaleX
            val startY = startKeyPoint.y * scaleY
            val endX = endKeyPoint.x * scaleX
            val endY = endKeyPoint.y * scaleY

            drawLine(
                color = Color.Green,
                start = androidx.compose.ui.geometry.Offset(startX, startY),
                end = androidx.compose.ui.geometry.Offset(endX, endY),
                strokeWidth = 4f
            )
        }
    }
}

@Composable
private fun PoseStatistics(
    person: Person?,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .width(200.dp)
            .clip(RoundedCornerShape(8.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.7f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "Pose Statistics",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )

            if (person != null) {
                Text(
                    text = "Overall Score: ${String.format("%.2f", person.score)}",
                    color = Color.White,
                    fontSize = 12.sp
                )

                val visibleKeypoints = person.keyPoints.count { it.score > 0.3 }
                Text(
                    text = "Visible Keypoints: $visibleKeypoints/17",
                    color = Color.White,
                    fontSize = 12.sp
                )

                // Calculate pose quality
                val poseQuality = when {
                    person.score > 0.7 -> "Excellent"
                    person.score > 0.5 -> "Good"
                    person.score > 0.3 -> "Fair"
                    else -> "Poor"
                }

                Text(
                    text = "Pose Quality: $poseQuality",
                    color = when (poseQuality) {
                        "Excellent" -> Color.Green
                        "Good" -> Color.Yellow
                        "Fair" -> Color.Magenta
                        else -> Color.Red
                    },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )

                // Basic pose analysis
                person.keyPoints.let { keyPoints ->
                    val leftShoulder = keyPoints[5]
                    val rightShoulder = keyPoints[6]
                    val leftHip = keyPoints[11]
                    val rightHip = keyPoints[12]

                    if (leftShoulder.score > 0.3 && rightShoulder.score > 0.3) {
                        val shoulderDistance = sqrt(
                            (leftShoulder.x - rightShoulder.x) * (leftShoulder.x - rightShoulder.x) +
                                    (leftShoulder.y - rightShoulder.y) * (leftShoulder.y - rightShoulder.y)
                        )

                        Text(
                            text = "Shoulder Width: ${String.format("%.0f", shoulderDistance)}px",
                            color = Color.White,
                            fontSize = 12.sp
                        )
                    }
                }
            } else {
                Text(
                    text = "No pose detected",
                    color = Color.Red,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Homepage(
    navController: NavController,
    sessionManager: SessionManager
) {
    var message by remember { mutableStateOf("Loading...") }
    var showPoseDetection by remember { mutableStateOf(false) }
    val name = sessionManager.getEmail() ?: "User"

    if (showPoseDetection) {
        // Show pose detection screen
        Column(modifier = Modifier.fillMaxSize()) {
            // Top bar with back button
            TopAppBar(
                title = { Text("Pose Detection") },
                navigationIcon = {
                    IconButton(onClick = { showPoseDetection = false }) {
                        Icon(Icons.Default.Home, contentDescription = "Back to Home")
                    }
                }
            )

            // Camera screen
            CameraScreen()
        }
    } else {
        // Original homepage with pose detection button
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Welcome $name") }
                )
            },
            bottomBar = {
                BottomNavBar(navController)
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(text = message)

                    // Pose Detection Button
                    Button(
                        onClick = { showPoseDetection = true },
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .height(56.dp)
                    ) {
                        Icon(
                            Icons.Default.CameraAlt,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Start Pose Detection")
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            sessionManager.clearSession()
                            navController.navigate(NavLogin)
                        }
                    ) {
                        Text("Logout")
                    }
                }
            }
        }
    }
}