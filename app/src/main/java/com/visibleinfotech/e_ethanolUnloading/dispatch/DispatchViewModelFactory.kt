package com.visibleinfotech.e_ethanolUnloading.dispatch


import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.visibleinfotech.e_ethanolUnloading.dispatch.DispatchRepository

class DispatchViewModelFactory(
    private val repository: DispatchRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DispatchViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DispatchViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}