package com.example.project_smartfit.camera

import android.widget.VideoView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.project_smartfit.data.ExerciseDatabase
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

/**
 * Reusable composable that plays the exercise animation video in a loop.
 *
 * @param exerciseId  e.g. "push-up", "squat", "plank"
 * @param modifier    sizing / positioning
 */
@Composable
fun ExerciseAnimationPlayer(
    exerciseId: String,
    modifier: Modifier = Modifier
        .size(120.dp)
        .clip(RoundedCornerShape(12.dp))
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val fileName = ExerciseDatabase.animationAsset(exerciseId)

    // Build asset URI
    val assetUri = remember(fileName) {
        "file:///android_asset/animation/$fileName".toUri()
    }

    // Keep a reference to the VideoView for lifecycle management
    val videoViewRef = remember { arrayOfNulls<VideoView>(1) }

    // Lifecycle observer: pause on background, resume on foreground
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            val vv = videoViewRef[0] ?: return@LifecycleEventObserver
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    if (vv.isPlaying) vv.pause()
                }
                Lifecycle.Event.ON_RESUME -> {
                    vv.start()
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            videoViewRef[0]?.stopPlayback()
        }
    }

    Box(
        modifier = modifier.background(Color.Black, RoundedCornerShape(12.dp))
    ) {
        AndroidView(
            factory = { ctx ->
                VideoView(ctx).apply {
                    setVideoURI(assetUri)
                    // Primary loop mechanism
                    setOnPreparedListener { mp ->
                        mp.isLooping = true
                        mp.setVolume(0f, 0f)
                        start()
                    }
                    // Fallback: if isLooping doesn't work on some devices
                    setOnCompletionListener {
                        seekTo(0)
                        start()
                    }
                    setOnErrorListener { _, _, _ -> true }
                    videoViewRef[0] = this
                }
            },
            update = { videoView ->
                if (videoView.tag != fileName) {
                    videoView.tag = fileName
                    val newUri = "file:///android_asset/animation/$fileName".toUri()
                    videoView.setVideoURI(newUri)
                    videoView.start()
                }
            },
            modifier = Modifier.matchParentSize()
        )
    }
}
