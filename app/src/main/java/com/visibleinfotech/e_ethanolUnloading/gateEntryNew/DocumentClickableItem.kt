package com.visibleinfotech.e_ethanolUnloading.gateEntryNew

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.visibleinfotech.e_ethanolUnloading.ui.theme.PrimaryBlue
import com.visibleinfotech.e_ethanolUnloading.ui.theme.PrimaryGreen

private const val TAG = "DocumentImageItem"

@Composable
fun DocumentClickableItem(
    label: String,
    imageUri: Uri?,
    imageName: String,
    isAvailable: Boolean,
    isUserUploaded: Boolean,
    imageBaseUrl: String?,
    onUploadClick: () -> Unit,
    onViewImage: (String) -> Unit,
    onRemoveImage: (() -> Unit)? = null
) {
    val context = LocalContext.current

    val fullImageUrl = remember(imageBaseUrl, imageName) {
        if (!imageBaseUrl.isNullOrEmpty() && imageName.isNotBlank()) {
            val trimmedName = imageName.trim()
            val fileNameWithExt = if (!trimmedName.endsWith(".jpg") && !trimmedName.endsWith(".png") && !trimmedName.endsWith(".jpeg")) {
                "$trimmedName.jpg"
            } else {
                trimmedName
            }
            "$imageBaseUrl$fileNameWithExt"
        } else {
            null
        }
    }

    val hasImage = isAvailable || isUserUploaded

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .clickable(
                enabled = hasImage
            ) {
                when {
                    isAvailable && fullImageUrl != null -> onViewImage(fullImageUrl)
                    isUserUploaded && imageUri != null -> onViewImage(imageUri.toString())
                }
            },
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Document icon/thumbnail
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        when {
                            hasImage -> PrimaryGreen.copy(alpha = 0.15f)
                            else -> Color(0xFFEEEEEE)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                when {
                    isAvailable && fullImageUrl != null -> {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(fullImageUrl)
                                .crossfade(true)
                                .error(android.R.drawable.ic_menu_gallery)
                                .build(),
                            contentDescription = label,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                    isUserUploaded && imageUri != null -> {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(imageUri)
                                .crossfade(true)
                                .build(),
                            contentDescription = label,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                    // No image - placeholder
                    else -> {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = label,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Black
                )
                Text(
                    text = when {
                        isAvailable -> "Available"
                        isUserUploaded -> "Uploaded by you"
                        else -> "Tap to upload"
                    },
                    fontSize = 11.sp,
                    color = when {
                        isAvailable -> PrimaryGreen
                        isUserUploaded -> PrimaryBlue
                        else -> Color.Gray
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            when {

                isAvailable -> {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Available",
                        tint = PrimaryGreen,
                        modifier = Modifier.size(20.dp)
                    )
                }
                // Image uploaded by user - show change and remove options
                isUserUploaded -> {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Change image button
                        IconButton(
                            onClick = onUploadClick,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddCircle,
                                contentDescription = "Change Image",
                                tint = PrimaryBlue,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        // Remove image button
                        onRemoveImage?.let { removeAction ->
                            IconButton(
                                onClick = removeAction,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remove Image",
                                    tint = Color.Red,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
                // No image - show upload button
                else -> {
                    IconButton(
                        onClick = onUploadClick,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddCircle,
                            contentDescription = "Upload",
                            tint = PrimaryBlue,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}