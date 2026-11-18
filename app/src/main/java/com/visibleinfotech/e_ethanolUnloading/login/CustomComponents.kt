package com.visibleinfotech.e_ethanolUnloading.login

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.visibleinfotech.e_ethanolUnloading.ui.theme.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import com.visibleinfotech.e_ethanolUnloading.ui.theme.AppTypography
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material.icons.Icons
import androidx.compose.runtime.getValue
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign

import androidx.compose.ui.layout.ContentScale

import androidx.compose.material3.Card
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.compose.material.icons.filled.Check
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WatchLater
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import coil.request.ImageRequest
import com.visibleinfotech.e_ethanolUnloading.sqlConnection.ConnectionStatus
import com.visibleinfotech.e_ethanolUnloading.utils.KeyboardUtils

@Composable
fun FormLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = AppTypography.formLabel,
        modifier = modifier
    )
}

@Composable
fun FormValueText(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = AppTypography.formValue,
        modifier = modifier
    )
}

@Composable
fun FormRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "$label :",
            style = AppTypography.formLabel,
            modifier = Modifier.weight(0.4f)
        )
        Text(
            text = value,
            style = AppTypography.labelMedium,
            color = DarkGreen,
            modifier = Modifier.weight(0.6f)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormDropdownField(
    value: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onItemSelected: (String) -> Unit,
    items: List<String>,
    label: String = "",
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        if (label.isNotEmpty()) {
            FormLabel(text = label)
            Spacer(modifier = Modifier.height(8.dp))
        }
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = onExpandedChange
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                shape = RoundedCornerShape(12.dp),
                textStyle = AppTypography.formValue,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryBlue,
                    unfocusedBorderColor = BorderColor,
                    focusedTextColor = TextDark,
                    unfocusedTextColor = TextDark
                )
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { onExpandedChange(false) }
            ) {
                items.forEach { item ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = item,
                                style = AppTypography.formValue
                            )
                        },
                        onClick = {
                            onItemSelected(item)
                            onExpandedChange(false)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun FormTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    errorMessage: String? = null
) {
    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = {
                Text(
                    text = label,
                    style = AppTypography.formLabel
                )
            },
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            textStyle = AppTypography.formValue,
            isError = isError,
            supportingText = {
                if (isError && errorMessage != null) {
                    Text(
                        text = errorMessage,
                        style = AppTypography.error
                    )
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryBlue,
                unfocusedBorderColor = BorderColor,
                focusedTextColor = TextDark,
                unfocusedTextColor = TextDark,
                focusedLabelColor = PrimaryBlue,
                unfocusedLabelColor = TextDark.copy(alpha = 0.6f),
                errorBorderColor = ErrorRed,
                errorLabelColor = ErrorRed
            )
        )
    }
}


@Composable
fun CustomTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    isError: Boolean = false,
    supportingText: @Composable (() -> Unit)? = null,
    shape: RoundedCornerShape = RoundedCornerShape(12.dp),
    focusedContainerColor: Color = BackgroundDarkColor,
    unfocusedContainerColor: Color = BackgroundDarkColor,
    focusedIndicatorColor: Color = DarkViolet,
    unfocusedIndicatorColor: Color = Color.Gray.copy(alpha = 0.3f)
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = modifier.fillMaxWidth(),
        isError = isError,
        supportingText = supportingText,
        shape = shape,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = focusedContainerColor,
            unfocusedContainerColor = unfocusedContainerColor,
            focusedIndicatorColor = focusedIndicatorColor,
            unfocusedIndicatorColor = unfocusedIndicatorColor,
            focusedLabelColor = focusedIndicatorColor,  // Label becomes DarkViolet when focused
            unfocusedLabelColor = TextDark.copy(alpha = 0.6f)
        )
    )
}

@Composable
fun PrimarySubmitButton(
    onClick: () -> Unit,
    text: String,
    gradientColors: List<Color> = listOf(DarkGreen, DarkGreen), // Default colors
    shadowColor: Color = DarkGreen, // Default shadow color
    isLoading: Boolean = false, // Add loading parameter
    enabled: Boolean = true, // Add enabled parameter
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && !isLoading && enabled) 0.96f else 1f,
        label = "scale"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow(
                elevation = if (isPressed && !isLoading && enabled) 4.dp else 12.dp,
                shape = RoundedCornerShape(18.dp),
                ambientColor = shadowColor.copy(alpha = 0.4f),
                spotColor = shadowColor.copy(alpha = 0.4f)
            )
            .clip(RoundedCornerShape(18.dp))
            .background(
                brush = Brush.linearGradient(colors = gradientColors)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled && !isLoading // Disable when loading or not enabled
            ) {
                if (!isLoading && enabled) {
                    onClick()
                }
            },
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            // Show loading indicator
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = Color.White,
                strokeWidth = 2.dp
            )
        } else {
            Text(
                text = text,
                style = AppTypography.button,
                color = Color.White
            )
        }
    }
}

