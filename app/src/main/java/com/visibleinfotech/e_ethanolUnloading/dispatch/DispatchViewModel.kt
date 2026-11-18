package com.visibleinfotech.e_ethanolUnloading.dispatch

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.visibleinfotech.e_ethanolUnloading.dispatch.models.DispatchRequest
import com.visibleinfotech.e_ethanolUnloading.dispatch.models.Party
import com.visibleinfotech.e_ethanolUnloading.dispatch.models.PartyAllocation
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class DispatchViewModel(
    private val repository: DispatchRepository
) : ViewModel() {

    companion object {
        private const val TAG = "DispatchViewModelLog"
    }

    private val _parties = MutableLiveData<List<Party>>(emptyList())
    val parties: LiveData<List<Party>> = _parties

    private val _isLoadingParties = MutableLiveData(false)
    val isLoadingParties: LiveData<Boolean> = _isLoadingParties

    private val _partiesError = MutableLiveData<String?>()
    val partiesError: LiveData<String?> = _partiesError

    private val _selectedParty = MutableLiveData<Party?>()
    val selectedParty: LiveData<Party?> = _selectedParty

    // ===== BROKER SELECTION =====
    private val _brokers = MutableLiveData<List<Party>>(emptyList())
    val brokers: LiveData<List<Party>> = _brokers

    private val _isLoadingBrokers = MutableLiveData(false)
    val isLoadingBrokers: LiveData<Boolean> = _isLoadingBrokers

    private val _selectedBroker = MutableLiveData<Party?>(null)
    val selectedBroker: LiveData<Party?> = _selectedBroker

    private val _showNoBrokerDialog = MutableLiveData(false)
    val showNoBrokerDialog: LiveData<Boolean> = _showNoBrokerDialog

    private val _noBrokerPartyName = MutableLiveData("")
    val noBrokerPartyName: LiveData<String> = _noBrokerPartyName

    // ===== FORM =====
    private val _vehicleNumber = MutableLiveData("")
    val vehicleNumber: LiveData<String> = _vehicleNumber

    private val _driverName = MutableLiveData("")
    val driverName: LiveData<String> = _driverName

    private val _driverMobileNumber = MutableLiveData("")
    val driverMobileNumber: LiveData<String> = _driverMobileNumber

    private val _invoiceNumber = MutableLiveData("")
    val invoiceNumber: LiveData<String> = _invoiceNumber

    private val _dispatchQuantity = MutableLiveData("")
    val dispatchQuantity: LiveData<String> = _dispatchQuantity

    private val _invoiceDate = MutableLiveData("")
    val invoiceDate: LiveData<String> = _invoiceDate

    private val _anugyaNumber = MutableLiveData("")
    val anugyaNumber: LiveData<String> = _anugyaNumber

    // ===== WEIGHT LIMIT =====
    private val _weightLimit = MutableLiveData<Double>(50.0)
    val weightLimit: LiveData<Double> = _weightLimit

    private val _showWeightLimitDialog = MutableLiveData(false)
    val showWeightLimitDialog: LiveData<Boolean> = _showWeightLimitDialog

    private val _weightLimitError = MutableLiveData<String?>()
    val weightLimitError: LiveData<String?> = _weightLimitError

    // ===== SUBMISSION =====
    private val _isSubmitting = MutableLiveData(false)
    val isSubmitting: LiveData<Boolean> = _isSubmitting

    private val _showValidationErrors = MutableLiveData(false)
    val showValidationErrors: LiveData<Boolean> = _showValidationErrors

    // ===== ALLOCATION =====
    private val _partyAllocation = MutableLiveData<PartyAllocation?>()
    val partyAllocation: LiveData<PartyAllocation?> = _partyAllocation

    private val _isLoadingAllocation = MutableLiveData(false)
    val isLoadingAllocation: LiveData<Boolean> = _isLoadingAllocation

    private val _availableQuantity = MutableLiveData<Double?>(null)
    val availableQuantity: LiveData<Double?> = _availableQuantity

    private val _availableQuantity2 = MutableLiveData<Double?>(null)
    val availableQuantity2: LiveData<Double?> = _availableQuantity2

    // ===== TRUCK CHECK =====
    private val _isCheckingTruck = MutableLiveData(false)
    val isCheckingTruck: LiveData<Boolean> = _isCheckingTruck

    private val _showTruckUsedDialog = MutableLiveData(false)
    val showTruckUsedDialog: LiveData<Boolean> = _showTruckUsedDialog

    private val _truckUsedNumber = MutableLiveData("")
    val truckUsedNumber: LiveData<String> = _truckUsedNumber

    // ===== BROKER NOT LINKED (legacy) =====
    private val _showBrokerNotLinkedDialog = MutableLiveData(false)
    val showBrokerNotLinkedDialog: LiveData<Boolean> = _showBrokerNotLinkedDialog

    private val _brokerNotLinkedPartyName = MutableLiveData("")
    val brokerNotLinkedPartyName: LiveData<String> = _brokerNotLinkedPartyName

    // ===== NO SAUDA =====
    private val _showNoSaudaDialog = MutableLiveData(false)
    val showNoSaudaDialog: LiveData<Boolean> = _showNoSaudaDialog

    private val _noSaudaPartyName = MutableLiveData("")
    val noSaudaPartyName: LiveData<String> = _noSaudaPartyName

    // ===== 4-SCENARIO DIALOGS =====
    private val _showInsufficientQtyDialog = MutableLiveData(false)
    val showInsufficientQtyDialog: LiveData<Boolean> = _showInsufficientQtyDialog

    private val _showQtyExceedsApprovedDialog = MutableLiveData(false)
    val showQtyExceedsApprovedDialog: LiveData<Boolean> = _showQtyExceedsApprovedDialog

    private val _showSaudaPendingApprovalDialog = MutableLiveData(false)
    val showSaudaPendingApprovalDialog: LiveData<Boolean> = _showSaudaPendingApprovalDialog

    private val _showQtyExceedsUnapprovedSaudaDialog = MutableLiveData(false)
    val showQtyExceedsUnapprovedSaudaDialog: LiveData<Boolean> = _showQtyExceedsUnapprovedSaudaDialog

    private val _aqM1Value = MutableLiveData(0.0)
    val aqM1Value: LiveData<Double> = _aqM1Value

    private val _aqM2Value = MutableLiveData(0.0)
    val aqM2Value: LiveData<Double> = _aqM2Value

    private val _minQtyValue = MutableLiveData(0.0)
    val minQtyValue: LiveData<Double> = _minQtyValue

    // ===== IMAGE URIs =====
    private val _anugyaImageUri = MutableLiveData<android.net.Uri?>()
    val anugyaImageUri: LiveData<android.net.Uri?> = _anugyaImageUri

    private val _biltyImageUri = MutableLiveData<android.net.Uri?>()
    val biltyImageUri: LiveData<android.net.Uri?> = _biltyImageUri

    private val _invoiceImageUri = MutableLiveData<android.net.Uri?>()
    val invoiceImageUri: LiveData<android.net.Uri?> = _invoiceImageUri

    private val _weighmentSlipImageUri = MutableLiveData<android.net.Uri?>()
    val weighmentSlipImageUri: LiveData<android.net.Uri?> = _weighmentSlipImageUri

    private val _otherDocumentImageUri = MutableLiveData<android.net.Uri?>()
    val otherDocumentImageUri: LiveData<android.net.Uri?> = _otherDocumentImageUri

    private var validationJob: Job? = null

    init {
        Log.d(TAG, "========== DispatchViewModel INITIALIZED ==========")
        loadWeightLimit()
    }

    fun resetAndReload() {
        Log.d(TAG, "🔄 resetAndReload()")
        resetForm()
        loadWeightLimit()
        loadAllParties()
    }

    fun loadAllParties() {
        Log.d(TAG, "📥 loadAllParties()")
        viewModelScope.launch {
            _isLoadingParties.value = true
            _partiesError.value = null

            try {
                val partyList = repository.getAllParties()
                Log.d(TAG, "   ← ${partyList.size} parties returned")
                _parties.value = partyList
                if (partyList.isEmpty()) {
                    _partiesError.value = "No parties found"
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error loading parties: ${e.message}", e)
                _partiesError.value = "Error loading parties: ${e.message}"
            } finally {
                _isLoadingParties.value = false
            }
        }
    }

    fun refreshBrokersForSelectedParty() {
        val party = _selectedParty.value ?: return
        val prevBrokerCode = _selectedBroker.value?.brokerCode
        Log.d(TAG, "🔄 refreshBrokersForSelectedParty | party=${party.accountId} prevBroker=$prevBrokerCode")

        viewModelScope.launch {
            _isLoadingBrokers.value = true
            try {
                val list = repository.getBrokersForParty(party.accountId)
                _brokers.value = list
                // Restore previous broker selection if still present
                if (prevBrokerCode != null) {
                    val stillExists = list.firstOrNull { it.brokerCode == prevBrokerCode }
                    if (stillExists != null) {
                        _selectedBroker.value = stillExists
                    } else {
                        _selectedBroker.value = null
                        _partyAllocation.value = null
                        _availableQuantity.value = null
                        _availableQuantity2.value = null
                        _aqM1Value.value = 0.0
                        _aqM2Value.value = 0.0
                        _minQtyValue.value = 0.0
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ refreshBrokers error: ${e.message}", e)
            } finally {
                _isLoadingBrokers.value = false
            }
        }
    }

    // ============================================================
    //  PARTY SELECTION
    // ============================================================
    fun setSelectedParty(party: Party?) {
        Log.d(TAG, "👤 setSelectedParty() | party=${party?.accountName ?: "<null>"}")
        _selectedParty.value = party

        // Reset downstream state whenever party changes
        _selectedBroker.value = null
        _brokers.value = emptyList()
        _partyAllocation.value = null
        _availableQuantity.value = null
        _availableQuantity2.value = null
        _aqM1Value.value = 0.0
        _aqM2Value.value = 0.0
        _minQtyValue.value = 0.0
        _showBrokerNotLinkedDialog.value = false

        if (party != null) {
            loadBrokersForParty(party.accountId)
        }
        clearValidationIfNeeded()
    }

    fun loadBrokersForParty(partyAccountId: String) {
        Log.d(TAG, "📥 loadBrokersForParty($partyAccountId)")
        viewModelScope.launch {
            _isLoadingBrokers.value = true
            try {
                val list = repository.getBrokersForParty(partyAccountId)
                Log.d(TAG, "   ← ${list.size} brokers")
                _brokers.value = list
                if (list.isEmpty()) {
                    _noBrokerPartyName.value = _selectedParty.value?.accountName ?: ""
                    _showNoBrokerDialog.value = true
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error loading brokers: ${e.message}", e)
                _brokers.value = emptyList()
            } finally {
                _isLoadingBrokers.value = false
            }
        }
    }

    // ============================================================
    //  BROKER SELECTION
    // ============================================================
    fun setSelectedBroker(broker: Party?) {
        Log.d(TAG, "🏦 setSelectedBroker() | broker=${broker?.brokerName ?: "<null>"}")
        _selectedBroker.value = broker

        // Reset allocation + dialog data holders on every broker change
        _partyAllocation.value = null
        _availableQuantity.value = null
        _availableQuantity2.value = null
        _aqM1Value.value = 0.0
        _aqM2Value.value = 0.0
        _minQtyValue.value = 0.0

        if (broker != null && _selectedParty.value != null) {
            val partyId = _selectedParty.value!!.accountId
            val brokerCode = broker.brokerCode.trim()
            if (brokerCode.isNotEmpty() && brokerCode != "0" && brokerCode != "null") {
                loadPartyAllocation(partyId, brokerCode)
            } else {
                _showBrokerNotLinkedDialog.value = true
            }
        }
        clearValidationIfNeeded()
    }

    fun dismissNoBrokerDialog() {
        Log.d(TAG, "🧹 dismissNoBrokerDialog()")
        _showNoBrokerDialog.value = false
        _noBrokerPartyName.value = ""
        _selectedParty.value = null
        _selectedBroker.value = null
        _brokers.value = emptyList()
        _partyAllocation.value = null
        _availableQuantity.value = null
        _availableQuantity2.value = null
        _aqM1Value.value = 0.0
        _aqM2Value.value = 0.0
        _minQtyValue.value = 0.0
    }

    fun dismissBrokerNotLinkedDialog() {
        Log.d(TAG, "🧹 dismissBrokerNotLinkedDialog()")
        _showBrokerNotLinkedDialog.value = false
        _selectedBroker.value = null
        _partyAllocation.value = null
        _availableQuantity.value = null
        _availableQuantity2.value = null
        _aqM1Value.value = 0.0
        _aqM2Value.value = 0.0
        _minQtyValue.value = 0.0
    }

    fun dismissNoSaudaDialog() {
        Log.d(TAG, "🧹 dismissNoSaudaDialog()")
        _showNoSaudaDialog.value = false
        _selectedParty.value = null
        _selectedBroker.value = null
        _brokers.value = emptyList()
        _partyAllocation.value = null
        _availableQuantity.value = null
        _availableQuantity2.value = null
        _noSaudaPartyName.value = ""
    }

    // ============================================================
    //  ALLOCATION LOADING
    // ============================================================
    fun loadPartyAllocation(partyAccountId: String, brokerAccountId: String) {
        Log.d(TAG, "📥 loadPartyAllocation() | party=$partyAccountId broker=$brokerAccountId")
        viewModelScope.launch {
            _isLoadingAllocation.value = true
            try {
                val allocation1 = repository.getPartyAllocation(partyAccountId, brokerAccountId)
                val allocation2 = repository.getPartyAllocation2(partyAccountId, brokerAccountId)

                val aqM1 = allocation1?.availableQuantity ?: 0.0
                val aqM2 = allocation2?.availableQuantity ?: 0.0
                val minQty = _weightLimit.value ?: 50.0

                Log.d(TAG, "   📊 A.Q m1 (approved)   = $aqM1")
                Log.d(TAG, "   📊 A.Q m2 (all status) = $aqM2")
                Log.d(TAG, "   📊 Min Qty (M.Q)       = $minQty")

                _aqM1Value.value = aqM1
                _aqM2Value.value = aqM2
                _minQtyValue.value = minQty

                _partyAllocation.value = allocation1
                _availableQuantity.value = if (aqM1 > 0) aqM1 else null
                _availableQuantity2.value = aqM2

                // ---- Case 0: No sauda at all ----
                if (allocation1 == null && allocation2 == null) {
                    Log.w(TAG, "   ⚠ [Party-Select] No sauda → NoSaudaDialog")
                    _noSaudaPartyName.value = _selectedParty.value?.accountName ?: ""
                    _showNoSaudaDialog.value = true
                    return@launch
                }

                val same = kotlin.math.abs(aqM1 - aqM2) < 0.01
                val lessThanM2 = aqM1 < aqM2

                // ---- Case 1: Only unapproved sauda exists ----
                if (lessThanM2 && aqM1 < minQty) {
                    Log.w(TAG, "   ⚠ Case B1: A.Q m1 < M.Q → SaudaPendingApprovalDialog")
                    _showSaudaPendingApprovalDialog.value = true
                    return@launch
                }

                // ---- Case 2: Approved sauda exists but A.Q m1 < M.Q ----
                if (same && aqM1 < minQty) {
                    Log.w(TAG, "   ⚠ Case A1: A.Q m1 < M.Q → InsufficientQtyDialog")
                    _showInsufficientQtyDialog.value = true
                    return@launch
                }

                Log.d(TAG, "   ✅ [Party-Select] No immediate dialog needed")

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error loading allocation: ${e.message}", e)
                _availableQuantity.value = null
                _availableQuantity2.value = null
            } finally {
                _isLoadingAllocation.value = false
            }
        }
    }

    // ============================================================
    //  QUANTITY VALIDATION
    // ============================================================
    fun validateQuantityAgainstAllocation(enteredQty: Double): Boolean {
        Log.d(TAG, "========== validateQuantityAgainstAllocation() START ==========")
        Log.d(TAG, "   enteredQty = $enteredQty")

        val aqM1 = _availableQuantity.value ?: 0.0
        val aqM2 = _availableQuantity2.value ?: 0.0
        val minQty = _weightLimit.value ?: 50.0

        Log.d(TAG, "   A.Q m1       = $aqM1")
        Log.d(TAG, "   A.Q m2       = $aqM2")
        Log.d(TAG, "   Min Qty (MQ) = $minQty")

        _aqM1Value.value = aqM1
        _aqM2Value.value = aqM2
        _minQtyValue.value = minQty

        val same = kotlin.math.abs(aqM1 - aqM2) < 0.01
        val lessThanM2 = aqM1 < aqM2

        if (same && enteredQty > aqM1) {
            Log.w(TAG, "   → Case A2: entered > A.Q m1")
            viewModelScope.launch {
                delay(2000)
                _showQtyExceedsApprovedDialog.value = true
            }
            return false
        }

        if (lessThanM2 && enteredQty > aqM1) {
            Log.w(TAG, "   → Case B2: entered > A.Q m1 (unapproved sauda)")
            viewModelScope.launch {
                delay(2000)
                _showQtyExceedsUnapprovedSaudaDialog.value = true
            }
            return false
        }

        if (aqM1 < minQty && same) {
            Log.w(TAG, "   → Fallback A1")
            viewModelScope.launch {
                delay(2000)
                _showInsufficientQtyDialog.value = true
            }
            return false
        }

        if (aqM1 < minQty && lessThanM2) {
            Log.w(TAG, "   → Fallback B1")
            viewModelScope.launch {
                delay(2000)
                _showSaudaPendingApprovalDialog.value = true
            }
            return false
        }

        if (aqM1 <= 0.0 && aqM2 <= 0.0) {
            Log.w(TAG, "   → Fallback: no sauda")
            viewModelScope.launch {
                delay(2000)
                _showNoSaudaDialog.value = true
            }
            return false
        }

        Log.d(TAG, "   → ✅ Quantity OK: PASS")
        return true
    }

    fun dismissInsufficientQtyDialog() {
        Log.d(TAG, "🧹 dismissInsufficientQtyDialog()")
        _showInsufficientQtyDialog.value = false
    }

    fun dismissQtyExceedsApprovedDialog() {
        Log.d(TAG, "🧹 dismissQtyExceedsApprovedDialog()")
        _showQtyExceedsApprovedDialog.value = false
    }

    fun dismissSaudaPendingApprovalDialog() {
        Log.d(TAG, "🧹 dismissSaudaPendingApprovalDialog()")
        _showSaudaPendingApprovalDialog.value = false
    }

    fun dismissQtyExceedsUnapprovedSaudaDialog() {
        Log.d(TAG, "🧹 dismissQtyExceedsUnapprovedSaudaDialog()")
        _showQtyExceedsUnapprovedSaudaDialog.value = false
    }

    // ============================================================
    //  FORM UPDATERS
    // ============================================================
    fun updateInvoiceDate(value: String) {
        Log.d(TAG, "✏ updateInvoiceDate('$value')")
        _invoiceDate.value = value
        clearValidationIfNeeded()
    }

    fun updateAnugyaImage(uri: android.net.Uri?) {
        Log.d(TAG, "🖼 updateAnugyaImage(${uri?.toString() ?: "null"})")
        _anugyaImageUri.value = uri
    }

    fun updateBiltyImage(uri: android.net.Uri?) {
        Log.d(TAG, "🖼 updateBiltyImage(${uri?.toString() ?: "null"})")
        _biltyImageUri.value = uri
    }

    fun updateInvoiceImage(uri: android.net.Uri?) {
        Log.d(TAG, "🖼 updateInvoiceImage(${uri?.toString() ?: "null"})")
        _invoiceImageUri.value = uri
    }

    fun updateWeighmentSlipImage(uri: android.net.Uri?) {
        Log.d(TAG, "🖼 updateWeighmentSlipImage(${uri?.toString() ?: "null"})")
        _weighmentSlipImageUri.value = uri
    }

    fun updateOtherDocumentImage(uri: android.net.Uri?) {
        Log.d(TAG, "🖼 updateOtherDocumentImage(${uri?.toString() ?: "null"})")
        _otherDocumentImageUri.value = uri
    }

    fun loadWeightLimit() {
        Log.d(TAG, "📥 loadWeightLimit()")
        viewModelScope.launch {
            try {
                val limit = repository.getWeightLimit()
                Log.d(TAG, "   ✅ Weight limit = $limit")
                _weightLimit.value = limit
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error loading weight limit", e)
                _weightLimit.value = 50.0
            }
        }
    }

    fun updateDispatchQuantity(value: String) {
        Log.d(TAG, "✏ updateDispatchQuantity('$value')")
        if (value.isEmpty() || value.matches(Regex("^\\d*\\.?\\d*$"))) {
            _dispatchQuantity.value = value
            _weightLimitError.value = null
            _showWeightLimitDialog.value = false

            validationJob?.cancel()

            if (value.isNotEmpty()) {
                validationJob = viewModelScope.launch {
                    delay(1000)
                    val currentValue = _dispatchQuantity.value ?: return@launch
                    val quantity = currentValue.toDoubleOrNull()
                    val limit = _weightLimit.value ?: 50.0
                    Log.d(TAG, "   ⏱ Debounced check | qty=$quantity limit=$limit")

                    if (quantity != null && quantity < limit && currentValue.isNotBlank()) {
                        _weightLimitError.value =
                            "Minimum dispatch quantity is ${String.format("%.2f", limit)} Qtls"
                        _showWeightLimitDialog.value = true
                    }
                }
            }
            clearValidationIfNeeded()
        } else {
            Log.w(TAG, "   ⚠ Rejected invalid quantity input: '$value'")
        }
    }

    fun dismissWeightLimitDialog() {
        Log.d(TAG, "🧹 dismissWeightLimitDialog()")
        _showWeightLimitDialog.value = false
    }

    fun checkTruckAvailability(truckNo: String, onResult: (Boolean) -> Unit) {
        Log.d(TAG, "🚚 checkTruckAvailability('$truckNo')")
        if (truckNo.isBlank()) {
            onResult(true)
            return
        }

        viewModelScope.launch {
            _isCheckingTruck.value = true
            try {
                val isInUse = repository.isTruckAlreadyInUse(truckNo)
                if (isInUse) {
                    _truckUsedNumber.value = truckNo
                    _showTruckUsedDialog.value = true
                    onResult(false)
                } else {
                    onResult(true)
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error checking truck: ${e.message}", e)
                onResult(false)
            } finally {
                _isCheckingTruck.value = false
            }
        }
    }

    fun dismissTruckUsedDialog() {
        Log.d(TAG, "🧹 dismissTruckUsedDialog()")
        _showTruckUsedDialog.value = false
    }

    fun clearVehicleNumber() {
        Log.d(TAG, "🧹 clearVehicleNumber()")
        _vehicleNumber.value = ""
        _truckUsedNumber.value = ""
    }

    // ============================================================
    //  FORM VALIDATION
    // ============================================================
    fun validateForm(): Boolean {
        Log.d(TAG, "========== validateForm() START ==========")
        val errors = mutableListOf<String>()

        if (_selectedParty.value == null) {
            errors.add("Please select a Party")
            Log.w(TAG, "   ❌ Party missing")
        }
        if (_selectedBroker.value == null) {
            errors.add("Please select a Broker")
            Log.w(TAG, "   ❌ Broker missing")
        }
        if (_invoiceNumber.value?.isBlank() == true) {
            errors.add("Invoice number is required")
            Log.w(TAG, "   ❌ Invoice blank")
        }
        if (_invoiceDate.value?.isBlank() == true) {
            errors.add("Invoice date is required")
            Log.w(TAG, "   ❌ Invoice date blank")
        }
        val vehicle = _vehicleNumber.value ?: ""
        when {
            vehicle.isBlank() -> {
                errors.add("Vehicle number is required")
                Log.w(TAG, "   ❌ Vehicle blank")
            }
            vehicle.length < 8 -> {
                errors.add("Vehicle number must be at least 8 characters")
                Log.w(TAG, "   ❌ Vehicle too short (${vehicle.length})")
            }
            vehicle.length > 10 -> {
                errors.add("Vehicle number cannot exceed 10 characters")
                Log.w(TAG, "   ❌ Vehicle too long (${vehicle.length})")
            }
        }
        if (_driverName.value?.isBlank() == true) {
            errors.add("Driver name is required")
            Log.w(TAG, "   ❌ Driver name blank")
        }
        val mobile = _driverMobileNumber.value ?: ""
        if (mobile.isBlank()) {
            errors.add("Driver mobile number is required")
            Log.w(TAG, "   ❌ Mobile blank")
        } else if (mobile.length < 10) {
            errors.add("Driver mobile number must be 10 digits")
            Log.w(TAG, "   ❌ Mobile too short (${mobile.length})")
        }

        val quantity = _dispatchQuantity.value?.toDoubleOrNull()
        if (quantity == null || quantity <= 0) {
            errors.add("Please enter a valid dispatch quantity greater than 0")
            Log.w(TAG, "   ❌ Quantity invalid ($quantity)")
        } else {
            val weightLimit = _weightLimit.value ?: 50.0
            if (quantity < weightLimit) {
                errors.add("Minimum dispatch quantity is ${String.format("%.2f", weightLimit)} Qtls")
                Log.w(TAG, "   ❌ Quantity below min ($quantity < $weightLimit)")
            }
        }

        val isValid = errors.isEmpty()
        Log.d(TAG, "   → isValid = $isValid | ${errors.size} errors")
        if (!isValid) {
            _showValidationErrors.value = true
            errors.forEach { Log.w(TAG, "      • $it") }
        }
        Log.d(TAG, "========== validateForm() END ==========")
        return isValid
    }

    // ============================================================
    //  SUBMIT
    // ============================================================
    fun submitDispatch(
        currentDateTime: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        Log.d(TAG, "========== submitDispatch() START ==========")
        viewModelScope.launch {
            _isSubmitting.value = true
            try {
                val party = _selectedParty.value
                val broker = _selectedBroker.value

                if (party == null) {
                    Log.e(TAG, "❌ Party not selected")
                    onError("Party not selected")
                    return@launch
                }
                if (broker == null) {
                    Log.e(TAG, "❌ Broker not selected")
                    onError("Broker not selected")
                    return@launch
                }

                val dispatchRequest = DispatchRequest(
                    partyId              = party.accountId,
                    partyName            = party.accountName,
                    brokerId             = broker.brokerCode,        // ✅ from broker dropdown
                    vehicleNumber        = _vehicleNumber.value ?: "",
                    driverName           = _driverName.value ?: "",
                    driverMobileNumber   = _driverMobileNumber.value ?: "",
                    invoiceNumber        = _invoiceNumber.value ?: "",
                    invoiceDate          = _invoiceDate.value ?: "",
                    dispatchQuantity     = _dispatchQuantity.value?.toDoubleOrNull() ?: 0.0,
                    dateTime             = currentDateTime,
                    anugyaNumber         = _anugyaNumber.value ?: "",
                    anugyaImageUri       = _anugyaImageUri.value?.toString(),
                    biltyImageUri        = _biltyImageUri.value?.toString(),
                    invoiceImageUri      = _invoiceImageUri.value?.toString(),
                    weighmentSlipImageUri = _weighmentSlipImageUri.value?.toString(),
                    otherDocumentImageUri = _otherDocumentImageUri.value?.toString()
                )

                Log.d(TAG, "   Request: $dispatchRequest")

                val result = repository.submitDispatch(dispatchRequest)

                if (result.success) {
                    Log.d(TAG, "   ✅ Submit success → resetForm")
                    onSuccess(result.message)
                    resetForm()
                } else {
                    Log.e(TAG, "   ❌ Submit failed")
                    onError(result.message)
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Exception during submission: ${e.message}", e)
                onError("Error: ${e.message}")
            } finally {
                _isSubmitting.value = false
                Log.d(TAG, "========== submitDispatch() END ==========")
            }
        }
    }

    // ============================================================
    //  RESET
    // ============================================================
    fun resetForm() {
        Log.d(TAG, "🧹 resetForm()")
        _selectedParty.value = null
        _selectedBroker.value = null
        _brokers.value = emptyList()
        _isLoadingBrokers.value = false
        _showNoBrokerDialog.value = false
        _noBrokerPartyName.value = ""

        _vehicleNumber.value = ""
        _driverName.value = ""
        _driverMobileNumber.value = ""
        _invoiceNumber.value = ""
        _dispatchQuantity.value = ""
        _anugyaNumber.value = ""
        _showValidationErrors.value = false
        _weightLimitError.value = null
        _showWeightLimitDialog.value = false
        _showTruckUsedDialog.value = false
        _truckUsedNumber.value = ""
        _isCheckingTruck.value = false
        _partyAllocation.value = null
        _availableQuantity.value = null
        _availableQuantity2.value = null
        _showBrokerNotLinkedDialog.value = false
        _brokerNotLinkedPartyName.value = ""
        _invoiceDate.value = ""
        _anugyaImageUri.value = null
        _biltyImageUri.value = null
        _invoiceImageUri.value = null
        _weighmentSlipImageUri.value = null
        _otherDocumentImageUri.value = null
        _showNoSaudaDialog.value = false
        _noSaudaPartyName.value = ""
        _showInsufficientQtyDialog.value = false
        _showQtyExceedsApprovedDialog.value = false
        _showSaudaPendingApprovalDialog.value = false
        _showQtyExceedsUnapprovedSaudaDialog.value = false
        _aqM1Value.value = 0.0
        _aqM2Value.value = 0.0
        _minQtyValue.value = 0.0
        Log.d(TAG, "   ✅ Form reset")
    }

    // ============================================================
    //  SIMPLE FIELD UPDATERS
    // ============================================================
    fun updateDriverName(value: String) {
        Log.d(TAG, "✏ updateDriverName('$value')")
        _driverName.value = value
        clearValidationIfNeeded()
    }

    fun updateDriverMobileNumber(value: String) {
        val filtered = value.filter { it.isDigit() }.take(10)
        Log.d(TAG, "✏ updateDriverMobileNumber('$value') → '$filtered'")
        _driverMobileNumber.value = filtered
        clearValidationIfNeeded()
    }

    fun updateInvoiceNumber(value: String) {
        Log.d(TAG, "✏ updateInvoiceNumber('$value')")
        _invoiceNumber.value = value
        clearValidationIfNeeded()
    }

    fun updateVehicleNumber(value: String) {
        val filtered = value.uppercase().filter { it.isLetterOrDigit() }.take(10)
        Log.d(TAG, "✏ updateVehicleNumber('$value') → '$filtered'")
        _vehicleNumber.value = filtered
        clearValidationIfNeeded()
    }

    fun updateAnugyaNumber(value: String) {
        Log.d(TAG, "✏ updateAnugyaNumber('$value')")
        _anugyaNumber.value = value
        clearValidationIfNeeded()
    }

    private fun clearValidationIfNeeded() {
        if (_showValidationErrors.value == true) {
            Log.d(TAG, "   🔄 Clearing validation errors flag")
            _showValidationErrors.value = false
        }
    }

    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "========== DispatchViewModel CLEARED ==========")
    }
}