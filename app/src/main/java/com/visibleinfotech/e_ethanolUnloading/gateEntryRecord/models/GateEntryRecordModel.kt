package com.visibleinfotech.e_ethanolUnloading.gateEntryRecord.models

data class GateEntryRecordModel(
    val gCode: String,
    val truckNo: String,
    val driverName: String,
    val driverMobile: String,
    val invoiceNo: String,
    val qty: String,
    val partyName: String,
    val brokerName: String,
    val modeCode: String,
    val modeName: String,
    val bags: String,
    val invQty: String,
    val invAmt: String,
    val invRate: String,
    val date: String,
    val status: String,
    val remarks: String,
    val user: String,
    val doc1: String,
    val doc2: String,
    val doc3: String,
    val doc4: String,
    val doc5: String
)