@Composable
fun DateTimeCard(
    dateTime: String,
    modifier: Modifier = Modifier,
    textColor: Color = ErrorRed,
    backgroundColor: Color = PrimaryBlue.copy(alpha = 0.1f)
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        ),
        shape = RoundedCornerShape(8.dp) // Smaller corner
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp), // Reduced padding
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.WatchLater,
                contentDescription = "Time",
                tint = textColor,
                modifier = Modifier.size(14.dp) // Smaller icon
            )

            Spacer(modifier = Modifier.width(6.dp))

            Text(
                text = "Date & Time: $dateTime",
                style = AppTypography.dateTime,
                color = textColor,
                fontSize = 11.sp, // Smaller font
                textAlign = TextAlign.Center
            )
        }
    }
}
@Composable
fun SectionTitle(
    title: String,
    modifier: Modifier = Modifier,
    color: Color = DarkGreen  // Added color parameter with default value
) {
    Text(
        text = title,
        style = AppTypography.titleMedium,
        color = color,  // Using the color parameter
        modifier = modifier.padding(vertical = 8.dp)
    )
}
@Composable
fun InfoText(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = AppTypography.caption,
        color = TextDark.copy(alpha = 0.7f),
        modifier = modifier
    )
}


@Composable
fun DashboardCard(
    title: String,
    subtitle: String,
    icon: Int,
    gradientColors: List<Color>,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .shadow(
                elevation = 10.dp,
                shape = RoundedCornerShape(24.dp),
                clip = false
            )
            .clip(RoundedCornerShape(24.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = gradientColors
                    )
                )
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = AppTypography.titleLarge,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = subtitle,
                    style = AppTypography.bodyMedium,
                    color = Color.White.copy(alpha = 0.9f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Image(
                painter = painterResource(id = icon),
                contentDescription = title,
                modifier = Modifier.size(72.dp), // Removed shadow modifier
                contentScale = ContentScale.Fit
            )
        }
    }
}
@Composable
fun CancelChequeImageUploadCard(
    imageUri: Uri?,
    onImageSelected: (Uri?) -> Unit,
    onShowOptions: () -> Unit,
    title: String = "Cancel Cheque",  // Added
    accentColor: Color = DarkViolet,  // Added
    isError: Boolean = false,
    errorMessage: String? = null,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }

    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (imageUri == null) 100.dp else 140.dp)
                .border(
                    width = 1.dp,
                    color = if (isError) ErrorRed else accentColor.copy(alpha = 0.3f), // Use accentColor
                    shape = RoundedCornerShape(12.dp)
                ),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = WhiteColor
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onShowOptions
                    )
            ) {
                if (imageUri == null) {
                    // Empty state - compact
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Icon
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(
                                    accentColor.copy(alpha = 0.1f), // Use accentColor
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(id = android.R.drawable.ic_menu_camera),
                                contentDescription = "Upload",
                                modifier = Modifier.size(24.dp),
                                tint = if (isError) ErrorRed else accentColor // Use accentColor
                            )
                        }

                        // Text content
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = if (isError) "$title Required" else "Upload $title", // Use title
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isError) ErrorRed else accentColor // Use accentColor
                            )
                            Text(
                                text = "Tap to select or capture image",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "JPG/PNG",
                                    fontSize = 10.sp,
                                    color = Color.Gray.copy(alpha = 0.7f)
                                )
                            }
                        }

                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = null,
                            tint = accentColor.copy(alpha = 0.5f), // Use accentColor
                            modifier = Modifier.size(16.dp)
                        )
                    }
                } else {
                    // Image preview - same as before but using accentColor for some elements
                    Box(modifier = Modifier.fillMaxSize()) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(imageUri)
                                .crossfade(true)
                                .build(),
                            contentDescription = title, // Use title
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )

                        // Overlay
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.4f))
                        )

                        // Top right - Remove button
                        Surface(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                                .size(28.dp)
                                .clickable { onImageSelected(null) },
                            shape = CircleShape,
                            color = Color.Black.copy(alpha = 0.7f)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remove",
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }

                        // Bottom center - Change button
                        Surface(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(8.dp)
                                .clickable(onClick = onShowOptions),
                            shape = RoundedCornerShape(16.dp),
                            color = Color.Black.copy(alpha = 0.8f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = "Change",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        // Top left - Badge
                        Surface(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(8.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF4CAF50).copy(alpha = 0.9f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(10.dp)
                                )
                                Text(
                                    text = "Uploaded",
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }

        // Error message
        if (errorMessage != null) {
            Row(
                modifier = Modifier
                    .padding(start = 8.dp, top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = ErrorRed,
                    modifier = Modifier.size(12.dp)
                )
                Text(
                    text = errorMessage,
                    color = ErrorRed,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        } else if (imageUri == null) {
            // Helper text
            Row(
                modifier = Modifier
                    .padding(start = 8.dp, top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = accentColor, // Use accentColor
                    modifier = Modifier.size(10.dp)
                )
                Text(
                    text = "Clear scanned copy of $title", // Use title
                    color = Color.Gray,
                    fontSize = 10.sp
                )
            }
        }
    }
}
@Composable
fun ImagePickerBottomSheetContent(
    title: String, // Dynamic title from caller
    onGalleryClick: () -> Unit,
    onCameraClick: () -> Unit,
    onCloseClick: () -> Unit = {} // Optional close handler
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(WhiteColor, Color(0xFFF8F6FF))
                )
            )
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        // Single title row with close button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Single title text - no subtitle
            Text(
                text = title, // Dynamic title from parameter
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = DarkViolet,
                letterSpacing = 0.5.sp
            )

            IconButton(
                onClick = onCloseClick,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Color.Gray,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Modern image options in a row (horizontal layout)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Gallery Option
            ModernCompactImageCard(
                title = "Gallery",
                subtitle = "Select from device",
                icon = Icons.Default.PhotoLibrary,
                gradientColors = listOf(Color(0xFFEDE4FF), Color(0xFFE0D4FF)),
                onClick = onGalleryClick,
                modifier = Modifier.weight(1f)
            )

            // Camera Option
            ModernCompactImageCard(
                title = "Camera",
                subtitle = "Capture new photo",
                icon = Icons.Default.CameraAlt,
                gradientColors = listOf(Color(0xFFE8EFFF), Color(0xFFDCE8FF)),
                onClick = onCameraClick,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}
@Composable
fun ModernCompactImageCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    gradientColors: List<Color>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(120.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp,
            pressedElevation = 4.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = gradientColors,
                        startY = 0f,
                        endY = 0.6f
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Icon with circular background
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White.copy(alpha = 0.9f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = DarkViolet,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Column {
                    Text(
                        text = title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = DarkViolet
                    )
                    Text(
                        text = subtitle,
                        fontSize = 11.sp,
                        color = Color.Gray.copy(alpha = 0.7f),
                        maxLines = 1
                    )
                }
            }
        }
    }
}
@Composable
fun DashboardMenuCard(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    icon: Int,
    gradientColors: List<Color>,
    onClick: () -> Unit
) {

    Card(
        modifier = modifier
            .height(160.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
    ) {

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = gradientColors
                    )
                )
                .padding(14.dp)
        ) {

            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {

                // Icon
                Box(
                    modifier = Modifier
                        .size(62.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = icon),
                        contentDescription = title,
                        modifier = Modifier.size(40.dp),
                        tint = Color.Unspecified
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Title
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp,
                    maxLines = 2
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Subtitle
                Text(
                    text = subtitle,
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun DashboardSectionTitle(
    title: String,
    backgroundColor: Color = PrimaryBlue.copy(alpha = 0.12f),
    borderColor: Color = DarkGreen.copy(alpha = 0.25f),
    textColor: Color = DarkGreen
) {

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {

        Box(
            modifier = Modifier
                .background(
                    backgroundColor,
                    shape = RoundedCornerShape(18.dp)
                )
                .border(
                    1.dp,
                    borderColor,
                    RoundedCornerShape(18.dp)
                )
                .padding(
                    horizontal = 32.dp,
                    vertical = 10.dp
                ),
            contentAlignment = Alignment.Center
        ) {

            Text(
                text = title,
                color = textColor,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp,
                fontFamily = FontFamily.Serif
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommonUppercaseTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    maxLength: Int = 100,
    enabled: Boolean = true,
    singleLine: Boolean = true,
    minLines: Int = 1,
    maxLines: Int = 1,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    isError: Boolean = false,
    errorMessage: String? = null,
    supportingMessage: String? = null,
    showCharacterCount: Boolean = false, // Changed to false by default
    isRequired: Boolean = true,
    focusRequester: FocusRequester? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = { newValue ->
            val processedValue = newValue.uppercase().take(maxLength)
            onValueChange(processedValue)
        },
        label = { Text(if (isRequired) "$label *" else label, fontSize = 12.sp) },
        placeholder = { if (placeholder.isNotEmpty()) Text(placeholder, fontSize = 13.sp) },
        modifier = modifier.fillMaxWidth().then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier),
        singleLine = singleLine,
        minLines = minLines,
        maxLines = maxLines,
        enabled = enabled,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction, capitalization = KeyboardCapitalization.Characters),
        isError = isError,
        supportingText = {
            when {
                isError && errorMessage != null -> Text(text = errorMessage, color = ErrorRed, fontSize = 10.sp)
                supportingMessage != null -> Text(text = supportingMessage, color = Color.Gray, fontSize = 10.sp)
            }
        },
        textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
        shape = RoundedCornerShape(10.dp), // Smaller corner
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = DarkViolet,
            unfocusedBorderColor = DarkViolet.copy(alpha = 0.4f),
            focusedLabelColor = DarkViolet,
            cursorColor = DarkViolet,
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White,
            errorBorderColor = if (isError) ErrorRed else DarkViolet.copy(alpha = 0.4f),
            errorLabelColor = if (isError) ErrorRed else DarkViolet,
            errorCursorColor = ErrorRed,
            errorContainerColor = Color.White
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MobileNumberTextField(
    mobileNumber: String,
    onMobileNumberChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Mobile Number *",
    showValidationErrors: Boolean = false,
    isError: Boolean = false,
    focusRequester: FocusRequester? = null,
    imeAction: ImeAction = ImeAction.Next
) {
    val hasError = isError || (showValidationErrors && (mobileNumber.isBlank() || mobileNumber.length != 10))

    OutlinedTextField(
        value = mobileNumber,
        onValueChange = { input ->
            val filtered = input.filter { it.isDigit() }.take(10)
            onMobileNumberChange(filtered)
        },
        label = { Text(label, fontSize = 12.sp) },
        placeholder = { Text("Enter 10-digit mobile number", fontSize = 13.sp) },
        modifier = modifier.fillMaxWidth().then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = imeAction),
        isError = hasError,
        supportingText = {
            when {
                hasError && mobileNumber.isBlank() -> Text(text = "Mobile number is required", color = ErrorRed, fontSize = 10.sp)
                hasError && mobileNumber.length != 10 -> Text(text = "Enter 10 digits (${mobileNumber.length}/10)", color = ErrorRed, fontSize = 10.sp)
                mobileNumber.length == 10 && !hasError -> Text(text = "✓ Valid", color = Color.Green, fontSize = 10.sp)
                else -> Text(text = "Enter 10 digit mobile number", color = Color.Gray, fontSize = 10.sp)
            }
        },
        textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
        shape = RoundedCornerShape(10.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = DarkViolet,
            unfocusedBorderColor = DarkViolet.copy(alpha = 0.4f),
            cursorColor = DarkViolet,
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White,
            errorBorderColor = if (hasError) ErrorRed else DarkViolet.copy(alpha = 0.4f),
            errorLabelColor = if (hasError) ErrorRed else DarkViolet,
            errorCursorColor = ErrorRed,
            errorContainerColor = Color.White
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountNumberTextField(
    accountNumber: String,
    onAccountNumberChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Account Number *",
    showValidationErrors: Boolean = false,
    isError: Boolean = false,
    minLength: Int = 8,
    maxLength: Int = 18,
    focusRequester: FocusRequester? = null,
    imeAction: ImeAction = ImeAction.Done
) {
    val hasError = isError || (
            showValidationErrors && (
                    accountNumber.isBlank() ||
                            accountNumber.length < minLength ||
                            accountNumber.length > maxLength
                    )
            )

    OutlinedTextField(
        value = accountNumber,
        onValueChange = { input ->
            val filtered = input.filter { it.isDigit() }.take(maxLength)
            onAccountNumberChange(filtered)
        },
        label = { Text(label, fontSize = 12.sp) },
        placeholder = { Text("Enter Account Number", fontSize = 13.sp) },
        modifier = modifier.fillMaxWidth().then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = imeAction),
        isError = hasError,
        supportingText = {
            when {
                hasError && accountNumber.isBlank() -> Text(text = "Account number required", color = ErrorRed, fontSize = 10.sp)
                hasError && accountNumber.length < minLength -> Text(text = "Min $minLength digits (${accountNumber.length}/$minLength)", color = ErrorRed, fontSize = 10.sp)
                hasError && accountNumber.length > maxLength -> Text(text = "Max $maxLength digits", color = ErrorRed, fontSize = 10.sp)
                accountNumber.length in minLength..maxLength && !hasError -> Text(text = "✓ Valid (${accountNumber.length} digits)", color = Color.Green, fontSize = 10.sp)
                else -> Text(text = "$minLength-$maxLength digits", color = Color.Gray, fontSize = 10.sp)
            }
        },
        textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
        shape = RoundedCornerShape(10.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = DarkViolet,
            unfocusedBorderColor = DarkViolet.copy(alpha = 0.4f),
            cursorColor = DarkViolet,
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White,
            errorBorderColor = if (hasError) ErrorRed else DarkViolet.copy(alpha = 0.4f),
            errorLabelColor = if (hasError) ErrorRed else DarkViolet,
            errorCursorColor = ErrorRed,
            errorContainerColor = Color.White
        )
    )
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> ModernSearchableDropdown(
    searchText: String,
    onSearchTextChange: (String) -> Unit,
    onClearClick: () -> Unit = {},
    label: String,
    items: List<T>,
    getItemLabel: (T) -> String,
    getItemId: (T) -> String,
    onItemSelected: (T) -> Unit,
    onDropdownDismiss: () -> Unit,
    isError: Boolean = false,
    errorMessage: String? = null,
    focusRequester: FocusRequester? = null,
    accentColor: Color = DarkGreen,
    noResultsMessage: String? = null,
    itemIcon: ImageVector = Icons.Default.Person,
    itemIconColor: Color = accentColor,
    itemTextColor: Color = Color.Black,
    modifier: Modifier = Modifier  // Added modifier parameter
){
    var expanded by remember { mutableStateOf(false) }
    val hideKeyboard = KeyboardUtils.hideKeyboard()

    Column(modifier = modifier) {  // Apply modifier here
        OutlinedTextField(
            value = searchText,
            onValueChange = {
                onSearchTextChange(it)
                expanded = true
            },
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged {
                    if (it.isFocused) {
                        expanded = true
                    }
                }
                .then(
                    focusRequester?.let { Modifier.focusRequester(it) } ?: Modifier
                ),
            label = { Text("$label *", fontSize = 13.sp) },
            placeholder = { Text("Search ${label.lowercase()}...", fontSize = 12.sp) },
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            isError = isError,
            supportingText = {
                if (isError && errorMessage != null) {
                    Text(
                        text = errorMessage,
                        color = ErrorRed,
                        fontSize = 11.sp
                    )
                }
            },
            trailingIcon = {
                if (searchText.isNotEmpty()) {
                    IconButton(onClick = {
                        onSearchTextChange("")
                        onClearClick()
                        expanded = false
                    }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                } else {
                    IconButton(onClick = { expanded = !expanded }) {
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Dropdown",
                            tint = accentColor
                        )
                    }
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = accentColor,
                unfocusedBorderColor = accentColor.copy(alpha = 0.3f),
                errorBorderColor = ErrorRed,
                focusedLabelColor = if (isError) ErrorRed else accentColor,
                errorLabelColor = ErrorRed,
            )
        )

        if (expanded && items.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp)
                    .heightIn(max = 240.dp),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                LazyColumn {
                    items(items = items, key = { getItemId(it) }) { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onItemSelected(item)
                                    expanded = false
                                    hideKeyboard()
                                    onDropdownDismiss()
                                }
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = itemIcon,
                                contentDescription = null,
                                tint = itemIconColor,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = getItemLabel(item),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = itemTextColor
                            )
                        }
                        HorizontalDivider(thickness = 0.5.dp, color = Color.LightGray.copy(alpha = 0.3f))
                    }
                }
            }
        }

        // NO RESULT FOUND
        if (expanded && searchText.isNotEmpty() && items.isEmpty() && noResultsMessage != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = Color(0xFFFF9800)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = noResultsMessage,
                        fontSize = 13.sp,
                        color = Color(0xFFE65100)
                    )
                }
            }
        }
    }
}
@Composable
fun CompactInputField(
    value: String,
    onValueChange: (String) -> Unit,
    hint: String,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    errorMessage: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    accentColor: Color = DarkGreen,
    icon: ImageVector? = null,
    focusRequester: FocusRequester? = null,
    readOnly: Boolean = false  // Add this parameter
) {
    Column {
        Box(
            modifier = modifier
                .shadow(
                    elevation = if (isError) 0.dp else 2.dp,
                    shape = RoundedCornerShape(8.dp)
                )
                .border(
                    width = if (isError) 1.dp else 0.dp,
                    color = if (isError) ErrorRed else accentColor,
                    shape = RoundedCornerShape(8.dp)
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (isError) ErrorRed.copy(alpha = 0.08f) else Color(0xFFF8F9FA),
                        RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (icon != null) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (isError) ErrorRed else accentColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    BasicTextField(
                        value = value,
                        onValueChange = onValueChange,
                        modifier = Modifier
                            .weight(1f)
                            .then(focusRequester?.let { Modifier.focusRequester(it) } ?: Modifier),
                        singleLine = true,
                        readOnly = readOnly,
                        textStyle = LocalTextStyle.current.copy(
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isError) ErrorRed else Color.Black
                        ),
                        keyboardOptions = keyboardOptions,
                        keyboardActions = keyboardActions,
                        decorationBox = { innerTextField ->
                            Box {
                                if (value.isEmpty()) {
                                    Text(
                                        text = hint,
                                        fontSize = 12.sp,
                                        color = if (isError) ErrorRed else Color.Gray
                                    )
                                }
                                innerTextField()
                            }
                        }
                    )
                }
            }
        }

        // Error message display
        if (isError && errorMessage != null) {
            Text(
                text = errorMessage,
                fontSize = 11.sp,
                color = ErrorRed,
                modifier = Modifier.padding(start = 12.dp, top = 4.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompactHintDropdown(
    value: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onItemSelected: (String) -> Unit,
    items: List<String>,
    hint: String,
    accentColor: Color = DarkGreen,
    iconColor: Color = DarkGreen,
    textColor: Color = DarkGreen,
    icon: ImageVector? = null,
    isError: Boolean = false,
    errorMessage: String? = null
) {
    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = if (isError) 0.dp else 2.dp,
                    shape = RoundedCornerShape(8.dp),
                    clip = false
                )
                .border(
                    width = if (isError) 1.dp else 0.dp,
                    color = if (isError) ErrorRed else DarkGreen,
                    shape = RoundedCornerShape(8.dp)
                )

        ) {
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = onExpandedChange
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onExpandedChange(!expanded) }
                        .background(
                            if (isError) ErrorRed.copy(alpha = 0.08f) else Color(0xFFF8F9FA),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (icon != null) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = if (isError) ErrorRed else iconColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }

                        if (value.isEmpty()) {
                            Text(
                                text = hint,
                                fontSize = 12.sp,
                                color = if (isError) ErrorRed else Color.Gray
                            )
                        } else {
                            Text(
                                text = value,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (isError) ErrorRed else textColor
                            )
                        }
                    }

                    Icon(
                        imageVector = if (expanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        tint = if (isError) ErrorRed else accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }

                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { onExpandedChange(false) },
                    modifier = Modifier
                        .background(Color.White)
                        .widthIn(max = 280.dp)
                ) {
                    items.forEach { item ->
                        DropdownMenuItem(
                            leadingIcon = icon?.let {
                                {
                                    Icon(
                                        imageVector = it,
                                        contentDescription = null,
                                        tint = accentColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            },
                            text = {
                                Text(
                                    text = item,
                                    fontSize = 13.sp,
                                    fontWeight = if (item == value) FontWeight.Bold else FontWeight.Normal,
                                    color = if (item == value) accentColor else Color.Black
                                )
                            },
                            onClick = {
                                onItemSelected(item)
                                onExpandedChange(false)
                            }
                        )
                    }
                }
            }
        }

        // Error message display
        if (isError && errorMessage != null) {
            Text(
                text = errorMessage,
                fontSize = 11.sp,
                color = ErrorRed,
                modifier = Modifier.padding(start = 12.dp, top = 4.dp)
            )
        }
    }
}
@Composable
fun FormSectionCard(
    title: String,
    accentColor: Color = DarkViolet,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = accentColor // Use the provided color or default DarkViolet
            )

            Spacer(modifier = Modifier.height(10.dp))

            content()
        }
    }
}

