package com.visibleinfotech.e_ethanolUnloading.unloadRecord

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.visibleinfotech.e_ethanolUnloading.unloadRecord.models.UnloadStatus
import com.visibleinfotech.e_ethanolUnloading.unloadRecord.models.UnloadingDetailModel
import com.visibleinfotech.e_ethanolUnloading.unloadRecord.models.UnloadingRecordModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

private const val TAG = "UnloadingRecordVM"

enum class UnloadApprovalStatus { APPROVED, PENDING, REJECTED }

data class UnloadingRecordUiState(
    val isLoading: Boolean = false,
    val allRecords: List<UnloadingRecordModel> = emptyList(),
    val filteredRecords: List<UnloadingRecordModel> = emptyList(),
    val searchQuery: String = "",
    val errorMessage: String = "",

    val showDetailDialog: Boolean = false,
    val selectedRecord: UnloadingRecordModel? = null,
    val detailRows: List<UnloadingDetailModel> = emptyList(),
    val isLoadingDetail: Boolean = false,
    val isDetailRejected: Boolean = false
) {
    val approvedCount: Int
        get() = filteredRecords.count { it.status == UnloadStatus.UNLOADED }

    val rejectedCount: Int
        get() = filteredRecords.count { it.status == UnloadStatus.REJECTED }

    val pendingCount: Int
        get() = filteredRecords.count { it.status == UnloadStatus.TEMP_REJECTED }
}

class UnloadingRecordViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = UnloadingRecordRepository(app)

    private val _uiState = MutableStateFlow(UnloadingRecordUiState())
    val uiState: StateFlow<UnloadingRecordUiState> = _uiState.asStateFlow()

    private var currentFromDate: String? = null
    private var currentToDate: String? = null
    private var currentStatus: UnloadApprovalStatus? = null

    init {
        Log.d(TAG, "ViewModel initialized – loading records")
        loadUnloadingRecords()
    }

    fun loadUnloadingRecords() {
        Log.d(TAG, "loadUnloadingRecords() called")
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, errorMessage = "") }
                Log.d(TAG, "Fetching records from repository…")

                val records = repository.getUnloadingRecords()
                Log.d(TAG, "Repository returned ${records.size} records")

                _uiState.update {
                    it.copy(allRecords = records, filteredRecords = records, isLoading = false)
                }
                Log.d(TAG, "UI state updated with all records")

                applyFiltersInternal()
            } catch (e: Exception) {
                Log.e(TAG, "loadUnloadingRecords error", e)
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = e.message ?: "Failed to load records")
                }
            }
        }
    }

    fun searchUnloadingRecords(query: String) {
        Log.d(TAG, "searchUnloadingRecords() query='$query'")
        _uiState.update { it.copy(searchQuery = query) }
        applyFiltersInternal()
    }

    fun filterUnloadingRecords(
        fromDate: String?,
        toDate: String?,
        status: UnloadApprovalStatus?
    ) {
        Log.d(TAG, "filterUnloadingRecords() from=$fromDate to=$toDate status=$status")
        currentFromDate = fromDate
        currentToDate = toDate
        currentStatus = status
        applyFiltersInternal()
    }

    private fun applyFiltersInternal() {
        val state = _uiState.value
        val q = state.searchQuery.trim()
        val fromMs = currentFromDate?.let { parseDateToMillis(it) }
        val toMs   = currentToDate?.let { parseDateToMillis(it) }

        Log.d(
            TAG,
            "applyFiltersInternal() – query='$q' fromMs=$fromMs toMs=$toMs status=$currentStatus"
        )

        val filtered = state.allRecords.filter { rec ->
            val matchesSearch = q.isEmpty() ||
                    rec.vehicleNo.contains(q, true) ||
                    rec.wsNo.contains(q, true) ||
                    rec.partyName.contains(q, true) ||
                    rec.thekedarName.contains(q, true) ||
                    rec.godownName.contains(q, true) ||
                    rec.brokerName.contains(q, true)

            val matchesStatus = when (currentStatus) {
                UnloadApprovalStatus.APPROVED -> rec.status == UnloadStatus.UNLOADED
                UnloadApprovalStatus.PENDING  -> rec.status == UnloadStatus.TEMP_REJECTED
                UnloadApprovalStatus.REJECTED -> rec.status == UnloadStatus.REJECTED
                null -> true
            }

            val recMs = parseDateToMillis(rec.unloadDate)
            val matchesFrom = fromMs == null || (recMs != null && recMs >= fromMs)
            val matchesTo   = toMs   == null || (recMs != null && recMs <= toMs)

            val keep = matchesSearch && matchesStatus && matchesFrom && matchesTo
            if (!keep) {
                Log.v(
                    TAG,
                    "Filtered out: ws=${rec.wsNo} vehicle=${rec.vehicleNo} " +
                            "search=$matchesSearch status=$matchesStatus " +
                            "from=$matchesFrom to=$matchesTo"
                )
            }
            keep
        }

        Log.d(TAG, "Filtered result: ${filtered.size} of ${state.allRecords.size}")
        _uiState.update { it.copy(filteredRecords = filtered) }
    }

    private fun parseDateToMillis(s: String?): Long? {
        if (s.isNullOrBlank()) return null
        return try {
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).parse(s)?.time
        } catch (e: Exception) {
            Log.w(TAG, "parseDateToMillis: failed format1 for '$s' – trying format2", e)
            try {
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(s)?.time
            } catch (e2: Exception) {
                Log.w(TAG, "parseDateToMillis: failed both formats for '$s'", e2)
                null
            }
        }
    }

    fun openDetailDialog(record: UnloadingRecordModel) {
        Log.d(
            TAG,
            "openDetailDialog() ws=${record.wsNo} indent=${record.indentNo} " +
                    "vehicle=${record.vehicleNo} status=${record.status}"
        )

        _uiState.update {
            it.copy(
                showDetailDialog = true,
                selectedRecord = record,
                detailRows = emptyList(),
                isLoadingDetail = true,
                isDetailRejected = record.status != UnloadStatus.UNLOADED
            )
        }

        viewModelScope.launch {
            try {
                Log.d(TAG, "Fetching detail rows for ws=${record.wsNo} indent=${record.indentNo}")
                val rows = repository.getUnloadingDetail(
                    indentNo  = record.indentNo,
                    wsNo      = record.wsNo,
                    vehicleNo = record.vehicleNo
                )
                Log.d(TAG, "Detail rows received: ${rows.size}")
                _uiState.update { it.copy(detailRows = rows, isLoadingDetail = false) }
            } catch (e: Exception) {
                Log.e(TAG, "openDetailDialog error", e)
                _uiState.update { it.copy(isLoadingDetail = false) }
            }
        }
    }

    fun closeDetailDialog() {
        Log.d(TAG, "closeDetailDialog() called")
        _uiState.update {
            it.copy(
                showDetailDialog = false,
                selectedRecord = null,
                detailRows = emptyList(),
                isLoadingDetail = false
            )
        }
    }
}