package com.visibleinfotech.e_ethanolUnloading

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class UserSession(
    val username: String = "",
    val userCode: String = "",
    val mobile: String = "",
    val eCode: String = "",
    val ip: String = "",
    val dbName: String = "",
    val dbUser: String = "",
    val dbPass: String = "",
    val instance: String = "",
    val ftpPath: String = "",
    val ftpUser: String = "",
    val ftpServer: String = "",
    val ftpPass: String = "",
    val siteCode: String = "",
    val companyName: String = "",
    val surveyArea: Int = 0,
    val surveyAccuracy: Float = 10.0f,
    val surveyAccuracyBrix: Float = 10.0f,
    val role: String = "Standard",
    val deviceId: String = ""
)

data class AppConfig(
    val locMin: String = "",
    val seasonName: String = "",
    val seasonCode: String = "",
    val imageApiUrl: String = "",
    val imageDownloadApiUrl: String = ""
)


data class CompanyInfo(
    val name: String = "",
    val code: String = ""
)


data class UserPermissions(
    val mapChange: String = "N",
    val outSurvey: String = "N",
    val deleteSurvey: String = "N"
)

class MainConstant(private val context: Context) {

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _userSession = MutableStateFlow(loadUserSession())
    val userSession: StateFlow<UserSession> = _userSession.asStateFlow()

    private val _appConfig = MutableStateFlow(loadAppConfig())
    val appConfig: StateFlow<AppConfig> = _appConfig.asStateFlow()

    private val _userPermissions = MutableStateFlow(loadUserPermissions())
    val userPermissions: StateFlow<UserPermissions> = _userPermissions.asStateFlow()

    private val _companyInfoList = MutableStateFlow(loadCompanyInfoList())
    val companyInfoList: StateFlow<List<CompanyInfo>> = _companyInfoList.asStateFlow()

    fun saveLogin(session: UserSession) {
        with(editor) {
            putString(KEY_USERNAME, session.username)
            putString(KEY_USER_CODE, session.userCode)
            putString(KEY_MOBILE, session.mobile)
            putString(KEY_ECODE, session.eCode)
            putString(KEY_IP, session.ip)
            putString(KEY_DB_NAME, session.dbName)
            putString(KEY_DB_USER, session.dbUser)
            putString(KEY_DB_PASS, session.dbPass)
            putString(KEY_INSTANCE, session.instance)
            putString(KEY_FTP_PATH, session.ftpPath)
            putString(KEY_FTP_USER, session.ftpUser)
            putString(KEY_FTP_SERVER, session.ftpServer)
            putString(KEY_FTP_PASS, session.ftpPass)
            putString(KEY_SITE_CODE, session.siteCode)
            putString(KEY_COMPANY_NAME, session.companyName)
            putInt(KEY_SURVEY_AREA, session.surveyArea)
            putFloat(KEY_SURVEY_ACCURACY, session.surveyAccuracy)
            putFloat(KEY_SURVEY_ACCURACY_BRIX, session.surveyAccuracyBrix)
            putString(KEY_ROLE, session.role)
            putString(KEY_DEVICE_ID, session.deviceId)
            apply()
        }
        _userSession.value = session
    }
    fun saveLogin(
        username: String, userCode: String, mobile: String, eCode: String, ip: String,
        dbName: String, dbUser: String, dbPass: String, instance: String,
        ftpPath: String, ftpUser: String, ftpServer: String, ftpPass: String,
        siteCode: String, companyName: String, surveyArea: Int,
        surveyAccuracy: Float, surveyAccuracyBrix: Float,
        deviceId: String = ""
    ) {
        saveLogin(
            UserSession(
                username = username,
                userCode = userCode,
                mobile = mobile,
                eCode = eCode,
                ip = ip,
                dbName = dbName,
                dbUser = dbUser,
                dbPass = dbPass,
                instance = instance,
                ftpPath = ftpPath,
                ftpUser = ftpUser,
                ftpServer = ftpServer,
                ftpPass = ftpPass,
                siteCode = siteCode,
                companyName = companyName,
                surveyArea = surveyArea,
                surveyAccuracy = surveyAccuracy,
                surveyAccuracyBrix = surveyAccuracyBrix,
                deviceId = deviceId
            )
        )
    }

