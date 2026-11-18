package com.visibleinfotech.e_ethanolUnloading.dispatch

import android.Manifest
import android.R
import android.app.Activity
import android.app.DatePickerDialog
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.google.zxing.integration.android.IntentIntegrator
import com.visibleinfotech.e_ethanolUnloading.QRScannerActivity
import com.visibleinfotech.e_ethanolUnloading.components.*
import com.visibleinfotech.e_ethanolUnloading.dispatch.models.Party
import com.visibleinfotech.e_ethanolUnloading.login.CompactInputField
import com.visibleinfotech.e_ethanolUnloading.login.CompactInputFieldNew
import com.visibleinfotech.e_ethanolUnloading.login.DateTimeCard
import com.visibleinfotech.e_ethanolUnloading.login.FormSectionCard
import com.visibleinfotech.e_ethanolUnloading.login.ModernSearchableDropdown
import com.visibleinfotech.e_ethanolUnloading.login.PrimarySubmitButton
import com.visibleinfotech.e_ethanolUnloading.ui.theme.*
import com.visibleinfotech.e_ethanolUnloading.utils.DateTimeUtils
import com.visibleinfotech.e_ethanolUnloading.utils.InputFilters
import com.visibleinfotech.e_ethanolUnloading.utils.KeyboardUtils
import com.visibleinfotech.e_ethanolUnloading.utils.SetStatusBarColor
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.runtime.DisposableEffect

private const val TAG = "DispatchFormLog"
private const val TAG_SCAN = "AnugyaScan"
private const val TAG_UI = "DispatchUI"
private const val TAG_VALIDATION = "DispatchValidationLog"
private const val TAG_IMG = "DispatchImage"

