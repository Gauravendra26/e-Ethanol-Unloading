package com.visibleinfotech.e_ethanolUnloading.indentAndToken

import android.widget.Toast
import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.visibleinfotech.e_ethanolUnloading.login.*
import com.visibleinfotech.e_ethanolUnloading.ui.theme.*
import com.visibleinfotech.e_ethanolUnloading.utils.DateTimeUtils
import com.visibleinfotech.e_ethanolUnloading.utils.KeyboardUtils
import com.visibleinfotech.e_ethanolUnloading.utils.SetStatusBarColor
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun IndentAndTokenScreen(
    onBack: () -> Unit = {},
    onSuccess: () -> Unit = {},
    viewModel: IndentAndTokenViewModel = viewModel(
        factory = IndentAndTokenViewModelFactory(LocalContext.current)
    )
) {
    SetStatusBarColor(IndentTeal)

    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val hideKeyboard = KeyboardUtils.hideKeyboard()
    val view = LocalView.current

    var currentDateTime by remember { mutableStateOf(DateTimeUtils.getCurrentDateTime()) }
    var showConfirmationDialog by remember { mutableStateOf(false) }

    val truckFocusRequester        = remember { FocusRequester() }
    val tokenFocusRequester        = remember { FocusRequester() }
    val driverNameFocusRequester   = remember { FocusRequester() }
    val driverMobileFocusRequester = remember { FocusRequester() }

    val isOptionSelected = uiState.selectedOption != null

    LaunchedEffect(Unit) {
        while (true) { delay(1000); currentDateTime = DateTimeUtils.getCurrentDateTime() }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.setErrorMessage(null)
        }
    }
    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.setSuccessMessage(null)
        }
    }

    BackHandler {
        focusManager.clearFocus(); viewModel.resetForm(); onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Indent & Token", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = {
                        focusManager.clearFocus(); viewModel.resetForm(); onBack()
                    }) {
                        Icon(
                            painter = painterResource(id = android.R.drawable.ic_menu_revert),
                            contentDescription = "Back", tint = Color.White
                        )
                    }
                },
                actions = {
                    OutlinedButton(
                        onClick = {
                            viewModel.resetForm()
                            focusManager.clearFocus(); hideKeyboard()
                            Toast.makeText(context, "Form reset", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.padding(end = 8.dp).height(36.dp),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.5f)),
                        contentPadding = PaddingValues(horizontal = 10.dp)
                    ) {
                        Icon(Icons.Default.Refresh, null, tint = Color.White,
                            modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Reset", color = Color.White, fontSize = 12.sp,
                            fontWeight = FontWeight.Medium)
                    }
                },

                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                modifier = Modifier.background(
                    Brush.horizontalGradient(
                        colors = listOf(IndentTeal, IndentCoralDark)
                    )
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                // ✅ Soft mint background instead of purple
                .background(IndentLightTeal.copy(alpha = 0.5f))
                .padding(paddingValues)
                .clickable { view.requestFocus() }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Spacer(Modifier.height(4.dp))

                DateTimeCard(
                    dateTime = currentDateTime,
                    textColor = IndentTeal,
                    backgroundColor = Color.White
                )

                FormSectionCard(title = "Select Pending Entry", IndentTeal) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (uiState.isLoadingList) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(color = IndentTeal,
                                    modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                Spacer(Modifier.width(8.dp))
                                Text("Loading pending entries...", fontSize = 12.sp)
                            }
                        } else {
                            ModernSearchableDropdown(
                                searchText = uiState.searchText,
                                onSearchTextChange = viewModel::updateSearchText,
                                onClearClick = {
                                    viewModel.clearSelectedOption(); focusManager.clearFocus()
                                },
                                label = "Select Truck",
                                items = uiState.filteredOptions,
                                getItemLabel = { option -> option.truckNo },
                                getItemId = { option -> option.gCode.toString() },
                                onItemSelected = { option ->
                                    viewModel.selectOption(option)
                                    focusManager.clearFocus()
                                    tokenFocusRequester.requestFocus()
                                },
                                onDropdownDismiss = { focusManager.clearFocus() },
                                focusRequester = truckFocusRequester,
                                accentColor = IndentTeal,
                                itemIcon = Icons.Default.LocalShipping,
                                itemIconColor = IndentTeal,
                                itemTextColor = Color.Black,
                                isError = uiState.showValidationErrors && !isOptionSelected,
                                errorMessage = if (uiState.showValidationErrors &&
                                    !isOptionSelected
                                ) "Please select a pending entry" else null,
                                noResultsMessage = "No pending entries found"
                            )
                        }

                        if (uiState.isLoadingDetails) {
                            LinearProgressIndicator(
                                modifier = Modifier.fillMaxWidth(),
                                color = IndentCoral,
                                trackColor = IndentCoral.copy(alpha = 0.2f)
                            )
                        }
                    }
                }

                if (isOptionSelected) {
                    uiState.selectedEntry?.let { entry ->

                        FormSectionCard(title = "Pending Entry Details", IndentTeal) {
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                DispatchReadOnlyRow("vehicle No", entry.truckNo,
                                    Icons.Default.LocalShipping)
                                DispatchReadOnlyRow("Quantity",
                                    (entry.qty.ifBlank { "-" }) + " Quintal",
                                    Icons.Default.ProductionQuantityLimits)
                                DispatchReadOnlyRow("Party",
                                    entry.partyName.ifBlank { "-" }, Icons.Default.Business)
                                DispatchReadOnlyRow("Broker",
                                    entry.brokerName.ifBlank { "-" }, Icons.Default.People)
                                DispatchReadOnlyRow("Mode Name",
                                    entry.modeName.ifBlank { "-" }, Icons.Default.Settings)
                                DispatchReadOnlyRow("Entry Date",
                                    entry.date.ifBlank { "-" }, Icons.Default.CalendarToday)
                            }
                        }


                    } ?: Card(
                        modifier = Modifier.fillMaxWidth(),
                        // ✅ Soft coral warning card
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, IndentCoral.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Info, null, tint = IndentCoralDark)
                            Spacer(Modifier.width(8.dp))
                            Text("No entry found for this selection.",
                                fontSize = 13.sp, color = IndentCoralDark)
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Please select a pending entry to continue",
                            color = Color.Gray, fontSize = 14.sp,
                            fontWeight = FontWeight.Medium)
                    }
                }

                Spacer(Modifier.height(4.dp))

                if (isOptionSelected && uiState.selectedEntry != null) {
                    PrimarySubmitButton(
                        onClick = {

                            focusManager.clearFocus()

                            val isValid = viewModel.isFormValid()

                            if (!isValid) {
                                viewModel.setShowValidationErrors(true)
                                Toast.makeText(context,
                                    "Please fill all required fields",
                                    Toast.LENGTH_LONG).show()
                                  return@PrimarySubmitButton
                            }

                            showConfirmationDialog = true
                        },
                        text = "Generate Token & Indent",
                        gradientColors = listOf(IndentTeal, IndentCoral),
                        shadowColor = IndentTeal,
                        isLoading = uiState.isSubmitting
                    )
                }

                Spacer(Modifier.height(120.dp))
            }
        }
    }

    if (showConfirmationDialog) {
        IndentConfirmationDialog(
            truckNo       = uiState.selectedEntry?.truckNo.orEmpty(),
            gCode         = uiState.selectedEntry?.gCode?.toString() ?: "-",
            doNo          = uiState.doNo,
            driverName    = uiState.driverName,
            driverMobile  = uiState.driverMobile,
            remarks       = uiState.remarks,
            onConfirm = {
                showConfirmationDialog = false

                val updateBy = com.visibleinfotech.e_ethanolUnloading.MainConstant(context)
                    .getUsername()
                    .ifBlank { "ADMIN" }

                viewModel.submitToken(updateBy = updateBy) { ok, msg ->
                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                    if (ok) {
                        viewModel.resetForm()
                        onSuccess()
                    }
                }
            },
            onDismiss = { showConfirmationDialog = false }
        )
    }
}

