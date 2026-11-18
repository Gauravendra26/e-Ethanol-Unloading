package com.visibleinfotech.e_ethanolUnloading.gateEntry.models

import java.math.BigDecimal

data class PlotAllocationData(
    val dCode: String,
    val dTruckNo: String,
    val dInvoiceNo: String,
    val dispatchQty: BigDecimal,
    val dDate: String,
    val plotId: String,
    val growerCode: String,
    val villageCode: String,
    val plantationDate: String?,
    val cropId: String,
    val caneId: String,
    val area: BigDecimal,
    val landName: String,
    val mDays: Int,
    val brokerCode: String,
    val villageName: String,
    val cropName: String,
    val varietyName: String,
    val plotVillageCode: String,
    val mazeType: String,
    val totalAvailable: BigDecimal,
    val quantityConsumed: BigDecimal,
    val plotWiseConsumption: String,
    val partyName: String? = null,  // Added for I_GrName
    val brokerName: String? = null  // Added for I_HarvName
)

data class TruckPlotAllocationSummary(
    val truckNo: String,
    val totalDispatched: BigDecimal,
    val totalConsumedFromPlots: BigDecimal,
    val plotCount: Int,
    val plotDetails: List<PlotAllocationData>
)