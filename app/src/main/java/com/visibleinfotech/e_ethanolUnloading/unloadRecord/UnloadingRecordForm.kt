package com.visibleinfotech.e_ethanolUnloading.unloadRecord

import android.app.DatePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.visibleinfotech.e_ethanolUnloading.ui.theme.BlackColor
import com.visibleinfotech.e_ethanolUnloading.ui.theme.DarkBlue
import com.visibleinfotech.e_ethanolUnloading.ui.theme.DarkGreen
import com.visibleinfotech.e_ethanolUnloading.ui.theme.DarkYellow
import com.visibleinfotech.e_ethanolUnloading.ui.theme.ErrorRed
import com.visibleinfotech.e_ethanolUnloading.ui.theme.Golden
import com.visibleinfotech.e_ethanolUnloading.ui.theme.PrimaryGreen
import com.visibleinfotech.e_ethanolUnloading.ui.theme.WhiteColor
import com.visibleinfotech.e_ethanolUnloading.unloadRecord.models.UnloadStatus
import com.visibleinfotech.e_ethanolUnloading.unloadRecord.models.UnloadingDetailModel
import com.visibleinfotech.e_ethanolUnloading.unloadRecord.models.UnloadingRecordModel
import com.visibleinfotech.e_ethanolUnloading.utils.SetStatusBarColor
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnloadingRecordForm(
    onBack: () -> Unit = {},
    viewModel: UnloadingRecordViewModel = viewModel()
) {

    SetStatusBarColor(DarkGreen)

    val uiState by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val hideKeyboard = {
        keyboardController?.hide()
        focusManager.clearFocus(force = true)
    }

    var showFilterSheet by remember { mutableStateOf(false) }
    var fromDate by remember { mutableStateOf("") }
    var toDate by remember { mutableStateOf("") }
    var selectedStatus by remember { mutableStateOf<UnloadApprovalStatus?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var selectedDateType by remember { mutableStateOf<DateType?>(null) }

    var tempFromDate by remember { mutableStateOf("") }
    var tempToDate by remember { mutableStateOf("") }
    var tempSelectedStatus by remember { mutableStateOf<UnloadApprovalStatus?>(null) }

    val pullToRefreshState = rememberPullToRefreshState()
    var isRefreshing by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.loadUnloadingRecords() }

    LaunchedEffect(isRefreshing) {
        if (isRefreshing) {
            viewModel.loadUnloadingRecords()
            viewModel.filterUnloadingRecords(
                fromDate.ifEmpty { null },
                toDate.ifEmpty { null },
                selectedStatus
            )
            isRefreshing = false
        }
    }

    LaunchedEffect(showFilterSheet) {
        if (showFilterSheet) {
            tempFromDate = fromDate
            tempToDate = toDate
            tempSelectedStatus = selectedStatus
        }
    }

    val hasFilter = fromDate.isNotEmpty() || toDate.isNotEmpty() || selectedStatus != null

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Unloading Record",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { hideKeyboard(); onBack() }) {
                        Icon(painterResource(android.R.drawable.ic_menu_revert), null, tint = Color.White)
                    }
                },
                actions = {
                    FilterChip(
                        selected = hasFilter,
                        onClick = { hideKeyboard(); showFilterSheet = true },
                        label = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.FilterList, null, modifier = Modifier.size(16.dp), tint = Golden)
                                Spacer(Modifier.width(4.dp))
                                Text("Filter", fontSize = 13.sp, color = Golden)
                            }
                        },
                        modifier = Modifier.padding(end = 8.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = DarkGreen,
                            selectedLabelColor = Color.White,
                            containerColor = PrimaryGreen.copy(alpha = 0.3f),
                            labelColor = DarkGreen
                        )
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                modifier = Modifier.background(Brush.horizontalGradient(listOf(DarkGreen, PrimaryGreen)))
            )
        }
    ) { paddingValues ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(PrimaryGreen.copy(0.2f))
                .padding(paddingValues)
                .pointerInput(Unit) { detectTapGestures(onTap = { hideKeyboard() }) }
        ) {
            PullToRefreshBox(
                state = pullToRefreshState,
                isRefreshing = isRefreshing,
                onRefresh = { isRefreshing = true; hideKeyboard() },
                modifier = Modifier.fillMaxSize()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Spacer(Modifier.height(4.dp))

                    // ---------- SEARCH BAR ----------
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White,
                        shadowElevation = 1.dp,
                        border = BorderStroke(
                            1.dp,
                            if (uiState.searchQuery.isNotEmpty()) DarkGreen else DarkGreen.copy(alpha = 0.5f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Search, null, tint = DarkGreen, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))

                            BasicTextField(
                                value = uiState.searchQuery,
                                onValueChange = { viewModel.searchUnloadingRecords(it) },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(onSearch = { hideKeyboard() }),
                                decorationBox = { inner ->
                                    Box {
                                        if (uiState.searchQuery.isEmpty()) {
                                            Text(
                                                "Search by Vehicle, WS No, Party, Godown",
                                                fontSize = 12.sp,
                                                color = Color.Gray
                                            )
                                        }
                                        inner()
                                    }
                                }
                            )

                            if (uiState.searchQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = { viewModel.searchUnloadingRecords(""); hideKeyboard() },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.Close, null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }

                    // ---------- ACTIVE FILTERS ----------
                    if (hasFilter) {
                        CompactActiveFilterSection(
                            fromDate = fromDate,
                            toDate = toDate,
                            selectedStatus = selectedStatus,
                            onClearAll = {
                                fromDate = ""; toDate = ""; selectedStatus = null
                                viewModel.filterUnloadingRecords(null, null, null)
                                hideKeyboard()
                            },
                            onClearDate = {
                                fromDate = ""; toDate = ""
                                viewModel.filterUnloadingRecords(null, null, selectedStatus)
                                hideKeyboard()
                            },
                            onClearStatus = {
                                selectedStatus = null
                                viewModel.filterUnloadingRecords(
                                    fromDate.ifEmpty { null },
                                    toDate.ifEmpty { null },
                                    null
                                )
                                hideKeyboard()
                            }
                        )
                    }

                    // ---------- STATS ----------
                    if (uiState.filteredRecords.isNotEmpty() && !uiState.isLoading) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            color = WhiteColor
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "📊 ${uiState.filteredRecords.size} Records",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = DarkGreen
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("✅", fontSize = 12.sp)
                                    Text(" ${uiState.approvedCount}", fontSize = 12.sp, color = Color(0xFF2E7D32))
                                    Spacer(Modifier.width(8.dp))
                                    Text("❌", fontSize = 12.sp)
                                    Text(" ${uiState.rejectedCount}", fontSize = 12.sp, color = Color.Red)
                                    Spacer(Modifier.width(8.dp))
                                    Text("⏳", fontSize = 12.sp)
                                    Text(" ${uiState.pendingCount}", fontSize = 12.sp, color = DarkYellow)
                                }
                            }
                        }
                    }

                    // ---------- LOADING ----------
                    if (uiState.isLoading) {
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = DarkGreen, modifier = Modifier.size(32.dp))
                        }
                    }

                    // ---------- EMPTY ----------
                    if (uiState.filteredRecords.isEmpty() && !uiState.isLoading) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.SearchOff, null, modifier = Modifier.size(60.dp), tint = PrimaryGreen)
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        if (uiState.searchQuery.isNotEmpty()) "No matches found" else "No records",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = DarkGreen
                                    )
                                    Text("Try adjusting filters", fontSize = 12.sp, color = Color.Gray)
                                }
                            }
                        }
                    }

                    // ---------- LIST ----------
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(
                            items = uiState.filteredRecords,
                            key = { "${it.wsNo}|${it.indentNo}|${it.vehicleNo}" }   // ← composite key
                        ) { item ->
                            UnloadingRecordCard(
                                item = item,
                                onClick = { viewModel.openDetailDialog(item) }       // ← pass onClick
                            )
                        }
                        item { Spacer(Modifier.height(240.dp)) }
                    }
                }
            }

            val selectedRecord = uiState.selectedRecord
            if (uiState.showDetailDialog && selectedRecord != null) {
                UnloadingDetailDialog(
                    record = selectedRecord,
                    rows = uiState.detailRows,
                    isLoading = uiState.isLoadingDetail,
                    status = selectedRecord.status,          // ← pass enum
                    onDismiss = { viewModel.closeDetailDialog() }
                )
            }
        }
    }

    if (showFilterSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFilterSheet = false; hideKeyboard() },
            containerColor = Color.White,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Filter Records", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = DarkGreen)
                        Text("Find unloading records", fontSize = 11.sp, color = Color.Gray)
                    }
                    Surface(
                        modifier = Modifier.size(32.dp).clickable { showFilterSheet = false; hideKeyboard() },
                        shape = CircleShape,
                        color = PrimaryGreen.copy(alpha = 0.15f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Close, null, tint = DarkGreen, modifier = Modifier.size(16.dp))
                        }
                    }
                }

                HorizontalDivider(color = PrimaryGreen.copy(alpha = 0.3f))

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.DateRange, null, tint = DarkGreen, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Date Range", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = DarkGreen)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        CompactDateCard("From", tempFromDate.ifEmpty { "Select" }, Modifier.weight(1f)) {
                            hideKeyboard(); selectedDateType = DateType.FROM; showDatePicker = true
                        }
                        CompactDateCard("To", tempToDate.ifEmpty { "Select" }, Modifier.weight(1f)) {
                            hideKeyboard(); selectedDateType = DateType.TO; showDatePicker = true
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Verified, null, tint = DarkGreen, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Status", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = DarkGreen)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CompactStatusChip("Approved",
                            tempSelectedStatus == UnloadApprovalStatus.APPROVED,
                            Color(0xFF2E7D32), Modifier.weight(1f)
                        ) {
                            tempSelectedStatus =
                                if (tempSelectedStatus == UnloadApprovalStatus.APPROVED) null
                                else UnloadApprovalStatus.APPROVED
                        }
                        CompactStatusChip("Pending",
                            tempSelectedStatus == UnloadApprovalStatus.PENDING,
                            Color(0xFFF57C00), Modifier.weight(1f)
                        ) {
                            tempSelectedStatus =
                                if (tempSelectedStatus == UnloadApprovalStatus.PENDING) null
                                else UnloadApprovalStatus.PENDING
                        }
                        CompactStatusChip("Rejected",
                            tempSelectedStatus == UnloadApprovalStatus.REJECTED,
                            Color(0xFFD32F2F), Modifier.weight(1f)
                        ) {
                            tempSelectedStatus =
                                if (tempSelectedStatus == UnloadApprovalStatus.REJECTED) null
                                else UnloadApprovalStatus.REJECTED
                        }
                    }
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = { tempFromDate = ""; tempToDate = ""; tempSelectedStatus = null; hideKeyboard() },
                        modifier = Modifier.weight(1f).height(44.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Refresh, null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Reset", fontSize = 13.sp)
                    }
                    Button(
                        onClick = {
                            fromDate = tempFromDate
                            toDate = tempToDate
                            selectedStatus = tempSelectedStatus
                            viewModel.filterUnloadingRecords(
                                fromDate.ifEmpty { null },
                                toDate.ifEmpty { null },
                                selectedStatus
                            )
                            showFilterSheet = false
                            hideKeyboard()
                        },
                        modifier = Modifier.weight(1f).height(44.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DarkGreen)
                    ) {
                        Icon(Icons.Default.Search, null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Apply", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }

    // ---------- DATE PICKER ----------
    if (showDatePicker) {
        ModernDatePickerDialog(
            onDateSelected = { selectedDate ->
                when (selectedDateType) {
                    DateType.FROM -> tempFromDate = selectedDate
                    DateType.TO   -> tempToDate = selectedDate
                    null -> {}
                }
                showDatePicker = false
                hideKeyboard()
            },
            onDismiss = { showDatePicker = false; hideKeyboard() }
        )
    }
}


@Composable
fun UnloadingRecordCard(
    item: UnloadingRecordModel,
    onClick: () -> Unit
) {
    val isUnloaded     = item.status == UnloadStatus.UNLOADED
    val isTempRejected = item.status == UnloadStatus.TEMP_REJECTED
    val isRejected     = item.status == UnloadStatus.REJECTED

    val statusColor = when {
        isRejected     -> Color(0xFFD32F2F)     // 🔴 red
        isTempRejected -> Color(0xFFEF6C00)     // 🟠 orange
        else           -> DarkGreen             // 🟢 green
    }
    val statusColorLight = when {
        isRejected     -> Color(0xFFFF6B6B)
        isTempRejected -> Color(0xFFFFB74D)
        else           -> PrimaryGreen
    }
    val statusText = when {
        isRejected     -> "REJECTED"
        isTempRejected -> "TEMP REJECTED"
        else           -> "UNLOADED"
    }
    val statusIcon = when {
        isRejected     -> "✗"
        isTempRejected -> "!"
        else           -> "✓"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.5.dp,
                brush = Brush.linearGradient(listOf(statusColor, statusColorLight)),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            statusColor.copy(alpha = 0.10f),
                            Color.White,
                            statusColor.copy(alpha = 0.06f)
                        )
                    )
                )
        ) {
            // LEFT ACCENT BAR
            Box(
                Modifier
                    .width(8.dp)
                    .height(180.dp)
                    .background(
                        Brush.verticalGradient(listOf(statusColorLight, statusColor))
                    )
            )

            Column(Modifier.weight(1f).padding(10.dp)) {

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(30.dp)
                                .background(
                                    Brush.linearGradient(listOf(statusColorLight, statusColor)),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocalShipping,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "VEHICLE NO",
                                color = Color.Gray,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = item.vehicleNo.ifBlank { "-" },
                                color = BlackColor,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color.Transparent,
                        shadowElevation = 3.dp,
                        modifier = Modifier.padding(start = 6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .background(
                                    Brush.horizontalGradient(listOf(statusColorLight, statusColor)),
                                    RoundedCornerShape(20.dp)
                                )
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = statusIcon,
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = statusText,
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 2.dp),
                    thickness = 1.dp,
                    color = statusColor.copy(alpha = 0.18f)
                )

                Spacer(Modifier.height(8.dp))

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Column(Modifier.weight(1.3f)) {
                        CompactUnloadingInfoRow("🧾 WS No", item.wsNo)
                        Spacer(Modifier.height(6.dp))
                        CompactUnloadingInfoRow("👤 Party", item.partyName)
                    }
                    Column(Modifier.weight(0.9f)) {
                        CompactUnloadingInfoRow("⚖️ Gross Wt", "${item.grossWeight} Qtl")
                        Spacer(Modifier.height(6.dp))
                        CompactUnloadingInfoRow("🏢 Godown", item.godownName)
                    }
                }

                Spacer(Modifier.height(10.dp))

                HorizontalDivider(
                    thickness = 1.dp,
                    color = statusColor.copy(alpha = 0.15f)
                )

                Spacer(Modifier.height(8.dp))

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Column(Modifier.weight(1f)) {
                        CompactUnloadingInfoRow("🚚 Thekedar", item.thekedarName)
                        Spacer(Modifier.height(6.dp))
                        CompactUnloadingInfoRow("🏷️ Indent", item.indentNo)
                    }
                    Column(Modifier.weight(1f)) {
                        CompactUnloadingInfoRow("👷 Unload By", item.unloadUser)
                        Spacer(Modifier.height(6.dp))
                        CompactUnloadingInfoRow("📅 Date", item.unloadDate)
                    }
                }

                Spacer(Modifier.height(10.dp))

                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    statusColor.copy(alpha = 0.08f),
                                    statusColor.copy(alpha = 0.02f)
                                )
                            ),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Tap to view unloading details",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = statusColor
                    )
                }
            }

            // RIGHT ACCENT BAR
            Box(
                Modifier
                    .width(8.dp)
                    .align(Alignment.Bottom)
                    .height(180.dp)
                    .background(
                        Brush.verticalGradient(listOf(statusColor, statusColorLight))
                    )
            )
        }
    }
}
@Composable
fun CompactUnloadingInfoRow(title: String, value: String) {
    if (value.isNotBlank()) {
        Column(modifier = Modifier.padding(vertical = 1.dp)) {
            Text(
                text = title,
                color = Color.Gray,
                fontSize = 10.sp,                 // ↑ from 10 → 11
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.3.sp
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = value,
                fontSize = 12.sp,                 // ↑ from 12 → 14
                fontWeight = FontWeight.SemiBold, // ↑ Medium → SemiBold
                color = Color(0xFF1A1A1A),        // near-black for contrast
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 16.sp
            )
        }
    }
}
@Composable
fun CompactActiveFilterSection(
    fromDate: String,
    toDate: String,
    selectedStatus: UnloadApprovalStatus?,
    onClearAll: () -> Unit,
    onClearDate: () -> Unit,
    onClearStatus: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = Color.White,
        shadowElevation = 2.dp
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(26.dp).background(PrimaryGreen.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) { Icon(Icons.Default.FilterAlt, null, tint = DarkGreen, modifier = Modifier.size(14.dp)) }
                    Spacer(Modifier.width(8.dp))
                    Text("Active Filters", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = DarkGreen)
                }
                Surface(
                    modifier = Modifier.clickable { onClearAll() },
                    shape = RoundedCornerShape(20.dp),
                    color = ErrorRed.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, ErrorRed.copy(alpha = 0.3f))
                ) {
                    Row(
                        Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Close, null, tint = ErrorRed, modifier = Modifier.size(12.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Clear Filter", color = ErrorRed, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (fromDate.isNotEmpty() || toDate.isNotEmpty()) {
                    CompactFilterChip("$fromDate → $toDate", Icons.Default.DateRange, DarkGreen, onClearDate)
                }
                selectedStatus?.let {
                    val (text, color) = when (it) {
                        UnloadApprovalStatus.APPROVED -> "Approved" to Color(0xFF2E7D32)
                        UnloadApprovalStatus.PENDING  -> "Pending"  to Color(0xFFF57C00)
                        UnloadApprovalStatus.REJECTED -> "Rejected" to Color(0xFFD32F2F)
                    }
                    CompactFilterChip(text, Icons.Default.Verified, color, onClearStatus)
                }
            }
        }
    }
}

