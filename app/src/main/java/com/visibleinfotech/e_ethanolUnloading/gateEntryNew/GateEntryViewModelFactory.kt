package com.visibleinfotech.e_ethanolUnloading.gateEntryNew

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class GateEntryViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GateEntryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return GateEntryViewModel(
                repository = GateEntryRepository(context),
                context = context
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}