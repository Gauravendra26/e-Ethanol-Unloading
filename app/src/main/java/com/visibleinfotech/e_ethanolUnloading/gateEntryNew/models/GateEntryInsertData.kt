package com.visibleinfotech.e_ethanolUnloading.gateEntryNew.models


data class GateEntryInsertData(
    val truckNo: String,
    val partyCode: String,
    val brokerCode: String,
    val dCode: String,  // D_Code from Vipl_ETDispatch
    val qty: String,    // D_Qty from selected truck
    val invoiceNo: String,
    val invQty: String,  // Entered invoice weight
    val invAmt: String,  // Entered invoice amount
    val doc1: String? = null,
    val doc2: String? = null,
    val doc3: String? = null,
    val doc4: String? = null,
    val doc5: String? = null,
    val date: String,    // Current date in SQL format
    val user: String,    // Username from MainConstant
    val modeCode: String,
    val bags: String,    // Entered number of bags
    val invRate: String, // Entered invoice rate
    val remarks: String? = null
)

data class GateEntryInsertResult(
    val success: Boolean,
    val message: String,
    val gCode: Int = 0
)