@Composable
fun CompactFilterChip(text: String, icon: ImageVector, color: Color, onRemove: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.15f))
            .clickable { onRemove() }
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(4.dp))
        Text(text, color = color, fontWeight = FontWeight.Medium, fontSize = 11.sp)
        Spacer(Modifier.width(4.dp))
        Icon(Icons.Default.Close, null, tint = color, modifier = Modifier.size(12.dp))
    }
}

@Composable
fun CompactDateCard(label: String, date: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(
        modifier = modifier.height(70.dp).clickable { onClick() },
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.fillMaxSize().padding(10.dp), verticalArrangement = Arrangement.Center) {
            Text(label, fontSize = 10.sp, color = Color.Gray)
            Spacer(Modifier.height(2.dp))
            Text(date, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = DarkGreen, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun CompactStatusChip(label: String, isSelected: Boolean, color: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(
        modifier = modifier.height(48.dp).clickable { onClick() },
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = if (isSelected) color else Color.White)
    ) {
        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            if (isSelected) {
                Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
            }
            Text(label, color = if (isSelected) Color.White else color, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
        }
    }
}

@Composable
fun ModernDatePickerDialog(onDateSelected: (String) -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        val cal = Calendar.getInstance()
        val dialog = DatePickerDialog(
            context,
            { _, y, m, d ->
                val sc = Calendar.getInstance().apply { set(y, m, d) }
                onDateSelected(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(sc.time))
            },
            cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)
        )
        dialog.datePicker.maxDate = System.currentTimeMillis()
        dialog.setOnDismissListener { onDismiss() }
        dialog.show()
    }
}