private fun parseAnugyaQr(raw: String): Map<String, String> {
    Log.d(TAG_SCAN, "🔍 parseAnugyaQr START | raw=[$raw]")
    val result = mutableMapOf<String, String>()

    raw.split(",").forEach { segment ->
        val trimmed = segment.trim()
        val dashIndex = trimmed.indexOf('-')
        if (dashIndex > 0) {
            val key = trimmed.substring(0, dashIndex).trim()
            var value = trimmed.substring(dashIndex + 1).trim()
            value = value.trimEnd('-').trim()
            result[key] = value
            Log.d(TAG_SCAN, "   ➜ parsed key=[$key] value=[$value]")
        } else {
            Log.w(TAG_SCAN, "   ⚠ Skipped malformed segment: [$trimmed]")
        }
    }
    Log.d(TAG_SCAN, "🔍 parseAnugyaQr END | ${result.size} pairs")
    return result
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DispatchForm(
    onBack: () -> Unit = {},
) {
    Log.d(TAG, "========== DispatchForm INITIALIZED ==========")
    Log.d(TAG_UI, "⏱  Init time: ${System.currentTimeMillis()}")

    SetStatusBarColor(IndentTeal)
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    val hideKeyboard = KeyboardUtils.hideKeyboard()

    val repository = remember { DispatchRepository(context) }
    val viewModel: DispatchViewModel = viewModel(
        factory = DispatchViewModelFactory(repository)
    )

    // ===== Party =====
    val parties by viewModel.parties.observeAsState(emptyList())
    val isLoadingParties by viewModel.isLoadingParties.observeAsState(false)
    val partyError by viewModel.partiesError.observeAsState()
    val selectedParty by viewModel.selectedParty.observeAsState()

    // ===== Form =====
    val vehicleNumber by viewModel.vehicleNumber.observeAsState("")
    val driverName by viewModel.driverName.observeAsState("")
    val driverMobileNumber by viewModel.driverMobileNumber.observeAsState("")
    val invoiceNumber by viewModel.invoiceNumber.observeAsState("")
    val invoiceDate by viewModel.invoiceDate.observeAsState("")
    val dispatchQuantity by viewModel.dispatchQuantity.observeAsState("")

    // ===== Images =====
    val anugyaImageUri by viewModel.anugyaImageUri.observeAsState()
    val biltyImageUri by viewModel.biltyImageUri.observeAsState()
    val invoiceImageUri by viewModel.invoiceImageUri.observeAsState()
    val weighmentSlipImageUri by viewModel.weighmentSlipImageUri.observeAsState()
    val otherDocumentImageUri by viewModel.otherDocumentImageUri.observeAsState()

    val isSubmitting by viewModel.isSubmitting.observeAsState(false)
    val showValidationErrors by viewModel.showValidationErrors.observeAsState(false)
    val availableQuantity by viewModel.availableQuantity.observeAsState()
    val isLoadingAllocation by viewModel.isLoadingAllocation.observeAsState(false)
    val weightLimit by viewModel.weightLimit.observeAsState(50.0)
    val showWeightLimitDialog by viewModel.showWeightLimitDialog.observeAsState(false)
    val weightLimitError by viewModel.weightLimitError.observeAsState()
    val isCheckingTruck by viewModel.isCheckingTruck.observeAsState(false)
    val showTruckUsedDialog by viewModel.showTruckUsedDialog.observeAsState(false)
    val truckUsedNumber by viewModel.truckUsedNumber.observeAsState("")
    val showBrokerNotLinkedDialog by viewModel.showBrokerNotLinkedDialog.observeAsState(false)
    val brokerNotLinkedPartyName by viewModel.brokerNotLinkedPartyName.observeAsState("")
    val showNoSaudaDialog by viewModel.showNoSaudaDialog.observeAsState(false)
    val noSaudaPartyName by viewModel.noSaudaPartyName.observeAsState("")

    // ===== NEW DIALOG OBSERVERS =====
    val showInsufficientQtyDialog by viewModel.showInsufficientQtyDialog.observeAsState(false)
    val showQtyExceedsApprovedDialog by viewModel.showQtyExceedsApprovedDialog.observeAsState(false)
    val showSaudaPendingApprovalDialog by viewModel.showSaudaPendingApprovalDialog.observeAsState(false)
    val showQtyExceedsUnapprovedSaudaDialog by viewModel.showQtyExceedsUnapprovedSaudaDialog.observeAsState(false)
    val aqM1Value by viewModel.aqM1Value.observeAsState(0.0)
    val aqM2Value by viewModel.aqM2Value.observeAsState(0.0)
    val minQtyValue by viewModel.minQtyValue.observeAsState(0.0)
    val brokers by viewModel.brokers.observeAsState(emptyList())
    val isLoadingBrokers by viewModel.isLoadingBrokers.observeAsState(false)
    val selectedBroker by viewModel.selectedBroker.observeAsState()
    val showNoBrokerDialog by viewModel.showNoBrokerDialog.observeAsState(false)
    val noBrokerPartyName by viewModel.noBrokerPartyName.observeAsState("")

    var brokerSearchText by remember { mutableStateOf("") }
    val brokerFocusRequester = remember { FocusRequester() }
    var currentDateTime by remember { mutableStateOf(DateTimeUtils.getCurrentDateTime()) }
    var partySearchText by remember { mutableStateOf("") }
    val anugyaNumber by viewModel.anugyaNumber.observeAsState("")

    // Focus requesters
    val partyFocusRequester = remember { FocusRequester() }
    val invoiceFocusRequester = remember { FocusRequester() }
    val quantityFocusRequester = remember { FocusRequester() }
    val vehicleFocusRequester = remember { FocusRequester() }

    // Dialog state
    var showConfirmationDialog by remember { mutableStateOf(false) }
    var submissionState by remember { mutableStateOf(SubmissionState()) }

    var showInvoiceDatePicker by remember { mutableStateOf(false) }

    var showImagePickerOptions by remember { mutableStateOf(false) }
    var currentImageField by remember { mutableStateOf<String?>(null) }
    var currentPhotoUri by remember { mutableStateOf<Uri?>(null) }

    var anugyaName by remember { mutableStateOf("") }
    var anugyaMobile by remember { mutableStateOf("") }
    var anugyaVillage by remember { mutableStateOf("") }
    var isAnugyaScanned by remember { mutableStateOf(false) }

    fun createImageUri(ctx: Context): Uri? {
        return try {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val imageFileName = "UNLOAD_${currentImageField?.uppercase()}_$timeStamp"
            Log.d(TAG_IMG, "📸 Creating image URI: $imageFileName")

            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, "$imageFileName.jpg")
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(
                        MediaStore.MediaColumns.RELATIVE_PATH,
                        "${Environment.DIRECTORY_PICTURES}/UnloadingDocs"
                    )
                }
            }

            val uri = ctx.contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
            )
            Log.d(TAG_IMG, "📸 Image URI created: ${uri?.toString() ?: "null"}")
            uri
        } catch (e: Exception) {
            Log.e(TAG_IMG, "❌ createImageUri error: ${e.message}", e)
            null
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        Log.d(TAG_IMG, "📸 Camera result: success=$success, field=$currentImageField")

        if (success && currentPhotoUri != null && currentImageField != null) {
            when (currentImageField) {
                "anugya" -> viewModel.updateAnugyaImage(currentPhotoUri)
                "bilty" -> viewModel.updateBiltyImage(currentPhotoUri)
                "invoice" -> viewModel.updateInvoiceImage(currentPhotoUri)
                "weighment" -> viewModel.updateWeighmentSlipImage(currentPhotoUri)
                "other" -> viewModel.updateOtherDocumentImage(currentPhotoUri)
            }
        } else {
            Toast.makeText(context, "Failed to capture image", Toast.LENGTH_SHORT).show()
        }
        showImagePickerOptions = false
        currentImageField = null
        currentPhotoUri = null
        hideKeyboard()
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Log.d(TAG_IMG, "✅ Camera permission granted")
            val uri = createImageUri(context)
            currentPhotoUri = uri
            uri?.let { cameraLauncher.launch(it) } ?: run {
                Toast.makeText(context, "Failed to create image file", Toast.LENGTH_SHORT).show()
                currentImageField = null
            }
        } else {
            Log.w(TAG_IMG, "❌ Camera permission denied")
            Toast.makeText(context, "Camera permission required", Toast.LENGTH_LONG).show()
            currentImageField = null
        }
        hideKeyboard()
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        Log.d(
            TAG_IMG,
            "📸 Gallery result: uri=${uri?.toString() ?: "null"}, field=$currentImageField"
        )
        uri?.let {
            when (currentImageField) {
                "anugya" -> viewModel.updateAnugyaImage(it)
                "bilty" -> viewModel.updateBiltyImage(it)
                "invoice" -> viewModel.updateInvoiceImage(it)
                "weighment" -> viewModel.updateWeighmentSlipImage(it)
                "other" -> viewModel.updateOtherDocumentImage(it)
            }
        }
        showImagePickerOptions = false
        currentImageField = null
        hideKeyboard()
    }

    fun showImagePicker(field: String) {
        Log.d(TAG_IMG, "📸 showImagePicker for field: $field")
        currentImageField = field
        showImagePickerOptions = true
        hideKeyboard()
    }

    val anugyaScannerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        Log.d(TAG_SCAN, "📷 QR result | code=${result.resultCode}")
        val parsed = IntentIntegrator.parseActivityResult(result.resultCode, result.data)

        if (parsed != null && parsed.contents != null) {
            val qrContent = parsed.contents
            Log.d(TAG_SCAN, "📷 QR Content: $qrContent")

            isAnugyaScanned = true
            val p = parseAnugyaQr(qrContent)

            val qrAnugyaId = p["AnugyaId"] ?: ""
            val qrSrcMandi = p["S.Mandi"] ?: ""
            val qrWeight = p["Total Weight(In Qty.)"] ?: ""
            val qrDriver = p["Driver"] ?: ""
            val qrVehicleNo = p["Vehicle No"] ?: ""

            viewModel.updateAnugyaNumber(qrAnugyaId)
            anugyaName = qrDriver
            anugyaVillage = qrSrcMandi

            if (qrVehicleNo.isNotBlank()) {
                viewModel.updateVehicleNumber(InputFilters.vehicleNumber(qrVehicleNo))
            }
            if (qrDriver.isNotBlank()) {
                viewModel.updateDriverName(InputFilters.name(qrDriver).uppercase())
            }
            if (qrWeight.isNotBlank()) {
                viewModel.updateDispatchQuantity(InputFilters.decimal(qrWeight))
            }

            Toast.makeText(context, "Anugya QR loaded!", Toast.LENGTH_SHORT).show()
        } else {
            Log.w(TAG_SCAN, "❌ QR cancelled or empty")
        }
    }

    val anugyaBarcodeScannerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        Log.d(TAG_SCAN, "📷 Barcode result | code=${result.resultCode}")
        val parsed = IntentIntegrator.parseActivityResult(result.resultCode, result.data)

        if (parsed != null && parsed.contents != null) {
            val bc = parsed.contents
            Log.d(TAG_SCAN, "📷 Barcode Content: $bc")

            isAnugyaScanned = true
            val p = parseAnugyaQr(bc)

            val qrAnugyaId = p["AnugyaId"] ?: bc
            val qrSrcMandi = p["S.Mandi"] ?: ""
            val qrWeight = p["Total Weight(In Qty.)"] ?: ""
            val qrDriver = p["Driver"] ?: ""
            val qrVehicleNo = p["Vehicle No"] ?: ""

            viewModel.updateAnugyaNumber(qrAnugyaId)
            anugyaName = qrDriver
            anugyaVillage = qrSrcMandi

            if (qrVehicleNo.isNotBlank()) {
                viewModel.updateVehicleNumber(InputFilters.vehicleNumber(qrVehicleNo))
            }
            if (qrDriver.isNotBlank()) {
                viewModel.updateDriverName(InputFilters.name(qrDriver).uppercase())
            }
            if (qrWeight.isNotBlank()) {
                viewModel.updateDispatchQuantity(InputFilters.decimal(qrWeight))
            }

            Toast.makeText(context, "Anugya Barcode loaded!", Toast.LENGTH_SHORT).show()
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                Log.d(TAG, "🔁 ON_RESUME → refreshing party list + weight limit + brokers")
                viewModel.loadWeightLimit()
                viewModel.loadAllParties()
                viewModel.refreshBrokersForSelectedParty()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            currentDateTime = DateTimeUtils.getCurrentDateTime()
        }
    }

    LaunchedEffect(submissionState) {
        if (submissionState.isSuccess || submissionState.isError) {
            if (submissionState.isSuccess) {
                viewModel.resetForm()
                partySearchText = ""
                anugyaName = ""
                anugyaMobile = ""
                anugyaVillage = ""
                isAnugyaScanned = false
            }
        }
    }

    val filteredParties = remember(partySearchText, parties) {
        if (partySearchText.isBlank()) parties
        else parties.filter {
            it.accountName.contains(partySearchText, ignoreCase = true) ||
                    it.accountId.contains(partySearchText, ignoreCase = true)
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Dispatch Form", color = Color.White, fontWeight = FontWeight.Bold)
                },
                navigationIcon = {
                    IconButton(onClick = {
                        hideKeyboard()
                        onBack()
                    }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_menu_revert),
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    OutlinedButton(
                        onClick = {
                            viewModel.resetAndReload()
                            partySearchText = ""
                            showConfirmationDialog = false
                            submissionState = SubmissionState()
                            anugyaName = ""
                            anugyaMobile = ""
                            anugyaVillage = ""
                            isAnugyaScanned = false
                            focusManager.clearFocus()
                            hideKeyboard()
                        },
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .height(36.dp),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.5f)),
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
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                modifier = Modifier.background(
                    Brush.horizontalGradient(colors = listOf(IndentTeal, IndentCoralDark))
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(IndentLightTeal.copy(alpha = 0.3f))
                .padding(paddingValues)
                .pointerInput(Unit) { detectTapGestures(onTap = { hideKeyboard() }) }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                DateTimeCard(
                    dateTime = currentDateTime,
                    textColor = IndentTeal,
                    backgroundColor = WhiteColor
                )

                FormSectionCard(title = "Party Details", accentColor = Color.Black) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {

                        if (isLoadingParties) {
                            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = IndentTeal)
                            }
                        }

                        partyError?.let { error ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer
                                )
                            ) {
                                Text(
                                    text = error,
                                    modifier = Modifier.padding(16.dp),
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }

                        ModernSearchableDropdown(
                            searchText = partySearchText,
                            onSearchTextChange = { partySearchText = it },
                            onClearClick = {
                                partySearchText = ""
                                brokerSearchText = ""
                                viewModel.setSelectedParty(null)
                                hideKeyboard()
                            },
                            label = "Party Name",
                            items = filteredParties,
                            getItemLabel = { party -> "${party.accountId} - ${party.accountName}" },
                            getItemId = { it.accountId },
                            onItemSelected = { party ->
                                viewModel.setSelectedParty(party)
                                partySearchText = party.accountName
                                brokerSearchText = ""
                                hideKeyboard()
                            },
                            onDropdownDismiss = { hideKeyboard() },
                            accentColor = IndentTeal,
                            itemIcon = Icons.Default.Business,
                            itemIconColor = IndentTeal,
                            itemTextColor = Color.Black,
                            isError = showValidationErrors && selectedParty == null,
                            errorMessage = if (showValidationErrors && selectedParty == null)
                                "Please select a party" else null,
                            noResultsMessage = "No party found"
                        )

                        if (selectedParty != null) {

                            if (isLoadingBrokers) {
                                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(
                                        color = IndentTeal,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }

                            val filteredBrokers = remember(brokerSearchText, brokers) {
                                if (brokerSearchText.isBlank()) brokers
                                else brokers.filter {
                                    it.brokerName.contains(brokerSearchText, ignoreCase = true) ||
                                            it.brokerCode.contains(brokerSearchText, ignoreCase = true)
                                }
                            }

                            ModernSearchableDropdown(
                                searchText = brokerSearchText,
                                onSearchTextChange = { brokerSearchText = it },
                                onClearClick = {
                                    brokerSearchText = ""
                                    viewModel.setSelectedBroker(null)
                                    hideKeyboard()
                                },
                                label = "Select Broker",
                                items = filteredBrokers,
                                getItemLabel = { b -> "${b.brokerCode} - ${b.brokerName}" },
                                getItemId = { it.brokerCode },
                                onItemSelected = { b ->
                                    viewModel.setSelectedBroker(b)
                                    brokerSearchText = b.brokerName
                                    hideKeyboard()
                                },
                                onDropdownDismiss = { hideKeyboard() },
                                accentColor = IndentTeal,
                                itemIcon = Icons.Default.AccountBalance,
                                itemIconColor = IndentTeal,
                                itemTextColor = Color.Black,
                                isError = showValidationErrors && selectedBroker == null,
                                errorMessage = if (showValidationErrors && selectedBroker == null)
                                    "Please select a broker" else null,
                                noResultsMessage = "No broker linked to this party"
                            )
                        }
                    }
                }
                if (selectedParty != null && selectedBroker != null) {
                    FormSectionCard(title = "Anugya Scan", accentColor = Color.Black) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = {
                                        hideKeyboard()
                                        val integrator = IntentIntegrator(context as Activity)
                                        integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE)
                                        integrator.setPrompt("Align QR code inside the frame")
                                        integrator.setBeepEnabled(true)
                                        integrator.setOrientationLocked(false)
                                        integrator.setCaptureActivity(QRScannerActivity::class.java)

                                        val intent = integrator.createScanIntent()
                                        intent.putExtra("SCAN_TYPE", "QR")
                                        intent.putExtra("SCAN_TITLE", "QR SCANNER")
                                        intent.putExtra("SCAN_COLOR", "#0D47A1")
                                        anugyaScannerLauncher.launch(intent)
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = IndentTeal),
                                    contentPadding = PaddingValues(horizontal = 6.dp)
                                ) {
                                    Icon(
                                        Icons.Default.QrCodeScanner,
                                        contentDescription = "QR Scan",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "QR Scan",
                                        color = Color.White,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp
                                    )
                                }

                                Button(
                                    onClick = {
                                        hideKeyboard()
                                        val integrator = IntentIntegrator(context as Activity)
                                        integrator.setDesiredBarcodeFormats(IntentIntegrator.ONE_D_CODE_TYPES)
                                        integrator.setPrompt("Align barcode inside the frame")
                                        integrator.setBeepEnabled(true)
                                        integrator.setOrientationLocked(false)
                                        integrator.setCaptureActivity(QRScannerActivity::class.java)

                                        val intent = integrator.createScanIntent()
                                        intent.putExtra("SCAN_TYPE", "BARCODE")
                                        intent.putExtra("SCAN_TITLE", "BARCODE SCANNER")
                                        intent.putExtra("SCAN_COLOR", "#00695C")
                                        anugyaBarcodeScannerLauncher.launch(intent)
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF00695C)
                                    ),
                                    contentPadding = PaddingValues(horizontal = 6.dp)
                                ) {
                                    Icon(
                                        Icons.Default.QrCode,
                                        contentDescription = "Barcode Scan",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Barcode Scan",
                                        color = Color.White,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(modifier = Modifier.weight(1f)) {
                                    CompactInputFieldNew(
                                        value = anugyaNumber,
                                        onValueChange = { input ->
                                            if (!isAnugyaScanned) {
                                                val filtered = input.filter {
                                                    it.isLetterOrDigit() || it == '-' || it == '/' || it == ' '
                                                }
                                                viewModel.updateAnugyaNumber(filtered)
                                            }
                                        },
                                        hint = "Enter Anugya Number (Optional)",
                                        isError = false,
                                        errorMessage = null,
                                        keyboardOptions = KeyboardOptions(
                                            keyboardType = KeyboardType.Text,
                                            imeAction = ImeAction.Next,
                                            capitalization = KeyboardCapitalization.Characters
                                        ),
                                        keyboardActions = KeyboardActions(onNext = { /* no-op */ }),
                                        accentColor = if (isAnugyaScanned) Color(0xFF00695C) else IndentTeal,
                                        icon = if (isAnugyaScanned) Icons.Default.Lock else Icons.Default.Edit,
                                        enabled = !isAnugyaScanned
                                    )
                                }

                                if (anugyaNumber.isNotEmpty()) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    IconButton(
                                        onClick = {
                                            viewModel.updateAnugyaNumber("")
                                            isAnugyaScanned = false
                                            anugyaName = ""
                                            anugyaMobile = ""
                                            anugyaVillage = ""
                                            hideKeyboard()
                                        },
                                        modifier = Modifier
                                            .size(44.dp)
                                            .background(
                                                color = Color(0xFFFFEBEE),
                                                shape = RoundedCornerShape(10.dp)
                                            )
                                    ) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Clear Anugya",
                                            tint = Color(0xFFC62828),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }

                            if (isAnugyaScanned) {
                                Text(
                                    text = "🔒 Locked — tap ✕ to clear",
                                    fontSize = 11.sp,
                                    color = Color(0xFF00695C),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                if (selectedParty != null && selectedBroker != null) {
                    FormSectionCard(title = "Dispatch Details", accentColor = Color.Black) {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {

                            CompactInputField(
                                value = invoiceNumber,
                                onValueChange = { input ->
                                    val filtered = InputFilters.documentNumber(input)
                                    viewModel.updateInvoiceNumber(filtered)
                                },
                                hint = "Enter Invoice Number*",
                                isError = showValidationErrors && invoiceNumber.isBlank(),
                                errorMessage = if (showValidationErrors && invoiceNumber.isBlank())
                                    "Invoice number is required" else null,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Text,
                                    imeAction = ImeAction.Next,
                                    capitalization = KeyboardCapitalization.Characters
                                ),
                                keyboardActions = KeyboardActions(
                                    onNext = { quantityFocusRequester.requestFocus() }
                                ),
                                accentColor = IndentTeal,
                                icon = Icons.Default.Receipt,
                                modifier = Modifier.focusRequester(invoiceFocusRequester)
                            )

                            InvoiceDateSelector(
                                invoiceDate = invoiceDate,
                                isError = showValidationErrors && invoiceDate.isBlank(),
                                errorMessage = if (showValidationErrors && invoiceDate.isBlank())
                                    "Invoice date is required" else null,
                                accentColor = IndentTeal,
                                onClick = {
                                    hideKeyboard()
                                    showInvoiceDatePicker = true
                                }
                            )

                            MinimumQuantityCard(
                                limit = weightLimit,
                                label = "Minimum Dispatch Limit:",
                                unit = "Qtls",
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            Column {
                                CompactInputField(
                                    value = dispatchQuantity,
                                    onValueChange = { input ->
                                        val filtered = InputFilters.decimal(input)
                                        viewModel.updateDispatchQuantity(filtered)
                                    },
                                    hint = "Enter Dispatch Quantity (Qtls)*",
                                    isError = showValidationErrors && (
                                            dispatchQuantity.isBlank() ||
                                                    dispatchQuantity.toDoubleOrNull() == null ||
                                                    (dispatchQuantity.toDoubleOrNull() ?: 0.0) <= 0
                                            ) || weightLimitError != null,
                                    errorMessage = when {
                                        weightLimitError != null -> weightLimitError
                                        showValidationErrors && dispatchQuantity.isBlank() ->
                                            "Dispatch quantity is required"
                                        showValidationErrors && dispatchQuantity.toDoubleOrNull() == null ->
                                            "Please enter a valid quantity"
                                        showValidationErrors && (dispatchQuantity.toDoubleOrNull() ?: 0.0) <= 0 ->
                                            "Quantity must be greater than 0"
                                        else -> null
                                    },
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Decimal,
                                        imeAction = ImeAction.Next
                                    ),
                                    keyboardActions = KeyboardActions(
                                        onNext = { vehicleFocusRequester.requestFocus() }
                                    ),
                                    accentColor = IndentTeal,
                                    icon = Icons.Default.ShoppingCart,
                                    modifier = Modifier.focusRequester(quantityFocusRequester)
                                )

                                AvailableQuantityDisplay(
                                    availableQuantity = availableQuantity,
                                    isLoadingAllocation = isLoadingAllocation,
                                    dispatchQuantity = dispatchQuantity
                                )
                            }

                            if (showWeightLimitDialog == true) {
                                MinimumQuantityDialog(
                                    enteredQuantity = dispatchQuantity.toDoubleOrNull() ?: 0.0,
                                    minimumQuantity = weightLimit,
                                    unit = "Qtls",
                                    onClear = {
                                        viewModel.dismissWeightLimitDialog()
                                        viewModel.updateDispatchQuantity("")
                                        hideKeyboard()
                                        quantityFocusRequester.requestFocus()
                                        Toast.makeText(
                                            context,
                                            "Please enter at least ${
                                                String.format("%.2f", weightLimit)
                                            } Qtls",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    },
                                    accentColor = Color(0xFFFF9800),
                                    clearButtonText = "Clear"
                                )
                            }

                            CompactInputField(
                                value = vehicleNumber,
                                onValueChange = { input ->
                                    val filtered = InputFilters.vehicleNumber(input)
                                    viewModel.updateVehicleNumber(filtered)
                                },
                                hint = "Enter Vehicle Number*",
                                isError = (showValidationErrors && vehicleNumber.isBlank()) ||
                                        (vehicleNumber.isNotEmpty() && vehicleNumber.length < 8) ||
                                        (vehicleNumber.isNotEmpty() && vehicleNumber.length > 10),
                                errorMessage = when {
                                    showValidationErrors && vehicleNumber.isBlank() ->
                                        "Vehicle number is required"
                                    vehicleNumber.isNotEmpty() && vehicleNumber.length < 8 ->
                                        "Vehicle number must be at least 8 characters"
                                    vehicleNumber.isNotEmpty() && vehicleNumber.length > 10 ->
                                        "Vehicle number cannot exceed 10 characters"
                                    else -> null
                                },
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Text,
                                    imeAction = ImeAction.Next,
                                    capitalization = KeyboardCapitalization.Characters
                                ),
                                keyboardActions = KeyboardActions(
                                    onNext = {
                                        focusManager.clearFocus()
                                        hideKeyboard()
                                    }
                                ),
                                accentColor = IndentTeal,
                                icon = Icons.Default.DirectionsCar,
                                modifier = Modifier.focusRequester(vehicleFocusRequester)
                            )

                            CompactInputField(
                                value = driverName,
                                onValueChange = { input ->
                                    val filtered = InputFilters.name(input).uppercase()
                                    viewModel.updateDriverName(filtered)
                                },
                                hint = "Enter Driver Name*",
                                isError = showValidationErrors && driverName.isBlank(),
                                errorMessage = if (showValidationErrors && driverName.isBlank())
                                    "Driver name is required" else null,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Text,
                                    imeAction = ImeAction.Next,
                                    capitalization = KeyboardCapitalization.Characters
                                ),
                                keyboardActions = KeyboardActions(
                                    onNext = {
                                        focusManager.clearFocus()
                                        hideKeyboard()
                                    }
                                ),
                                accentColor = IndentTeal,
                                icon = Icons.Default.Person
                            )

                            CompactInputField(
                                value = driverMobileNumber,
                                onValueChange = { input ->
                                    val filtered = InputFilters.mobile(input, maxLength = 10)
                                    viewModel.updateDriverMobileNumber(filtered)
                                },
                                hint = "Enter Driver Mobile Number*",
                                isError = showValidationErrors && (
                                        driverMobileNumber.isBlank() || driverMobileNumber.length < 10
                                        ),
                                errorMessage = when {
                                    showValidationErrors && driverMobileNumber.isBlank() ->
                                        "Driver mobile number is required"
                                    showValidationErrors && driverMobileNumber.length < 10 ->
                                        "Driver mobile number must be 10 digits"
                                    else -> null
                                },
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Number,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(onDone = { hideKeyboard() }),
                                accentColor = IndentTeal,
                                icon = Icons.Default.Phone
                            )
                        }
                    }
                }

                if (selectedParty != null && selectedBroker != null) {
                    FormSectionCard(title = "Document Image Upload", accentColor = Color.Black) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

                            DispatchImageUploadCard(
                                title = "Anugya",
                                imageUri = anugyaImageUri,
                                accentColor = IndentTeal,
                                onShowOptions = { showImagePicker("anugya") }
                            )

                            DispatchImageUploadCard(
                                title = "Bilty",
                                imageUri = biltyImageUri,
                                accentColor = IndentTeal,
                                onShowOptions = { showImagePicker("bilty") }
                            )

                            DispatchImageUploadCard(
                                title = "Invoice",
                                imageUri = invoiceImageUri,
                                accentColor = IndentTeal,
                                onShowOptions = { showImagePicker("invoice") }
                            )

                            DispatchImageUploadCard(
                                title = "Weighment Slip",
                                imageUri = weighmentSlipImageUri,
                                accentColor = IndentTeal,
                                onShowOptions = { showImagePicker("weighment") }
                            )

                            DispatchImageUploadCard(
                                title = "Other Document",
                                imageUri = otherDocumentImageUri,
                                accentColor = IndentTeal,
                                onShowOptions = { showImagePicker("other") }
                            )
                        }
                    }
                }

                if (selectedParty != null && selectedBroker != null) {
                    Spacer(modifier = Modifier.height(4.dp))

                    PrimarySubmitButton(
                        onClick = {
                            hideKeyboard()
                            viewModel.validateForm()

                            when {
                                selectedParty == null -> {
                                    partyFocusRequester.requestFocus()
                                    return@PrimarySubmitButton
                                }
                                selectedBroker == null -> {
                                    brokerFocusRequester.requestFocus()
                                    Toast.makeText(context, "Please select a broker", Toast.LENGTH_SHORT).show()
                                    return@PrimarySubmitButton
                                }
                                invoiceNumber.isBlank() -> {
                                    invoiceFocusRequester.requestFocus()
                                    return@PrimarySubmitButton
                                }
                                invoiceDate.isBlank() -> {
                                    Toast.makeText(
                                        context,
                                        "Please select invoice date",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    showInvoiceDatePicker = true
                                    return@PrimarySubmitButton
                                }
                                dispatchQuantity.isBlank() -> {
                                    quantityFocusRequester.requestFocus()
                                    return@PrimarySubmitButton
                                }
                                vehicleNumber.isBlank() -> {
                                    vehicleFocusRequester.requestFocus()
                                    Toast.makeText(
                                        context,
                                        "Vehicle number is required",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    return@PrimarySubmitButton
                                }
                                vehicleNumber.length < 8 -> {
                                    vehicleFocusRequester.requestFocus()
                                    Toast.makeText(
                                        context,
                                        "Vehicle number must be at least 8 characters",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    return@PrimarySubmitButton
                                }
                                vehicleNumber.length > 10 -> {
                                    vehicleFocusRequester.requestFocus()
                                    Toast.makeText(
                                        context,
                                        "Vehicle number cannot exceed 10 characters",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    return@PrimarySubmitButton
                                }
                                driverName.isBlank() -> {
                                    focusManager.clearFocus()
                                    hideKeyboard()
                                    Toast.makeText(
                                        context,
                                        "Driver name is required",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    return@PrimarySubmitButton
                                }
                                driverMobileNumber.isBlank() || driverMobileNumber.length < 10 -> {
                                    focusManager.clearFocus()
                                    hideKeyboard()
                                    Toast.makeText(
                                        context,
                                        "Please enter a valid 10-digit driver mobile number",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    return@PrimarySubmitButton
                                }
                            }
                            val quantity = dispatchQuantity.toDoubleOrNull() ?: 0.0

                            // ✅ NEW: 4-scenario allocation validation
                            if (!viewModel.validateQuantityAgainstAllocation(quantity)) {
                                Log.w(TAG_VALIDATION, "❌ validateQuantityAgainstAllocation() returned false")
                                return@PrimarySubmitButton
                            }

                            viewModel.checkTruckAvailability(vehicleNumber) { isAvailable ->
                                if (isAvailable) {
                                    showConfirmationDialog = true
                                }
                            }
                        },
                        text = if (isCheckingTruck) "Checking Truck..." else "Submit Dispatch",
                        gradientColors = listOf(IndentTeal, IndentCoral),
                        shadowColor = IndentTeal,
                        isLoading = isSubmitting || isCheckingTruck,
                        enabled = !isCheckingTruck
                    )
                }

                Spacer(modifier = Modifier.height(300.dp))
            }
        }
    }
    if (showNoBrokerDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissNoBrokerDialog() },
            title = { Text("No Broker Linked") },
            text = {
                Text("The party \"$noBrokerPartyName\" has no broker linked. Please contact the administrator.")
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.dismissNoBrokerDialog()
                    partySearchText = ""
                    brokerSearchText = ""
                    hideKeyboard()
                    partyFocusRequester.requestFocus()
                }) { Text("OK") }
            }
        )
    }

    if (showConfirmationDialog && selectedParty != null && selectedBroker != null) {
        DispatchConfirmationDialog(
            selectedParty = selectedParty!!,
            selectedBrokerName = selectedBroker!!.brokerName,
            selectedBrokerCode = selectedBroker!!.brokerCode,
            invoiceNumber = invoiceNumber,
            invoiceDate = invoiceDate,
            dispatchQuantity = dispatchQuantity,
            vehicleNumber = vehicleNumber,
            driverName = driverName,
            driverMobileNumber = driverMobileNumber,
            anugyaNumber = anugyaNumber,
            anugyaName = anugyaName,
            anugyaMobile = anugyaMobile,
            anugyaVillage = anugyaVillage,
            hasAnugyaImage = anugyaImageUri != null,
            hasBiltyImage = biltyImageUri != null,
            hasInvoiceImage = invoiceImageUri != null,
            hasWeighmentImage = weighmentSlipImageUri != null,
            hasOtherDocumentImage = otherDocumentImageUri != null,
            onConfirm = {
                showConfirmationDialog = false
                submissionState = SubmissionState(
                    isLoading = true,
                    progress = 0,
                    status = "Submitting..."
                )
                viewModel.submitDispatch(
                    currentDateTime = currentDateTime,
                    onSuccess = { message ->
                        submissionState =
                            SubmissionState(isSuccess = true, successMessage = message)
                    },
                    onError = { error ->
                        submissionState = SubmissionState(isError = true, errorMessage = error)
                    }
                )
            },
            onDismiss = {
                showConfirmationDialog = false
                hideKeyboard()
            }
        )
    }

    SubmissionHandler(
        submissionState = submissionState,
        onDismissError = {
            submissionState = SubmissionState()
            hideKeyboard()
        },
        onDismissSuccess = {
            submissionState = SubmissionState()
            hideKeyboard()
        }
    )

    if (showTruckUsedDialog) {
        TruckAlreadyUsedDialog(
            truckNumber = truckUsedNumber,
            onOk = {
                viewModel.dismissTruckUsedDialog()
                viewModel.resetForm()
                partySearchText = ""
                anugyaName = ""
                anugyaMobile = ""
                anugyaVillage = ""
                isAnugyaScanned = false
                hideKeyboard()
                partyFocusRequester.requestFocus()
            }
        )
    }

    if (showBrokerNotLinkedDialog) {
        BrokerNotLinkedDialog(
            partyName = brokerNotLinkedPartyName,
            onOk = {
                viewModel.dismissBrokerNotLinkedDialog()
                viewModel.resetForm()
                partySearchText = ""
                anugyaName = ""
                anugyaMobile = ""
                anugyaVillage = ""
                isAnugyaScanned = false
                hideKeyboard()
                partyFocusRequester.requestFocus()
            }
        )
    }

    // ============================================================
    //  NO SAUDA DIALOG
    // ============================================================
    if (showNoSaudaDialog) {
        NoSaudaDialog(
            partyName = noSaudaPartyName,
            onOk = {
                viewModel.dismissNoSaudaDialog()
                viewModel.resetForm()
                partySearchText = ""
                anugyaName = ""
                anugyaMobile = ""
                anugyaVillage = ""
                isAnugyaScanned = false
                hideKeyboard()
                partyFocusRequester.requestFocus()
            }
        )
    }

    // ============================================================
    //  DIALOG 1: Insufficient Quantity, No Sauda
    // ============================================================
    if (showInsufficientQtyDialog) {
        InsufficientQtyNoSaudaDialog(
            aqM1 = aqM1Value,
            minQty = minQtyValue,
            onOk = {
                viewModel.dismissInsufficientQtyDialog()
                viewModel.resetForm()
                partySearchText = ""
                anugyaName = ""
                anugyaMobile = ""
                anugyaVillage = ""
                isAnugyaScanned = false
                hideKeyboard()
                partyFocusRequester.requestFocus()
            }
        )
    }

    // ============================================================
    //  DIALOG 2: Qty Exceeds Approved
    // ============================================================
    if (showQtyExceedsApprovedDialog) {
        QtyExceedsApprovedDialog(
            aqM1 = aqM1Value,
            enteredQty = dispatchQuantity.toDoubleOrNull() ?: 0.0,
            onOk = {
                viewModel.dismissQtyExceedsApprovedDialog()
                viewModel.resetForm()
                partySearchText = ""
                anugyaName = ""
                anugyaMobile = ""
                anugyaVillage = ""
                isAnugyaScanned = false
                hideKeyboard()
                partyFocusRequester.requestFocus()
            }
        )
    }

    // ============================================================
    //  DIALOG 3: Unapproved Sauda Exists, Qty < M.Q
    // ============================================================
    if (showSaudaPendingApprovalDialog) {
        SaudaPendingApprovalDialog(
            aqM1 = aqM1Value,
            aqM2 = aqM2Value,
            minQty = minQtyValue,
            onOk = {
                viewModel.dismissSaudaPendingApprovalDialog()
                viewModel.resetForm()
                partySearchText = ""
                anugyaName = ""
                anugyaMobile = ""
                anugyaVillage = ""
                isAnugyaScanned = false
                hideKeyboard()
                partyFocusRequester.requestFocus()
            }
        )
    }

    // ============================================================
    //  DIALOG 4: Unapproved Sauda Exists, Qty > A.Q m1
    // ============================================================
    if (showQtyExceedsUnapprovedSaudaDialog) {
        QtyExceedsUnapprovedSaudaDialog(
            aqM1 = aqM1Value,
            aqM2 = aqM2Value,
            enteredQty = dispatchQuantity.toDoubleOrNull() ?: 0.0,
            onOk = {
                viewModel.dismissQtyExceedsUnapprovedSaudaDialog()
                viewModel.resetForm()
                partySearchText = ""
                anugyaName = ""
                anugyaMobile = ""
                anugyaVillage = ""
                isAnugyaScanned = false
                hideKeyboard()
                partyFocusRequester.requestFocus()
            }
        )
    }
    // ============================================================
    //  INVOICE DATE PICKER
    // ============================================================
    if (showInvoiceDatePicker) {
        CompactInvoiceDatePicker(
            onDateSelected = { selectedDate ->
                Log.d(TAG_UI, "📅 Invoice date selected: $selectedDate")
                viewModel.updateInvoiceDate(selectedDate)
                showInvoiceDatePicker = false
                hideKeyboard()
            },
            onDismiss = {
                showInvoiceDatePicker = false
                hideKeyboard()
            }
        )
    }

    // ============================================================
    //  IMAGE PICKER BOTTOM SHEET
    // ============================================================
    if (showImagePickerOptions) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val scope = rememberCoroutineScope()

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
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Select ${currentImageField?.replaceFirstChar { it.uppercase() } ?: "Document"} Image",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = IndentTeal,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            scope.launch { sheetState.hide() }
                            galleryLauncher.launch("image/*")
                        }
                        .padding(vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.PhotoLibrary,
                        contentDescription = null,
                        tint = IndentTeal,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("Choose from Gallery", fontSize = 15.sp, color = Color.Black)
                }

                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            scope.launch { sheetState.hide() }
                            val hasPerm = ContextCompat.checkSelfPermission(
                                context, Manifest.permission.CAMERA
                            ) == PackageManager.PERMISSION_GRANTED
                            if (hasPerm) {
                                val uri = createImageUri(context)
                                currentPhotoUri = uri
                                uri?.let { cameraLauncher.launch(it) }
                            } else {
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        }
                        .padding(vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.CameraAlt,
                        contentDescription = null,
                        tint = IndentTeal,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("Take Photo", fontSize = 15.sp, color = Color.Black)
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

// ============================================================
//  INVOICE DATE SELECTOR
// ============================================================
@Composable
fun InvoiceDateSelector(
    invoiceDate: String,
    isError: Boolean,
    errorMessage: String?,
    accentColor: Color,
    onClick: () -> Unit,
) {
    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = if (isError) 0.dp else 2.dp,
                    shape = RoundedCornerShape(8.dp)
                )
                .border(
                    width = if (isError) 1.dp else 0.dp,
                    color = if (isError) Color(0xFFE53935) else accentColor,
                    shape = RoundedCornerShape(8.dp)
                )
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { onClick() })
                }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (isError) Color(0xFFE53935).copy(alpha = 0.08f) else Color(0xFFF8F9FA),
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
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = "Invoice Date",
                        tint = if (isError) Color(0xFFE53935) else accentColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (invoiceDate.isBlank()) "Select invoice date*" else invoiceDate,
                        fontSize = 12.sp,
                        color = when {
                            isError -> Color(0xFFE53935)
                            invoiceDate.isBlank() -> Color.Gray
                            else -> Color.Black
                        }
                    )
                }

                if (invoiceDate.isNotBlank()) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Selected",
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(16.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "Open",
                        tint = if (isError) Color(0xFFE53935) else accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        if (isError && errorMessage != null) {
            Text(
                text = errorMessage,
                fontSize = 11.sp,
                color = Color(0xFFE53935),
                modifier = Modifier.padding(start = 12.dp, top = 4.dp)
            )
        }
    }
}