    fun saveAllConfigs(config: AppConfig) {
        with(editor) {
            putString(KEY_LOC_MIN, config.locMin)
            putString(KEY_SEASON_NAME, config.seasonName)
            putString(KEY_SEASON_CODE, config.seasonCode)
            putString(KEY_IMAGE_API_URL, config.imageApiUrl)
            putString(KEY_DOWNLOAD_IMAGE_API_URL, config.imageDownloadApiUrl)
            apply()
        }
        _appConfig.value = config
    }

    fun saveAllConfigs(
        seasonName: String,
        seasonCode: String,
        locMin: String,
        imageApiUrl: String,
        imageDownloadApiUrl: String = ""
    ) {
        saveAllConfigs(
            AppConfig(
                locMin = locMin,
                seasonName = seasonName,
                seasonCode = seasonCode,
                imageApiUrl = imageApiUrl,
                imageDownloadApiUrl = imageDownloadApiUrl
            )
        )
    }

    fun saveSys1(
        locMin: String,
        imageApiUrl: String,
        imageDownloadApiUrl: String = ""
    ) {
        val currentConfig = _appConfig.value
        saveAllConfigs(
            currentConfig.copy(
                locMin = locMin,
                imageApiUrl = imageApiUrl,
                imageDownloadApiUrl = imageDownloadApiUrl
            )
        )
    }

    fun saveCompanyInfo(companyInfo: CompanyInfo) {
        with(editor) {
            putString(KEY_CNAME, companyInfo.name)
            putString(KEY_CCODE, companyInfo.code)
            apply()
        }
        val currentList = _companyInfoList.value.toMutableList()
        if (currentList.isEmpty()) {
            _companyInfoList.value = listOf(companyInfo)
        } else {
            currentList[0] = companyInfo
            _companyInfoList.value = currentList
        }
    }
    fun getDeviceId(): String = _userSession.value.deviceId
    fun saveDeviceId(deviceId: String) {
        editor.putString(KEY_DEVICE_ID, deviceId).apply()
        val current = _userSession.value
        _userSession.value = current.copy(deviceId = deviceId)
    }
    fun saveCompanyInfo(name: String, code: String) {
        saveCompanyInfo(CompanyInfo(name, code))
    }

    fun saveCompanyInfoList(companies: List<CompanyInfo>) {
        val jsonArray = JSONArray()
        companies.forEach { company ->
            JSONObject().apply {
                put(JSON_KEY_CNAME, company.name)
                put(JSON_KEY_CCODE, company.code)
                jsonArray.put(this)
            }
        }
        editor.putString(KEY_COMPANY_LIST, jsonArray.toString()).apply()
        _companyInfoList.value = companies
    }

    fun saveCNameCCode(cname: String, ccode: String) {
        saveCompanyInfo(cname, ccode)
    }

    fun saveCNameCCodeList(data: List<Pair<String, String>>) {
        val companies = data.map { CompanyInfo(it.first, it.second) }
        saveCompanyInfoList(companies)
    }

    fun savePermissions(permissions: UserPermissions) {
        with(editor) {
            putString(KEY_MAP_PERMISSION, permissions.mapChange)
            putString(KEY_OUT_SURVEY_PERMISSION, permissions.outSurvey)
            putString(KEY_DELETE_SURVEY_PERMISSION, permissions.deleteSurvey)
            apply()
        }
        _userPermissions.value = permissions
    }

    fun saveMapPermission(value: String) {
        val current = _userPermissions.value
        savePermissions(current.copy(mapChange = value))
    }

