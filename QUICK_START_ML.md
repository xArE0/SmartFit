# Quick Start - Feature Extraction for ML Integration

## 🚀 Getting Started

Your SmartFit project is now ready for ML model integration. Here's how to proceed:

---

## 1️⃣ Understanding the Architecture

### Data Flow
```
Camera Feed
    ↓
MoveNet Pose Detection (17 keypoints)
    ↓
Feature Extraction (12 angles)
    ↓
Exercise Detection (type & reps)
    ↓
Display Raw Metrics
    ↓
[Ready for ML Model]
```

### Key Classes

**FeatureExtractor.kt** - Core feature calculation
```kotlin
// Extracts 12 angles from pose keypoints
val features = featureExtractor.extractFeatures(person)
```

**ExerciseDetector.kt** - Exercise identification
```kotlin
// Detects exercise type and counts reps
val exerciseState = exerciseDetector.processFrame(person, features, exercise)
```

**PoseDetectionViewModel.kt** - State management
```kotlin
// Manages detection state and provides UI updates
val state = viewModel.state.collectAsState()
```

---

## 2️⃣ Extracted Features (12 Angles)

All values ready for ML model input:

```kotlin
data class PostureFeatures(
    // Head & Neck (2)
    neckFlexion: Float,           // degrees (-90 to +90)
    headTilt: Float,              // degrees (-45 to +45)
    
    // Spine & Core (3)
    spineAlignment: Float,        // pixels deviation
    torsoLean: Float,             // degrees
    shoulderLevel: Float,         // % asymmetry
    
    // Arms (3)
    leftArmFlexion: Float,        // degrees (0 to 180)
    rightArmFlexion: Float,       // degrees (0 to 180)
    armElevation: Float,          // pixels relative to hips
    
    // Legs (4)
    hipAlignment: Float,          // % asymmetry
    leftKneeFlexion: Float,       // degrees (0 to 180)
    rightKneeFlexion: Float,      // degrees (0 to 180)
    legAlignment: Float,          // degree difference
    
    // Metadata (2)
    detectedJointCount: Int,      // 0-17 joints
    confidenceMetrics: Map<String, Float>
)
```

---

## 3️⃣ Supported Exercises

```kotlin
enum class ExerciseType {
    PUSHUP,           // Elbow angle tracking
    SQUAT,            // Knee angle tracking
    PLANK,            // Static hold detection
    JUMPING_JACKS,    // Hip position changes
    DUMBBELL_CURL,    // Elbow flexion pattern
    UNKNOWN
}
```

---

## 4️⃣ Building an ML Model

### Step 1: Collect Training Data

```kotlin
// In your data collection script
val trainingData = mutableListOf<Pair<PostureFeatures, String>>()

// While user exercises
val features = featureExtractor.extractFeatures(person)
val qualityLabel = getUserRating()  // "good", "bad", "perfect", etc.

trainingData.add(features to qualityLabel)
```

### Step 2: Prepare Feature Vectors

```kotlin
// Convert to ML-friendly format
val featureVector = floatArrayOf(
    features.neckFlexion,
    features.headTilt,
    features.spineAlignment,
    features.torsoLean,
    features.shoulderLevel,
    features.leftArmFlexion,
    features.rightArmFlexion,
    features.armElevation,
    features.hipAlignment,
    features.leftKneeFlexion,
    features.rightKneeFlexion,
    features.legAlignment
)
```

### Step 3: Train Model

```python
# Example using TensorFlow
import tensorflow as tf
from sklearn.preprocessing import StandardScaler

# Load collected data
X = np.array([feature_vectors])  # Shape: (n_samples, 12)
y = np.array([labels])           # Shape: (n_samples,)

# Normalize features
scaler = StandardScaler()
X = scaler.fit_transform(X)

# Build model
model = tf.keras.Sequential([
    tf.keras.layers.Dense(64, activation='relu', input_shape=(12,)),
    tf.keras.layers.Dropout(0.3),
    tf.keras.layers.Dense(32, activation='relu'),
    tf.keras.layers.Dropout(0.2),
    tf.keras.layers.Dense(3, activation='softmax')  # good, fair, poor
])

model.compile(optimizer='adam', loss='sparse_categorical_crossentropy')
model.fit(X, y, epochs=50, batch_size=32)

# Convert to TensorFlow Lite
converter = tf.lite.TFLiteConverter.from_keras_model(model)
tflite_model = converter.convert()
```