@Composable
fun DispatchReadOnlyRow(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = PrimaryBlue,
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "$label:",
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = PrimaryBlue,
            modifier = Modifier.width(100.dp)
        )
        Text(
            text = value.ifEmpty { "-" },
            fontSize = 12.sp,
            fontWeight = FontWeight.Normal,
            color = Color.Black
        )
    }
}
@Composable
fun CompactHeader(
    username: String,
    userCode: String,
    connectionStatus: ConnectionStatus,
) {
    val statusText: String
    val statusColor: Color

    when (connectionStatus) {
        ConnectionStatus.CONNECTED -> {
            statusText = "LIVE"
            statusColor = Color(0xFF00E676)
        }
        ConnectionStatus.SERVER_DOWN -> {
            statusText = "SERVER DOWN"
            statusColor = Color(0xFFFF9800)
        }
        ConnectionStatus.NO_INTERNET -> {
            statusText = "NO INTERNET"
            statusColor = Color.Red
        }
        else -> {
            statusText = "CHECKING"
            statusColor = Color.Gray
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            LightViolet, LightViolet, BrandVioletLight )
                    )
                )
                .padding(horizontal = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Welcome Back, ($userCode)",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = username.ifEmpty { "User" },
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White.copy(alpha = 0.15f))
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(RoundedCornerShape(100.dp))
                            .background(statusColor)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = statusText,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}


