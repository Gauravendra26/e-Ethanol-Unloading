package com.visibleinfotech.e_ethanolUnloading.login

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.visibleinfotech.e_ethanolUnloading.MainConstant
import com.visibleinfotech.e_ethanolUnloading.ui.theme.PrimaryBlue
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import android.widget.Toast
import com.visibleinfotech.e_ethanolUnloading.ui.theme.DarkBlue
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.visibleinfotech.e_ethanolUnloading.R

private const val TAG_MAIN = "MainConstant_Debug"

@Composable
fun LoginScreen(
    onNavigateToRegister: () -> Unit = {},
    onNavigateToDashboard: () -> Unit = {}
) {

    val context = LocalContext.current
    val viewModel: LoginViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()

    val appFontFamily = FontFamily(
        Font(R.font.poppins_regular)
    )

    LaunchedEffect(Unit) {
        logMainConstantState(context, "LOGIN SCREEN ENTERED (after logout)")
    }

    LaunchedEffect(Unit) {
        viewModel.resetUIState()
    }

    LaunchedEffect(Unit) {
        viewModel.getDeviceId(context)
    }

    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFF8FBFF),
                            Color(0xFFDCEBFF),
                            Color(0xFF8CB8FF),
                            Color(0xFF3D7BFF),
                            Color(0xFF1E4FCB),
                            Color(0xFF0D2A7A)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Spacer(modifier = Modifier.height(70.dp))

            Box(
                contentAlignment = Alignment.Center
            ) {

                Card(
                    modifier = Modifier
                        .size(135.dp),
                    shape = RoundedCornerShape(40.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 15.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White.copy(alpha = 0.95f)
                    )
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(R.drawable.logo_new),
                            contentDescription = null
                        )
                    }
                }

                Card(
                    modifier = Modifier
                        .size(150.dp)
                        .offset(y = 6.dp),
                    shape = RoundedCornerShape(45.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White.copy(alpha = 0.08f)
                    )
                ) {}
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "e-Ethanol Control",
                fontSize = 30.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = appFontFamily,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Smart Ethanol Management System",
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.85f),
                fontFamily = appFontFamily,
                letterSpacing = 0.8.sp
            )

            Spacer(modifier = Modifier.height(35.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 30.dp),
                shape = RoundedCornerShape(28.dp),
                elevation = CardDefaults.cardElevation(8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                )
            ) {

                Column(
                    modifier = Modifier.padding(24.dp)
                ) {

                    Text(
                        text = "Login",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkBlue,
                        fontFamily = appFontFamily
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "Mobile Number",
                        color = DarkBlue,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = uiState.mobileNumber,
                        onValueChange = {
                            if (it.length <= 10 && it.all { c -> c.isDigit() }) {
                                viewModel.updateMobileNumber(it)
                            }
                        },
                        leadingIcon = {
                            Icon(
                                painter = painterResource(android.R.drawable.sym_action_call),
                                contentDescription = null,
                                tint = PrimaryBlue,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        placeholder = {
                            Text(
                                text = "Mobile Number",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Phone
                        ),
                        textStyle = LocalTextStyle.current.copy(
                            fontSize = 14.sp
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedBorderColor = PrimaryBlue,
                            unfocusedBorderColor = Color(0xFFDADADA),
                            cursorColor = PrimaryBlue
                        )
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Login Button
                    Button(
                        onClick = {
                            if (viewModel.isDeviceIdAvailable()) {
                                viewModel.login(context) {
                                    viewModel.clearFormAndReset()
                                    onNavigateToDashboard()
                                }
                            } else {
                                Toast.makeText(
                                    context,
                                    "Getting Device ID...",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryBlue
                        )
                    ) {

                        if (uiState.isLoading) {

                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )

                        } else {

                            Text(
                                text = "LOGIN",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Register Button
                    OutlinedButton(
                        onClick = onNavigateToRegister,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {

                        Text(
                            text = "REGISTER DEVICE",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = PrimaryBlue
                        )
                    }


                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Card(
                modifier = Modifier.padding(bottom = 40.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White.copy(alpha = 0.15f)
                ),
                shape = RoundedCornerShape(50.dp)
            ) {

                Text(
                    text = "🔒 Secure Login • Device Verified",
                    modifier = Modifier.padding(
                        horizontal = 16.dp,
                        vertical = 8.dp
                    ),
                    color = Color.White,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
        }

        // Error Dialog
        if (uiState.errorMessage != null) {
            Dialog(
                onDismissRequest = {
                    viewModel.clearError()
                    viewModel.clearFormAndReset()
                },
                properties = DialogProperties(
                    usePlatformDefaultWidth = false,
                    decorFitsSystemWindows = false
                )
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .wrapContentHeight(),
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = 20.dp
                    ),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .background(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            Color(0xFFFFEBEE),
                                            Color(0xFFFFCDD2)
                                        )
                                    ),
                                    shape = RoundedCornerShape(50)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(android.R.drawable.ic_dialog_alert),
                                contentDescription = "Error",
                                tint = Color(0xFFE53935),
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Login Failed",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = DarkBlue,
                            fontFamily = appFontFamily
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFFFFF5F5)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = uiState.errorMessage!!,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                color = Color(0xFFC62828),
                                fontSize = 14.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                fontFamily = appFontFamily
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = {
                                viewModel.clearError()
                                viewModel.clearFormAndReset()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PrimaryBlue,
                                contentColor = Color.White
                            ),
                            elevation = ButtonDefaults.buttonElevation(
                                defaultElevation = 4.dp,
                                pressedElevation = 2.dp
                            )
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    painter = painterResource(android.R.drawable.ic_menu_revert),
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = Color.White
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "TRY AGAIN",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color.White
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        TextButton(
                            onClick = {
                                viewModel.clearError()
                                viewModel.clearFormAndReset()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp)
                        ) {
                            Text(
                                text = "Cancel",
                                color = Color.Gray,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        // Company Selection Dialog
        if (uiState.showCompanySelectionDialog && uiState.companyList.isNotEmpty()) {
            AlertDialog(
                onDismissRequest = {
                    viewModel.dismissCompanyDialog()
                    viewModel.clearFormAndReset()
                },
                title = {
                    Text(
                        "Select Company",
                        color = PrimaryBlue
                    )
                },
                text = {
                    Column {
                        Text(
                            text = "Your mobile number is registered with multiple companies. Please select one:",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        var selectedIndex by remember { mutableIntStateOf(0) }

                        uiState.companyList.forEachIndexed { index, company ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable { selectedIndex = index },
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (selectedIndex == index)
                                        PrimaryBlue.copy(alpha = 0.1f)
                                    else
                                        Color.White
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = selectedIndex == index,
                                        onClick = { selectedIndex = index },
                                        colors = RadioButtonDefaults.colors(
                                            selectedColor = PrimaryBlue
                                        )
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = company.companyName,
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 16.sp
                                        )
                                        Text(
                                            text = "Company Code: ${company.loginDetail.companyCode}",
                                            fontSize = 12.sp,
                                            color = Color.Gray
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                viewModel.selectCompany(selectedIndex, context) {
                                    viewModel.clearFormAndReset()
                                    onNavigateToDashboard()
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PrimaryBlue
                            )
                        ) {
                            Text("Proceed to Dashboard")
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = {
                        viewModel.dismissCompanyDialog()
                        viewModel.clearFormAndReset()
                    }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

private fun logMainConstantState(context: android.content.Context, label: String) {
    val mc = MainConstant(context)

    Log.d(TAG_MAIN, "═══════════════════════════════════════════════════")
    Log.d(TAG_MAIN, "🔎 MainConstant STATE DUMP: $label")
    Log.d(TAG_MAIN, "═══════════════════════════════════════════════════")

    Log.d(TAG_MAIN, "───────── UserSession ─────────")
    Log.d(TAG_MAIN, "  username      = '${mc.getUsername()}'")
    Log.d(TAG_MAIN, "  userCode      = '${mc.getUserCode()}'")
    Log.d(TAG_MAIN, "  mobile        = '${mc.getMobile()}'")
    Log.d(TAG_MAIN, "  eCode         = '${mc.geteCode()}'")
    Log.d(TAG_MAIN, "  ip            = '${mc.getIp()}'")
    Log.d(TAG_MAIN, "  dbName        = '${mc.getDbName()}'")
    Log.d(TAG_MAIN, "  dbUser        = '${mc.getDbUser()}'")
    Log.d(TAG_MAIN, "  dbPass        = '${if (mc.getDbPass().isEmpty()) "<empty>" else "***hidden***"}'")
    Log.d(TAG_MAIN, "  instance      = '${mc.getInstance()}'")
    Log.d(TAG_MAIN, "  ftpPath       = '${mc.getFtpPath()}'")
    Log.d(TAG_MAIN, "  ftpUser       = '${mc.getFtpUser()}'")
    Log.d(TAG_MAIN, "  ftpServer     = '${mc.getFtpServer()}'")
    Log.d(TAG_MAIN, "  ftpPass       = '${if (mc.getFtpPass().isEmpty()) "<empty>" else "***hidden***"}'")
    Log.d(TAG_MAIN, "  siteCode      = '${mc.getSiteCode()}'")
    Log.d(TAG_MAIN, "  companyName   = '${mc.getCompanyName()}'")
    Log.d(TAG_MAIN, "  surveyArea    = ${mc.getSurveyArea()}")
    Log.d(TAG_MAIN, "  surveyAcc     = ${mc.getSurveyAccuracy()}")
    Log.d(TAG_MAIN, "  surveyAccBrix = ${mc.getSurveyAccuracyBrix()}")
    Log.d(TAG_MAIN, "  role          = '${mc.getRole()}'")

    Log.d(TAG_MAIN, "───────── AppConfig ─────────")
    Log.d(TAG_MAIN, "  locMin              = '${mc.getLocMin()}'")
    Log.d(TAG_MAIN, "  seasonName          = '${mc.getSeasonName()}'")
    Log.d(TAG_MAIN, "  seasonCode          = '${mc.getSeasonCode()}'")
    Log.d(TAG_MAIN, "  imageApiUrl         = '${mc.getImageApiUrl()}'")
    Log.d(TAG_MAIN, "  imageDownloadApiUrl = '${mc.getImageDownloadApiUrl()}'")

    Log.d(TAG_MAIN, "───────── Permissions ─────────")
    Log.d(TAG_MAIN, "  mapChange     = '${mc.getMapPermission()}'")
    Log.d(TAG_MAIN, "  outSurvey     = '${mc.getOutSurveyPermission()}'")
    Log.d(TAG_MAIN, "  deleteSurvey  = '${mc.getDeleteSurveyPermission()}'")

    Log.d(TAG_MAIN, "───────── Company Info ─────────")
    Log.d(TAG_MAIN, "  cName         = '${mc.getCName()}'")
    Log.d(TAG_MAIN, "  cCode         = '${mc.getCCode()}'")
    Log.d(TAG_MAIN, "  companyList   = ${mc.getCompanyInfoList()}")
    Log.d(TAG_MAIN, "  deviceId      = '${mc.getDeviceId()}'")

    Log.d(TAG_MAIN, "───────── Raw SharedPreferences ─────────")
    try {
        val prefs = context.getSharedPreferences("e_ethanol_prefs", android.content.Context.MODE_PRIVATE)
        val all = prefs.all
        if (all.isEmpty()) {
            Log.d(TAG_MAIN, "  ✅ SharedPreferences is EMPTY (logout successful)")
        } else {
            Log.w(TAG_MAIN, "  ⚠️ SharedPreferences still contains ${all.size} keys:")
            all.forEach { (key, value) ->
                Log.w(TAG_MAIN, "     • $key = $value")
            }
        }
    } catch (e: Exception) {
        Log.e(TAG_MAIN, "  ❌ Error reading SharedPreferences: ${e.message}")
    }

    Log.d(TAG_MAIN, "───────── Login Status ─────────")
    Log.d(TAG_MAIN, "  isLoggedIn    = ${mc.isLoggedIn()}")

    Log.d(TAG_MAIN, "═══════════════════════════════════════════════════")
}