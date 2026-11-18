package com.visibleinfotech.e_ethanolUnloading.gateEntryRecord


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.visibleinfotech.e_ethanolUnloading.gateEntryRecord.models.GateEntryRecordModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class GateEntryRecordViewModel(
    private val repository: GateEntryRecordRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(GateEntryRecordUiState())
    val uiState: StateFlow<GateEntryRecordUiState> = _uiState.asStateFlow()

    val isLoading = MutableStateFlow(false)

    init {
        loadGateEntryRecords()
    }

    fun loadGateEntryRecords() {
        viewModelScope.launch {
            try {
                isLoading.value = true
                val records = repository.getGateEntryRecords()
                _uiState.update { state ->
                    state.copy(
                        allRecords = records,
                        filteredRecords = records,
                        isLoading = false
                    )
                }
                updateCounts()
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false) }
            } finally {
                isLoading.value = false
            }
        }
    }

    fun searchGateEntryRecords(query: String) {
        _uiState.update { state ->
            state.copy(searchQuery = query)
        }
        applyFilters()
    }

    fun filterGateEntryRecords(
        fromDate: String?,
        toDate: String?,
        status: GateEntryStatus?
    ) {
        _uiState.update { state ->
            state.copy(
                filterFromDate = fromDate,
                filterToDate = toDate,
                filterStatus = status
            )
        }
        applyFilters()
    }

    private fun applyFilters() {
        val state = _uiState.value
        var filtered = state.allRecords

        // Apply search
        if (state.searchQuery.isNotEmpty()) {
            val query = state.searchQuery.lowercase()
            filtered = filtered.filter { record ->
                record.truckNo.lowercase().contains(query) ||
                        record.partyName.lowercase().contains(query) ||
                        record.invoiceNo.lowercase().contains(query) ||
                        record.driverName.lowercase().contains(query)
            }
        }

        // Apply date range
        if (!state.filterFromDate.isNullOrEmpty()) {
            filtered = filtered.filter { it.date >= state.filterFromDate }
        }
        if (!state.filterToDate.isNullOrEmpty()) {
            filtered = filtered.filter { it.date <= state.filterToDate }
        }

        // Apply status
        when (state.filterStatus) {
            GateEntryStatus.COMPLETED -> filtered = filtered.filter { it.status == "Y" }
            GateEntryStatus.PENDING -> filtered = filtered.filter { it.status != "Y" }
            null -> { /* No filter */ }
        }

        _uiState.update { state ->
            state.copy(filteredRecords = filtered)
        }
        updateCounts()
    }

    private fun updateCounts() {
        val records = _uiState.value.filteredRecords
        _uiState.update { state ->
            state.copy(
                completedCount = records.count { it.status == "Y" },
                pendingCount = records.count { it.status != "Y" }
            )
        }
    }
}

data class GateEntryRecordUiState(
    val allRecords: List<GateEntryRecordModel> = emptyList(),
    val filteredRecords: List<GateEntryRecordModel> = emptyList(),
    val searchQuery: String = "",
    val filterFromDate: String? = null,
    val filterToDate: String? = null,
    val filterStatus: GateEntryStatus? = null,
    val isLoading: Boolean = false,
    val completedCount: Int = 0,
    val pendingCount: Int = 0
)