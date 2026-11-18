package com.visibleinfotech.e_ethanolUnloading.gateEntry.models

data class GateEntryUpdateRequest(
    val dCode: String,
    val truckNo: String,
    val driverName: String = "",
    val driverMobile: String = "",
    val invoiceNo: String = "",
    val quantity: String = "",
    val partyName: String = "",
    val brokerName: String = "",
    val modeCode: String = "",
    val invoiceWeight: String = "",
    val numberOfBags: String = "",
    val invoiceRate: String = "",
    val invoiceAmount: String = "",
    val remarks: String = "",
    val doc1Uri: String? = null,
    val doc2Uri: String? = null,
    val doc3Uri: String? = null,
    val doc4Uri: String? = null,
    val doc5Uri: String? = null,
    val updateBy: String = "",  // Added
    val updateDate: String = "" // Added
)