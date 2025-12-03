# Feature Extraction Angle Reference Guide

## Supported Angles & Measurements by Exercise

### PUSHUP
**Primary Angles:**
- `leftArmFlexion` & `rightArmFlexion` - Track elbow angle during movement
  - Down position: < 85°
  - Up position: > 160°

**Supporting Angles:**
- `spineAlignment` - Maintain straight body line
- `shoulderLevel` - Symmetry between shoulders
- `armElevation` - Relative shoulder height

### SQUAT
**Primary Angles:**
- `leftKneeFlexion` & `rightKneeFlexion` - Track knee bend
  - Down position: < 90°
  - Up position: > 160°

**Supporting Angles:**
- `spineAlignment` - Keep torso upright
- `torsoLean` - Forward lean (should be minimal < 30°)
- `legAlignment` - Symmetry between legs

### PLANK
**Primary Angles:**
- `spineAlignment` - Horizontal body alignment (target > 75)
- `armElevation` - Shoulder position stability

**Supporting Measurements:**
- `shoulderLevel` - Minimal side tilt
- `hipAlignment` - Keep hips level

### JUMPING_JACKS
**Primary Measurements:**
- Hip position changes (via keypoints)
- `hipAlignment` - Detects leg position changes

**Supporting Angles:**
- `armElevation` - Arm raising pattern
- `shoulderLevel` - Shoulder symmetry

### DUMBBELL_CURL
**Primary Angles:**
- `leftArmFlexion` & `rightArmFlexion` - Elbow angle throughout movement
  - Top position: < 30°
  - Bottom position: > 160°

**Supporting Metrics:**
- `shoulderLevel` - Stability (minimize shoulder shrugging)
- `spineAlignment` - Maintain upright posture
- `armElevation` - Prevent shoulder elevation

## General Measurements

### All Exercises Track:

1. **Upper Body**
   - `neckFlexion` (degrees) - Forward head posture indicator
   - `headTilt` (degrees) - Side-to-side head tilt
   - `spineAlignment` (pixels) - Center-line deviation
   - `torsoLean` (degrees) - Forward/backward lean
   - `shoulderLevel` (%) - Left-right asymmetry
   - `armElevation` (pixels) - Height relative to hips

2. **Lower Body**
   - `hipAlignment` (%) - Left-right hip symmetry
   - `leftKneeFlexion` (degrees) - Left leg bend
   - `rightKneeFlexion` (degrees) - Right leg bend
   - `legAlignment` (degrees) - Symmetry between legs

3. **Detection Quality**
   - `detectedJointCount` (count) - Number of reliably detected joints
   - `confidenceMetrics` (map) - Per-angle confidence scores

## Keypoint Indices (MoveNet 17-point)

```
0: NOSE
1: LEFT_EYE
2: RIGHT_EYE
3: LEFT_EAR
4: RIGHT_EAR
5: LEFT_SHOULDER
6: RIGHT_SHOULDER
7: LEFT_ELBOW
8: RIGHT_ELBOW
9: LEFT_WRIST
10: RIGHT_WRIST
11: LEFT_HIP
12: RIGHT_HIP
13: LEFT_KNEE
14: RIGHT_KNEE
15: LEFT_ANKLE
16: RIGHT_ANKLE
```

## Angle Calculation Methods

All angles are calculated using:
- **3-point angles**: Using two vectors from a joint (e.g., shoulder-elbow-wrist)
- **Alignment metrics**: Horizontal/vertical deviation from reference lines
- **Distance calculations**: Measuring separation between body parts
- **Asymmetry ratios**: Comparing left vs right measurements

## Data Flow for ML Model

```
Raw Video Frame
    ↓
Pose Detection (MoveNet) → 17 Keypoints
    ↓
Feature Extraction → PostureFeatures
    ├─ neckFlexion (°)
    ├─ headTilt (°)
    ├─ spineAlignment (px)
    ├─ torsoLean (°)
    ├─ shoulderLevel (%)
    ├─ leftArmFlexion (°)
    ├─ rightArmFlexion (°)
    ├─ armElevation (px)
    ├─ hipAlignment (%)
    ├─ leftKneeFlexion (°)
    ├─ rightKneeFlexion (°)
    ├─ legAlignment (°)
    └─ detectedJointCount
    ↓
[Ready for ML Model Input]
    ↓
ML Model
├─ Posture Classification
├─ Form Quality Scoring
└─ Personalized Recommendations
```

## Exercise Type Auto-Detection Criteria

**PUSHUP**: Horizontal body + bent elbows + good pose detection
**SQUAT**: Bent knees + upright torso + legs changing angle
**PLANK**: Horizontal alignment + extended arms + static
**JUMPING_JACKS**: Significant hip position changes + arm movements
**DUMBBELL_CURL**: Standing upright + repeated arm flexion/extension

## Confidence Thresholds

- Keypoint confidence: 0.3 (30% minimum for inclusion)
- Minimum keypoints to process: 10 joints
- Exercise detection confidence: 0.5 (50%)
- Rep counting confidence: 0.6 (60%) minimum

## Future Model Features

Once ML model is trained, add:
- Posture quality classification (0-100)
- Form feedback per angle
- Personalized correction suggestions
- Rep quality scoring
- Injury risk assessment
- Progressive difficulty recommendations

