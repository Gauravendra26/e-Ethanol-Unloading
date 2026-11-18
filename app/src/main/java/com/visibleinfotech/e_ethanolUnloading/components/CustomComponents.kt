package com.visibleinfotech.e_ethanolUnloading.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.visibleinfotech.e_ethanolUnloading.MenuItem
import com.visibleinfotech.e_ethanolUnloading.R
import com.visibleinfotech.e_ethanolUnloading.ui.theme.BackgroundColor
import com.visibleinfotech.e_ethanolUnloading.ui.theme.BrandViolet
import com.visibleinfotech.e_ethanolUnloading.ui.theme.PrimaryBlue
import com.visibleinfotech.e_ethanolUnloading.ui.theme.TextDark
import com.visibleinfotech.e_ethanolUnloading.ui.theme.WhiteColor
import kotlin.collections.forEach
import androidx.compose.material3.Surface
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.visibleinfotech.e_ethanolUnloading.sqlConnection.ConnectionStatus
import com.visibleinfotech.e_ethanolUnloading.ui.theme.DarkYellow
import com.visibleinfotech.e_ethanolUnloading.ui.theme.ErrorRed
import com.visibleinfotech.e_ethanolUnloading.ui.theme.WarningYellow

@Composable
fun MenuRow(
    items: List<MenuItem>,
    onConnectionClick: (() -> Unit) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items.forEach { item ->
            if (item.countLabel != null) {
                CountMenuCard(
                    modifier = Modifier.weight(1f),
                    title = item.title,
                    countLabel = item.countLabel,
                    icon = item.icon,               // ← pass icon
                    gradientColors = item.gradientColors,
                    onClick = { onConnectionClick { item.onClick() } }
                )
            } else {
                ModernMenuCard(
                    modifier = Modifier.weight(1f),
                    title = item.title,
                    icon = item.icon,
                    gradientColors = item.gradientColors,
                    onClick = { onConnectionClick { item.onClick() } }
                )
            }
        }
    }
}

@Composable
fun ModernMenuCard(
    modifier: Modifier = Modifier,
    title: String,
    icon: Int,
    gradientColors: List<Color>,
    onClick: () -> Unit,
) {
    Card(
        modifier = modifier.height(82.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        onClick = onClick
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(colors = gradientColors))
                .padding(horizontal = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = icon),
                        contentDescription = null,
                        tint = Color.Unspecified,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = title,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    lineHeight = 18.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun CountMenuCard(
    modifier: Modifier = Modifier,
    title: String,
    countLabel: String,
    icon: Int,
    gradientColors: List<Color>,
    onClick: () -> Unit,
) {
    Card(
        modifier = modifier
            .height(130.dp)
            .shadow(8.dp, RoundedCornerShape(18.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = if (gradientColors.size >= 2) gradientColors
                        else listOf(Color.Gray, Color.DarkGray)
                    )
                )
                .padding(horizontal = 10.dp, vertical = 10.dp)
        ) {
            // Top row: icon (left) + big count (right-aligned visually)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(WhiteColor.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = icon),
                        contentDescription = title,
                        tint = Color.Unspecified,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // ─── Count number (top-right) ───
                Text(
                    text = countLabel,
                    color = WhiteColor,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = when {
                        countLabel.length >= 4 -> 22.sp
                        countLabel.length == 3 -> 26.sp
                        else -> 30.sp
                    },
                    maxLines = 1,
                    lineHeight = 30.sp
                )
            }

            // Bottom area: divider + title
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
            ) {
                // "Pending" label (small, under the number) — shift it here so we don't overload the top
                Text(
                    text = "Pending",
                    color = WhiteColor.copy(alpha = 0.75f),
                    fontWeight = FontWeight.Medium,
                    fontSize = 10.sp,
                    letterSpacing = 0.4.sp
                )
                Spacer(modifier = Modifier.height(4.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.55f)
                        .height(1.dp)
                        .background(WhiteColor.copy(alpha = 0.25f))
                )
                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = title,
                    color = WhiteColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    maxLines = 2,
                    lineHeight = 14.sp
                )
            }
        }
    }
}