### Step 4: Integrate into App

```kotlin
// Load and run model
private var interpreter: Interpreter? = null

init {
    val modelBuffer = loadModelFile("posture_quality_model.tflite")
    interpreter = Interpreter(modelBuffer)
}

fun predictPostureQuality(features: PostureFeatures): Float {
    val input = floatArrayOf(
        features.neckFlexion,
        features.headTilt,
        // ... all 12 features
    ).reshape(1, 12)
    
    val output = Array(1) { FloatArray(3) }  // 3 classes
    interpreter?.run(input, output)
    
    return output[0][0] * 100  // Convert to 0-100
}
```

---

## 5️⃣ Usage Examples

### Example 1: Get Features During Exercise

```kotlin
// In PoseDetectionViewModel
fun processCameraFrame(bitmap: Bitmap) {
    val person = poseDetector.detectPose(bitmap)
    
    if (person != null) {
        val features = featureExtractor.extractFeatures(person)
        
        // Use features for ML prediction
        Log.d("Features", """
            Neck Flexion: ${features.neckFlexion}°
            Left Arm: ${features.leftArmFlexion}°
            Right Arm: ${features.rightArmFlexion}°
            Left Knee: ${features.leftKneeFlexion}°
            Right Knee: ${features.rightKneeFlexion}°
            Joints Detected: ${features.detectedJointCount}
        """.trimIndent())
    }
}
```

### Example 2: Exercise Detection

```kotlin
// Auto-detect exercise and track reps
fun startAutoDetection() {
    val person = poseDetector.detectPose(bitmap)
    val features = featureExtractor.extractFeatures(person)
    
    // Auto-detect exercise type
    val exerciseType = exerciseDetector.detectExerciseType(person, features)
    
    // Process frame for rep counting
    val state = exerciseDetector.processFrame(person, features, exerciseType)
    
    Log.d("Exercise", "Type: $exerciseType, Reps: ${state.repCount}")
}
```

### Example 3: Extract Specific Angles

```kotlin
fun analyzePostureAngles(features: PostureFeatures) {
    when {
        // Check neck posture
        features.neckFlexion > 15 -> Log.w("Posture", "Forward head detected")
        
        // Check spine alignment
        features.spineAlignment < 50 -> Log.w("Posture", "Poor spine alignment")
        
        // Check arm symmetry
        abs(features.leftArmFlexion - features.rightArmFlexion) > 20 -> 
            Log.w("Posture", "Asymmetrical arm angles")
        
        // Check leg symmetry
        abs(features.leftKneeFlexion - features.rightKneeFlexion) > 15 -> 
            Log.w("Posture", "Asymmetrical leg angles")
    }
}
```

---

## 6️⃣ Common Integration Patterns

### Pattern 1: Real-time Posture Feedback

```kotlin
// Continuous feature extraction and ML prediction
val features = featureExtractor.extractFeatures(person)
val quality = mlModel.predict(features)

when {
    quality >= 85 -> showGreenFeedback("Perfect form!")
    quality >= 70 -> showYellowFeedback("Good form")
    quality >= 50 -> showOrangeFeedback("Adjust posture")
    else -> showRedFeedback("Poor form")
}
```

### Pattern 2: Rep-based Feedback

```kotlin
// Provide feedback per repetition
val exerciseState = exerciseDetector.processFrame(person, features, exercise)

if (exerciseState.repCount > previousRepCount) {
    val quality = mlModel.predict(features)
    showRepFeedback(exerciseState.repCount, quality)
    previousRepCount = exerciseState.repCount
}
```