    fun saveOutSurveyPermission(value: String) {
        val current = _userPermissions.value
        savePermissions(current.copy(outSurvey = value))
    }

    fun saveDelSurveyPermission(value: String) {
        val current = _userPermissions.value
        savePermissions(current.copy(deleteSurvey = value))
    }

    fun saveAllPermissions(mapPerm: String, outSurveyPerm: String, delPerm: String) {
        savePermissions(UserPermissions(mapPerm, outSurveyPerm, delPerm))
    }

    // Save user role
    fun setRole(role: String) {
        val current = _userSession.value
        saveLogin(current.copy(role = role))
    }

    fun saveCurrentYear(seasonName: String) {
        val current = _appConfig.value
        saveAllConfigs(current.copy(seasonName = seasonName))
    }

    fun currentYear(sName: String) = saveCurrentYear(sName)

    fun logout() {
        Log.d("MainConstant", "=== LOGOUT START ===")

        try {
            // 1. Clear the main prefs file SYNCHRONOUSLY (commit, not apply)
            val cleared = editor.clear().commit()
            Log.d("MainConstant", "Main prefs cleared: $cleared")

            // 2. Clear ALL other SharedPreferences files in the app
            clearAllSharedPreferences()

            // 3. Reset all StateFlows to default values
            _userSession.value = UserSession()
            _appConfig.value = AppConfig()
            _userPermissions.value = UserPermissions()
            _companyInfoList.value = emptyList()

            Log.d("MainConstant", "=== LOGOUT: All data cleared successfully ===")
        } catch (e: Exception) {
            Log.e("MainConstant", "Error during logout: ${e.message}", e)
        }
    }

    private fun clearAllSharedPreferences() {
        try {
            val prefsDir = File(context.applicationInfo.dataDir, "shared_prefs")
            if (prefsDir.exists() && prefsDir.isDirectory) {
                prefsDir.listFiles()?.forEach { file ->
                    val prefName = file.nameWithoutExtension
                    Log.d("MainConstant", "Clearing SharedPreferences file: $prefName")
                    context.getSharedPreferences(prefName, Context.MODE_PRIVATE)
                        .edit()
                        .clear()
                        .commit()
                }
            } else {
                Log.d("MainConstant", "No shared_prefs directory found")
            }
        } catch (e: Exception) {
            Log.e("MainConstant", "Error clearing all prefs: ${e.message}", e)
        }
    }

    // Another logout method that returns boolean for compatibility
    fun logoutAccount(): Boolean {
        logout()
        return true
    }

    // Clear specific data without full logout (if needed)
    fun clearAllPreferences() {
        editor.clear().commit()
        clearAllSharedPreferences()
        _userSession.value = UserSession()
        _appConfig.value = AppConfig()
        _userPermissions.value = UserPermissions()
        _companyInfoList.value = emptyList()
    }

