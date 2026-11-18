package com.visibleinfotech.e_ethanolUnloading.utils


import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

@Composable
fun SetStatusBarColor(
    color: Color,
    darkIcons: Boolean = false
) {
    val activity = LocalActivity.current

    SideEffect {
        activity?.window?.statusBarColor = color.toArgb()
    }
}