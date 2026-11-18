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
import com.visibleinfotech.e_ethanolUnloading.ui.theme.DarkBlue
import com.visibleinfotech.e_ethanolUnloading.ui.theme.DarkGreen
import com.visibleinfotech.e_ethanolUnloading.ui.theme.LightBlue
import com.visibleinfotech.e_ethanolUnloading.ui.theme.PrimaryBlue
import com.visibleinfotech.e_ethanolUnloading.ui.theme.PrimaryGreen
import com.visibleinfotech.e_ethanolUnloading.utils.KeyboardUtils
import com.visibleinfotech.e_ethanolUnloading.utils.SetStatusBarColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SaudaVerify(
    onBack: () -> Unit = {},
) {
    SetStatusBarColor(PrimaryBlue)

    val context = LocalContext.current
    val hideKeyboard = KeyboardUtils.hideKeyboard()

    val application = context.applicationContext as android.app.Application
    val repository = remember { SaudaVerifyRepository(context) }
    val viewModel: SaudaVerifyViewModel = viewModel(
        factory = SaudaVerifyViewModelFactory(application, repository)
    )

    val isLoading by viewModel.isLoading.observeAsState(false)
    val filteredRecords by viewModel.filteredRecords.observeAsState(emptyList())
    val searchQuery by viewModel.searchQuery.observeAsState("")
    val error by viewModel.error.observeAsState()
    val isVerifying by viewModel.isVerifying.observeAsState(false)
    val verifyResult by viewModel.verifyResult.observeAsState()

    var confirmItem by remember { mutableStateOf<SaudaItem?>(null) }
    var editingItem by remember { mutableStateOf<SaudaItem?>(null) }   // ← NEW

    LaunchedEffect(Unit) { viewModel.loadList() }

    LaunchedEffect(error) {
        error?.let { Toast.makeText(context, it, Toast.LENGTH_LONG).show() }
    }
    LaunchedEffect(verifyResult) {
        verifyResult?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearVerifyResult()
        }
    }

    BackHandler { hideKeyboard(); onBack() }

    Scaffold(
        containerColor = Color.White,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Sauda Verify",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { hideKeyboard(); onBack() }) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Default.Refresh, "Refresh", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PrimaryBlue)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(LightBlue.copy(alpha = 0.1f))
                .padding(padding)
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Spacer(Modifier.height(4.dp))

            // Search
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = Color.White,
                border = BorderStroke(
                    1.dp,
                    if (searchQuery.isNotEmpty()) PrimaryBlue else PrimaryBlue.copy(alpha = 0.3f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Search, null, tint = PrimaryBlue, modifier = Modifier.size(18.dp))
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
                                if (searchQuery.isEmpty()) Text(
                                    "Search Party, Sauda, Place, Crop, Broker",
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
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
                    color = Color.White,
                    border = BorderStroke(1.dp, PrimaryBlue.copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "📋 ${filteredRecords.size} Approved Sauda to Verify",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = PrimaryBlue
                        )
                    }
                }
            }

            // Loading
            if (isLoading) {
                Box(
                    Modifier.fillMaxWidth().padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = PrimaryBlue, modifier = Modifier.size(32.dp))
                }
            }

            // Empty
            if (filteredRecords.isEmpty() && !isLoading) {
                Box(
                    Modifier.fillMaxWidth().padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, PrimaryBlue.copy(alpha = 0.2f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.SearchOff, null, modifier = Modifier.size(60.dp), tint = PrimaryBlue)
                            Spacer(Modifier.height(8.dp))
                            Text(
                                if (searchQuery.isNotEmpty()) "No matches found" else "Nothing to verify",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryBlue
                            )
                            Text("Pull to refresh or tap Retry", fontSize = 12.sp, color = Color.Gray)
                            Spacer(Modifier.height(16.dp))
                            Button(
                                onClick = { viewModel.refresh() },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
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
                    VerifyCard(
                        item = item,
                        isVerifying = isVerifying,
                        onVerify = { confirmItem = item },
                        onEdit = { editingItem = item }        // ← NEW
                    )
                }
                item { Spacer(Modifier.height(120.dp)) }
            }
        }
    }

    // ==================== VERIFY CONFIRM DIALOG ====================
    confirmItem?.let { item ->
        AlertDialog(
            onDismissRequest = { if (!isVerifying) confirmItem = null },
            title = { Text("Verify Sauda", fontWeight = FontWeight.Bold, color = PrimaryBlue) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Sauda ID: ${item.plotId}", fontWeight = FontWeight.SemiBold, color = PrimaryBlue)
                    Text("Party: ${item.partyName}")
                    Spacer(Modifier.height(4.dp))
                    Text("Mark this sauda as verified?", fontSize = 13.sp, color = Color.Gray)
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.verifySauda(item.plotId); confirmItem = null },
                    enabled = !isVerifying,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    if (isVerifying) CircularProgressIndicator(
                        modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White
                    ) else Text("Verify", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmItem = null }, enabled = !isVerifying) {
                    Text("Cancel", color = Color.Gray)
                }
            },
            shape = RoundedCornerShape(16.dp),
            containerColor = Color.White
        )
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
                modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp),
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
                        Icon(Icons.Default.Edit, null, tint = PrimaryBlue, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Edit Sauda — ${item.plotId}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryBlue
                        )
                    }

                    Spacer(Modifier.height(12.dp))
                    HorizontalDivider(color = PrimaryBlue.copy(alpha = 0.2f))
                    Spacer(Modifier.height(14.dp))

                    // Row 1: Rate | Quantity
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        VerifyEditField(
                            modifier = Modifier.weight(1f),
                            label = "Rate (₹/Qtl)",
                            value = rate,
                            keyboardType = KeyboardType.Decimal
                        ) { rate = it }
                        VerifyEditField(
                            modifier = Modifier.weight(1f),
                            label = "Quantity (Qtl)",
                            value = area,
                            keyboardType = KeyboardType.Decimal
                        ) { area = it }
                    }
                    Spacer(Modifier.height(10.dp))

                    // Row 2: Due Days | Cash Discount
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        VerifyEditField(
                            modifier = Modifier.weight(1f),
                            label = "Due Days",
                            value = dueDays,
                            keyboardType = KeyboardType.Number
                        ) { dueDays = it }
                        VerifyEditField(
                            modifier = Modifier.weight(1f),
                            label = "Cash Discount (%)",
                            value = cashDisc,
                            keyboardType = KeyboardType.Decimal
                        ) { cashDisc = it }
                    }
                    Spacer(Modifier.height(10.dp))

                    // Row 3: Weight Deduction | Hand Deduction
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        VerifyEditField(
                            modifier = Modifier.weight(1f),
                            label = "Weight Deduction (Gram)",
                            value = wtDed,
                            keyboardType = KeyboardType.Decimal
                        ) { wtDed = it }
                        VerifyEditField(
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
                            modifier = Modifier.weight(1f).height(46.dp),
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
                                        context, message,
                                        if (success) Toast.LENGTH_SHORT else Toast.LENGTH_LONG
                                    ).show()
                                    if (success) editingItem = null
                                }
                            },
                            modifier = Modifier.weight(1f).height(46.dp),
                            enabled = !saving,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
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

