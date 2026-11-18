package com.visibleinfotech.e_ethanolUnloading.dispatch.models

data class DispatchRecord(
    val dCode: String,
    val brokerCode: String,
    val brokerName: String,
    val partyCode: String,
    val partyName: String,
    val truckNo: String,
    val driverName: String,
    val driverMobile: String,
    val invoiceNo: String,
    val quantity: String,
    val date: String,
    val userName: String,
    val status: String
)