package com.visibleinfotech.e_ethanolUnloading.indentAndToken

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.visibleinfotech.e_ethanolUnloading.utils.DateTimeUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class IndentAndTokenUiState(

    val isLoadingList: Boolean = false,
    val isLoadingDetails: Boolean = false,
    val isSubmitting: Boolean = false,

    val searchText: String = "",
    val allOptions: List<TruckOption> = emptyList(),
    val filteredOptions: List<TruckOption> = emptyList(),
    val selectedOption: TruckOption? = null,

    val selectedEntry: PendingIndentEntry? = null,

    val doNo: String = "",
    val driverName: String = "",
    val driverMobile: String = "",
    val remarks: String = "",

    val showValidationErrors: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

class IndentAndTokenViewModel(
    private val repository: IndentAndTokenRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(IndentAndTokenUiState())
    val uiState: StateFlow<IndentAndTokenUiState> = _uiState.asStateFlow()

    init { loadPendingOptions() }

    fun loadPendingOptions() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingList = true, errorMessage = null) }
            val options = repository.getPendingTruckOptions()
            _uiState.update {
                it.copy(
                    isLoadingList = false,
                    allOptions = options,
                    filteredOptions = options
                )
            }
        }
    }

    fun updateSearchText(text: String) {
        val filtered = if (text.isBlank()) {
            _uiState.value.allOptions
        } else {
            val q = text.trim().lowercase()
            _uiState.value.allOptions.filter {
                it.truckNo.lowercase().contains(q)
            }
        }
        _uiState.update { it.copy(searchText = text, filteredOptions = filtered) }
    }

    fun selectOption(option: TruckOption) {
        _uiState.update {
            it.copy(
                selectedOption = option,
                searchText = option.toString(),
                selectedEntry = null,
                doNo = "",
                driverName = "",
                driverMobile = "",
                remarks = ""
            )
        }
        loadTruckDetails(option.gCode)
    }

    fun clearSelectedOption() {
        _uiState.update {
            it.copy(
                selectedOption = null,
                searchText = "",
                selectedEntry = null,
                filteredOptions = it.allOptions,
                doNo = "",
                driverName = "",
                driverMobile = "",
                remarks = ""
            )
        }
    }

    private fun loadTruckDetails(gCode: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingDetails = true, errorMessage = null) }
            val entry = repository.getPendingEntryByGCode(gCode)
            _uiState.update {
                it.copy(
                    isLoadingDetails = false,
                    selectedEntry = entry,
                    driverName = entry?.driverName.orEmpty(),
                    driverMobile = entry?.driverMobile.orEmpty(),
                    doNo = entry?.doNo?.toString().orEmpty(),
                    remarks = entry?.remarks.orEmpty()
                )
            }
        }
    }

    fun updateDoNo(v: String)       = _uiState.update { it.copy(doNo = v.filter(Char::isDigit).take(10)) }
    fun updateDriverName(v: String) = _uiState.update { it.copy(driverName = v.uppercase().take(100)) }
    fun updateDriverMobile(v: String) = _uiState.update {
        it.copy(driverMobile = v.filter(Char::isDigit).take(10))
    }
    fun updateRemarks(v: String)    = _uiState.update { it.copy(remarks = v.uppercase().take(200)) }

    fun setShowValidationErrors(show: Boolean) =
        _uiState.update { it.copy(showValidationErrors = show) }

    fun setErrorMessage(msg: String?)   = _uiState.update { it.copy(errorMessage = msg) }
    fun setSuccessMessage(msg: String?) = _uiState.update { it.copy(successMessage = msg) }

    fun isFormValid(): Boolean {
        val s = _uiState.value
        return s.selectedOption != null &&
                s.selectedEntry != null &&
                s.driverName.isNotBlank() &&
                s.driverMobile.length == 10
    }

    fun submitToken(updateBy: String, onDone: (Boolean, String) -> Unit) {
        val s = _uiState.value
        val entry = s.selectedEntry
        if (entry == null) { onDone(false, "No entry selected"); return }

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }

            val payload = TokenUpdateData(
                gCode        = entry.gCode,
                truckNo      = entry.truckNo,
                driverName   = s.driverName,
                driverMobile = s.driverMobile,
                doNo         = s.doNo,
                remarks      = s.remarks,
                updateBy     = updateBy,
                updateDate   = DateTimeUtils.getCurrentDateTime()
            )

        }
    }

    fun resetForm() {
        _uiState.update {
            IndentAndTokenUiState(
                allOptions = it.allOptions,
                filteredOptions = it.allOptions
            )
        }
    }
}

class IndentAndTokenViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return IndentAndTokenViewModel(IndentAndTokenRepository(context)) as T
    }
}