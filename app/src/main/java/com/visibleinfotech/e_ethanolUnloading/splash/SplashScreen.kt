package com.visibleinfotech.e_ethanolUnloading.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import com.visibleinfotech.e_ethanolUnloading.MainConstant
import com.visibleinfotech.e_ethanolUnloading.R
@Composable
fun SplashScreen(
    onNavigateToLogin: () -> Unit,
    onNavigateToDashboard: () -> Unit
) {
    val context = LocalContext.current
    val mainConstant = remember { MainConstant(context) }

    val scale = remember {
        Animatable(0f)
    }

    LaunchedEffect(Unit) {
        scale.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = 1000
            )
        )

        delay(2000)

        // Check if user is already logged in
        if (mainConstant.isLoggedIn()) {
            onNavigateToDashboard()
        } else {
            onNavigateToLogin()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.mipmap.ic_logo),
            contentDescription = "Logo",
            modifier = Modifier
                .size(200.dp)
                .scale(scale.value)
        )
    }


}