package com.visibleinfotech.e_ethanolUnloading.dispatch

import android.R
import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.visibleinfotech.e_ethanolUnloading.dispatch.models.DispatchRecord
import com.visibleinfotech.e_ethanolUnloading.ui.theme.*
import com.visibleinfotech.e_ethanolUnloading.utils.KeyboardUtils
import com.visibleinfotech.e_ethanolUnloading.utils.SetStatusBarColor
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DispatchRecordsScreen(
    onBack: () -> Unit = {},
) {
    SetStatusBarColor(IndentTeal)
    val context = LocalContext.current
    val hideKeyboard = KeyboardUtils.hideKeyboard()

    val repository = remember { DispatchRepository(context) }
    val viewModel: DispatchRecordsViewModel = viewModel(
        factory = DispatchRecordsViewModelFactory(repository)
    )

    // ===== LiveData → State =====
    val isLoading by viewModel.isLoading.observeAsState(false)
    val filteredRecords by viewModel.filteredRecords.observeAsState(emptyList())
    val searchQuery by viewModel.searchQuery.observeAsState("")
    val totalRecords by viewModel.totalRecords.observeAsState(0)
    val totalQuantity by viewModel.totalQuantity.observeAsState(0.0)
    val error by viewModel.error.observeAsState()

    var showFilterSheet by remember { mutableStateOf(false) }
    var fromDate by remember { mutableStateOf("") }
    var toDate by remember { mutableStateOf("") }
    var tempFromDate by remember { mutableStateOf("") }
    var tempToDate by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }
    var selectedDateType by remember { mutableStateOf<DateType?>(null) }

    var isRefreshing by remember { mutableStateOf(false) }
    val pullToRefreshState: PullToRefreshState = rememberPullToRefreshState()

    val hasFilter = fromDate.isNotEmpty() || toDate.isNotEmpty()

    LaunchedEffect(Unit) {
        viewModel.loadRecords()
    }
    LaunchedEffect(isRefreshing) {
        if (isRefreshing) {
            viewModel.loadRecords()
            kotlinx.coroutines.delay(400)
            isRefreshing = false
        }
    }
    LaunchedEffect(showFilterSheet) {
        if (showFilterSheet) {
            tempFromDate = fromDate
            tempToDate = toDate
        }
    }

    LaunchedEffect(error) {
        error?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
        }
    }

    // ===== UI =====
    // ===== UI =====
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Dispatch Records",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { hideKeyboard(); onBack() }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_menu_revert),
                            contentDescription = "Back",
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
                                    tint = if (hasFilter) Color.White else Color(0xFFFFD54F)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = "Filter",
                                    fontSize = 13.sp,
                                    color = if (hasFilter) Color.White else Color(0xFFFFD54F)
                                )
                            }
                        },
                        modifier = Modifier.padding(end = 8.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = IndentCoralDark,
                            selectedLabelColor = Color.White,
                            containerColor = Color.White.copy(alpha = 0.2f),
                            labelColor = Color.White
                        )
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                modifier = Modifier.background(
                    Brush.horizontalGradient(colors = listOf(IndentTeal, IndentCoralDark))
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(IndentLightTeal.copy(alpha = 0.3f))
                .padding(padding)
                .pointerInput(Unit) { detectTapGestures(onTap = { hideKeyboard() }) }
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
                    Spacer(Modifier.height(4.dp))
                    // ---- Search bar ----
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White,
                        shadowElevation = 1.dp,
                        border = BorderStroke(
                            1.dp,
                            if (searchQuery.isNotEmpty()) IndentTeal
                            else IndentTeal.copy(alpha = 0.4f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = null,
                                tint = IndentTeal,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))

                            BasicTextField(
                                value = searchQuery,
                                onValueChange = viewModel::updateSearchQuery,
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(
                                    onSearch = { hideKeyboard() }
                                ),
                                decorationBox = { innerTextField ->
                                    Box {
                                        if (searchQuery.isEmpty()) {
                                            Text(
                                                "Search by Party, Vehicle, Invoice, Driver",
                                                fontSize = 12.sp,
                                                color = Color.Gray
                                            )
                                        }
                                        innerTextField()
                                    }
                                }
                            )

                            if (searchQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = {
                                        viewModel.updateSearchQuery("")
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

                    // ---- Active filter chips ----
                    if (hasFilter) {
                        ActiveFilterSection(
                            fromDate = fromDate,
                            toDate = toDate,
                            onClearAll = {
                                fromDate = ""
                                toDate = ""
                                viewModel.clearFilters()
                                hideKeyboard()
                            },
                            onClearDate = {
                                fromDate = ""
                                toDate = ""
                                viewModel.applyDateFilter(null, null)
                                hideKeyboard()
                            }
                        )
                    }

                    // ---- Stats bar ----
                    if (filteredRecords.isNotEmpty() && !isLoading) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            color = Color.White
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "📊 $totalRecords Records",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = IndentTeal
                                )
                                Text(
                                    text = "🚛 Total Qty: ${
                                        String.format(
                                            "%.2f",
                                            totalQuantity
                                        )
                                    } Qtls",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = IndentCoralDark
                                )
                            }
                        }
                    }

                    // ---- Loading ----
                    if (isLoading) {
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(
                                color = IndentTeal,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }

                    // ---- Empty state ----
                    // ---- Empty state ----
                    if (filteredRecords.isEmpty() && !isLoading) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
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
                                        tint = IndentTeal
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        text = if (searchQuery.isNotEmpty() || hasFilter)
                                            "No matches found" else "No dispatch records",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = IndentTeal
                                    )
                                    Text(
                                        text = "Try adjusting filters",
                                        fontSize = 12.sp,
                                        color = Color.Gray
                                    )

                                    if (searchQuery.isEmpty() && !hasFilter) {
                                        Spacer(Modifier.height(16.dp))

                                        Button(
                                            onClick = {
                                                hideKeyboard()
                                                viewModel.loadRecords()
                                            },
                                            shape = RoundedCornerShape(10.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = IndentTeal),
                                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Refresh,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(Modifier.width(6.dp))
                                            Text(
                                                text = "Retry",
                                                color = Color.White,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // ---- Records list ----
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredRecords, key = { it.dCode }) { record ->
                            DispatchRecordCard(record)
                        }
                        item { Spacer(Modifier.height(240.dp)) }
                    }
                }
            }
        }
    }
    // ========== FILTER BOTTOM SHEET ==========
    if (showFilterSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFilterSheet = false; hideKeyboard() },
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "Filter Dispatch Records",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = IndentTeal
                        )
                        Text(
                            "Find dispatch records by date",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                    Surface(
                        modifier = Modifier
                            .size(32.dp)
                            .clickable { showFilterSheet = false; hideKeyboard() },
                        shape = CircleShape,
                        color = IndentTeal.copy(alpha = 0.15f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = null,
                                tint = IndentTeal,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                HorizontalDivider(thickness = 2.dp, color = IndentTeal.copy(alpha = 0.3f))

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.DateRange,
                            contentDescription = null,
                            tint = IndentTeal,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Date Range",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = IndentTeal
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        DateCard(
                            label = "From",
                            date = tempFromDate.ifEmpty { "Select" },
                            modifier = Modifier.weight(1f),
                            onClick = {
                                hideKeyboard()
                                selectedDateType = DateType.FROM
                                showDatePicker = true
                            }
                        )
                        DateCard(
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

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            tempFromDate = ""
                            tempToDate = ""
                            hideKeyboard()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
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
                            viewModel.applyDateFilter(
                                fromDate.ifEmpty { null },
                                toDate.ifEmpty { null }
                            )
                            showFilterSheet = false
                            hideKeyboard()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = IndentTeal)
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

    // ========== DATE PICKER ==========
    if (showDatePicker) {
        DatePickerDialogHelper(
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

@Composable
private fun DispatchRecordCard(record: DispatchRecord) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, IndentTeal.copy(alpha = 0.15f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = IndentTeal.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, IndentTeal.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = "#${record.dCode}",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = IndentTeal
                    )
                }
                Spacer(Modifier.width(8.dp))
                StatusChip(record.status)
                Spacer(Modifier.weight(1f))
                Text(
                    text = formatDate(record.date),
                    fontSize = 11.sp,
                    color = Color.Gray,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = IndentTeal.copy(alpha = 0.15f))
            Spacer(Modifier.height(10.dp))

            // Party + Invoice
            InfoRow(Icons.Default.Business, "Party", record.partyName.ifBlank { "-" }, IndentTeal)
            InfoRow(Icons.Default.Receipt, "Invoice", record.invoiceNo.ifBlank { "-" }, IndentCoral)

            Spacer(Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                HalfInfoCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.LocalShipping,
                    label = "Vehicle Number",
                    value = record.truckNo.ifBlank { "-" },
                    accent = IndentTeal
                )
                HalfInfoCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.ProductionQuantityLimits,
                    label = "Quantity",
                    value = "${record.quantity} Qtls",
                    accent = IndentCoral
                )
            }

            Spacer(Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                HalfInfoCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Person,
                    label = "Driver Name",
                    value = record.driverName.ifBlank { "-" },
                    accent = IndentTeal
                )
                HalfInfoCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Phone,
                    label = "Mobile No.",
                    value = record.driverMobile.ifBlank { "-" },
                    accent = IndentCoral
                )
            }

        }
    }
}

