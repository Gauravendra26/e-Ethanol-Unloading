package com.visibleinfotech.e_ethanolUnloading.unloading.models

data class UnloadingUiState(
    val vehicleList: List<VehicleData> = emptyList(),
    val filteredVehicleList: List<VehicleData> = emptyList(),
    val vehicleSearchText: String = "",
    val selectedVehicle: String = "",
    val selectedGatePass: String = "",

    val selectedGrossData: GrossData? = null,
    val isLoadingGrossData: Boolean = false,
    val isSubmitting: Boolean = false,

    val godownList: List<GodownData> = emptyList(),
    val filteredGodownList: List<GodownData> = emptyList(),
    val godownSearchText: String = "",
    val selectedGodown: GodownData? = null,

    val thekedarList: List<ThekedarData> = emptyList(),
    val filteredThekedarList: List<ThekedarData> = emptyList(),
    val thekedarSearchText: String = "",
    val selectedThekedar: ThekedarData? = null,
    val rejectList: List<RejectData> = emptyList(),

    val isLoading: Boolean = false,
    val errorMessage: String = "",
    val showValidationErrors: Boolean = false,
    val hasPreviousUnloading: Boolean = false,
    val labMoistureHighAvg: String = "N",
    val moistureRejectionValue: Double = 0.0,
    val showRejectLimitBanner: Boolean = true,
    val godownLocked: Boolean = false,
    val thekedarLocked: Boolean = false
)