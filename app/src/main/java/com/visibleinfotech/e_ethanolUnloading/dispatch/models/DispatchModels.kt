package com.visibleinfotech.e_ethanolUnloading.dispatch.models

data class Party(
    val accountId: String,
    val accountName: String,
    val placeName: String,
    val placeId: String,
    val brokerCode: String,
    val brokerName: String = ""
)

data class DispatchRequest(
    val partyId: String,
    val partyName: String,
    val brokerId: String,
    val vehicleNumber: String,
    val driverName: String,
    val driverMobileNumber: String,
    val invoiceNumber: String,
    val invoiceDate: String = "",        // ✅ ADD — format: yyyy-MM-dd
    val dispatchQuantity: Double,
    val dateTime: String,
    val anugyaNumber: String,
    val anugyaImageUri: String? = null,
    val biltyImageUri: String? = null,
    val invoiceImageUri: String? = null,
    val weighmentSlipImageUri: String? = null,
    val otherDocumentImageUri: String? = null
)
data class DispatchResult(
    val success: Boolean,
    val message: String
)

data class PartyAllocation(
    val plEstTons: Double,
    val plWeigh: Double,
    val dQty: Double = 0.0,
    val finalAvailableQuantity: Double = 0.0,
    val isValid: Boolean = true
) {
    val availableQuantity: Double
        get() = finalAvailableQuantity

    val calculatedData: Double
        get() = plEstTons - plWeigh

    val isZeroOrNegative: Boolean
        get() = finalAvailableQuantity <= 0
}