@Composable
fun DocumentStatusRow(
    label: String,
    isAvailable: Boolean,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() },
        colors = CardDefaults.cardColors(
            containerColor = if (isAvailable) PrimaryGreen.copy(alpha = 0.1f) else ErrorRed.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Black
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = if (isAvailable) "Available" else "Not Available",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isAvailable) PrimaryGreen else ErrorRed
                )
                Icon(
                    imageVector = if (isAvailable) Icons.Default.CheckCircle else Icons.Default.Cancel,
                    contentDescription = if (isAvailable) "Available" else "Not Available",
                    tint = if (isAvailable) PrimaryGreen else ErrorRed,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullScreenImageViewer(
    imageUrl: String,
    onDismiss: () -> Unit
) {

    val context = LocalContext.current
    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }
    var lastClickTime by remember { mutableStateOf(0L) }

    val transformableState = rememberTransformableState { zoomChange, panChange, _ ->
        val newScale = (scale * zoomChange).coerceIn(1f, 5f)
        scale = newScale

        if (scale > 1f) {
            val maxOffsetX = 300f * (scale - 1f)
            val maxOffsetY = 300f * (scale - 1f)

            offsetX = (offsetX + panChange.x).coerceIn(-maxOffsetX, maxOffsetX)
            offsetY = (offsetY + panChange.y).coerceIn(-maxOffsetY, maxOffsetY)
        } else {
            offsetX = 0f
            offsetY = 0f
        }
    }

    BackHandler {
        if (scale > 1f) {
            // If zoomed in, reset first
            scale = 1f
            offsetX = 0f
            offsetY = 0f
        } else {
            onDismiss()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.95f))
    ) {

        IconButton(
            onClick = {
                if (scale > 1f) {
                    scale = 1f
                    offsetX = 0f
                    offsetY = 0f
                } else {
                    onDismiss()
                }
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .zIndex(1f)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close",
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
        }

        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(imageUrl)
                .crossfade(true)
                .error(android.R.drawable.ic_menu_gallery)
                .build(),
            contentDescription = "Full Screen Image",
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offsetX
                    translationY = offsetY
                }
                .clip(RoundedCornerShape(0.dp))
                .transformable(state = transformableState)
                .clickable {
                    // Double tap detection
                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastClickTime < 300) {
                        // Double tap - toggle zoom
                        if (scale > 1f) {
                            // Zoom out
                            scale = 1f
                            offsetX = 0f
                            offsetY = 0f
                        } else {
                            // Zoom in to center
                            scale = 3f
                            offsetX = 0f
                            offsetY = 0f
                        }
                        lastClickTime = 0L
                    } else {
                        lastClickTime = currentTime
                    }
                },
            contentScale = ContentScale.Fit
        )

        if (scale > 1f) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp)
                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "${(scale * 100).toInt()}%",
                    color = Color.White,
                    fontSize = MaterialTheme.typography.bodySmall.fontSize
                )
            }
        }
    }
}


