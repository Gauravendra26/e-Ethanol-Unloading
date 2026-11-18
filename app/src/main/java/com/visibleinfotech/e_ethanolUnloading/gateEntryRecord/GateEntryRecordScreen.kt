package com.visibleinfotech.e_ethanolUnloading.gateEntryRecord

import android.app.DatePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.*
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.visibleinfotech.e_ethanolUnloading.R
import com.visibleinfotech.e_ethanolUnloading.gateEntryRecord.models.GateEntryRecordModel
import com.visibleinfotech.e_ethanolUnloading.ui.theme.*
import com.visibleinfotech.e_ethanolUnloading.utils.KeyboardUtils
import com.visibleinfotech.e_ethanolUnloading.utils.SetStatusBarColor
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GateEntryRecordScreen(
    onBack: () -> Unit = {},
    viewModel: GateEntryRecordViewModel = viewModel(
        factory = GateEntryRecordViewModelFactory(LocalContext.current)
    )
) {
    SetStatusBarColor(DarkGreen)
    val isLoading by viewModel.isLoading.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val hideKeyboard = KeyboardUtils.hideKeyboard()

    var showFilterSheet by remember { mutableStateOf(false) }

    var fromDate by remember { mutableStateOf("") }
    var toDate by remember { mutableStateOf("") }
    var selectedStatus by remember { mutableStateOf<GateEntryStatus?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var selectedDateType by remember { mutableStateOf<DateType?>(null) }

    var tempFromDate by remember { mutableStateOf("") }
    var tempToDate by remember { mutableStateOf("") }
    var tempSelectedStatus by remember { mutableStateOf<GateEntryStatus?>(null) }

    // Pull to Refresh
    val pullToRefreshState = rememberPullToRefreshState()
    var isRefreshing by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadGateEntryRecords()
    }

    LaunchedEffect(isRefreshing) {
        if (isRefreshing) {
            viewModel.loadGateEntryRecords()

            if (fromDate.isNotEmpty() || toDate.isNotEmpty() || selectedStatus != null) {
                viewModel.filterGateEntryRecords(
                    fromDate.ifEmpty { null },
                    toDate.ifEmpty { null },
                    selectedStatus
                )
            }
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
                        text = "Gate Entry Records",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        hideKeyboard()
                        onBack()
                    }) {
                        Icon(
                            painterResource(android.R.drawable.ic_menu_revert),
                            null,
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    FilterChip(
                        selected = hasFilter,
                        onClick = {
                            hideKeyboard()
                            showFilterSheet = true
                        },
                        label = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.FilterList,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = Golden
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = "Filter", fontSize = 13.sp, color = Golden)
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                ),
                modifier = Modifier.background(
                    Brush.horizontalGradient(
                        listOf(
                            DarkGreen,
                            PrimaryGreen
                        )
                    )
                )
            )
        }
    ) { paddingValues ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(PrimaryGreen.copy(0.2f))
                .padding(paddingValues)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = {
                            hideKeyboard()
                        }
                    )
                }
        ) {
            PullToRefreshBox(
                state = pullToRefreshState,
                isRefreshing = isRefreshing,
                onRefresh = {
                    isRefreshing = true
                    hideKeyboard()
                },
                modifier = Modifier.fillMaxSize()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {

                    Spacer(modifier = Modifier.height(4.dp))

                    // Search Bar
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White,
                        shadowElevation = 1.dp,
                        border = BorderStroke(1.dp, if (uiState.searchQuery.isNotEmpty()) DarkGreen else DarkGreen.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = DarkGreen,
                                modifier = Modifier.size(18.dp)
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            BasicTextField(
                                value = uiState.searchQuery,
                                onValueChange = { query ->
                                    viewModel.searchGateEntryRecords(query)
                                },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(
                                    onSearch = {
                                        viewModel.searchGateEntryRecords(uiState.searchQuery)
                                        hideKeyboard()
                                    }
                                ),
                                decorationBox = { innerTextField ->
                                    Box {
                                        if (uiState.searchQuery.isEmpty()) {
                                            Text(
                                                text = "Search by Truck No, Party, Invoice",
                                                fontSize = 12.sp,
                                                color = Color.Gray
                                            )
                                        }
                                        innerTextField()
                                    }
                                }
                            )

                            if (uiState.searchQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = {
                                        viewModel.searchGateEntryRecords("")
                                        hideKeyboard()
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = null,
                                        tint = Color.Gray,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Active Filters Section
                    if (hasFilter) {
                        CompactActiveFilterSection(
                            fromDate = fromDate,
                            toDate = toDate,
                            selectedStatus = selectedStatus,
                            onClearAll = {
                                fromDate = ""
                                toDate = ""
                                selectedStatus = null
                                viewModel.filterGateEntryRecords(null, null, null)
                                hideKeyboard()
                            },
                            onClearDate = {
                                fromDate = ""
                                toDate = ""
                                viewModel.filterGateEntryRecords(null, null, selectedStatus)
                                hideKeyboard()
                            },
                            onClearStatus = {
                                selectedStatus = null
                                viewModel.filterGateEntryRecords(fromDate.ifEmpty { null }, toDate.ifEmpty { null }, null)
                                hideKeyboard()
                            }
                        )
                    }

                    // Stats Summary
                    if (uiState.filteredRecords.isNotEmpty() && !isLoading) {
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
                                    text = "📊 ${uiState.filteredRecords.size} Records",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = DarkGreen
                                )

                                Row(
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Completed Count (Green)
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(text = "✅", fontSize = 12.sp)
                                        Text(
                                            text = " ${uiState.completedCount}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color(0xFF2E7D32)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    // Pending Count (Orange)
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(text = "⏳", fontSize = 12.sp)
                                        Text(
                                            text = " ${uiState.pendingCount}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = DarkYellow
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Loading Indicator
                    if (isLoading) {
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = DarkGreen, modifier = Modifier.size(32.dp))
                        }
                    }

                    // Empty State
                    if (uiState.filteredRecords.isEmpty() && !isLoading) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        Icons.Default.SearchOff,
                                        contentDescription = null,
                                        modifier = Modifier.size(60.dp),
                                        tint = PrimaryGreen
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = if (uiState.searchQuery.isNotEmpty()) "No matches found" else "No records",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = DarkGreen
                                    )
                                    Text(text = "Try adjusting filters", fontSize = 12.sp, color = Color.Gray)
                                }
                            }
                        }
                    }

                    // Records List
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(
                            items = uiState.filteredRecords,
                            key = { it.gCode }
                        ) { item ->
                            GateEntryRecordCard(item)
                        }
                        item {
                            Spacer(modifier = Modifier.height(240.dp))
                        }
                    }
                }
            }
        }
    }

    // Filter Bottom Sheet
    if (showFilterSheet) {
        ModalBottomSheet(
            onDismissRequest = {
                showFilterSheet = false
                hideKeyboard()
            },
            containerColor = Color.White,
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .width(40.dp)
                        .height(4.dp)
                        .background(Color.LightGray, RoundedCornerShape(50))
                )
            },
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Filter Records", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = DarkGreen)
                        Text("Find gate entry records", fontSize = 11.sp, color = Color.Gray)
                    }
                    Surface(
                        modifier = Modifier.size(32.dp).clickable {
                            showFilterSheet = false
                            hideKeyboard()
                        },
                        shape = CircleShape,
                        color = PrimaryGreen.copy(alpha = 0.15f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Close, contentDescription = null, tint = DarkGreen, modifier = Modifier.size(16.dp))
                        }
                    }
                }

                HorizontalDivider(color = PrimaryGreen.copy(alpha = 0.3f))

                // Date Range
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.DateRange, contentDescription = null, tint = DarkGreen, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Date Range", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = DarkGreen)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CompactDateCard(
                            label = "From",
                            date = tempFromDate.ifEmpty { "Select" },
                            modifier = Modifier.weight(1f),
                            onClick = {
                                hideKeyboard()
                                selectedDateType = DateType.FROM
                                showDatePicker = true
                            }
                        )
                        CompactDateCard(
                            label = "To",
                            date = tempToDate.ifEmpty { "Select" },
                            modifier = Modifier.weight(1f),
                            onClick = {
                                hideKeyboard()
                                selectedDateType = DateType.TO
                                showDatePicker = true
                            }
                        )
                    }
                }

                // Status
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Verified, contentDescription = null, tint = DarkGreen, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Status", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = DarkGreen)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CompactStatusChip(
                            label = "Completed",
                            isSelected = tempSelectedStatus == GateEntryStatus.COMPLETED,
                            color = Color(0xFF2E7D32),
                            modifier = Modifier.weight(1f)
                        ) {
                            tempSelectedStatus = if (tempSelectedStatus == GateEntryStatus.COMPLETED) null else GateEntryStatus.COMPLETED
                            hideKeyboard()
                        }
                        CompactStatusChip(
                            label = "Pending",
                            isSelected = tempSelectedStatus == GateEntryStatus.PENDING,
                            color = Color(0xFFF57C00),
                            modifier = Modifier.weight(1f)
                        ) {
                            tempSelectedStatus = if (tempSelectedStatus == GateEntryStatus.PENDING) null else GateEntryStatus.PENDING
                            hideKeyboard()
                        }
                    }
                }

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            tempFromDate = ""
                            tempToDate = ""
                            tempSelectedStatus = null
                            hideKeyboard()
                        },
                        modifier = Modifier.weight(1f).height(44.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reset", fontSize = 13.sp)
                    }
                    Button(
                        onClick = {
                            fromDate = tempFromDate
                            toDate = tempToDate
                            selectedStatus = tempSelectedStatus
                            viewModel.filterGateEntryRecords(
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
                        Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Apply", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }

    // Date Picker
    if (showDatePicker) {
        ModernDatePickerDialog(
            onDateSelected = { selectedDate ->
                when (selectedDateType) {
                    DateType.FROM -> tempFromDate = selectedDate
                    DateType.TO -> tempToDate = selectedDate
                    null -> {}
                }
                showDatePicker = false
                hideKeyboard()
            },
            onDismiss = {
                showDatePicker = false
                hideKeyboard()
            }
        )
    }
}

// ==================== COMPACT FILTER COMPONENTS ====================

@Composable
fun CompactActiveFilterSection(
    fromDate: String,
    toDate: String,
    selectedStatus: GateEntryStatus?,
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
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(26.dp).background(PrimaryGreen.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.FilterAlt, contentDescription = null, tint = DarkGreen, modifier = Modifier.size(14.dp))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Active Filters", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = DarkGreen)
                }
                Surface(
                    modifier = Modifier.clickable { onClearAll() },
                    shape = RoundedCornerShape(20.dp),
                    color = ErrorRed.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, ErrorRed.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null, tint = ErrorRed, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Clear Filter", color = ErrorRed, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            androidx.compose.foundation.layout.FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (fromDate.isNotEmpty() || toDate.isNotEmpty()) {
                    CompactFilterChip(
                        text = "$fromDate → $toDate",
                        icon = Icons.Default.DateRange,
                        color = DarkGreen,
                        onRemove = onClearDate
                    )
                }
                if (selectedStatus != null) {
                    val (text, color) = when (selectedStatus) {
                        GateEntryStatus.COMPLETED -> "Completed" to Color(0xFF2E7D32)
                        GateEntryStatus.PENDING -> "Pending" to Color(0xFFF57C00)
                    }
                    CompactFilterChip(text = text, icon = Icons.Default.Verified, color = color, onRemove = onClearStatus)
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
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text(text, color = color, fontWeight = FontWeight.Medium, fontSize = 11.sp)
        Spacer(modifier = Modifier.width(4.dp))
        Icon(Icons.Default.Close, contentDescription = null, tint = color, modifier = Modifier.size(12.dp))
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
        Column(modifier = Modifier.fillMaxSize().padding(10.dp), verticalArrangement = Arrangement.Center) {
            Text(label, fontSize = 10.sp, color = Color.Gray)
            Spacer(modifier = Modifier.height(2.dp))
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
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSelected) Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
            if (isSelected) Spacer(modifier = Modifier.width(4.dp))
            Text(label, color = if (isSelected) Color.White else color, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
        }
    }
}

// ==================== GATE ENTRY RECORD CARD ====================

@Composable
fun GateEntryRecordCard(item: GateEntryRecordModel) {
    val statusColor = when (item.status) {
        "Y" -> Color(0xFF2E7D32) // Completed
        else -> Color(0xFFF57C00) // Pending
    }

    val statusText = when (item.status) {
        "Y" -> "COMPLETED"
        else -> "PENDING"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.5.dp,
                color = statusColor,
                shape = RoundedCornerShape(14.dp)
            ),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row {
            // Left Accent Bar
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .height(160.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(statusColor, statusColor.copy(alpha = 0.6f))
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    Column(modifier = Modifier.weight(1.3f)) {
                        CompactGateEntryInfoRow("🚛 Vehicle No.", item.truckNo)
                        CompactGateEntryInfoRow("👤 Driver Name", item.driverName)
                        CompactGateEntryInfoRow("📱 Driver Mobile", item.driverMobile)
                    }
                    Column(modifier = Modifier.weight(0.7f)) {
                        CompactGateEntryInfoRow("📄 Invoice No.", item.invoiceNo)
                        CompactGateEntryInfoRow("📦 Qty", "${item.qty} Qtl")
                        CompactGateEntryInfoRow("🏷️ Mode", item.modeName)
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 4.dp),
                    color = Color.LightGray.copy(alpha = 0.5f)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    Column(modifier = Modifier.weight(1.3f)) {
                        CompactGateEntryInfoRow("🏢 Party Name", item.partyName)
                        CompactGateEntryInfoRow("🤝 Broker Name", item.brokerName)
                        CompactGateEntryInfoRow("📄 Invoice Qty", "${item.invQty} Qtl")
                    }
                    Column(modifier = Modifier.weight(0.7f)) {
                        CompactGateEntryInfoRow("🛍️ No. Of Bags", "${item.bags}")
                        CompactGateEntryInfoRow("💰 Invoice Amount", "₹${item.invAmt}")
                        CompactGateEntryInfoRow("💹 Invoice Rate", "₹${item.invRate}")
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 4.dp),
                    color = Color.LightGray.copy(alpha = 0.5f)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    Column(modifier = Modifier.weight(1.3f)) {
                        CompactGateEntryInfoRow("📅 Entry Date", item.date)
                        if (item.remarks.isNotEmpty()) {
                            CompactGateEntryInfoRow("📝 Remarks", item.remarks)
                        }
                    }
                    Column(modifier = Modifier.weight(0.7f), horizontalAlignment = Alignment.End) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = statusColor.copy(alpha = 0.15f),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = when (item.status) {
                                        "Y" -> "✓"
                                        else -> "⏳"
                                    },
                                    color = statusColor,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = statusText,
                                    color = statusColor,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 10.sp
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "G-${item.gCode}",
                            fontSize = 10.sp,
                            color = Color.Gray
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .width(6.dp)
                    .align(Alignment.Bottom)
                    .height(160.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                statusColor.copy(alpha = 0.6f),
                                statusColor
                            )
                        )
                    )
            )
        }
    }
}

@Composable
fun CompactGateEntryInfoRow(title: String, value: String) {
    if (value.isNotBlank()) {
        Column(
        ) {
            Text(
                text = title,
                color = Color.Gray,
                fontSize = 10.sp
            )
            Text(
                text = value,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun ModernDatePickerDialog(
    onDateSelected: (String) -> Unit,
    onDismiss: () -> Unit
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
                val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
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


enum class DateType { FROM, TO }
enum class GateEntryStatus { COMPLETED, PENDING }