// ==================== VERIFY CARD ====================
@Composable
private fun VerifyCard(
    item: SaudaItem,
    isVerifying: Boolean,
    onVerify: () -> Unit,
    onEdit: () -> Unit                      // ← NEW
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.5.dp, PrimaryBlue.copy(alpha = 0.35f), RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .fillMaxHeight()
                    .background(Brush.verticalGradient(listOf(PrimaryBlue, DarkBlue)))
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
                        color = PrimaryBlue.copy(alpha = 0.10f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, PrimaryBlue.copy(alpha = 0.4f))
                    ) {
                        Text(
                            "Sauda No. ${item.plotId}",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PrimaryBlue
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        color = PrimaryGreen.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, PrimaryGreen.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                Modifier
                                    .size(6.dp)
                                    .background(PrimaryGreen, RoundedCornerShape(50))
                            )
                            Spacer(Modifier.width(5.dp))
                            Text(
                                "APPROVED",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = PrimaryGreen
                            )
                        }
                    }
                    Spacer(Modifier.weight(1f))

                    // ✅ Edit icon in header
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(28.dp),
                        enabled = !isVerifying
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = PrimaryBlue,
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
                HorizontalDivider(color = PrimaryBlue.copy(alpha = 0.15f))
                Spacer(Modifier.height(10.dp))

                VerifyInfoLine(
                    Icons.Default.AccountCircle,
                    "Broker",
                    item.brokerName.ifBlank { "-" },
                    PrimaryBlue
                )
                Spacer(Modifier.height(4.dp))
                VerifyInfoLine(
                    Icons.Default.Business,
                    "Party",
                    item.partyName.ifBlank { "-" },
                    PrimaryBlue,
                    allowWrap = true
                )
                Spacer(Modifier.height(4.dp))
                VerifyInfoLine(Icons.Default.LocationOn, "Place", item.placeName.ifBlank { "-" }, DarkBlue)
                Spacer(Modifier.height(4.dp))
                VerifyInfoLine(Icons.Default.Agriculture, "Crop", item.cropType.ifBlank { "-" }, PrimaryBlue)
                Spacer(Modifier.height(4.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    VerifyHalf(
                        Modifier.weight(1f),
                        Icons.Default.Payments,
                        "Rate",
                        "₹ ${item.rate} /Qtl",
                        PrimaryBlue
                    )
                    VerifyHalf(
                        Modifier.weight(1f),
                        Icons.Default.SquareFoot,
                        "Quantity",
                        "${item.area} Qtl",
                        DarkBlue
                    )
                }
                Spacer(Modifier.height(4.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    VerifyHalf(
                        Modifier.weight(1f),
                        Icons.Default.AccountCircle,
                        "Hand Deduction",
                        "₹ ${item.perBagDed} /Qtl",
                        PrimaryBlue
                    )
                    VerifyHalf(
                        Modifier.weight(1f),
                        Icons.Default.Schedule,
                        "Due Days",
                        "${item.paymentDueDays} days",
                        DarkBlue
                    )
                }
                Spacer(Modifier.height(4.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    VerifyHalf(
                        Modifier.weight(1f),
                        Icons.Default.Scale,
                        "Weight Deduction",
                        "${item.wtDed} Gram",
                        DarkBlue
                    )
                    VerifyHalf(
                        Modifier.weight(1f),
                        Icons.Default.Percent,
                        "Cash Discount",
                        "${item.cashDisc} %",
                        PrimaryBlue
                    )
                }
                Spacer(Modifier.height(4.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    VerifyHalf(
                        Modifier.weight(1f),
                        Icons.Default.Person,
                        "Approved By",
                        item.saudaApprovedBy.ifBlank { "-" },
                        DarkBlue
                    )

                }

                Spacer(Modifier.height(4.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {

                    VerifyHalf(
                        Modifier.weight(1f),
                        Icons.Default.EventAvailable,
                        "Approved Date",
                        item.saudaApprovedDate.ifBlank { "-" },
                        PrimaryBlue
                    )
                }
                Spacer(Modifier.height(10.dp))

                Button(
                    onClick = onVerify,
                    modifier = Modifier.fillMaxWidth().height(42.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                    enabled = !isVerifying
                ) {
                    Icon(Icons.Default.Verified, null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Verify Sauda", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun VerifyInfoLine(
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
        Icon(icon, null, tint = accent, modifier = Modifier.size(16.dp))
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
private fun VerifyHalf(
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

// ✅ NEW: edit field used inside the edit dialog
@Composable
private fun VerifyEditField(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    onValueChange: (String) -> Unit,
) {
    Column(modifier = modifier) {
        Text(label, fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(4.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
            shape = RoundedCornerShape(10.dp),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = ImeAction.Next),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryBlue,
                unfocusedBorderColor = PrimaryBlue.copy(alpha = 0.35f),
                cursorColor = PrimaryBlue,
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White
            )
        )
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