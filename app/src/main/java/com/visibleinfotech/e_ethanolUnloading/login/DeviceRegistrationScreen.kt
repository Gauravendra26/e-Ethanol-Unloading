package com.visibleinfotech.e_ethanolUnloading.login

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.visibleinfotech.e_ethanolUnloading.ui.theme.PrimaryBlue
import com.visibleinfotech.e_ethanolUnloading.ui.theme.PrimaryGreen

@Composable
fun DeviceRegistrationScreen(
    onRegistrationComplete: () -> Unit = {},
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val viewModel: RegistrationViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()

    // Create scroll state
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(scrollState) // Added verticalScroll
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(40.dp))

        Text(
            text = "Register Device",
            fontSize = 24.sp,
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Company Code Field
        OutlinedTextField(
            value = uiState.companyCode.uppercase(),
            onValueChange = {
                viewModel.updateCompanyCode(it.uppercase())
            },
            label = { Text("Company Unique Code") },
            placeholder = { Text("Enter Company Unique code") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Next
            ),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            isError = uiState.companyCodeError != null,
            supportingText = {
                when {
                    uiState.companyCodeError != null -> {
                        Text(uiState.companyCodeError!!, color = MaterialTheme.colorScheme.error)
                    }
                    uiState.isVerifyingCompany -> {
                        Text("Verifying company code...", color = MaterialTheme.colorScheme.primary)
                    }
                }
            },
            enabled = !uiState.isVerifyingCompany && !uiState.isLoading
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Company Name Field (Read-only)
        OutlinedTextField(
            value = uiState.companyName,
            onValueChange = {},
            label = { Text("Company Name") },
            readOnly = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                disabledContainerColor = Color.LightGray.copy(alpha = 0.3f),
                disabledTextColor = MaterialTheme.colorScheme.onSurface
            ),
            enabled = false,
            supportingText = {
                if (uiState.companyName.isEmpty() && uiState.companyCode.length == 15 && !uiState.isVerifyingCompany) {
                    Text("Invalid company code", color = MaterialTheme.colorScheme.error)
                }
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Simple Server Status TextView
        if (uiState.companyName.isNotEmpty() && !uiState.isVerifyingCompany) {
            Text(
                text = when {
                    uiState.isCheckingServer -> "Checking server connection..."
                    uiState.isServerActive -> "✓ Server Active"
                    else -> "✗ Server Not Active"
                },
                fontSize = 14.sp,
                color = when {
                    uiState.isCheckingServer -> Color.Gray
                    uiState.isServerActive -> Color.Green
                    else -> Color.Red
                },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Mobile Number Field
        OutlinedTextField(
            value = uiState.mobileNumber,
            onValueChange = { viewModel.updateMobileNumber(it) },
            label = { Text("Mobile Number") },
            placeholder = { Text("Enter 10-digit mobile number") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Phone,
                imeAction = ImeAction.Next
            ),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            isError = uiState.mobileError != null,
            supportingText = {
                when {
                    uiState.mobileError != null -> {
                        Text(uiState.mobileError!!, color = MaterialTheme.colorScheme.error)
                    }
                    uiState.isVerifyingMobile -> {
                        Text("Verifying mobile number...", color = MaterialTheme.colorScheme.primary)
                    }
                }
            },
            enabled = uiState.companyName.isNotEmpty() && uiState.isServerActive && !uiState.isVerifyingMobile && !uiState.isLoading
        )

        Spacer(modifier = Modifier.height(12.dp))

        // User Name Field (Auto-fill)
        OutlinedTextField(
            value = uiState.userName,
            onValueChange = { },
            label = { Text("Full Name") },
            placeholder = { Text("Name will auto-fill after mobile verification") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            readOnly = true,
            colors = OutlinedTextFieldDefaults.colors(
                disabledContainerColor = Color.LightGray.copy(alpha = 0.3f),
                disabledTextColor = MaterialTheme.colorScheme.onSurface
            ),
            enabled = false,
            isError = uiState.userName.isEmpty() && uiState.mobileNumber.length == 10 && !uiState.isVerifyingMobile && uiState.mobileError == null,
            supportingText = {
                when {
                    uiState.userName.isEmpty() && uiState.mobileNumber.length == 10 && !uiState.isVerifyingMobile && uiState.mobileError == null -> {
                        Text("Mobile number not found in database", color = MaterialTheme.colorScheme.error)
                    }
                    uiState.userName.isNotEmpty() -> {
                        Text("✓ Name verified", color = Color.Green)
                    }
                }
            }
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Register Button
        Button(
            onClick = {
                viewModel.registerDevice(context) {
                    // This will be called after dialog is dismissed
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
            enabled = !uiState.isLoading &&
                    uiState.companyCode.length == 15 &&
                    uiState.companyName.isNotEmpty() &&
                    uiState.isServerActive &&
                    uiState.mobileNumber.length == 10 &&
                    uiState.userName.isNotEmpty() &&
                    uiState.mobileError == null &&
                    uiState.companyCodeError == null &&
                    !uiState.isVerifyingCompany &&
                    !uiState.isVerifyingMobile
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
            } else {
                Text("Register Device", fontSize = 18.sp, color = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Back Button
        TextButton(
            onClick = onBack,
            enabled = !uiState.isLoading,
            modifier = Modifier.padding(bottom = 16.dp) // Added padding at bottom
        ) {
            Text("Back to Login")
        }

        // Add extra bottom padding for better scrolling
        Spacer(modifier = Modifier.height(20.dp))

        // Error Dialog
        if (uiState.errorMessage != null) {
            AlertDialog(
                onDismissRequest = { viewModel.clearError() },
                text = { Text(uiState.errorMessage!!) },
                confirmButton = {
                    TextButton(onClick = { viewModel.clearError() }) {
                        Text("OK")
                    }
                }
            )
        }

        // Success Dialog
        if (uiState.showSuccessDialog) {
            AlertDialog(
                onDismissRequest = { },
                title = {
                    Text(
                        "Registration Successful",
                        color = PrimaryGreen
                    )
                },
                text = {
                    Text("Your device has been registered successfully.\n\nPlease contact admin for Active.")
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.dismissSuccessDialog()
                            viewModel.resetForm()
                            onRegistrationComplete()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                    ) {
                        Text("OK")
                    }
                }
            )
        }
    }
}