@Composable
fun UploadProgressDialog(
    show: Boolean,
    progress: Int,
    statusMessage: String,
    imageName: String,
    onDismiss: () -> Unit = {}
) {
    if (show) {
        AlertDialog(
            onDismissRequest = onDismiss,
            containerColor = Color.White,
            shape = RoundedCornerShape(24.dp),
            title = {
                Text(
                    text = "Uploading Image",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryBlue
                )
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Image name and status
                    Text(
                        text = imageName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Black
                    )

                    Text(
                        text = statusMessage,
                        fontSize = 14.sp,
                        color = Color.Gray
                    )

                    // Progress bar with percentage
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .background(
                                Color.LightGray,
                                RoundedCornerShape(12.dp)
                            )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progress / 100f)
                                .height(8.dp)
                                .background(
                                    if (progress >= 100) PrimaryGreen else PrimaryBlue,
                                    RoundedCornerShape(12.dp)
                                )
                        )
                    }

                    // Percentage text
                    Text(
                        text = "$progress%",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryBlue
                    )

                    if (progress == 100) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Complete",
                            tint = PrimaryGreen,
                            modifier = Modifier.size(32.dp)
                        )
                        Text(
                            text = "Upload Complete!",
                            fontSize = 14.sp,
                            color = PrimaryGreen,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            confirmButton = {
                if (progress == 100) {
                    TextButton(
                        onClick = onDismiss,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = PrimaryBlue
                        )
                    ) {
                        Text("OK", fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                // No dismiss button while uploading
            }
        )
    }
}

