package com.visibleinfotech.e_ethanolUnloading.sauda

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class SaudaRecordsViewModel(
    private val repository: SaudaRecordsRepository
) : ViewModel() {

    companion object { private const val TAG = "SaudaRecordsVM" }

    private val _records = MutableLiveData<List<SaudaRecord>>(emptyList())

    private val _filteredRecords = MutableLiveData<List<SaudaRecord>>(emptyList())
    val filteredRecords: LiveData<List<SaudaRecord>> = _filteredRecords

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _searchQuery = MutableLiveData("")
    val searchQuery: LiveData<String> = _searchQuery

    private val _totalRecords = MutableLiveData(0)
    val totalRecords: LiveData<Int> = _totalRecords

    private val _totalQuantity = MutableLiveData(0.0)
    val totalQuantity: LiveData<Double> = _totalQuantity

    private var currentFrom: String? = null
    private var currentTo: String? = null

    fun loadRecords() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val list = repository.getApprovedRecords(currentFrom, currentTo)
                _records.value = list
                applyFilters()
                Log.d(TAG, "Loaded ${list.size} records")
            } catch (e: Exception) {
                Log.e(TAG, "loadRecords error: ${e.message}", e)
                _error.value = "Failed to load records: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateSearchQuery(q: String) {
        _searchQuery.value = q
        applyFilters()
    }

    fun applyDateFilter(fromDate: String?, toDate: String?) {
        currentFrom = fromDate
        currentTo = toDate
        loadRecords()
    }

    fun clearFilters() {
        currentFrom = null
        currentTo = null
        loadRecords()
    }

    private fun applyFilters() {
        val all = _records.value ?: emptyList()
        val q = _searchQuery.value?.trim()?.lowercase().orEmpty()

        val filtered = if (q.isBlank()) all else all.filter {
            it.plotId.contains(q, true) ||
                    it.partyName.contains(q, true) ||
                    it.placeName.contains(q, true) ||
                    it.cropType.contains(q, true) ||
                    it.brokerName.contains(q, true) ||
                    it.approvedBy.contains(q, true)
        }

        _filteredRecords.value = filtered
        _totalRecords.value = filtered.size
        _totalQuantity.value = filtered.sumOf { it.area.toDoubleOrNull() ?: 0.0 }
    }
}

class SaudaRecordsViewModelFactory(
    private val repository: SaudaRecordsRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SaudaRecordsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SaudaRecordsViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}