@Composable
private fun IndentConfirmationDialog(
    truckNo: String, gCode: String, doNo: String,
    driverName: String, driverMobile: String, remarks: String,
    onConfirm: () -> Unit, onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = "Confirm",
                    fontWeight = FontWeight.Bold,
                    color = IndentTeal  // ✅ Teal instead of purple
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Token & Indent Generation",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal,
                    color = IndentTeal.copy(alpha = 0.7f)
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Are you sure you want to generate the token & indent for this entry?",
                    fontSize = 13.sp,
                    color = Color.DarkGray,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                HorizontalDivider(color = IndentTeal.copy(alpha = 0.3f))
                Spacer(Modifier.height(4.dp))

                DetailRow("Vehicle No", truckNo)
                DetailRow("Driver", driverName)
                DetailRow("Mobile", driverMobile)
                if (remarks.isNotBlank()) DetailRow("Remarks", remarks)
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                // ✅ Coral confirm button (pops against teal)
                colors = ButtonDefaults.buttonColors(containerColor = IndentCoral),
                shape = RoundedCornerShape(8.dp)
            ) { Text("Confirm", color = Color.White, fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF757575))
            ) { Text("Cancel") }
        },
        shape = RoundedCornerShape(16.dp),
        containerColor = Color.White
    )
}