package com.visibleinfotech.e_ethanolUnloading.indentAndToken

data class TruckOption(
    val gCode: Int,
    val truckNo: String
) {
    override fun toString(): String = truckNo
}

data class PendingIndentEntry(
    val gCode: Int = 0,
    val truckNo: String = "",
    val partyCode: Int? = null,
    val brokerCode: Int? = null,
    val doNo: Int? = null,
    val qty: String = "",
    val invoiceNo: String = "",
    val invQty: String = "",
    val invAmt: String = "",
    val doc1: String = "",
    val doc2: String = "",
    val doc3: String = "",
    val doc4: String = "",
    val doc5: String = "",
    val date: String = "",
    val user: String = "",
    val modeName: String = "",
    val bags: Int? = null,
    val invRate: String = "",
    val remarks: String = "",
    val partyName: String = "",
    val brokerName: String = "",
    val driverName: String = "",
    val driverMobile: String = "",
    val itemCode: Int? = null,
    val challanNo: String = "",
    val challanDate: String = "",
    val capturedPhoto: String = ""
)

data class IndentUpdateResult(
    val success: Boolean,
    val message: String
)

data class TokenUpdateData(
    val gCode: Int,
    val truckNo: String,
    val driverName: String,
    val driverMobile: String,
    val doNo: String,
    val remarks: String,
    val updateBy: String,
    val updateDate: String
)