@Composable
fun GateEntryConfirmationDialog(
    truckNo: String,
    driverName: String,
    driverMobile: String,
    modeCode: String,
    modeName: String,
    invoiceNo: String,
    quantity: String,
    partyName: String,
    brokerName: String,
    invoiceWeight: String,
    numberOfBags: String,
    invoiceRate: String,
    invoiceAmount: String,
    hasDoc1: Boolean,
    hasDoc2: Boolean,
    hasDoc3: Boolean,
    hasDoc4: Boolean,
    hasDoc5: Boolean,
    remarks: String,
    rfidTag: String = "",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Confirm Gate Entry",
                fontWeight = FontWeight.Bold,
                color = PrimaryBlue
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Truck Details
                Text(
                    text = "Truck Details",
                    fontWeight = FontWeight.Bold,
                    color = PrimaryBlue,
                    fontSize = 14.sp
                )
                HorizontalDivider(color = PrimaryBlue.copy(alpha = 0.3f))

                DetailRow(label = "Truck No", value = truckNo)
                DetailRow(label = "Driver Name", value = driverName)
                DetailRow(label = "Driver Mobile", value = driverMobile)

                // 👇 ADD RFID TAG HERE - After Driver Mobile
                DetailRow(
                    label = "RFID Tag",
                    value = rfidTag.ifEmpty { "Not scanned" },
                    valueColor = if (rfidTag.isNotEmpty()) PrimaryGreen else Color.Red
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Mode Details
                Text(
                    text = "Mode Details",
                    fontWeight = FontWeight.Bold,
                    color = VibrantTeal,
                    fontSize = 14.sp
                )
                HorizontalDivider(color = VibrantTeal.copy(alpha = 0.3f))

                DetailRow(label = "Mode", value = "$modeCode - $modeName")

                Spacer(modifier = Modifier.height(8.dp))

                // Dispatch Details
                Text(
                    text = "Dispatch Details",
                    fontWeight = FontWeight.Bold,
                    color = PrimaryBlue,
                    fontSize = 14.sp
                )
                HorizontalDivider(color = PrimaryBlue.copy(alpha = 0.3f))

                DetailRow(label = "Invoice No", value = invoiceNo)
                DetailRow(label = "Quantity", value = quantity)
                DetailRow(label = "Party Name", value = partyName)
                DetailRow(label = "Broker Name", value = brokerName)

                Spacer(modifier = Modifier.height(8.dp))

                // Invoice Details
                Text(
                    text = "Invoice Details",
                    fontWeight = FontWeight.Bold,
                    color = DarkOrange,
                    fontSize = 14.sp
                )
                HorizontalDivider(color = DarkOrange.copy(alpha = 0.3f))

                DetailRow(label = "Invoice Weight", value = invoiceWeight)
                DetailRow(label = "Number of Bags", value = numberOfBags)
                DetailRow(label = "Invoice Rate", value = invoiceRate)
                DetailRow(label = "Invoice Amount", value = invoiceAmount)

                Spacer(modifier = Modifier.height(8.dp))

                // Documents Status
                Text(
                    text = "Documents",
                    fontWeight = FontWeight.Bold,
                    color = PrimaryBlue,
                    fontSize = 14.sp
                )
                HorizontalDivider(color = PrimaryBlue.copy(alpha = 0.3f))

                DetailRow(
                    label = "Anugya",
                    value = if (hasDoc1) "✅ Uploaded" else "❌ Missing",
                    valueColor = if (hasDoc1) PrimaryGreen else Color.Red
                )
                DetailRow(
                    label = "Bilty",
                    value = if (hasDoc2) "✅ Uploaded" else "❌ Missing",
                    valueColor = if (hasDoc2) PrimaryGreen else Color.Red
                )
                DetailRow(
                    label = "Invoice",
                    value = if (hasDoc3) "✅ Uploaded" else "❌ Missing",
                    valueColor = if (hasDoc3) PrimaryGreen else Color.Red
                )
                DetailRow(
                    label = "Weighment",
                    value = if (hasDoc4) "✅ Uploaded" else "❌ Missing",
                    valueColor = if (hasDoc4) PrimaryGreen else Color.Red
                )
                DetailRow(
                    label = "Other",
                    value = if (hasDoc5) "✅ Uploaded" else "Optional",
                    valueColor = if (hasDoc5) PrimaryGreen else Color.Gray
                )

                if (remarks.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Remarks",
                        fontWeight = FontWeight.Bold,
                        color = PrimaryBlue,
                        fontSize = 14.sp
                    )
                    HorizontalDivider(color = PrimaryBlue.copy(alpha = 0.3f))
                    DetailRow(label = "Remarks", value = remarks)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryGreen
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Confirm", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = Color.Red
                )
            ) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(16.dp),
        containerColor = Color.White
    )
}

