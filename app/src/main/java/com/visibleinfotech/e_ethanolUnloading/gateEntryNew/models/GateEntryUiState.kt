package com.visibleinfotech.e_ethanolUnloading.gateEntryNew.models


data class GateEntryUiState(
    // Truck selection
    val truckList: List<String> = emptyList(),
    val filteredTruckList: List<String> = emptyList(),
    val truckSearchText: String = "",
    val selectedTruckNo: String = "",

    // Driver details
    val driverName: String = "",
    val driverMobile: String = "",

    // Dispatch details
    val invoiceNo: String = "",
    val quantity: String = "",
    val partyName: String = "",
    val brokerName: String = "",

    // Invoice details
    val invoiceWeight: String = "",
    val numberOfBags: String = "",
    val invoiceRate: String = "",
    val invoiceAmount: String = "",

    // Mode selection
    val modeList: List<ModeMasterData> = emptyList(),
    val filteredModeList: List<ModeMasterData> = emptyList(),
    val modeSearchText: String = "",
    val selectedModeCode: String = "",
    val selectedModeName: String = "",

    // Document names from database
    val doc1Name: String = "",
    val doc2Name: String = "",
    val doc3Name: String = "",
    val doc4Name: String = "",
    val doc5Name: String = "",

    // Document availability flags
    val doc1Available: Boolean = false,
    val doc2Available: Boolean = false,
    val doc3Available: Boolean = false,
    val doc4Available: Boolean = false,
    val doc5Available: Boolean = false,

    // User uploaded document URIs
    val doc1Uri: String = "",
    val doc2Uri: String = "",
    val doc3Uri: String = "",
    val doc4Uri: String = "",
    val doc5Uri: String = "",

    // User uploaded flags
    val doc1UserUploaded: Boolean = false,
    val doc2UserUploaded: Boolean = false,
    val doc3UserUploaded: Boolean = false,
    val doc4UserUploaded: Boolean = false,
    val doc5UserUploaded: Boolean = false,


    // Additional details
    val remarks: String = "",
    val currentDateTime: String = "",

    // UI states
    val isLoading: Boolean = false,
    val isSubmitting: Boolean = false,
    val showValidationErrors: Boolean = false,
    val fullScreenImageUrl: String? = null,

    // API URLs
    val imageApiUrl: String? = null
)