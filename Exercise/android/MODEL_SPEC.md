# Exercise Classifier — Android Integration Guide

## Files
| File | Description |
|---|---|
| `exercise_classifier.tflite` | TFLite model (482.3 KB) |
| `labels.txt` | Class labels (one per line, 7 classes) |
| `model_spec.json` | Machine-readable model specification |

## Model Details
- **Architecture**: Bidirectional LSTM (Keras → TFLite)
- **Training Accuracy**: 100% on test set
- **Input**: `float32[1, 180, 66]` — 180 frames × 22 keypoints × 3 coordinates (x, y, z)
- **Output**: `float32[1, 7]` — Softmax probabilities for 7 classes
- **Coordinate Format**: Normalized (0.0 to 1.0), from MediaPipe Pose

## Classes (Index → Label)
| Index | Label |
|---|---|
| 0 | hammer curl |
| 1 | lateral raise |
| 2 | leg raises |
| 3 | plank |
| 4 | push-up |
| 5 | russian twist |
| 6 | squat |

## Keypoint Mapping (22 Keypoints)

Each frame contains 66 values: 22 keypoints × 3 coordinates (x, y, z).

| Model Index | Body Part | MediaPipe Index |
|---|---|---|
| 0  | Nose | 0 |
| 1  | Left Eye | 2 |
| 2  | Right Eye | 5 |
| 3  | Left Ear | 7 |
| 4  | Right Ear | 8 |
| 5  | Left Shoulder | 11 |
| 6  | Right Shoulder | 12 |
| 7  | Left Elbow | 13 |
| 8  | Right Elbow | 14 |
| 9  | Left Wrist | 15 |
| 10 | Right Wrist | 16 |
| 11 | Left Hip | 23 |
| 12 | Right Hip | 24 |
| 13 | Left Knee | 25 |
| 14 | Right Knee | 26 |
| 15 | Left Ankle | 27 |
| 16 | Right Ankle | 28 |
| 17 | Left Heel | 29 |
| 18 | Right Heel | 30 |
| 19 | Left Foot Index | 31 |
| 20 | Right Foot Index | 32 |
| 21 | Nose (duplicate) | 0 |

## ⚠️ Important: Flex Ops Required

This model uses LSTM layers which require **TensorFlow Lite Flex delegate** on Android.

### Gradle Dependencies
```groovy
dependencies {
    implementation 'org.tensorflow:tensorflow-lite:2.16.1'
    implementation 'org.tensorflow:tensorflow-lite-select-tf-ops:2.16.1'
}
```

## Android Integration (Kotlin)

### 1. Place Files
Copy `exercise_classifier.tflite` and `labels.txt` into `app/src/main/assets/`.

### 2. Load Model
```kotlin
val model = Interpreter(loadModelFile("exercise_classifier.tflite"))

private fun loadModelFile(filename: String): MappedByteBuffer {
    val fd = assets.openFd(filename)
    val input = FileInputStream(fd.fileDescriptor)
    val channel = input.channel
    return channel.map(FileChannel.MapMode.READ_ONLY, fd.startOffset, fd.declaredLength)
}
```

### 3. Prepare Input
```kotlin
// Collect 180 frames of pose landmarks
// Each frame: 22 keypoints × 3 coords = 66 floats
val input = Array(1) { Array(180) { FloatArray(66) } }

// For each frame, map MediaPipe landmarks to model keypoints:
val mpToModel = intArrayOf(0, 2, 5, 7, 8, 11, 12, 13, 14, 15, 16, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 0)

fun fillFrame(frameIndex: Int, poseLandmarks: List<NormalizedLandmark>) {
    for ((modelIdx, mpIdx) in mpToModel.withIndex()) {
        val lm = poseLandmarks[mpIdx]
        input[0][frameIndex][modelIdx * 3 + 0] = lm.x()  // 0.0-1.0
        input[0][frameIndex][modelIdx * 3 + 1] = lm.y()  // 0.0-1.0
        input[0][frameIndex][modelIdx * 3 + 2] = lm.z()  // depth
    }
}
```

### 4. Run Inference
```kotlin
val output = Array(1) { FloatArray(7) }
model.run(input, output)

val probabilities = output[0]
val predictedIndex = probabilities.indices.maxByOrNull { probabilities[it] } ?: 0
val predictedLabel = labels[predictedIndex]
val confidence = probabilities[predictedIndex]
```

### 5. Interpolation (if frames ≠ 180)
```kotlin
// If you collected N frames (N ≠ 180), interpolate to 180:
fun interpolateSequence(frames: List<FloatArray>, targetLen: Int = 180): Array<FloatArray> {
    val n = frames.size
    return Array(targetLen) { t ->
        val srcIdx = t.toFloat() * (n - 1) / (targetLen - 1)
        val lo = srcIdx.toInt().coerceIn(0, n - 2)
        val hi = (lo + 1).coerceIn(0, n - 1)
        val frac = srcIdx - lo
        FloatArray(66) { i -> frames[lo][i] * (1 - frac) + frames[hi][i] * frac }
    }
}
```

## Input Details
```
Name:  serving_default_input_1:0
Shape: [1, 180, 66]
Type:  float32
```

## Output Details
```
Name:  StatefulPartitionedCall:0
Shape: [1, 7]
Type:  float32
```