// =====================================================================
// Small helper composables
// =====================================================================

@Composable
private fun StatusChip(status: String) {
    val (bg, fg, label) = when (status.uppercase()) {
        "Y" -> Triple(Color(0xFFE8F5E9), Color(0xFF2E7D32), "Approved")
        "N" -> Triple(Color(0xFFFFF3E0), Color(0xFFE65100), "Pending")
        else -> Triple(Color(0xFFF5F5F5), Color(0xFF616161), "Unknown")
    }

    Surface(
        color = bg,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, fg.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier
                .size(6.dp)
                .background(fg, RoundedCornerShape(50)))
            Spacer(Modifier.width(5.dp))
            Text(label, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = fg)
        }
    }
}

@Composable
private fun InfoRow(
    icon: ImageVector,
    label: String,
    value: String,
    accent: Color,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = accent, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text("$label:", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
        Spacer(Modifier.width(6.dp))
        Text(
            value,
            fontSize = 13.sp,
            color = Color(0xFF212121),
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun HalfInfoCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    value: String,
    accent: Color,
) {
    Surface(
        modifier = modifier,
        color = accent.copy(alpha = 0.06f),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.15f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = accent, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(6.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    label,
                    fontSize = 9.sp,
                    lineHeight = 10.sp,
                    color = Color.Gray,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    value,
                    fontSize = 12.sp,
                    lineHeight = 13.sp,
                    color = Color(0xFF212121),
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// =====================================================================
// Filter helpers
// =====================================================================

@Composable
private fun ActiveFilterSection(
    fromDate: String,
    toDate: String,
    onClearAll: () -> Unit,
    onClearDate: () -> Unit,
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
                        modifier = Modifier
                            .size(26.dp)
                            .background(IndentTeal.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.FilterAlt,
                            null,
                            tint = IndentTeal,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Active Filters",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = IndentTeal
                    )
                }
                Surface(
                    modifier = Modifier.clickable { onClearAll() },
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFFE53935).copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, Color(0xFFE53935).copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Close,
                            null,
                            tint = Color(0xFFE53935),
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "Clear Filter",
                            color = Color(0xFFE53935),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (fromDate.isNotEmpty() || toDate.isNotEmpty()) {
                    FilterChipInline(
                        text = "${fromDate.ifEmpty { "..." }} → ${toDate.ifEmpty { "..." }}",
                        icon = Icons.Default.CalendarToday,
                        color = IndentTeal,
                        onRemove = onClearDate
                    )
                }
            }
        }
    }
}

@Composable
private fun FilterChipInline(
    text: String,
    icon: ImageVector,
    color: Color,
    onRemove: () -> Unit,
) {
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
private fun DateCard(
    label: String,
    date: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Card(
        modifier = modifier
            .height(70.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(label, fontSize = 10.sp, color = Color.Gray)
            Spacer(Modifier.height(2.dp))
            Text(
                date,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = IndentTeal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun DatePickerDialogHelper(
    onDateSelected: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        val calendar = Calendar.getInstance()
        val dialog = DatePickerDialog(
            context,
            { _, year, month, day ->
                val selected = Calendar.getInstance().apply { set(year, month, day) }
                val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                onDateSelected(formatter.format(selected.time))
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

private fun formatDate(dateTimeString: String): String {
    return try {
        val input = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val output = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        val date = input.parse(dateTimeString)
        output.format(date ?: Date())
    } catch (e: Exception) {
        dateTimeString
    }
}

enum class DateType { FROM, TO }