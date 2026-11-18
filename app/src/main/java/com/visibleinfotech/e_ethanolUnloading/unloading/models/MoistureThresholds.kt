package com.visibleinfotech.e_ethanolUnloading.unloading.models

data class MoistureThresholds(
    val reject1: Double,
    val reject2: Double,
    val reject3: Double,
    val highAvgFlag: String,
)