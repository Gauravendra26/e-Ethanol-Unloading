package com.visibleinfotech.e_ethanolUnloading.sauda

data class VerifiedSaudaRecord(
    val plotId: String = "",
    val partyName: String = "",
    val placeName: String = "",
    val cropType: String = "",
    val brokerName: String = "",
    val area: String = "",
    val rate: String = "",
    val perBagDed: String = "",
    val paymentDueDays: String = "",
    val wtDed: String = "",
    val cashDisc: String = "",
    val approvedBy: String = "",
    val approvedDate: String = "",
    val verifiedBy: String = "",
    val verifiedDate: String = "",
    val createdOn: String = "",
)