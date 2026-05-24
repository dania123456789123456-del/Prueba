package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onTimeout: () -> Unit) {
    // Floating transition setup (2 seconds duration loop: 1000ms up, 1000ms down)
    val infiniteTransition = rememberInfiniteTransition(label = "SplashFloat")
    val translationY by infiniteTransition.animateFloat(
        initialValue = -8f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "SplashFloatY"
    )

    // Running progress animation mock representing the 2px bottom line
    val progressTransition = rememberInfiniteTransition(label = "SplashProgress")
    val progressShift by progressTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = EaseInOutQuad),
            repeatMode = RepeatMode.Restart
        ),
        label = "SplashProgressX"
    )

    LaunchedEffect(Unit) {
        delay(1100)
        onTimeout()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Ink),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Floating 70dp box with 20dp border-radius, gradient from Purple to Pink with centered emoji 🎬
            Box(
                modifier = Modifier
                    .size(70.dp)
                    .graphicsLayer { this.translationY = translationY }
                    .clip(RoundedCornerShape(20.dp))
                    .background(Brush.linearGradient(listOf(Purple, Pink))),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🎬",
                    fontSize = 32.sp
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // App Name: CineStream with gradient from white to PurpleLight
            Text(
                text = "CineStream",
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 180dp x 2dp Infinite Progress Bar with background opacity and Purple->Pink fill
            Box(
                modifier = Modifier
                    .width(180.dp)
                    .height(2.dp)
                    .clip(RoundedCornerShape(1.dp))
                    .background(Color.White.copy(alpha = 0.08f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(0.35f)
                        .offset(x = (180.dp - (180.dp * 0.35f)) * progressShift)
                        .background(Brush.horizontalGradient(listOf(Purple, Pink)))
                )
            }
        }
    }
}
