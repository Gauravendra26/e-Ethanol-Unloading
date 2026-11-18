package com.visibleinfotech.e_ethanolUnloading.unloading

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warehouse
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.visibleinfotech.e_ethanolUnloading.login.CompactHintDropdown
import com.visibleinfotech.e_ethanolUnloading.login.CompactInputField
import com.visibleinfotech.e_ethanolUnloading.login.ModernSearchableDropdown
import com.visibleinfotech.e_ethanolUnloading.ui.theme.DarkGreen
import com.visibleinfotech.e_ethanolUnloading.ui.theme.DarkOrange
import com.visibleinfotech.e_ethanolUnloading.ui.theme.ErrorRed
import com.visibleinfotech.e_ethanolUnloading.ui.theme.Orange
import com.visibleinfotech.e_ethanolUnloading.ui.theme.PrimaryGreen
import com.visibleinfotech.e_ethanolUnloading.ui.theme.WhiteColor
import com.visibleinfotech.e_ethanolUnloading.utils.SetStatusBarColor
import com.visibleinfotech.e_ethanolUnloading.unloading.models.UnloadingRow

private const val TAG_FORM = "UnloadingFormLog"
@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalComposeUiApi::class,
    ExperimentalFoundationApi::class
)
@Composable
fun UnloadingForm(
    onBack: () -> Unit = {},
    viewModel: UnloadingViewModel = viewModel(
        factory = UnloadingViewModelFactory(LocalContext.current)
    ),
) {
    SetStatusBarColor(DarkGreen)

    val uiState by viewModel.uiState.collectAsState()
    val effectiveRejectMoisture by viewModel.effectiveRejectMoisture.collectAsState()

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    var showConfirmationDialog by remember { mutableStateOf(false) }
    var showSubmitConfirmDialog by remember { mutableStateOf(false) }

    var showRejectReasonDialog by remember { mutableStateOf(false) }
    var showRejectConfirmDialog by remember { mutableStateOf(false) }

    var godownExpanded by remember { mutableStateOf(false) }
    var thekedarExpanded by remember { mutableStateOf(false) }

    val unloadingRows = remember { mutableStateListOf(UnloadingRow(srNo = 1)) }
    var showEmptyRowsAlert by remember { mutableStateOf(false) }
    var showSubmittingOverlay by remember { mutableStateOf(false) }

    var selectedRejectCode by remember { mutableStateOf("0") }
    var selectedRejectName by remember { mutableStateOf("") }
    var rejectExpanded by remember { mutableStateOf(false) }

    var remarkText by remember { mutableStateOf("") }

    var overLimitPendingIndex by remember { mutableStateOf<Int?>(null) }
    var showOverLimitConfirmDialog by remember { mutableStateOf(false) }

    val hideKeyboard = remember {
        {
            keyboardController?.hide()
            focusManager.clearFocus(force = true)
        }
    }

    LaunchedEffect(showRejectReasonDialog) {
        if (showRejectReasonDialog && uiState.rejectList.isEmpty()) {
            viewModel.loadRejectList()
        }
    }

    LaunchedEffect(uiState.selectedGrossData, uiState.selectedVehicle) {
        val gross = uiState.selectedGrossData ?: return@LaunchedEffect
        if (uiState.selectedVehicle.isBlank()) return@LaunchedEffect

        val hasUserEntries = unloadingRows.any {
            !it.isPersisted && (it.bags.isNotBlank() || it.moisture.isNotBlank())
        }
        if (hasUserEntries) return@LaunchedEffect

        viewModel.loadPreviousUnloading(
            gatePass = uiState.selectedGatePass,
            vehicleNo = uiState.selectedVehicle
        ) { previousRows ->
            if (previousRows.isNotEmpty()) {
                unloadingRows.clear()
                unloadingRows.addAll(previousRows)
                unloadingRows.add(UnloadingRow(srNo = previousRows.size + 1))
                Log.d(TAG_FORM, "Loaded ${previousRows.size} finalised ('Y') rows")
            }
        }
    }

    LaunchedEffect(uiState.selectedVehicle, uiState.selectedGatePass, uiState.selectedGrossData) {
        if (uiState.selectedVehicle.isBlank() || uiState.selectedGatePass.isBlank()) return@LaunchedEffect
        if (uiState.selectedGrossData == null) return@LaunchedEffect

        val hasUserEntries = unloadingRows.any {
            !it.isPersisted && (it.bags.isNotBlank() || it.moisture.isNotBlank())
        }
        if (hasUserEntries) return@LaunchedEffect

        viewModel.loadPendingUnloading(
            gatePass = uiState.selectedGatePass,
            vehicleNo = uiState.selectedVehicle
        ) { pendingRows ->
            if (pendingRows.isEmpty()) {
                Log.d(TAG_FORM, "No pending ('N') rows")
                return@loadPendingUnloading
            }

            val existingCodes = unloadingRows
                .filter { it.isPersisted && it.uCode > 0 }
                .map { it.uCode }
                .toSet()

            val toAdd = pendingRows.filter { it.uCode <= 0 || it.uCode !in existingCodes }
            if (toAdd.isEmpty()) {
                Log.d(TAG_FORM, "All pending rows already loaded — skipping")
                return@loadPendingUnloading
            }

            val hasNewPlaceholder = unloadingRows.size == 1 &&
                    unloadingRows[0].bags.isBlank() &&
                    unloadingRows[0].moisture.isBlank() &&
                    !unloadingRows[0].isPersisted
            if (hasNewPlaceholder) unloadingRows.clear()

            unloadingRows.addAll(toAdd)
            unloadingRows.add(UnloadingRow(srNo = unloadingRows.size + 1))

            Log.d(
                TAG_FORM,
                "Merged ${toAdd.size} pending ('N') rows — total rows = ${unloadingRows.size}"
            )
        }
    }

    BackHandler {
        hideKeyboard()
        onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Unloading Form",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        hideKeyboard()
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
                            viewModel.resetForm()
                            unloadingRows.clear()
                            unloadingRows.add(UnloadingRow(srNo = 1))
                            remarkText = ""
                            overLimitPendingIndex = null
                            showOverLimitConfirmDialog = false
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
                    Brush.horizontalGradient(
                        listOf(DarkGreen, PrimaryGreen)
                    )
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(PrimaryGreen.copy(alpha = 0.2f))
                .padding(paddingValues)
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { hideKeyboard() })
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

                when {
                    uiState.isLoading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = DarkGreen)
                        }
                    }

                    uiState.vehicleList.isEmpty() -> {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = WhiteColor),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 40.dp, horizontal = 20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocalShipping,
                                    contentDescription = null,
                                    tint = DarkGreen,
                                    modifier = Modifier.size(56.dp)
                                )
                                Text(
                                    text = "No Vehicle Available",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DarkGreen
                                )
                                Text(
                                    text = "कोई वाहन उपलब्ध नहीं है",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = DarkGreen.copy(alpha = 0.75f)
                                )
                                Text(
                                    text = "Please contact Plant",
                                    fontSize = 13.sp,
                                    color = Color.Gray,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "कृपया प्लांट से संपर्क करें",
                                    fontSize = 13.sp,
                                    color = Color.Gray,
                                    textAlign = TextAlign.Center
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                OutlinedButton(
                                    onClick = { viewModel.refreshVehicles() },
                                    modifier = Modifier.height(40.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, DarkGreen)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = null,
                                        tint = DarkGreen,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Refresh",
                                        color = DarkGreen,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }

                    else -> {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = WhiteColor),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "Vehicle Details",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = DarkGreen
                                )

                                ModernSearchableDropdown(
                                    searchText = uiState.vehicleSearchText,
                                    onSearchTextChange = { viewModel.updateVehicleSearchText(it) },
                                    onClearClick = {
                                        viewModel.clearSelectedVehicle()
                                        unloadingRows.clear()
                                        unloadingRows.add(UnloadingRow(srNo = 1))
                                        remarkText = ""
                                        overLimitPendingIndex = null
                                        showOverLimitConfirmDialog = false
                                        hideKeyboard()
                                    },
                                    label = "Select Vehicle",
                                    items = uiState.filteredVehicleList,
                                    getItemLabel = { it.vehicleNo },
                                    getItemId = { it.gatePass },
                                    onItemSelected = { vehicle ->
                                        viewModel.selectVehicle(vehicle.vehicleNo, vehicle.gatePass)
                                        hideKeyboard()
                                    },
                                    onDropdownDismiss = { hideKeyboard() },
                                    accentColor = DarkGreen,
                                    noResultsMessage = "No vehicles found",
                                    itemIcon = Icons.Default.LocalShipping,
                                    itemIconColor = DarkGreen,
                                    itemTextColor = Color.Black,
                                    modifier = Modifier.fillMaxWidth(),
                                    isError = uiState.showValidationErrors && uiState.selectedVehicle.isBlank(),
                                    errorMessage = if (uiState.showValidationErrors && uiState.selectedVehicle.isBlank())
                                        "Please select a vehicle" else null
                                )
                                if (uiState.selectedVehicle.isNotBlank()) {

                                    if (uiState.isLoadingGrossData) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 12.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            CircularProgressIndicator(
                                                color = DarkGreen,
                                                strokeWidth = 2.dp,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    }

                                    uiState.selectedGrossData?.let { gross ->

                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(8.dp),
                                            colors = CardDefaults.cardColors(
                                                containerColor = PrimaryGreen.copy(alpha = 0.08f)
                                            ),
                                            border = BorderStroke(1.dp, PrimaryGreen.copy(alpha = 0.35f))
                                        ) {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(10.dp),
                                                verticalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    text = "Gross Entry Details",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = DarkGreen
                                                )

                                                GrossInfoRow("Ws No", gross.gatePass)
                                                GrossInfoRow("Indent No", gross.labourSlipNo)
                                                GrossInfoRow("Party Name", gross.growerName)
                                                GrossInfoRow("Transporter", gross.transpName)
                                                GrossInfoRow("Weight (WT)", "${gross.wt} Quintal")
                                                GrossInfoRow("Date", gross.date)
                                                GrossInfoRow("Driver Name", gross.driverName)
                                                GrossInfoRow("Driver Mobile", gross.driverMobile)
                                            }
                                        }
                                    }

                                    HorizontalDivider(
                                        modifier = Modifier.fillMaxWidth(),
                                        color = DarkGreen.copy(alpha = 0.20f)
                                    )

                                    val godownLocked = uiState.godownLocked

                                    CompactHintDropdown(
                                        value = uiState.selectedGodown?.name ?: "",
                                        expanded = if (godownLocked) false else godownExpanded,
                                        onExpandedChange = { if (!godownLocked) godownExpanded = it },
                                        onItemSelected = { selectedName ->
                                            if (!godownLocked) {
                                                val godown = uiState.godownList.find { it.name == selectedName }
                                                if (godown != null) viewModel.selectGodown(godown)
                                            }
                                            godownExpanded = false
                                            hideKeyboard()
                                        },
                                        items = uiState.godownList.map { it.name },
                                        hint = "Select Godown",
                                        accentColor = if (godownLocked) Color.Gray else DarkGreen,
                                        iconColor   = if (godownLocked) Color.Gray else DarkGreen,
                                        textColor   = if (godownLocked) Color.Gray else Color.Black,
                                        icon = Icons.Default.Warehouse,
                                        isError = !godownLocked &&
                                                uiState.showValidationErrors &&
                                                uiState.selectedGodown == null,
                                        errorMessage = "कृपया गोदाम चुनें"
                                    )


                                    HorizontalDivider(
                                        modifier = Modifier.fillMaxWidth(),
                                        color = DarkGreen.copy(alpha = 0.20f)
                                    )

                                    val thekedarLocked = uiState.thekedarLocked

                                    CompactHintDropdown(
                                        value = uiState.selectedThekedar?.accountName ?: "",
                                        expanded = if (thekedarLocked) false else thekedarExpanded,
                                        onExpandedChange = { if (!thekedarLocked) thekedarExpanded = it },
                                        onItemSelected = { selectedName ->
                                            if (!thekedarLocked) {
                                                val thekedar =
                                                    uiState.thekedarList.find { it.accountName == selectedName }
                                                if (thekedar != null) {
                                                    viewModel.selectThekedar(thekedar)
                                                }
                                            }
                                            thekedarExpanded = false
                                            hideKeyboard()
                                        },
                                        items = uiState.thekedarList.map { it.accountName },
                                        hint = "Select Thekedar",
                                        accentColor = if (thekedarLocked) Color.Gray else DarkGreen,
                                        iconColor   = if (thekedarLocked) Color.Gray else DarkGreen,
                                        textColor   = if (thekedarLocked) Color.Gray else Color.Black,
                                        icon = Icons.Default.Person,
                                        isError = !thekedarLocked &&
                                                uiState.showValidationErrors &&
                                                uiState.selectedThekedar == null,
                                        errorMessage = "कृपया ठेकेदार चुनें"
                                    )

                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))


                        if (
                            uiState.selectedVehicle.isNotBlank() &&
                            unloadingRows.isNotEmpty()
                        ) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = WhiteColor),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Start Unloading",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = DarkGreen
                                        )

                                        Text(
                                            text = "Total Rows: ${unloadingRows.size}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = DarkGreen.copy(alpha = 0.75f)
                                        )
                                    }

                                    if (uiState.hasPreviousUnloading) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(
                                                    color = DarkOrange.copy(alpha = 0.12f),
                                                    shape = RoundedCornerShape(6.dp)
                                                )
                                                .padding(horizontal = 8.dp, vertical = 6.dp)
                                        ) {
                                            Text(
                                                text = "Previously saved entries loaded below. " +
                                                        "Add new rows if needed.",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = DarkOrange
                                            )
                                        }
                                    }

                                    HorizontalDivider(color = DarkGreen.copy(alpha = 0.30f))

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(
                                                color = PrimaryGreen.copy(alpha = 0.12f),
                                                shape = RoundedCornerShape(6.dp)
                                            )
                                            .padding(vertical = 6.dp, horizontal = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Sr.No",
                                            modifier = Modifier.weight(0.5f),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = DarkGreen,
                                            textAlign = TextAlign.Center
                                        )
                                        Text(
                                            text = "Bags",
                                            modifier = Modifier.weight(1.4f),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = DarkGreen,
                                            textAlign = TextAlign.Center
                                        )
                                        Text(
                                            text = "Moisture",
                                            modifier = Modifier.weight(1.4f),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = DarkGreen,
                                            textAlign = TextAlign.Center
                                        )
                                        Spacer(modifier = Modifier.width(30.dp))
                                    }

                                    unloadingRows.forEachIndexed { index, row ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 1.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {

                                            Box(
                                                modifier = Modifier.weight(0.5f),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(24.dp)
                                                        .background(
                                                            color = PrimaryGreen.copy(alpha = 0.18f),
                                                            shape = CircleShape
                                                        ),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = "${index + 1}",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = DarkGreen
                                                    )
                                                }
                                            }

                                            Box(
                                                modifier = Modifier
                                                    .weight(1.5f)
                                                    .padding(horizontal = 3.dp)
                                            ) {
                                                CompactInputField(
                                                    value = row.bags,
                                                    onValueChange = { newValue ->
                                                        val filtered = newValue.filter { it.isDigit() }
                                                        unloadingRows[index] = row.copy(bags = filtered)
                                                    },
                                                    hint = "Bags",
                                                    modifier = Modifier.fillMaxWidth(),
                                                    accentColor = Orange,
                                                    keyboardOptions = KeyboardOptions(
                                                        keyboardType = KeyboardType.Number,
                                                        imeAction = ImeAction.Next
                                                    )
                                                )
                                            }

                                            // Moisture field — turns red if over the (effective) rejection limit
                                            val moistureLimit = effectiveRejectMoisture
                                            val moistureValue = row.moisture.toDoubleOrNull() ?: 0.0
                                            val isOverLimit =
                                                moistureLimit > 0.0 &&
                                                        row.moisture.isNotBlank() &&
                                                        moistureValue > moistureLimit

                                            Box(
                                                modifier = Modifier
                                                    .weight(1.5f)
                                                    .padding(horizontal = 3.dp)
                                            ) {
                                                CompactInputField(
                                                    value = row.moisture,
                                                    onValueChange = { newValue ->
                                                        val filtered = newValue.filter { it.isDigit() || it == '.' }
                                                        if (filtered.count { it == '.' } <= 1) {
                                                            unloadingRows[index] = row.copy(moisture = filtered)
                                                        }
                                                    },
                                                    hint = "Moisture",
                                                    modifier = Modifier.fillMaxWidth(),
                                                    accentColor = if (isOverLimit) Color(0xFFE53935) else Orange,
                                                    keyboardOptions = KeyboardOptions(
                                                        keyboardType = KeyboardType.Decimal,
                                                        imeAction = ImeAction.Done
                                                    )
                                                )
                                            }

                                            val isPersisted = row.isPersisted
                                            val isBusy = row.isSaving
                                            val canSubmitRow = !isBusy &&
                                                    row.bags.isNotBlank() &&
                                                    row.moisture.isNotBlank()

                                            Box(
                                                modifier = Modifier
                                                    .size(28.dp)
                                                    .padding(start = 2.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(26.dp)
                                                        .clip(CircleShape)
                                                        .background(
                                                            color = when {
                                                                isBusy -> DarkOrange.copy(alpha = 0.6f)
                                                                canSubmitRow -> if (isPersisted) PrimaryGreen else Orange
                                                                else -> Orange.copy(alpha = 0.35f)
                                                            },
                                                            shape = CircleShape
                                                        )
                                                        .clickable(
                                                            enabled = !isBusy && canSubmitRow,
                                                            onClick = {
                                                                if (!canSubmitRow) {
                                                                    Toast.makeText(
                                                                        context,
                                                                        "Please fill Bags and Moisture",
                                                                        Toast.LENGTH_SHORT
                                                                    ).show()
                                                                    hideKeyboard()
                                                                    return@clickable
                                                                }

                                                                val limit = effectiveRejectMoisture
                                                                val moistureVal =
                                                                    unloadingRows[index].moisture.toDoubleOrNull() ?: 0.0
                                                                val overLimit =
                                                                    limit > 0.0 && moistureVal > limit

                                                                if (overLimit) {
                                                                    // ask for confirmation first
                                                                    overLimitPendingIndex = index
                                                                    showOverLimitConfirmDialog = true
                                                                    hideKeyboard()
                                                                    return@clickable
                                                                }

                                                                // ---- normal path: save / update immediately ----
                                                                performRowSave(
                                                                    index = index,
                                                                    isPersisted = isPersisted,
                                                                    unloadingRows = unloadingRows,
                                                                    viewModel = viewModel,
                                                                    context = context,
                                                                    hideKeyboard = hideKeyboard
                                                                )
                                                            }
                                                        ),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    if (isBusy) {
                                                        CircularProgressIndicator(
                                                            modifier = Modifier.size(12.dp),
                                                            strokeWidth = 1.5.dp,
                                                            color = Color.White
                                                        )
                                                    } else {
                                                        Icon(
                                                            imageVector = Icons.Default.Done,
                                                            contentDescription = if (isPersisted) "Update" else "Save",
                                                            tint = Color.White,
                                                            modifier = Modifier.size(14.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    if (
                                        uiState.showRejectLimitBanner &&
                                        effectiveRejectMoisture.isFinite() &&
                                        effectiveRejectMoisture > 0.0
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(
                                                    color = Color(0xFFE53935).copy(alpha = 0.10f),
                                                    shape = RoundedCornerShape(6.dp)
                                                )
                                                .padding(horizontal = 8.dp, vertical = 6.dp)
                                        ) {
                                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                Text(
                                                    text = "इस मॉइस्चर से ऊपर गाड़ी रिजेक्ट होगी — " +
                                                            String.format("%.2f %%", effectiveRejectMoisture),
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = Color(0xFFE53935)
                                                )

                                                Text(
                                                    text = "Reject करने के लिए moisture वाली जगह moisture डालिए " +
                                                            "और फिर Reject button दिखेगा उस पर click कीजिए",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = Color(0xFFE53935).copy(alpha = 0.85f)
                                                )
                                            }
                                        }
                                    }

                                }
                            }
                        }

                        val moistureValues = unloadingRows
                            .filter { it.isPersisted }
                            .mapNotNull { it.moisture.toDoubleOrNull() }
                            .filter { it > 0.0 }

                        val totalBags = unloadingRows
                            .filter { it.isPersisted }
                            .sumOf { it.bags.toIntOrNull() ?: 0 }

                        val hasAnyData = unloadingRows.any { it.isPersisted }

                        if (hasAnyData) {
                            val highestMoisture = moistureValues.maxOrNull() ?: 0.0
                            val averageMoisture =
                                if (moistureValues.isNotEmpty()) moistureValues.average() else 0.0

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {

                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(60.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = CardDefaults.cardColors(containerColor = WhiteColor),
                                    border = BorderStroke(1.dp, PrimaryGreen.copy(alpha = 0.60f)),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(horizontal = 6.dp, vertical = 6.dp),
                                        verticalArrangement = Arrangement.Center,
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = "Total Bags",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = PrimaryGreen,
                                            textAlign = TextAlign.Center
                                        )
                                        Spacer(modifier = Modifier.height(3.dp))
                                        Text(
                                            text = "$totalBags",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.Black
                                        )
                                    }
                                }

                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(60.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = CardDefaults.cardColors(containerColor = WhiteColor),
                                    border = BorderStroke(1.dp, PrimaryGreen.copy(alpha = 0.60f)),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(horizontal = 6.dp, vertical = 6.dp),
                                        verticalArrangement = Arrangement.Center,
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = "Highest Moisture",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = PrimaryGreen,
                                            textAlign = TextAlign.Center
                                        )
                                        Spacer(modifier = Modifier.height(3.dp))
                                        Text(
                                            text = String.format("%.2f %%", highestMoisture),
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.Black
                                        )
                                    }
                                }

                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(60.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = CardDefaults.cardColors(containerColor = WhiteColor),
                                    border = BorderStroke(1.dp, PrimaryGreen.copy(alpha = 0.60f)),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(horizontal = 6.dp, vertical = 6.dp),
                                        verticalArrangement = Arrangement.Center,
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = "Average Moisture",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = PrimaryGreen,
                                            textAlign = TextAlign.Center
                                        )
                                        Spacer(modifier = Modifier.height(3.dp))
                                        Text(
                                            text = String.format("%.2f %%", averageMoisture),
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.Black
                                        )
                                    }
                                }
                            }

                            CompactInputField(
                                value = remarkText,
                                onValueChange = { newValue ->
                                    remarkText = newValue.take(500)
                                },
                                hint = "Enter Remark (Optional)",
                                modifier = Modifier.fillMaxWidth(),
                                accentColor = DarkGreen,
                                icon = Icons.Default.EditNote,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Text,
                                    imeAction = ImeAction.Done,
                                    capitalization = KeyboardCapitalization.Sentences
                                ),
                                keyboardActions = KeyboardActions(
                                    onDone = { hideKeyboard() }
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                        }

                        val hasAnyOverLimitRow = effectiveRejectMoisture > 0.0 &&
                                unloadingRows.any { r ->
                                    r.moisture.isNotBlank() &&
                                            (r.moisture.toDoubleOrNull() ?: 0.0) >
                                            effectiveRejectMoisture
                                }

                        if (uiState.selectedVehicle.isNotBlank()) {

                            if (!hasAnyOverLimitRow) {
                                Button(
                                    onClick = {
                                        hideKeyboard()

                                        if (!viewModel.isFormValid()) {
                                            viewModel.setShowValidationErrors(true)
                                            Toast.makeText(
                                                context,
                                                "Please fill all required fields",
                                                Toast.LENGTH_LONG
                                            ).show()
                                            return@Button
                                        }

                                        val savedRows = unloadingRows.filter { it.isPersisted }

                                        val hasUnsavedRow = unloadingRows.any {
                                            !it.isPersisted && (it.bags.isNotBlank() || it.moisture.isNotBlank())
                                        }

                                        if (savedRows.isEmpty() || hasUnsavedRow) {
                                            showEmptyRowsAlert = true
                                            return@Button
                                        }

                                        showConfirmationDialog = true
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                                    contentPadding = PaddingValues()
                                ) {
                                    Card(
                                        modifier = Modifier.fillMaxSize(),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(
                                                    Brush.horizontalGradient(
                                                        listOf(PrimaryGreen, DarkGreen)
                                                    )
                                                ),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Text(
                                                text = "Submit",
                                                color = Color.White,
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }

                            if (hasAnyOverLimitRow) {

                                Spacer(modifier = Modifier.height(8.dp))

                                Button(
                                    onClick = {
                                        hideKeyboard()

                                        if (!viewModel.isFormValid()) {
                                            viewModel.setShowValidationErrors(true)
                                            Toast.makeText(
                                                context,
                                                "Please fill all required fields",
                                                Toast.LENGTH_LONG
                                            ).show()
                                            return@Button
                                        }

                                        selectedRejectCode = "0"
                                        selectedRejectName = ""
                                        showRejectReasonDialog = true
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                                    contentPadding = PaddingValues()
                                ) {
                                    Card(
                                        modifier = Modifier.fillMaxSize(),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(
                                                    Brush.horizontalGradient(
                                                        listOf(Color(0xFFE53935), Color(0xFFB71C1C))
                                                    )
                                                ),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Text(
                                                text = "Reject",
                                                color = Color.White,
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        if (showOverLimitConfirmDialog && overLimitPendingIndex != null) {
                            val pendingRow = unloadingRows.getOrNull(overLimitPendingIndex!!)
                            AlertDialog(
                                onDismissRequest = {
                                    showOverLimitConfirmDialog = false
                                    overLimitPendingIndex = null
                                },
                                confirmButton = {
                                    TextButton(
                                        onClick = {
                                            showOverLimitConfirmDialog = false
                                            overLimitPendingIndex = null
                                        }
                                    ) {
                                        Text("OK", color = Color(0xFFE53935), fontWeight = FontWeight.Bold)
                                    }
                                },
                                title = {
                                    Text(
                                        "Moisture Limit Exceeded",
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFE53935)
                                    )
                                },
                                text = {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(
                                            text = "इस पंक्ति में मॉइस्चर सीमा (" +
                                                    String.format(
                                                        "%.2f %%",
                                                        effectiveRejectMoisture
                                                    ) +
                                                    ") से अधिक है।",
                                            fontSize = 13.sp,
                                            color = Color.DarkGray
                                        )
                                        HorizontalDivider(color = Color(0xFFE53935).copy(alpha = 0.20f))
                                        Text(
                                            text = "Moisture of this row (" +
                                                    (pendingRow?.moisture ?: "-") +
                                                    "%) is above the rejection limit. " +
                                                    "Please correct the moisture value — this row cannot be saved as-is.",
                                            fontSize = 13.sp,
                                            color = Color.DarkGray
                                        )
                                    }
                                },
                                containerColor = WhiteColor
                            )
                        }

                        if (showEmptyRowsAlert) {
                            AlertDialog(
                                onDismissRequest = { showEmptyRowsAlert = false },
                                confirmButton = {
                                    TextButton(onClick = { showEmptyRowsAlert = false }) {
                                        Text(
                                            "OK",
                                            color = PrimaryGreen,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                },
                                title = {
                                    Text(
                                        text = "अधूरी जानकारी / Incomplete Entry",
                                        fontWeight = FontWeight.Bold,
                                        color = PrimaryGreen
                                    )
                                },
                                text = {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(
                                            text = "पहले भरे हुए Bags और Moisture को Done बटन से अपलोड करें, " +
                                                    "उसके बाद ही Submit बटन क्लिक करें।",
                                            fontSize = 13.sp,
                                            color = Color.DarkGray
                                        )
                                        HorizontalDivider(color = DarkGreen.copy(alpha = 0.20f))
                                        Text(
                                            text = "Please upload the filled Bags and Moisture " +
                                                    "using the Done button first, then tap Submit.",
                                            fontSize = 13.sp,
                                            color = Color.DarkGray
                                        )
                                    }
                                },
                                containerColor = WhiteColor
                            )
                        }

                        if (showConfirmationDialog) {
                            UnloadingConfirmationDialog(
                                vehicleNo = uiState.selectedVehicle,
                                godownName = uiState.selectedGodown?.name ?: "",
                                thekedarName = uiState.selectedThekedar?.accountName ?: "",
                                rows = unloadingRows.filter { it.isPersisted },
                                onDismiss = { showConfirmationDialog = false },
                                onSubmit = {
                                    showConfirmationDialog = false
                                    showSubmitConfirmDialog = true
                                }
                            )
                        }

                        if (showSubmitConfirmDialog) {
                            AlertDialog(
                                onDismissRequest = { showSubmitConfirmDialog = false },
                                confirmButton = {
                                    TextButton(
                                        onClick = {
                                            showSubmitConfirmDialog = false
                                            showSubmittingOverlay = true

                                            val persistedRows = unloadingRows.filter { it.isPersisted }
                                            val moistureList = persistedRows
                                                .mapNotNull { it.moisture.toDoubleOrNull() }
                                                .filter { it > 0.0 }
                                            val finalAvg = if (moistureList.isNotEmpty()) moistureList.average() else 0.0
                                            val finalHigh = moistureList.maxOrNull() ?: 0.0

                                            viewModel.submitUnloadingEntry(
                                                gatePass = uiState.selectedGatePass,
                                                godownCode = uiState.selectedGodown?.code ?: "",
                                                finalAvg = finalAvg,
                                                finalHigh = finalHigh,
                                                remark = remarkText
                                            ) { success, message ->
                                                showSubmittingOverlay = false
                                                if (success) {
                                                    Toast.makeText(
                                                        context,
                                                        "Unloading entry submitted successfully",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                    unloadingRows.clear()
                                                    unloadingRows.add(UnloadingRow(srNo = 1))
                                                    remarkText = ""
                                                    overLimitPendingIndex = null
                                                    showOverLimitConfirmDialog = false
                                                    viewModel.resetForm()
                                                } else {
                                                    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                                                }
                                            }
                                        }
                                    ) {
                                        Text("Yes", color = PrimaryGreen, fontWeight = FontWeight.Bold)
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showSubmitConfirmDialog = false }) {
                                        Text("Cancel", color = Color.Gray)
                                    }
                                },
                                title = {
                                    Text(
                                        "Are you sure?",
                                        fontWeight = FontWeight.Bold,
                                        color = PrimaryGreen
                                    )
                                },
                                text = {
                                    Text("Do you want to submit this unloading entry?")
                                },
                                containerColor = WhiteColor
                            )
                        }

                        if (showRejectConfirmDialog) {
                            AlertDialog(
                                onDismissRequest = { showRejectConfirmDialog = false },
                                confirmButton = {
                                    TextButton(
                                        onClick = {
                                            showRejectConfirmDialog = false
                                            showSubmittingOverlay = true

                                            val persistedRows = unloadingRows.filter { it.isPersisted }
                                            val moistureList = persistedRows
                                                .mapNotNull { it.moisture.toDoubleOrNull() }
                                                .filter { it > 0.0 }
                                            val finalAvg = if (moistureList.isNotEmpty()) moistureList.average() else 0.0
                                            val finalHigh = moistureList.maxOrNull() ?: 0.0

                                            val rejectMoisture = unloadingRows
                                                .asSequence()
                                                .mapNotNull { it.moisture.toDoubleOrNull() }
                                                .filter { it > effectiveRejectMoisture }
                                                .maxOrNull()
                                                ?: finalHigh

                                            viewModel.rejectUnloadingEntry(
                                                gatePass = uiState.selectedGatePass,
                                                godownCode = uiState.selectedGodown?.code ?: "",
                                                rejectCode = selectedRejectCode,
                                                finalAvg = finalAvg,
                                                finalHigh = finalHigh,
                                                rejectMoisture = rejectMoisture,
                                                remark = remarkText
                                            ) { success, message ->
                                                showSubmittingOverlay = false
                                                if (success) {
                                                    Toast.makeText(
                                                        context,
                                                        "Unloading entry rejected",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                    unloadingRows.clear()
                                                    unloadingRows.add(UnloadingRow(srNo = 1))
                                                    remarkText = ""
                                                    overLimitPendingIndex = null
                                                    showOverLimitConfirmDialog = false
                                                    viewModel.resetForm()
                                                } else {
                                                    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                                                }
                                            }
                                        }
                                    ) {
                                        Text("Yes", color = Color(0xFFE53935), fontWeight = FontWeight.Bold)
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showRejectConfirmDialog = false }) {
                                        Text("Cancel", color = Color.Gray)
                                    }
                                },
                                title = {
                                    Text(
                                        "Are you sure?",
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFE53935)
                                    )
                                },
                                text = {
                                    Text("Do you want to reject this unloading entry?")
                                },
                                containerColor = WhiteColor
                            )
                        }

                        if (showSubmittingOverlay) {
                            Dialog(
                                onDismissRequest = { /* block dismiss */ },
                                properties = DialogProperties(
                                    dismissOnBackPress = false,
                                    dismissOnClickOutside = false
                                )
                            ) {
                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = WhiteColor)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(20.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        CircularProgressIndicator(
                                            color = PrimaryGreen,
                                            strokeWidth = 2.dp,
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Text(
                                            text = "Saving… / सेव हो रहा है…",
                                            fontSize = 14.sp,
                                            color = DarkGreen,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }

                        if (showRejectReasonDialog) {

                            val overLimitMoistures: List<String> =
                                unloadingRows
                                    .filter { r ->
                                        r.moisture.isNotBlank() &&
                                                (r.moisture.toDoubleOrNull() ?: 0.0) >
                                                effectiveRejectMoisture
                                    }
                                    .mapNotNull { r ->
                                        val v = r.moisture.toDoubleOrNull()
                                        if (v != null) String.format("%.2f %%", v) else null
                                    }
                                    .distinct()

                            AlertDialog(
                                onDismissRequest = { showRejectReasonDialog = false },
                                confirmButton = {
                                    TextButton(
                                        enabled = uiState.rejectList.isNotEmpty(),
                                        onClick = {
                                            if (selectedRejectName.isBlank() ||
                                                selectedRejectCode.isBlank() ||
                                                selectedRejectCode == "0"
                                            ) {
                                                Toast.makeText(
                                                    context,
                                                    "कृपया रिजेक्ट कारण चुनें",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                                return@TextButton
                                            }

                                            showRejectReasonDialog = false
                                            showRejectConfirmDialog = true
                                        }
                                    ) {
                                        Text(
                                            "Submit",
                                            color = if (uiState.rejectList.isNotEmpty()) ErrorRed else Color.Gray,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showRejectReasonDialog = false }) {
                                        Text("Cancel", color = Color.Gray)
                                    }
                                },
                                title = {
                                    Text(
                                        "Select Reject Reason",
                                        fontWeight = FontWeight.Bold,
                                        color = ErrorRed
                                    )
                                },
                                text = {
                                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {

                                        if (uiState.rejectList.isEmpty()) {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 16.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.spacedBy(12.dp)
                                            ) {
                                                CircularProgressIndicator(
                                                    color = PrimaryGreen,
                                                    strokeWidth = 2.dp,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                                TextButton(onClick = { viewModel.loadRejectList() }) {
                                                    Text(
                                                        "Retry",
                                                        color = PrimaryGreen,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        } else {
                                            CompactHintDropdown(
                                                value = selectedRejectName,
                                                expanded = rejectExpanded,
                                                onExpandedChange = { rejectExpanded = it },
                                                onItemSelected = { name ->
                                                    val item = uiState.rejectList.find { it.name == name }
                                                    if (item != null) {
                                                        selectedRejectCode = item.code
                                                        selectedRejectName = item.name
                                                    }
                                                    rejectExpanded = false
                                                    hideKeyboard()
                                                },
                                                items = uiState.rejectList.map { it.name },
                                                hint = "Select Reject Reason",
                                                accentColor = ErrorRed,
                                                iconColor = ErrorRed,
                                                textColor = Color.Black,
                                                icon = Icons.Default.Close,
                                                isError = false,
                                                errorMessage = null
                                            )

                                            if (selectedRejectName.isBlank()) {
                                                Text(
                                                    text = "कृपया सबमिट करने से पहले कारण चुनें",
                                                    fontSize = 11.sp,
                                                    color = ErrorRed,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }

                                            if (overLimitMoistures.isNotEmpty()) {
                                                Text(
                                                    text = "रिजेक्ट मॉइस्चर" +
                                                            " (" + overLimitMoistures.joinToString(", ") + ")",
                                                    fontSize = 11.sp,
                                                    color = Color.Black,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                        }
                                    }
                                },
                                containerColor = WhiteColor
                            )
                        }
                        Spacer(modifier = Modifier.height(300.dp))

                    }
                }
            }
        }
    }
}


private fun performRowSave(
    index: Int,
    isPersisted: Boolean,
    unloadingRows: SnapshotStateList<UnloadingRow>,
    viewModel: UnloadingViewModel,
    context: Context,
    hideKeyboard: () -> Unit,
) {
    if (index !in unloadingRows.indices) return

    unloadingRows[index] = unloadingRows[index].copy(isSaving = true)

    if (isPersisted) {
        viewModel.updateRowInDb(
            row = unloadingRows[index],
            allRows = unloadingRows.toList()
        ) { success, message ->
            if (index !in unloadingRows.indices) return@updateRowInDb
            unloadingRows[index] = unloadingRows[index].copy(isSaving = false)
            Toast.makeText(
                context,
                if (success) "Row updated" else message,
                if (success) Toast.LENGTH_SHORT else Toast.LENGTH_LONG
            ).show()
            hideKeyboard()
        }
    } else {
        viewModel.addRowToDb(
            row = unloadingRows[index],
            allRows = unloadingRows.toList()
        ) { newCode, message ->
            if (index !in unloadingRows.indices) return@addRowToDb
            if (newCode > 0) {
                unloadingRows[index] = unloadingRows[index].copy(
                    uCode = newCode,
                    isSaving = false
                )
                if (index == unloadingRows.lastIndex) {
                    unloadingRows.add(UnloadingRow(srNo = unloadingRows.size + 1))
                }
                Toast.makeText(context, "Row saved", Toast.LENGTH_SHORT).show()
            } else {
                unloadingRows[index] = unloadingRows[index].copy(isSaving = false)
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            }
            hideKeyboard()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnloadingConfirmationDialog(
    vehicleNo: String,
    godownName: String,
    thekedarName: String,
    rows: List<UnloadingRow>,
    onDismiss: () -> Unit,
    onSubmit: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .heightIn(max = 560.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = WhiteColor),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(listOf(DarkGreen, PrimaryGreen))
                        )
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Text(
                        text = "Confirm Details",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.CenterStart)
                    )

                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .size(28.dp)
                            .background(Color.White.copy(alpha = 0.20f), CircleShape)
                            .clickable { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {

                    ConfirmRow(label = "Vehicle", value = vehicleNo)
                    ConfirmRow(label = "Godown", value = godownName)
                    ConfirmRow(label = "Thekedar", value = thekedarName)

                    HorizontalDivider(color = DarkGreen.copy(alpha = 0.20f))

                    Text(
                        text = "Unloading Details",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkGreen
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = PrimaryGreen.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(6.dp)
                            )
                            .padding(vertical = 6.dp, horizontal = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Sr",
                            modifier = Modifier.weight(0.5f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = DarkGreen,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            "Bags",
                            modifier = Modifier.weight(1f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = DarkGreen,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            "Moisture",
                            modifier = Modifier.weight(1f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = DarkGreen,
                            textAlign = TextAlign.Center
                        )
                    }

                    rows.forEachIndexed { index, row ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "${index + 1}",
                                modifier = Modifier.weight(0.5f),
                                fontSize = 12.sp,
                                color = Color.Black,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                row.bags.ifBlank { "-" },
                                modifier = Modifier.weight(1f),
                                fontSize = 12.sp,
                                color = Color.Black,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                row.moisture.ifBlank { "-" },
                                modifier = Modifier.weight(1f),
                                fontSize = 12.sp,
                                color = Color.Black,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    val totalBags = rows.sumOf { it.bags.toIntOrNull() ?: 0 }
                    val moistures = rows.mapNotNull { it.moisture.toDoubleOrNull() }
                    val avgMoisture = if (moistures.isNotEmpty()) moistures.average() else 0.0
                    val maxMoisture = moistures.maxOrNull() ?: 0.0

                    HorizontalDivider(color = DarkGreen.copy(alpha = 0.20f))

                    ConfirmRow(label = "Total Bags", value = "$totalBags")
                    ConfirmRow(
                        label = "Avg. Moisture",
                        value = String.format("%.2f %%", avgMoisture)
                    )
                    ConfirmRow(
                        label = "Highest. Moisture",
                        value = String.format("%.2f %%", maxMoisture)
                    )
                }

                HorizontalDivider(color = DarkGreen.copy(alpha = 0.20f))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onSubmit,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryGreen
                        )
                    ) {
                        Text(
                            "Submit",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ConfirmRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color.DarkGray,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = value.ifBlank { "-" },
            fontSize = 12.sp,
            color = Color.Black,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun GrossInfoRow(label: String, value: String) {
    if (value.isBlank() || value.equals("NULL", ignoreCase = true)) return
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = Color.DarkGray,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = value,
            fontSize = 11.sp,
            color = Color.Black,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.End,
            modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp)
        )
    }
}