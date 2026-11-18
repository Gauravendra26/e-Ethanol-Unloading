package com.visibleinfotech.e_ethanolUnloading.login

data class LoginDetail(
    val role: String,
    val kSurveyAccuracy: String,
    val kSurveyAccuracyBrix: String,
    val kSurveyArea: String,
    val companyCode: String,
    val ecode: String,
    val isActive: Boolean,
    val username: String,
    val mobile: String,
    val dbIp: String,
    val dbUser: String,
    val dbPass: String,
    val dbName: String,
    val instance: String?,
    val ftpIp: String,
    val ftpPath: String,
    val ftpUser: String,
    val ftpPass: String,
    val siteCode: String,
    val companyName: String
)
