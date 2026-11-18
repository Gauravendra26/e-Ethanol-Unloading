package com.visibleinfotech.e_ethanolUnloading

import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.visibleinfotech.e_ethanolUnloading.components.MenuRow
import com.visibleinfotech.e_ethanolUnloading.components.WebsiteLinkCard
import com.visibleinfotech.e_ethanolUnloading.components.WelcomeCard
import com.visibleinfotech.e_ethanolUnloading.login.LoginViewModel
import com.visibleinfotech.e_ethanolUnloading.sauda.SaudaRecordsRepository
import com.visibleinfotech.e_ethanolUnloading.sauda.SaudaRepository
import com.visibleinfotech.e_ethanolUnloading.sauda.SaudaVerifyRepository
import com.visibleinfotech.e_ethanolUnloading.sauda.VerifiedSaudaRecordsRepository
import com.visibleinfotech.e_ethanolUnloading.sqlConnection.ConnectionChecker
import com.visibleinfotech.e_ethanolUnloading.sqlConnection.ConnectionStatus
import com.visibleinfotech.e_ethanolUnloading.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet

private const val TAG_DASH = "DashboardLog"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Dashboard(
    onNavigateToGateEntry: () -> Unit,
    onNavigateToGateEntryNew: () -> Unit,
    onNavigateToTestingScreen: () -> Unit,
    onNavigateToIndentAndTokenScreen: () -> Unit,
    onNavigateToDispatchRecordsScreen: () -> Unit,
    onNavigateToGateEntryRecordScreen: () -> Unit,
    onNavigateToSaudaForm: () -> Unit,
    onNavigateToSaudaVerify: () -> Unit,
    onNavigateToSaudaRecordsScreen: () -> Unit,
    onNavigateToVerifiedSaudaRecordsScreen: () -> Unit,
    onNavigateToDispatchForm: () -> Unit,
    onNavigateToUnloadingRecordForm: () -> Unit,
    onNavigateToUnloading: () -> Unit,
    onLogout: () -> Unit,
) {

    val context = LocalContext.current
    val mainConstant = remember { MainConstant(context) }
    val loginViewModel: LoginViewModel = viewModel()

    var showMenu by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    val userSession by mainConstant.userSession.collectAsState()
    val username = userSession.username
    val userCode = userSession.userCode

    var connectionStatus by remember { mutableStateOf(ConnectionStatus.CHECKING) }
    var showConnectionDialog by remember { mutableStateOf(false) }
    var dialogTitle by remember { mutableStateOf("") }
    var dialogMessage by remember { mutableStateOf("") }
    var dialogColor by remember { mutableStateOf(Color.Red) }

    var showDeviceDeactivatedDialog by remember { mutableStateOf(false) }
    var isCheckingDevice by remember { mutableStateOf(false) }

    var showUpdateDialog by remember { mutableStateOf(false) }
    var serverVersion by remember { mutableStateOf<String?>(null) }
    var localVersion by remember { mutableStateOf<String?>(null) }
    var showVersionErrorDialog by remember { mutableStateOf(false) }
    var versionErrorMessage by remember { mutableStateOf("") }
    var versionCheckDone by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        val checker = ConnectionChecker(context)
        while (true) {
            connectionStatus = checker.checkConnection()
            delay(10000)
        }
    }

    var saudaApproveCount by remember { mutableStateOf<Int?>(null) }
    var saudaVerifyCount by remember { mutableStateOf<Int?>(null) }
    var saudaRecordsCount by remember { mutableStateOf<Int?>(null) }
    var verifiedRecordsCount by remember { mutableStateOf<Int?>(null) }

    val saudaRepo            = remember { SaudaRepository(context) }
    val saudaVerifyRepo      = remember { SaudaVerifyRepository(context) }
    val saudaRecordsRepo     = remember { SaudaRecordsRepository(context) }
    val verifiedRecordsRepo  = remember { VerifiedSaudaRecordsRepository(context) }

    LaunchedEffect(userCode) {
        if (userCode.isBlank()) {
            saudaApproveCount = 0
            saudaVerifyCount = 0
            saudaRecordsCount = 0
            verifiedRecordsCount = 0
            return@LaunchedEffect
        }

        while (true) {
            val approve = try {
                saudaRepo.getSaudaList(userCode).size
            } catch (_: Exception) { -1 }

            val verify = try {
                saudaVerifyRepo.getVerifyList(userCode).size
            } catch (_: Exception) { -1 }

            val approveRecords = try {
                saudaRecordsRepo.getApprovedRecords().size
            } catch (_: Exception) { -1 }

            val verifiedRecords = try {
                verifiedRecordsRepo.getVerifiedRecords().size
            } catch (_: Exception) { -1 }

            if (saudaApproveCount    != approve)         saudaApproveCount    = approve
            if (saudaVerifyCount     != verify)          saudaVerifyCount     = verify
            if (saudaRecordsCount    != approveRecords)  saudaRecordsCount    = approveRecords
            if (verifiedRecordsCount != verifiedRecords) verifiedRecordsCount = verifiedRecords

            Log.d(
                TAG_DASH,
                "Counts → approve=$approve verify=$verify approveRec=$approveRecords verifyRec=$verifiedRecords"
            )

            delay(30_000L)
        }
    }

    fun getLocalVersionName(): String {
        return try {
            val packageInfo: PackageInfo = context.packageManager
                .getPackageInfo(context.packageName, 0)
            val v = packageInfo.versionName ?: "N/A"
            Log.d(TAG_DASH, "getLocalVersionName: Local version = $v")
            v
        } catch (e: PackageManager.NameNotFoundException) {
            Log.e(TAG_DASH, "getLocalVersionName: Error getting version", e)
            "N/A"
        }
    }


    suspend fun fetchServerVersion(): Pair<String?, String?> = withContext(Dispatchers.IO) {
        var connection: Connection? = null
        var statement: PreparedStatement? = null
        var rs: ResultSet? = null
        try {
            val dbUser     = mainConstant.getDbUser()
            val dbPass     = mainConstant.getDbPass()
            val dbName     = mainConstant.getDbName()
            val dbIp       = mainConstant.getIp()
            val dbInstance = mainConstant.getInstance()

            val siteCode = mainConstant.getSiteCode()
            val mobile   = mainConstant.getMobile()

            Log.d(TAG_DASH, "========== fetchServerVersion START ==========")
            Log.d(TAG_DASH, "  siteCode=$siteCode, mobile=$mobile")
            Log.d(TAG_DASH, "  DB IP=$dbIp, DB=$dbName, Instance=${dbInstance ?: "<null>"}")

            if (dbIp.isNullOrBlank() || dbName.isNullOrBlank()) {
                Log.w(TAG_DASH, "fetchServerVersion: DB config missing → skipping")
                return@withContext Pair(null, null)
            }
            if (siteCode.isNullOrBlank() || mobile.isNullOrBlank()) {
                Log.w(TAG_DASH, "fetchServerVersion: site/mobile blank → skipping")
                return@withContext Pair(null, null)
            }

            val conStr = com.visibleinfotech.e_ethanolUnloading.sqlConnection.ConnectionStr()
            connection = conStr.connectionclasss(
                dbUser, dbPass, dbName, dbIp, dbInstance
            )

            if (connection == null) {
                Log.e(TAG_DASH, "fetchServerVersion: DB connection failed")
                return@withContext Pair(null, "Unable to connect to the Server")
            }

            val query = "SELECT G_AppVer FROM Vipl_Accounts_master " +
                    "WHERE G_FactoryCode = ? AND G_MobileNo = ?"
            Log.d(TAG_DASH, "fetchServerVersion: Query = $query")
            Log.d(TAG_DASH, "fetchServerVersion: Params = [$siteCode, $mobile]")

            statement = connection.prepareStatement(query)
            statement.queryTimeout = 10
            statement.setString(1, siteCode)
            statement.setString(2, mobile)
            rs = statement.executeQuery()

            return@withContext if (rs.next()) {
                val ver = rs.getString("G_AppVer")?.trim()
                Log.d(TAG_DASH, "fetchServerVersion: Server version = $ver")
                Pair(ver, null)
            } else {
                Log.w(TAG_DASH, "fetchServerVersion: No record found for version check")
                Pair(null, "Record not found")
            }
        } catch (e: Exception) {
            Log.e(TAG_DASH, "fetchServerVersion error: ${e.message}", e)
            Pair(null, "Error: ${e.message}")
        } finally {
            try { rs?.close() } catch (_: Exception) {}
            try { statement?.close() } catch (_: Exception) {}
            try { connection?.close() } catch (_: Exception) {}
            Log.d(TAG_DASH, "========== fetchServerVersion END ==========")
        }
    }

    fun runVersionCheck() {
        scope.launch {
            Log.d(TAG_DASH, "runVersionCheck: starting")
            val lv = getLocalVersionName()
            localVersion = lv

            val (serverVer, errorMsg) = fetchServerVersion()
            serverVersion = serverVer

            if (errorMsg != null && serverVer == null) {
                if (errorMsg.startsWith("Unable to connect")) {
                    versionErrorMessage =
                        "Unable to connect to the server.\nPlease try again later."
                    showVersionErrorDialog = true
                } else {
                    Log.d(TAG_DASH, "runVersionCheck: $errorMsg — no dialog")
                }
                versionCheckDone = true
                return@launch
            }

            val sv = serverVer?.trim()
            val lvTrim = lv.trim()
            Log.d(TAG_DASH, "runVersionCheck: server='$sv' local='$lvTrim'")

            if (!sv.isNullOrBlank() && sv != lvTrim) {
                Log.w(TAG_DASH, "runVersionCheck: ❌ version mismatch → showing update dialog")
                showUpdateDialog = true
            } else {
                Log.d(TAG_DASH, "runVersionCheck: ✅ versions match")
            }
            versionCheckDone = true
        }
    }

    LaunchedEffect(Unit) {
        if (!versionCheckDone) {
            runVersionCheck()
        }
    }

    suspend fun checkDeviceActive(): Boolean = withContext(Dispatchers.IO) {
        var connection: Connection? = null
        var statement: PreparedStatement? = null
        var rs: ResultSet? = null
        try {
            val checkDbUser = "Saini"
            val checkDbPass = "vipl12345"
            val checkDbName = "vipl"
            val checkDbIp = "SG2NWPLS19SQL-v08.mssql.shr.prod.sin2.secureserver.net"
            val checkInstance: String? = null

            val deviceId = mainConstant.getDeviceId()

            Log.d(TAG_DASH, "========== checkDeviceActive START ==========")
            Log.d(TAG_DASH, "  DeviceId    : '${deviceId ?: "<null>"}'")
            Log.d(TAG_DASH, "  DB IP       : $checkDbIp")
            Log.d(TAG_DASH, "  DB Name     : $checkDbName")
            Log.d(TAG_DASH, "  DB User     : $checkDbUser")
            Log.d(TAG_DASH, "  DB Instance : '${checkInstance ?: "<null>"}'")
            Log.d(TAG_DASH, "  Company DB (from MainConstant): '${mainConstant.getDbName()}'")
            Log.d(TAG_DASH, "  LoggedIn    : ${mainConstant.isLoggedIn()}")
            Log.d(TAG_DASH, "=============================================")

            if (deviceId.isNullOrBlank()) {
                Log.e(TAG_DASH, "checkDeviceActive: deviceId is null/blank")
                return@withContext false
            }

            val conStr = com.visibleinfotech.e_ethanolUnloading.sqlConnection.ConnectionStr()

            connection = conStr.connectionclasss(
                checkDbUser,
                checkDbPass,
                checkDbName,
                checkDbIp,
                checkInstance
            )

            if (connection == null) {
                Log.e(TAG_DASH, "checkDeviceActive: DB connection failed (null)")
                return@withContext true
            }

            Log.d(TAG_DASH, "checkDeviceActive: DB connected to '$checkDbName' @ $checkDbIp")

            val query = "SELECT e_active FROM vipl_imei " +
                    "WHERE E_imei3 = ? AND E_App = 'field'"
            Log.d(TAG_DASH, "checkDeviceActive: Query = $query")
            Log.d(TAG_DASH, "checkDeviceActive: Param deviceId = $deviceId")

            statement = connection.prepareStatement(query)
            statement.queryTimeout = 10
            statement.setString(1, deviceId)
            rs = statement.executeQuery()

            if (rs.next()) {
                val eActive = rs.getString("e_active")?.trim() ?: ""
                Log.d(TAG_DASH, "checkDeviceActive: e_active='$eActive'")
                eActive.equals("Yes", ignoreCase = true)
            } else {
                Log.w(TAG_DASH, "checkDeviceActive: device row not found for deviceId=$deviceId")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG_DASH, "checkDeviceActive error: ${e.message}", e)
            true
        } finally {
            try { rs?.close() } catch (_: Exception) {}
            try { statement?.close() } catch (_: Exception) {}
            try { connection?.close() } catch (_: Exception) {}
            Log.d(TAG_DASH, "========== checkDeviceActive END ==========")
        }
    }

    fun handleClickWithDeviceCheck(action: () -> Unit) {
        if (isCheckingDevice) return
        isCheckingDevice = true

        scope.launch {
            Log.d(TAG_DASH, "handleClickWithDeviceCheck: running device-active check...")
            val active = checkDeviceActive()
            isCheckingDevice = false

            Log.d(TAG_DASH, "handleClickWithDeviceCheck: active=$active")

            if (active) {
                action()
            } else {
                Log.w(TAG_DASH, "handleClickWithDeviceCheck: device deactivated → showing dialog")
                showDeviceDeactivatedDialog = true
            }
        }
    }

    fun handleConnectionClick(action: () -> Unit) {
        if (connectionStatus == ConnectionStatus.CONNECTED) {
            handleClickWithDeviceCheck(action)
        } else {
            when (connectionStatus) {
                ConnectionStatus.NO_INTERNET -> {
                    dialogTitle = "No Internet Connection"
                    dialogMessage = "Please enable mobile data or Wi-Fi to continue."
                    dialogColor = ErrorRed
                }
                ConnectionStatus.SERVER_DOWN -> {
                    dialogTitle = "Server Unavailable"
                    dialogMessage = "Unable to connect to the server.\nPlease try again later."
                    dialogColor = WarningYellow
                }
                else -> {
                    dialogTitle = "Checking Connection"
                    dialogMessage = "Please wait while we connect to the server..."
                    dialogColor = Color.Gray
                }
            }
            showConnectionDialog = true
        }
    }

    fun openVisibleInfotechLink() {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.visibleinfotech.in"))
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG_DASH, "Failed to open Visible Infotech link", e)
            Toast.makeText(context, "Unable to open link.", Toast.LENGTH_SHORT).show()
        }
    }

    fun handleLogout() {
        Log.d(TAG_DASH, "=== Logout Started ===")
        try {
            loginViewModel.logout(context)
            mainConstant.logout()

            val prefs = context.getSharedPreferences("e_ethanol_prefs", android.content.Context.MODE_PRIVATE)
            Log.d(TAG_DASH, "After logout, remaining prefs keys: ${prefs.all.keys}")
            Log.d(TAG_DASH, "Username after logout: '${mainConstant.getUsername()}'")
            Log.d(TAG_DASH, "UserCode after logout: '${mainConstant.getUserCode()}'")
            Log.d(TAG_DASH, "IsLoggedIn after logout: ${mainConstant.isLoggedIn()}")

            connectionStatus = ConnectionStatus.CHECKING
            onLogout()

            Log.d(TAG_DASH, "=== Logout Completed ===")
        } catch (e: Exception) {
            Log.e(TAG_DASH, "Error during logout: ${e.message}", e)
            onLogout()
        }
    }

    if (showConnectionDialog) {
        AlertDialog(
            onDismissRequest = { showConnectionDialog = false },
            confirmButton = {
                TextButton(
                    onClick = { showConnectionDialog = false },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "OK",
                        color = dialogColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(dialogColor)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = dialogTitle,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = TextDark
                    )
                }
            },
            text = {
                Text(
                    text = dialogMessage,
                    fontSize = 14.sp,
                    lineHeight = 22.sp,
                    color = Color.Gray
                )
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = WhiteColor,
            tonalElevation = 8.dp
        )
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = {
                Text(
                    text = "Confirm Logout",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDark
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to logout? All session data will be cleared.",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutDialog = false
                        handleLogout()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.shadow(4.dp, RoundedCornerShape(12.dp))
                ) {
                    Text("Logout", color = WhiteColor, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showLogoutDialog = false },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Cancel", color = Color.Gray, fontWeight = FontWeight.Medium)
                }
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = WhiteColor
        )
    }

    if (showDeviceDeactivatedDialog) {
        AlertDialog(
            onDismissRequest = { /* blocked */ },
            properties = androidx.compose.ui.window.DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false
            ),
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Warning",
                    tint = ErrorRed,
                    modifier = Modifier.size(48.dp)
                )
            },
            title = {
                Text(
                    text = "डिवाइस निष्क्रिय / Device Deactivated",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = ErrorRed,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "आपकी डिवाइस प्लांट द्वारा निष्क्रिय कर दी गई है।",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextDark,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "कृपया प्लांट से संपर्क करें।",
                        fontSize = 13.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = Color(0xFFEEEEEE))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Your device has been deactivated by the Plant.\nPlease contact the Plant.",
                        fontSize = 13.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        Log.d(TAG_DASH, "Device-deactivated dialog OK clicked — logging out")
                        showDeviceDeactivatedDialog = false
                        handleLogout()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .shadow(4.dp, RoundedCornerShape(12.dp))
                ) {
                    Text(
                        "OK / ठीक है",
                        color = WhiteColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = WhiteColor
        )
    }

    // ─── Update Available dialog (blocks app usage) ────────────────
    if (showUpdateDialog) {
        AlertDialog(
            onDismissRequest = { /* blocked */ },
            properties = androidx.compose.ui.window.DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false
            ),
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Update",
                    tint = WarningYellow,
                    modifier = Modifier.size(48.dp)
                )
            },
            title = {
                Text(
                    text = "Update Available",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDark,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "A new version of the app is available on the server.",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Server Version  : ${serverVersion ?: "-"}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextDark
                    )
                    Text(
                        text = "Current Version : ${localVersion ?: "-"}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextDark
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Please update the app to continue.",
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        Log.d(TAG_DASH, "Update dialog OK clicked → forcing logout")
                        showUpdateDialog = false
                        handleLogout()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .shadow(4.dp, RoundedCornerShape(12.dp))
                ) {
                    Text("OK / Update", color = WhiteColor, fontWeight = FontWeight.Bold)
                }
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = WhiteColor
        )
    }

    if (showVersionErrorDialog) {
        AlertDialog(
            onDismissRequest = { showVersionErrorDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Error",
                    tint = ErrorRed,
                    modifier = Modifier.size(48.dp)
                )
            },
            title = {
                Text(
                    text = "Connection Error",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = ErrorRed,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Text(
                    text = versionErrorMessage.ifBlank { "Unable to check for updates." },
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showVersionErrorDialog = false
                        runVersionCheck()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Retry", color = WhiteColor, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showVersionErrorDialog = false },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("OK", color = Color.Gray, fontWeight = FontWeight.Medium)
                }
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = WhiteColor
        )
    }


    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.bg),
            contentDescription = "Dashboard Background",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.3f))
        )

        Box(
            modifier = Modifier
                .size(400.dp)
                .offset(x = (-150).dp, y = (-150).dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(VeryLightBlue.copy(alpha = 0.15f), Color.Transparent),
                        radius = 250f
                    )
                )
        )
        Box(
            modifier = Modifier
                .size(300.dp)
                .offset(x = 200.dp, y = 500.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(SoftLavender.copy(alpha = 0.1f), Color.Transparent),
                        radius = 200f
                    )
                )
        )
        Box(
            modifier = Modifier
                .size(200.dp)
                .offset(x = 50.dp, y = 700.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(SoftYellow.copy(alpha = 0.1f), Color.Transparent),
                        radius = 150f
                    )
                )
        )

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        Brush.horizontalGradient(
                                            colors = listOf(PrimaryBlue, BrandViolet)
                                        )
                                    )
                                    .shadow(6.dp, RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "E",
                                    color = WhiteColor,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "e-Ethanol",
                                    color = WhiteColor,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp,
                                    letterSpacing = 0.5.sp
                                )
                                Text(
                                    text = "Ethanol Management",
                                    color = WhiteColor.copy(alpha = 0.7f),
                                    fontSize = 10.sp,
                                    letterSpacing = 0.3.sp
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    ),
                    actions = {
                        Box {
                            IconButton(
                                onClick = { showMenu = true },
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(WhiteColor.copy(alpha = 0.15f))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "Menu",
                                    tint = WhiteColor,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false },
                                containerColor = WhiteColor,
                                shape = RoundedCornerShape(16.dp),
                                tonalElevation = 8.dp
                            ) {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = "Logout",
                                            fontSize = 14.sp,
                                            color = ErrorRed,
                                            fontWeight = FontWeight.Medium
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            Icons.AutoMirrored.Filled.Logout,
                                            contentDescription = "Logout",
                                            tint = ErrorRed
                                        )
                                    },
                                    onClick = {
                                        showMenu = false
                                        showLogoutDialog = true
                                    },
                                    modifier = Modifier
                                        .padding(horizontal = 8.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                )
                            }
                        }
                    }
                )
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .padding(bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {

                    WelcomeCard(
                        username = username,
                        userCode = userCode,
                        connectionStatus = connectionStatus
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(3.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(PrimaryBlue, BrandViolet)
                                    )
                                )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Quick Actions",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = WhiteColor,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            thickness = 1.dp,
                            color = WhiteColor.copy(alpha = 0.3f)
                        )
                    }

                    if (userSession.role.equals("Administrator", ignoreCase = true)) {

                        MenuRow(
                            items = listOf(
                                MenuItem(
                                    title = "SAUDA APPROVE",
                                    icon = R.drawable.unloading,
                                    gradientColors = listOf(PrimaryGreen, DarkGreen),
                                    onClick = onNavigateToSaudaForm,
                                    countLabel = saudaApproveCount
                                        ?.let { if (it < 0) "—" else it.toString() }
                                        ?: "…"
                                ),
                                MenuItem(
                                    title = "SAUDA APPROVE\nRECORD",
                                    icon = R.drawable.unloading_report,
                                    gradientColors = listOf(PrimaryGreen, DarkGreen),
                                    onClick = { onNavigateToSaudaRecordsScreen() },
                                    countLabel = saudaRecordsCount
                                        ?.let { if (it < 0) "—" else it.toString() }
                                        ?: "…"
                                )
                            ),
                            onConnectionClick = { action -> handleConnectionClick(action) }
                        )

                        MenuRow(
                            items = listOf(
                                MenuItem(
                                    title = "VERIFY SAUDA",
                                    icon = R.drawable.unloading,
                                    gradientColors = listOf(PrimaryBlue, DarkBlue),
                                    onClick = onNavigateToSaudaVerify,
                                    countLabel = saudaVerifyCount
                                        ?.let { if (it < 0) "—" else it.toString() }
                                        ?: "…"
                                ),
                                MenuItem(
                                    title = "VERIFY SAUDA\nRECORD",
                                    icon = R.drawable.unloading_report,
                                    gradientColors = listOf(PrimaryBlue, DarkBlue),
                                    onClick = { onNavigateToVerifiedSaudaRecordsScreen() },
                                    countLabel = verifiedRecordsCount
                                        ?.let { if (it < 0) "—" else it.toString() }
                                        ?: "…"
                                )
                            ),
                            onConnectionClick = { action -> handleConnectionClick(action) }
                        )
                    } else {

                        MenuRow(
                            items = listOf(
                                MenuItem(
                                    title = "DISPATCH FORM",
                                    icon = R.drawable.unloading,
                                    gradientColors = listOf(IndentTeal, IndentCoralDark),
                                    onClick = onNavigateToDispatchForm
                                ),
                                MenuItem(
                                    title = "DISPATCH RECORDS",
                                    icon = R.drawable.unloading_report,
                                    gradientColors = listOf(IndentTeal, IndentCoralDark),
                                    onClick = { onNavigateToDispatchRecordsScreen() }
                                )
                            ),
                            onConnectionClick = { action -> handleConnectionClick(action) }
                        )
                        MenuRow(
                            items = listOf(
                                MenuItem(
                                    title = "UNLOADING",
                                    icon = R.drawable.unloading,
                                    gradientColors = listOf(PrimaryGreen, DarkGreen),
                                    onClick = onNavigateToUnloading
                                ),
                                MenuItem(
                                    title = "UNLOADING REPORT",
                                    icon = R.drawable.unloading_report,
                                    gradientColors = listOf(PrimaryGreen, DarkGreen),
                                    onClick = { onNavigateToUnloadingRecordForm() }
                                )
                            ),
                            onConnectionClick = { action -> handleConnectionClick(action) }
                        )
                    }
                }

                WebsiteLinkCard(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 90.dp),
                    onWebsiteClick = { openVisibleInfotechLink() }
                )

                if (isCheckingDevice) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.35f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = WhiteColor)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    color = PrimaryBlue,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Verifying device…",
                                    fontSize = 14.sp,
                                    color = TextDark,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

data class MenuItem(
    val title: String,
    val icon: Int,
    val gradientColors: List<Color>,
    val onClick: () -> Unit,
    val countLabel: String? = null,
)