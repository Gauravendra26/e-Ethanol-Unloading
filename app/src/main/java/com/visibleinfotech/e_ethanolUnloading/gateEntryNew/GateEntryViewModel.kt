package com.visibleinfotech.e_ethanolUnloading.gateEntryNew

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.visibleinfotech.e_ethanolUnloading.MainConstant
import com.visibleinfotech.e_ethanolUnloading.gateEntryNew.models.*
import com.visibleinfotech.e_ethanolUnloading.gateEntryNew.models.GateEntryInsertData
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
        Log.d(TAG, "🚀 ViewModel initialized")
        Log.d(TAG, "📱 Context: ${context.packageName}")
        loadTruckNumbers()
        loadImageApiUrl()
        loadModeMasterList()
    }

    fun loadImageApiUrl() {
        Log.d(TAG, "📸 loadImageApiUrl() called")
        viewModelScope.launch {
            try {
                Log.d(TAG, "🔄 Fetching image API URL from repository...")
                val url = repository.getImageApiUrl()
                _uiState.update { it.copy(imageApiUrl = url) }
                Log.d(TAG, "✅ Image API URL loaded successfully: $url")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error loading image API URL: ${e.message}", e)
            }
        }
    }

    fun loadTruckNumbers() {
        Log.d(TAG, "🚛 loadTruckNumbers() called")
        viewModelScope.launch {
            try {
                Log.d(TAG, "🔄 Fetching truck numbers from repository...")
                _uiState.update { it.copy(isLoading = true) }
                val trucks = repository.getAvailableTruckNumbers()
                _uiState.update { state ->
                    state.copy(
                        truckList = trucks,
                        filteredTruckList = trucks,
                        isLoading = false
                    )
                }
                Log.d(TAG, "✅ Loaded ${trucks.size} truck numbers: ${trucks.take(5)}${if (trucks.size > 5) "..." else ""}")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error loading truck numbers: ${e.message}", e)
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun updateTruckSearchText(text: String) {
        Log.d(TAG, "🔍 updateTruckSearchText() called with: '$text'")
        _uiState.update { state ->
            val filtered = if (text.isBlank()) {
                Log.d(TAG, "📋 Search text blank, showing all ${state.truckList.size} trucks")
                state.truckList
            } else {
                val results = state.truckList.filter { it.contains(text, ignoreCase = true) }
                Log.d(TAG, "📋 Search found ${results.size} matching trucks for '$text'")
                results
            }
            state.copy(
                truckSearchText = text,
                filteredTruckList = filtered
            )
        }
    }

    fun selectTruck(truckNo: String) {
        Log.d(TAG, "🎯 selectTruck() called with: '$truckNo'")
        viewModelScope.launch {
            try {
                Log.d(TAG, "🔄 Loading data for truck: $truckNo")
                _uiState.update { state ->
                    state.copy(
                        selectedTruckNo = truckNo,
                        truckSearchText = truckNo,
                        isLoading = true
                    )
                }

                val truckData = repository.getTruckDispatchData(truckNo)
                Log.d(TAG, "📦 Repository returned truckData: ${if (truckData != null) "✅ NOT NULL" else "❌ NULL"}")

                if (truckData != null) {
                    Log.d(TAG, "📋 Truck data details:")
                    Log.d(TAG, "   👤 Driver: ${truckData.driverName}")
                    Log.d(TAG, "   📱 Mobile: ${truckData.driverMobile}")
                    Log.d(TAG, "   📄 Invoice: ${truckData.invoiceNo}")
                    Log.d(TAG, "   📦 Quantity: ${truckData.quantity}")
                    Log.d(TAG, "   🏢 Party: ${truckData.partyName}")
                    Log.d(TAG, "   🤝 Broker: ${truckData.brokerName}")
                    Log.d(TAG, "   📎 Docs: [${truckData.doc1}, ${truckData.doc2}, ${truckData.doc3}, ${truckData.doc4}, ${truckData.doc5}]")

                    _uiState.update { state ->
                        state.copy(
                            driverName = truckData.driverName.uppercase(),
                            driverMobile = truckData.driverMobile,
                            invoiceNo = truckData.invoiceNo,
                            quantity = truckData.quantity,
                            partyName = truckData.partyName,
                            brokerName = truckData.brokerName,
                            doc1Name = truckData.doc1,
                            doc2Name = truckData.doc2,
                            doc3Name = truckData.doc3,
                            doc4Name = truckData.doc4,
                            doc5Name = truckData.doc5,
                            doc1Available = truckData.doc1.isNotBlank(),
                            doc2Available = truckData.doc2.isNotBlank(),
                            doc3Available = truckData.doc3.isNotBlank(),
                            doc4Available = truckData.doc4.isNotBlank(),
                            doc5Available = truckData.doc5.isNotBlank(),
                            isLoading = false
                        )
                    }
                    Log.d(TAG, "✅ Truck data loaded successfully for: $truckNo")
                } else {
                    Log.w(TAG, "⚠️ No truck data found for: $truckNo")
                    _uiState.update { it.copy(isLoading = false) }
                }

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error loading truck data for $truckNo: ${e.message}", e)
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun clearSelectedTruck() {
        Log.d(TAG, "🧹 clearSelectedTruck() called - Clearing all form data")
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
                selectedModeCode = "",
                selectedModeName = "",
                modeSearchText = ""
            )
        }
        Log.d(TAG, "✅ Form cleared successfully")
    }

    // ==================== MAIN METHOD: UPDATE GATE ENTRY ====================

    fun updateGateEntryWithIndentAndToken(
        onProgress: (Int, String) -> Unit = { _, _ -> },
        onSuccess: (String) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        Log.d(TAG, "🚀🚀🚀 updateGateEntryWithIndentAndToken() called")
        val state = _uiState.value
        Log.d(TAG, "📊 Current UI State:")
        Log.d(TAG, "   Selected Truck: ${state.selectedTruckNo}")
        Log.d(TAG, "   Driver: ${state.driverName}")
        Log.d(TAG, "   Invoice: ${state.invoiceNo}")
        Log.d(TAG, "   Mode Code: ${state.selectedModeCode}")

        if (!isFormValid()) {
            Log.w(TAG, "⚠️ Form validation FAILED")
            _uiState.update { it.copy(showValidationErrors = true) }
            onError("Please fill all required fields")
            Log.d(TAG, "❌ Validation error sent to callback")
            return
        }
        Log.d(TAG, "✅ Form validation PASSED")

        viewModelScope.launch {
            try {
                Log.d(TAG, "🔄 Starting gate entry submission process...")
                _uiState.update { it.copy(isSubmitting = true) }
                onProgress(5, "Preparing...")
                Log.d(TAG, "📊 Progress: 5% - Preparing...")

                Log.d(TAG, "🔍 Fetching dispatch data for truck: ${state.selectedTruckNo}")
                val dispatchData = repository.getDispatchDataForTruck(state.selectedTruckNo)
                if (dispatchData == null) {
                    Log.e(TAG, "❌ No dispatch record found for truck: ${state.selectedTruckNo}")
                    onError("No dispatch record found for this truck")
                    _uiState.update { it.copy(isSubmitting = false) }
                    return@launch
                }

                Log.d(TAG, "✅ Found dispatch data:")
                Log.d(TAG, "   D_Code: ${dispatchData.dCode}")
                Log.d(TAG, "   Party Code: ${dispatchData.partyCode}")
                Log.d(TAG, "   Broker Code: ${dispatchData.brokerCode}")

                val mainConstant = MainConstant(context)
                val username = mainConstant.getUsername() ?: "SYSTEM"
                Log.d(TAG, "👤 User: $username")

                 val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)  // Use US locale for consistency
                val currentDateTime = dateFormat.format(Date())
                Log.d(TAG, "🕐 Current DateTime: $currentDateTime")
                // STEP 1: Update Vipl_ETDispatch
                Log.d(TAG, "📝 STEP 1: Updating Gate Entry...")
                onProgress(15, "Updating gate entry...")

                val updateRequest = GateEntryUpdateRequest(
                    dCode = dispatchData.dCode,
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

                Log.d(TAG, "📦 Update Request:")
                Log.d(TAG, "   D_Code: ${updateRequest.dCode}")
                Log.d(TAG, "   Truck: ${updateRequest.truckNo}")
                Log.d(TAG, "   Doc1 Uploaded: ${updateRequest.doc1Uri != null}")
                Log.d(TAG, "   Doc2 Uploaded: ${updateRequest.doc2Uri != null}")
                Log.d(TAG, "   Doc3 Uploaded: ${updateRequest.doc3Uri != null}")
                Log.d(TAG, "   Doc4 Uploaded: ${updateRequest.doc4Uri != null}")
                Log.d(TAG, "   Doc5 Uploaded: ${updateRequest.doc5Uri != null}")

                val updateResult = repository.updateGateEntryWithImages(
                    updateRequest = updateRequest,
                    onImageProgress = { imageName, progress, status ->
                        val overallProgress = 15 + ((progress.toFloat() / 100f) * 30).toInt()
                        Log.d(TAG, "📸 Image Progress: $imageName - $status ($progress%) -> Overall: $overallProgress%")
                        onProgress(overallProgress, "$imageName: $status")
                    },
                    onImageComplete = { imageName, success ->
                        Log.d(TAG, "📸 Image Complete: $imageName - ${if (success) "✅ SUCCESS" else "❌ FAILED"}")
                    }
                )

                Log.d(TAG, "📊 Update Result: success=${updateResult.success}, message=${updateResult.message}")

                if (!updateResult.success) {
                    Log.e(TAG, "❌ Gate Entry update failed: ${updateResult.message}")
                    onError("Gate Entry update failed: ${updateResult.message}")
                    _uiState.update { it.copy(isSubmitting = false) }
                    return@launch
                }

                Log.d(TAG, "✅ Gate Entry updated successfully")
                onProgress(50, "Gate entry updated!")
                Log.d(TAG, "📊 Progress: 50% - Gate entry updated!")

                // STEP 2: Get updated document names
                Log.d(TAG, "📝 STEP 2: Fetching updated document names...")
                val updatedData = repository.getTruckDispatchData(state.selectedTruckNo)
                Log.d(TAG, "📄 Updated docs: [${updatedData?.doc1}, ${updatedData?.doc2}, ${updatedData?.doc3}, ${updatedData?.doc4}, ${updatedData?.doc5}]")

                // STEP 3: Insert into Vipl_ETGateEntry
                Log.d(TAG, "📝 STEP 3: Inserting Gate Entry record...")
                onProgress(60, "Creating gate entry record...")

                val gateEntryData = GateEntryInsertData(
                    truckNo = state.selectedTruckNo,
                    partyCode = dispatchData.partyCode,
                    brokerCode = dispatchData.brokerCode,
                    dCode = dispatchData.dCode,
                    qty = state.quantity,
                    invoiceNo = state.invoiceNo,
                    invQty = state.invoiceWeight,
                    invAmt = state.invoiceAmount,
                    doc1 = updatedData?.doc1 ?: dispatchData.doc1,
                    doc2 = updatedData?.doc2 ?: dispatchData.doc2,
                    doc3 = updatedData?.doc3 ?: dispatchData.doc3,
                    doc4 = updatedData?.doc4 ?: dispatchData.doc4,
                    doc5 = updatedData?.doc5 ?: dispatchData.doc5,
                    date = currentDateTime,
                    user = username,
                    modeCode = state.selectedModeCode,
                    bags = state.numberOfBags,
                    invRate = state.invoiceRate,
                    remarks = state.remarks
                )

                Log.d(TAG, "📦 GateEntryInsertData:")
                Log.d(TAG, "   Truck: ${gateEntryData.truckNo}")
                Log.d(TAG, "   D_Code: ${gateEntryData.dCode}")
                Log.d(TAG, "   Qty: ${gateEntryData.qty}")
                Log.d(TAG, "   Mode: ${gateEntryData.modeCode}")
                Log.d(TAG, "   Docs: [${gateEntryData.doc1}, ${gateEntryData.doc2}, ${gateEntryData.doc3}, ${gateEntryData.doc4}, ${gateEntryData.doc5}]")

                val insertResult = repository.insertGateEntryRecord(gateEntryData)
                Log.d(TAG, "📊 Insert Result: success=${insertResult.success}, gCode=${insertResult.gCode}, message=${insertResult.message}")

                if (insertResult.success) {
                    Log.d(TAG, "✅✅✅ Gate Entry record inserted successfully! G_Code: ${insertResult.gCode}")
                    onProgress(100, "✅ Done!")
                    Log.d(TAG, "📊 Progress: 100% - ✅ Done!")
                    onSuccess("Gate Entry completed!")
                    Log.d(TAG, "🎉 Success callback triggered")
                    resetForm()
                    Log.d(TAG, "🔄 Form reset after successful submission")
                } else {
                    Log.e(TAG, "❌ Failed to create gate entry record: ${insertResult.message}")
                    onError("Failed to create gate entry record: ${insertResult.message}")
                }

            } catch (e: Exception) {
                Log.e(TAG, "❌❌❌ EXCEPTION in updateGateEntryWithIndentAndToken: ${e.message}", e)
                Log.e(TAG, "Stack trace:", e)
                onError("Error: ${e.message}")
            } finally {
                _uiState.update { it.copy(isSubmitting = false) }
                Log.d(TAG, "🔓 isSubmitting set to false")
            }
        }
    }

    // ==================== DOCUMENT URI UPDATE METHODS ====================

    fun updateDoc1Uri(uri: String) {
        Log.d(TAG, "📎 updateDoc1Uri() called with: '$uri'")
        _uiState.update {
            it.copy(doc1Uri = uri, doc1UserUploaded = uri.isNotBlank())
        }
        Log.d(TAG, "   doc1UserUploaded = ${uri.isNotBlank()}")
    }

    fun updateDoc2Uri(uri: String) {
        Log.d(TAG, "📎 updateDoc2Uri() called with: '$uri'")
        _uiState.update {
            it.copy(doc2Uri = uri, doc2UserUploaded = uri.isNotBlank())
        }
        Log.d(TAG, "   doc2UserUploaded = ${uri.isNotBlank()}")
    }

    fun updateDoc3Uri(uri: String) {
        Log.d(TAG, "📎 updateDoc3Uri() called with: '$uri'")
        _uiState.update {
            it.copy(doc3Uri = uri, doc3UserUploaded = uri.isNotBlank())
        }
        Log.d(TAG, "   doc3UserUploaded = ${uri.isNotBlank()}")
    }

    fun updateDoc4Uri(uri: String) {
        Log.d(TAG, "📎 updateDoc4Uri() called with: '$uri'")
        _uiState.update {
            it.copy(doc4Uri = uri, doc4UserUploaded = uri.isNotBlank())
        }
        Log.d(TAG, "   doc4UserUploaded = ${uri.isNotBlank()}")
    }

    fun updateDoc5Uri(uri: String) {
        Log.d(TAG, "📎 updateDoc5Uri() called with: '$uri'")
        _uiState.update {
            it.copy(doc5Uri = uri, doc5UserUploaded = uri.isNotBlank())
        }
        Log.d(TAG, "   doc5UserUploaded = ${uri.isNotBlank()}")
    }

    // ==================== UI UPDATE METHODS ====================

    fun updateDateTime(dateTime: String) {
        Log.d(TAG, "🕐 updateDateTime() called: $dateTime")
        _uiState.update { it.copy(currentDateTime = dateTime) }
    }

    fun updateDriverName(name: String) {
        Log.d(TAG, "👤 updateDriverName() called: '$name'")
        _uiState.update { it.copy(driverName = name) }
    }

    fun updateDriverMobile(mobile: String) {
        Log.d(TAG, "📱 updateDriverMobile() called: '$mobile' (length: ${mobile.length})")
        _uiState.update { it.copy(driverMobile = mobile) }
    }

    fun updateInvoiceNo(value: String) {
        Log.d(TAG, "📄 updateInvoiceNo() called: '$value'")
        _uiState.update { it.copy(invoiceNo = value) }
    }

    fun updateQuantity(value: String) {
        Log.d(TAG, "📦 updateQuantity() called: '$value'")
        _uiState.update { it.copy(quantity = value) }
    }

    fun updatePartyName(value: String) {
        Log.d(TAG, "🏢 updatePartyName() called: '$value'")
        _uiState.update { it.copy(partyName = value) }
    }

    fun updateBrokerName(value: String) {
        Log.d(TAG, "🤝 updateBrokerName() called: '$value'")
        _uiState.update { it.copy(brokerName = value) }
    }

    fun updateInvoiceWeight(value: String) {
        Log.d(TAG, "⚖️ updateInvoiceWeight() called: '$value'")
        _uiState.update { it.copy(invoiceWeight = value) }
    }

    fun updateNumberOfBags(value: String) {
        Log.d(TAG, "🛍️ updateNumberOfBags() called: '$value'")
        _uiState.update { it.copy(numberOfBags = value) }
    }

    fun updateInvoiceRate(value: String) {
        Log.d(TAG, "💰 updateInvoiceRate() called: '$value'")
        _uiState.update { it.copy(invoiceRate = value) }
    }

    fun updateInvoiceAmount(value: String) {
        Log.d(TAG, "💵 updateInvoiceAmount() called: '$value'")
        _uiState.update { it.copy(invoiceAmount = value) }
    }

    fun updateRemarks(remarks: String) {
        Log.d(TAG, "📝 updateRemarks() called: '$remarks'")
        _uiState.update { it.copy(remarks = remarks) }
    }

    fun setShowValidationErrors(show: Boolean) {
        Log.d(TAG, "⚠️ setShowValidationErrors() called: $show")
        _uiState.update { it.copy(showValidationErrors = show) }
    }

    fun setFullScreenImageUrl(url: String?) {
        Log.d(TAG, "🖼️ setFullScreenImageUrl() called: ${if (url != null) "URL (${url.length} chars)" else "NULL"}")
        _uiState.update { it.copy(fullScreenImageUrl = url) }
    }

    // ==================== MODE MASTER FUNCTIONS ====================

    fun loadModeMasterList() {
        Log.d(TAG, "📋 loadModeMasterList() called")
        viewModelScope.launch {
            try {
                Log.d(TAG, "🔄 Fetching mode master list from repository...")
                val modes = repository.getModeMasterList()
                _uiState.update { state ->
                    state.copy(modeList = modes, filteredModeList = modes)
                }
                Log.d(TAG, "✅ Loaded ${modes.size} modes: ${modes.take(5).joinToString { "${it.code}:${it.name}" }}${if (modes.size > 5) "..." else ""}")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error loading modes: ${e.message}", e)
            }
        }
    }

    fun updateModeSearchText(text: String) {
        Log.d(TAG, "🔍 updateModeSearchText() called with: '$text'")
        _uiState.update { state ->
            val filtered = if (text.isBlank()) {
                Log.d(TAG, "📋 Mode search blank, showing all ${state.modeList.size} modes")
                state.modeList
            } else {
                val results = state.modeList.filter { mode ->
                    mode.name.contains(text, ignoreCase = true) ||
                            mode.code.contains(text, ignoreCase = true)
                }
                Log.d(TAG, "📋 Mode search found ${results.size} matching modes for '$text'")
                results
            }
            state.copy(modeSearchText = text, filteredModeList = filtered)
        }
    }

    fun selectMode(mode: ModeMasterData) {
        Log.d(TAG, "🎯 selectMode() called: code='${mode.code}', name='${mode.name}'")
        _uiState.update { state ->
            state.copy(
                selectedModeCode = mode.code,
                selectedModeName = mode.name,
                modeSearchText = mode.name
            )
        }
        Log.d(TAG, "✅ Mode selected: ${mode.code} - ${mode.name}")
    }

    fun clearSelectedMode() {
        Log.d(TAG, "🧹 clearSelectedMode() called")
        _uiState.update { state ->
            state.copy(selectedModeCode = "", selectedModeName = "", modeSearchText = "")
        }
        Log.d(TAG, "✅ Mode cleared")
    }

    // ==================== FORM VALIDATION ====================

    fun isFormValid(): Boolean {
        val state = _uiState.value
        Log.d(TAG, "🔍 isFormValid() called")

        val basicFieldsValid = state.selectedTruckNo.isNotBlank() &&
                state.driverName.isNotBlank() &&
                state.driverMobile.length == 10 &&
                state.invoiceNo.isNotBlank() &&
                state.quantity.isNotBlank() &&
                state.partyName.isNotBlank() &&
                state.selectedModeCode.isNotBlank()

        Log.d(TAG, "   Basic fields valid: $basicFieldsValid")
        if (!basicFieldsValid) {
            Log.d(TAG, "   ❌ Basic validation failed:")
            Log.d(TAG, "      Truck: ${state.selectedTruckNo.isNotBlank()}")
            Log.d(TAG, "      Driver: ${state.driverName.isNotBlank()}")
            Log.d(TAG, "      Mobile (10 digits): ${state.driverMobile.length == 10}")
            Log.d(TAG, "      Invoice: ${state.invoiceNo.isNotBlank()}")
            Log.d(TAG, "      Quantity: ${state.quantity.isNotBlank()}")
            Log.d(TAG, "      Party: ${state.partyName.isNotBlank()}")
            Log.d(TAG, "      Mode: ${state.selectedModeCode.isNotBlank()}")
            return false
        }

        val imagesValid = listOf(
            state.doc1Available to state.doc1UserUploaded,
            state.doc2Available to state.doc2UserUploaded,
            state.doc3Available to state.doc3UserUploaded,
            state.doc4Available to state.doc4UserUploaded
        ).all { (isAvailable, isUserUploaded) -> isAvailable || isUserUploaded }

        Log.d(TAG, "   Images valid: $imagesValid")
        if (!imagesValid) {
            Log.d(TAG, "   ❌ Image validation failed:")
            Log.d(TAG, "      Doc1: avail=${state.doc1Available}, uploaded=${state.doc1UserUploaded}")
            Log.d(TAG, "      Doc2: avail=${state.doc2Available}, uploaded=${state.doc2UserUploaded}")
            Log.d(TAG, "      Doc3: avail=${state.doc3Available}, uploaded=${state.doc3UserUploaded}")
            Log.d(TAG, "      Doc4: avail=${state.doc4Available}, uploaded=${state.doc4UserUploaded}")
            return false
        }

        Log.d(TAG, "✅ ALL VALIDATION PASSED")
        return true
    }

    fun resetForm() {
        Log.d(TAG, "🔄🔄🔄 resetForm() called")
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
                modeSearchText = ""
            )
        }
        Log.d(TAG, "✅ State reset complete")
        Log.d(TAG, "🔄 Reloading truck numbers and mode list...")
        loadTruckNumbers()
        loadModeMasterList()
        Log.d(TAG, "✅ Reset complete")
    }
}