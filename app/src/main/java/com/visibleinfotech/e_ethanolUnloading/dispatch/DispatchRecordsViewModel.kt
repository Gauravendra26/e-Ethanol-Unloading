package com.visibleinfotech.e_ethanolUnloading.dispatch

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.visibleinfotech.e_ethanolUnloading.dispatch.models.DispatchRecord
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

class DispatchRecordsViewModel(
    private val repository: DispatchRepository
) : ViewModel() {

    companion object {
        private const val TAG = "DispatchRecordsVM"
    }

    // ===== Raw data from DB =====
    private val _records = MutableLiveData<List<DispatchRecord>>(emptyList())
    val records: LiveData<List<DispatchRecord>> = _records

    // ===== Filtered data shown in UI =====
    private val _filteredRecords = MutableLiveData<List<DispatchRecord>>(emptyList())
    val filteredRecords: LiveData<List<DispatchRecord>> = _filteredRecords

    // ===== Load state =====
    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    // ===== Search =====
    private val _searchQuery = MutableLiveData("")
    val searchQuery: LiveData<String> = _searchQuery

    // ===== Date filter =====
    private val _fromDate = MutableLiveData("")   // yyyy-MM-dd
    val fromDate: LiveData<String> = _fromDate

    private val _toDate = MutableLiveData("")     // yyyy-MM-dd
    val toDate: LiveData<String> = _toDate

    // ===== Aggregates =====
    private val _totalRecords = MutableLiveData(0)
    val totalRecords: LiveData<Int> = _totalRecords

    private val _totalQuantity = MutableLiveData(0.0)
    val totalQuantity: LiveData<Double> = _totalQuantity

    // ===== Load =====
    fun loadRecords() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                val list = repository.getDispatchRecords()
                _records.value = list
                applyFilters()
                Log.d(TAG, "Records loaded: ${list.size}")
            } catch (e: Exception) {
                Log.e(TAG, "Error loading records: ${e.message}", e)
                _error.value = "Failed to load records: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun refresh() = loadRecords()

    // ===== Search =====
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        applyFilters()
    }

    // ===== Date filter =====
    fun applyDateFilter(from: String?, to: String?) {
        _fromDate.value = from.orEmpty()
        _toDate.value = to.orEmpty()
        applyFilters()
    }

    fun clearFilters() {
        _fromDate.value = ""
        _toDate.value = ""
        applyFilters()
    }

    // ===== Core filter logic =====
    private fun applyFilters() {
        val all = _records.value ?: emptyList()
        val query = _searchQuery.value?.trim()?.lowercase().orEmpty()
        val from = _fromDate.value.orEmpty()
        val to = _toDate.value.orEmpty()

        val result = all.filter { r ->
            // ---- Search match ----
            val matchesSearch = query.isBlank() ||
                    r.dCode.contains(query, true) ||
                    r.truckNo.contains(query, true) ||
                    r.partyName.contains(query, true) ||
                    r.invoiceNo.contains(query, true) ||
                    r.driverName.contains(query, true) ||
                    r.driverMobile.contains(query, true) ||
                    r.userName.contains(query, true)

            // ---- Date match ----
            val matchesDate = matchesDateRange(r.date, from, to)

            matchesSearch && matchesDate
        }

        _filteredRecords.value = result
        _totalRecords.value = result.size
        _totalQuantity.value = result.sumOf { it.quantity.toDoubleOrNull() ?: 0.0 }
    }

    /**
     * `recordDate` is expected to look like "yyyy-MM-dd HH:mm:ss" (from D_Date column).
     * We compare only the date portion using plain string compare — safe because
     * yyyy-MM-dd sorts lexicographically.
     */
    private fun matchesDateRange(recordDate: String, from: String, to: String): Boolean {
        if (from.isBlank() && to.isBlank()) return true

        val dateOnly = try {
            val input = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val output = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            input.parse(recordDate)?.let { output.format(it) } ?: recordDate.take(10)
        } catch (e: Exception) {
            recordDate.take(10)  // fallback: first 10 chars "yyyy-MM-dd"
        }

        return when {
            from.isNotBlank() && to.isNotBlank() -> dateOnly in from..to
            from.isNotBlank() -> dateOnly >= from
            to.isNotBlank() -> dateOnly <= to
            else -> true
        }
    }
}

class DispatchRecordsViewModelFactory(
    private val repository: DispatchRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DispatchRecordsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DispatchRecordsViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}