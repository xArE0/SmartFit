package com.example.project_smartfit

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.YuvImage
import android.util.Log
import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
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
import java.io.ByteArrayOutputStream
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

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
                setUseNNAPI(false) // More stable
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

            // Parse results - MoveNet outputs normalized coordinates [0,1]
            val keyPoints = mutableListOf<KeyPoint>()

            for (i in 0 until 17) {
                val normalizedY = outputArray[0][0][i][0]
                val normalizedX = outputArray[0][0][i][1]
                val score = outputArray[0][0][i][2]

                // Convert normalized coordinates to bitmap coordinates
                val x = normalizedX * bitmap.width
                val y = normalizedY * bitmap.height

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
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    val cameraPermission = rememberPermissionState(android.Manifest.permission.CAMERA)

    var currentPerson by remember { mutableStateOf<Person?>(null) }
    var poseDetector by remember { mutableStateOf<PoseDetector?>(null) }
    var isModelLoaded by remember { mutableStateOf(false) }
    var lastBitmapSize by remember { mutableStateOf(Pair(640, 480)) } // Default fallback

    // FPS tracking
    var frameCount by remember { mutableIntStateOf(0) }
    var lastFpsTimestamp by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var fps by remember { mutableIntStateOf(0) }

    // Timer for elapsed time
    var startTime by remember { mutableStateOf<Long?>(null) }
    var elapsedTime by remember { mutableLongStateOf(0L) }

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

    // Timer update
    LaunchedEffect(currentPerson) {
        if (currentPerson != null) {
            if (startTime == null) startTime = System.currentTimeMillis()
            elapsedTime = System.currentTimeMillis() - (startTime ?: System.currentTimeMillis())
        } else {
            startTime = null
            elapsedTime = 0L
        }
    }

    if (cameraPermission.status.isGranted) {
        Box(modifier = Modifier.fillMaxSize()) {
            var previewViewRef by remember { mutableStateOf<PreviewView?>(null) }

            if (isModelLoaded) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { context ->
                        PreviewView(context).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            scaleType = PreviewView.ScaleType.FILL_CENTER
                        }
                    },
                    update = { previewView ->
                        previewViewRef = previewView
                        startCamera(
                            context = context,
                            lifecycleOwner = lifecycleOwner,
                            previewView = previewView,
                            poseDetector = poseDetector,
                            onPoseDetected = { person, bitmapWidth, bitmapHeight ->
                                currentPerson = person
                                lastBitmapSize = Pair(bitmapWidth, bitmapHeight)

                                // FPS calculation
                                frameCount++
                                val now = System.currentTimeMillis()
                                if (now - lastFpsTimestamp > 1000) {
                                    fps = frameCount
                                    frameCount = 0
                                    lastFpsTimestamp = now
                                }
                            }
                        )
                    }
                )

                // Overlay Canvas exactly on top of PreviewView
                if (
                    currentPerson != null &&
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
                            drawPose(
                                person = currentPerson!!,
                                canvasWidth = size.width,
                                canvasHeight = size.height,
                                bitmapWidth = lastBitmapSize.first,
                                bitmapHeight = lastBitmapSize.second
                            )
                        }
                    }
                }

                // Statistics overlay with FPS and elapsed time
                PoseStatistics(
                    person = currentPerson,
                    fps = fps,
                    elapsedTimeMs = elapsedTime,
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
    context: Context,
    lifecycleOwner: LifecycleOwner,
    previewView: PreviewView,
    poseDetector: PoseDetector?,
    onPoseDetected: (Person?, Int, Int) -> Unit // Add bitmap size
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
                    try {
                        poseDetector?.let { detector ->
                            val bitmap = imageProxyToBitmap(imageProxy)
                            val person = detector.detectPose(bitmap)
                            onPoseDetected(person, bitmap.width, bitmap.height)
                        }
                    } catch (e: Exception) {
                        Log.e("CameraX", "Error processing frame", e)
                    } finally {
                        imageProxy.close()
                    }
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

// Simple ImageProxy to Bitmap conversion
private fun imageProxyToBitmap(imageProxy: ImageProxy): Bitmap {
    val yBuffer = imageProxy.planes[0].buffer
    val uBuffer = imageProxy.planes[1].buffer
    val vBuffer = imageProxy.planes[2].buffer

    val ySize = yBuffer.remaining()
    val uSize = uBuffer.remaining()
    val vSize = vBuffer.remaining()

    val nv21 = ByteArray(ySize + uSize + vSize)

    // U and V are swapped for NV21 format
    yBuffer.get(nv21, 0, ySize)
    vBuffer.get(nv21, ySize, vSize)
    uBuffer.get(nv21, ySize + vSize, uSize)

    val yuvImage = YuvImage(
        nv21,
        ImageFormat.NV21,
        imageProxy.width,
        imageProxy.height,
        null
    )

    val out = ByteArrayOutputStream()
    yuvImage.compressToJpeg(Rect(0, 0, imageProxy.width, imageProxy.height), 100, out)
    val imageBytes = out.toByteArray()
    val bitmap = android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)

    // Apply rotation and mirroring for front camera
    val matrix = Matrix().apply {
        postRotate(imageProxy.imageInfo.rotationDegrees.toFloat())
        // Mirror for front camera
        postScale(-1f, 1f, bitmap.width / 2f, bitmap.height / 2f)
    }

    return Bitmap.createBitmap(
        bitmap,
        0,
        0,
        bitmap.width,
        bitmap.height,
        matrix,
        true
    )
}

