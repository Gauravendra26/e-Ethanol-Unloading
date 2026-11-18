package com.visibleinfotech.e_ethanolUnloading.sauda.models

data class SaudaItem(
    val plotId: String = "",
    val partyName: String = "",
    val placeName: String = "",
    val cropType: String = "",
    val landName: String = "",
    val area: String = "",
    val rate: String = "",
    val amount: String = "",
    val paymentCond: String = "",
    val paymentDueDays: String = "",
    val cashDisc: String = "",
    val wtDed: String = "",
    val brokerCode: String = "",
    val brokerName: String = "",
    val createdOn: String = "",
    val createdBy: String = "",
    val approve: String = "",
    val saudaApprovedBy: String = "",
    val perBagDed: String = "",
    val saudaApprovedDate: String = ""
)

data class SaudaApproveResult(
    val success: Boolean,
    val message: String
)