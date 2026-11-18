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

class SaudaVerifyViewModel(
    application: Application,
    private val repository: SaudaVerifyRepository
) : AndroidViewModel(application) {

    companion object { private const val TAG = "SaudaVerifyVM" }

    private val mainConstant = MainConstant(application.applicationContext)

    private val _records = MutableLiveData<List<SaudaItem>>(emptyList())
    val records: LiveData<List<SaudaItem>> = _records

    private val _filteredRecords = MutableLiveData<List<SaudaItem>>(emptyList())
    val filteredRecords: LiveData<List<SaudaItem>> = _filteredRecords

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _searchQuery = MutableLiveData("")
    val searchQuery: LiveData<String> = _searchQuery

    private val _isVerifying = MutableLiveData(false)
    val isVerifying: LiveData<Boolean> = _isVerifying

    private val _verifyResult = MutableLiveData<String?>()
    val verifyResult: LiveData<String?> = _verifyResult

    fun loadList() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val list = repository.getVerifyList(mainConstant.getUserCode())
                _records.value = list
                applyFilters()
            } catch (e: Exception) {
                Log.e(TAG, "load error: ${e.message}", e)
                _error.value = "Failed to load: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun refresh() = loadList()

    fun updateSearchQuery(q: String) {
        _searchQuery.value = q
        applyFilters()
    }

    private fun applyFilters() {
        val all = _records.value ?: emptyList()
        val q = _searchQuery.value?.trim()?.lowercase().orEmpty()
        _filteredRecords.value = if (q.isBlank()) all else all.filter {
            it.plotId.contains(q, true) ||
                    it.partyName.contains(q, true) ||
                    it.placeName.contains(q, true) ||
                    it.cropType.contains(q, true) ||
                    it.brokerName.contains(q, true)
        }
    }

    fun verifySauda(plotId: String) {
        viewModelScope.launch {
            _isVerifying.value = true
            _verifyResult.value = null
            try {
                val user = mainConstant.getUsername()
                val date = DateTimeUtils.getCurrentDateTime()
                val result = repository.verifySauda(plotId, user, date)
                _verifyResult.value = result.message
                if (result.success) loadList()
            } catch (e: Exception) {
                _verifyResult.value = "Error: ${e.message}"
            } finally {
                _isVerifying.value = false
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
            _isVerifying.value = true
            _error.value = null
            try {
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
                    onDone(true, "Sauda $plotId updated")
                    loadList()
                } else {
                    onDone(false, result.exceptionOrNull()?.message ?: "Failed to update")
                }
            } catch (e: Exception) {
                Log.e(TAG, "updateSauda error: ${e.message}", e)
                onDone(false, "Error: ${e.message}")
            } finally {
                _isVerifying.value = false
            }
        }
    }
    fun clearVerifyResult() { _verifyResult.value = null }
}

class SaudaVerifyViewModelFactory(
    private val application: Application,
    private val repository: SaudaVerifyRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SaudaVerifyViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SaudaVerifyViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}