package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    viewModel: MainViewModel,
    onLoginSuccess: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp

    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Pulse animation for login logo
    val infiniteTransition = rememberInfiniteTransition(label = "LogoPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "LogoPulseScale"
    )

    // Twinkling stars background drawing helper
    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawBehind {
                // Background radial gradient from deep violet #2E1065 to black Ink
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF2E1065), Ink),
                        center = Offset(size.width * 0.3f, size.height * 0.5f),
                        radius = size.maxDimension * 0.85f
                    )
                )

                // Render tiny white star seeds
                val starPositions = listOf(
                    Offset(0.1f, 0.15f), Offset(0.25f, 0.08f), Offset(0.4f, 0.22f),
                    Offset(0.7f, 0.12f), Offset(0.85f, 0.28f), Offset(0.08f, 0.45f),
                    Offset(0.92f, 0.48f), Offset(0.18f, 0.72f), Offset(0.35f, 0.85f),
                    Offset(0.6f, 0.65f), Offset(0.78f, 0.82f), Offset(0.5f, 0.9f)
                )
                starPositions.forEach { prop ->
                    drawCircle(
                        color = Color.White.copy(alpha = 0.35f),
                        radius = 2.dp.toPx(),
                        center = Offset(prop.x * size.width, prop.y * size.height)
                    )
                }
            },
        contentAlignment = Alignment.Center
    ) {
        // Film strip decorative item at standard bottom screen
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(70.dp)
                .background(Color.Transparent)
                .drawBehind {
                    // Draw continuous film perforations
                    val heightPx = size.height
                    val widthPx = size.width
                    val strokeW = 1.5.dp.toPx()
                    drawLine(
                        color = Purple.copy(alpha = 0.15f),
                        start = Offset(0f, 0f),
                        end = Offset(widthPx, 0f),
                        strokeWidth = strokeW
                    )

                    var startX = 20.dp.toPx()
                    val holeWidth = 50.dp.toPx()
                    val holeHeight = 36.dp.toPx()
                    val gap = 14.dp.toPx()

                    while (startX < widthPx) {
                        drawRoundRect(
                            color = Purple.copy(alpha = 0.12f),
                            topLeft = Offset(startX, (heightPx - holeHeight) / 2),
                            size = androidx.compose.ui.geometry.Size(holeWidth, holeHeight),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(5.dp.toPx()),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5.dp.toPx())
                        )
                        startX += holeWidth + gap
                    }
                }
        )

        // Login Card box
        val isMobileCompact = screenWidth < 500.dp
        Box(
            modifier = Modifier
                .padding(16.dp)
                .widthIn(max = 820.dp)
                .fillMaxWidth(0.94f)
                .clip(RoundedCornerShape(24.dp))
                .background(Ink2.copy(alpha = 0.94f))
                .border(1.dp, Color(0xFF8A5CF6).copy(alpha = 0.18f), RoundedCornerShape(24.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (isMobileCompact) {
                // Stack vertically
                Column(modifier = Modifier.fillMaxWidth()) {
                    LeftBrandingPanel(pulseScale, modifier = Modifier.fillMaxWidth())
                    RightFormPanel(
                        username = username,
                        password = password,
                        errorMessage = errorMessage,
                        onUsernameChange = { username = it },
                        onPasswordChange = { password = it },
                        onLoginClick = {
                            viewModel.doLogin(
                                user = username,
                                pass = password,
                                onSuccess = { onLoginSuccess() },
                                onError = { errorMessage = it }
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            } else {
                // Side by side row
                Row(
                    modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LeftBrandingPanel(pulseScale, modifier = Modifier.width(280.dp).fillMaxHeight())
                    RightFormPanel(
                        username = username,
                        password = password,
                        errorMessage = errorMessage,
                        onUsernameChange = { username = it },
                        onPasswordChange = { password = it },
                        onLoginClick = {
                            viewModel.doLogin(
                                user = username,
                                pass = password,
                                onSuccess = { onLoginSuccess() },
                                onError = { errorMessage = it }
                            )
                        },
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                }
            }
        }
    }
}

@Composable
private fun LeftBrandingPanel(logoScale: Float, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Purple.copy(alpha = 0.22f),
                        Pink.copy(alpha = 0.08f),
                        Panel.copy(alpha = 0.95f)
                    )
                )
            )
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Clustered Logo with pulse
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .graphicsLayer {
                        scaleX = logoScale
                        scaleY = logoScale
                    }
                    .clip(RoundedCornerShape(18.dp))
                    .background(Brush.linearGradient(listOf(Purple, Pink))),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "🎬", fontSize = 28.sp)
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "CineStream",
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Tu plataforma de\nstreaming personal",
                fontSize = 12.sp,
                color = Muted,
                lineHeight = 16.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Three decorative bullet dots showing color accents
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(modifier = Modifier.size(6.dp).clip(RoundedCornerShape(3.dp)).background(Purple))
                Box(modifier = Modifier.size(6.dp).clip(RoundedCornerShape(3.dp)).background(Pink.copy(alpha = 0.6f)))
                Box(modifier = Modifier.size(6.dp).clip(RoundedCornerShape(3.dp)).background(Muted.copy(alpha = 0.4f)))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RightFormPanel(
    username: String,
    password: String,
    errorMessage: String?,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onLoginClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Iniciar Sesión",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Custom unfilled text fields that exactly replicate HTML inputs
        Text(
            text = "USUARIO",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Muted,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        OutlinedTextField(
            value = username,
            onValueChange = onUsernameChange,
            placeholder = { Text("Escribe tu usuario", color = Color.White.copy(alpha = 0.2f), fontSize = 14.sp) },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Purple,
                unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                focusedContainerColor = Color.White.copy(alpha = 0.05f),
                unfocusedContainerColor = Color.White.copy(alpha = 0.05f),
                focusedTextColor = TextMain,
                unfocusedTextColor = TextMain
            ),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp)
        )

        Text(
            text = "CONTRASEÑA",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Muted,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        OutlinedTextField(
            value = password,
            onValueChange = onPasswordChange,
            placeholder = { Text("Escribe tu contraseña", color = Color.White.copy(alpha = 0.2f), fontSize = 14.sp) },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Purple,
                unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                focusedContainerColor = Color.White.copy(alpha = 0.05f),
                unfocusedContainerColor = Color.White.copy(alpha = 0.05f),
                focusedTextColor = TextMain,
                unfocusedTextColor = TextMain
            ),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp)
        )

        Button(
            onClick = onLoginClick,
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
            contentPadding = PaddingValues(),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .background(
                    Brush.linearGradient(listOf(Purple, PurpleDark)),
                    shape = RoundedCornerShape(12.dp)
                )
        ) {
            Text(
                text = "▶ Entrar",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        if (!errorMessage.isNullOrEmpty()) {
            Text(
                text = "⚠ $errorMessage",
                color = Color(0xFFF87171),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = 10.dp).align(Alignment.CenterHorizontally)
            )
        }
    }
}
