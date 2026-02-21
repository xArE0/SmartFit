"""
SmartFit Exercise Classifier — Training & Evaluation
=====================================================
Architecture  : Bidirectional LSTM  (matches exercise_classifier.tflite)
Input shape   : (batch, 180, 66)   — 180 frames × 22 keypoints × 3 coords
Output classes: 7  (see labels.txt)

Data layout expected
--------------------
DATA_DIR/
  hammer curl/    ← folder name = class label
    seq_001.npy   ← numpy array of shape (180, 66), dtype float32
    seq_002.npy
    ...
  lateral raise/
    ...
  (one folder per class, matching labels.txt)

Run
---
  python train_and_evaluate.py                         # uses default DATA_DIR
  python train_and_evaluate.py --data path/to/data     # custom data path
  python train_and_evaluate.py --data path --epochs 80 --batch 16
"""

import argparse, os, sys
import numpy as np
import matplotlib
matplotlib.use("Agg")          # headless — saves PNGs without a display
import matplotlib.pyplot as plt
import matplotlib.gridspec as gridspec
from pathlib import Path

# ── labels from labels.txt ── (order must match training folders)
LABELS = [
    "hammer curl",
    "lateral raise",
    "leg raises",
    "plank",
    "push-up",
    "russian twist",
    "squat",
]
NUM_CLASSES   = len(LABELS)
SEQ_LEN       = 180
FEATURE_DIM   = 66   # 22 keypoints × 3 coords


# ─────────────────────────────────────────────
# 1.  DATA LOADING
# ─────────────────────────────────────────────
def load_dataset(data_dir: str):
    """
    Walk DATA_DIR looking for sub-folders named after each label.
    Returns X: (N, 180, 66)  y: (N,) int class indices.
    """
    data_dir = Path(data_dir)
    X, y = [], []

    for class_idx, label in enumerate(LABELS):
        class_dir = data_dir / label
        if not class_dir.is_dir():
            # Try a case-insensitive match
            matches = [d for d in data_dir.iterdir()
                       if d.is_dir() and d.name.lower() == label.lower()]
            if not matches:
                print(f"  [WARN] folder not found for class '{label}' — skipping")
                continue
            class_dir = matches[0]

        files = list(class_dir.glob("*.npy"))
        if not files:
            print(f"  [WARN] no .npy files in {class_dir} — skipping")
            continue

        for f in files:
            seq = np.load(str(f)).astype(np.float32)
            if seq.shape != (SEQ_LEN, FEATURE_DIM):
                # Attempt reshape or skip
                if seq.size == SEQ_LEN * FEATURE_DIM:
                    seq = seq.reshape(SEQ_LEN, FEATURE_DIM)
                else:
                    print(f"  [WARN] unexpected shape {seq.shape} in {f} — skipping")
                    continue
            X.append(seq)
            y.append(class_idx)

    if not X:
        sys.exit("ERROR: No data loaded. Check DATA_DIR and folder names.")

    return np.array(X, dtype=np.float32), np.array(y, dtype=np.int32)


# ─────────────────────────────────────────────
# 2.  MODEL DEFINITION  (mirrors the TFLite model)
# ─────────────────────────────────────────────
def build_model():
    import tensorflow as tf
    from tensorflow import keras

    inp = keras.Input(shape=(SEQ_LEN, FEATURE_DIM), name="input")

    x = keras.layers.Bidirectional(
            keras.layers.LSTM(128, return_sequences=True))(inp)
    x = keras.layers.Dropout(0.3)(x)

    x = keras.layers.Bidirectional(
            keras.layers.LSTM(64))(x)
    x = keras.layers.Dropout(0.3)(x)

    x = keras.layers.Dense(64, activation="relu")(x)
    x = keras.layers.Dropout(0.3)(x)

    out = keras.layers.Dense(NUM_CLASSES, activation="softmax", name="output")(x)

    model = keras.Model(inp, out)
    model.compile(
        optimizer=keras.optimizers.Adam(1e-3),
        loss="sparse_categorical_crossentropy",
        metrics=["accuracy"],
    )
    return model


