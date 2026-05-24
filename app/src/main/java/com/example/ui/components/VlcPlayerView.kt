package com.example.ui.components

import android.net.Uri
import android.view.SurfaceView
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer
import org.videolan.libvlc.interfaces.IMedia

@Composable
fun VlcPlayerView(
    videoUrl: String,
    subtitleUrl: String?,
    playbackSpeed: Float = 1.0f,
    modifier: Modifier = Modifier,
    onVideoEvent: (MediaPlayer.Event) -> Unit = {},
    onPlayerInitialized: (MediaPlayer) -> Unit = {},
    scaleType: MediaPlayer.ScaleType = MediaPlayer.ScaleType.SURFACE_FILL
) {
    val context = androidx.compose.ui.platform.LocalContext.current

    var libVlc by remember { mutableStateOf<LibVLC?>(null) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    DisposableEffect(videoUrl, subtitleUrl) {
        val args = ArrayList<String>().apply {
            add("-vvv")
            add("--http-reconnect")
            add("--no-sub-autodetect")
        }
        val vlc = LibVLC(context, args)
        val player = MediaPlayer(vlc)

        libVlc = vlc
        mediaPlayer = player

        onPlayerInitialized(player)

        player.setEventListener { event ->
            onVideoEvent(event)
        }

        player.rate = playbackSpeed

        try {
            val media = Media(vlc, Uri.parse(videoUrl))
            media.setHWDecoderEnabled(true, false)

            if (!subtitleUrl.isNullOrEmpty()) {
                val slave = IMedia.Slave(
                    IMedia.Slave.Type.Subtitle,
                    4,
                    subtitleUrl
                )
                media.addSlave(slave)
            }

            player.media = media
            media.release()
            player.play()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        onDispose {
            player.stop()
            player.vlcVout.detachViews()
            player.release()
            vlc.release()
        }
    }

    AndroidView(
        factory = { ctx ->
            FrameLayout(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                val surfaceView = SurfaceView(ctx).apply {
                    layoutParams = FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                    )
                }
                addView(surfaceView)

                post {
                    mediaPlayer?.let { player ->
                        player.videoScale = MediaPlayer.ScaleType.SURFACE_FILL
                        player.vlcVout.setVideoView(surfaceView)
                        player.vlcVout.attachViews()
                        val width = surfaceView.width.takeIf { it > 0 } ?: 1920
                        val height = surfaceView.height.takeIf { it > 0 } ?: 1080
                        player.vlcVout.setWindowSize(width, height)
                        post {
                            player.videoScale = MediaPlayer.ScaleType.SURFACE_FILL
                        }
                    }
                }
            }
        },
        update = { frameLayout ->
            mediaPlayer?.let { player ->
                player.videoScale = MediaPlayer.ScaleType.SURFACE_FILL
                val sv = frameLayout.getChildAt(0) as? SurfaceView
                sv?.let {
                    player.vlcVout.setWindowSize(it.width, it.height)
                }
            }
        },
        modifier = modifier
    )
}
