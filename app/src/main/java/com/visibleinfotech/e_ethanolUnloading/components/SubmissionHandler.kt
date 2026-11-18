package com.visibleinfotech.e_ethanolUnloading.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.visibleinfotech.e_ethanolUnloading.ui.theme.BrandViolet
import com.visibleinfotech.e_ethanolUnloading.ui.theme.ErrorRed
import com.visibleinfotech.e_ethanolUnloading.ui.theme.PrimaryGreen
import com.visibleinfotech.e_ethanolUnloading.ui.theme.SoftSurface

data class SubmissionState(
    val isLoading: Boolean = false,
    val progress: Int = 0,
    val status: String = "",
    val isError: Boolean = false,
    val errorMessage: String = "",
    val isSuccess: Boolean = false,
    val successMessage: String = ""
)

@Composable
fun SubmissionProgressDialog(
    progress: Int,
    status: String,
    isError: Boolean = false,
    errorMessage: String = "",
    onDismissError: () -> Unit = {}
) {
    Dialog(
        onDismissRequest = { if (isError) onDismissError() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp)
                .wrapContentHeight(),
            shape = RoundedCornerShape(28.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
            colors = CardDefaults.cardColors(containerColor = SoftSurface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                if (!isError) {
                    val animatedProgress by animateFloatAsState(
                        targetValue = progress / 100f,
                        label = "ProgressAnimation"
                    )

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(100.dp)
                    ) {
                        CircularProgressIndicator(
                            progress = { 1f },
                            modifier = Modifier.fillMaxSize(),
                            color = BrandViolet.copy(alpha = 0.1f),
                            strokeWidth = 8.dp,
                            strokeCap = StrokeCap.Round
                        )

                        CircularProgressIndicator(
                            progress = { animatedProgress },
                            modifier = Modifier.fillMaxSize(),
                            color = BrandViolet,
                            strokeWidth = 8.dp,
                            strokeCap = StrokeCap.Round
                        )

                        Text(
                            text = "$progress%",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = BrandViolet
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = status,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF2D3436),
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = "Please wait while we process your request...",
                            fontSize = 14.sp,
                            color = Color(0xFF636E72),
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .background(ErrorRed.copy(alpha = 0.1f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ErrorOutline,
                            contentDescription = "Error Icon",
                            tint = ErrorRed,
                            modifier = Modifier.size(40.dp)
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Submission Failed",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2D3436),
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = errorMessage.ifEmpty { "An unexpected error occurred. Please try again." },
                            fontSize = 14.sp,
                            color = Color(0xFF636E72),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }

                    Button(
                        onClick = onDismissError,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandViolet)
                    ) {
                        Text("Try Again", color = Color.White, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
fun SubmissionResultDialog(
    isSuccess: Boolean,
    message: String,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp)
                .wrapContentHeight(),
            shape = RoundedCornerShape(28.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .background(
                            if (isSuccess) PrimaryGreen.copy(alpha = 0.1f) else ErrorRed.copy(alpha = 0.1f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isSuccess) Icons.Default.Check else Icons.Rounded.ErrorOutline,
                        contentDescription = if (isSuccess) "Success Icon" else "Error Icon",
                        tint = if (isSuccess) PrimaryGreen else ErrorRed,
                        modifier = Modifier.size(40.dp)
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = if (isSuccess) "Success!" else "Failed!",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSuccess) PrimaryGreen else ErrorRed,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = message,
                        fontSize = 14.sp,
                        color = Color(0xFF636E72),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }

                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSuccess) PrimaryGreen else BrandViolet
                    )
                ) {
                    Text("OK", color = Color.White, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

// This version does NOT require a content lambda - just shows dialogs
@Composable
fun SubmissionHandler(
    submissionState: SubmissionState,
    onDismissError: () -> Unit = {},
    onDismissSuccess: () -> Unit = {}
) {
    // Progress Dialog
    if (submissionState.isLoading) {
        SubmissionProgressDialog(
            progress = submissionState.progress,
            status = submissionState.status,
            isError = false
        )
    }

    // Error Dialog
    if (submissionState.isError) {
        SubmissionResultDialog(
            isSuccess = false,
            message = submissionState.errorMessage,
            onDismiss = onDismissError
        )
    }

    // Success Dialog
    if (submissionState.isSuccess) {
        SubmissionResultDialog(
            isSuccess = true,
            message = submissionState.successMessage,
            onDismiss = onDismissSuccess
        )
    }
}