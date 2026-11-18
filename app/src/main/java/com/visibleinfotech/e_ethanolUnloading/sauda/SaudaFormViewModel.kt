package com.visibleinfotech.e_ethanolUnloading.sauda

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.visibleinfotech.e_ethanolUnloading.MainConstant
import com.visibleinfotech.e_ethanolUnloading.sauda.models.SaudaItem
import com.visibleinfotech.e_ethanolUnloading.utils.DateTimeUtils
import kotlinx.coroutines.launch

class SaudaFormViewModel(
    application: Application,
    private val repository: SaudaRepository
) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "SaudaFormVM"
    }

    // ✅ Initialized from Application context
    private val mainConstant = MainConstant(application.applicationContext)

    // ===== Raw data =====
    private val _records = MutableLiveData<List<SaudaItem>>(emptyList())
    val records: LiveData<List<SaudaItem>> = _records

    // ===== Filtered data =====
    private val _filteredRecords = MutableLiveData<List<SaudaItem>>(emptyList())
    val filteredRecords: LiveData<List<SaudaItem>> = _filteredRecords

    // ===== Load state =====
    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    // ===== Search =====
    private val _searchQuery = MutableLiveData("")
    val searchQuery: LiveData<String> = _searchQuery

    // ===== Approve state =====
    private val _isApproving = MutableLiveData(false)
    val isApproving: LiveData<Boolean> = _isApproving

    private val _approveResult = MutableLiveData<String?>()
    val approveResult: LiveData<String?> = _approveResult

    // ===== Load =====
    fun loadSaudaList() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                val list = repository.getSaudaList(mainConstant.getUserCode())
                _records.value = list
                applyFilters()
                Log.d(TAG, "Sauda loaded: ${list.size}")
            } catch (e: Exception) {
                Log.e(TAG, "Error loading sauda: ${e.message}", e)
                _error.value = "Failed to load sauda: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun refresh() = loadSaudaList()

    // ===== Search =====
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        applyFilters()
    }

    // ===== Filter logic =====
    private fun applyFilters() {
        val all = _records.value ?: emptyList()
        val query = _searchQuery.value?.trim()?.lowercase().orEmpty()

        _filteredRecords.value = if (query.isBlank()) {
            all
        } else {
            all.filter { r ->
                r.plotId.contains(query, true) ||
                        r.partyName.contains(query, true) ||
                        r.placeName.contains(query, true) ||
                        r.cropType.contains(query, true) ||
                        r.brokerName.contains(query, true)
            }
        }
    }

    // ===== Approve =====
    fun approveSauda(plotId: String) {
        viewModelScope.launch {
            _isApproving.value = true
            _approveResult.value = null

            try {
                val username     = mainConstant.getUsername()
                val approvedDate = DateTimeUtils.getCurrentDateTime()

                Log.d(TAG, "Approve → plotId=$plotId, user=$username, date=$approvedDate")

                val result = repository.approveSauda(
                    plotId       = plotId,
                    approvedBy   = username,
                    approvedDate = approvedDate
                )

                _approveResult.value = result.message

                if (result.success) {
                    loadSaudaList()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Approve error: ${e.message}", e)
                _approveResult.value = "Error: ${e.message}"
            } finally {
                _isApproving.value = false
            }
        }
    }
    fun rejectSauda(plotId: String) {
        viewModelScope.launch {
            _isApproving.value = true
            _approveResult.value = null
            _error.value = null

            try {
                val username     = mainConstant.getUsername()
                val rejectedDate = DateTimeUtils.getCurrentDateTime()

                Log.d(TAG, "Reject → plotId=$plotId, user=$username, date=$rejectedDate")

                val result = repository.rejectSauda(
                    plotId       = plotId,
                    rejectedBy   = username,
                    rejectedDate = rejectedDate
                )

                if (result.isSuccess) {
                    _approveResult.value = "Sauda $plotId rejected successfully"
                    loadSaudaList()
                } else {
                    _error.value = result.exceptionOrNull()?.message ?: "Failed to reject"
                }
            } catch (e: Exception) {
                Log.e(TAG, "Reject error: ${e.message}", e)
                _error.value = "Error: ${e.message}"
            } finally {
                _isApproving.value = false
            }
        }
    }

    fun updateSauda(
        plotId: String,
        rate: String,
        area: String,
        paymentDueDays: String,
        cashDisc: String,
        wtDed: String,
        perBagDed: String,
        onDone: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            _isApproving.value = true
            _error.value = null

            try {
                Log.d(TAG, "Update → plotId=$plotId")
                val result = repository.updateSauda(
                    plotId         = plotId,
                    rate           = rate,
                    area           = area,
                    paymentDueDays = paymentDueDays,
                    cashDisc       = cashDisc,
                    wtDed          = wtDed,
                    perBagDed      = perBagDed
                )

                if (result.isSuccess) {
                    Log.d(TAG, "Update OK — refreshing list")
                    onDone(true, "Sauda $plotId updated")
                    loadSaudaList()   // refresh list so UI shows new values
                } else {
                    val msg = result.exceptionOrNull()?.message ?: "Failed to update"
                    Log.e(TAG, "Update failed: $msg")
                    onDone(false, msg)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Update error: ${e.message}", e)
                onDone(false, "Error: ${e.message}")
            } finally {
                _isApproving.value = false
            }
        }
    }

    fun clearApproveResult() {
        _approveResult.value = null
    }
}

class SaudaFormViewModelFactory(
    private val application: Application,
    private val repository: SaudaRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SaudaFormViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SaudaFormViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}