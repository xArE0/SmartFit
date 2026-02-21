package com.example.project_smartfit.camera

import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.view.Surface
import android.view.TextureView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.project_smartfit.data.ExerciseDatabase
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

/**
 * Reusable composable that plays the exercise animation video in a loop.
 * Uses TextureView + MediaPlayer with AssetFileDescriptor for reliable playback.
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

    // Track whether the MediaPlayer is prepared and whether an error occurred
    var isPrepared by remember { mutableStateOf(false) }
    var hasError by remember { mutableStateOf(false) }

    // Create and remember a single MediaPlayer instance
    val mediaPlayer = remember { MediaPlayer() }

    // Clean up MediaPlayer on dispose
    DisposableEffect(Unit) {
        onDispose {
            try {
                if (mediaPlayer.isPlaying) mediaPlayer.stop()
                mediaPlayer.release()
            } catch (_: Exception) {}
        }
    }

    // Pause/resume with lifecycle — only when prepared
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            try {
                when (event) {
                    Lifecycle.Event.ON_PAUSE -> {
                        if (isPrepared && mediaPlayer.isPlaying) mediaPlayer.pause()
                    }
                    Lifecycle.Event.ON_RESUME -> {
                        if (isPrepared) mediaPlayer.start()
                    }
                    else -> {}
                }
            } catch (_: Exception) {}
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    if (hasError) {
        // Show error fallback UI
        Box(
            modifier = modifier.background(Color(0xFF1A1A1A), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.VideocamOff,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text("Video unavailable", color = Color.Gray, fontSize = 12.sp)
            }
        }
    } else {
        Box(
            modifier = modifier.background(Color.Black, RoundedCornerShape(12.dp))
        ) {
            AndroidView(
                factory = { ctx ->
                    TextureView(ctx).apply {
                        surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                            override fun onSurfaceTextureAvailable(
                                surfaceTexture: SurfaceTexture,
                                width: Int,
                                height: Int
                            ) {
                                try {
                                    val afd = ctx.assets.openFd("animation/$fileName")
                                    mediaPlayer.reset()
                                    mediaPlayer.setDataSource(
                                        afd.fileDescriptor,
                                        afd.startOffset,
                                        afd.length
                                    )
                                    afd.close()
                                    mediaPlayer.setSurface(Surface(surfaceTexture))
                                    mediaPlayer.setVolume(0f, 0f)
                                    mediaPlayer.isLooping = true
                                    mediaPlayer.setOnPreparedListener { mp ->
                                        isPrepared = true
                                        mp.start()
                                    }
                                    mediaPlayer.setOnErrorListener { _, what, extra ->
                                        Log.e("AnimPlayer", "Error: what=$what extra=$extra for $fileName")
                                        hasError = true
                                        true
                                    }
                                    mediaPlayer.prepareAsync()
                                } catch (e: Exception) {
                                    Log.e("AnimPlayer", "Failed to play animation/$fileName", e)
                                    hasError = true
                                }
                            }

                            override fun onSurfaceTextureSizeChanged(
                                surface: SurfaceTexture, width: Int, height: Int
                            ) {}

                            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                                return true
                            }

                            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
                        }
                    }
                },
                modifier = Modifier.matchParentSize()
            )
        }
    }
}
