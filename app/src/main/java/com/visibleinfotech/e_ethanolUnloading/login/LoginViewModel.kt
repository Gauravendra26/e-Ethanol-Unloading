package com.visibleinfotech.e_ethanolUnloading.login

import android.content.Context
import android.media.MediaDrm
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.visibleinfotech.e_ethanolUnloading.MainConstant
import com.visibleinfotech.e_ethanolUnloading.sqlConnection.ConnectionStr
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.sql.Connection
import java.sql.ResultSet
import java.sql.SQLException
import java.sql.Statement
import java.util.UUID

data class LoginUiState(
    val mobileNumber: String = "",
    val mobileError: String? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val showCompanySelectionDialog: Boolean = false,
    val companyList: List<CompanyDetail> = emptyList(),
    val loginDetails: List<LoginDetail> = emptyList()
)

data class CompanyDetail(
    val companyName: String,
    val loginDetail: LoginDetail
)

class LoginViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private var deviceId: String? = null
    private val tag = "LoginViewModelData"
    private val mainDbIp   = "SG2NWPLS19SQL-v08.mssql.shr.prod.sin2.secureserver.net"
    private val mainDbName = "vipl"
    private val mainDbUser = "Saini"
    private val mainDbPass = "vipl12345"
    private val USE_TEST_DB = false
    private val TEST_DB_NAME = "vipltest16"

    init {
        Log.d(tag, "=== LoginViewModel INITIALIZED ===")
        Log.d(tag, "Main DB Config - IP: $mainDbIp, DB: $mainDbName, User: $mainDbUser")
        Log.d(tag, "USE_TEST_DB = $USE_TEST_DB, TEST_DB_NAME = $TEST_DB_NAME")
    }

    fun updateMobileNumber(mobile: String) {
        Log.d(tag, "updateMobileNumber called with: $mobile")

        val filteredMobile = mobile.filter { it.isDigit() }.take(10)
        Log.d(tag, "Filtered mobile: $filteredMobile, Length: ${filteredMobile.length}")

        val error = if (filteredMobile.isNotEmpty() && filteredMobile.length != 10)
            "Mobile number must be 10 digits" else null

        if (error != null) {
            Log.w(tag, "Mobile validation error: $error")
        }

        _uiState.update {
            it.copy(
                mobileNumber = filteredMobile,
                mobileError = error
            )
        }

        Log.d(tag, "Mobile number updated successfully")
    }

    fun getDeviceId(context: Context) {
        Log.d(tag, "=== getDeviceId START ===")

        if (deviceId != null) {
            Log.d(tag, "Device ID already available: $deviceId")
            return
        }

        try {
            Log.d(tag, "Getting WideVine device ID")
            val wideVineUuid = UUID(-0x121074568629b532L, -0x5c37d8232ae2de13L)
            Log.d(tag, "WideVine UUID: $wideVineUuid")

            val wvDrm = MediaDrm(wideVineUuid)
            val wideVineId = wvDrm.getPropertyByteArray(MediaDrm.PROPERTY_DEVICE_UNIQUE_ID)
            deviceId = bytesToHex(wideVineId)

            Log.d(tag, "Device ID retrieved successfully: $deviceId")
            Log.d(tag, "Device ID length: ${deviceId?.length}")

            _uiState.update { it.copy(errorMessage = null) }

        } catch (e: Exception) {
            Log.e(tag, "Failed to get device ID: ${e.message}", e)
            deviceId = null
            _uiState.update { it.copy(errorMessage = null) }
        }

        Log.d(tag, "=== getDeviceId END ===")
    }

    fun login(context: Context, onSuccess: () -> Unit) {
        Log.d(tag, "=== login START ===")

        val mobile = _uiState.value.mobileNumber
        Log.d(tag, "Login attempt with mobile: $mobile")

        if (mobile.isEmpty()) {
            Log.w(tag, "Login failed: Mobile number empty")
            _uiState.update { it.copy(mobileError = "Please enter mobile number") }
            return
        }

        if (mobile.length != 10) {
            Log.w(tag, "Login failed: Mobile number length ${mobile.length} != 10")
            _uiState.update { it.copy(mobileError = "Mobile number must be 10 digits") }
            return
        }

        if (deviceId == null) {
            Log.e(tag, "Login failed: Device ID not available")
            _uiState.update { it.copy(errorMessage = "Device ID not available") }
            return
        }

        Log.d(tag, "All validations passed. Device ID: $deviceId")
        Log.d(tag, "Starting login coroutine")

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            Log.d(tag, "Launching login coroutine on viewModelScope")

            val result = withContext(Dispatchers.IO) {
                Log.d(tag, "Executing login on IO thread")
                performLogin(mobile, deviceId!!)
            }

            Log.d(tag, "Login completed. Result type: ${result::class.simpleName}")
            _uiState.update { it.copy(isLoading = false) }

            when (result) {
                is LoginResult.Success -> {
                    Log.d(tag, "Login SUCCESS. Companies found: ${result.loginDetails.size}")

                    result.loginDetails.forEachIndexed { index, detail ->
                        Log.d(tag, "Company ${index + 1}: ${detail.companyName}, Active: ${detail.isActive}")
                    }

                    if (result.loginDetails.size > 1) {
                        Log.d(tag, "Multiple companies found. Showing company selection dialog")
                        _uiState.update {
                            it.copy(
                                showCompanySelectionDialog = true,
                                companyList = result.loginDetails.map { detail ->
                                    CompanyDetail(detail.companyName, detail)
                                },
                                loginDetails = result.loginDetails
                            )
                        }
                    } else if (result.loginDetails.isNotEmpty()) {
                        Log.d(tag, "Single company found. Proceeding with login for: ${result.loginDetails.first().companyName}")
                        proceedWithLogin(context, result.loginDetails.first(), onSuccess)
                    } else {
                        Log.e(tag, "No company details found in success result")
                        _uiState.update { it.copy(errorMessage = "No company details found") }
                    }
                }
                is LoginResult.Error -> {
                    Log.e(tag, "Login FAILED: ${result.message}")
                    _uiState.update { it.copy(errorMessage = result.message) }
                }
            }

            Log.d(tag, "=== login END ===")
        }
    }

    fun selectCompany(index: Int, context: Context, onSuccess: () -> Unit) {
        Log.d(tag, "=== selectCompany START ===")
        Log.d(tag, "Selected company index: $index")

        val loginDetails = _uiState.value.loginDetails
        Log.d(tag, "Total companies available: ${loginDetails.size}")

        if (index in loginDetails.indices) {
            val selectedCompany = loginDetails[index]
            Log.d(tag, "Selected company: ${selectedCompany.companyName}")

            proceedWithLogin(context, selectedCompany) {
                Log.d(tag, "Company login successful, dismissing dialog and calling onSuccess")
                _uiState.update { it.copy(showCompanySelectionDialog = false) }
                onSuccess()
            }
        } else {
            Log.e(tag, "Invalid company index: $index. Available indices: 0 to ${loginDetails.size - 1}")
        }

        Log.d(tag, "=== selectCompany END ===")
    }

    fun dismissCompanyDialog() {
        Log.d(tag, "Company selection dialog dismissed by user")
        _uiState.update { it.copy(showCompanySelectionDialog = false) }
    }

    fun clearError() {
        Log.d(tag, "Clearing error message")
        _uiState.update { it.copy(errorMessage = null) }
    }

    private suspend fun performLogin(mobile: String, deviceId: String): LoginResult {
        Log.d(tag, "=== performLogin START ===")
        Log.d(tag, "Mobile: $mobile")
        Log.d(tag, "DeviceId: $deviceId")
        Log.d(tag, "DeviceId length: ${deviceId.length}")
        withContext(Dispatchers.Main) {
            resetUIState()
        }
        var connection: Connection? = null
        var resultSet: ResultSet? = null

        return try {
            Log.d(tag, "Creating ConnectionStr instance")
            val conStr = ConnectionStr()
            Log.d(tag, "ConnectionStr instance created successfully")

            Log.d(tag, "Attempting to connect to main database...")
            Log.d(tag, "Connection params - User: $mainDbUser, DB: $mainDbName, IP: $mainDbIp")

            connection = conStr.connectionclasss(
                mainDbUser,
                mainDbPass,
                mainDbName,
                mainDbIp,
                null
            )

            if (connection == null) {
                Log.e(tag, "Failed to connect to main database - Connection is null")
                return LoginResult.Error("Database connection failed")
            }

            Log.d(tag, "Main database connection established successfully")
            Log.d(tag, "Connection hashcode: ${connection.hashCode()}")

            val query = """
                SELECT ISNULL(E_Role,'Standard') AS E_Role, 
                       K_SurveyAccuracy, 
                       K_SurveyAccuracyBrix, 
                       k_surveyArea, 
                       b.k_key, 
                       a.e_code, 
                       a.e_active, 
                       a.e_name, 
                       a.user_mobile, 
                       b.k_server, 
                       b.k_uid, 
                       b.k_pwd, 
                       b.k_database, 
                       b.k_dataInstance, 
                       b.k_ftpIp, 
                       b.k_ftppath, 
                       b.k_ftpUID, 
                       b.k_ftpPWD, 
                       b.k_site_code, 
                       b.k_company_name
                FROM vipl_imei a 
                INNER JOIN vipl_key b ON b.k_key = a.e_key 
                WHERE (a.E_IMEI3 = '$deviceId' OR a.E_IMEI4 = '$deviceId') 
                AND a.user_mobile = '$mobile'
                AND a.E_App = 'field'
            """.trimIndent()

            Log.d(tag, "Executing query on main database")
            Log.d(tag, "Query: $query")

            val statement = connection.createStatement()
            statement.setQueryTimeout(30000)
            Log.d(tag, "Statement created with 30s timeout")

            resultSet = statement.executeQuery(query)
            Log.d(tag, "Query executed successfully")

            val loginDetails = mutableListOf<LoginDetail>()
            var rowCount = 0

            while (resultSet.next()) {
                rowCount++
                Log.d(tag, "Processing row $rowCount")

                // ✅ FORCED TEST DB OVERRIDE
                val kDatabaseFromKey = resultSet.getString("k_database") ?: ""
                val effectiveDbName = if (USE_TEST_DB) TEST_DB_NAME else kDatabaseFromKey

                val loginDetail = LoginDetail(
                    role = resultSet.getString("E_Role") ?: "Standard",
                    kSurveyAccuracy = resultSet.getString("K_SurveyAccuracy") ?: "",
                    kSurveyAccuracyBrix = resultSet.getString("K_SurveyAccuracyBrix") ?: "",
                    kSurveyArea = resultSet.getString("k_surveyArea") ?: "",
                    companyCode = resultSet.getString("k_key") ?: "",
                    ecode = resultSet.getString("e_code") ?: "",
                    isActive = resultSet.getString("e_active")?.trim()?.equals("Yes", ignoreCase = true) ?: false,
                    username = resultSet.getString("e_name") ?: "",
                    mobile = resultSet.getString("user_mobile") ?: "",
                    dbIp = resultSet.getString("k_server") ?: "",
                    dbUser = resultSet.getString("k_uid") ?: "",
                    dbPass = resultSet.getString("k_pwd") ?: "",

                    // ✅ FORCED TEST DB — k_database from vipl_key is IGNORED when USE_TEST_DB=true
                    dbName = effectiveDbName,

                    instance = resultSet.getString("k_dataInstance"),
                    ftpIp = resultSet.getString("k_ftpIp") ?: "",
                    ftpPath = resultSet.getString("k_ftppath") ?: "",
                    ftpUser = resultSet.getString("k_ftpUID") ?: "",
                    ftpPass = resultSet.getString("k_ftpPWD") ?: "",
                    siteCode = resultSet.getString("k_site_code") ?: "",
                    companyName = resultSet.getString("k_company_name") ?: ""
                )

                loginDetails.add(loginDetail)

                Log.d(tag, "Row $rowCount - Company: ${loginDetail.companyName}")
                Log.d(tag, "  - Role: ${loginDetail.role}")
                Log.d(tag, "  - Active: ${loginDetail.isActive}")
                Log.d(tag, "  - DB Server: ${loginDetail.dbIp}")
                Log.d(tag, "  - DB Name from vipl_key: [$kDatabaseFromKey]")
                Log.d(tag, "  - DB Name (EFFECTIVE): [${loginDetail.dbName}]  ${if (USE_TEST_DB) "⚠ FORCED TEST DB" else "✅ live"}")
                Log.d(tag, "  - DB User: ${loginDetail.dbUser}")
                Log.d(tag, "  - Site Code: ${loginDetail.siteCode}")
            }

            Log.d(tag, "Query completed. Total rows found: $rowCount")

            if (loginDetails.isEmpty()) {
                Log.e(tag, "No matching records found for mobile: $mobile and deviceId: $deviceId")
                LoginResult.Error("Device not registered or invalid mobile number")
            } else {
                Log.d(tag, "Successfully found ${loginDetails.size} company(s)")
                LoginResult.Success(loginDetails)
            }

        } catch (e: SQLException) {
            Log.e(tag, "SQL Error in login: ${e.message}", e)
            Log.e(tag, "SQL State: ${e.sqlState}")
            Log.e(tag, "Error Code: ${e.errorCode}")
            LoginResult.Error("Database error: ${e.message}")
        } catch (e: Exception) {
            Log.e(tag, "Unexpected error in login: ${e.message}", e)
            Log.e(tag, "Exception type: ${e.javaClass.simpleName}")
            LoginResult.Error(e.message ?: "Login failed")
        } finally {
            try {
                resultSet?.close()
                Log.d(tag, "ResultSet closed")
                connection?.close()
                Log.d(tag, "Database connection closed")
            } catch (e: SQLException) {
                Log.e(tag, "Error closing database resources: ${e.message}", e)
            }
            Log.d(tag, "=== performLogin END ===")
        }
    }


    fun resetUIState() {
        Log.d(tag, "=== resetUIState START ===")
        _uiState.update {
            LoginUiState(
                mobileNumber = "",
                mobileError = null,
                isLoading = false,
                errorMessage = null,
                showCompanySelectionDialog = false,
                companyList = emptyList(),
                loginDetails = emptyList()
            )
        }
        Log.d(tag, "UI State reset to initial values")
        Log.d(tag, "=== resetUIState END ===")
    }

    fun clearFormAndReset() {
        Log.d(tag, "=== clearFormAndReset START ===")
        _uiState.update {
            LoginUiState(
                mobileNumber = "",
                mobileError = null,
                isLoading = false,
                errorMessage = null,
                showCompanySelectionDialog = false,
                companyList = emptyList(),
                loginDetails = emptyList()
            )
        }
        Log.d(tag, "Form cleared and reset to initial state")
        Log.d(tag, "=== clearFormAndReset END ===")
    }

    fun logout(context: Context) {
        Log.d(tag, "=== logout START ===")

        val constant = MainConstant(context)
        constant.logout()

        deviceId = null

        resetUIState()

        Log.d(tag, "User logged out, all data cleared and UI reset")
        Log.d(tag, "=== logout END ===")
    }

    private fun proceedWithLogin(context: Context, loginDetail: LoginDetail, onSuccess: () -> Unit) {
        Log.d(tag, "=== proceedWithLogin START ===")
        Log.d(tag, "Proceeding with login for company: ${loginDetail.companyName}")
        Log.d(tag, "User: ${loginDetail.username}, Mobile: ${loginDetail.mobile}")

        viewModelScope.launch {
            Log.d(tag, "Launching verification coroutine")
            _uiState.update { it.copy(isLoading = true) }

            val result = withContext(Dispatchers.IO) {
                Log.d(tag, "Executing verification on IO thread")
                verifyAndProceedLogin(context, loginDetail)
            }

            _uiState.update { it.copy(isLoading = false) }
            Log.d(tag, "Verification completed with result: $result")

            if (result) {
                Log.d(tag, "Verification successful, calling onSuccess callback")
                onSuccess()
            } else {
                Log.e(tag, "Verification failed")
            }

            Log.d(tag, "=== proceedWithLogin END ===")
        }
    }

    private suspend fun verifyAndProceedLogin(context: Context, loginDetail: LoginDetail): Boolean {
        Log.d(tag, "=== verifyAndProceedLogin START ===")
        Log.d(tag, "Verifying login for company: ${loginDetail.companyName}")
        Log.d(tag, "Company details:")
        Log.d(tag, "  - Company Code: ${loginDetail.companyCode}")
        Log.d(tag, "  - Site Code: ${loginDetail.siteCode}")
        Log.d(tag, "  - DB IP: ${loginDetail.dbIp}")
        Log.d(tag, "  - DB Name: ${loginDetail.dbName}")
        Log.d(tag, "  - DB User: ${loginDetail.dbUser}")
        Log.d(tag, "  - Instance: ${loginDetail.instance}")
        Log.d(tag, "  - FTP Server: ${loginDetail.ftpIp}")
        Log.d(tag, "  - FTP Path: ${loginDetail.ftpPath}")

        var connection: Connection? = null
        var resultSet: ResultSet? = null
        var statement: Statement? = null
        var configResultSet: ResultSet? = null

        return try {
            Log.d(tag, "Checking device activation status")
            if (!loginDetail.isActive) {
                Log.e(tag, "Device is not active - Activation required")
                withContext(Dispatchers.Main) {
                    _uiState.update {
                        it.copy(errorMessage = "Your device is not activated. Please contact admin.")
                    }
                }
                return false
            }
            Log.d(tag, "Device is active")

            Log.d(tag, "Attempting to connect to company database...")
            Log.d(tag, "  → Using DB: ${loginDetail.dbName}")
            Log.d(tag, "  → Using IP: ${loginDetail.dbIp}")
            Log.d(tag, "  → Using User: ${loginDetail.dbUser}")

            val conStr = ConnectionStr()
            Log.d(tag, "ConnectionStr instance created")

            connection = conStr.connectionclasss(
                loginDetail.dbUser,
                loginDetail.dbPass,
                loginDetail.dbName,             // ✅ live DB (from k_database) OR forced test DB
                loginDetail.dbIp,
                loginDetail.instance
            )

            if (connection == null) {
                Log.e(tag, "Failed to connect to company database - Connection is null")
                withContext(Dispatchers.Main) {
                    _uiState.update {
                        it.copy(errorMessage = "Failed to connect to company database")
                    }
                }
                return false
            }

            Log.d(tag, "Company database connection established successfully")
            Log.d(tag, "Connection hashcode: ${connection.hashCode()}")

            // ===== FETCH SYSTEM CONFIGURATION =====
            Log.d(tag, "=== Fetching System Configuration from VIPL_Sys1 ===")
            var imageApiUrl = ""
            var imageDownloadApiUrl = ""

            try {
                val configQuery = """
                SELECT   
                    ISNULL(C_IMAGE_API, '') AS C_IMAGE_API,
                    ISNULL(C_IMG_DOW_API, '') AS C_IMG_DOW_API 
                FROM VIPL_Sys1
            """.trimIndent()

                Log.d(tag, "Executing system config query: $configQuery")

                statement = connection.createStatement()
                statement.queryTimeout = 5000
                configResultSet = statement.executeQuery(configQuery)

                if (configResultSet.next()) {
                    imageApiUrl = configResultSet.getString("C_IMAGE_API")?.trim() ?: ""
                    imageDownloadApiUrl = configResultSet.getString("C_IMG_DOW_API")?.trim() ?: ""

                    Log.d(tag, "System Configuration Retrieved:")
                    Log.d(tag, "  - C_IMAGE_API: $imageApiUrl")
                    Log.d(tag, "  - C_IMG_DOW_API: $imageDownloadApiUrl")
                } else {
                    Log.w(tag, "No record found in VIPL_Sys1 table, using default values")
                }
            } catch (e: SQLException) {
                Log.e(tag, "SQL Error fetching system config: ${e.message}", e)
            } catch (e: Exception) {
                Log.e(tag, "Unexpected error fetching system config: ${e.message}", e)
            }

            try {
                configResultSet?.close()
                statement?.close()
            } catch (e: SQLException) {
                Log.w(tag, "Error closing config query resources: ${e.message}")
            }

            // ===== VERIFY ACCOUNT =====
            val accountQuery = """
            SELECT account_id, account_name 
            FROM Vipl_Accounts_master 
            WHERE G_FactoryCode = '${loginDetail.siteCode}' 
            AND G_MobileNo = '${loginDetail.mobile}'
            AND G_enable = 1
        """.trimIndent()

            Log.d(tag, "Executing account verification query")
            Log.d(tag, "Query: $accountQuery")

            val accountStatement = connection.createStatement()
            accountStatement.queryTimeout = 5000
            Log.d(tag, "Statement created with 5s timeout")

            val accountResultSet = accountStatement.executeQuery(accountQuery)

            var accountVerified = false
            var accountName = ""
            var accountId = ""

            if (accountResultSet.next()) {
                accountVerified = true
                accountId = accountResultSet.getString("account_id") ?: ""
                accountName = accountResultSet.getString("account_name") ?: ""
                Log.d(tag, "Account verified successfully")
                Log.d(tag, "  - Account ID: $accountId")
                Log.d(tag, "  - Account Name: $accountName")
            } else {
                Log.e(tag, "Account not found for mobile: ${loginDetail.mobile} and site code: ${loginDetail.siteCode}")
            }

            try {
                accountResultSet?.close()
                accountStatement?.close()
            } catch (e: SQLException) {
                Log.w(tag, "Error closing account verification resources: ${e.message}")
            }

            if (!accountVerified) {
                Log.e(tag, "Account verification failed")
                withContext(Dispatchers.Main) {
                    _uiState.update {
                        it.copy(errorMessage = "Account not found or inactive")
                    }
                }
                return false
            }

            Log.d(tag, "Saving login details and system configuration to SharedPreferences")
            val constant = MainConstant(context)

            constant.saveAllConfigs(
                seasonName = constant.getSeasonName(),
                seasonCode = constant.getSeasonCode(),
                locMin = constant.getLocMin(),
                imageApiUrl = imageApiUrl,
                imageDownloadApiUrl = imageDownloadApiUrl
            )
            Log.d(tag, "System configuration saved successfully")

            val surveyArea = loginDetail.kSurveyArea.toIntOrNull() ?: 0
            val surveyAccuracy = loginDetail.kSurveyAccuracy.toFloatOrNull() ?: 10.0f
            val surveyAccuracyBrix = loginDetail.kSurveyAccuracyBrix.toFloatOrNull() ?: 10.0f

            Log.d(tag, "Converted values:")
            Log.d(tag, "  - Survey Area: $surveyArea (from: ${loginDetail.kSurveyArea})")
            Log.d(tag, "  - Survey Accuracy: $surveyAccuracy (from: ${loginDetail.kSurveyAccuracy})")
            Log.d(tag, "  - Survey Accuracy Brix: $surveyAccuracyBrix (from: ${loginDetail.kSurveyAccuracyBrix})")

            constant.saveLogin(
                username = loginDetail.username,
                userCode = accountId,
                mobile = loginDetail.mobile,
                eCode = loginDetail.ecode,
                ip = loginDetail.dbIp,
                dbName = loginDetail.dbName,     // ✅ effective DB (test or live)
                dbUser = loginDetail.dbUser,
                dbPass = loginDetail.dbPass,
                instance = loginDetail.instance ?: "",
                ftpPath = loginDetail.ftpPath,
                ftpUser = loginDetail.ftpUser,
                ftpServer = loginDetail.ftpIp,
                ftpPass = loginDetail.ftpPass,
                siteCode = loginDetail.siteCode,
                companyName = loginDetail.companyName,
                surveyArea = surveyArea,
                surveyAccuracy = surveyAccuracy,
                surveyAccuracyBrix = surveyAccuracyBrix,
                deviceId = deviceId ?: ""
            )

            constant.setRole(loginDetail.role)
            constant.setLoggedIn(true)

            Log.d(tag, "All data saved successfully")
            Log.d(tag, "  - C_IMAGE_API: ${constant.getImageApiUrl()}")
            Log.d(tag, "  - Username: ${constant.getUsername()}")
            Log.d(tag, "  - User Code: ${constant.getUserCode()}")
            Log.d(tag, "  - Site Code: ${constant.getSiteCode()}")
            Log.d(tag, "  - Company: ${constant.getCompanyName()}")
            Log.d(tag, "  - DeviceId: ${constant.getDeviceId()}")
            Log.d(tag, "  - DB Name saved: ${constant.getDbName()}")
            Log.d(tag, "Login successful for user: ${loginDetail.username}")
            Log.d(tag, "=== verifyAndProceedLogin END ===")
            true

        } catch (e: SQLException) {
            Log.e(tag, "SQL Error in verification: ${e.message}", e)
            Log.e(tag, "SQL State: ${e.sqlState}")
            Log.e(tag, "Error Code: ${e.errorCode}")
            withContext(Dispatchers.Main) {
                _uiState.update { it.copy(errorMessage = "Verification failed: ${e.message}") }
            }
            false
        } catch (e: Exception) {
            Log.e(tag, "Unexpected error in verification: ${e.message}", e)
            Log.e(tag, "Exception type: ${e.javaClass.simpleName}")
            withContext(Dispatchers.Main) {
                _uiState.update { it.copy(errorMessage = e.message ?: "Verification failed") }
            }
            false
        } finally {
            try {
                configResultSet?.close()
                statement?.close()
                resultSet?.close()
                connection?.close()
                Log.d(tag, "All database resources closed successfully")
            } catch (e: SQLException) {
                Log.e(tag, "Error closing resources: ${e.message}", e)
            }
        }
    }

    private fun bytesToHex(bytes: ByteArray): String {
        Log.d(tag, "Converting byte array to hex string. Byte count: ${bytes.size}")
        val hexChars = CharArray(bytes.size * 2)
        for (i in bytes.indices) {
            val v = bytes[i].toInt() and 0xFF
            hexChars[i * 2] = HEX_ARRAY[v ushr 4]
            hexChars[i * 2 + 1] = HEX_ARRAY[v and 0x0F]
        }
        val result = String(hexChars)
        Log.d(tag, "Hex conversion completed. Result length: ${result.length}")
        return result
    }

    fun isDeviceIdAvailable(): Boolean {
        val available = deviceId != null
        Log.d(tag, "isDeviceIdAvailable: $available")
        return available
    }

    companion object {
        private val HEX_ARRAY = "0123456789ABCDEF".toCharArray()
    }
}

sealed class LoginResult {
    data class Success(val loginDetails: List<LoginDetail>) : LoginResult()
    data class Error(val message: String) : LoginResult()
}