# ─────────────────────────────────────────────
# 3.  PLOTTING HELPERS
# ─────────────────────────────────────────────
ACCENT  = "#6C63FF"   # purple
ACCENT2 = "#FF6584"   # pink / secondary
BG      = "#0F0F1A"
PANEL   = "#1C1C2E"
TEXT    = "#E8E8F0"
GRID    = "#2A2A3E"

plt.rcParams.update({
    "figure.facecolor":  BG,
    "axes.facecolor":    PANEL,
    "axes.edgecolor":    GRID,
    "axes.labelcolor":   TEXT,
    "xtick.color":       TEXT,
    "ytick.color":       TEXT,
    "text.color":        TEXT,
    "grid.color":        GRID,
    "grid.linewidth":    0.6,
    "legend.framealpha": 0.15,
    "legend.edgecolor":  GRID,
    "font.family":       "sans-serif",
    "font.size":         10,
})


def save_loss_accuracy_plot(history, out_path: str):
    epochs = range(1, len(history["loss"]) + 1)

    fig, axes = plt.subplots(1, 2, figsize=(14, 5))
    fig.suptitle("Training Metrics — SmartFit Exercise Classifier",
                 fontsize=14, fontweight="bold", color=TEXT, y=1.01)

    # ── Loss ──
    ax = axes[0]
    ax.plot(epochs, history["loss"],     color=ACCENT,  lw=2,   label="Train Loss")
    ax.plot(epochs, history["val_loss"], color=ACCENT2, lw=2,   label="Val Loss",   linestyle="--")
    ax.set_title("Loss over Epochs", color=TEXT, fontweight="bold")
    ax.set_xlabel("Epoch")
    ax.set_ylabel("Loss")
    ax.legend()
    ax.grid(True)
    ax.set_xlim(1, max(epochs))

    # ── Accuracy ──
    ax = axes[1]
    ax.plot(epochs, history["accuracy"],     color=ACCENT,  lw=2, label="Train Accuracy")
    ax.plot(epochs, history["val_accuracy"], color=ACCENT2, lw=2, label="Val Accuracy", linestyle="--")
    ax.set_title("Accuracy over Epochs", color=TEXT, fontweight="bold")
    ax.set_xlabel("Epoch")
    ax.set_ylabel("Accuracy")
    ax.set_ylim(0, 1.05)
    ax.legend()
    ax.grid(True)
    ax.set_xlim(1, max(epochs))

    fig.tight_layout()
    fig.savefig(out_path, dpi=150, bbox_inches="tight", facecolor=BG)
    plt.close(fig)
    print(f"  Saved → {out_path}")


def save_confusion_matrix(y_true, y_pred, out_path: str):
    from sklearn.metrics import confusion_matrix

    cm = confusion_matrix(y_true, y_pred, labels=list(range(NUM_CLASSES)))
    # Normalize row-wise → recall per class
    cm_norm = cm.astype(float) / cm.sum(axis=1, keepdims=True).clip(min=1)

    fig, ax = plt.subplots(figsize=(10, 8))
    fig.suptitle("Confusion Matrix — SmartFit Exercise Classifier\n(row-normalised, shows recall per class)",
                 fontsize=12, fontweight="bold", color=TEXT)

    im = ax.imshow(cm_norm, vmin=0, vmax=1, cmap="RdPu", aspect="auto")

    # Colorbar
    cbar = fig.colorbar(im, ax=ax, fraction=0.046, pad=0.04)
    cbar.ax.yaxis.set_tick_params(color=TEXT)
    cbar.set_label("Recall", color=TEXT)

    # Tick labels
    short = [l.replace(" ", "\n") for l in LABELS]
    ax.set_xticks(range(NUM_CLASSES))
    ax.set_yticks(range(NUM_CLASSES))
    ax.set_xticklabels(short, fontsize=9)
    ax.set_yticklabels(short, fontsize=9)
    ax.set_xlabel("Predicted Label", fontsize=11)
    ax.set_ylabel("True Label",      fontsize=11)

    # Annotate cells — both raw count and norm value
    for i in range(NUM_CLASSES):
        for j in range(NUM_CLASSES):
            raw  = cm[i, j]
            norm = cm_norm[i, j]
            color = "white" if norm < 0.5 else "#111"
            ax.text(j, i, f"{norm:.2f}\n({raw})",
                    ha="center", va="center",
                    fontsize=8, color=color, fontweight="bold" if i == j else "normal")

    fig.tight_layout()
    fig.savefig(out_path, dpi=150, bbox_inches="tight", facecolor=BG)
    plt.close(fig)
    print(f"  Saved → {out_path}")


