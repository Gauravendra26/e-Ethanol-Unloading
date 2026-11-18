package com.visibleinfotech.e_ethanolUnloading.gateEntry.models

data class InsertIndentResult(
    val success: Boolean,
    val message: String,
    val insertedCount: Int = 0,
    val errors: List<String> = emptyList(),
    val indentNumbers: List<Int> = emptyList()  // Added this field
)