@Composable
fun DetailRow(
    label: String,
    value: String,
    valueColor: Color = Color.Black
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = Color.Gray,
            fontSize = 13.sp
        )
        Text(
            text = value,
            color = valueColor,
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp
        )
    }
}
@Composable
fun CompactAmountField(
    value: String,
    onValueChange: (String) -> Unit,
    hint: String,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    errorMessage: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    accentColor: Color = DarkGreen,
    focusRequester: FocusRequester? = null
) {
    Column {
        Box(
            modifier = modifier
                .shadow(
                    elevation = if (isError) 0.dp else 2.dp,
                    shape = RoundedCornerShape(8.dp)
                )
                .border(
                    width = if (isError) 1.dp else 0.dp,
                    color = if (isError) ErrorRed else DarkGreen,
                    shape = RoundedCornerShape(8.dp)
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (isError) ErrorRed.copy(alpha = 0.08f) else Color(0xFFF8F9FA),
                        RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "₹",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isError) ErrorRed else accentColor
                    )
                    Spacer(modifier = Modifier.width(8.dp))

                    BasicTextField(
                        value = value,
                        onValueChange = onValueChange,
                        modifier = Modifier
                            .weight(1f)
                            .then(focusRequester?.let { Modifier.focusRequester(it) } ?: Modifier),
                        singleLine = true,
                        textStyle = LocalTextStyle.current.copy(
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isError) ErrorRed else Color.Black
                        ),
                        keyboardOptions = keyboardOptions,
                        keyboardActions = keyboardActions,
                        decorationBox = { innerTextField ->
                            Box {
                                if (value.isEmpty()) {
                                    Text(
                                        text = hint,
                                        fontSize = 12.sp,
                                        color = if (isError) ErrorRed else Color.Gray
                                    )
                                }
                                innerTextField()
                            }
                        }
                    )
                }
            }
        }

        // Error message display
        if (isError && errorMessage != null) {
            Text(
                text = errorMessage,
                fontSize = 11.sp,
                color = ErrorRed,
                modifier = Modifier.padding(start = 12.dp, top = 4.dp)
            )
        }
    }
}

