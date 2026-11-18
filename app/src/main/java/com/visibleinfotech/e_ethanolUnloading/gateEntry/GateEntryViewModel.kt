package com.visibleinfotech.e_ethanolUnloading.gateEntry

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.visibleinfotech.e_ethanolUnloading.MainConstant
import com.visibleinfotech.e_ethanolUnloading.gateEntry.models.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val TAG = "GateEntryViewModel"

class GateEntryViewModel(
    private val repository: GateEntryRepository,
    private val context: Context
) : ViewModel() {
    private val _uiState = MutableStateFlow(GateEntryUiState())
    val uiState: StateFlow<GateEntryUiState> = _uiState.asStateFlow()

    init {
        Log.d(TAG, "ViewModel initialized")
        loadTruckNumbers()
        loadImageApiUrl()
        loadModeMasterList()
    }

    fun loadImageApiUrl() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Loading Image API URL...")
                val url = repository.getImageApiUrl()
                _uiState.update { it.copy(imageApiUrl = url) }
                Log.d(TAG, "✅ Image API URL loaded in ViewModel: $url")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error loading image API URL: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    fun loadTruckNumbers() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Loading truck numbers...")
                _uiState.update { it.copy(isLoading = true) }
                val trucks = repository.getAvailableTruckNumbers()
                _uiState.update { state ->
                    state.copy(
                        truckList = trucks,
                        filteredTruckList = trucks,
                        isLoading = false
                    )
                }
                Log.d(TAG, "✅ Loaded ${trucks.size} truck numbers")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error loading truck numbers: ${e.message}")
                e.printStackTrace()
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun updateTruckSearchText(text: String) {
        _uiState.update { state ->
            val filtered = if (text.isBlank()) {
                state.truckList
            } else {
                state.truckList.filter { truckNo ->
                    truckNo.contains(text, ignoreCase = true)
                }
            }
            state.copy(
                truckSearchText = text,
                filteredTruckList = filtered,
                selectedTruckNo = if (text.isBlank()) "" else state.selectedTruckNo
            )
        }
    }

    fun selectTruck(truckNo: String) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Selecting truck: $truckNo")
                _uiState.update { state ->
                    state.copy(
                        selectedTruckNo = truckNo,
                        truckSearchText = truckNo,
                        filteredTruckList = state.truckList,
                        isLoading = true
                    )
                }

                // Load truck dispatch data
                val truckData = repository.getTruckDispatchData(truckNo)

                // Load plot allocation for this truck
                loadTruckPlotAllocation(truckNo)

                if (truckData != null) {
                    Log.d(TAG, "=== TRUCK DATA RECEIVED ===")
                    Log.d(TAG, "Invoice No: ${truckData.invoiceNo}")
                    Log.d(TAG, "Doc1: ${truckData.doc1}")
                    Log.d(TAG, "Doc2: ${truckData.doc2}")
                    Log.d(TAG, "Doc3: ${truckData.doc3}")
                    Log.d(TAG, "Doc4: ${truckData.doc4}")
                    Log.d(TAG, "Doc5: ${truckData.doc5}")
                } else {
                    Log.w(TAG, "No truck data returned for: $truckNo")
                }

                _uiState.update { state ->
                    state.copy(
                        invoiceNo = truckData?.invoiceNo ?: "",
                        quantity = truckData?.quantity ?: "",
                        partyName = truckData?.partyName ?: "",
                        brokerName = truckData?.brokerName ?: "",
                        doc1Name = truckData?.doc1 ?: "",
                        doc2Name = truckData?.doc2 ?: "",
                        doc3Name = truckData?.doc3 ?: "",
                        doc4Name = truckData?.doc4 ?: "",
                        doc5Name = truckData?.doc5 ?: "",
                        doc1Available = truckData?.doc1?.isNotBlank() ?: false,
                        doc2Available = truckData?.doc2?.isNotBlank() ?: false,
                        doc3Available = truckData?.doc3?.isNotBlank() ?: false,
                        doc4Available = truckData?.doc4?.isNotBlank() ?: false,
                        doc5Available = truckData?.doc5?.isNotBlank() ?: false,
                        doc1Uri = "",
                        doc2Uri = "",
                        doc3Uri = "",
                        doc4Uri = "",
                        doc5Uri = "",
                        doc1UserUploaded = false,
                        doc2UserUploaded = false,
                        doc3UserUploaded = false,
                        doc4UserUploaded = false,
                        doc5UserUploaded = false,
                        isLoading = false
                    )
                }

                val state = _uiState.value
                Log.d(TAG, "=== UI STATE UPDATED ===")
                Log.d(TAG, "doc1Name: ${state.doc1Name}, Available: ${state.doc1Available}")
                Log.d(TAG, "doc2Name: ${state.doc2Name}, Available: ${state.doc2Available}")
                Log.d(TAG, "doc3Name: ${state.doc3Name}, Available: ${state.doc3Available}")
                Log.d(TAG, "doc4Name: ${state.doc4Name}, Available: ${state.doc4Available}")
                Log.d(TAG, "doc5Name: ${state.doc5Name}, Available: ${state.doc5Available}")
                Log.d(TAG, "imageApiUrl: ${state.imageApiUrl}")

                // Log plot allocation summary
                state.plotAllocationSummary?.let { allocation ->
                    Log.d(TAG, "=== PLOT ALLOCATION SUMMARY ===")
                    Log.d(TAG, "Total Dispatched: ${allocation.totalDispatched}")
                    Log.d(TAG, "Total Consumed: ${allocation.totalConsumedFromPlots}")
                    Log.d(TAG, "Plot Count: ${allocation.plotCount}")
                }

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error loading truck data: ${e.message}")
                e.printStackTrace()
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun loadTruckPlotAllocation(truckNo: String) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Loading plot allocation for truck: $truckNo")
                _uiState.update { it.copy(isLoadingPlotAllocation = true) }

                val allocation = repository.getTruckPlotAllocation(truckNo)

                _uiState.update { state ->
                    state.copy(
                        plotAllocationSummary = allocation,
                        isLoadingPlotAllocation = false
                    )
                }

                if (allocation != null) {
                    Log.d(TAG, "✅ Plot allocation loaded: ${allocation.plotCount} plots")
                } else {
                    Log.d(TAG, "❌ No plot allocation found for truck: $truckNo")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error loading plot allocation: ${e.message}")
                e.printStackTrace()
                _uiState.update { it.copy(isLoadingPlotAllocation = false) }
            }
        }
    }

    fun clearSelectedTruck() {
        Log.d(TAG, "Clearing selected truck")
        _uiState.update { state ->
            state.copy(
                selectedTruckNo = "",
                truckSearchText = "",
                driverName = "",
                driverMobile = "",
                invoiceNo = "",
                quantity = "",
                partyName = "",
                brokerName = "",
                invoiceWeight = "",
                numberOfBags = "",
                invoiceRate = "",
                invoiceAmount = "",
                doc1Name = "",
                doc2Name = "",
                doc3Name = "",
                doc4Name = "",
                doc5Name = "",
                doc1Available = false,
                doc2Available = false,
                doc3Available = false,
                doc4Available = false,
                doc5Available = false,
                doc1Uri = "",
                doc2Uri = "",
                doc3Uri = "",
                doc4Uri = "",
                doc5Uri = "",
                doc1UserUploaded = false,
                doc2UserUploaded = false,
                doc3UserUploaded = false,
                doc4UserUploaded = false,
                doc5UserUploaded = false,
                remarks = "",
                showValidationErrors = false,
                selectedModeCode = "",
                selectedModeName = "",
                modeSearchText = "",
                plotAllocationSummary = null
            )
        }
    }