### Pattern 3: Session Analytics

```kotlin
// Collect metrics for session summary
data class SessionMetrics(
    val totalReps: Int,
    val averageQuality: Float,
    val bestRep: Float,
    val worstRep: Float,
    val exerciseType: ExerciseType
)

fun generateSessionSummary(metrics: SessionMetrics) {
    // Display to user at end of workout
}
```

---

## 7️⃣ Debugging & Validation

### Verify Feature Extraction

```kotlin
// Check if features are being extracted correctly
fun validateFeatures(features: PostureFeatures) {
    assert(features.detectedJointCount >= 10) { "Too few joints detected" }
    assert(features.neckFlexion in -90f..90f) { "Invalid neck flexion" }
    assert(features.leftArmFlexion in 0f..180f) { "Invalid arm angle" }
    assert(features.confidenceMetrics.isNotEmpty()) { "No confidence data" }
}
```

### Monitor Keypoint Detection

```kotlin
// Log detected keypoints per frame
fun monitorDetection(person: Person) {
    val detected = person.keyPoints.count { it.score > 0.3 }
    Log.d("Detection", "Keypoints: $detected/17, Confidence: ${person.score}")
    
    // Track which keypoints have issues
    person.keyPoints.forEachIndexed { i, kp ->
        if (kp.score < 0.3) Log.w("Detection", "Keypoint $i low confidence")
    }
}
```

---

## 8️⃣ Performance Tips

### Optimization 1: Batch Processing
```kotlin
// Process multiple frames before updating UI
private var frameBuffer = mutableListOf<PostureFeatures>()

fun processCameraFrame(bitmap: Bitmap) {
    val features = featureExtractor.extractFeatures(poseDetector.detectPose(bitmap))
    frameBuffer.add(features)
    
    if (frameBuffer.size >= 5) {
        val avgFeatures = averageFeatures(frameBuffer)
        updateUI(avgFeatures)
        frameBuffer.clear()
    }
}
```

### Optimization 2: Reduce Inference Frequency
```kotlin
// Run ML model every N frames instead of every frame
private var inferenceCounter = 0

fun processCameraFrame(bitmap: Bitmap) {
    val features = featureExtractor.extractFeatures(poseDetector.detectPose(bitmap))
    
    if (++inferenceCounter % 3 == 0) {  // Every 3 frames
        val prediction = mlModel.predict(features)
        updateUI(prediction)
    }
}
```

---

## 📚 Documentation References

- **FEATURE_EXTRACTION_ANGLES.md** - Complete angle reference
- **FEATURE_EXTRACTION_CLEANUP.md** - What was changed
- **VERIFICATION_REPORT.md** - Build verification details

---

## ✅ Checklist for ML Integration

- [ ] Collected training data (100+ samples per exercise)
- [ ] Annotated data with quality labels
- [ ] Trained and validated ML model
- [ ] Converted model to TensorFlow Lite format
- [ ] Tested model accuracy on validation set
- [ ] Added model interpreter to app
- [ ] Integrated predictions into UI
- [ ] Tested on device
- [ ] Gathered user feedback
- [ ] Iterated on model

---

## 🤔 FAQ

**Q: Why were scores removed?**
A: Hardcoded rules don't generalize. ML models trained on real data will be more accurate.

**Q: How many samples do I need for training?**
A: Start with 100-200 per exercise/quality level. More is better.

**Q: Can I use transfer learning?**
A: Yes! You can fine-tune existing fitness models with your collected data.

**Q: What's the minimum FPS needed?**
A: 30 FPS for smooth real-time feedback. Features are calculated per frame.

**Q: How do I handle poor lighting?**
A: Train with diverse lighting conditions. Confidence metrics help detect issues.

---

## 🚀 Ready to Build?

1. Open Android Studio
2. Build the project: `./gradlew build`
3. Collect training data using the app
4. Train your ML model
5. Add model file to app
6. Integrate predictions
7. Deploy!

**Status: ✅ READY FOR ML INTEGRATION**