@Composable
fun CompactInputFieldNew(
    value: String,
    onValueChange: (String) -> Unit,
    hint: String,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    errorMessage: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    accentColor: Color = DarkGreen,
    icon: ImageVector? = null,
    focusRequester: FocusRequester? = null,
    enabled: Boolean = true                                    // ⬅️ 1) NEW parameter
) {
    Column {
        Box(
            modifier = modifier
                .alpha(if (enabled) 1f else 0.75f)             // ⬅️ 2) dim when disabled
                .shadow(
                    elevation = if (isError) 0.dp else 2.dp,
                    shape = RoundedCornerShape(8.dp)
                )
                .border(
                    width = if (isError) 1.dp else 0.dp,
                    color = if (isError) ErrorRed else accentColor,
                    shape = RoundedCornerShape(8.dp)
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        when {
                            isError -> ErrorRed.copy(alpha = 0.08f)
                            !enabled -> Color(0xFFF0F0F0)       // ⬅️ 3) disabled bg tint
                            else -> Color(0xFFF8F9FA)
                        },
                        RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (icon != null) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = when {
                                isError -> ErrorRed
                                !enabled -> Color(0xFF757575)   // ⬅️ grey icon when disabled
                                else -> accentColor
                            },
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    BasicTextField(
                        value = value,
                        onValueChange = onValueChange,
                        enabled = enabled,                       // ⬅️ forward to BasicTextField
                        modifier = Modifier
                            .weight(1f)
                            .then(focusRequester?.let { Modifier.focusRequester(it) } ?: Modifier),
                        singleLine = true,
                        textStyle = LocalTextStyle.current.copy(
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = when {
                                isError -> ErrorRed
                                !enabled -> Color(0xFF555555)   // ⬅️ grey text when disabled
                                else -> Color.Black
                            }
                        ),
                        keyboardOptions = keyboardOptions,
                        keyboardActions = keyboardActions,
                        decorationBox = { innerTextField ->
                            Box {
                                if (value.isEmpty()) {
                                    Text(
                                        text = hint,
                                        fontSize = 12.sp,
                                        color = when {
                                            isError -> ErrorRed
                                            !enabled -> Color(0xFF9E9E9E)
                                            else -> Color.Gray
                                        }
                                    )
                                }
                                innerTextField()
                            }
                        }
                    )
                }
            }
        }

        // Error message display
        if (isError && errorMessage != null) {
            Text(
                text = errorMessage,
                fontSize = 11.sp,
                color = ErrorRed,
                modifier = Modifier.padding(start = 12.dp, top = 4.dp)
            )
        }
    }
}

@Composable
fun CompactInputField(
    value: String,
    onValueChange: (String) -> Unit,
    hint: String,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    errorMessage: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    accentColor: Color = DarkGreen,
    icon: ImageVector? = null,
    focusRequester: FocusRequester? = null
) {
    Column {
        Box(
            modifier = modifier
                .shadow(
                    elevation = if (isError) 0.dp else 2.dp,
                    shape = RoundedCornerShape(8.dp)
                )
                .border(
                    width = if (isError) 1.dp else 0.dp,
                    color = if (isError) ErrorRed else accentColor,
                    shape = RoundedCornerShape(8.dp)
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (isError) ErrorRed.copy(alpha = 0.08f) else Color(0xFFF8F9FA),
                        RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (icon != null) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (isError) ErrorRed else accentColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    BasicTextField(
                        value = value,
                        onValueChange = onValueChange,
                        modifier = Modifier
                            .weight(1f)
                            .then(focusRequester?.let { Modifier.focusRequester(it) } ?: Modifier),
                        singleLine = true,
                        textStyle = LocalTextStyle.current.copy(
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isError) ErrorRed else Color.Black
                        ),
                        keyboardOptions = keyboardOptions,
                        keyboardActions = keyboardActions,
                        decorationBox = { innerTextField ->
                            Box {
                                if (value.isEmpty()) {
                                    Text(
                                        text = hint,
                                        fontSize = 12.sp,
                                        color = if (isError) ErrorRed else Color.Gray
                                    )
                                }
                                innerTextField()
                            }
                        }
                    )
                }
            }
        }

        // Error message display
        if (isError && errorMessage != null) {
            Text(
                text = errorMessage,
                fontSize = 11.sp,
                color = ErrorRed,
                modifier = Modifier.padding(start = 12.dp, top = 4.dp)
            )
        }
    }
}

