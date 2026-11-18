package com.visibleinfotech.e_ethanolUnloading.unloadRecord.models

enum class UnloadStatus {
    UNLOADED,       // 🟢 green  — GR_UNLOADFLAG='Y', U_RejectCode=0/NULL, U_DedCode=NULL/0
    TEMP_REJECTED,  // 🟠 orange — GR_UNLOADFLAG='Y', U_RejectCode<>0,     U_DedCode=NULL/0
    REJECTED,       // 🔴 red    — GR_UNLOADFLAG='Y', U_RejectCode<>0,     U_DedCode=4
    UNKNOWN
}

data class UnloadingRecordModel(
    val wsNo: String = "",
    val partyName: String = "",
    val brokerName: String = "",
    val grossWeight: String = "",
    val thekedarName: String = "",
    val indentNo: String = "",
    val vehicleNo: String = "",
    val godownName: String = "",
    val unloadUser: String = "",
    val unloadDate: String = "",
    val status: UnloadStatus = UnloadStatus.UNKNOWN
) {
    val detailKey: String get() = "$wsNo|$indentNo|$vehicleNo"

    val tempReject: String
        get() = if (status == UnloadStatus.TEMP_REJECTED ||
            status == UnloadStatus.REJECTED) "Y" else "N"
}

