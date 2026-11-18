package com.visibleinfotech.e_ethanolUnloading

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.visibleinfotech.e_ethanolUnloading.ui.theme.PrimaryGreen
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestingScreen(
    onBack: () -> Unit = {},
    viewModel: RFIDScannerViewModel = viewModel()
) {

    val context = LocalContext.current
    val scannerState by viewModel.scannerState.collectAsState()
    val view = LocalView.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var isInitializing by remember { mutableStateOf(true) }
    val isScanning by remember { derivedStateOf { scannerState.isScanning } }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            viewModel.initializeScanner(context)
        } else {
            Toast.makeText(context, "Permissions required for RFID scanning", Toast.LENGTH_LONG).show()
            isInitializing = false
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    Log.d("TestingScreen", "Screen paused - resetting scanner")
                }
                Lifecycle.Event.ON_RESUME -> {
                    Log.d("TestingScreen", "Screen resumed - reinitializing scanner if needed")
                    if (!scannerState.isConnected) {
                        isInitializing = true
                        viewModel.initializeScanner(context)
                    }
                }
                Lifecycle.Event.ON_DESTROY -> {
                    Log.d("TestingScreen", "Screen destroyed - resetting scanner")
                    viewModel.resetScanner()
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(Unit) {
        Log.d("TestingScreen", "Checking permissions...")
        isInitializing = true
        val neededPermissions = listOf(
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.RECORD_AUDIO
        )
        val hasAllPermissions = neededPermissions.all {
            context.checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED
        }
        if (!hasAllPermissions) {
            Log.d("TestingScreen", "Requesting permissions...")
            permissionLauncher.launch(neededPermissions.toTypedArray())
        } else {
            Log.d("TestingScreen", "Permissions already granted, initializing scanner...")
            viewModel.initializeScanner(context)
            delay(1000)
            isInitializing = false
        }
    }

    if (isInitializing) {
        Dialog(
            onDismissRequest = {},
            properties = DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false
            )
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Initializing RFID Scanner...",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Please wait",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    if (isScanning) {
        Dialog(
            onDismissRequest = {},
            properties = DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false
            )
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Scanning for RFID Tag...",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Hold the scanner near the tag",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("RFID Scanner") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                } ,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
                .clickable {
                    view.requestFocus()
                },
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Scanner Status Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (scannerState.isConnected)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (scannerState.isConnected) "✅ Scanner Ready" else "❌ Scanner Disconnected",
                        color = if (scannerState.isConnected)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Icon(
                        imageVector = if (scannerState.isConnected)
                            Icons.Default.CheckCircle
                        else
                            Icons.Default.Error,
                        contentDescription = null,
                        tint = if (scannerState.isConnected)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.error
                    )
                }
            }

            Text(
                text = "RFID Tag",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            // Single Tag Input Field
            TagInputField(
                label = "Tag",
                tagValue = scannerState.tag1,
                onTagChange = { viewModel.updateTag1(it) },
                vehicleNumber = scannerState.vehicleNumber,
                foundVehicle = scannerState.foundVehicle1,
                isNewTag = scannerState.isNewTag1,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isScanning
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        Log.d("TestingScreen", "Scan button clicked")
                        viewModel.scanSingleTag()
                    },
                    modifier = Modifier.weight(1f),
                    enabled = !isScanning && scannerState.isConnected
                ) {
                    Icon(
                        imageVector = if (isScanning) Icons.Default.Pending else Icons.Default.RadioButtonChecked,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isScanning) "Scanning..." else "Scan")
                }


            }

        }
    }
}

@Composable
fun TagInputField(
    label: String,
    tagValue: String,
    onTagChange: (String) -> Unit,
    vehicleNumber: String,
    foundVehicle: String,
    isNewTag: Boolean,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val color = when {
        tagValue.isEmpty() -> Color.Unspecified // No tag, default color
        isNewTag -> MaterialTheme.colorScheme.primary // New tag (green)
        foundVehicle == vehicleNumber && vehicleNumber.isNotEmpty() -> MaterialTheme.colorScheme.secondary
        foundVehicle.isNotEmpty() && foundVehicle != vehicleNumber -> MaterialTheme.colorScheme.error
        else -> Color.Unspecified
    }

    OutlinedTextField(
        value = tagValue,
        onValueChange = onTagChange,
        label = { Text(label) },
        modifier = modifier,
        enabled = enabled,
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = color,
            unfocusedBorderColor = color,
            focusedLabelColor = color,
            unfocusedLabelColor = color
        ),
        trailingIcon = {
            if (tagValue.isNotEmpty() && enabled) {
                IconButton(
                    onClick = { onTagChange("") }
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Clear")
                }
            }
        },
        supportingText = {
            when {
                tagValue.isEmpty() -> {
                    // No tag, show nothing or a neutral message
                }
                isNewTag -> Text("🆕 New Tag", color = PrimaryGreen)
                foundVehicle == vehicleNumber && vehicleNumber.isNotEmpty() ->
                    Text("✅ Assigned to this vehicle", color = MaterialTheme.colorScheme.secondary)
                foundVehicle.isNotEmpty() && foundVehicle != vehicleNumber ->
                    Text("⚠️ Assigned to: $foundVehicle", color = MaterialTheme.colorScheme.error)
            }
        }
    )
}