@SuppressLint("DefaultLocale")
private fun DrawScope.drawPose(
    person: Person,
    canvasWidth: Float,
    canvasHeight: Float,
    bitmapWidth: Int,
    bitmapHeight: Int
) {
    val pointRadius = 12f
    val connectionStrokeWidth = 6f

    // Draw skeleton connections first (behind keypoints)
    PoseDetector.bodyConnections.forEach { connection ->
        val startKeyPoint = person.keyPoints[connection.first]
        val endKeyPoint = person.keyPoints[connection.second]

        if (startKeyPoint.score > 0.3 && endKeyPoint.score > 0.3) {
            // Scale keypoints to canvas size using actual bitmap size
            val startX = (startKeyPoint.x / bitmapWidth) * canvasWidth
            val startY = (startKeyPoint.y / bitmapHeight) * canvasHeight
            val endX = (endKeyPoint.x / bitmapWidth) * canvasWidth
            val endY = (endKeyPoint.y / bitmapHeight) * canvasHeight

            drawLine(
                color = Color.Green,
                start = androidx.compose.ui.geometry.Offset(startX, startY),
                end = androidx.compose.ui.geometry.Offset(endX, endY),
                strokeWidth = connectionStrokeWidth
            )
        }
    }

    // Draw keypoints on top
    person.keyPoints.forEachIndexed { index, keyPoint ->
        if (keyPoint.score > 0.3) {
            // Scale keypoints to canvas size using actual bitmap size
            val x = (keyPoint.x / bitmapWidth) * canvasWidth
            val y = (keyPoint.y / bitmapHeight) * canvasHeight

            val color = when (index) {
                0, 1, 2, 3, 4 -> Color.Red // Head
                5, 6, 7, 8, 9, 10 -> Color.Blue // Arms
                11, 12 -> Color.Magenta // Hips
                13, 14, 15, 16 -> Color.Cyan // Legs
                else -> Color.White
            }

            drawCircle(
                color = color,
                radius = pointRadius,
                center = androidx.compose.ui.geometry.Offset(x, y)
            )

            // Draw confidence score
            if (keyPoint.score > 0.5) {
                drawContext.canvas.nativeCanvas.apply {
                    drawText(
                        String.format("%.2f", keyPoint.score),
                        x + pointRadius + 5f,
                        y - pointRadius,
                        android.graphics.Paint().apply {
                            textSize = 24f
                            isAntiAlias = true
                            setShadowLayer(2f, 1f, 1f, android.graphics.Color.BLACK)
                        }
                    )
                }
            }
        }
    }
}

@SuppressLint("DefaultLocale")
@Composable
private fun PoseStatistics(
    person: Person?,
    fps: Int = 0,
    elapsedTimeMs: Long = 0L,
    @SuppressLint("ModifierParameter") modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .width(160.dp)
            .padding(4.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color.Black.copy(alpha = 0.18f))
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = "FPS: $fps | ${elapsedTimeMs / 1000}s",
                color = Color.Yellow,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            if (person != null) {
                Text(
                    text = "Score: ${String.format("%.2f", person.score)}",
                    color = Color.White,
                    fontSize = 12.sp
                )
                val highConfidenceKeypoints = person.keyPoints.count { it.score > 0.5 }
                val visibleKeypoints = person.keyPoints.count { it.score > 0.3 }
                Text(
                    text = "High: $highConfidenceKeypoints  Vis: $visibleKeypoints",
                    color = Color.White,
                    fontSize = 12.sp
                )
                val avgConfidence = person.keyPoints.map { it.score }.average()
                val minConfidence = person.keyPoints.minByOrNull { it.score }
                val maxConfidence = person.keyPoints.maxByOrNull { it.score }
                Text(
                    text = "Avg: ${String.format("%.2f", avgConfidence)}",
                    color = Color.LightGray,
                    fontSize = 11.sp
                )
                Text(
                    text = "Min: ${String.format("%.2f", minConfidence?.score ?: 0f)}",
                    color = Color.Red,
                    fontSize = 11.sp
                )
                Text(
                    text = "Max: ${String.format("%.2f", maxConfidence?.score ?: 0f)}",
                    color = Color.Green,
                    fontSize = 11.sp
                )
            } else {
                Text(
                    text = "No pose",
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