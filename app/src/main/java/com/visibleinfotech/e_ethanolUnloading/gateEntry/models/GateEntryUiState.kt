package com.visibleinfotech.e_ethanolUnloading.gateEntry.models

data class GateEntryUiState(
    // Truck selection
    val truckList: List<String> = emptyList(),
    val filteredTruckList: List<String> = emptyList(),
    val truckSearchText: String = "",
    val selectedTruckNo: String = "",

    // Form fields
    val driverName: String = "",
    val driverMobile: String = "",
    val invoiceNo: String = "",
    val quantity: String = "",
    val partyName: String = "",
    val brokerName: String = "",
    val invoiceWeight: String = "",
    val numberOfBags: String = "",
    val invoiceRate: String = "",
    val invoiceAmount: String = "",
    val remarks: String = "",

    // Mode fields
    val modeList: List<ModeMasterData> = emptyList(),
    val filteredModeList: List<ModeMasterData> = emptyList(),
    val modeSearchText: String = "",
    val selectedModeCode: String = "",
    val selectedModeName: String = "",

    // Document fields
    val doc1Name: String = "",
    val doc2Name: String = "",
    val doc3Name: String = "",
    val doc4Name: String = "",
    val doc5Name: String = "",

    val doc1Available: Boolean = false,
    val doc2Available: Boolean = false,
    val doc3Available: Boolean = false,
    val doc4Available: Boolean = false,
    val doc5Available: Boolean = false,

    val doc1Uri: String = "",
    val doc2Uri: String = "",
    val doc3Uri: String = "",
    val doc4Uri: String = "",
    val doc5Uri: String = "",

    val doc1UserUploaded: Boolean = false,
    val doc2UserUploaded: Boolean = false,
    val doc3UserUploaded: Boolean = false,
    val doc4UserUploaded: Boolean = false,
    val doc5UserUploaded: Boolean = false,
    val rfidTag: String = "",
    // Plot Allocation
    val plotAllocationSummary: TruckPlotAllocationSummary? = null,
    val isLoadingPlotAllocation: Boolean = false,

    // UI states
    val isLoading: Boolean = false,
    val isSubmitting: Boolean = false,
    val showValidationErrors: Boolean = false,
    val imageApiUrl: String? = null,
    val fullScreenImageUrl: String? = null,
    val currentDateTime: String = ""
)