enum class DateType { FROM, TO }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnloadingDetailDialog(
    record: UnloadingRecordModel,
    rows: List<UnloadingDetailModel>,
    isLoading: Boolean,
    status: UnloadStatus,
    onDismiss: () -> Unit
) {
    val isUnloaded     = status == UnloadStatus.UNLOADED
    val isTempRejected = status == UnloadStatus.TEMP_REJECTED
    val isRejected     = status == UnloadStatus.REJECTED

    val statusColor = when {
        isRejected     -> Color(0xFFD32F2F)
        isTempRejected -> Color(0xFFEF6C00)
        else           -> DarkGreen
    }
    val statusColorLight = when {
        isRejected     -> Color(0xFFFF6B6B)
        isTempRejected -> Color(0xFFFFB74D)
        else           -> PrimaryGreen
    }
    val statusText = when {
        isRejected     -> "REJECTED"
        isTempRejected -> "TEMP REJECTED"
        else           -> "UNLOADED"
    }
    val statusIcon = when {
        isRejected     -> "✗"
        isTempRejected -> "!"
        else           -> "✓"
    }

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
                .padding(horizontal = 18.dp)
                .heightIn(max = 600.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = WhiteColor),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
        ) {
            Column(Modifier.fillMaxWidth()) {

                Box(
                    Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                listOf(statusColor, statusColorLight)
                            )
                        )
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Column(Modifier.align(Alignment.CenterStart)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Unloading Details",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.width(8.dp))
                            // small status badge
                            Box(
                                Modifier
                                    .background(
                                        Color.White.copy(alpha = 0.22f),
                                        RoundedCornerShape(20.dp)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    "$statusIcon $statusText",
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }
                        Spacer(Modifier.height(2.dp))
                        Text(
                            "Vehicle No : ${record.vehicleNo} • WS No : ${record.wsNo}",
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Box(
                        Modifier
                            .align(Alignment.CenterEnd)
                            .size(28.dp)
                            .background(Color.White.copy(alpha = 0.2f), CircleShape)
                            .clickable { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Close,
                            "Close",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // ---------- BODY ----------
                Column(
                    Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {

                    ConfirmInfoRow("Party", record.partyName)
                    ConfirmInfoRow("Thekedar", record.thekedarName)
                    ConfirmInfoRow("Godown", record.godownName)
                    ConfirmInfoRow("Gross Weight", "${record.grossWeight} Qtl")

                    HorizontalDivider(color = statusColor.copy(alpha = 0.2f))

                    if (isLoading) {
                        Box(
                            Modifier.fillMaxWidth().padding(vertical = 20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = statusColor,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    } else if (rows.isEmpty()) {
                        Box(
                            Modifier.fillMaxWidth().padding(vertical = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No unloading rows found",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }

                    } else {

                        Row(
                            Modifier
                                .fillMaxWidth()
                                .background(
                                    statusColor.copy(alpha = 0.12f),
                                    RoundedCornerShape(6.dp)
                                )
                                .padding(vertical = 6.dp, horizontal = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            DialogHeader("Sr.No", Modifier.weight(0.5f), statusColor)
                            DialogHeader("No Of Bags", Modifier.weight(1f), statusColor)
                            DialogHeader("Moisture", Modifier.weight(1f), statusColor)
                        }

                        rows.forEachIndexed { idx, r ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                DialogCell("${idx + 1}", Modifier.weight(0.5f))
                                DialogCell(r.bags, Modifier.weight(1f))
                                DialogCell(r.moisture, Modifier.weight(1f))
                            }
                        }

                        HorizontalDivider(color = statusColor.copy(alpha = 0.2f))

                        val totalBags = rows.sumOf { it.bags.toIntOrNull() ?: 0 }
                        val avgMoist = rows.mapNotNull { it.moisture.toDoubleOrNull() }
                            .takeIf { it.isNotEmpty() }?.average() ?: 0.0
                        val maxMoist = rows.mapNotNull { it.moisture.toDoubleOrNull() }
                            .maxOrNull() ?: 0.0

                        ConfirmInfoRow("Total Bags", "$totalBags")
                        ConfirmInfoRow("Average Moisture", String.format("%.2f %%", avgMoist))
                        ConfirmInfoRow("Highest Moisture", String.format("%.2f %%", maxMoist))
                    }
                }

                // ---------- FOOTER (themed) ----------
                HorizontalDivider(color = statusColor.copy(alpha = 0.2f))
                Row(
                    Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.height(42.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = statusColor)
                    ) {
                        Text("Close", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun DialogHeader(text: String, modifier: Modifier, color: Color) {
    Text(
        text,
        modifier = modifier,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = color,
        textAlign = TextAlign.Center
    )
}

@Composable
private fun DialogCell(text: String, modifier: Modifier) {
    Text(
        text.ifBlank { "-" },
        modifier = modifier,
        fontSize = 12.sp,
        color = Color.Black,
        textAlign = TextAlign.Center
    )
}

@Composable
private fun ConfirmInfoRow(label: String, value: String) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 12.sp, color = Color.DarkGray, fontWeight = FontWeight.Medium)
        Text(
            value.ifBlank { "-" },
            fontSize = 12.sp,
            color = Color.Black,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 8.dp).weight(1f),
            textAlign = TextAlign.End
        )
    }
}