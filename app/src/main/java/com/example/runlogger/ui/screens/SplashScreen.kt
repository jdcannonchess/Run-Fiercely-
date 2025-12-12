package com.example.runlogger.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.runlogger.R
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onSplashComplete: () -> Unit
) {
    // Animation state
    var startAnimation by remember { mutableStateOf(false) }
    var fadeOut by remember { mutableStateOf(false) }
    
    // Fade in animation
    val alpha by animateFloatAsState(
        targetValue = when {
            fadeOut -> 0f
            startAnimation -> 1f
            else -> 0f
        },
        animationSpec = tween(durationMillis = 500),
        label = "splash_alpha"
    )
    
    // Control the animation sequence
    LaunchedEffect(Unit) {
        startAnimation = true      // Start fade in
        delay(500)                 // Wait for fade in
        delay(1500)                // Show image for 1.5 seconds
        fadeOut = true             // Start fade out
        delay(500)                 // Wait for fade out
        onSplashComplete()         // Navigate to main app
    }
    
    // Splash screen UI
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0EAFA2))
    ) {
        // Centered runner image
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.splash_runner),
                contentDescription = "Run Logger",
                modifier = Modifier
                    .size(200.dp)
                    .alpha(alpha)
            )
        }
        
        // Tagline at bottom
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 80.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Text(
                text = "run fiercely",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                fontStyle = FontStyle.Italic,
                letterSpacing = 4.sp,
                color = Color.White,
                modifier = Modifier.alpha(alpha)
            )
        }
    }
}