// ========== COMBINED METHOD: Update Gate Entry + Insert Indent Records + Insert Token Records ==========

    fun updateGateEntryWithIndentAndToken(
        onProgress: (Int, String) -> Unit = { _, _ -> },
        onSuccess: (String) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val state = _uiState.value

        if (!isFormValid()) {
            _uiState.update { it.copy(showValidationErrors = true) }
            onError("Please fill all required fields and upload required documents")
            return
        }

        if (state.plotAllocationSummary == null || state.plotAllocationSummary.plotDetails.isEmpty()) {
            onError("No plot allocation data available. Please select a truck first.")
            return
        }

        // 👈 VALIDATE RFID TAG
        if (state.rfidTag.isEmpty()) {
            onError("RFID tag is required")
            _uiState.update { it.copy(isSubmitting = false) }
            return
        }

        if (state.rfidTag.length < 8) {
            onError("RFID tag must be at least 8 characters")
            _uiState.update { it.copy(isSubmitting = false) }
            return
        }

        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isSubmitting = true) }
                onProgress(5, "Preparing update...")

                val dCode = repository.getDispatchCodeForTruck(state.selectedTruckNo)
                if (dCode.isNullOrEmpty()) {
                    onError("No dispatch record found for this truck")
                    _uiState.update { it.copy(isSubmitting = false) }
                    return@launch
                }

                val mainConstant = MainConstant(context)
                val username = mainConstant.getUsername() ?: "SYSTEM"
                val dateFormat = SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault())
                val currentDateTime = dateFormat.format(Date())

                onProgress(10, "Updating gate entry...")
                val updateRequest = GateEntryUpdateRequest(
                    dCode = dCode,
                    truckNo = state.selectedTruckNo,
                    driverName = state.driverName,
                    driverMobile = state.driverMobile,
                    invoiceNo = state.invoiceNo,
                    quantity = state.quantity,
                    partyName = state.partyName,
                    brokerName = state.brokerName,
                    modeCode = state.selectedModeCode,
                    invoiceWeight = state.invoiceWeight,
                    numberOfBags = state.numberOfBags,
                    invoiceRate = state.invoiceRate,
                    invoiceAmount = state.invoiceAmount,
                    remarks = state.remarks,
                    doc1Uri = if (state.doc1UserUploaded) state.doc1Uri else null,
                    doc2Uri = if (state.doc2UserUploaded) state.doc2Uri else null,
                    doc3Uri = if (state.doc3UserUploaded) state.doc3Uri else null,
                    doc4Uri = if (state.doc4UserUploaded) state.doc4Uri else null,
                    doc5Uri = if (state.doc5UserUploaded) state.doc5Uri else null,
                    updateBy = username,
                    updateDate = currentDateTime
                )

                val updateResult = repository.updateGateEntryWithImages(
                    updateRequest = updateRequest,
                    onImageProgress = { imageName, progress, status ->
                        val overallProgress = 10 + ((progress.toFloat() / 100f) * 20).toInt()
                        onProgress(overallProgress, "Gate entry: $imageName - $status")
                    },
                    onImageComplete = { imageName, success ->
                        Log.d(TAG, "Image $imageName upload complete: $success")
                    }
                )

                if (!updateResult.success) {
                    onError("Gate Entry update failed: ${updateResult.message}")
                    _uiState.update { it.copy(isSubmitting = false) }
                    return@launch
                }

                Log.d(TAG, "✅ Gate Entry updated successfully!")

                // STEP 2: Insert Indent Records
                val plotCount = state.plotAllocationSummary.plotDetails.size
                onProgress(30, "Inserting $plotCount indent records...")

                val indentResult = repository.insertIndentRecords(
                    truckNo = state.selectedTruckNo,
                    driverName = state.driverName,
                    driverMobile = state.driverMobile,
                    invoiceNo = state.invoiceNo,
                    invoiceWeight = state.invoiceWeight,
                    numberOfBags = state.numberOfBags,
                    invoiceRate = state.invoiceRate,
                    invoiceAmount = state.invoiceAmount,
                    remarks = state.remarks,
                    modeCode = state.selectedModeCode,
                    modeName = state.selectedModeName,
                    createdBy = username,
                    plotAllocations = state.plotAllocationSummary.plotDetails
                )

                if (!indentResult.success) {
                    onError("Indent insertion failed: ${indentResult.message}")
                    _uiState.update { it.copy(isSubmitting = false) }
                    return@launch
                }

                Log.d(TAG, "✅ Indent records inserted successfully!")
                Log.d(TAG, "Indent Numbers: ${indentResult.indentNumbers}")

                // STEP 3: Insert Token Records with indent numbers and RFID tag
                onProgress(60, "Inserting $plotCount token records...")

                val tokenResult = repository.insertTokenRecords(
                    truckNo = state.selectedTruckNo,
                    driverName = state.driverName,
                    driverMobile = state.driverMobile,
                    invoiceNo = state.invoiceNo,
                    invoiceWeight = state.invoiceWeight,
                    numberOfBags = state.numberOfBags,
                    invoiceRate = state.invoiceRate,
                    invoiceAmount = state.invoiceAmount,
                    remarks = state.remarks,
                    modeCode = state.selectedModeCode,
                    modeName = state.selectedModeName,
                    createdBy = username,
                    plotAllocations = state.plotAllocationSummary.plotDetails,
                    indentNumbers = indentResult.indentNumbers,
                    rfidTag = state.rfidTag  // 👈 PASS THE RFID TAG
                )

                if (tokenResult.success) {
                    Log.d(TAG, "✅ All $plotCount token records inserted successfully!")
                    onProgress(100, "Done!")
                    val successMessage = "Gate Entry updated, $plotCount indent records and $plotCount token records created successfully!"
                    onSuccess(successMessage)
                    resetForm()
                } else {
                    Log.e(TAG, "❌ Token insertion completed with errors: ${tokenResult.message}")
                    onError("Indent records inserted but token insertion failed: ${tokenResult.message}")
                }

            } catch (e: Exception) {
                Log.e(TAG, "❌ Exception during update: ${e.message}", e)
                onError("Error: ${e.message}")
            } finally {
                _uiState.update { it.copy(isSubmitting = false) }
            }
        }
    }
    fun updateDoc1Uri(uri: String) {
        Log.d(TAG, "updateDoc1Uri: $uri")
        _uiState.update { it.copy(
            doc1Uri = uri,
            doc1UserUploaded = uri.isNotBlank()
        )}
    }

    fun updateDoc2Uri(uri: String) {
        Log.d(TAG, "updateDoc2Uri: $uri")
        _uiState.update { it.copy(
            doc2Uri = uri,
            doc2UserUploaded = uri.isNotBlank()
        )}
    }

    fun updateDoc3Uri(uri: String) {
        Log.d(TAG, "updateDoc3Uri: $uri")
        _uiState.update { it.copy(
            doc3Uri = uri,
            doc3UserUploaded = uri.isNotBlank()
        )}
    }

    fun updateDoc4Uri(uri: String) {
        Log.d(TAG, "updateDoc4Uri: $uri")
        _uiState.update { it.copy(
            doc4Uri = uri,
            doc4UserUploaded = uri.isNotBlank()
        )}
    }

    fun updateDoc5Uri(uri: String) {
        Log.d(TAG, "updateDoc5Uri: $uri")
        _uiState.update { it.copy(
            doc5Uri = uri,
            doc5UserUploaded = uri.isNotBlank()
        )}
    }

    // ========== FIELD UPDATE METHODS ==========

    fun updateDriverName(name: String) {
        _uiState.update { it.copy(driverName = name) }
    }

    fun updateDriverMobile(mobile: String) {
        _uiState.update { it.copy(driverMobile = mobile) }
    }

    fun updateInvoiceNo(value: String) {
        _uiState.update { it.copy(invoiceNo = value) }
    }

    fun updateQuantity(value: String) {
        _uiState.update { it.copy(quantity = value) }
    }

    fun updatePartyName(value: String) {
        _uiState.update { it.copy(partyName = value) }
    }

    fun updateBrokerName(value: String) {
        _uiState.update { it.copy(brokerName = value) }
    }

    fun updateInvoiceWeight(value: String) {
        _uiState.update { it.copy(invoiceWeight = value) }
    }

    fun updateNumberOfBags(value: String) {
        _uiState.update { it.copy(numberOfBags = value) }
    }

    fun updateInvoiceRate(value: String) {
        _uiState.update { it.copy(invoiceRate = value) }
    }

    fun updateInvoiceAmount(value: String) {
        _uiState.update { it.copy(invoiceAmount = value) }
    }

    fun updateRemarks(remarks: String) {
        _uiState.update { it.copy(remarks = remarks) }
    }

    fun updateDateTime(dateTime: String) {
        _uiState.update { it.copy(currentDateTime = dateTime) }
    }

    fun setShowValidationErrors(show: Boolean) {
        _uiState.update { it.copy(showValidationErrors = show) }
    }

    fun setFullScreenImageUrl(url: String?) {
        Log.d(TAG, "Setting full screen image URL: $url")
        _uiState.update { it.copy(fullScreenImageUrl = url) }
    }

    // ========== MODE MASTER FUNCTIONS ==========

    fun loadModeMasterList() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Loading mode master list...")
                val modes = repository.getModeMasterList()
                _uiState.update { state ->
                    state.copy(
                        modeList = modes,
                        filteredModeList = modes
                    )
                }
                Log.d(TAG, "✅ Loaded ${modes.size} modes")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error loading mode master list: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    fun updateModeSearchText(text: String) {
        _uiState.update { state ->
            val filtered = if (text.isBlank()) {
                state.modeList
            } else {
                state.modeList.filter { mode ->
                    mode.name.contains(text, ignoreCase = true) ||
                            mode.code.contains(text, ignoreCase = true)
                }
            }
            state.copy(
                modeSearchText = text,
                filteredModeList = filtered
            )
        }
    }

    fun selectMode(mode: ModeMasterData) {
        Log.d(TAG, "Selecting mode: ${mode.code} - ${mode.name}")
        _uiState.update { state ->
            state.copy(
                selectedModeCode = mode.code,
                selectedModeName = mode.name,
                modeSearchText = mode.name
            )
        }
    }

    fun clearSelectedMode() {
        Log.d(TAG, "Clearing selected mode")
        _uiState.update { state ->
            state.copy(
                selectedModeCode = "",
                selectedModeName = "",
                modeSearchText = ""
            )
        }
    }

    fun isFormValid(): Boolean {
        val state = _uiState.value

        val basicFieldsValid = state.selectedTruckNo.isNotBlank() &&
                state.driverName.isNotBlank() &&
                state.driverMobile.length == 10 &&
                state.invoiceNo.isNotBlank() &&
                state.quantity.isNotBlank() &&
                state.partyName.isNotBlank() &&
                state.selectedModeCode.isNotBlank()

        if (!basicFieldsValid) {
            Log.d(TAG, "Form validation failed: basic fields")
            return false
        }

        val imagesValid = listOf(
            state.doc1Available to state.doc1UserUploaded,
            state.doc2Available to state.doc2UserUploaded,
            state.doc3Available to state.doc3UserUploaded,
            state.doc4Available to state.doc4UserUploaded
        ).all { (isAvailable, isUserUploaded) ->
            isAvailable || isUserUploaded
        }

        if (!imagesValid) {
            Log.d(TAG, "Form validation failed: required images missing")
            return false
        }

        // Check if plot allocation exists
        if (state.plotAllocationSummary == null || state.plotAllocationSummary.plotDetails.isEmpty()) {
            Log.d(TAG, "Form validation failed: no plot allocation data")
            return false
        }

        Log.d(TAG, "Form validation: SUCCESS")
        return true
    }
    fun updateRfidTag(tag: String) {
        _uiState.value = _uiState.value.copy(rfidTag = tag)
    }
    fun resetForm() {
        Log.d(TAG, "Resetting form")
        _uiState.update {
            it.copy(
                selectedTruckNo = "",
                truckSearchText = "",
                driverName = "",
                driverMobile = "",
                invoiceNo = "",
                quantity = "",
                partyName = "",
                brokerName = "",
                invoiceWeight = "",
                numberOfBags = "",
                invoiceRate = "",
                invoiceAmount = "",
                remarks = "",
                doc1Name = "",
                doc2Name = "",
                doc3Name = "",
                doc4Name = "",
                doc5Name = "",
                doc1Available = false,
                doc2Available = false,
                doc3Available = false,
                doc4Available = false,
                doc5Available = false,
                doc1Uri = "",
                doc2Uri = "",
                doc3Uri = "",
                doc4Uri = "",
                doc5Uri = "",
                doc1UserUploaded = false,
                doc2UserUploaded = false,
                doc3UserUploaded = false,
                doc4UserUploaded = false,
                doc5UserUploaded = false,
                showValidationErrors = false,
                isSubmitting = false,
                isLoading = false,
                fullScreenImageUrl = null,
                selectedModeCode = "",
                selectedModeName = "",
                modeSearchText = "",
                plotAllocationSummary = null
            )
        }
        loadTruckNumbers()
        loadModeMasterList()
    }
}