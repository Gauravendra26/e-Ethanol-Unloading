package com.visibleinfotech.e_ethanolUnloading.ui.theme


import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.visibleinfotech.e_ethanolUnloading.R

// ============================================
// FONT FAMILIES
// ============================================

// 1. POPPINS - Modern Sans Serif (Primary)
val Poppins = FontFamily(
    Font(R.font.poppins_regular, weight = FontWeight.Normal),
    Font(R.font.poppins_medium, weight = FontWeight.Medium),
    Font(R.font.poppins_semibold, weight = FontWeight.SemiBold),
    Font(R.font.poppins_bold, weight = FontWeight.Bold)
)

// 2. SERIF - Classic/Traditional (For Saudia/Contracts)
val Serif = FontFamily(
    Font(R.font.serif_regular, weight = FontWeight.Normal),
    Font(R.font.serif_bold, weight = FontWeight.Bold)
)

// 3. ARIMO - Clean, Neutral Sans Serif (Best for Data/Numbers)
val Arimo = FontFamily(
    Font(R.font.arimo_variable, weight = FontWeight.Normal),
    Font(R.font.arimo_variable, weight = FontWeight.Medium),
    Font(R.font.arimo_variable, weight = FontWeight.Bold)
)

// 4. GOOGLE SANS - Modern, Professional (Best for Headers)
val GoogleSans = FontFamily(
    Font(R.font.google_sans_variable, weight = FontWeight.Normal),
    Font(R.font.google_sans_variable, weight = FontWeight.Medium),
    Font(R.font.google_sans_variable, weight = FontWeight.Bold)
)

// 5. LORA - Elegant Serif (Best for Hindi/Local Text)
val Lora = FontFamily(
    Font(R.font.lora_variable, weight = FontWeight.Normal),
    Font(R.font.lora_variable, weight = FontWeight.Medium),
    Font(R.font.lora_variable, weight = FontWeight.Bold)
)

// ============================================
// TYPOGRAPHY (Primary)
// ============================================

// Modern Typography with Poppins
val Typography = Typography(
    headlineLarge = TextStyle(
        fontFamily = Poppins,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 40.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = Poppins,
        fontWeight = FontWeight.SemiBold,
        fontSize = 26.sp,
        lineHeight = 34.sp
    ),
    titleLarge = TextStyle(
        fontFamily = Poppins,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 30.sp
    ),
    titleMedium = TextStyle(
        fontFamily = Poppins,
        fontWeight = FontWeight.Medium,
        fontSize = 18.sp,
        lineHeight = 26.sp
    ),
    labelLarge = TextStyle(
        fontFamily = Poppins,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = Poppins,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = Poppins,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    )
)

// ============================================
// EXTENDED TYPOGRAPHY VARIANTS
// ============================================

// 1. Serif Typography (For Sauda/Contract Screens)
val SerifTypography = Typography(
    headlineLarge = Typography.headlineLarge.copy(fontFamily = Serif),
    headlineMedium = Typography.headlineMedium.copy(fontFamily = Serif),
    titleLarge = Typography.titleLarge.copy(fontFamily = Serif),
    titleMedium = Typography.titleMedium.copy(fontFamily = Serif),
    labelLarge = Typography.labelLarge.copy(fontFamily = Serif),
    bodyLarge = Typography.bodyLarge.copy(fontFamily = Serif),
    bodyMedium = Typography.bodyMedium.copy(fontFamily = Serif)
)

// 2. Arimo Typography (For Tables, Lists, Data Display)
val ArimoTypography = Typography(
    headlineLarge = Typography.headlineLarge.copy(fontFamily = Arimo),
    headlineMedium = Typography.headlineMedium.copy(fontFamily = Arimo),
    titleLarge = Typography.titleLarge.copy(fontFamily = Arimo),
    titleMedium = Typography.titleMedium.copy(fontFamily = Arimo),
    labelLarge = Typography.labelLarge.copy(fontFamily = Arimo),
    bodyLarge = Typography.bodyLarge.copy(fontFamily = Arimo),
    bodyMedium = Typography.bodyMedium.copy(fontFamily = Arimo)
)

// 3. Google Sans Typography (For Headers, Navigation)
val GoogleSansTypography = Typography(
    headlineLarge = Typography.headlineLarge.copy(fontFamily = GoogleSans),
    headlineMedium = Typography.headlineMedium.copy(fontFamily = GoogleSans),
    titleLarge = Typography.titleLarge.copy(fontFamily = GoogleSans),
    titleMedium = Typography.titleMedium.copy(fontFamily = GoogleSans),
    labelLarge = Typography.labelLarge.copy(fontFamily = GoogleSans),
    bodyLarge = Typography.bodyLarge.copy(fontFamily = GoogleSans),
    bodyMedium = Typography.bodyMedium.copy(fontFamily = GoogleSans)
)

// 4. Lora Typography (For Hindi/Local Language Content)
val LoraTypography = Typography(
    headlineLarge = Typography.headlineLarge.copy(fontFamily = Lora),
    headlineMedium = Typography.headlineMedium.copy(fontFamily = Lora),
    titleLarge = Typography.titleLarge.copy(fontFamily = Lora),
    titleMedium = Typography.titleMedium.copy(fontFamily = Lora),
    labelLarge = Typography.labelLarge.copy(fontFamily = Lora),
    bodyLarge = Typography.bodyLarge.copy(fontFamily = Lora),
    bodyMedium = Typography.bodyMedium.copy(fontFamily = Lora)
)

// ============================================
// CUSTOM TEXT STYLES (Utility)
// ============================================

// For numeric/table data - using Arimo (monospace-like feel)
val DataTextStyle = TextStyle(
    fontFamily = Arimo,
    fontWeight = FontWeight.Normal,
    fontSize = 14.sp
)

// For Hindi/Devanagari text - using Lora
val HindiTextStyle = TextStyle(
    fontFamily = Lora,
    fontWeight = FontWeight.Normal,
    fontSize = 14.sp
)

// For Sauda (Contract) related content - using Serif
val SaudaTextStyle = TextStyle(
    fontFamily = Serif,
    fontWeight = FontWeight.Medium,
    fontSize = 16.sp
)

// For App Headers - using Google Sans
val HeaderTextStyle = TextStyle(
    fontFamily = GoogleSans,
    fontWeight = FontWeight.Bold,
    fontSize = 20.sp
)