    // Check if user is logged in
    fun isLoggedIn(): Boolean {
        return sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false)
    }

    // Set login status
    fun setLoggedIn(isLoggedIn: Boolean) {
        editor.putBoolean(KEY_IS_LOGGED_IN, isLoggedIn).apply()
    }

    // Getter methods
    fun getUsername(): String = _userSession.value.username
    fun getMobile(): String = _userSession.value.mobile
    fun geteCode(): String = _userSession.value.eCode
    fun getIp(): String = _userSession.value.ip
    fun getDbName(): String = _userSession.value.dbName
    fun getDbUser(): String = _userSession.value.dbUser
    fun getDbPass(): String = _userSession.value.dbPass
    fun getInstance(): String = _userSession.value.instance
    fun getFtpPath(): String = _userSession.value.ftpPath
    fun getFtpUser(): String = _userSession.value.ftpUser
    fun getFtpServer(): String = _userSession.value.ftpServer
    fun getFtpPass(): String = _userSession.value.ftpPass
    fun getSiteCode(): String = _userSession.value.siteCode
    fun getCompanyName(): String = _userSession.value.companyName
    fun getSurveyArea(): Int = _userSession.value.surveyArea
    fun getSurveyAccuracy(): Float = _userSession.value.surveyAccuracy
    fun getSurveyAccuracyBrix(): Float = _userSession.value.surveyAccuracyBrix
    fun getRole(): String = _userSession.value.role
    fun getUserCode(): String = _userSession.value.userCode

    // Config getters
    fun getSeasonName(): String = _appConfig.value.seasonName
    fun getSeasonCode(): String = _appConfig.value.seasonCode
    fun getLocMin(): String = _appConfig.value.locMin
    fun getCurrentYear(): String = _appConfig.value.seasonName
    fun getImageApiUrl(): String = _appConfig.value.imageApiUrl
    fun getImageDownloadApiUrl(): String = _appConfig.value.imageDownloadApiUrl

    // Permission getters
    fun getMapPermission(): String = _userPermissions.value.mapChange
    fun getOutSurveyPermission(): String = _userPermissions.value.outSurvey
    fun getDeleteSurveyPermission(): String = _userPermissions.value.deleteSurvey

    // Legacy getters
    fun getMapPermissionSP(): String = getMapPermission()
    fun getOutSurveyPermissionSP(): String = getOutSurveyPermission()
    fun getDelSurveyPermissionSP(): String = getDeleteSurveyPermission()

    // Company info getters
    fun getCName(): String = _companyInfoList.value.firstOrNull()?.name ?: ""
    fun getCCode(): String = _companyInfoList.value.firstOrNull()?.code ?: ""
    fun getCompanyInfoList(): List<CompanyInfo> = _companyInfoList.value

    fun getCNameCCodeList(): List<Pair<String, String>> {
        return _companyInfoList.value.map { it.name to it.code }
    }

    fun getCCodeList(): List<String> {
        return _companyInfoList.value.map { it.code }
    }

    private fun loadUserSession(): UserSession {
        return UserSession(
            username = sharedPreferences.getString(KEY_USERNAME, "") ?: "",
            userCode = sharedPreferences.getString(KEY_USER_CODE, "") ?: "",
            mobile = sharedPreferences.getString(KEY_MOBILE, "") ?: "",
            eCode = sharedPreferences.getString(KEY_ECODE, "") ?: "",
            ip = sharedPreferences.getString(KEY_IP, "") ?: "",
            dbName = sharedPreferences.getString(KEY_DB_NAME, "") ?: "",
            dbUser = sharedPreferences.getString(KEY_DB_USER, "") ?: "",
            dbPass = sharedPreferences.getString(KEY_DB_PASS, "") ?: "",
            instance = sharedPreferences.getString(KEY_INSTANCE, "") ?: "",
            ftpPath = sharedPreferences.getString(KEY_FTP_PATH, "") ?: "",
            ftpUser = sharedPreferences.getString(KEY_FTP_USER, "") ?: "",
            ftpServer = sharedPreferences.getString(KEY_FTP_SERVER, "") ?: "",
            ftpPass = sharedPreferences.getString(KEY_FTP_PASS, "") ?: "",
            siteCode = sharedPreferences.getString(KEY_SITE_CODE, "") ?: "",
            companyName = sharedPreferences.getString(KEY_COMPANY_NAME, "") ?: "",
            surveyArea = sharedPreferences.getInt(KEY_SURVEY_AREA, 0),
            surveyAccuracy = sharedPreferences.getFloat(KEY_SURVEY_ACCURACY, 10.0f),
            surveyAccuracyBrix = sharedPreferences.getFloat(KEY_SURVEY_ACCURACY_BRIX, 10.0f),
            role = sharedPreferences.getString(KEY_ROLE, "Standard") ?: "Standard",
            deviceId = sharedPreferences.getString(KEY_DEVICE_ID, "") ?: ""
        )
    }

    private fun loadAppConfig(): AppConfig {
        return AppConfig(
            locMin = sharedPreferences.getString(KEY_LOC_MIN, "") ?: "",
            seasonName = sharedPreferences.getString(KEY_SEASON_NAME, "") ?: "",
            seasonCode = sharedPreferences.getString(KEY_SEASON_CODE, "") ?: "",
            imageApiUrl = sharedPreferences.getString(KEY_IMAGE_API_URL, "") ?: "",
            imageDownloadApiUrl = sharedPreferences.getString(KEY_DOWNLOAD_IMAGE_API_URL, "") ?: ""
        )
    }

    private fun loadUserPermissions(): UserPermissions {
        return UserPermissions(
            mapChange = sharedPreferences.getString(KEY_MAP_PERMISSION, "N") ?: "N",
            outSurvey = sharedPreferences.getString(KEY_OUT_SURVEY_PERMISSION, "N") ?: "N",
            deleteSurvey = sharedPreferences.getString(KEY_DELETE_SURVEY_PERMISSION, "N") ?: "N"
        )
    }

    private fun loadCompanyInfoList(): List<CompanyInfo> {
        val jsonString = sharedPreferences.getString(KEY_COMPANY_LIST, null) ?: return emptyList()
        return try {
            val jsonArray = JSONArray(jsonString)
            val companies = mutableListOf<CompanyInfo>()
            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)
                companies.add(
                    CompanyInfo(
                        name = jsonObject.getString(JSON_KEY_CNAME),
                        code = jsonObject.getString(JSON_KEY_CCODE)
                    )
                )
            }
            companies
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    private val editor: SharedPreferences.Editor
        get() = sharedPreferences.edit()

    companion object {
        private const val PREFS_NAME = "e_ethanol_prefs"
        private const val KEY_USERNAME = "name"
        private const val KEY_USER_CODE = "userCode"
        private const val KEY_MOBILE = "mobile"
        private const val KEY_ECODE = "code"
        private const val KEY_IP = "ip"
        private const val KEY_DB_NAME = "dbName"
        private const val KEY_DB_USER = "dbUser"
        private const val KEY_DB_PASS = "dbPass"
        private const val KEY_INSTANCE = "instance"
        private const val KEY_FTP_PATH = "ftpPath"
        private const val KEY_FTP_USER = "ftpUser"
        private const val KEY_FTP_SERVER = "ftpServer"
        private const val KEY_FTP_PASS = "ftpPass"
        private const val KEY_SITE_CODE = "siteCode"
        private const val KEY_COMPANY_NAME = "companyName"
        private const val KEY_SURVEY_AREA = "k_surveyArea"
        private const val KEY_SURVEY_ACCURACY = "K_SurveyAccuracy"
        private const val KEY_SURVEY_ACCURACY_BRIX = "k_SurveyAccuracyBrix"
        private const val KEY_ROLE = "E_Role"
        private const val KEY_LOC_MIN = "C_LocMin"
        private const val KEY_SEASON_NAME = "season_name"
        private const val KEY_SEASON_CODE = "season_code"
        private const val KEY_IMAGE_API_URL = "image_api_url"
        private const val KEY_DOWNLOAD_IMAGE_API_URL = "image_download_api_url"
        private const val KEY_MAP_PERMISSION = "G_MapChangePerm"
        private const val KEY_OUT_SURVEY_PERMISSION = "G_OutSurveyPerm"
        private const val KEY_DELETE_SURVEY_PERMISSION = "G_DelSurveyPerm"
        private const val KEY_CNAME = "cname"
        private const val KEY_CCODE = "ccode"
        private const val KEY_COMPANY_LIST = "company_list"
        private const val JSON_KEY_CNAME = "cname"
        private const val JSON_KEY_CCODE = "ccode"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_DEVICE_ID = "deviceId"
    }
}