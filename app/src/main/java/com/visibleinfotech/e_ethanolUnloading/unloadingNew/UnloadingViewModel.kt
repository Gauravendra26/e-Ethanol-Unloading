package com.visibleinfotech.e_ethanolUnloading.unloadingNew

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.visibleinfotech.e_ethanolUnloading.MainConstant
import com.visibleinfotech.e_ethanolUnloading.unloading.models.GodownData
import com.visibleinfotech.e_ethanolUnloading.unloading.models.ThekedarData
import com.visibleinfotech.e_ethanolUnloading.unloading.models.UnloadingRow
import com.visibleinfotech.e_ethanolUnloading.unloading.models.UnloadingUiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val TAG = "UnloadingViewModelLog"

class UnloadingViewModel(
    private val repository: UnloadingRepository,
    private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(UnloadingUiState())
    val uiState: StateFlow<UnloadingUiState> = _uiState.asStateFlow()

    init {
        loadVehicles()
        loadMoistureRejectionValue()   // ← MUST BE HERE
    }

    private fun loadMoistureRejectionValue() {
        viewModelScope.launch {
            Log.d(TAG, "loadMoistureRejectionValue() called")
            try {
                val value = repository.getMoistureRejectionValue()
                Log.d(TAG, "Moisture rejection value fetched = $value")
                _uiState.update { current ->
                    Log.d(TAG, "Updating uiState.moistureRejectionValue: ${current.moistureRejectionValue} -> $value")
                    current.copy(moistureRejectionValue = value)
                }
                Log.d(TAG, "Moisture rejection value loaded: $value")
            } catch (e: Exception) {
                Log.e(TAG, "Error loading moisture rejection value", e)
            }
        }
    }
    private fun loadVehicles() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, errorMessage = "") }
                val vehicles = repository.getVehicleList()
                Log.d(TAG, "loadVehicles() returned ${vehicles.size} rows")
                _uiState.update {
                    it.copy(
                        vehicleList = vehicles,
                        filteredVehicleList = vehicles,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading vehicles", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Failed to load vehicles"
                    )
                }
            }
        }
    }

    fun refreshVehicles() = loadVehicles()

    fun updateVehicleSearchText(text: String) {
        Log.d(TAG, "updateVehicleSearchText: '$text'")
        _uiState.update { state ->
            val filtered = if (text.isBlank()) {
                state.vehicleList
            } else {
                state.vehicleList.filter {
                    it.vehicleNo.contains(text, ignoreCase = true) ||
                            it.gatePass.contains(text, ignoreCase = true)
                }
            }
            state.copy(
                vehicleSearchText = text,
                filteredVehicleList = filtered
            )
        }
    }

    fun selectVehicle(vehicleNo: String, gatePass: String) {
        Log.d(TAG, "selectVehicle: '$vehicleNo' (gatepass=$gatePass)")
        _uiState.update {
            it.copy(
                selectedVehicle = vehicleNo,
                selectedGatePass = gatePass,
                vehicleSearchText = vehicleNo,
                selectedGrossData = null,
                selectedGodown = null,
                selectedThekedar = null,
                godownSearchText = "",
                thekedarSearchText = "",
                hasPreviousUnloading = false
            )
        }
        loadGrossData(gatePass)
        loadGodowns()
        loadThekedars()
    }

    fun clearSelectedVehicle() {
        Log.d(TAG, "clearSelectedVehicle")
        _uiState.update {
            it.copy(
                selectedVehicle = "",
                selectedGatePass = "",
                vehicleSearchText = "",
                filteredVehicleList = it.vehicleList,
                selectedGrossData = null,
                selectedGodown = null,
                selectedThekedar = null,
                godownSearchText = "",
                thekedarSearchText = "",
                godownList = emptyList(),
                filteredGodownList = emptyList(),
                thekedarList = emptyList(),
                filteredThekedarList = emptyList(),
                hasPreviousUnloading = false
            )
        }
    }

    private fun loadGrossData(gatePass: String) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoadingGrossData = true) }
                val gross = repository.getGrossDataByGatePass(gatePass)
                _uiState.update {
                    it.copy(
                        selectedGrossData = gross,
                        isLoadingGrossData = false
                    )
                }
                Log.d(TAG, "Gross data loaded: ${gross?.growerName}")
            } catch (e: Exception) {
                Log.e(TAG, "Error loading gross data", e)
                _uiState.update {
                    it.copy(
                        isLoadingGrossData = false,
                        errorMessage = e.message ?: "Failed to load gross data"
                    )
                }
            }
        }
    }

    // =========================================================
    //  PREVIOUS UNLOADING ROWS + RESTORE GODOWN/THEKEDAR
    // =========================================================
    fun loadPreviousUnloading(
        gatePass: String,
        vehicleNo: String,
        onResult: (List<UnloadingRow>) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val gross = _uiState.value.selectedGrossData
                val indent = gross?.indentNo?.ifBlank { gross.indent } ?: ""

                if (indent.isBlank()) {
                    onResult(emptyList())
                    return@launch
                }

                val rows = repository.getPreviousUnloadingRows(
                    indent = indent,
                    wsNo = gatePass,
                    vehicleNo = vehicleNo
                )

                _uiState.update { it.copy(hasPreviousUnloading = rows.isNotEmpty()) }
                onResult(rows)

                if (rows.isEmpty()) return@launch

                // ✅ Restore godown from previous rows
                val prevGodownCode = repository.getPreviousGodownCode(
                    indent = indent,
                    wsNo = gatePass,
                    vehicleNo = vehicleNo
                )

                // ✅ NEW: Restore thekedar from U_Contractor on the previous rows.
                //         Falls back to Gross transporter code if U_Contractor is empty.
                val prevContractorCode = repository.getPreviousContractorCode(
                    indent = indent,
                    wsNo = gatePass,
                    vehicleNo = vehicleNo
                )

                val (grossTranspCode, grossTranspName) =
                    repository.getPreviousTransporter(gatePass)

                val effectiveThekedarCode =
                    prevContractorCode.ifBlank { grossTranspCode }

                // Only use the Gross name when the code also came from Gross
                val effectiveThekedarName =
                    if (prevContractorCode.isNotBlank()) "" else grossTranspName

                Log.d(
                    TAG,
                    "loadPreviousUnloading: restoring godown=$prevGodownCode " +
                            "thekedarCode=$effectiveThekedarCode " +
                            "(source=${if (prevContractorCode.isNotBlank()) "pending/previous" else "gross"}) " +
                            "name='$effectiveThekedarName'"
                )

                restoreGodownAndThekedar(
                    godownCode = prevGodownCode,
                    transpCode = effectiveThekedarCode,
                    transpName = effectiveThekedarName
                )

            } catch (e: Exception) {
                Log.e(TAG, "loadPreviousUnloading error", e)
                onResult(emptyList())
            }
        }
    }

    fun loadPendingUnloading(
        gatePass: String,
        vehicleNo: String,
        onResult: (List<UnloadingRow>) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val gross = _uiState.value.selectedGrossData
                val indent = gross?.indentNo?.ifBlank { gross.indent } ?: ""

                if (indent.isBlank()) {
                    onResult(emptyList())
                    return@launch
                }

                val rows = repository.getPendingUnloadingRows(
                    indent = indent,
                    wsNo = gatePass,
                    vehicleNo = vehicleNo
                )

                Log.d(
                    TAG,
                    "loadPendingUnloading: ${rows.size} pending rows " +
                            "(indent=$indent wsNo=$gatePass vehicle=$vehicleNo)"
                )
                onResult(rows)

                if (rows.isEmpty()) return@launch

                // ✅ Restore godown from pending rows (fallback to Gross godown)
                val pendingGodownCode = repository.getPreviousGodownCode(
                    indent = indent,
                    wsNo = gatePass,
                    vehicleNo = vehicleNo
                )
                val prevGodownCode = pendingGodownCode.ifBlank {
                    gross?.godownCode ?: ""
                }

                // ✅ NEW: Restore thekedar from U_Contractor on the pending rows.
                //         Falls back to Gross transporter code if U_Contractor is empty.
                val pendingContractorCode = repository.getPreviousContractorCode(
                    indent = indent,
                    wsNo = gatePass,
                    vehicleNo = vehicleNo
                )

                val (grossTranspCode, grossTranspName) =
                    repository.getPreviousTransporter(gatePass)

                val effectiveThekedarCode =
                    pendingContractorCode.ifBlank { grossTranspCode }

                val effectiveThekedarName =
                    if (pendingContractorCode.isNotBlank()) "" else grossTranspName

                Log.d(
                    TAG,
                    "loadPendingUnloading: restoring godown=$prevGodownCode " +
                            "thekedarCode=$effectiveThekedarCode " +
                            "(source=${if (pendingContractorCode.isNotBlank()) "pending" else "gross"}) " +
                            "name='$effectiveThekedarName'"
                )

                restoreGodownAndThekedar(
                    godownCode = prevGodownCode,
                    transpCode = effectiveThekedarCode,
                    transpName = effectiveThekedarName
                )

            } catch (e: Exception) {
                Log.e(TAG, "loadPendingUnloading error", e)
                onResult(emptyList())
            }
        }
    }

    private fun restoreGodownAndThekedar(
        godownCode: String,
        transpCode: String,
        transpName: String
    ) {
        viewModelScope.launch {
            var waited = 0
            while (waited < 30) {
                val s = _uiState.value
                if (s.godownList.isNotEmpty() && s.thekedarList.isNotEmpty()) break
                delay(100)
                waited++
            }

            val state = _uiState.value

            Log.d(
                TAG,
                "restoreGodownAndThekedar: " +
                        "godownCode='$godownCode' " +
                        "transpCode='$transpCode' transpName='$transpName' " +
                        "godownCount=${state.godownList.size} " +
                        "thekedarCount=${state.thekedarList.size}"
            )

            // ---- Godown ----
            if (godownCode.isNotBlank() && state.godownList.isNotEmpty()) {
                val match = state.godownList.firstOrNull {
                    it.code.trim().equals(godownCode.trim(), ignoreCase = true)
                }
                if (match != null) {
                    _uiState.update {
                        it.copy(
                            selectedGodown = match,
                            godownSearchText = match.name
                        )
                    }
                    Log.d(TAG, "Auto-selected godown: ${match.name} (${match.code})")
                } else {
                    Log.w(
                        TAG,
                        "No godown matched '$godownCode'. Available codes = " +
                                state.godownList.map { it.code }
                    )
                }
            } else {
                Log.w(
                    TAG,
                    "Skipping godown restore — godownCode='$godownCode', " +
                            "godownList.size=${state.godownList.size}"
                )
            }

            // ---- Thekedar ----
            if (state.thekedarList.isNotEmpty()) {
                var match = state.thekedarList.firstOrNull {
                    it.accountId.trim().equals(transpCode.trim(), ignoreCase = true)
                }
                if (match == null && transpName.isNotBlank()) {
                    match = state.thekedarList.firstOrNull {
                        it.accountName.trim().equals(transpName.trim(), ignoreCase = true)
                    }
                }
                if (match != null) {
                    _uiState.update {
                        it.copy(
                            selectedThekedar = match,
                            thekedarSearchText = match.accountName
                        )
                    }
                    Log.d(TAG, "Auto-selected thekedar: ${match.accountName} (${match.accountId})")
                } else {
                    Log.w(
                        TAG,
                        "No thekedar matched transpCode='$transpCode' name='$transpName'. " +
                                "Available = ${state.thekedarList.map { "${it.accountId}/${it.accountName}" }}"
                    )
                }
            } else {
                Log.w(TAG, "Skipping thekedar restore — thekedarList is empty")
            }
        }
    }

    // =========================================================
    //  GODOWN
    // =========================================================
    private fun loadGodowns() {
        viewModelScope.launch {
            try {
                val godowns = repository.getGodownList()
                _uiState.update {
                    it.copy(
                        godownList = godowns,
                        filteredGodownList = godowns
                    )
                }
                Log.d(TAG, "Loaded ${godowns.size} godowns")
            } catch (e: Exception) {
                Log.e(TAG, "Error loading godowns", e)
                _uiState.update {
                    it.copy(errorMessage = e.message ?: "Failed to load godowns")
                }
            }
        }
    }

    fun updateGodownSearchText(text: String) {
        _uiState.update { state ->
            val filtered = if (text.isBlank()) {
                state.godownList
            } else {
                state.godownList.filter {
                    it.name.contains(text, ignoreCase = true) ||
                            it.code.contains(text, ignoreCase = true) ||
                            it.capacity.contains(text, ignoreCase = true)
                }
            }
            state.copy(godownSearchText = text, filteredGodownList = filtered)
        }
    }

    fun selectGodown(godown: GodownData) {
        _uiState.update {
            it.copy(selectedGodown = godown, godownSearchText = godown.name)
        }
    }

    fun clearSelectedGodown() {
        _uiState.update {
            it.copy(
                selectedGodown = null,
                godownSearchText = "",
                filteredGodownList = it.godownList
            )
        }
    }

    // =========================================================
    //  THEKEDAR
    // =========================================================
    private fun loadThekedars() {
        viewModelScope.launch {
            try {
                val thekedars = repository.getThekedarList()
                _uiState.update {
                    it.copy(
                        thekedarList = thekedars,
                        filteredThekedarList = thekedars
                    )
                }
                Log.d(TAG, "Loaded ${thekedars.size} thekedars")
            } catch (e: Exception) {
                Log.e(TAG, "Error loading Thekedars", e)
                _uiState.update {
                    it.copy(errorMessage = e.message ?: "Failed to load Thekedars")
                }
            }
        }
    }

    fun updateThekedarSearchText(text: String) {
        _uiState.update { state ->
            val filtered = if (text.isBlank()) {
                state.thekedarList
            } else {
                state.thekedarList.filter {
                    it.accountName.contains(text, ignoreCase = true) ||
                            it.accountId.contains(text, ignoreCase = true)
                }
            }
            state.copy(thekedarSearchText = text, filteredThekedarList = filtered)
        }
    }

    fun selectThekedar(thekedar: ThekedarData) {
        _uiState.update {
            it.copy(selectedThekedar = thekedar, thekedarSearchText = thekedar.accountName)
        }
    }

    fun clearSelectedThekedar() {
        _uiState.update {
            it.copy(
                selectedThekedar = null,
                thekedarSearchText = "",
                filteredThekedarList = it.thekedarList
            )
        }
    }

    // =========================================================
    //  VALIDATION
    // =========================================================
    fun isFormValid(): Boolean {
        val s = _uiState.value
        return s.selectedVehicle.isNotBlank() &&
                s.selectedGodown != null &&
                s.selectedThekedar != null
    }

    fun setShowValidationErrors(show: Boolean) {
        _uiState.update { it.copy(showValidationErrors = show) }
    }

    // =========================================================
    //  ADD ROW TO DB
    // =========================================================
    fun addRowToDb(
        row: UnloadingRow,
        allRows: List<UnloadingRow>,
        onResult: (Long, String) -> Unit
    ) {
        val state = _uiState.value
        val gross = state.selectedGrossData

        if (state.selectedVehicle.isBlank() || gross == null) {
            onResult(-1L, "Vehicle or Gross data missing")
            return
        }
        if (state.selectedGodown == null) {
            onResult(-1L, "Please select a godown first")
            return
        }
        if (state.selectedThekedar == null) {
            onResult(-1L, "Please select a thekedar first")
            return
        }
        if (row.bags.isBlank() || row.moisture.isBlank()) {
            onResult(-1L, "Please fill Bags and Moisture")
            return
        }

        val bagsInt = row.bags.toIntOrNull() ?: 0
        val moistureDbl = row.moisture.toDoubleOrNull() ?: 0.0
        val godownCode = state.selectedGodown!!.code
        val contractorCode = state.selectedThekedar!!.accountId   // ✅

        val allMoistures = (allRows.filter { it !== row } + row)
            .mapNotNull { it.moisture.toDoubleOrNull() }
            .filter { it > 0.0 }
        val avgMoisture = if (allMoistures.isNotEmpty()) allMoistures.average() else moistureDbl
        val highMoisture = allMoistures.maxOrNull() ?: moistureDbl

        viewModelScope.launch {
            try {
                val username = MainConstant(context).getUsername() ?: "MOBILE"

                val newCode = repository.insertUnloadingRow(
                    indent = gross.indentNo.ifBlank { gross.indent },
                    wsNo = state.selectedGatePass,
                    vehicleNo = state.selectedVehicle,
                    noOfBags = bagsInt,
                    moisture = moistureDbl,
                    avg = avgMoisture,
                    high = highMoisture,
                    godownCode = godownCode,
                    contractorCode = contractorCode,   // ✅
                    username = username
                )

                if (newCode > 0) onResult(newCode, "Row saved")
                else onResult(-1L, "Failed to save row")

            } catch (e: Exception) {
                Log.e(TAG, "addRowToDb error", e)
                onResult(-1L, e.message ?: "Unknown error")
            }
        }
    }

    fun submitUnloadingEntry(
        gatePass: String,
        godownCode: String,
        finalAvg: Double,
        finalHigh: Double,
        remark: String,
        onResult: (Boolean, String) -> Unit
    ) {
        if (gatePass.isBlank()) { onResult(false, "Gate pass missing"); return }
        if (godownCode.isBlank()) { onResult(false, "Godown code missing"); return }

        val state = _uiState.value
        val gross = state.selectedGrossData
        val vehicleNo = state.selectedVehicle
        val indent = gross?.indentNo?.ifBlank { gross.indent } ?: gatePass

        val thekedarCode = state.selectedThekedar?.accountId ?: ""
        val thekedarName = state.selectedThekedar?.accountName ?: ""

        viewModelScope.launch {
            try {
                val username = MainConstant(context).getUsername() ?: "MOBILE"

                val finalized = repository.finalizeUnloadingRows(
                    indent = indent,
                    wsNo = gatePass,
                    vehicleNo = vehicleNo,
                    finalAvg = finalAvg,
                    finalHigh = finalHigh,
                    rejectCode = null,
                    remark = remark
                )

                if (!finalized) {
                    onResult(false, "Failed to finalize unloading rows")
                    return@launch
                }

                val ok = repository.updateGrossUnloadFlags(
                    gatePass = gatePass,
                    godownCode = godownCode,
                    transpCode = thekedarCode,
                    transpName = thekedarName,
                    username = username,
                    isReject = false
                )

                if (ok) onResult(true, "Unloading submitted successfully")
                else onResult(false, "Failed to update Gross entry")

            } catch (e: Exception) {
                Log.e(TAG, "submitUnloadingEntry error", e)
                onResult(false, e.message ?: "Unknown error")
            }
        }
    }

    fun rejectUnloadingEntry(
        gatePass: String,
        godownCode: String,
        rejectCode: String,
        finalAvg: Double,
        finalHigh: Double,
        remark: String,
        onResult: (Boolean, String) -> Unit
    ) {
        if (gatePass.isBlank()) { onResult(false, "Gate pass missing"); return }
        if (godownCode.isBlank()) { onResult(false, "Godown code missing"); return }

        val state = _uiState.value
        val gross = state.selectedGrossData
        val vehicleNo = state.selectedVehicle
        val indent = gross?.indentNo?.ifBlank { gross.indent } ?: gatePass

        val thekedarCode = state.selectedThekedar?.accountId ?: ""
        val thekedarName = state.selectedThekedar?.accountName ?: ""

        val safeCode = rejectCode.trim().let { if (it.isBlank() || it == "0") "4" else it }

        viewModelScope.launch {
            try {
                val username = MainConstant(context).getUsername() ?: "MOBILE"

                val finalized = repository.finalizeUnloadingRows(
                    indent = indent,
                    wsNo = gatePass,
                    vehicleNo = vehicleNo,
                    finalAvg = finalAvg,
                    finalHigh = finalHigh,
                    rejectCode = safeCode,
                    remark = remark
                )

                if (!finalized) {
                    onResult(false, "Failed to finalize unloading rows")
                    return@launch
                }

                val ok = repository.updateGrossUnloadFlags(
                    gatePass = gatePass,
                    godownCode = godownCode,
                    transpCode = thekedarCode,
                    transpName = thekedarName,
                    username = username,
                    isReject = true
                )

                if (ok) onResult(true, "Unloading rejected")
                else onResult(false, "Failed to reject Gross entry")

            } catch (e: Exception) {
                Log.e(TAG, "rejectUnloadingEntry error", e)
                onResult(false, e.message ?: "Unknown error")
            }
        }
    }

    fun loadRejectList() {
        viewModelScope.launch {
            try {
                val rejects = repository.getRejectList()
                _uiState.update { it.copy(rejectList = rejects) }
                Log.d(TAG, "Loaded ${rejects.size} reject reasons")
            } catch (e: Exception) {
                Log.e(TAG, "Error loading reject list", e)
            }
        }
    }

    fun resetForm() {
        _uiState.update { current ->
            UnloadingUiState(
                vehicleList = current.vehicleList,
                filteredVehicleList = current.vehicleList,
                moistureRejectionValue = current.moistureRejectionValue   // ✅ preserve
            )
        }
        loadVehicles()
        loadMoistureRejectionValue()   // ✅ re-fetch too
    }

    fun updateRowInDb(
        row: UnloadingRow,
        allRows: List<UnloadingRow>,
        onResult: (Boolean, String) -> Unit
    ) {
        val state = _uiState.value
        val gross = state.selectedGrossData

        if (row.uCode <= 0) {
            onResult(false, "Invalid row code")
            return
        }
        if (row.bags.isBlank() || row.moisture.isBlank()) {
            onResult(false, "Please fill Bags and Moisture")
            return
        }

        val bagsInt = row.bags.toIntOrNull() ?: 0
        val moistureDbl = row.moisture.toDoubleOrNull() ?: 0.0

        val allMoistures = (allRows.filter { it.uCode != row.uCode } + row)
            .mapNotNull { it.moisture.toDoubleOrNull() }
            .filter { it > 0.0 }
        val avgMoisture = if (allMoistures.isNotEmpty()) allMoistures.average() else moistureDbl
        val highMoisture = allMoistures.maxOrNull() ?: moistureDbl

        viewModelScope.launch {
            try {
                val ok = repository.updateUnloadingRow(
                    uCode = row.uCode,
                    noOfBags = bagsInt,
                    moisture = moistureDbl,
                    avg = avgMoisture,
                    high = highMoisture
                )
                if (ok) onResult(true, "Row updated")
                else onResult(false, "Failed to update row")
            } catch (e: Exception) {
                Log.e(TAG, "updateRowInDb error", e)
                onResult(false, e.message ?: "Unknown error")
            }
        }
    }
}