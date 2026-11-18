package com.visibleinfotech.e_ethanolUnloading.ui.theme

import android.os.Build
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(

    primary = PrimaryBlue,
    secondary = LightBlue,
    tertiary = PrimaryBlue,

    background = BackgroundColor,
    surface = WhiteColor,

    onPrimary = TextLight,
    onSecondary = TextLight,
    onTertiary = TextLight,

    onBackground = TextDark,
    onSurface = TextDark
)

private val DarkColorScheme = darkColorScheme(

    primary = PrimaryBlue,
    secondary = LightBlue,
    tertiary = PrimaryBlue
)

@Composable
fun EEthanolTheme(
    darkTheme: Boolean = false,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {

    val colorScheme = when {

        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {

            val context = LocalContext.current

            if (darkTheme) {
                dynamicDarkColorScheme(context)
            } else {
                dynamicLightColorScheme(context)
            }
        }

        darkTheme -> DarkColorScheme

        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}