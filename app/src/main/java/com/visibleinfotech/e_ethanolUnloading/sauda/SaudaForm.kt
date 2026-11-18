package com.visibleinfotech.e_ethanolUnloading.sauda

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.visibleinfotech.e_ethanolUnloading.sauda.models.SaudaItem
import com.visibleinfotech.e_ethanolUnloading.ui.theme.DarkGreen
import com.visibleinfotech.e_ethanolUnloading.ui.theme.PrimaryGreen
import com.visibleinfotech.e_ethanolUnloading.ui.theme.WhiteColor
import com.visibleinfotech.e_ethanolUnloading.utils.KeyboardUtils
import com.visibleinfotech.e_ethanolUnloading.utils.SetStatusBarColor

private val RejectRed = Color(0xFFD32F2F)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SaudaForm(
    onBack: () -> Unit = {},
) {
    SetStatusBarColor(DarkGreen)

    val context = LocalContext.current
    val hideKeyboard = KeyboardUtils.hideKeyboard()

    val application = context.applicationContext as android.app.Application
    val repository = remember { SaudaRepository(context) }
    val viewModel: SaudaFormViewModel = viewModel(
        factory = SaudaFormViewModelFactory(application, repository)
    )

    val isLoading by viewModel.isLoading.observeAsState(false)
    val filteredRecords by viewModel.filteredRecords.observeAsState(emptyList())
    val searchQuery by viewModel.searchQuery.observeAsState("")
    val error by viewModel.error.observeAsState()
    val isApproving by viewModel.isApproving.observeAsState(false)
    val approveResult by viewModel.approveResult.observeAsState()

    var dialogAction by remember { mutableStateOf<Pair<SaudaItem, String>?>(null) }

    var editingItem by remember { mutableStateOf<SaudaItem?>(null) }

    LaunchedEffect(Unit) { viewModel.loadSaudaList() }

    LaunchedEffect(error) {
        error?.let { Toast.makeText(context, it, Toast.LENGTH_LONG).show() }
    }
    LaunchedEffect(approveResult) {
        approveResult?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearApproveResult()
        }
    }

    BackHandler { hideKeyboard(); onBack() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Sauda Approve",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { hideKeyboard(); onBack() }) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                modifier = Modifier.background(
                    Brush.horizontalGradient(colors = listOf(DarkGreen, PrimaryGreen))
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(PrimaryGreen.copy(alpha = 0.2f))
                .padding(padding)
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Spacer(Modifier.height(4.dp))

            // Search bar
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = Color.White,
                shadowElevation = 1.dp,
                border = BorderStroke(
                    1.dp,
                    if (searchQuery.isNotEmpty()) DarkGreen else DarkGreen.copy(alpha = 0.5f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Search, null, tint = DarkGreen, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = viewModel::updateSearchQuery,
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { hideKeyboard() }),
                        decorationBox = { inner ->
                            Box {
                                if (searchQuery.isEmpty()) {
                                    Text(
                                        "Search Party, Sauda, Place, Crop, Broker",
                                        fontSize = 12.sp,
                                        color = Color.Gray
                                    )
                                }
                                inner()
                            }
                        }
                    )
                    if (searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { viewModel.updateSearchQuery(""); hideKeyboard() },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.Close, null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }

            // Stats
            if (filteredRecords.isNotEmpty() && !isLoading) {
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
                            "📋 ${filteredRecords.size} Pending Sauda",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = DarkGreen
                        )
                    }
                }
            }

            // Loading
            if (isLoading) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = DarkGreen, modifier = Modifier.size(32.dp))
                }
            }

            // Empty
            if (filteredRecords.isEmpty() && !isLoading) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
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
                                null,
                                modifier = Modifier.size(60.dp),
                                tint = PrimaryGreen
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                if (searchQuery.isNotEmpty()) "No matches found" else "No pending sauda",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = DarkGreen
                            )
                            Text("Pull to refresh or tap Retry", fontSize = 12.sp, color = Color.Gray)
                            Spacer(Modifier.height(16.dp))
                            Button(
                                onClick = { viewModel.refresh() },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = DarkGreen)
                            ) {
                                Icon(Icons.Default.Refresh, null, modifier = Modifier.size(16.dp), tint = Color.White)
                                Spacer(Modifier.width(6.dp))
                                Text("Retry", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }

            // List
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredRecords, key = { it.plotId }) { item ->
                    SaudaCard(
                        item = item,
                        isApproving = isApproving,
                        onApprove = { dialogAction = item to "APPROVE" },
                        onReject = { dialogAction = item to "REJECT" },
                        onEdit = { editingItem = item }        // ← NEW
                    )
                }
                item { Spacer(Modifier.height(120.dp)) }
            }
        }
    }

    // ==================== APPROVE / REJECT CONFIRMATION ====================
    dialogAction?.let { (item, action) ->
        val isReject = action == "REJECT"
        val accent = if (isReject) RejectRed else PrimaryGreen
        val borderAccent = accent.copy(alpha = 0.30f)
        val lightBorder = Color(0xFFE0E0E0)

        Dialog(
            onDismissRequest = { if (!isApproving) dialogAction = null },
            properties = DialogProperties(
                dismissOnBackPress = !isApproving,
                dismissOnClickOutside = !isApproving
            )
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp),
                shape = RoundedCornerShape(24.dp),
                color = Color.White,
                tonalElevation = 0.dp,
                shadowElevation = 12.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .padding(vertical = 20.dp, horizontal = 10.dp)
                ) {
                    // Icon badge
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .align(Alignment.CenterHorizontally)
                            .background(Color.White, RoundedCornerShape(50))
                            .border(1.5.dp, borderAccent, RoundedCornerShape(50)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isReject) Icons.Default.Cancel else Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(34.dp)
                        )
                    }

                    Spacer(Modifier.height(10.dp))

                    Text(
                        text = if (isReject) "Reject Sauda?" else "Approve Sauda?",
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isReject) RejectRed else DarkGreen
                    )

                    Spacer(Modifier.height(4.dp))

                    Text(
                        text = if (isReject)
                            "This sauda will be marked as rejected."
                        else
                            "This sauda will be marked as approved.",
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                        fontSize = 12.sp,
                        color = Color.Gray
                    )

                    Spacer(Modifier.height(10.dp))

                    // Summary card
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, lightBorder)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Surface(
                                color = Color.White,
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, borderAccent)
                            ) {
                                Text(
                                    "Sauda No. ${item.plotId}",
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = accent
                                )
                            }

                            Spacer(Modifier.height(10.dp))

                            DialogInfoRow(
                                icon = Icons.Default.Business,
                                label = "Party",
                                value = item.partyName.ifBlank { "-" },
                                accent = accent,
                                allowWrap = true
                            )
                            Spacer(Modifier.height(8.dp))
                            DialogInfoRow(
                                icon = Icons.Default.LocationOn,
                                label = "Place",
                                value = item.placeName.ifBlank { "-" },
                                accent = accent
                            )
                            Spacer(Modifier.height(8.dp))
                            DialogInfoRow(
                                icon = Icons.Default.Agriculture,
                                label = "Crop",
                                value = item.cropType.ifBlank { "-" },
                                accent = accent
                            )
                        }
                    }

                    Spacer(Modifier.height(18.dp))

                    Button(
                        onClick = {
                            if (isReject) viewModel.rejectSauda(item.plotId)
                            else viewModel.approveSauda(item.plotId)
                            dialogAction = null
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        enabled = !isApproving,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accent,
                            disabledContainerColor = accent.copy(alpha = 0.5f)
                        )
                    ) {
                        if (isApproving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = Color.White
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                if (isReject) "Rejecting..." else "Approving...",
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                        } else {
                            Icon(
                                imageVector = if (isReject) Icons.Default.Cancel else Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                if (isReject) "Yes, Reject" else "Yes, Approve",
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = { dialogAction = null },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        enabled = !isApproving,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.2.dp, Color(0xFFBDBDBD)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color.White,
                            contentColor = Color(0xFF616161)
                        )
                    ) {
                        Text("Cancel", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    }
                }
            }
        }
    }

    // ==================== EDIT DIALOG ====================
    editingItem?.let { item ->
        var rate by remember(item.plotId) { mutableStateOf(item.rate) }
        var area by remember(item.plotId) { mutableStateOf(item.area) }
        var dueDays by remember(item.plotId) { mutableStateOf(item.paymentDueDays) }
        var cashDisc by remember(item.plotId) { mutableStateOf(item.cashDisc) }
        var wtDed by remember(item.plotId) { mutableStateOf(item.wtDed) }
        var perBagDed by remember(item.plotId) { mutableStateOf(item.perBagDed) }
        var saving by remember { mutableStateOf(false) }

        Dialog(
            onDismissRequest = { if (!saving) editingItem = null },
            properties = DialogProperties(
                dismissOnBackPress = !saving,
                dismissOnClickOutside = !saving
            )
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp),
                shape = RoundedCornerShape(24.dp),
                color = Color.White,
                shadowElevation = 12.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 20.dp, horizontal = 16.dp)
                ) {
                    // Header
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Edit, null, tint = DarkGreen, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Edit Sauda — ${item.plotId}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = DarkGreen
                        )
                    }

                    Spacer(Modifier.height(12.dp))
                    HorizontalDivider(color = PrimaryGreen.copy(alpha = 0.2f))
                    Spacer(Modifier.height(14.dp))

                    // ── Row 1: Rate | Quantity ─────────────────────────────
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        EditField(
                            modifier = Modifier.weight(1f),
                            label = "Rate (₹/Qtl)",
                            value = rate,
                            keyboardType = KeyboardType.Decimal
                        ) { rate = it }
                        EditField(
                            modifier = Modifier.weight(1f),
                            label = "Quantity (Qtl)",
                            value = area,
                            keyboardType = KeyboardType.Decimal
                        ) { area = it }
                    }

                    Spacer(Modifier.height(10.dp))

                    // ── Row 2: Due Days | Cash Discount ───────────────────
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        EditField(
                            modifier = Modifier.weight(1f),
                            label = "Due Days",
                            value = dueDays,
                            keyboardType = KeyboardType.Number
                        ) { dueDays = it }
                        EditField(
                            modifier = Modifier.weight(1f),
                            label = "Cash Discount (%)",
                            value = cashDisc,
                            keyboardType = KeyboardType.Decimal
                        ) { cashDisc = it }
                    }

                    Spacer(Modifier.height(10.dp))

                    // ── Row 3: Weight Deduction | Hand Deduction ──────────
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        EditField(
                            modifier = Modifier.weight(1f),
                            label = "Weight Deduction (Gram)",
                            value = wtDed,
                            keyboardType = KeyboardType.Decimal
                        ) { wtDed = it }
                        EditField(
                            modifier = Modifier.weight(1f),
                            label = "Hand Deduction (₹/Qtl)",
                            value = perBagDed,
                            keyboardType = KeyboardType.Decimal
                        ) { perBagDed = it }
                    }

                    Spacer(Modifier.height(20.dp))

                    // Actions
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick = { editingItem = null },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp),
                            enabled = !saving,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.2.dp, Color(0xFFBDBDBD))
                        ) {
                            Text("Cancel", color = Color(0xFF616161), fontWeight = FontWeight.SemiBold)
                        }
                        Button(
                            onClick = {
                                saving = true

                                viewModel.updateSauda(
                                    plotId         = item.plotId,
                                    rate           = rate,
                                    area           = area,
                                    paymentDueDays = dueDays,
                                    cashDisc       = cashDisc,
                                    wtDed          = wtDed,
                                    perBagDed      = perBagDed
                                ) { success, message ->
                                    saving = false
                                    Toast.makeText(
                                        context,
                                        message,
                                        if (success) Toast.LENGTH_SHORT else Toast.LENGTH_LONG
                                    ).show()
                                    if (success) {
                                        editingItem = null
                                    }
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp),
                            enabled = !saving,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = DarkGreen)
                        ) {
                            if (saving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = Color.White
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("Saving…", color = Color.White)
                            } else {
                                Icon(Icons.Default.Save, null, tint = Color.White, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Save", color = Color.White, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==================== SAUDA CARD ====================
@Composable
private fun SaudaCard(
    item: SaudaItem,
    isApproving: Boolean,
    onApprove: () -> Unit,
    onReject: () -> Unit,
    onEdit: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.5.dp,
                color = PrimaryGreen.copy(alpha = 0.4f),
                shape = RoundedCornerShape(14.dp)
            ),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .fillMaxHeight()
                    .background(
                        Brush.verticalGradient(
                            listOf(DarkGreen, PrimaryGreen.copy(alpha = 0.6f))
                        )
                    )
            )
            Column(
                modifier = Modifier
                    .padding(14.dp)
                    .weight(1f)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = PrimaryGreen.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, PrimaryGreen.copy(alpha = 0.4f))
                    ) {
                        Text(
                            "Sauda No. ${item.plotId}",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = DarkGreen
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        color = Color(0xFFFFF3E0),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, Color(0xFFF57C00).copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(Modifier.size(6.dp).background(Color(0xFFF57C00), RoundedCornerShape(50)))
                            Spacer(Modifier.width(5.dp))
                            Text(
                                "PENDING",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFF57C00)
                            )
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    // Edit icon — quick access from header
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(28.dp),
                        enabled = !isApproving
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = DarkGreen,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(Modifier.width(4.dp))
                    Text(
                        formatDate(item.createdOn),
                        fontSize = 11.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(Modifier.height(10.dp))
                HorizontalDivider(color = PrimaryGreen.copy(alpha = 0.2f))
                Spacer(Modifier.height(10.dp))

                InfoLine(
                    Icons.Default.AccountCircle,
                    "Broker",
                    item.brokerName.ifBlank { "-" },
                    DarkGreen,
                    allowWrap = true
                )
                Spacer(Modifier.height(4.dp))
                InfoLine(
                    Icons.Default.Business,
                    "Party",
                    item.partyName.ifBlank { "-" },
                    DarkGreen,
                    allowWrap = true
                )
                Spacer(Modifier.height(4.dp))
                InfoLine(Icons.Default.LocationOn, "Place", item.placeName.ifBlank { "-" }, PrimaryGreen)
                Spacer(Modifier.height(4.dp))
                InfoLine(Icons.Default.Agriculture, "Crop", item.cropType.ifBlank { "-" }, DarkGreen)
                Spacer(Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    HalfInfo(Modifier.weight(1f), Icons.Default.SquareFoot, "Quantity", "${item.area} Qtl", DarkGreen)
                    HalfInfo(Modifier.weight(1f), Icons.Default.Payments, "Rate", "₹ ${item.rate} /Qtl", PrimaryGreen)
                }

                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    HalfInfo(Modifier.weight(1f), Icons.Default.AccountCircle, "Hand Deduction", "₹ ${item.perBagDed} /Qtl", PrimaryGreen)
                    HalfInfo(Modifier.weight(1f), Icons.Default.Schedule, "Due Days", "${item.paymentDueDays} days", DarkGreen)
                }
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    HalfInfo(Modifier.weight(1f), Icons.Default.Scale, "Weight Deduction", "${item.wtDed} Gram", DarkGreen)
                    HalfInfo(Modifier.weight(1f), Icons.Default.Percent, "Cash Discount", "${item.cashDisc} %", PrimaryGreen)
                }
                Spacer(Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Reject
                    OutlinedButton(
                        onClick = onReject,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.5.dp, RejectRed),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = RejectRed),
                        enabled = !isApproving
                    ) {
                        Icon(Icons.Default.Cancel, null, tint = RejectRed, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Reject", color = RejectRed, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    }

                    // Approve
                    Button(
                        onClick = onApprove,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DarkGreen),
                        enabled = !isApproving
                    ) {
                        Icon(Icons.Default.CheckCircle, null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Approve", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

// ==================== HELPERS ====================

@Composable
private fun InfoLine(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    accent: Color,
    allowWrap: Boolean = false,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = if (allowWrap) Alignment.Top else Alignment.CenterVertically
    ) {
        Icon(
            icon,
            null,
            tint = accent,
            modifier = Modifier
                .size(16.dp)
                .padding(top = if (allowWrap) 2.dp else 0.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text("$label:", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
        Spacer(Modifier.width(6.dp))
        Text(
            value,
            fontSize = 13.sp,
            color = Color(0xFF212121),
            fontWeight = FontWeight.SemiBold,
            maxLines = if (allowWrap) Int.MAX_VALUE else 1,
            softWrap = allowWrap,
            overflow = if (allowWrap) TextOverflow.Visible else TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun HalfInfo(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
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
                Text(label, fontSize = 9.sp, lineHeight = 10.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
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

private fun formatDate(dateTimeString: String): String {
    return try {
        val input = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
        val output = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault())
        val date = input.parse(dateTimeString)
        output.format(date ?: java.util.Date())
    } catch (e: Exception) {
        dateTimeString.take(10)
    }
}

@Composable
private fun DialogInfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    accent: Color,
    allowWrap: Boolean = false,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = if (allowWrap) Alignment.Top else Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .background(Color.White, RoundedCornerShape(8.dp))
                .border(1.dp, accent.copy(alpha = 0.25f), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = accent, modifier = Modifier.size(14.dp))
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                label,
                fontSize = 10.sp,
                color = Color.Gray,
                fontWeight = FontWeight.Medium
            )
            Text(
                value,
                fontSize = 13.sp,
                color = Color(0xFF212121),
                fontWeight = FontWeight.SemiBold,
                maxLines = if (allowWrap) Int.MAX_VALUE else 1,
                softWrap = allowWrap,
                overflow = if (allowWrap) TextOverflow.Visible else TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun EditField(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    onValueChange: (String) -> Unit,
) {
    Column(modifier = modifier) {           // ← was just Modifier.fillMaxWidth()
        Text(
            label,
            fontSize = 11.sp,
            color = Color.Gray,
            fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.height(4.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
            shape = RoundedCornerShape(10.dp),
            keyboardOptions = KeyboardOptions(
                keyboardType = keyboardType,
                imeAction = ImeAction.Next
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = DarkGreen,
                unfocusedBorderColor = DarkGreen.copy(alpha = 0.35f),
                cursorColor = DarkGreen,
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White
            )
        )
    }
}