@Composable
fun WebsiteLinkCard(
    modifier: Modifier = Modifier,
    onWebsiteClick: () -> Unit = {}
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onWebsiteClick() }
            .shadow(4.dp, RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = WhiteColor.copy(alpha = 0.9f)
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                // Decorative gradient bar
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height(26.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    PrimaryBlue,
                                    BrandViolet
                                )
                            )
                        )
                )

                Image(
                    painter = painterResource(id = R.drawable.logo),
                    contentDescription = "Visible Infotech Logo",
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(BackgroundColor)
                        .padding(2.dp),
                    contentScale = ContentScale.Fit
                )

                Column(horizontalAlignment = Alignment.Start) {
                    Text(
                        text = "VISIBLE INFOTECH PVT LTD",
                        color = TextDark,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Arrow indicator - Compact
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                PrimaryBlue.copy(alpha = 0.1f),
                                BrandViolet.copy(alpha = 0.1f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = android.R.drawable.ic_menu_share),
                    contentDescription = "Visit Website",
                    tint = BrandViolet,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}



@Composable
fun WelcomeCard(
    modifier: Modifier = Modifier,
    username: String?,
    userCode: String?,
    connectionStatus: ConnectionStatus
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            PrimaryBlue,
                            BrandViolet
                        )
                    )
                )
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {

                    Column {
                        Text(
                            text = "Welcome back 👋",
                            color = WhiteColor.copy(alpha = 0.8f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = username ?: "User",
                            color = WhiteColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(5.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (connectionStatus == ConnectionStatus.CONNECTED)
                                            Color(0xFF4ADE80)
                                        else if (connectionStatus == ConnectionStatus.NO_INTERNET)
                                            Color(0xFFF87171)
                                        else
                                            Color(0xFFFBBF24)
                                    )
                            )
                            Text(
                                text = "Account ID: ${userCode ?: "N/A"}",
                                color = WhiteColor.copy(alpha = 0.7f),
                                fontSize = 10.sp
                            )
                        }
                    }
                }

                // Connection Status Badge - Compact
                ConnectionStatusBadge(connectionStatus = connectionStatus)
            }
        }
    }
}


@Composable
private fun ConnectionStatusBadge(connectionStatus: ConnectionStatus) {
    val (label, color) = when (connectionStatus) {
        ConnectionStatus.CONNECTED -> "Online" to Color(0xFF22C55E)
        ConnectionStatus.NO_INTERNET -> "Offline" to ErrorRed
        else -> "Connecting" to WarningYellow
    }

    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .shadow(3.dp, RoundedCornerShape(16.dp)),
        color = color,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(WhiteColor)
            )
            Text(
                text = label,
                color = WhiteColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun CompactDialogRowHalf(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF8F9FA)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(0.5f)
            ) {

                Text(
                    text = label,
                    fontSize = 10.sp,
                    color = Color.Gray,
                    fontWeight = FontWeight.Medium
                )
            }
            Text(
                text = value,
                fontSize = 12.sp,
                color = Color.Black,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(0.5f),
                textAlign = TextAlign.End
            )
        }
    }
}

@Composable
fun QuantityExceedsDialog(
    enteredQuantity: Double,
    availableQuantity: Double,
    unit: String = "Qtls",
    onClear: () -> Unit,
    accentColor: Color = Color(0xFFFF6B6B),
    clearButtonText: String = "Clear"
) {
    Dialog(
        onDismissRequest = onClear,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            shape = RoundedCornerShape(28.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    accentColor.copy(alpha = 0.1f),
                                    accentColor.copy(alpha = 0.05f)
                                ),
                                radius = 80f
                            ),
                            shape = RoundedCornerShape(36.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(40.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Quantity Exceeds Available Stock",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1F2937),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "You have entered ${String.format("%.2f", enteredQuantity)} $unit, but only ${String.format("%.2f", availableQuantity)} $unit is available for dispatch.\n\nPlease enter a quantity within the available limit.",
                    fontSize = 14.sp,
                    color = Color(0xFF4B5563),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onClear,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accentColor
                    ),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 2.dp,
                        pressedElevation = 4.dp
                    )
                ) {
                    Text(
                        text = clearButtonText,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun MinimumQuantityDialog(
    title: String = "Minimum Quantity Required",
    enteredQuantity: Double,
    minimumQuantity: Double,
    unit: String = "Quintal",
    onClear: () -> Unit,
    accentColor: Color = Color(0xFFFF9800),
    clearButtonText: String = "Clear"
) {
    Dialog(
        onDismissRequest = onClear,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            shape = RoundedCornerShape(28.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Icon Circle
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    accentColor.copy(alpha = 0.1f),
                                    accentColor.copy(alpha = 0.05f)
                                ),
                                radius = 80f
                            ),
                            shape = RoundedCornerShape(36.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(40.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Title
                Text(
                    text = title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1F2937),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Message
                Text(
                    text = "You have entered ${String.format("%.2f", enteredQuantity)} $unit, but the minimum dispatch quantity is ${String.format("%.2f", minimumQuantity)} $unit.\n\nPlease enter a quantity of at least ${String.format("%.2f", minimumQuantity)} $unit.",
                    fontSize = 14.sp,
                    color = Color(0xFF4B5563),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Single Clear Button
                Button(
                    onClick = onClear,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accentColor
                    ),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 2.dp,
                        pressedElevation = 4.dp
                    )
                ) {
                    Text(
                        text = clearButtonText,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun MinimumQuantityCard(
    limit: Double,
    label: String = "Minimum Quantity Limit:",
    unit: String = "Quintal",
    backgroundColor: Color = DarkYellow.copy(alpha = 0.2f),
    borderColor: Color = DarkYellow.copy(alpha = 0.3f),
    textColor: Color = DarkYellow,
    modifier: Modifier = Modifier
) {
    if (limit > 0) {
        Card(
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = backgroundColor),
            border = BorderStroke(1.dp, borderColor)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp, horizontal = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = textColor,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = label,
                        fontSize = 10.sp,
                        color = Color.Black
                    )
                }
                Text(
                    text = "${String.format("%.2f", limit)} $unit",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
            }
        }
    }


}
