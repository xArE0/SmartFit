# MediaPipe Pose Model

## Required Model File

You need to download the MediaPipe Pose Landmarker model and place it in this directory.

**Model Name:** `pose_landmarker_lite.task`

**Download Link:** https://storage.googleapis.com/mediapipe-models/pose_landmarker/pose_landmarker_lite/float16/latest/pose_landmarker_lite.task

## How to Download

### Option 1: Direct Download
1. Click the link above or visit: https://developers.google.com/mediapipe/solutions/vision/pose_landmarker#models
2. Download `pose_landmarker_lite.task`
3. Place it in `app/src/main/assets/`

### Option 2: Using wget/curl
```bash
cd app/src/main/assets
wget https://storage.googleapis.com/mediapipe-models/pose_landmarker/pose_landmarker_lite/float16/latest/pose_landmarker_lite.task
```

## Model Variants

MediaPipe offers three pose models:
- **Lite** (recommended for mobile): Fast, good accuracy
- **Full**: Better accuracy, slower
- **Heavy**: Best accuracy, slowest

We're using **Lite** for real-time performance on mobile devices.

## File Size
Approximately 12 MB

## Verification
After downloading, verify the file exists:
- Path: `app/src/main/assets/pose_landmarker_lite.task`
- Size: ~12 MB
