package com.visibleinfotech.e_ethanolUnloading.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DateTimeUtils {

    // For database insertion - includes seconds
    private val databaseFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

    // Original method - keeps seconds for database operations
    fun getCurrentDateTime(): String {
        return databaseFormatter.format(Date())
    }


    // Additional utility functions
    fun getCurrentDate(): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return formatter.format(Date())
    }

    fun getCurrentTime(): String {
        val formatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        return formatter.format(Date())
    }

    // For UI display - time without seconds
    fun getCurrentTimeForDisplay(): String {
        val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
        return formatter.format(Date())
    }

    fun formatDateTime(date: Date): String {
        return databaseFormatter.format(date)
    }


    fun formatDate(date: Date, pattern: String = "yyyy-MM-dd"): String {
        val formatter = SimpleDateFormat(pattern, Locale.getDefault())
        return formatter.format(date)
    }
}