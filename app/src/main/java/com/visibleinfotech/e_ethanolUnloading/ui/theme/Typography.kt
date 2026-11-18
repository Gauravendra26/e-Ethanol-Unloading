package com.visibleinfotech.e_ethanolUnloading.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.visibleinfotech.e_ethanolUnloading.R


val PoppinsFontFamily = FontFamily(
    Font(R.font.poppins_thin, FontWeight.Thin),
    Font(R.font.poppins_extralight, FontWeight.ExtraLight),
    Font(R.font.poppins_light, FontWeight.Light),
    Font(R.font.poppins_regular, FontWeight.Normal),
    Font(R.font.poppins_medium, FontWeight.Medium),
    Font(R.font.poppins_semibold, FontWeight.SemiBold),
    Font(R.font.poppins_bold, FontWeight.Bold),
    Font(R.font.poppins_black, FontWeight.Black)
)


object AppTypography {
    // Headers
    val headlineLarge = TextStyle(
        fontFamily = PoppinsFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        letterSpacing = 0.sp,
        lineHeight = 40.sp
    )

    val headlineMedium = TextStyle(
        fontFamily = PoppinsFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        letterSpacing = 0.sp,
        lineHeight = 36.sp
    )

    val headlineSmall = TextStyle(
        fontFamily = PoppinsFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 22.sp,
        letterSpacing = 0.sp,
        lineHeight = 32.sp
    )
  val headlineVerySmall = TextStyle(
        fontFamily = PoppinsFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 20.sp,
        letterSpacing = 0.sp,
        lineHeight = 32.sp
    )

    // Titles
    val titleLarge = TextStyle(
        fontFamily = PoppinsFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        letterSpacing = 0.sp,
        lineHeight = 28.sp
    )

    val titleMedium = TextStyle(
        fontFamily = PoppinsFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 18.sp,
        letterSpacing = 0.sp,
        lineHeight = 24.sp
    )

    val titleSmall = TextStyle(
        fontFamily = PoppinsFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        letterSpacing = 0.sp,
        lineHeight = 22.sp
    )

    // Body text
    val bodyLarge = TextStyle(
        fontFamily = PoppinsFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        letterSpacing = 0.5.sp,
        lineHeight = 24.sp
    )

    val bodyMedium = TextStyle(
        fontFamily = PoppinsFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        letterSpacing = 0.25.sp,
        lineHeight = 20.sp
    )

    val bodySmall = TextStyle(
        fontFamily = PoppinsFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        letterSpacing = 0.sp,
        lineHeight = 16.sp
    )

    // Labels
    val labelLarge = TextStyle(
        fontFamily = PoppinsFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        letterSpacing = 0.sp,
        lineHeight = 20.sp
    )

    val labelMedium = TextStyle(
        fontFamily = PoppinsFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        letterSpacing = 0.sp,
        lineHeight = 18.sp
    )

    val labelSmall = TextStyle(
        fontFamily = PoppinsFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        letterSpacing = 0.sp,
        lineHeight = 16.sp
    )

    // Button text
    val button = TextStyle(
        fontFamily = PoppinsFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        letterSpacing = 0.5.sp,
        lineHeight = 20.sp
    )

    // Form labels
    val formLabel = TextStyle(
        fontFamily = PoppinsFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        letterSpacing = 0.sp,
        lineHeight = 20.sp,
        color = TextDark
    )

    // Form values
    val formValue = TextStyle(
        fontFamily = PoppinsFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        letterSpacing = 0.sp,
        lineHeight = 20.sp,
        color = TextDark
    )

    // Hint/Placeholder text
    val hint = TextStyle(
        fontFamily = PoppinsFontFamily,
        fontWeight = FontWeight.Light,
        fontSize = 14.sp,
        letterSpacing = 0.sp,
        lineHeight = 20.sp,
        color = TextDark.copy(alpha = 0.5f)
    )

    // Error text
    val error = TextStyle(
        fontFamily = PoppinsFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        letterSpacing = 0.sp,
        lineHeight = 16.sp,
        color = ErrorRed
    )

    // Caption text
    val caption = TextStyle(
        fontFamily = PoppinsFontFamily,
        fontWeight = FontWeight.Light,
        fontSize = 12.sp,
        letterSpacing = 0.25.sp,
        lineHeight = 16.sp
    )

    val dateTime = TextStyle(
        fontFamily = PoppinsFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        letterSpacing = 0.sp,
        lineHeight = 24.sp
    )
}

val AppMaterialTypography = Typography(
    displayLarge = AppTypography.headlineLarge,
    displayMedium = AppTypography.headlineMedium,
    displaySmall = AppTypography.headlineSmall,
    headlineLarge = AppTypography.headlineLarge,
    headlineMedium = AppTypography.headlineMedium,
    headlineSmall = AppTypography.headlineSmall,
    titleLarge = AppTypography.titleLarge,
    titleMedium = AppTypography.titleMedium,
    titleSmall = AppTypography.titleSmall,
    bodyLarge = AppTypography.bodyLarge,
    bodyMedium = AppTypography.bodyMedium,
    bodySmall = AppTypography.bodySmall,
    labelLarge = AppTypography.labelLarge,
    labelMedium = AppTypography.labelMedium,
    labelSmall = AppTypography.labelSmall
)