// ============================================================
//  COMPACT INVOICE DATE PICKER
// ============================================================
@Composable
fun CompactInvoiceDatePicker(
    onDateSelected: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        val calendar = Calendar.getInstance()

        val dialog = DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val selectedCalendar = Calendar.getInstance().apply {
                    set(year, month, dayOfMonth)
                }
                val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                onDateSelected(formatter.format(selectedCalendar.time))
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )

        dialog.datePicker.maxDate = System.currentTimeMillis()
        dialog.setOnDismissListener { onDismiss() }
        dialog.show()
    }
}

// ============================================================
//  DISPATCH IMAGE UPLOAD CARD
// ============================================================
@Composable
fun DispatchImageUploadCard(
    title: String,
    imageUri: Uri?,
    accentColor: Color,
    onShowOptions: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(
                        color = if (imageUri != null) accentColor.copy(alpha = 0.12f)
                        else Color(0xFFF0F0F0),
                        shape = RoundedCornerShape(8.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (imageUri != null) {
                    AsyncImage(
                        model = imageUri,
                        contentDescription = title,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        Icons.Default.AddAPhoto,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (imageUri != null) "Image selected ✓" else "No image selected",
                    fontSize = 11.sp,
                    color = if (imageUri != null) Color(0xFF4CAF50) else Color.Gray
                )
            }

            TextButton(
                onClick = onShowOptions,
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(
                    imageVector = if (imageUri != null) Icons.Default.Edit else Icons.Default.Add,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (imageUri != null) "Change" else "Add",
                    color = accentColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

// ============================================================
//  BROKER NOT LINKED DIALOG
// ============================================================
@Composable
fun BrokerNotLinkedDialog(
    partyName: String,
    onOk: () -> Unit,
) {
    Dialog(
        onDismissRequest = { /* force OK */ },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(
                            color = Color(0xFFFFF3E0),
                            shape = RoundedCornerShape(50)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.LinkOff,
                        contentDescription = "No Broker",
                        tint = Color(0xFFFF6B00),
                        modifier = Modifier.size(48.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Surface(
                    color = Color(0xFFFFEBEE),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFFE53935))
                ) {
                    Text(
                        text = partyName,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFC62828)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "इस पार्टी के साथ कोई ब्रोकर लिंक नहीं है!",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFC62828),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "No broker is linked with this party!",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF616161),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "पहले ब्रोकर लिंक कराएं।\nPlease link the broker first.",
                    fontSize = 13.sp,
                    color = Color(0xFF757575),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onOk,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828))
                ) {
                    Text(
                        text = "OK / ठीक है",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

// ============================================================
//  NO SAUDA DIALOG
// ============================================================
@Composable
fun NoSaudaDialog(
    partyName: String,
    onOk: () -> Unit,
) {
    Dialog(
        onDismissRequest = { /* force OK */ },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(
                            color = Color(0xFFFFF3E0),
                            shape = RoundedCornerShape(50)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "No Sauda",
                        tint = Color(0xFFFF6B00),
                        modifier = Modifier.size(48.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Surface(
                    color = Color(0xFFFFEBEE),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Orange)
                ) {
                    Text(
                        text = partyName,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Orange
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "इस पार्टी के लिए अभी सौदा नहीं डाला गया है!",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Orange,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "No sauda has been placed for this party yet!",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF616161),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "पहले सौदा डालें।\nPlease place the sauda first.",
                    fontSize = 13.sp,
                    color = Color(0xFF757575),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onOk,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Orange)
                ) {
                    Text(
                        text = "OK / ठीक है",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

// ============================================================
//  TRUCK ALREADY USED DIALOG
// ============================================================
@Composable
fun TruckAlreadyUsedDialog(
    truckNumber: String,
    onOk: () -> Unit,
) {
    Dialog(
        onDismissRequest = { /* force OK */ },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(color = Color(0xFFFFF3E0), shape = RoundedCornerShape(50)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.LocalShipping,
                        contentDescription = "Truck",
                        tint = Color(0xFFFF6B00),
                        modifier = Modifier.size(48.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Surface(
                    color = Color(0xFFFFEBEE),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFFE53935))
                ) {
                    Text(
                        text = truckNumber,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFC62828)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "यह ट्रक पहले से उपयोग में है!",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFC62828),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "This truck is already in use!",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF616161),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onOk,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828))
                ) {
                    Text(
                        text = "OK / ठीक है",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

// ============================================================
//  DIALOG 1 — Insufficient Quantity, No Sauda
//  Color: Deep Orange
// ============================================================
@Composable
fun InsufficientQtyNoSaudaDialog(
    aqM1: Double,
    minQty: Double,
    onOk: () -> Unit,
) {
    Dialog(
        onDismissRequest = { /* force OK */ },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(color = Color(0xFFFFF3E0), shape = RoundedCornerShape(50)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Insufficient",
                        tint = Color(0xFFE65100),
                        modifier = Modifier.size(48.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Surface(
                    color = Color(0xFFFFE0B2),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFFE65100))
                ) {
                    Text(
                        text = "अपर्याप्त मात्रा / Insufficient Quantity",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFE65100),
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "इस पार्टी के पास पर्याप्त मात्रा नहीं है।\nपहले सौदा डालवाएं।",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFE65100),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Insufficient quantity available for this party.\nPlease add the sauda first.",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF616161),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                Surface(
                    color = Color(0xFFFFF8E1),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Available: ${String.format("%.2f", aqM1)} Qtls",
                            fontSize = 13.sp,
                            color = Color(0xFF424242)
                        )
                        Text(
                            text = "Minimum Required: ${String.format("%.2f", minQty)} Qtls",
                            fontSize = 13.sp,
                            color = Color(0xFF424242)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onOk,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100))
                ) {
                    Text(
                        text = "OK / ठीक है",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

// ============================================================
//  DIALOG 2 — Qty Exceeds A.Q (Approved Sauda Exists)
//  Color: Red
// ============================================================
@Composable
fun QtyExceedsApprovedDialog(
    aqM1: Double,
    enteredQty: Double,
    onOk: () -> Unit,
) {
    Dialog(
        onDismissRequest = { /* force OK */ },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(color = Color(0xFFFFEBEE), shape = RoundedCornerShape(50)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ErrorOutline,
                        contentDescription = "Exceeds",
                        tint = Color(0xFFC62828),
                        modifier = Modifier.size(48.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Surface(
                    color = Color(0xFFFFCDD2),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFFC62828))
                ) {
                    Text(
                        text = "मात्रा अधिक है / Quantity Exceeds",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFC62828),
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "आपकी उपलब्ध मात्रा ${String.format("%.2f", aqM1)} Qtls है।",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFC62828),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "अगर आप इससे ज़्यादा मात्रा डिस्पैच करना चाहते हैं,\nतो पहले इस पार्टी के लिए सौदा डालवाएं।",
                    fontSize = 14.sp,
                    color = Color(0xFF616161),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Your available quantity is ${String.format("%.2f", aqM1)} Qtls.\n" +
                            "If you want to dispatch more, please add the sauda first.",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF616161),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                Surface(
                    color = Color(0xFFFFF8E1),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Available: ${String.format("%.2f", aqM1)} Qtls",
                            fontSize = 13.sp,
                            color = Color(0xFF424242)
                        )
                        Text(
                            text = "Entered: ${String.format("%.2f", enteredQty)} Qtls",
                            fontSize = 13.sp,
                            color = Color(0xFF424242)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onOk,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828))
                ) {
                    Text(
                        text = "OK / ठीक है",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

// ============================================================
//  DIALOG 3 — Unapproved Sauda Exists, Qty < M.Q
//  Color: Purple / Indigo
// ============================================================
@Composable
fun SaudaPendingApprovalDialog(
    aqM1: Double,
    aqM2: Double,
    minQty: Double,
    onOk: () -> Unit,
) {
    Dialog(
        onDismissRequest = { /* force OK */ },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(color = Color(0xFFEDE7F6), shape = RoundedCornerShape(50)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.HourglassEmpty,
                        contentDescription = "Pending",
                        tint = Color(0xFF5E35B1),
                        modifier = Modifier.size(48.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Surface(
                    color = Color(0xFFD1C4E9),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFF5E35B1))
                ) {
                    Text(
                        text = "सौदा approve नहीं हुआ / Sauda Pending Approval",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF5E35B1),
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "डिस्पैच करने के लिए पर्याप्त मात्रा नहीं है।",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF5E35B1),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "इस पार्टी के लिए सौदा उपलब्ध है,\nलेकिन अभी approve नहीं किया गया है।\nकृपया पहले प्लांट से approve करवाएं।",
                    fontSize = 14.sp,
                    color = Color(0xFF616161),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Insufficient quantity for dispatch.\n" +
                            "A sauda exists for this party but is not yet approved.\n" +
                            "Please get it approved from the plant first.",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF616161),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                Surface(
                    color = Color(0xFFF3E5F5),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Approved A.Q: ${String.format("%.2f", aqM1)} Qtls",
                            fontSize = 13.sp,
                            color = Color(0xFF424242)
                        )
                        Text(
                            text = "Total A.Q (unapproved): ${String.format("%.2f", aqM2)} Qtls",
                            fontSize = 13.sp,
                            color = Color(0xFF424242)
                        )
                        Text(
                            text = "Minimum Required: ${String.format("%.2f", minQty)} Qtls",
                            fontSize = 13.sp,
                            color = Color(0xFF424242)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onOk,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5E35B1))
                ) {
                    Text(
                        text = "OK / ठीक है",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

// ============================================================
//  DIALOG 4 — Unapproved Sauda Exists, Qty > A.Q m1
//  Color: Blue
// ============================================================
@Composable
fun QtyExceedsUnapprovedSaudaDialog(
    aqM1: Double,
    aqM2: Double,
    enteredQty: Double,
    onOk: () -> Unit,
) {
    Dialog(
        onDismissRequest = { /* force OK */ },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(color = Color(0xFFE3F2FD), shape = RoundedCornerShape(50)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Info",
                        tint = Color(0xFF1565C0),
                        modifier = Modifier.size(48.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Surface(
                    color = Color(0xFFBBDEFB),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFF1565C0))
                ) {
                    Text(
                        text = "मात्रा अधिक + सौदा pending",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1565C0),
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "आपकी उपलब्ध मात्रा ${String.format("%.2f", aqM1)} Qtls है।",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1565C0),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "अगर आप इससे ज़्यादा मात्रा डिस्पैच करना चाहते हैं,\n" +
                            "तो इस पार्टी के लिए सौदा उपलब्ध है\n" +
                            "लेकिन approve नहीं किया गया है।\n" +
                            "कृपया पहले प्लांट से approve करवाएं।",
                    fontSize = 14.sp,
                    color = Color(0xFF616161),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Your available quantity is ${String.format("%.2f", aqM1)} Qtls.\n" +
                            "If you want to dispatch more, a sauda exists for this party\n" +
                            "but is not yet approved. Please get it approved from the plant first.",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF616161),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                Surface(
                    color = Color(0xFFE3F2FD),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Approved A.Q: ${String.format("%.2f", aqM1)} Qtls",
                            fontSize = 13.sp,
                            color = Color(0xFF424242)
                        )
                        Text(
                            text = "Total A.Q (unapproved): ${String.format("%.2f", aqM2)} Qtls",
                            fontSize = 13.sp,
                            color = Color(0xFF424242)
                        )
                        Text(
                            text = "Entered: ${String.format("%.2f", enteredQty)} Qtls",
                            fontSize = 13.sp,
                            color = Color(0xFF424242)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onOk,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0))
                ) {
                    Text(
                        text = "OK / ठीक है",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}
@Composable
fun DispatchConfirmationDialog(
    selectedParty: Party,
    selectedBrokerName: String = "",
    selectedBrokerCode: String = "",
    invoiceNumber: String,
    invoiceDate: String,
    dispatchQuantity: String,
    vehicleNumber: String,
    driverName: String,
    driverMobileNumber: String,
    anugyaNumber: String = "",
    anugyaName: String = "",
    anugyaMobile: String = "",
    anugyaVillage: String = "",
    hasAnugyaImage: Boolean = false,
    hasBiltyImage: Boolean = false,
    hasInvoiceImage: Boolean = false,
    hasWeighmentImage: Boolean = false,
    hasOtherDocumentImage: Boolean = false,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
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
                .padding(horizontal = 20.dp, vertical = 16.dp),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .heightIn(max = 560.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Confirm Dispatch Details",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = IndentTeal
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.Gray,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .heightIn(max = 420.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Party Details",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = IndentTeal
                    )
                    CompactDialogRowHalf(label = "Party Name", value = selectedParty.accountName)
                    CompactDialogRowHalf(label = "Location", value = selectedParty.placeName)
                    if (selectedBrokerName.isNotEmpty()) {
                        CompactDialogRowHalf(
                            label = "Broker",
                            value = "$selectedBrokerCode - $selectedBrokerName"
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        "Dispatch Details",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = IndentTeal
                    )
                    CompactDialogRowHalf(
                        label = "Invoice No",
                        value = invoiceNumber.ifEmpty { "-" })
                    CompactDialogRowHalf(
                        label = "Invoice Date",
                        value = invoiceDate.ifBlank { "-" })
                    CompactDialogRowHalf(label = "Quantity", value = "$dispatchQuantity Qtls")
                    CompactDialogRowHalf(
                        label = "Vehicle No",
                        value = vehicleNumber.ifEmpty { "-" })
                    CompactDialogRowHalf(label = "Driver Name", value = driverName)
                    CompactDialogRowHalf(label = "Driver Mobile", value = driverMobileNumber)

                    if (anugyaNumber.isNotEmpty() || anugyaName.isNotEmpty() ||
                        anugyaMobile.isNotEmpty() || anugyaVillage.isNotEmpty()
                    ) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Anugya Details",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = IndentTeal
                        )
                        if (anugyaNumber.isNotEmpty()) {
                            CompactDialogRowHalf(label = "Anugya No", value = anugyaNumber)
                        }
                        if (anugyaName.isNotEmpty()) {
                            CompactDialogRowHalf(label = "Driver", value = anugyaName)
                        }
                        if (anugyaMobile.isNotEmpty()) {
                            CompactDialogRowHalf(label = "Mobile", value = anugyaMobile)
                        }
                        if (anugyaVillage.isNotEmpty()) {
                            CompactDialogRowHalf(label = "Source Mandi", value = anugyaVillage)
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Documents Uploaded",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = IndentTeal
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CompactDialogRowHalf(
                            label = "Anugya",
                            value = if (hasAnugyaImage) "✓" else "✗",
                            modifier = Modifier.weight(1f)
                        )
                        CompactDialogRowHalf(
                            label = "Bilty",
                            value = if (hasBiltyImage) "✓" else "✗",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CompactDialogRowHalf(
                            label = "Invoice",
                            value = if (hasInvoiceImage) "✓" else "✗",
                            modifier = Modifier.weight(1f)
                        )
                        CompactDialogRowHalf(
                            label = "Weigh Slip",
                            value = if (hasWeighmentImage) "✓" else "✗",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CompactDialogRowHalf(
                            label = "Other Doc",
                            value = if (hasOtherDocumentImage) "✓" else "✗",
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.weight(1f))
                    }

                    Spacer(modifier = Modifier.height(2.dp))
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color(0xFFDDDDDD))
                    ) {
                        Text("Cancel", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }

                    Button(
                        onClick = onConfirm,
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = IndentCoral)
                    ) {
                        Text(
                            "Confirm",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

// ============================================================
//  AVAILABLE QUANTITY DISPLAY
// ============================================================
@Composable
fun AvailableQuantityDisplay(
    availableQuantity: Double?,
    isLoadingAllocation: Boolean,
    dispatchQuantity: String,
) {
    when {
        isLoadingAllocation -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = IndentTeal,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Loading available quantity...", fontSize = 11.sp, color = Color.Gray)
            }
        }

        availableQuantity != null -> {
            val isOver = (dispatchQuantity.toDoubleOrNull() ?: 0.0) > availableQuantity
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = "Available Quantity: ${String.format("%.2f", availableQuantity)} Qtls",
                    fontSize = 12.sp,
                    color = if (isOver) MaterialTheme.colorScheme.error else Color(0xFF4CAF50),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }


}