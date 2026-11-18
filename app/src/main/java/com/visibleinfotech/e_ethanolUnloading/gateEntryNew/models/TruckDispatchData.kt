package com.visibleinfotech.e_ethanolUnloading.gateEntryNew.models

data class TruckDispatchData(
    val dCode: String,
    val truckNo: String,
    val driverName: String,
    val driverMobile: String,
    val invoiceNo: String,
    val quantity: String,
    val partyName: String,
    val brokerName: String,
    val doc1: String,
    val doc2: String,
    val doc3: String,
    val doc4: String,
    val doc5: String,
    val updateBy: String = "",  // Added
    val updateDate: String = "" // Added
)