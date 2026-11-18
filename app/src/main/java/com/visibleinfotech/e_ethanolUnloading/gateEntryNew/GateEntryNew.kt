package com.visibleinfotech.e_ethanolUnloading.gateEntryNew

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.visibleinfotech.e_ethanolUnloading.login.*
import com.visibleinfotech.e_ethanolUnloading.ui.theme.*
import com.visibleinfotech.e_ethanolUnloading.utils.DateTimeUtils
import com.visibleinfotech.e_ethanolUnloading.utils.SetStatusBarColor
import kotlinx.coroutines.delay
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.foundation.ExperimentalFoundationApi
import com.visibleinfotech.e_ethanolUnloading.utils.KeyboardUtils
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val TAG = "GateEntry"

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
@Composable
fun GateEntryNew(
    onBack: () -> Unit = {},
    onSuccess: () -> Unit = {},
    viewModel: GateEntryViewModel = viewModel(
        factory = GateEntryViewModelFactory(LocalContext.current)
    )
) {
    SetStatusBarColor(PrimaryBlue)
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val hideKeyboard = KeyboardUtils.hideKeyboard()
    var showConfirmationDialog by remember { mutableStateOf(false) }
    var currentDateTime by remember { mutableStateOf(DateTimeUtils.getCurrentDateTime()) }
    val coroutineScope = rememberCoroutineScope()
    val view = LocalView.current

    var showProgressDialog by remember { mutableStateOf(false) }
    var progressValue by remember { mutableStateOf(0) }
    var progressMessage by remember { mutableStateOf("") }
    var isUploadComplete by remember { mutableStateOf(false) }
    var uploadSuccess by remember { mutableStateOf(false) }
    var uploadErrorMessage by remember { mutableStateOf("") }

    val scrollState = rememberScrollState()

    val truckFocusRequester = remember { FocusRequester() }
    val driverNameFocusRequester = remember { FocusRequester() }
    val driverMobileFocusRequester = remember { FocusRequester() }
    val modeFocusRequester = remember { FocusRequester() }

    val isTruckSelected = uiState.selectedTruckNo.isNotBlank()
    val hasDispatchData = uiState.invoiceNo.isNotBlank() || uiState.quantity.isNotBlank() ||
            uiState.partyName.isNotBlank() || uiState.brokerName.isNotBlank()

    // Image upload states
    var showImagePickerOptions by remember { mutableStateOf(false) }
    var currentImageField by remember { mutableStateOf<String?>(null) }
    var currentPhotoUri by remember { mutableStateOf<Uri?>(null) }

    // Document URI states
    var doc1Uri by remember { mutableStateOf<Uri?>(null) } // Anugya
    var doc2Uri by remember { mutableStateOf<Uri?>(null) } // Bilty
    var doc3Uri by remember { mutableStateOf<Uri?>(null) } // Invoice
    var doc4Uri by remember { mutableStateOf<Uri?>(null) } // Weighment
    var doc5Uri by remember { mutableStateOf<Uri?>(null) } // Other

    // ========== FUNCTION TO CREATE IMAGE URI ==========
    fun createImageUri(context: Context): Uri? {
        return try {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val imageFileName = "GATE_${currentImageField?.uppercase()}_${timeStamp}"

            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, "$imageFileName.jpg")
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/GateEntryDocs")
                }
            }

            context.contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error creating image URI: ${e.message}")
            e.printStackTrace()
            null
        }
    }

    // ========== CAMERA LAUNCHER ==========
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        Log.d(TAG, "Camera result: success=$success, field=$currentImageField, uri=$currentPhotoUri")

        if (success && currentPhotoUri != null && currentImageField != null) {
            when (currentImageField) {
                "doc1" -> {
                    Log.d(TAG, "Updating Anugya image from camera")
                    doc1Uri = currentPhotoUri
                    viewModel.updateDoc1Uri(currentPhotoUri.toString())
                }
                "doc2" -> {
                    Log.d(TAG, "Updating Bilty image from camera")
                    doc2Uri = currentPhotoUri
                    viewModel.updateDoc2Uri(currentPhotoUri.toString())
                }
                "doc3" -> {
                    Log.d(TAG, "Updating Invoice image from camera")
                    doc3Uri = currentPhotoUri
                    viewModel.updateDoc3Uri(currentPhotoUri.toString())
                }
                "doc4" -> {
                    Log.d(TAG, "Updating Weighment image from camera")
                    doc4Uri = currentPhotoUri
                    viewModel.updateDoc4Uri(currentPhotoUri.toString())
                }
                "doc5" -> {
                    Log.d(TAG, "Updating Other document image from camera")
                    doc5Uri = currentPhotoUri
                    viewModel.updateDoc5Uri(currentPhotoUri.toString())
                }
                else -> Log.w(TAG, "Unknown image field: $currentImageField")
            }
        } else {
            Log.e(TAG, "Failed to capture image: success=$success, field=$currentImageField")
            if (success) {
            }
        }

        showImagePickerOptions = false
        currentImageField = null
        currentPhotoUri = null
        hideKeyboard()
    }

    // ========== CAMERA PERMISSION HANDLING ==========
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        if (isGranted) {
            Log.d(TAG, "Camera permission granted")
            currentPhotoUri?.let { uri ->
                cameraLauncher.launch(uri)
            }
        } else {
            Log.e(TAG, "Camera permission denied")
            Toast.makeText(
                context,
                "Camera permission is required to capture images",
                Toast.LENGTH_LONG
            ).show()
            showImagePickerOptions = false
            currentImageField = null
            currentPhotoUri = null
        }
    }

    // ========== GALLERY LAUNCHER ==========
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        Log.d(TAG, "Gallery result: uri=$uri, field=$currentImageField")

        if (uri != null && currentImageField != null) {
            when (currentImageField) {
                "doc1" -> {
                    Log.d(TAG, "Updating Anugya image from gallery")
                    doc1Uri = uri
                    viewModel.updateDoc1Uri(uri.toString())
                }
                "doc2" -> {
                    Log.d(TAG, "Updating Bilty image from gallery")
                    doc2Uri = uri
                    viewModel.updateDoc2Uri(uri.toString())
                }
                "doc3" -> {
                    Log.d(TAG, "Updating Invoice image from gallery")
                    doc3Uri = uri
                    viewModel.updateDoc3Uri(uri.toString())
                }
                "doc4" -> {
                    Log.d(TAG, "Updating Weighment image from gallery")
                    doc4Uri = uri
                    viewModel.updateDoc4Uri(uri.toString())
                }
                "doc5" -> {
                    Log.d(TAG, "Updating Other document image from gallery")
                    doc5Uri = uri
                    viewModel.updateDoc5Uri(uri.toString())
                }
                else -> Log.w(TAG, "Unknown image field: $currentImageField")
            }
        } else {
            Log.w(TAG, "Gallery returned null URI or currentImageField is null")
        }

        showImagePickerOptions = false
        currentImageField = null
        hideKeyboard()
    }

    // ========== FUNCTIONS ==========
    fun showImagePicker(field: String) {
        Log.d(TAG, "showImagePicker called for field: $field")
        currentImageField = field
        showImagePickerOptions = true
        hideKeyboard()
    }

    fun handleCameraClick() {
        val uri = createImageUri(context)
        if (uri == null) {
            Toast.makeText(context, "Failed to create image file", Toast.LENGTH_SHORT).show()
            return
        }
        currentPhotoUri = uri

        if (hasCameraPermission) {
            cameraLauncher.launch(uri)
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Update date/time every second
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            currentDateTime = DateTimeUtils.getCurrentDateTime()
            viewModel.updateDateTime(currentDateTime)
        }
    }

    BackHandler {
        focusManager.clearFocus()
        viewModel.resetForm()
        onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Gate Entry",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        focusManager.clearFocus()
                        viewModel.resetForm()
                        onBack()
                    }) {
                        Icon(
                            painter = painterResource(id = android.R.drawable.ic_menu_revert),
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    OutlinedButton(
                        onClick = {
                            Log.d(TAG, "Reset button clicked")
                            viewModel.resetForm()
                            // Reset local image URIs
                            doc1Uri = null
                            doc2Uri = null
                            doc3Uri = null
                            doc4Uri = null
                            doc5Uri = null
                            // Dismiss any open dialogs
                            showConfirmationDialog = false
                            showImagePickerOptions = false
                            currentImageField = null
                            currentPhotoUri = null
                            // Reset progress dialog states
                            showProgressDialog = false
                            progressValue = 0
                            progressMessage = ""
                            isUploadComplete = false
                            uploadSuccess = false
                            uploadErrorMessage = ""
                            focusManager.clearFocus()
                            hideKeyboard()
                            Toast.makeText(
                                context,
                                "Form reset successfully",
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .height(36.dp),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(
                            1.dp,
                            Color.White.copy(alpha = 0.5f)
                        ),
                        contentPadding = PaddingValues(horizontal = 10.dp)
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )

                        Spacer(modifier = Modifier.width(4.dp))

                        Text(
                            "Reset",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PrimaryBlue
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(LightBlue.copy(alpha = 0.1f))
                .padding(paddingValues)
                .clickable {
                    view.requestFocus()
                }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Spacer(modifier = Modifier.height(4.dp))

                // Date & Time Card
                DateTimeCard(
                    dateTime = currentDateTime,
                    textColor = PrimaryBlue,
                    backgroundColor = Color.White
                )

                FormSectionCard(title = "Gate Entry Details", PrimaryBlue) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        // Truck Number Dropdown
                        ModernSearchableDropdown(
                            searchText = uiState.truckSearchText,
                            onSearchTextChange = { text ->
                                viewModel.updateTruckSearchText(text)
                            },
                            onClearClick = {
                                viewModel.updateTruckSearchText("")
                                viewModel.clearSelectedTruck()
                                focusManager.clearFocus()
                            },
                            label = "Select Truck Number",
                            items = uiState.filteredTruckList,
                            getItemLabel = { truckNo -> truckNo },
                            getItemId = { truckNo -> truckNo },
                            onItemSelected = { truckNo ->
                                viewModel.selectTruck(truckNo)
                                focusManager.clearFocus()
                                if (uiState.selectedTruckNo.isNotBlank()) {
                                    driverNameFocusRequester.requestFocus()
                                }
                            },
                            onDropdownDismiss = {
                                focusManager.clearFocus()
                            },
                            focusRequester = truckFocusRequester,
                            accentColor = PrimaryBlue,
                            itemIcon = Icons.Default.LocalShipping,
                            itemIconColor = PrimaryBlue,
                            itemTextColor = Color.Black,
                            isError = uiState.showValidationErrors && uiState.selectedTruckNo.isBlank(),
                            errorMessage = if (uiState.showValidationErrors && uiState.selectedTruckNo.isBlank()) {
                                "Please select a truck number"
                            } else {
                                null
                            },
                            noResultsMessage = "No truck numbers found"
                        )

                        if (isTruckSelected) {
                            Text(
                                text = "Driver Details",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryBlue
                            )

                            HorizontalDivider(
                                color = PrimaryBlue.copy(alpha = 0.3f),
                                thickness = 1.dp
                            )

                            CompactInputField(
                                value = uiState.driverName,
                                onValueChange = {
                                    viewModel.updateDriverName(it.take(100))
                                },
                                hint = "Enter Driver Name *",
                                modifier = Modifier.fillMaxWidth(),
                                isError = uiState.showValidationErrors && uiState.driverName.isBlank(),
                                errorMessage = "Driver name is required",
                                icon = Icons.Default.Person,
                                accentColor = PrimaryBlue,
                                focusRequester = driverNameFocusRequester,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Text,
                                    imeAction = ImeAction.Next
                                )
                            )

                            CompactInputField(
                                value = uiState.driverMobile,
                                onValueChange = {
                                    viewModel.updateDriverMobile(
                                        it.filter { ch -> ch.isDigit() }.take(10)
                                    )
                                },
                                hint = "Enter Driver Mobile Number *",
                                modifier = Modifier.fillMaxWidth(),
                                isError = uiState.showValidationErrors && uiState.driverMobile.length != 10,
                                errorMessage = "Mobile number must be 10 digits",
                                icon = Icons.Default.Phone,
                                accentColor = PrimaryBlue,
                                focusRequester = driverMobileFocusRequester,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Phone,
                                    imeAction = ImeAction.Next
                                )
                            )

                             Text(
                                text = "Mode Selection",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = VibrantTeal,
                                modifier = Modifier.padding(top = 4.dp)
                            )

                            HorizontalDivider(
                                color = DarkVibrantTeal.copy(alpha = 0.3f),
                                thickness = 1.dp
                            )

                            ModernSearchableDropdown(
                                searchText = uiState.modeSearchText,
                                onSearchTextChange = { text ->
                                    viewModel.updateModeSearchText(text)
                                },
                                onClearClick = {
                                    viewModel.updateModeSearchText("")
                                    viewModel.clearSelectedMode()
                                    focusManager.clearFocus()
                                },
                                label = "Select Mode *",
                                items = uiState.filteredModeList,
                                getItemLabel = { mode -> "${mode.code} - ${mode.name}" },
                                getItemId = { mode -> mode.code },
                                onItemSelected = { mode ->
                                    viewModel.selectMode(mode)
                                    focusManager.clearFocus()
                                },
                                onDropdownDismiss = {
                                    focusManager.clearFocus()
                                },
                                focusRequester = modeFocusRequester,
                                accentColor = DarkVibrantTeal,
                                itemIcon = Icons.Default.Settings,
                                itemIconColor = DarkVibrantTeal,
                                itemTextColor = Color.Black,
                                isError = uiState.showValidationErrors && uiState.selectedModeCode.isBlank(),
                                errorMessage = if (uiState.showValidationErrors && uiState.selectedModeCode.isBlank()) {
                                    "Please select a mode"
                                } else {
                                    null
                                },
                                noResultsMessage = "No modes found"
                            )

                            Text(
                                text = "Dispatch Details",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryBlue,
                                modifier = Modifier.padding(top = 4.dp)
                            )

                            HorizontalDivider(
                                color = PrimaryBlue.copy(alpha = 0.3f),
                                thickness = 1.dp,
                                modifier = Modifier.padding(vertical = 2.dp)
                            )

                            if (hasDispatchData) {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = PrimaryBlue.copy(alpha = 0.05f)
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        DispatchReadOnlyRow(
                                            label = "Invoice No",
                                            value = uiState.invoiceNo,
                                            icon = Icons.Default.Receipt
                                        )
                                        DispatchReadOnlyRow(
                                            label = "Quantity",
                                            value = "${uiState.quantity} Quintal",
                                            icon = Icons.Default.ProductionQuantityLimits
                                        )
                                        DispatchReadOnlyRow(
                                            label = "Party Name",
                                            value = uiState.partyName,
                                            icon = Icons.Default.Business
                                        )
                                        DispatchReadOnlyRow(
                                            label = "Broker Name",
                                            value = uiState.brokerName,
                                            icon = Icons.Default.People
                                        )
                                    }
                                }
                            } else {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = PrimaryGreen.copy(alpha = 0.1f)
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Edit Mode",
                                            tint = PrimaryGreen,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "Enter new dispatch details",
                                            fontSize = 11.sp,
                                            color = PrimaryGreen,
                                            modifier = Modifier.padding(start = 4.dp)
                                        )
                                    }
                                }

                                CompactInputField(
                                    value = uiState.invoiceNo,
                                    onValueChange = {
                                        viewModel.updateInvoiceNo(it.uppercase().take(50))
                                    },
                                    hint = "Enter Invoice Number *",
                                    modifier = Modifier.fillMaxWidth(),
                                    isError = uiState.showValidationErrors && uiState.invoiceNo.isBlank(),
                                    errorMessage = "Invoice number is required",
                                    icon = Icons.Default.Receipt,
                                    accentColor = PrimaryBlue,
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Text,
                                        imeAction = ImeAction.Next
                                    )
                                )

                                CompactInputField(
                                    value = uiState.quantity,
                                    onValueChange = {
                                        viewModel.updateQuantity(
                                            it.filter { ch -> ch.isDigit() || ch == '.' }.take(15)
                                        )
                                    },
                                    hint = "Enter Quantity *",
                                    modifier = Modifier.fillMaxWidth(),
                                    isError = uiState.showValidationErrors && uiState.quantity.isBlank(),
                                    errorMessage = "Quantity is required",
                                    icon = Icons.Default.ProductionQuantityLimits,
                                    accentColor = PrimaryBlue,
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Number,
                                        imeAction = ImeAction.Next
                                    )
                                )

                                CompactInputField(
                                    value = uiState.partyName,
                                    onValueChange = {
                                        viewModel.updatePartyName(it.uppercase().take(100))
                                    },
                                    hint = "Enter Party Name *",
                                    modifier = Modifier.fillMaxWidth(),
                                    isError = uiState.showValidationErrors && uiState.partyName.isBlank(),
                                    errorMessage = "Party name is required",
                                    icon = Icons.Default.Business,
                                    accentColor = PrimaryBlue,
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Text,
                                        imeAction = ImeAction.Next
                                    )
                                )

                                CompactInputField(
                                    value = uiState.brokerName,
                                    onValueChange = {
                                        viewModel.updateBrokerName(it.uppercase().take(100))
                                    },
                                    hint = "Enter Broker Name",
                                    modifier = Modifier.fillMaxWidth(),
                                    isError = false,
                                    errorMessage = null,
                                    icon = Icons.Default.People,
                                    accentColor = PrimaryBlue,
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Text,
                                        imeAction = ImeAction.Next
                                    )
                                )
                            }

                            // Invoice Details Section
                            Text(
                                text = "Invoice Details",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = DarkOrange,
                                modifier = Modifier.padding(top = 4.dp)
                            )

                            HorizontalDivider(
                                color = DarkOrange.copy(alpha = 0.3f),
                                thickness = 1.dp,
                                modifier = Modifier.padding(vertical = 2.dp)
                            )

                            CompactInputField(
                                value = uiState.invoiceWeight,
                                onValueChange = {
                                    viewModel.updateInvoiceWeight(
                                        it.filter { ch -> ch.isDigit() || ch == '.' }.take(15)
                                    )
                                },
                                hint = "Enter Invoice Weight *",
                                modifier = Modifier.fillMaxWidth(),
                                isError = uiState.showValidationErrors && uiState.invoiceWeight.isBlank(),
                                errorMessage = "Invoice Weight is required",
                                icon = Icons.Default.Scale,
                                accentColor = DarkOrange,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Number,
                                    imeAction = ImeAction.Next
                                )
                            )

                            CompactInputField(
                                value = uiState.numberOfBags,
                                onValueChange = {
                                    viewModel.updateNumberOfBags(
                                        it.filter { ch -> ch.isDigit() }.take(10)
                                    )
                                },
                                hint = "Enter Number of Bags *",
                                modifier = Modifier.fillMaxWidth(),
                                isError = uiState.showValidationErrors && uiState.numberOfBags.isBlank(),
                                errorMessage = "Number of Bags is required",
                                icon = Icons.Default.Inventory,
                                accentColor = DarkOrange,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Number,
                                    imeAction = ImeAction.Next
                                )
                            )

                            CompactInputField(
                                value = uiState.invoiceRate,
                                onValueChange = {
                                    viewModel.updateInvoiceRate(
                                        it.filter { ch -> ch.isDigit() || ch == '.' }.take(15)
                                    )
                                },
                                hint = "Enter Invoice Rate *",
                                modifier = Modifier.fillMaxWidth(),
                                isError = uiState.showValidationErrors && uiState.invoiceRate.isBlank(),
                                errorMessage = "Invoice Rate is required",
                                icon = Icons.Default.MonetizationOn,
                                accentColor = DarkOrange,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Number,
                                    imeAction = ImeAction.Next
                                )
                            )

                            CompactInputField(
                                value = uiState.invoiceAmount,
                                onValueChange = {
                                    viewModel.updateInvoiceAmount(
                                        it.filter { ch -> ch.isDigit() || ch == '.' }.take(15)
                                    )
                                },
                                hint = "Enter Invoice Amount *",
                                modifier = Modifier.fillMaxWidth(),
                                isError = uiState.showValidationErrors && uiState.invoiceAmount.isBlank(),
                                errorMessage = "Invoice Amount is required",
                                icon = Icons.Default.CurrencyRupee,
                                accentColor = DarkOrange,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Number,
                                    imeAction = ImeAction.Done
                                )
                            )

                            FormSectionCard(title = "Documents List") {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    val imageBaseUrl = uiState.imageApiUrl

                                    DocumentClickableItem(
                                        label = "Anugya",
                                        imageUri = doc1Uri,
                                        imageName = uiState.doc1Name,
                                        isAvailable = uiState.doc1Available,
                                        isUserUploaded = uiState.doc1UserUploaded,
                                        imageBaseUrl = imageBaseUrl,
                                        onUploadClick = {
                                            showImagePicker("doc1")
                                            hideKeyboard()
                                        },
                                        onViewImage = { url ->
                                            viewModel.setFullScreenImageUrl(url)
                                        },
                                        onRemoveImage = {
                                            doc1Uri = null
                                            viewModel.updateDoc1Uri("")
                                            Toast.makeText(context, "Anugya image removed", Toast.LENGTH_SHORT).show()
                                        }
                                    )

                                    if (uiState.showValidationErrors && !uiState.doc1Available && !uiState.doc1UserUploaded) {
                                        Text(
                                            text = "⚠️ Anugya image is required",
                                            fontSize = 11.sp,
                                            color = Color.Red,
                                            modifier = Modifier.padding(start = 8.dp, top = 2.dp)
                                        )
                                    }

                                    DocumentClickableItem(
                                        label = "Bilty",
                                        imageUri = doc2Uri,
                                        imageName = uiState.doc2Name,
                                        isAvailable = uiState.doc2Available,
                                        isUserUploaded = uiState.doc2UserUploaded,
                                        imageBaseUrl = imageBaseUrl,
                                        onUploadClick = {
                                            showImagePicker("doc2")
                                            hideKeyboard()
                                        },
                                        onViewImage = { url ->
                                            viewModel.setFullScreenImageUrl(url)
                                        },
                                        onRemoveImage = {
                                            doc2Uri = null
                                            viewModel.updateDoc2Uri("")
                                            Toast.makeText(context, "Bilty image removed", Toast.LENGTH_SHORT).show()
                                        }
                                    )

                                    if (uiState.showValidationErrors && !uiState.doc2Available && !uiState.doc2UserUploaded) {
                                        Text(
                                            text = "⚠️ Bilty image is required",
                                            fontSize = 11.sp,
                                            color = Color.Red,
                                            modifier = Modifier.padding(start = 8.dp, top = 2.dp)
                                        )
                                    }

                                    DocumentClickableItem(
                                        label = "Invoice",
                                        imageUri = doc3Uri,
                                        imageName = uiState.doc3Name,
                                        isAvailable = uiState.doc3Available,
                                        isUserUploaded = uiState.doc3UserUploaded,
                                        imageBaseUrl = imageBaseUrl,
                                        onUploadClick = {
                                            showImagePicker("doc3")
                                            hideKeyboard()
                                        },
                                        onViewImage = { url ->
                                            viewModel.setFullScreenImageUrl(url)
                                        },
                                        onRemoveImage = {
                                            doc3Uri = null
                                            viewModel.updateDoc3Uri("")
                                            Toast.makeText(context, "Invoice image removed", Toast.LENGTH_SHORT).show()
                                        }
                                    )

                                    if (uiState.showValidationErrors && !uiState.doc3Available && !uiState.doc3UserUploaded) {
                                        Text(
                                            text = "⚠️ Invoice image is required",
                                            fontSize = 11.sp,
                                            color = Color.Red,
                                            modifier = Modifier.padding(start = 8.dp, top = 2.dp)
                                        )
                                    }

                                    DocumentClickableItem(
                                        label = "Weighment",
                                        imageUri = doc4Uri,
                                        imageName = uiState.doc4Name,
                                        isAvailable = uiState.doc4Available,
                                        isUserUploaded = uiState.doc4UserUploaded,
                                        imageBaseUrl = imageBaseUrl,
                                        onUploadClick = {
                                            showImagePicker("doc4")
                                            hideKeyboard()
                                        },
                                        onViewImage = { url ->
                                            viewModel.setFullScreenImageUrl(url)
                                        },
                                        onRemoveImage = {
                                            doc4Uri = null
                                            viewModel.updateDoc4Uri("")
                                            Toast.makeText(context, "Weighment image removed", Toast.LENGTH_SHORT).show()
                                        }
                                    )

                                    if (uiState.showValidationErrors && !uiState.doc4Available && !uiState.doc4UserUploaded) {
                                        Text(
                                            text = "⚠️ Weighment image is required",
                                            fontSize = 11.sp,
                                            color = Color.Red,
                                            modifier = Modifier.padding(start = 8.dp, top = 2.dp)
                                        )
                                    }

                                    DocumentClickableItem(
                                        label = "Other (Optional)",
                                        imageUri = doc5Uri,
                                        imageName = uiState.doc5Name,
                                        isAvailable = uiState.doc5Available,
                                        isUserUploaded = uiState.doc5UserUploaded,
                                        imageBaseUrl = imageBaseUrl,
                                        onUploadClick = {
                                            showImagePicker("doc5")
                                            hideKeyboard()
                                        },
                                        onViewImage = { url ->
                                            viewModel.setFullScreenImageUrl(url)
                                        },
                                        onRemoveImage = {
                                            doc5Uri = null
                                            viewModel.updateDoc5Uri("")
                                            Toast.makeText(context, "Other document image removed", Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                }
                            }

                            // Additional Details Section
                            FormSectionCard(title = "Additional Details (Optional)") {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    OutlinedTextField(
                                        value = uiState.remarks,
                                        onValueChange = {
                                            viewModel.updateRemarks(it.uppercase().take(200))
                                        },
                                        label = { Text("Remarks") },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(18.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = PrimaryBlue,
                                            unfocusedBorderColor = PrimaryBlue.copy(alpha = 0.4f)
                                        ),
                                        maxLines = 3,
                                        keyboardOptions = KeyboardOptions(
                                            keyboardType = KeyboardType.Text,
                                            imeAction = ImeAction.Done
                                        )
                                    )
                                }
                            }

                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Text(
                                    text = "Please select a truck number to continue",
                                    color = Color.Gray,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.align(Alignment.Center)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                if (isTruckSelected) {
                    PrimarySubmitButton(
                        onClick = {
                            if (!uiState.isSubmitting) {
                                focusManager.clearFocus()

                                if (!viewModel.isFormValid()) {
                                    viewModel.setShowValidationErrors(true)
                                    Toast.makeText(
                                        context,
                                        "Please fill all required fields and upload required documents",
                                        Toast.LENGTH_LONG
                                    ).show()
                                    return@PrimarySubmitButton
                                }

                                showConfirmationDialog = true
                            }
                        },
                        text = "Update Gate Entry",
                        gradientColors = listOf(PrimaryBlue, DarkBlue),
                        shadowColor = PrimaryBlue,
                        isLoading = uiState.isSubmitting
                    )
                }

                Spacer(modifier = Modifier.height(300.dp))
            }
        }
    }

    // Image Picker Bottom Sheet
    if (showImagePickerOptions) {
        val sheetState = rememberModalBottomSheetState(
            skipPartiallyExpanded = true
        )
        val coroutineScope = rememberCoroutineScope()

        ModalBottomSheet(
            onDismissRequest = {
                showImagePickerOptions = false
                currentImageField = null
                hideKeyboard()
            },
            sheetState = sheetState,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            containerColor = Color.White
        ) {
            ImagePickerBottomSheetContent(
                title = currentImageField?.let { field ->
                    when (field) {
                        "doc1" -> "Anugya"
                        "doc2" -> "Bilty"
                        "doc3" -> "Invoice"
                        "doc4" -> "Weighment"
                        "doc5" -> "Other Document"
                        else -> "Document"
                    }
                } ?: "Document",
                onGalleryClick = {
                    galleryLauncher.launch("image/*")
                    coroutineScope.launch {
                        sheetState.hide()
                        showImagePickerOptions = false
                    }
                },
                onCameraClick = {
                    handleCameraClick()
                    coroutineScope.launch {
                        sheetState.hide()
                        showImagePickerOptions = false
                    }
                },
                onCloseClick = {
                    coroutineScope.launch {
                        sheetState.hide()
                        showImagePickerOptions = false
                        currentImageField = null
                        hideKeyboard()
                    }
                }
            )
        }
    }

    // Full Screen Image Viewer
    if (uiState.fullScreenImageUrl != null) {
        FullScreenImageViewer(
            imageUrl = uiState.fullScreenImageUrl!!,
            onDismiss = {
                viewModel.setFullScreenImageUrl(null)
            }
        )
    }

    // Confirmation Dialog
    if (showConfirmationDialog) {
        GateEntryConfirmationDialog(
            truckNo = uiState.selectedTruckNo,
            driverName = uiState.driverName,
            driverMobile = uiState.driverMobile,
            modeCode = uiState.selectedModeCode,
            modeName = uiState.selectedModeName,
            invoiceNo = uiState.invoiceNo,
            quantity = uiState.quantity,
            partyName = uiState.partyName,
            brokerName = uiState.brokerName,
            invoiceWeight = uiState.invoiceWeight,
            numberOfBags = uiState.numberOfBags,
            invoiceRate = uiState.invoiceRate,
            invoiceAmount = uiState.invoiceAmount,
            hasDoc1 = doc1Uri != null || uiState.doc1Available,
            hasDoc2 = doc2Uri != null || uiState.doc2Available,
            hasDoc3 = doc3Uri != null || uiState.doc3Available,
            hasDoc4 = doc4Uri != null || uiState.doc4Available,
            hasDoc5 = doc5Uri != null || uiState.doc5Available,
            remarks = uiState.remarks,
            onConfirm = {
                showConfirmationDialog = false

                // Show progress dialog
                showProgressDialog = true
                progressValue = 0
                progressMessage = "Starting update..."
                isUploadComplete = false
                uploadSuccess = false
                uploadErrorMessage = ""

                // Call the combined method
                viewModel.updateGateEntryWithIndentAndToken(
                    onProgress = { progress, status ->
                        progressValue = progress
                        progressMessage = status
                        Log.d(TAG, "Update progress: $progress% - $status")
                    },
                    onSuccess = { message ->
                        progressValue = 100
                        progressMessage = "✅ $message"
                        isUploadComplete = true
                        uploadSuccess = true

                        Log.d(TAG, "✅ Success: $message")

                        coroutineScope.launch {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(
                                    context,
                                    message,
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    },
                    onError = { error ->
                        progressValue = 0
                        progressMessage = "❌ $error"
                        isUploadComplete = true
                        uploadSuccess = false
                        uploadErrorMessage = error

                        Log.e(TAG, "❌ Error: $error")
                    }
                )
            },
            onDismiss = {
                showConfirmationDialog = false
            }
        )
    }

    // Progress Dialog
    if (showProgressDialog) {
        AlertDialog(
            onDismissRequest = {
                // Only allow dismiss if completed
                if (isUploadComplete) {
                    showProgressDialog = false
                }
            },
            title = {
                Text(
                    text = if (isUploadComplete && uploadSuccess) "✅ Update Complete" else "Updating Gate Entry...",
                    fontWeight = FontWeight.Bold,
                    color = if (isUploadComplete && uploadSuccess) PrimaryGreen else PrimaryBlue
                )
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Progress indicator
                    if (!isUploadComplete) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(48.dp),
                            color = PrimaryBlue,
                            strokeWidth = 4.dp
                        )
                    } else if (uploadSuccess) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Success",
                            tint = PrimaryGreen,
                            modifier = Modifier.size(48.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = "Error",
                            tint = Color.Red,
                            modifier = Modifier.size(48.dp)
                        )
                    }

                    // Progress message
                    Text(
                        text = progressMessage,
                        fontSize = 14.sp,
                        color = if (isUploadComplete && !uploadSuccess) Color.Red else Color.Black,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )

                    // Progress bar
                    if (!isUploadComplete) {
                        LinearProgressIndicator(
                            progress = { progressValue / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp),
                            color = PrimaryBlue,
                            trackColor = PrimaryBlue.copy(alpha = 0.2f)
                        )

                        // Progress percentage
                        Text(
                            text = "$progressValue%",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryBlue
                        )
                    }

                    // Show error details if failed
                    if (isUploadComplete && !uploadSuccess && uploadErrorMessage.isNotEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = Color.Red.copy(alpha = 0.1f)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = uploadErrorMessage,
                                fontSize = 12.sp,
                                color = Color.Red,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                if (isUploadComplete) {
                    Button(
                        onClick = {
                            showProgressDialog = false
                            if (uploadSuccess) {
                                onSuccess()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (uploadSuccess) PrimaryGreen else Color.Red
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = if (uploadSuccess) "OK" else "Close",
                            color = Color.White
                        )
                    }
                }
            },
            dismissButton = {
                if (isUploadComplete && !uploadSuccess) {
                    TextButton(
                        onClick = {
                            showProgressDialog = false
                        }
                    ) {
                        Text("Dismiss")
                    }
                }
            },
            shape = RoundedCornerShape(16.dp),
            containerColor = Color.White,
            tonalElevation = 8.dp
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
