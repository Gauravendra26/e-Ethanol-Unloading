package com.visibleinfotech.e_ethanolUnloading.unloadingNew.models


data class UnloadingRow(
    val srNo: Int,
    val bags: String = "",
    val moisture: String = "",
    val uCode: Long = -1L,
    val isSaving: Boolean = false
) {
    val isPersisted: Boolean get() = uCode > 0
}