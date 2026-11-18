package com.visibleinfotech.e_ethanolUnloading.login

import android.content.Context
import android.media.MediaDrm
import android.os.Build
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import java.util.UUID

data class RegistrationUiState(
    val companyCode: String = "",
    val companyCodeError: String? = null,
    val companyName: String = "",
    val userName: String = "",
    val mobileNumber: String = "",
    val mobileError: String? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val showSuccessDialog: Boolean = false,
    val isVerifyingCompany: Boolean = false,
    val isVerifyingMobile: Boolean = false,
    val isServerActive: Boolean = false,  // Add this
    val isCheckingServer: Boolean = false  // Add this
)

class RegistrationViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(RegistrationUiState())
    val uiState: StateFlow<RegistrationUiState> = _uiState.asStateFlow()

    private var deviceId: String? = null
    private val tag = "RegistrationViewModel"

    private val mainDbIp = "SG2NWPLS19SQL-v08.mssql.shr.prod.sin2.secureserver.net"
    private val mainDbName = "vipl"
    private val mainDbUser = "Saini"
    private val mainDbPass = "vipl12345"

    private var verifiedDbIp: String? = null
    private var verifiedDbUser: String? = null
    private var verifiedDbPass: String? = null
    private var verifiedDbName: String? = null
    private var verifiedInstance: String? = null
    private var verifiedSiteCode: String? = null

    fun updateCompanyCode(code: String) {
        Log.d(tag, "updateCompanyCode called with: $code")

        val filteredCode = code.filter { it.isLetterOrDigit() }.take(15)
        Log.d(tag, "Filtered company code: $filteredCode, Length: ${filteredCode.length}")

        _uiState.update {
            it.copy(
                companyCode = filteredCode,
                companyCodeError = if (filteredCode.isNotEmpty() && filteredCode.length != 15)
                    "Company code must be 15 alphanumeric characters" else null
            )
        }

        if (filteredCode.length == 15) {
            Log.d(tag, "Company code reached 15 chars, triggering verification")
            verifyCompanyCode(filteredCode)
        } else {
            Log.d(tag, "Company code not complete yet, clearing company name")
            _uiState.update { it.copy(companyName = "") }
        }
    }

    fun updateMobileNumber(mobile: String) {
        Log.d(tag, "updateMobileNumber called with: $mobile")

        val filteredMobile = mobile.filter { it.isDigit() }.take(10)
        Log.d(tag, "Filtered mobile: $filteredMobile, Length: ${filteredMobile.length}")

        _uiState.update {
            it.copy(
                mobileNumber = filteredMobile,
                mobileError = if (filteredMobile.isNotEmpty() && filteredMobile.length != 10)
                    "Mobile number must be 10 digits" else null
            )
        }

        if (filteredMobile.length == 10 && _uiState.value.companyName.isNotEmpty()) {
            Log.d(tag, "Mobile number complete and company verified, triggering mobile verification")
            verifyMobileNumber(filteredMobile)
        } else if (filteredMobile.length != 10) {
            Log.d(tag, "Mobile number not complete, clearing username")
            _uiState.update { it.copy(userName = "") }
        }
    }

    fun clearError() {
        Log.d(tag, "clearError called")
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun dismissSuccessDialog() {
        Log.d(tag, "dismissSuccessDialog called")
        _uiState.update { it.copy(showSuccessDialog = false) }
    }

    private fun getDeviceId(context: Context): String? {
        Log.d(tag, "getDeviceId called")

        if (deviceId != null) {
            Log.d(tag, "Device ID already cached: $deviceId")
            return deviceId
        }

        return try {
            Log.d(tag, "Getting WideVine device ID")
            val wideVineUuid = UUID(-0x121074568629b532L, -0x5c37d8232ae2de13L)
            val wvDrm = MediaDrm(wideVineUuid)
            val wideVineId = wvDrm.getPropertyByteArray(MediaDrm.PROPERTY_DEVICE_UNIQUE_ID)
            deviceId = bytesToHex(wideVineId)
            Log.d(tag, "Device ID retrieved successfully: $deviceId")
            deviceId
        } catch (e: Exception) {
            Log.e(tag, "Failed to get device ID: ${e.message}", e)
            null
        }
    }

    private suspend fun checkServerConnection(
        dbIp: String,
        dbUser: String,
        dbPass: String,
        dbName: String,
        instance: String?
        // Remove context parameter
    ): Boolean {
        Log.d(tag, "=== checkServerConnection START ===")
        var connection: Connection? = null

        return try {
            Log.d(tag, "Testing connection to company database server")
            val conStr = ConnectionStr()
            connection = conStr.connectionclasss(
                dbUser,
                dbPass,
                dbName,
                dbIp,
                instance
            )

            val isConnected = connection != null
            if (isConnected) {
                Log.d(tag, "Server connection SUCCESSFUL")
                // Test if we can execute a simple query
                try {
                    val statement = connection.createStatement()
                    statement.setQueryTimeout(5000)
                    val resultSet = statement.executeQuery("SELECT 1")
                    resultSet.close()
                    Log.d(tag, "Server query test SUCCESSFUL")
                } catch (e: Exception) {
                    Log.e(tag, "Server query test FAILED: ${e.message}")
                    return false
                }
            } else {
                Log.e(tag, "Server connection FAILED - Connection is null")
            }
            isConnected
        } catch (e: Exception) {
            Log.e(tag, "Error checking server connection: ${e.message}", e)
            false
        } finally {
            try {
                connection?.close()
                Log.d(tag, "Test connection closed")
            } catch (e: SQLException) {
                Log.e(tag, "Error closing test connection: ${e.message}")
            }
            Log.d(tag, "=== checkServerConnection END ===")
        }
    }

    // Update verifyCompanyCode function
    private fun verifyCompanyCode(companyCode: String) {
        Log.d(tag, "verifyCompanyCode called for: $companyCode")

        viewModelScope.launch {
            Log.d(tag, "Starting company verification coroutine")
            _uiState.update {
                it.copy(
                    isVerifyingCompany = true,
                    companyCodeError = null,
                    isServerActive = false,  // Reset server status
                    isCheckingServer = false
                )
            }

            val result = withContext(Dispatchers.IO) {
                Log.d(tag, "Executing company verification on IO thread")
                executeCompanyVerification(companyCode)
            }

            if (result != null) {
                Log.d(tag, "Company verification SUCCESSFUL: ${result.companyName}")
                verifiedDbIp = result.dbIp
                verifiedDbUser = result.dbUser
                verifiedDbPass = result.dbPass
                verifiedDbName = result.dbName
                verifiedInstance = result.instance
                verifiedSiteCode = result.siteCode

                Log.d(tag, "Company DB details - IP: $verifiedDbIp, DB: $verifiedDbName, User: $verifiedDbUser, SiteCode: $verifiedSiteCode")

                // Check server connection
                _uiState.update { it.copy(isCheckingServer = true) }

                val isServerActive = withContext(Dispatchers.IO) {
                    checkServerConnection(
                        verifiedDbIp!!,
                        verifiedDbUser!!,
                        verifiedDbPass!!,
                        verifiedDbName!!,
                        verifiedInstance
                    )
                }

                Log.d(tag, "Server active status: $isServerActive")

                _uiState.update {
                    it.copy(
                        companyName = result.companyName,
                        isVerifyingCompany = false,
                        isServerActive = isServerActive,
                        isCheckingServer = false
                    )
                }
            } else {
                Log.e(tag, "Company verification FAILED for code: $companyCode")
                _uiState.update {
                    it.copy(
                        companyName = "",
                        companyCodeError = "Invalid or inactive company code",
                        isVerifyingCompany = false,
                        isServerActive = false,
                        isCheckingServer = false
                    )
                }
            }
        }
    }
    private fun verifyMobileNumber(mobileNumber: String) {
        Log.d(tag, "verifyMobileNumber called for: $mobileNumber")

        viewModelScope.launch {
            Log.d(tag, "Starting mobile verification coroutine")
            _uiState.update { it.copy(isVerifyingMobile = true, mobileError = null) }

            val userName = withContext(Dispatchers.IO) {
                Log.d(tag, "Executing mobile verification on IO thread")
                executeMobileVerification(mobileNumber)
            }

            if (!userName.isNullOrEmpty()) {
                Log.d(tag, "Mobile verification SUCCESSFUL. User name: $userName")
                _uiState.update {
                    it.copy(
                        userName = userName,
                        isVerifyingMobile = false,
                        mobileError = null
                    )
                }
            } else {
                Log.e(tag, "Mobile verification FAILED for number: $mobileNumber")
                _uiState.update {
                    it.copy(
                        userName = "",
                        mobileError = "Mobile number not found in database",
                        isVerifyingMobile = false
                    )
                }
            }
        }
    }

    private fun executeCompanyVerification(companyCode: String): CompanyVerificationResult? {
        Log.d(tag, "=== executeCompanyVerification START ===")
        Log.d(tag, "Verifying company code: $companyCode")

        var connection: Connection? = null
        var resultSet: ResultSet? = null

        try {
            Log.d(tag, "Creating ConnectionStr instance")
            val conStr = ConnectionStr()

            Log.d(tag, "Connecting to main database - IP: $mainDbIp, DB: $mainDbName, User: $mainDbUser")
            connection = conStr.connectionclasss(
                mainDbUser,
                mainDbPass,
                mainDbName,
                mainDbIp,
                null
            )

            if (connection == null) {
                Log.e(tag, "Failed to connect to main database - Connection is null")
                return null
            }

            Log.d(tag, "Main database connection established successfully")

            val query = """
                SELECT k_server, k_uid, k_pwd, k_database, k_dataInstance,
                       k_company_name, K_site_code
                FROM vipl_key 
                WHERE k_key = '$companyCode' AND k_active = 'Yes'
            """.trimIndent()

            Log.d(tag, "Executing query: $query")

            val statement = connection.createStatement()
            resultSet = statement.executeQuery(query)

            if (resultSet.next()) {
                val dbIp = resultSet.getString("k_server")
                val dbUser = resultSet.getString("k_uid")
                val dbPass = resultSet.getString("k_pwd")
                val dbName = resultSet.getString("k_database")
                val instance = resultSet.getString("k_dataInstance")
                val companyName = resultSet.getString("k_company_name")
                val siteCode = resultSet.getString("K_site_code")

                Log.d(tag, "Company verification SUCCESS - Details:")
                Log.d(tag, "  Company Name: $companyName")
                Log.d(tag, "  DB Server: $dbIp")
                Log.d(tag, "  DB Name: $dbName")
                Log.d(tag, "  DB User: $dbUser")
                Log.d(tag, "  Instance: $instance")
                Log.d(tag, "  Site Code: $siteCode")

                return CompanyVerificationResult(
                    dbIp = dbIp,
                    dbUser = dbUser,
                    dbPass = dbPass,
                    dbName = dbName,
                    instance = instance,
                    companyName = companyName,
                    siteCode = siteCode
                )
            } else {
                Log.e(tag, "No matching active company found for code: $companyCode")
                return null
            }

        } catch (e: SQLException) {
            Log.e(tag, "SQL Error in company verification: ${e.message}", e)
            return null
        } catch (e: Exception) {
            Log.e(tag, "Error in company verification: ${e.message}", e)
            return null
        } finally {
            try {
                resultSet?.close()
                connection?.close()
                Log.d(tag, "Database resources closed")
            } catch (e: SQLException) {
                Log.e(tag, "Error closing resources: ${e.message}")
            }
            Log.d(tag, "=== executeCompanyVerification END ===")
        }
    }

    private fun executeMobileVerification(mobileNumber: String): String? {
        Log.d(tag, "=== executeMobileVerification START ===")
        Log.d(tag, "Verifying mobile number: $mobileNumber")

        var connection: Connection? = null
        var resultSet: ResultSet? = null

        try {
            if (verifiedDbIp == null || verifiedDbUser == null || verifiedDbPass == null || verifiedDbName == null) {
                Log.e(tag, "Company database connection details not available")
                Log.e(tag, "verifiedDbIp: $verifiedDbIp, verifiedDbUser: $verifiedDbUser, verifiedDbName: $verifiedDbName")
                return null
            }

            Log.d(tag, "Company DB details available:")
            Log.d(tag, "  Server: $verifiedDbIp")
            Log.d(tag, "  Database: $verifiedDbName")
            Log.d(tag, "  User: $verifiedDbUser")
            Log.d(tag, "  Instance: $verifiedInstance")

            val conStr = ConnectionStr()

            Log.d(tag, "Connecting to company database...")
            connection = conStr.connectionclasss(
                verifiedDbUser!!,
                verifiedDbPass!!,
                verifiedDbName!!,
                verifiedDbIp!!,
                verifiedInstance
            )

            if (connection == null) {
                Log.e(tag, "Failed to connect to company database - Connection is null")
                return null
            }

            Log.d(tag, "Company database connection established successfully")

            val query = """
                SELECT account_id, account_name 
                FROM Vipl_Accounts_master 
                WHERE G_enable = 1 AND G_MobileNo = '$mobileNumber'
            """.trimIndent()

            Log.d(tag, "Executing query: $query")

            val statement = connection.createStatement()
            statement.setQueryTimeout(5000)
            resultSet = statement.executeQuery(query)

            if (resultSet.next()) {
                val accountId = resultSet.getString("account_id")
                val userName = resultSet.getString("account_name")
                Log.d(tag, "Mobile verification SUCCESS - Account ID: $accountId, User Name: $userName")
                return userName
            } else {
                Log.e(tag, "No account found for mobile number: $mobileNumber")
                return null
            }

        } catch (e: SQLException) {
            Log.e(tag, "SQL Error in mobile verification: ${e.message}", e)
            return null
        } catch (e: Exception) {
            Log.e(tag, "Error in mobile verification: ${e.message}", e)
            return null
        } finally {
            try {
                resultSet?.close()
                connection?.close()
                Log.d(tag, "Database resources closed")
            } catch (e: SQLException) {
                Log.e(tag, "Error closing resources: ${e.message}")
            }
            Log.d(tag, "=== executeMobileVerification END ===")
        }
    }

    fun registerDevice(context: Context, onSuccess: () -> Unit) {
        Log.d(tag, "=== registerDevice START ===")
        val state = _uiState.value

        Log.d(tag, "Current state - CompanyCode: ${state.companyCode}, CompanyName: ${state.companyName}")
        Log.d(tag, "Current state - MobileNumber: ${state.mobileNumber}, UserName: ${state.userName}")

        if (state.companyCode.isEmpty()) {
            Log.e(tag, "Validation failed: Company code empty")
            _uiState.update { it.copy(companyCodeError = "Please enter company code") }
            return
        }

        if (state.companyCode.length != 15) {
            Log.e(tag, "Validation failed: Company code length ${state.companyCode.length} != 15")
            _uiState.update { it.copy(companyCodeError = "Company code must be 15 alphanumeric characters") }
            return
        }

        if (state.companyName.isEmpty()) {
            Log.e(tag, "Validation failed: Company name empty")
            _uiState.update { it.copy(companyCodeError = "Invalid company code") }
            return
        }

        if (state.mobileNumber.isEmpty() || state.mobileNumber.length != 10) {
            Log.e(tag, "Validation failed: Mobile number invalid - ${state.mobileNumber}")
            _uiState.update { it.copy(mobileError = "Please enter valid 10-digit mobile number") }
            return
        }

        if (state.userName.isEmpty()) {
            Log.e(tag, "Validation failed: User name empty")
            _uiState.update { it.copy(errorMessage = "Mobile number not verified. Please check your mobile number.") }
            return
        }

        val deviceId = getDeviceId(context)
        if (deviceId == null) {
            Log.e(tag, "Validation failed: Unable to get device ID")
            _uiState.update { it.copy(errorMessage = "Unable to get device ID") }
            return
        }

        Log.d(tag, "All validations passed. Device ID: $deviceId")
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            Log.d(tag, "Starting registration coroutine")
            val result = withContext(Dispatchers.IO) {
                Log.d(tag, "Executing registration on IO thread")
                performRegistration(
                    deviceId,
                    state.companyCode,
                    state.userName,
                    state.mobileNumber,
                    state.companyName,
                    context
                )
            }

            _uiState.update { it.copy(isLoading = false) }

            if (result) {
                Log.d(tag, "Registration SUCCESSFUL")
                // Show success dialog instead of toast
                _uiState.update { it.copy(showSuccessDialog = true) }
            } else {
                Log.e(tag, "Registration FAILED")
                // Error message is already set in performRegistration
            }
            Log.d(tag, "=== registerDevice END ===")
        }
    }

    private suspend fun performRegistration(
        deviceId: String,
        companyCode: String,
        userName: String,
        mobileNumber: String,
        companyName: String,
        context: Context  // Add context parameter
    ): Boolean {
        Log.d(tag, "=== performRegistration START ===")
        Log.d(tag, "Registration params:")
        Log.d(tag, "  Device ID: $deviceId")
        Log.d(tag, "  Company Code: $companyCode")
        Log.d(tag, "  User Name: $userName")
        Log.d(tag, "  Mobile Number: $mobileNumber")
        Log.d(tag, "  Company Name: $companyName")

        var connection: Connection? = null
        var resultSet: ResultSet? = null

        try {
            Log.d(tag, "Creating ConnectionStr instance")
            val conStr = ConnectionStr()

            Log.d(tag, "Connecting to main database for registration")
            connection = conStr.connectionclasss(
                mainDbUser,
                mainDbPass,
                mainDbName,
                mainDbIp,
                null
            )

            if (connection == null) {
                Log.e(tag, "Failed to connect to main database for registration")
                withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(
                        context,
                        "Database connection failed",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
                return false
            }

            Log.d(tag, "Main database connection established")

            val androidVersion = Build.VERSION.RELEASE
            val phoneModel = Build.MODEL
            val phoneBrand = Build.BRAND
            val deviceDetail = "Android Version: $androidVersion, Brand: $phoneBrand, Model: $phoneModel"

            Log.d(tag, "Device details: $deviceDetail")

            val checkQuery = """
            SELECT * FROM VIPL_IMEI 
            WHERE (E_IMEI3 = '$deviceId' OR E_IMEI4 = '$deviceId') 
            AND E_App = 'field' 
            AND E_KEY = '$companyCode'
        """.trimIndent()

            Log.d(tag, "Checking if device already registered: $checkQuery")

            val statement = connection.createStatement()
            resultSet = statement.executeQuery(checkQuery)

            var alreadyRegistered = false
            var isActive = false
            var existingUserName = ""

            if (resultSet.next()) {
                alreadyRegistered = true
                val activeStatus = resultSet.getString("E_Active")
                isActive = activeStatus.trim().equals("Yes", ignoreCase = true)
                existingUserName = resultSet.getString("E_name") ?: ""
                Log.d(tag, "Device already registered - Active Status: $isActive, User: $existingUserName")
            } else {
                Log.d(tag, "Device not registered previously")
            }

            // If already registered, show appropriate message and return false
            if (alreadyRegistered) {
                Log.w(tag, "Device already registered")


                _uiState.update {
                    it.copy(
                        errorMessage = if (isActive)
                            "Your device is already registered and Active"
                        else
                            "Device Already Registered. Contact Admin for Active."
                    )
                }
                return false
            }

            val mobileCheckQuery = """
            SELECT E_Role, E_name FROM vipl_imei 
            WHERE user_mobile = '$mobileNumber' 
            AND E_App = 'field' 
            AND E_KEY = '$companyCode'
        """.trimIndent()

            Log.d(tag, "Checking if mobile already used: $mobileCheckQuery")
            resultSet = statement.executeQuery(mobileCheckQuery)
            var mobileUsed = false
            var existingMobileUser = ""
            if (resultSet.next()) {
                mobileUsed = true
                existingMobileUser = resultSet.getString("E_name") ?: ""
                Log.d(tag, "Mobile already used by: $existingMobileUser")
            }
            Log.d(tag, "Mobile used before: $mobileUsed")

            if (mobileUsed) {
                Log.w(tag, "Mobile number already used")
                withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(
                        context,
                        "Mobile number $mobileNumber is already registered with user: $existingMobileUser",
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                }

                _uiState.update {
                    it.copy(
                        mobileError = "Mobile number is already registered with another device",
                        errorMessage = "Mobile number is already registered with user: $existingMobileUser"
                    )
                }
                return false
            }

            val licenseQuery = """
            SELECT ISNULL(k_applicence, 0) AS k_applicence 
            FROM vipl_key 
            WHERE K_key = '$companyCode'
        """.trimIndent()

            Log.d(tag, "Checking license availability: $licenseQuery")
            resultSet = statement.executeQuery(licenseQuery)
            var kApplicence = 0
            if (resultSet.next()) {
                kApplicence = resultSet.getInt("k_applicence")
                Log.d(tag, "Total licenses available: $kApplicence")
            }

            val countQuery = """
            SELECT COUNT(*) AS count 
            FROM vipl_imei 
            WHERE e_key = '$companyCode' AND E_active = 'Yes'
        """.trimIndent()

            Log.d(tag, "Counting active devices: $countQuery")
            resultSet = statement.executeQuery(countQuery)
            var count = 0
            if (resultSet.next()) {
                count = resultSet.getInt("count")
                Log.d(tag, "Active devices count: $count")
            }

            val licenseOver = (kApplicence == 0 || kApplicence <= count)
            Log.d(tag, "License over: $licenseOver (kApplicence=$kApplicence, count=$count)")

            if (licenseOver) {
                Log.w(tag, "License limit reached")
                withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(
                        context,
                        "License limit reached for this company. Max licenses: $kApplicence",
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                }

                _uiState.update {
                    it.copy(errorMessage = "License limit reached for this company")
                }
                return false
            }

            // All checks passed, proceed with registration
            Log.d(tag, "All checks passed. Proceeding with registration")

            val insertQuery = """
            INSERT INTO VIPL_IMEI(
                E_Date, E_IMEI3, E_IMEI4, E_name, E_ACTIVE, E_APP, 
                e_key, user_mobile, E_PlantName, E_plantCode, E_MakeModelVer
            ) VALUES(
                getDate(), 
                '$deviceId', 
                '$deviceId', 
                '$userName', 
                'No', 
                'field', 
                '$companyCode', 
                '$mobileNumber', 
                '$companyName', 
                '${verifiedSiteCode ?: companyCode}', 
                '$deviceDetail'
            )
        """.trimIndent()

            Log.d(tag, "Executing insert query: $insertQuery")
            val rowsAffected = statement.executeUpdate(insertQuery)

            if (rowsAffected > 0) {
                Log.d(tag, "Registration INSERT successful. Rows affected: $rowsAffected")
                return true
            } else {
                Log.e(tag, "Registration INSERT failed. Rows affected: 0")
                withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(
                        context,
                        "Registration failed. Please try again.",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
                return false
            }

        } catch (e: SQLException) {
            Log.e(tag, "SQL Error in registration: ${e.message}", e)
            withContext(Dispatchers.Main) {
                android.widget.Toast.makeText(
                    context,
                    "Database error: ${e.message}",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
            return false
        } catch (e: Exception) {
            Log.e(tag, "Error in registration: ${e.message}", e)
            withContext(Dispatchers.Main) {
                android.widget.Toast.makeText(
                    context,
                    "Error: ${e.message}",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
            return false
        } finally {
            try {
                resultSet?.close()
                connection?.close()
                Log.d(tag, "Database resources closed")
            } catch (e: SQLException) {
                Log.e(tag, "Error closing resources: ${e.message}")
            }
            Log.d(tag, "=== performRegistration END ===")
        }
    }

    private fun bytesToHex(bytes: ByteArray): String {
        val hexChars = CharArray(bytes.size * 2)
        for (i in bytes.indices) {
            val v = bytes[i].toInt() and 0xFF
            hexChars[i * 2] = HEX_ARRAY[v ushr 4]
            hexChars[i * 2 + 1] = HEX_ARRAY[v and 0x0F]
        }
        return String(hexChars)
    }
    fun resetForm() {
        Log.d(tag, "resetForm called - Clearing all registration data")
        _uiState.update {
            RegistrationUiState(
                companyCode = "",
                companyCodeError = null,
                companyName = "",
                userName = "",
                mobileNumber = "",
                mobileError = null,
                isLoading = false,
                errorMessage = null,
                showSuccessDialog = false,
                isVerifyingCompany = false,
                isVerifyingMobile = false
            )
        }
        // Clear verified company details
        verifiedDbIp = null
        verifiedDbUser = null
        verifiedDbPass = null
        verifiedDbName = null
        verifiedInstance = null
        verifiedSiteCode = null
        deviceId = null
    }
    companion object {
        private val HEX_ARRAY = "0123456789ABCDEF".toCharArray()
    }
}

data class CompanyVerificationResult(
    val dbIp: String,
    val dbUser: String,
    val dbPass: String,
    val dbName: String,
    val instance: String?,
    val companyName: String,
    val siteCode: String
)