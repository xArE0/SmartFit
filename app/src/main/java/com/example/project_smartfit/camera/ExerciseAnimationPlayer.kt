package com.example.project_smartfit.camera

import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.view.Surface
import android.view.TextureView
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

    // Create and remember a single MediaPlayer instance
    val mediaPlayer = remember { MediaPlayer() }

    // Clean up MediaPlayer on dispose
    DisposableEffect(Unit) {
        onDispose {
            try {
                mediaPlayer.stop()
                mediaPlayer.release()
            } catch (_: Exception) {}
        }
    }

    // Pause/resume with lifecycle
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            try {
                when (event) {
                    Lifecycle.Event.ON_PAUSE -> {
                        if (mediaPlayer.isPlaying) mediaPlayer.pause()
                    }
                    Lifecycle.Event.ON_RESUME -> {
                        mediaPlayer.start()
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
                                mediaPlayer.setSurface(Surface(surfaceTexture))
                                mediaPlayer.setVolume(0f, 0f)
                                mediaPlayer.isLooping = true
                                mediaPlayer.prepareAsync()
                                mediaPlayer.setOnPreparedListener { mp ->
                                    mp.start()
                                }
                                mediaPlayer.setOnErrorListener { _, what, extra ->
                                    Log.e("AnimPlayer", "Error: what=$what extra=$extra")
                                    true
                                }
                                afd.close()
                            } catch (e: Exception) {
                                Log.e("AnimPlayer", "Failed to play animation/$fileName", e)
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
