package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.ProgressRecord
import com.example.data.model.*
import com.example.data.repository.Config
import com.example.ui.components.VlcPlayerView
import com.example.ui.theme.*
import com.example.viewmodel.MainViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.videolan.libvlc.MediaPlayer
import java.util.concurrent.TimeUnit

@Composable
fun PlayerScreen(
    videoUrl: String,
    title: String,
    saveKey: String,
    qualities: List<Quality>?,
    subtitles: List<Subtitle>?,
    viewModel: MainViewModel,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()

    // Lock screen to Landscape
    DisposableEffect(Unit) {
    val originalOrientation = activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
    activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

    // Ocultar barra de notificaciones (modo inmersivo)
    val window = activity?.window
    val decorView = window?.decorView
    decorView?.systemUiVisibility = (
        android.view.View.SYSTEM_UI_FLAG_FULLSCREEN or
        android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
        android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
        android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
        android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
    )

    onDispose {
        activity?.requestedOrientation = originalOrientation
        activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        // Restaurar barra al salir
        decorView?.systemUiVisibility = android.view.View.SYSTEM_UI_FLAG_VISIBLE
    }
}

    // Media states
    var currentUrl by remember { mutableStateOf(videoUrl) }
    var selectedSubtitleUrl by remember { mutableStateOf<String?>(null) }
    var scaleType by remember { mutableStateOf(MediaPlayer.ScaleType.SURFACE_FILL) }

    var mediaPlayerRef by remember { mutableStateOf<MediaPlayer?>(null) }
    var isPlaying by remember { mutableStateOf(true) }
    var duration by remember { mutableStateOf(0L) }
    var position by remember { mutableStateOf(0L) }
    var bufferPercent by remember { mutableStateOf(0f) }

    // Overlays UI States
    var showControls by remember { mutableStateOf(true) }
    var showQualityPopup by remember { mutableStateOf(false) }
    var showSubtitlePopup by remember { mutableStateOf(false) }
    var showAudioPopup by remember { mutableStateOf(false) }

    // Resume prompt dialog state
    var showResumePrompt by remember { mutableStateOf(false) }
    var savedProgressRecord by remember { mutableStateOf<ProgressRecord?>(null) }

    // Gesture status overlays (shows current Brightness/Volume swipe value)
    var gestureOverlayText by remember { mutableStateOf("") }
    var showGestureOverlay by remember { mutableStateOf(false) }

    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }

    // Read previous progress log
    LaunchedEffect(saveKey) {
        val prog = viewModel.progressRecords.value[saveKey]
        if (prog != null && prog.t > 10) {
            savedProgressRecord = prog
            showResumePrompt = true
        }
    }

    // Controls Auto-Hide timer
    LaunchedEffect(showControls, isPlaying) {
        if (showControls && isPlaying) {
            delay(4000)
            showControls = false
        }
    }

    // Periodic Progress Storage logic (runs every 5 seconds)
    LaunchedEffect(position, duration) {
        if (position > 0 && duration > 0) {
            val elapsedSec = (position / 1000).toInt()
            val totalSec = (duration / 1000).toInt()
            if (elapsedSec % 5 == 0) {
                viewModel.saveWatchProgress(saveKey, elapsedSec, totalSec)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { offset ->
                        val midX = size.width / 2
                        if (offset.x < midX) {
                            // Skip reverse 10s
                            mediaPlayerRef?.let { play ->
                                val target = maxOf(0L, play.time - 10000)
                                play.time = target
                                position = target
                                gestureOverlayText = "⏪ -10s"
                                showGestureOverlay = true
                            }
                        } else {
                            // Skip forward 10s
                            mediaPlayerRef?.let { play ->
                                val target = minOf(duration, play.time + 10000)
                                play.time = target
                                position = target
                                gestureOverlayText = "⏩ +10s"
                                showGestureOverlay = true
                            }
                        }
                    },
                    onTap = {
                        showControls = !showControls
                    }
                )
            }
    ) {
        VlcPlayerView(
            videoUrl = currentUrl,
            subtitleUrl = selectedSubtitleUrl,
            scaleType = scaleType,
            onPlayerInitialized = { player ->
                mediaPlayerRef = player
            },
            onVideoEvent = { event ->
                when (event.type) {
                    MediaPlayer.Event.Playing -> {
                        isPlaying = true
                        mediaPlayerRef?.let { play ->
                            duration = play.length
                        }
                    }
                    MediaPlayer.Event.Paused -> {
                        isPlaying = false
                    }
                    MediaPlayer.Event.TimeChanged -> {
                        position = event.timeChanged
                    }
                    MediaPlayer.Event.PositionChanged -> {
                        // PositionChanged: tracks buffers
                    }
                    MediaPlayer.Event.Buffering -> {
                        bufferPercent = event.buffering
                    }
                    MediaPlayer.Event.EndReached -> {
                        isPlaying = false
                        viewModel.saveWatchProgress(saveKey, (duration / 1000).toInt(), (duration / 1000).toInt())
                        onClose()
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        if (bufferPercent > 0f && bufferPercent < 100f) {
            CircularProgressIndicator(
                color = Purple,
                modifier = Modifier
                    .size(52.dp)
                    .align(Alignment.Center)
            )
        }

        AnimatedVisibility(
            visible = showGestureOverlay,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.7f))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = gestureOverlayText,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
            LaunchedEffect(showGestureOverlay) {
                if (showGestureOverlay) {
                    delay(800)
                    showGestureOverlay = false
                }
            }
        }

        // ── CONTROLS OVERLAY AREA ──
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.35f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Black.copy(alpha = 0.85f), Color.Transparent)
                            )
                        )
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        mediaPlayerRef?.let { play ->
                            viewModel.saveWatchProgress(saveKey, (play.time / 1000).toInt(), (duration / 1000).toInt())
                        }
                        onClose()
                    }) {
                        Text(text = "← Volver", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Text(
                        text = title,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Bottom control panel
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f))
                            )
                        )
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    val progressRatio = if (duration > 0) position.toFloat() / duration else 0f
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(14.dp)
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onTap = { offset ->
                                        val faction = offset.x / size.width
                                        mediaPlayerRef?.let { play ->
                                            val dest = (faction * duration).toLong()
                                            play.time = dest
                                            position = dest
                                        }
                                    }
                                )
                            },
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color.White.copy(alpha = 0.2f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(progressRatio)
                                    .background(Brush.horizontalGradient(listOf(Purple, Pink)))
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = {
                            mediaPlayerRef?.let { play ->
                                if (isPlaying) {
                                    play.pause()
                                    isPlaying = false
                                } else {
                                    play.play()
                                    isPlaying = true
                                }
                            }
                        }) {
                            Text(
                                text = if (isPlaying) "⏸" else "▶",
                                color = Color.White,
                                fontSize = 18.sp
                            )
                        }

                        IconButton(onClick = {
                            mediaPlayerRef?.let { play ->
                                val target = maxOf(0L, play.time - 10000)
                                play.time = target
                                position = target
                            }
                        }) {
                            Text(text = "↩ 10s", color = Color.White, fontSize = 12.sp)
                        }

                        IconButton(onClick = {
                            mediaPlayerRef?.let { play ->
                                val target = minOf(duration, play.time + 10000)
                                play.time = target
                                position = target
                            }
                        }) {
                            Text(text = "↪ 10s", color = Color.White, fontSize = 12.sp)
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Text(
                            text = "${formatTime(position)} / ${formatTime(duration)}",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.weight(1f))

                        IconButton(onClick = {
                            showSubtitlePopup = !showSubtitlePopup
                            showQualityPopup = false
                            showAudioPopup = false
                        }) {
                            Text(text = "💬", fontSize = 16.sp)
                        }

                        IconButton(onClick = {
                            showAudioPopup = !showAudioPopup
                            showQualityPopup = false
                            showSubtitlePopup = false
                        }) {
                            Text(text = "🎵", fontSize = 16.sp)
                        }

                        IconButton(onClick = {
                            showQualityPopup = !showQualityPopup
                            showSubtitlePopup = false
                            showAudioPopup = false
                        }) {
                            Text(text = "⚙", fontSize = 16.sp)
                        }

                        IconButton(onClick = {
                            scaleType = if (scaleType == MediaPlayer.ScaleType.SURFACE_FILL) {
                                MediaPlayer.ScaleType.SURFACE_BEST_FIT
                            } else {
                                MediaPlayer.ScaleType.SURFACE_FILL
                            }
                            gestureOverlayText = if (scaleType == MediaPlayer.ScaleType.SURFACE_FILL) "↔ Llena pantalla" else "▣ Proporcional"
                            showGestureOverlay = true
                        }) {
                            Text(text = "↔", fontSize = 18.sp, color = Color.White)
                        }
                    }
                }
            }
        }

        // ── POPUPS OVERLAYS (Qualities / Subs) ──
        if (showQualityPopup && !qualities.isNullOrEmpty()) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 72.dp, end = 16.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF100E26).copy(alpha = 0.95f))
                    .border(1.dp, Color(0xFF8A5CF6).copy(alpha = 0.28f), RoundedCornerShape(12.dp))
                    .padding(vertical = 6.dp)
                    .width(136.dp)
            ) {
                Column {
                    Text(
                        text = "Calidad",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Muted,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                    qualities.forEach { q ->
                        val isSel = currentUrl == q.url
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val savedPos = position
                                    currentUrl = q.url
                                    showQualityPopup = false
                                    scope.launch {
                                        delay(1000)
                                        mediaPlayerRef?.time = savedPos
                                    }
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = q.label, color = if (isSel) PurpleLight else TextMain, fontSize = 13.sp)
                            if (isSel) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(Purple)
                                )
                            }
                        }
                    }
                }
            }
        }

        if (showSubtitlePopup && !subtitles.isNullOrEmpty()) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 72.dp, end = 16.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF100E26).copy(alpha = 0.95f))
                    .border(1.dp, Color(0xFF8A5CF6).copy(alpha = 0.28f), RoundedCornerShape(12.dp))
                    .padding(vertical = 6.dp)
                    .width(136.dp)
            ) {
                Column {
                    Text(
                        text = "Subtítulos",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Muted,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedSubtitleUrl = null
                                showSubtitlePopup = false
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Desactivar", color = if (selectedSubtitleUrl == null) PurpleLight else TextMain, fontSize = 13.sp)
                        if (selectedSubtitleUrl == null) {
                            Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(Purple))
                        }
                    }
                    subtitles.forEach { sub ->
                        val isSel = selectedSubtitleUrl == sub.url
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedSubtitleUrl = sub.url
                                    showSubtitlePopup = false
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = sub.label, color = if (isSel) PurpleLight else TextMain, fontSize = 13.sp)
                            if (isSel) {
                                Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(Purple))
                            }
                        }
                    }
                }
            }
        }

        if (showAudioPopup) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 72.dp, end = 16.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF100E26).copy(alpha = 0.95f))
                    .border(1.dp, Color(0xFF8A5CF6).copy(alpha = 0.28f), RoundedCornerShape(12.dp))
                    .padding(vertical = 6.dp)
                    .width(180.dp)
            ) {
                Column {
                    Text(
                        text = "Pistas de Audio",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Muted,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )

                    val audiosList = mediaPlayerRef?.audioTracks
                    if (audiosList.isNullOrEmpty()) {
                        Text(
                            text = "Única pista del video",
                            fontSize = 12.sp,
                            color = Muted,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    } else {
                        audiosList.forEachIndexed { idx, track ->
                            val currentAudioTrackId = mediaPlayerRef?.audioTrack ?: -1
                            val isSel = track.id == currentAudioTrackId

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        mediaPlayerRef?.audioTrack = track.id
                                        showAudioPopup = false
                                    }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = track.name ?: "Pista ${idx + 1}",
                                    color = if (isSel) PurpleLight else TextMain,
                                    fontSize = 13.sp
                                )
                                if (isSel) {
                                    Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(Purple))
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showResumePrompt && savedProgressRecord != null) {
            val record = savedProgressRecord!!
            val durationMin = itemDurationLabel(record.t)

            AlertDialog(
                onDismissRequest = { showResumePrompt = false },
                containerColor = CardColor,
                title = {
                    Text(
                        text = "¿Continuar viendo?",
                        fontSize = 16.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        text = "Se detectó un progreso guardado en el minuto $durationMin. ¿Deseas saltar allí?",
                        fontSize = 13.sp,
                        color = Color(0xFFC0B8E8)
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            mediaPlayerRef?.let { play ->
                                val target = record.t * 1000L
                                play.time = target
                                position = target
                            }
                            showResumePrompt = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Purple)
                    ) {
                        Text("Continuar", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showResumePrompt = false }) {
                        Text("Desde el inicio", color = Muted)
                    }
                }
            )
        }
    }
}

private fun formatTime(millis: Long): String {
    if (millis <= 0L) return "0:00"
    val hours = TimeUnit.MILLISECONDS.toHours(millis)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60
    val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%d:%02d", minutes, seconds)
    }
}

private fun itemDurationLabel(secs: Int): String {
    val m = secs / 60
    val s = secs % 60
    return String.format("%d:%02d", m, s)
}