# ─────────────────────────────────────────────
# 4.  MAIN
# ─────────────────────────────────────────────
def main():
    parser = argparse.ArgumentParser(description="Train SmartFit BiLSTM and plot metrics")
    parser.add_argument("--data",       default="data",  help="Path to dataset root directory")
    parser.add_argument("--epochs",     type=int, default=60)
    parser.add_argument("--batch",      type=int, default=32)
    parser.add_argument("--val_split",  type=float, default=0.2)
    parser.add_argument("--out",        default=".",     help="Directory to save output PNGs")
    args = parser.parse_args()

    os.makedirs(args.out, exist_ok=True)

    # ── Import TF here so errors surface early ──
    import tensorflow as tf
    from sklearn.model_selection import train_test_split
    print(f"\nTensorFlow {tf.__version__}  |  GPUs: {tf.config.list_physical_devices('GPU')}")

    # ── Load data ──
    print(f"\nLoading data from: {args.data}")
    X, y = load_dataset(args.data)
    print(f"  Loaded {len(X)} sequences  |  classes: {NUM_CLASSES}")

    # Unique classes present in data
    present = sorted(set(y.tolist()))
    print(f"  Classes present: {[LABELS[i] for i in present]}")

    # ── Train / val split ──
    X_train, X_val, y_train, y_val = train_test_split(
        X, y, test_size=args.val_split, stratify=y, random_state=42
    )
    print(f"  Train: {len(X_train)}  |  Val: {len(X_val)}")

    # ── Build & train model ──
    model = build_model()
    model.summary()

    callbacks = [
        tf.keras.callbacks.EarlyStopping(
            monitor="val_loss", patience=10, restore_best_weights=True, verbose=1
        ),
        tf.keras.callbacks.ReduceLROnPlateau(
            monitor="val_loss", factor=0.5, patience=5, verbose=1
        ),
    ]

    print(f"\nTraining for up to {args.epochs} epochs (batch={args.batch}) …\n")
    hist = model.fit(
        X_train, y_train,
        validation_data=(X_val, y_val),
        epochs=args.epochs,
        batch_size=args.batch,
        callbacks=callbacks,
        verbose=1,
    )

    # ── Generate metrics plots ──
    print("\nGenerating plots …")

    # Plot 1 — Loss & Accuracy curves
    curves_path = os.path.join(args.out, "loss_accuracy_curves.png")
    save_loss_accuracy_plot(hist.history, curves_path)

    # Plot 2 — Confusion matrix (validation set)
    y_pred = np.argmax(model.predict(X_val, verbose=0), axis=1)
    cm_path = os.path.join(args.out, "confusion_matrix.png")
    save_confusion_matrix(y_val, y_pred, cm_path)

    # ── Final metrics summary ──
    val_loss, val_acc = model.evaluate(X_val, y_val, verbose=0)
    print(f"\n{'─'*45}")
    print(f"  Final val loss    : {val_loss:.4f}")
    print(f"  Final val accuracy: {val_acc*100:.2f}%")
    print(f"  Epochs trained    : {len(hist.history['loss'])}")
    print(f"{'─'*45}")
    print("\nDone! Output files:")
    print(f"  {curves_path}")
    print(f"  {cm_path}")


if __name__ == "__main__":
    main()
