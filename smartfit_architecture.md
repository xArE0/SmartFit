# SmartFit — System Architecture

---

## Core System 1 — Session Tracking

Captures raw video, preprocesses frames, and estimates body pose keypoints.

**Modules:** Camera / Image Input · Image Preprocessing · Pose Estimation

| Module | Input | Output |
|--------|-------|--------|
| Camera / Image Input | Device camera hardware, front/back toggle | `ImageProxy` frame |
| Image Preprocessing | `ImageProxy` | `MPImage` + timestamp |
| Pose Estimation | `MPImage` | `NormalizedLandmarks` list + `isPoseVisible` flag |

```mermaid
flowchart LR
    CAM["Device Camera\nHardware"] -->|raw video| IA["CameraX\nImageAnalysis"]
    UI_FLIP["User: Front/Back\nToggle"] -->|Camera Selector| IA
    IA -->|ImageProxy| CONV["imageProxyToBitmap()\nBitmapImageBuilder"]
    CLOCK["SystemClock\ntimestamp"] --> CONV
    CONV -->|MPImage + timestamp| PL["PoseLandmarker\nMediaPipe async"]
    PL --> VIS{"Visibility\nFilter"}
    VIS -->|valid pose| LM["NormalizedLandmarks\n→ Performance Analysis"]
    VIS -->|low confidence| OV["isPoseVisible=false\n→ UI Overlay"]
```

---

## Core System 2 — Performance Analysis

Analyses structured keypoints to classify exercises, count reps, and evaluate form.

**Modules:** Exercise Tracking · Posture Analysis

| Module | Input | Output |
|--------|-------|--------|
| Exercise Tracking | `NormalizedLandmarks` | `lockedExercise`, `repCount` |
| Posture Analysis | `NormalizedLandmarks` + exercise label | `formFeedback`, `formScore` |

```mermaid
flowchart TD
    LM["NormalizedLandmarks\n(from Session Tracking)"] --> CLS["ExerciseClassifier\nTFLite 6-class MLP"]
    CLS -->|confidence ≥ 0.7| LOCK["Lock-on Phase\nSCANNING → LOCKED_IN"]
    LOCK -->|lockedExercise| RC["RepCounter\nangle state machine"]
    LOCK -->|exercise label| FE["ExerciseFeedbackEngine\njoint angle rules"]
    LM --> FE
    VIS["isPoseVisible\n(Session Tracking)"] -.->|gate| RC
    RC -->|repCount| DS["→ Data Storage"]
    FE -->|formFeedback tip| FG["→ Feedback Generation"]
    FE -->|formScore 0-100| DS
```

---

## Core System 3 — Exercise Data Processing

Converts analysis outputs into audio/visual coaching and persists all workout data.

**Modules:** Feedback Generation · Data Storage

| Module | Input | Output |
|--------|-------|--------|
| Feedback Generation | `formFeedback`, `repCount`, `isPoseVisible` | Spoken tip (TTS), visual HUD |
| Data Storage | `WorkoutSession`, plans, challenge state | Persisted JSON / SharedPreferences |

```mermaid
flowchart TD
    FF["formFeedback\n(Posture Analysis)"] --> TTS["VoiceFeedbackManager\nTTS + 8s cooldown"]
    RP["repCount + exercise\n(Exercise Tracking)"] --> HUD["Visual HUD Renderer\nWorkoutHud / ScanningHud"]
    PV["isPoseVisible\n(Session Tracking)"] -->|500ms debounce| OVL["Position-yourself\nOverlay"]
    TTS -->|spoken tip| SPK["Speaker"]
    HUD --> UI["→ User Interface"]
    OVL --> UI

    WS["WorkoutSession\n(reps, score, duration)"] --> WR["WorkoutRepository\nJSON file I/O"]
    EP["Custom ExercisePlan"] --> CPR["CustomPlanRepository\nSharedPreferences"]
    CH["Challenge Progress\n(day, streak)"] --> CHR["ChallengeRepository\nSharedPreferences"]
    WR --> UI
    CPR --> UI
    CHR --> UI
```

---

## Core System 4 — User Profile Management

Handles all screens, navigation, and user identity/profile data.

**Modules:** User Interface · User Profile Database

| Module | Input | Output |
|--------|-------|--------|
| User Interface | Data from Storage, HUD state, user gestures | Rendered screens, navigation events |
| User Profile Database | Profile fields, auth state | User identity + goals for personalisation |

```mermaid
flowchart TD
    USR["User Gestures\n& Taps"] --> COMP["Jetpack Compose\nScreen Tree"]
    COMP --> NAV["Navigation.kt\nNavHostController"]
    NAV --> BNB["BottomNavBar\n5 Tabs"]
    NAV --> CAM_S["CameraScreen\n→ Session Tracking"]
    NAV --> EIS["ExerciseInfoScreen\nExerciseAnimationPlayer"]
    NAV --> PRG["ProgressScreen\nWorkoutSummaryScreen"]

    DS["Data Storage\n(Core System 3)"] -->|plans, sessions| COMP
    FG["Feedback Generation\n(Core System 3)"] -->|HUD state| CAM_S

    PROF_ED["Profile Screen\nOnboarding edits"] --> SM["SessionManager\nSharedPreferences"]
    PROF_ED -->|if online| FS["Firebase Firestore\nusers/uid"]
    FS -->|sync on login| SM
    SM -->|userId, goals| COMP
    SM -->|session tagging| DS
```

---

## Full Cross-System Overview

```mermaid
flowchart TD
    subgraph ST["① Session Tracking"]
        direction LR
        ST1["Camera /\nImage Input"] --> ST2["Image\nPreprocessing"] --> ST3["Pose\nEstimation"]
    end

    subgraph PA["② Performance Analysis"]
        direction LR
        PA2["Exercise\nTracking"] --> PA1["Posture\nAnalysis"]
    end

    subgraph EDP["③ Exercise Data Processing"]
        direction LR
        EDP1["Feedback\nGeneration"] --- EDP2["Data\nStorage"]
    end

    subgraph UPM["④ User Profile Management"]
        direction LR
        UPM2["User Profile\nDatabase"] --> UPM1["User Interface"]
    end

    ST3 --> PA2
    ST3 --> PA1
    ST3 -.->|isPoseVisible| EDP1
    PA2 --> EDP1
    PA2 --> EDP2
    PA1 --> EDP1
    PA1 --> EDP2
    EDP1 --> UPM1
    EDP2 --> UPM1
    EDP2 --> UPM2
    UPM1 -->|camera start / plan| ST1
```
