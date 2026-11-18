package com.visibleinfotech.e_ethanolUnloading.gateEntry

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.util.Log
import com.visibleinfotech.e_ethanolUnloading.MainConstant
import com.visibleinfotech.e_ethanolUnloading.gateEntry.models.*
import com.visibleinfotech.e_ethanolUnloading.sqlConnection.ConnectionStr
import com.visibleinfotech.e_ethanolUnloading.utils.UriHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.SQLException
import java.util.concurrent.TimeUnit
import kotlin.random.Random

private const val TAG = "GateEntryRepository"

class GateEntryRepository(private val context: Context) {

    private val mainConstant = MainConstant(context)
    private var imageApiUrl: String? = null
    private var imageUploadApiUrl: String? = null
    private var modeListCache: List<ModeMasterData>? = null

    private fun generateDocumentImageName(prefix: String): String {
        val randomDigits = (1..15).map { Random.nextInt(0, 10) }.joinToString("")
        return "${prefix}_$randomDigits"
    }

    suspend fun uploadDocumentImage(imageFile: File, documentType: String): Pair<Boolean, String> {
        return withContext(Dispatchers.IO) {
            try {
                val imageApiUrl = getImageUploadApiUrl()

                if (imageApiUrl.isNullOrEmpty()) {
                    Log.e(TAG, "Image API URL is empty or not configured")
                    return@withContext Pair(false, "")
                }

                if (!imageFile.exists()) {
                    Log.e(TAG, "Image file does not exist: ${imageFile.absolutePath}")
                    return@withContext Pair(false, "")
                }

                val imageName = when (documentType) {
                    "doc1" -> generateDocumentImageName("Anugya")
                    "doc2" -> generateDocumentImageName("Bilty")
                    "doc3" -> generateDocumentImageName("Invoice")
                    "doc4" -> generateDocumentImageName("Weighment")
                    "doc5" -> generateDocumentImageName("Other")
                    else -> generateDocumentImageName("Document")
                }

                Log.d(TAG, "=== UPLOADING IMAGE TO API ===")
                Log.d(TAG, "Document Type: $documentType")
                Log.d(TAG, "API URL: $imageApiUrl")
                Log.d(TAG, "Image Path: ${imageFile.absolutePath}")
                Log.d(TAG, "Generated Image Name: $imageName")
                Log.d(TAG, "Image Size: ${imageFile.length()} bytes")

                val client = OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .build()

                val requestBody = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart(
                        "image",
                        "$imageName.jpg",
                        imageFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
                    )
                    .addFormDataPart("image_name", imageName)
                    .addFormDataPart("site_code", mainConstant.getSiteCode())
                    .addFormDataPart("document_type", "gateentry_$documentType")
                    .build()

                val request = Request.Builder()
                    .url(imageApiUrl)
                    .post(requestBody)
                    .build()

                val response = client.newCall(request).execute()

                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    Log.d(TAG, "Image upload successful for $documentType. Response: $responseBody")
                    return@withContext Pair(true, imageName)
                } else {
                    Log.e(
                        TAG,
                        "Image upload failed for $documentType. Response code: ${response.code}"
                    )
                    Log.e(TAG, "Response message: ${response.message}")
                    return@withContext Pair(false, "")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error uploading image for $documentType", e)
                return@withContext Pair(false, "")
            }
        }
    }

    private suspend fun processAndUploadImage(uriString: String?, documentType: String): String? {
        if (uriString.isNullOrEmpty()) {
            Log.d(TAG, "No image provided for $documentType")
            return null
        }

        return try {
            val uri = Uri.parse(uriString)
            val imageFile = UriHelper.getFileFromUri(context, uri)

            if (imageFile != null && imageFile.exists()) {
                Log.d(TAG, "Processing $documentType image...")
                val uploadResult = uploadDocumentImage(imageFile, documentType)
                imageFile.delete()

                if (uploadResult.first) {
                    Log.d(TAG, "$documentType image uploaded successfully: ${uploadResult.second}")
                    uploadResult.second
                } else {
                    Log.e(TAG, "Failed to upload $documentType image")
                    null
                }
            } else {
                Log.e(TAG, "Failed to convert URI to file for $documentType")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing $documentType image: ${e.message}")
            null
        }
    }

    suspend fun getDispatchCodeForTruck(truckNo: String): String? = withContext(Dispatchers.IO) {
        var connection: Connection? = null
        var statement: PreparedStatement? = null
        var resultSet: ResultSet? = null

        try {
            val conStr = ConnectionStr()
            val dbIp = mainConstant.getIp()
            val dbName = mainConstant.getDbName()
            val dbUser = mainConstant.getDbUser()
            val dbPass = mainConstant.getDbPass()
            val instance = mainConstant.getInstance()

            connection = conStr.connectionclasss(dbUser, dbPass, dbName, dbIp, instance)
            Log.d(
                TAG,
                "Database connection dbUser: " + dbUser + " dbPass: " + dbPass + " dbName: " + dbName + " dbIp: " + dbIp
            )
            if (connection == null) {
                Log.e(TAG, "Database connection failed while fetching G_Code")
                return@withContext null
            }

            val query = """
                SELECT TOP 1 CAST(G_Code AS VARCHAR) as G_Code
                FROM Vipl_ETGateEntry 
                WHERE G_TruckNo = ? AND ISNULL(G_Status, 'N') = 'N'
                ORDER BY G_Code DESC
            """.trimIndent()

            Log.d(TAG, "Executing query to get G_Code for truck: $truckNo")

            statement = connection.prepareStatement(query)
            statement.setString(1, truckNo)
            statement.queryTimeout = 10000
            resultSet = statement.executeQuery()

            if (resultSet.next()) {
                val dCode = resultSet.getString("G_Code")?.trim()
                Log.d(TAG, "✅ Found G_Code: $dCode for truck: $truckNo")
                return@withContext dCode
            } else {
                Log.d(TAG, "❌ No G_Code found for truck: $truckNo")
                return@withContext null
            }

        } catch (e: SQLException) {
            Log.e(TAG, "❌ SQL Exception while fetching G_Code: ${e.message}")
            e.printStackTrace()
        } catch (e: Exception) {
            Log.e(TAG, "❌ Exception while fetching G_Code: ${e.message}")
            e.printStackTrace()
        } finally {
            try {
                resultSet?.close()
                statement?.close()
                connection?.close()
            } catch (e: Exception) {
                Log.e(TAG, "Error closing resources: ${e.message}")
            }
        }
        return@withContext null
    }

    suspend fun getTruckPlotAllocation(truckNo: String): TruckPlotAllocationSummary? =
        withContext(Dispatchers.IO) {
            var connection: Connection? = null
            var statement: PreparedStatement? = null
            var resultSet: ResultSet? = null

            try {
                val conStr = ConnectionStr()
                val dbIp = mainConstant.getIp()
                val dbName = mainConstant.getDbName()
                val dbUser = mainConstant.getDbUser()
                val dbPass = mainConstant.getDbPass()
                val instance = mainConstant.getInstance()

                connection = conStr.connectionclasss(dbUser, dbPass, dbName, dbIp, instance)

                if (connection == null) {
                    Log.e(TAG, "Database connection failed while fetching plot allocation")
                    return@withContext null
                }

                val query = """
                WITH PlotAvailable AS
                (
                    SELECT
                        PL_PLOTID,
                        PL_GrowerCode,
                        PL_VillageCode,
                        PL_PlantationDate,
                        PL_CropId,
                        PL_CaneId,
                        PL_Area,
                        PL_LandName,
                        PL_MDays,
                        PL_BrokerCode,
                        PL_VillName,
                        PL_CropName,
                        PL_VarName,
                        PL_Plotvillcode,
                        Pl_MazeType,
                        PL_EstTons,
                        PL_EstTons + (PL_EstTons * 5.0 / 100) AS TotalAvailable,
                        PL_CreatedOn
                    FROM VIPL_CanePlotSurvey
                    WHERE PL_Enable = 1
                ),
                
                DispatchWithRunningTotal AS
                (
                    SELECT
                        G_Code,
                        G_BrokerCode,
                        G_Qty,
                        G_Date,
                        G_TruckNo,
                        G_InvoiceNo,
                        G_PartyCode,
                        SUM(G_Qty) OVER
                        (
                            PARTITION BY G_BrokerCode
                            ORDER BY G_Date, G_Code
                            ROWS UNBOUNDED PRECEDING
                        ) AS RunningTotal,
                        ISNULL(
                            SUM(G_Qty) OVER
                            (
                                PARTITION BY G_BrokerCode
                                ORDER BY G_Date, G_Code
                                ROWS BETWEEN UNBOUNDED PRECEDING AND 1 PRECEDING
                            ),
                            0
                        ) AS PreviousRunningTotal
                    FROM Vipl_ETGateEntry
                ),
                
                PlotCumulative AS
                (
                    SELECT
                        p1.PL_PLOTID,
                        p1.PL_GrowerCode,
                        p1.PL_VillageCode,
                        p1.PL_PlantationDate,
                        p1.PL_CropId,
                        p1.PL_CaneId,
                        p1.PL_Area,
                        p1.PL_LandName,
                        p1.PL_MDays,
                        p1.PL_BrokerCode,
                        p1.PL_VillName,
                        p1.PL_CropName,
                        p1.PL_VarName,
                        p1.PL_Plotvillcode,
                        p1.Pl_MazeType,
                        p1.TotalAvailable,
                        p1.PL_CreatedOn,
                        (
                            SELECT SUM(p2.TotalAvailable)
                            FROM PlotAvailable p2
                            WHERE p2.PL_BrokerCode = p1.PL_BrokerCode
                              AND p2.PL_CreatedOn <= p1.PL_CreatedOn
                        ) AS CumulativeAvailable,
                        ISNULL
                        (
                            (
                                SELECT SUM(p2.TotalAvailable)
                                FROM PlotAvailable p2
                                WHERE p2.PL_BrokerCode = p1.PL_BrokerCode
                                  AND p2.PL_CreatedOn < p1.PL_CreatedOn
                            ),
                            0
                        ) AS PreviousCumulative
                    FROM PlotAvailable p1
                ),
                
                Allocation AS
                (
                    SELECT
                        d.G_Code,
                        d.G_TruckNo,
                        d.G_InvoiceNo,
                        d.G_Qty AS Dispatch_Qty,
                        d.G_Date,
                        d.RunningTotal,
                        d.PreviousRunningTotal,
                        p.PL_PLOTID,
                        p.PL_GrowerCode,
                        p.PL_VillageCode,
                        p.PL_PlantationDate,
                        p.PL_CropId,
                        p.PL_CaneId,
                        p.PL_Area,
                        p.PL_LandName,
                        p.PL_MDays,
                        p.PL_BrokerCode,
                        p.PL_VillName,
                        p.PL_CropName,
                        p.PL_VarName,
                        p.PL_Plotvillcode,
                        p.Pl_MazeType,
                        p.TotalAvailable,
                        p.CumulativeAvailable,
                        p.PreviousCumulative,
                        
                        CASE
                            WHEN d.RunningTotal <= p.CumulativeAvailable 
                                 AND d.PreviousRunningTotal >= p.PreviousCumulative
                            THEN d.G_Qty
                            
                            WHEN d.RunningTotal > p.PreviousCumulative 
                                 AND d.PreviousRunningTotal < p.PreviousCumulative
                                 AND d.RunningTotal <= p.CumulativeAvailable
                            THEN d.RunningTotal - p.PreviousCumulative
                            
                            WHEN d.PreviousRunningTotal < p.CumulativeAvailable 
                                 AND d.PreviousRunningTotal >= p.PreviousCumulative
                                 AND d.RunningTotal > p.CumulativeAvailable
                            THEN p.CumulativeAvailable - d.PreviousRunningTotal
                            
                            WHEN d.PreviousRunningTotal < p.PreviousCumulative 
                                 AND d.RunningTotal > p.CumulativeAvailable
                            THEN p.TotalAvailable
                            
                            ELSE 0
                        END AS Quantity_Consumed_From_This_Plot
                    FROM DispatchWithRunningTotal d
                    CROSS JOIN PlotCumulative p
                    WHERE d.G_BrokerCode = p.PL_BrokerCode
                      AND d.RunningTotal > p.PreviousCumulative
                      AND d.PreviousRunningTotal < p.CumulativeAvailable
                      AND CASE
                            WHEN d.RunningTotal <= p.CumulativeAvailable 
                                 AND d.PreviousRunningTotal >= p.PreviousCumulative
                            THEN d.G_Qty
                            WHEN d.RunningTotal > p.PreviousCumulative 
                                 AND d.PreviousRunningTotal < p.PreviousCumulative
                                 AND d.RunningTotal <= p.CumulativeAvailable
                            THEN d.RunningTotal - p.PreviousCumulative
                            WHEN d.PreviousRunningTotal < p.CumulativeAvailable 
                                 AND d.PreviousRunningTotal >= p.PreviousCumulative
                                 AND d.RunningTotal > p.CumulativeAvailable
                            THEN p.CumulativeAvailable - d.PreviousRunningTotal
                            WHEN d.PreviousRunningTotal < p.PreviousCumulative 
                                 AND d.RunningTotal > p.CumulativeAvailable
                            THEN p.TotalAvailable
                            ELSE 0
                          END > 0
                )
                
SELECT
    Allocation.G_Code,
    Allocation.G_TruckNo,
    Allocation.G_InvoiceNo,
    Allocation.Dispatch_Qty,
    CONVERT(VARCHAR, Allocation.G_Date, 120) as G_Date,
    Allocation.PL_PLOTID,
    Allocation.PL_GrowerCode,
    Allocation.PL_VillageCode,
    CONVERT(VARCHAR, Allocation.PL_PlantationDate, 120) as PL_PlantationDate,
    Allocation.PL_CropId,
    Allocation.PL_CaneId,
    Allocation.PL_Area,
    Allocation.PL_LandName,
    Allocation.PL_MDays,
    Allocation.PL_BrokerCode,
    Allocation.PL_VillName,
    Allocation.PL_CropName,
    Allocation.PL_VarName,
    Allocation.PL_Plotvillcode,
    Allocation.Pl_MazeType,
    Allocation.TotalAvailable,
    Allocation.Quantity_Consumed_From_This_Plot,
    'PlotID ' + CAST(Allocation.PL_PLOTID AS VARCHAR(20)) + ' -> ' + CAST(ROUND(Allocation.Quantity_Consumed_From_This_Plot, 0) AS VARCHAR(20)) AS PlotWiseConsumption,
    ISNULL(am.account_name, '') as party_name,
    ISNULL(bm.account_name, '') as broker_name
FROM Allocation
LEFT JOIN VIPL_Accounts_master am ON am.account_id = Allocation.PL_GrowerCode
LEFT JOIN VIPL_Accounts_master bm ON bm.account_id = Allocation.PL_BrokerCode
WHERE Allocation.G_TruckNo = ? 
ORDER BY
    Allocation.G_Date,
    Allocation.G_Code,
    Allocation.PL_PLOTID
""".trimIndent()
                Log.d(TAG, "Query: $query")

                Log.d(TAG, "=== FETCHING TRUCK PLOT ALLOCATION ===")
                Log.d(TAG, "Truck No: $truckNo")

                statement = connection.prepareStatement(query)
                statement.setString(1, truckNo)
                statement.queryTimeout = 30000
                resultSet = statement.executeQuery()

                val allocations = mutableListOf<PlotAllocationData>()
                var totalDispatched = java.math.BigDecimal.ZERO
                var totalConsumed = java.math.BigDecimal.ZERO

                while (resultSet.next()) {
                    val dispatchQty =
                        resultSet.getBigDecimal("Dispatch_Qty") ?: java.math.BigDecimal.ZERO
                    val quantityConsumed =
                        resultSet.getBigDecimal("Quantity_Consumed_From_This_Plot")
                            ?: java.math.BigDecimal.ZERO

                    if (totalDispatched == java.math.BigDecimal.ZERO) {
                        totalDispatched = dispatchQty
                    }
                    totalConsumed += quantityConsumed

                    val allocation = PlotAllocationData(
                        dCode = resultSet.getString("G_Code")?.trim() ?: "",
                        dTruckNo = resultSet.getString("G_TruckNo")?.trim() ?: "",
                        dInvoiceNo = resultSet.getString("G_InvoiceNo")?.trim() ?: "",
                        dispatchQty = dispatchQty,
                        dDate = resultSet.getString("G_Date")?.trim() ?: "",
                        plotId = resultSet.getString("PL_PLOTID")?.trim() ?: "",
                        growerCode = resultSet.getString("PL_GrowerCode")?.trim() ?: "",
                        villageCode = resultSet.getString("PL_VillageCode")?.trim() ?: "",
                        plantationDate = resultSet.getString("PL_PlantationDate")?.trim(),
                        cropId = resultSet.getString("PL_CropId")?.trim() ?: "",
                        caneId = resultSet.getString("PL_CaneId")?.trim() ?: "",
                        area = resultSet.getBigDecimal("PL_Area") ?: java.math.BigDecimal.ZERO,
                        landName = resultSet.getString("PL_LandName")?.trim() ?: "",
                        mDays = resultSet.getInt("PL_MDays"),
                        brokerCode = resultSet.getString("PL_BrokerCode")?.trim() ?: "",
                        villageName = resultSet.getString("PL_VillName")?.trim() ?: "",
                        cropName = resultSet.getString("PL_CropName")?.trim() ?: "",
                        varietyName = resultSet.getString("PL_VarName")?.trim() ?: "",
                        plotVillageCode = resultSet.getString("PL_Plotvillcode")?.trim() ?: "",
                        mazeType = resultSet.getString("Pl_MazeType")?.trim() ?: "",
                        totalAvailable = resultSet.getBigDecimal("TotalAvailable")
                            ?: java.math.BigDecimal.ZERO,
                        quantityConsumed = quantityConsumed,
                        plotWiseConsumption = resultSet.getString("PlotWiseConsumption")?.trim()
                            ?: "",
                        partyName = resultSet.getString("party_name")?.trim() ?: "",
                        brokerName = resultSet.getString("broker_name")?.trim() ?: ""
                    )
                    allocations.add(allocation)
                }

                // Log the detailed allocation data
                Log.d(TAG, "=== PLOT ALLOCATION SUMMARY FOR TRUCK: $truckNo ===")
                Log.d(TAG, "Total Dispatch Qty: $totalDispatched")
                Log.d(TAG, "Total Consumed from Plots: $totalConsumed")
                Log.d(TAG, "Number of Plots: ${allocations.size}")
                Log.d(TAG, "=== PLOT ALLOCATION DETAILS ===")

                if (allocations.isEmpty()) {
                    Log.d(TAG, "No plot allocation found for this truck")
                } else {
                    allocations.forEachIndexed { index, plot ->
                        Log.d(TAG, "Plot #${index + 1}:")
                        Log.d(TAG, "  Plot ID: ${plot.plotId}")
                        Log.d(TAG, "  Village: ${plot.villageName}")
                        Log.d(TAG, "  Crop: ${plot.cropName}")
                        Log.d(TAG, "  Variety: ${plot.varietyName}")
                        Log.d(TAG, "  Land Name: ${plot.landName}")
                        Log.d(TAG, "  Area: ${plot.area}")
                        Log.d(TAG, "  Total Available: ${plot.totalAvailable}")
                        Log.d(TAG, "  Quantity Consumed: ${plot.quantityConsumed}")
                        Log.d(TAG, "  Consumption: ${plot.plotWiseConsumption}")
                        Log.d(TAG, "  ---")
                    }

                    Log.d(TAG, "=== PLOT ALLOCATION SUMMARY TABLE ===")
                    Log.d(TAG, "Truck No | Invoice | Plot ID | Village | Qty Consumed")
                    Log.d(TAG, "---------|---------|---------|---------|-------------")
                    allocations.forEach { plot ->
                        Log.d(
                            TAG,
                            "${plot.dTruckNo} | ${plot.dInvoiceNo} | ${plot.plotId} | ${plot.villageName} | ${plot.quantityConsumed}"
                        )
                    }
                }

                return@withContext TruckPlotAllocationSummary(
                    truckNo = truckNo,
                    totalDispatched = totalDispatched,
                    totalConsumedFromPlots = totalConsumed,
                    plotCount = allocations.size,
                    plotDetails = allocations
                )

            } catch (e: SQLException) {
                Log.e(TAG, "❌ SQL Exception while fetching plot allocation: ${e.message}")
                e.printStackTrace()
            } catch (e: Exception) {
                Log.e(TAG, "❌ Exception while fetching plot allocation: ${e.message}")
                e.printStackTrace()
            } finally {
                try {
                    resultSet?.close()
                    statement?.close()
                    connection?.close()
                } catch (e: Exception) {
                    Log.e(TAG, "Error closing resources: ${e.message}")
                }
            }
            return@withContext null
        }

    suspend fun updateGateEntryWithImages(
        updateRequest: GateEntryUpdateRequest,
        onImageProgress: (String, Int, String) -> Unit = { _, _, _ -> },
        onImageComplete: (String, Boolean) -> Unit = { _, _ -> },
    ): UpdateResult = withContext(Dispatchers.IO) {
        var connection: Connection? = null
        var statement: PreparedStatement? = null

        try {
            Log.d(TAG, "=========== UPDATE GATE ENTRY START ===========")
            Log.d(TAG, "G_Code: ${updateRequest.dCode}")
            Log.d(TAG, "Truck No: ${updateRequest.truckNo}")
            Log.d(TAG, "Update By: ${updateRequest.updateBy}")
            Log.d(TAG, "Update Date: ${updateRequest.updateDate}")

            // Upload images
            val doc1ImageName = processAndUploadImageWithProgress(
                uriString = updateRequest.doc1Uri,
                documentType = "doc1",
                onImageProgress = onImageProgress,
                onImageComplete = onImageComplete,
                imageName = "Anugya"
            )

            val doc2ImageName = processAndUploadImageWithProgress(
                uriString = updateRequest.doc2Uri,
                documentType = "doc2",
                onImageProgress = onImageProgress,
                onImageComplete = onImageComplete,
                imageName = "Bilty"
            )

            val doc3ImageName = processAndUploadImageWithProgress(
                uriString = updateRequest.doc3Uri,
                documentType = "doc3",
                onImageProgress = onImageProgress,
                onImageComplete = onImageComplete,
                imageName = "Invoice"
            )

            val doc4ImageName = processAndUploadImageWithProgress(
                uriString = updateRequest.doc4Uri,
                documentType = "doc4",
                onImageProgress = onImageProgress,
                onImageComplete = onImageComplete,
                imageName = "Weighment"
            )

            val doc5ImageName = if (!updateRequest.doc5Uri.isNullOrEmpty()) {
                processAndUploadImageWithProgress(
                    uriString = updateRequest.doc5Uri,
                    documentType = "doc5",
                    onImageProgress = onImageProgress,
                    onImageComplete = onImageComplete,
                    imageName = "Other"
                )
            } else {
                null
            }

            Log.d(TAG, "=========== IMAGE UPLOAD SUMMARY ===========")
            Log.d(TAG, "Doc1 (Anugya): ${doc1ImageName ?: "NOT UPLOADED / KEEP EXISTING"}")
            Log.d(TAG, "Doc2 (Bilty): ${doc2ImageName ?: "NOT UPLOADED / KEEP EXISTING"}")
            Log.d(TAG, "Doc3 (Invoice): ${doc3ImageName ?: "NOT UPLOADED / KEEP EXISTING"}")
            Log.d(TAG, "Doc4 (Weighment): ${doc4ImageName ?: "NOT UPLOADED / KEEP EXISTING"}")
            Log.d(TAG, "Doc5 (Other): ${doc5ImageName ?: "NOT UPLOADED / KEEP EXISTING"}")

            // Update database
            val conStr = ConnectionStr()
            val dbIp = mainConstant.getIp()
            val dbName = mainConstant.getDbName()
            val dbUser = mainConstant.getDbUser()
            val dbPass = mainConstant.getDbPass()
            val instance = mainConstant.getInstance()

            connection = conStr.connectionclasss(dbUser, dbPass, dbName, dbIp, instance)

            if (connection == null) {
                Log.e(TAG, "Database connection failed")
                return@withContext UpdateResult(false, "Database connection failed")
            }

            val updateFields = mutableListOf<String>()
            val parameters = mutableListOf<Any>()


            if (updateRequest.invoiceNo.isNotBlank()) {
                updateFields.add("G_InvoiceNo = ?")
                parameters.add(updateRequest.invoiceNo)
            }

            if (updateRequest.quantity.isNotBlank()) {
                val qty = updateRequest.quantity.toDoubleOrNull()
                if (qty != null && qty > 0) {
                    updateFields.add("G_Qty = ?")
                    parameters.add(java.math.BigDecimal(qty))
                }
            }

            if (doc1ImageName != null) {
                updateFields.add("G_Doc1 = ?")
                parameters.add(doc1ImageName)
            }

            if (doc2ImageName != null) {
                updateFields.add("G_Doc2 = ?")
                parameters.add(doc2ImageName)
            }

            if (doc3ImageName != null) {
                updateFields.add("G_Doc3 = ?")
                parameters.add(doc3ImageName)
            }

            if (doc4ImageName != null) {
                updateFields.add("G_Doc4 = ?")
                parameters.add(doc4ImageName)
            }

            if (doc5ImageName != null) {
                updateFields.add("G_Doc5 = ?")
                parameters.add(doc5ImageName)
            }

            updateFields.add("G_Status = ?")
            parameters.add("Y")

            if (updateRequest.updateBy.isNotBlank()) {
                updateFields.add("G_UpdateBy = ?")
                parameters.add(updateRequest.updateBy)
            }

            if (updateRequest.updateDate.isNotBlank()) {
                try {
                    val dateFormat = java.text.SimpleDateFormat(
                        "dd-MM-yyyy HH:mm:ss",
                        java.util.Locale.getDefault()
                    )
                    val parsedDate = dateFormat.parse(updateRequest.updateDate)
                    if (parsedDate != null) {
                        val sqlDateFormat = java.text.SimpleDateFormat(
                            "yyyy-MM-dd HH:mm:ss",
                            java.util.Locale.getDefault()
                        )
                        val formattedDate = sqlDateFormat.format(parsedDate)
                        updateFields.add("G_UpdateDate = ?")
                        parameters.add(formattedDate)
                    } else {
                        val currentDateTime = java.text.SimpleDateFormat(
                            "yyyy-MM-dd HH:mm:ss",
                            java.util.Locale.getDefault()
                        )
                            .format(java.util.Date())
                        updateFields.add("G_UpdateDate = ?")
                        parameters.add(currentDateTime)
                    }
                } catch (e: Exception) {
                    val currentDateTime = java.text.SimpleDateFormat(
                        "yyyy-MM-dd HH:mm:ss",
                        java.util.Locale.getDefault()
                    )
                        .format(java.util.Date())
                    updateFields.add("G_UpdateDate = ?")
                    parameters.add(currentDateTime)
                }
            } else {
                val currentDateTime =
                    java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
                        .format(java.util.Date())
                updateFields.add("G_UpdateDate = ?")
                parameters.add(currentDateTime)
            }

            if (updateFields.isEmpty()) {
                Log.e(TAG, "No fields to update")
                return@withContext UpdateResult(false, "No fields to update")
            }

            val query = """
                UPDATE Vipl_ETGateEntry 
                SET ${updateFields.joinToString(", ")}
                WHERE G_Code = ? AND G_TruckNo = ?
            """.trimIndent()

            Log.d(TAG, "Update Query: $query")

            statement = connection.prepareStatement(query)

            var paramIndex = 1
            for (param in parameters) {
                when (param) {
                    is String -> statement.setString(paramIndex, param)
                    is Long -> statement.setLong(paramIndex, param)
                    is Int -> statement.setInt(paramIndex, param)
                    is java.math.BigDecimal -> statement.setBigDecimal(paramIndex, param)
                    else -> statement.setObject(paramIndex, param)
                }
                paramIndex++
            }

            statement.setString(paramIndex, updateRequest.dCode)
            paramIndex++
            statement.setString(paramIndex, updateRequest.truckNo)

            Log.d(TAG, "=========== EXECUTING UPDATE ===========")
            val rowsAffected = statement.executeUpdate()

            if (rowsAffected > 0) {
                Log.d(TAG, "✅ Gate Entry updated successfully. Rows affected: $rowsAffected")
                UpdateResult(true, "Gate Entry updated successfully!")
            } else {
                Log.e(TAG, "❌ No rows updated. Check if G_Code and G_TruckNo exist.")
                UpdateResult(false, "Failed to update gate entry. Record not found.")
            }

        } catch (e: SQLException) {
            Log.e(TAG, "SQL Error in updateGateEntry: ${e.message}", e)
            UpdateResult(false, "Database error: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "Error in updateGateEntry: ${e.message}", e)
            UpdateResult(false, "Error: ${e.message}")
        } finally {
            try {
                statement?.close()
                connection?.close()
            } catch (e: Exception) {
                Log.e(TAG, "Close Error in updateGateEntry: ${e.message}")
            }
        }
    }

    private suspend fun processAndUploadImageWithProgress(
        uriString: String?,
        documentType: String,
        onImageProgress: (String, Int, String) -> Unit,
        onImageComplete: (String, Boolean) -> Unit,
        imageName: String,
    ): String? {
        if (uriString.isNullOrEmpty()) {
            Log.d(TAG, "No image provided for $documentType")
            return null
        }

        return try {
            onImageProgress(imageName, 0, "Preparing $imageName image...")

            val uri = Uri.parse(uriString)
            val imageFile = UriHelper.getFileFromUri(context, uri)

            if (imageFile != null && imageFile.exists()) {
                val fileSizeKB = imageFile.length() / 1024
                Log.d(TAG, "Processing $documentType image - Size: ${fileSizeKB}KB")

                onImageProgress(imageName, 20, "Converting $imageName image...")
                kotlinx.coroutines.delay(300)

                onImageProgress(imageName, 40, "Uploading $imageName image...")

                val uploadResult = uploadDocumentImage(imageFile, documentType)
                imageFile.delete()

                if (uploadResult.first) {
                    Log.d(TAG, "$documentType image uploaded successfully: ${uploadResult.second}")
                    onImageProgress(imageName, 100, "$imageName uploaded successfully!")
                    onImageComplete(imageName, true)
                    uploadResult.second
                } else {
                    Log.e(TAG, "Failed to upload $documentType image")
                    onImageProgress(imageName, 0, "Failed to upload $imageName")
                    onImageComplete(imageName, false)
                    null
                }
            } else {
                Log.e(TAG, "Failed to convert URI to file for $documentType")
                onImageComplete(imageName, false)
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing $documentType image: ${e.message}")
            onImageComplete(imageName, false)
            null
        }
    }

    suspend fun getImageApiUrl(): String? = withContext(Dispatchers.IO) {
        if (imageApiUrl != null) {
            Log.d(TAG, "Using cached image API URL: $imageApiUrl")
            return@withContext imageApiUrl
        }

        var connection: Connection? = null
        var statement: PreparedStatement? = null
        var resultSet: ResultSet? = null

        try {
            val conStr = ConnectionStr()
            val dbIp = mainConstant.getIp()
            val dbName = mainConstant.getDbName()
            val dbUser = mainConstant.getDbUser()
            val dbPass = mainConstant.getDbPass()
            val instance = mainConstant.getInstance()

            connection = conStr.connectionclasss(dbUser, dbPass, dbName, dbIp, instance)

            if (connection == null) {
                Log.e(TAG, "Database connection failed while fetching image API URL")
                return@withContext null
            }

            val query = "SELECT C_IMG_DOW_API, C_IMAGE_API FROM VIPL_sys1"
            Log.d(TAG, "Executing query: $query")

            statement = connection.prepareStatement(query)
            statement.queryTimeout = 10000
            resultSet = statement.executeQuery()

            if (resultSet.next()) {
                imageApiUrl = resultSet.getString("C_IMG_DOW_API")?.trim()
                imageUploadApiUrl = resultSet.getString("C_IMAGE_API")?.trim()
                return@withContext imageApiUrl
            } else {
                Log.e(TAG, "❌ No result found for C_IMG_DOW_API in VIPL_sys1 table")
                return@withContext null
            }

        } catch (e: SQLException) {
            Log.e(TAG, "❌ SQL Exception while fetching image API URL: ${e.message}")
            e.printStackTrace()
        } catch (e: Exception) {
            Log.e(TAG, "❌ Exception while fetching image API URL: ${e.message}")
            e.printStackTrace()
        } finally {
            try {
                resultSet?.close()
                statement?.close()
                connection?.close()
            } catch (e: Exception) {
                Log.e(TAG, "Error closing resources: ${e.message}")
            }
        }
        return@withContext null
    }

    suspend fun getImageUploadApiUrl(): String? = withContext(Dispatchers.IO) {
        if (imageUploadApiUrl != null) {
            return@withContext imageUploadApiUrl
        }
        getImageApiUrl()
        return@withContext imageUploadApiUrl
    }

    suspend fun getAvailableTruckNumbers(): List<String> = withContext(Dispatchers.IO) {
        var connection: Connection? = null
        var statement: PreparedStatement? = null
        var resultSet: ResultSet? = null
        val trucks = mutableListOf<String>()

        try {
            val conStr = ConnectionStr()
            val dbIp = mainConstant.getIp()
            val dbName = mainConstant.getDbName()
            val dbUser = mainConstant.getDbUser()
            val dbPass = mainConstant.getDbPass()
            val instance = mainConstant.getInstance()

            connection = conStr.connectionclasss(dbUser, dbPass, dbName, dbIp, instance)

            if (connection == null) {
                Log.e(TAG, "Database connection failed while loading truck numbers")
                return@withContext emptyList()
            }

            val query = """
                SELECT DISTINCT G_TruckNo 
                FROM Vipl_ETGateEntry WHERE ISNULL(G_Status, 'N') = 'N'
                ORDER BY G_TruckNo
            """.trimIndent()

            Log.d(TAG, "Executing query: $query")

            statement = connection.prepareStatement(query)
            statement.queryTimeout = 10000
            resultSet = statement.executeQuery()

            while (resultSet.next()) {
                val truckNo = resultSet.getString("G_TruckNo")?.trim() ?: ""
                if (truckNo.isNotEmpty()) {
                    trucks.add(truckNo)
                }
            }

            Log.d(TAG, "✅ Total Available Trucks Found: ${trucks.size}")

        } catch (e: SQLException) {
            Log.e(TAG, "❌ SQL Exception while loading trucks: ${e.message}")
            e.printStackTrace()
        } catch (e: Exception) {
            Log.e(TAG, "❌ Exception while loading trucks: ${e.message}")
            e.printStackTrace()
        } finally {
            try {
                resultSet?.close()
                statement?.close()
                connection?.close()
                Log.d(TAG, "Resources closed")
            } catch (e: Exception) {
                Log.e(TAG, "Error closing resources: ${e.message}")
            }
        }
        return@withContext trucks
    }

    suspend fun getTruckDispatchData(truckNo: String): TruckDispatchData? =
        withContext(Dispatchers.IO) {
            var connection: Connection? = null
            var statement: PreparedStatement? = null
            var resultSet: ResultSet? = null

            try {
                val conStr = ConnectionStr()
                val dbIp = mainConstant.getIp()
                val dbName = mainConstant.getDbName()
                val dbUser = mainConstant.getDbUser()
                val dbPass = mainConstant.getDbPass()
                val instance = mainConstant.getInstance()

                connection = conStr.connectionclasss(dbUser, dbPass, dbName, dbIp, instance)

                if (connection == null) {
                    Log.e(TAG, "Database connection failed")
                    return@withContext null
                }

                val query = """
                SELECT 
                    CAST(d.G_Code AS VARCHAR) as G_Code,
                    d.G_PartyCode,
                    d.G_TruckNo,  
                    d.G_InvoiceNo,
                    CAST(d.G_Qty AS VARCHAR) as G_Qty,
                    d.G_Date,
                    d.G_User,
                    d.G_Doc1,
                    d.G_Doc2,
                    d.G_Doc3,
                    d.G_Doc4,
                    d.G_Doc5,      
                    ISNULL(am.account_name, '') as party_name,
                    ISNULL(bm.account_name, '') as broker_name
                FROM Vipl_ETGateEntry d
                LEFT JOIN VIPL_Accounts_master am ON am.account_id = d.G_PartyCode
                LEFT JOIN VIPL_Accounts_master bm ON bm.account_id = d.G_BrokerCode
                WHERE d.G_TruckNo = ? AND ISNULL(d.G_Status, 'N') = 'N'
                ORDER BY d.G_Code DESC
            """.trimIndent()

                Log.d(TAG, "Executing query for truck: $truckNo")

                statement = connection.prepareStatement(query)
                statement.setString(1, truckNo)
                statement.queryTimeout = 10000
                resultSet = statement.executeQuery()

                if (resultSet.next()) {
                    val doc1 = resultSet.getString("G_Doc1")?.trim() ?: ""
                    val doc2 = resultSet.getString("G_Doc2")?.trim() ?: ""
                    val doc3 = resultSet.getString("G_Doc3")?.trim() ?: ""
                    val doc4 = resultSet.getString("G_Doc4")?.trim() ?: ""
                    val doc5 = resultSet.getString("G_Doc5")?.trim() ?: ""

                    Log.d(TAG, "=== DOCUMENT NAMES FROM DATABASE ===")
                    Log.d(TAG, "G_Doc1: '$doc1' (Available: ${doc1.isNotBlank()})")
                    Log.d(TAG, "G_Doc2: '$doc2' (Available: ${doc2.isNotBlank()})")
                    Log.d(TAG, "G_Doc3: '$doc3' (Available: ${doc3.isNotBlank()})")
                    Log.d(TAG, "G_Doc4: '$doc4' (Available: ${doc4.isNotBlank()})")
                    Log.d(TAG, "G_Doc5: '$doc5' (Available: ${doc5.isNotBlank()})")

                    val data = TruckDispatchData(
                        dCode = resultSet.getString("G_Code")?.trim() ?: "",
                        truckNo = resultSet.getString("G_TruckNo")?.trim() ?: "",
                        invoiceNo = resultSet.getString("G_InvoiceNo")?.trim() ?: "",
                        quantity = resultSet.getString("G_Qty")?.trim() ?: "",
                        partyName = resultSet.getString("party_name")?.trim() ?: "",
                        brokerName = resultSet.getString("broker_name")?.trim() ?: "",
                        doc1 = doc1,
                        doc2 = doc2,
                        doc3 = doc3,
                        doc4 = doc4,
                        doc5 = doc5
                    )

                    Log.d(TAG, "✅ Truck data found: Invoice=${data.invoiceNo}")

                    return@withContext data
                } else {
                    Log.d(TAG, "❌ No dispatch data found for truck: $truckNo")
                    return@withContext null
                }

            } catch (e: SQLException) {
                Log.e(TAG, "❌ SQL Exception while getting truck data: ${e.message}")
                e.printStackTrace()
            } catch (e: Exception) {
                Log.e(TAG, "❌ Exception while getting truck data: ${e.message}")
                e.printStackTrace()
            } finally {
                try {
                    resultSet?.close()
                    statement?.close()
                    connection?.close()
                    Log.d(TAG, "Resources closed")
                } catch (e: Exception) {
                    Log.e(TAG, "Error closing resources: ${e.message}")
                }
            }
            return@withContext null
        }

    suspend fun getModeMasterList(): List<ModeMasterData> = withContext(Dispatchers.IO) {
        modeListCache?.let {
            Log.d(TAG, "Returning cached mode list: ${it.size} items")
            return@withContext it
        }

        var connection: Connection? = null
        var statement: PreparedStatement? = null
        var resultSet: ResultSet? = null
        val modeList = mutableListOf<ModeMasterData>()

        try {
            val conStr = ConnectionStr()
            val dbIp = mainConstant.getIp()
            val dbName = mainConstant.getDbName()
            val dbUser = mainConstant.getDbUser()
            val dbPass = mainConstant.getDbPass()
            val instance = mainConstant.getInstance()

            connection = conStr.connectionclasss(dbUser, dbPass, dbName, dbIp, instance)

            if (connection == null) {
                Log.e(TAG, "Database connection failed while fetching mode master")
                return@withContext emptyList()
            }

            val query = """
                SELECT M_Code, M_Name 
                FROM VIPL_ModeMaster 
                ORDER BY M_Name
            """.trimIndent()

            Log.d(TAG, "Executing query: $query")

            statement = connection.prepareStatement(query)
            statement.queryTimeout = 10000
            resultSet = statement.executeQuery()

            while (resultSet.next()) {
                val code = resultSet.getString("M_Code")?.trim() ?: ""
                val name = resultSet.getString("M_Name")?.trim() ?: ""

                if (code.isNotEmpty() && name.isNotEmpty()) {
                    modeList.add(ModeMasterData(code = code, name = name))
                    Log.d(TAG, "Found mode: $code - $name")
                }
            }

            modeListCache = modeList

        } catch (e: SQLException) {
            Log.e(TAG, "❌ SQL Exception while fetching mode master: ${e.message}")
            e.printStackTrace()
        } catch (e: Exception) {
            Log.e(TAG, "❌ Exception while fetching mode master: ${e.message}")
            e.printStackTrace()
        } finally {
            try {
                resultSet?.close()
                statement?.close()
                connection?.close()
                Log.d(TAG, "Resources closed")
            } catch (e: Exception) {
                Log.e(TAG, "Error closing resources: ${e.message}")
            }
        }
        return@withContext modeList
    }


    suspend fun insertIndentRecords(
        truckNo: String,
        driverName: String,
        driverMobile: String,
        invoiceNo: String,
        invoiceWeight: String,
        numberOfBags: String,
        invoiceRate: String,
        invoiceAmount: String,
        remarks: String,
        modeCode: String,
        modeName: String,
        createdBy: String,
        plotAllocations: List<PlotAllocationData>,
    ): InsertIndentResult = withContext(Dispatchers.IO) {
        var connection: Connection? = null
        var statement: PreparedStatement? = null
        var resultSet: ResultSet? = null
        var maxNoStatement: PreparedStatement? = null
        var maxNoResultSet: ResultSet? = null

        try {
            Log.d(TAG, "=========== INSERT INDENT RECORDS START ===========")
            Log.d(TAG, "Truck No: $truckNo")
            Log.d(TAG, "Number of Plot Allocations: ${plotAllocations.size}")

            val conStr = ConnectionStr()
            val dbIp = mainConstant.getIp()
            val dbName = mainConstant.getDbName()
            val dbUser = mainConstant.getDbUser()
            val dbPass = mainConstant.getDbPass()
            val instance = mainConstant.getInstance()

            connection = conStr.connectionclasss(dbUser, dbPass, dbName, dbIp, instance)

            if (connection == null) {
                Log.e(TAG, "Database connection failed")
                return@withContext InsertIndentResult(false, "Database connection failed", 0)
            }

            // STEP 1: Get the current MAX I_No
            var currentMaxNo = 0
            try {
                val maxQuery = "SELECT ISNULL(MAX(I_No), 0) as MaxNo FROM VIPL_Indent"
                Log.d(TAG, "Getting MAX I_No: $maxQuery")

                maxNoStatement = connection.prepareStatement(maxQuery)
                maxNoStatement.queryTimeout = 10000
                maxNoResultSet = maxNoStatement.executeQuery()

                if (maxNoResultSet.next()) {
                    currentMaxNo = maxNoResultSet.getInt("MaxNo")
                    Log.d(TAG, "Current MAX I_No: $currentMaxNo")
                } else {
                    Log.w(TAG, "No records found in VIPL_Indent, starting from 1")
                    currentMaxNo = 0
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error getting MAX I_No: ${e.message}")
                currentMaxNo = 0
            } finally {
                try {
                    maxNoResultSet?.close()
                    maxNoStatement?.close()
                } catch (e: Exception) {
                    Log.e(TAG, "Error closing MAX query resources: ${e.message}")
                }
            }

            val currentDate =
                java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
                    .format(java.util.Date())
            val billDate = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                .format(java.util.Date())

            var insertedCount = 0
            val errors = mutableListOf<String>()
            var nextNo = currentMaxNo + 1
            val indentNumbersList = mutableListOf<Int>()  // ← ADD THIS LINE

            // Process each plot allocation
            plotAllocations.forEachIndexed { index, plot ->
                try {
                    Log.d(TAG, "========== Processing Plot #${index + 1} ==========")
                    Log.d(TAG, "Plot ID: ${plot.plotId}")
                    Log.d(TAG, "Quantity Consumed: ${plot.quantityConsumed}")
                    Log.d(TAG, "Village Code: ${plot.villageCode}")
                    Log.d(TAG, "Crop ID: ${plot.cropId}")
                    Log.d(TAG, "Variety ID: ${plot.varietyName}")

                    // Fetch additional data from master tables
                    val villageName = getVillageName(connection, plot.villageCode)
                    val cropName = getCropName(connection, plot.cropId)
                    val varietyName = getVarietyName(connection, plot.caneId)
                    val zoneInfo = getZoneInfo(connection, plot.villageCode)

                    Log.d(TAG, "Fetched Data:")
                    Log.d(TAG, "  Village Name: $villageName")
                    Log.d(TAG, "  Crop Name: $cropName")
                    Log.d(TAG, "  Variety Name: $varietyName")
                    Log.d(TAG, "  Zone Code: ${zoneInfo.first}, Zone Name: ${zoneInfo.second}")

                    // Build insert query with I_No included
                    val insertQuery = """
                    INSERT INTO VIPL_Indent (
                        I_No,
                        I_PlotNumber,
                        I_GrowerCode,
                        I_GrVillageCode,
                        I_PlantationDate,
                        I_CropId,
                        I_CaneId,
                        I_Area,
                        I_CreatedOn,
                        I_CreatedBy,
                        I_SupplyModeId,
                        I_LandName,
                        I_Enable,
                        I_EstTons,
                        I_Weigh,
                        I_MDays,
                        I_HarvCode,
                        I_PrintFlag,
                        I_TransCode,
                        I_EstModeWt,
                        I_WeighFlag,
                        I_RouteCode,
                        I_BurntCane,
                        I_GrName,
                        I_GrVillName,
                        I_TransName,
                        I_HarvName,
                        I_CropName,
                        I_VarName,
                        I_ModeName,
                        I_BurnCaneYN,
                        I_ZoneCode,
                        I_ZoneName,
                        I_PlotVillCode,
                        I_DelFlag,
                        I_Type,
                        Pl_InvoiceNo,
                        Pl_InvoiceWt,
                        Pl_remark,
                        Pl_Billdate,
                        I_DriverMobile,
                        I_DriverName,
                        I_Bags,
                        I_MazeType,
                        I_InvoiceAmt,
                        I_Rate
                    ) VALUES (
                        ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?
                    )
                """.trimIndent()

                    statement = connection.prepareStatement(insertQuery)

                    // Set parameters
                    var paramIndex = 1
                    statement.setInt(
                        paramIndex++,
                        nextNo
                    )                                    // I_No
                    statement.setString(
                        paramIndex++,
                        plot.plotId
                    )                            // I_PlotNumber
                    statement.setString(
                        paramIndex++,
                        plot.growerCode
                    )                        // I_GrowerCode
                    statement.setString(
                        paramIndex++,
                        plot.villageCode
                    )                       // I_GrVillageCode (ID)
                    statement.setString(
                        paramIndex++,
                        plot.plantationDate
                    )                    // I_PlantationDate
                    statement.setString(
                        paramIndex++,
                        plot.cropId
                    )                            // I_CropId
                    statement.setString(
                        paramIndex++,
                        plot.caneId
                    )                            // I_CaneId
                    statement.setBigDecimal(
                        paramIndex++,
                        plot.area
                    )                          // I_Area
                    statement.setString(
                        paramIndex++,
                        currentDate
                    )                            // I_CreatedOn
                    statement.setString(
                        paramIndex++,
                        createdBy
                    )                              // I_CreatedBy
                    statement.setString(
                        paramIndex++,
                        modeCode
                    )                               // I_SupplyModeId
                    statement.setString(
                        paramIndex++,
                        plot.landName
                    )                          // I_LandName
                    statement.setInt(
                        paramIndex++,
                        1
                    )                                         // I_Enable
                    statement.setBigDecimal(
                        paramIndex++,
                        plot.quantityConsumed
                    )              // I_EstTons
                    statement.setInt(
                        paramIndex++,
                        0
                    )                                         // I_Weigh
                    statement.setInt(
                        paramIndex++,
                        plot.mDays
                    )                                // I_MDays
                    statement.setString(
                        paramIndex++,
                        plot.brokerCode
                    )                        // I_HarvCode
                    statement.setInt(
                        paramIndex++,
                        0
                    )                                         // I_PrintFlag
                    statement.setInt(
                        paramIndex++,
                        0
                    )                                         // I_TransCode
                    statement.setInt(
                        paramIndex++,
                        5
                    )                                         // I_EstModeWt
                    statement.setInt(
                        paramIndex++,
                        1
                    )                                         // I_WeighFlag
                    statement.setInt(
                        paramIndex++,
                        0
                    )                                         // I_RouteCode
                    statement.setInt(
                        paramIndex++,
                        0
                    )                                         // I_BurntCane
                    statement.setString(
                        paramIndex++,
                        plot.partyName ?: ""
                    )                   // I_GrName (from accounts_master)
                    statement.setString(
                        paramIndex++,
                        villageName
                    )                            // I_GrVillName (from place_master)
                    statement.setString(
                        paramIndex++,
                        truckNo
                    )                                // I_TransName
                    statement.setString(
                        paramIndex++,
                        plot.brokerName ?: ""
                    )                  // I_HarvName (from accounts_master)
                    statement.setString(
                        paramIndex++,
                        cropName
                    )                               // I_CropName (from CropType)
                    statement.setString(
                        paramIndex++,
                        varietyName
                    )                            // I_VarName (from VarietyMaster)
                    statement.setString(
                        paramIndex++,
                        modeName
                    )                               // I_ModeName
                    statement.setString(
                        paramIndex++,
                        "No"
                    )                                   // I_BurnCaneYN
                    statement.setString(
                        paramIndex++,
                        zoneInfo.first
                    )                         // I_ZoneCode
                    statement.setString(
                        paramIndex++,
                        zoneInfo.second
                    )                        // I_ZoneName
                    statement.setString(
                        paramIndex++,
                        plot.plotVillageCode
                    )                   // I_PlotVillCode
                    statement.setInt(
                        paramIndex++,
                        1
                    )                                         // I_DelFlag
                    statement.setString(
                        paramIndex++,
                        "P"
                    )                                    // I_Type
                    statement.setString(
                        paramIndex++,
                        invoiceNo
                    )                              // Pl_InvoiceNo
                    statement.setBigDecimal(
                        paramIndex++,
                        invoiceWeight.toDoubleOrNull()?.let { java.math.BigDecimal.valueOf(it) }
                            ?: java.math.BigDecimal.ZERO
                    )                                                                         // Pl_InvoiceWt
                    statement.setString(
                        paramIndex++,
                        remarks
                    )                                // Pl_remark
                    statement.setString(
                        paramIndex++,
                        billDate
                    )                               // Pl_Billdate
                    statement.setString(
                        paramIndex++,
                        driverMobile
                    )                           // I_DriverMobile
                    statement.setString(
                        paramIndex++,
                        driverName
                    )                             // I_DriverName
                    statement.setInt(
                        paramIndex++,
                        numberOfBags.toIntOrNull() ?: 0
                    )           // I_Bags
                    statement.setString(
                        paramIndex++,
                        plot.mazeType
                    )                          // I_MazeType
                    statement.setBigDecimal(
                        paramIndex++,
                        invoiceAmount.toDoubleOrNull()?.let { java.math.BigDecimal.valueOf(it) }
                            ?: java.math.BigDecimal.ZERO
                    )                                                                         // I_InvoiceAmt
                    statement.setBigDecimal(
                        paramIndex++,
                        invoiceRate.toDoubleOrNull()?.let { java.math.BigDecimal.valueOf(it) }
                            ?: java.math.BigDecimal.ZERO
                    )                                                                         // I_Rate

                    Log.d(TAG, "Inserting with I_No: $nextNo")
                    Log.d(TAG, "  Village Name: $villageName")
                    Log.d(TAG, "  Crop Name: $cropName")
                    Log.d(TAG, "  Variety Name: $varietyName")

                    val rowsAffected = statement.executeUpdate()

                    if (rowsAffected > 0) {
                        insertedCount++
                        indentNumbersList.add(nextNo)
                        Log.d(
                            TAG,
                            "✅ Inserted record for Plot #${index + 1} (Plot ID: ${plot.plotId}, I_No: $nextNo)"
                        )
                        nextNo++
                    } else {
                        Log.e(TAG, "❌ Failed to insert record for Plot #${index + 1}")
                        errors.add("Failed to insert Plot ID: ${plot.plotId}")
                    }

                } catch (e: SQLException) {
                    Log.e(TAG, "❌ SQL Error inserting plot #${index + 1}: ${e.message}")
                    Log.e(TAG, "SQL State: ${e.sqlState}")
                    Log.e(TAG, "Error Code: ${e.errorCode}")
                    e.printStackTrace()
                    errors.add("Error for Plot ID ${plot.plotId}: ${e.message}")
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Error inserting plot #${index + 1}: ${e.message}")
                    e.printStackTrace()
                    errors.add("Error for Plot ID ${plot.plotId}: ${e.message}")
                } finally {
                    try {
                        statement?.close()
                    } catch (e: Exception) {
                        Log.e(TAG, "Error closing statement: ${e.message}")
                    }
                }
            }

            Log.d(TAG, "=========== INSERT INDENT RECORDS COMPLETE ===========")
            Log.d(TAG, "Total Inserted: $insertedCount / ${plotAllocations.size}")
            Log.d(TAG, "Final I_No value: ${nextNo - 1}")
            Log.d(TAG, "Indent Numbers: $indentNumbersList")  // ← ADD THIS LINE

            if (errors.isNotEmpty()) {
                Log.e(TAG, "Errors encountered: ${errors.joinToString("; ")}")
            }

            return@withContext InsertIndentResult(
                success = insertedCount == plotAllocations.size,
                message = if (insertedCount == plotAllocations.size) {
                    "All $insertedCount indent records inserted successfully! (I_No: ${nextNo - insertedCount} to ${nextNo - 1})"
                } else {
                    "Inserted $insertedCount out of ${plotAllocations.size} records. ${errors.size} errors."
                },
                insertedCount = insertedCount,
                errors = errors,
                indentNumbers = indentNumbersList  // ← ADD THIS LINE (return the list)
            )

        } catch (e: SQLException) {
            Log.e(TAG, "❌ SQL Exception while inserting indent records: ${e.message}")
            e.printStackTrace()
            return@withContext InsertIndentResult(false, "Database error: ${e.message}", 0)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Exception while inserting indent records: ${e.message}")
            e.printStackTrace()
            return@withContext InsertIndentResult(false, "Error: ${e.message}", 0)
        } finally {
            try {
                resultSet?.close()
                statement?.close()
                connection?.close()
            } catch (e: Exception) {
                Log.e(TAG, "Error closing resources: ${e.message}")
            }
        }
    }

    private suspend fun getVillageName(connection: Connection, villageCode: String): String {
        var statement: PreparedStatement? = null
        var resultSet: ResultSet? = null

        return try {
            val query = """
            SELECT ISNULL(place_name, '') as village_name
            FROM Vipl_place_master
            WHERE place_id = ?
        """.trimIndent()

            statement = connection.prepareStatement(query)
            statement.setString(1, villageCode)
            statement.queryTimeout = 10000
            resultSet = statement.executeQuery()

            if (resultSet.next()) {
                val villageName = resultSet.getString("village_name")?.trim() ?: ""
                Log.d(TAG, "Village Name for Code $villageCode: $villageName")
                villageName
            } else {
                Log.w(TAG, "No village found for code: $villageCode")
                ""
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting village name: ${e.message}")
            ""
        } finally {
            try {
                resultSet?.close()
                statement?.close()
            } catch (e: Exception) {
                Log.e(TAG, "Error closing resources: ${e.message}")
            }
        }
    }

    private suspend fun getCropName(connection: Connection, cropId: String): String {
        var statement: PreparedStatement? = null
        var resultSet: ResultSet? = null

        return try {
            val query = """
            SELECT ISNULL(cCropType, '') as crop_name
            FROM VIPL_CropType
            WHERE nCropId = ?
        """.trimIndent()

            statement = connection.prepareStatement(query)
            statement.setString(1, cropId)
            statement.queryTimeout = 10000
            resultSet = statement.executeQuery()

            if (resultSet.next()) {
                val cropName = resultSet.getString("crop_name")?.trim() ?: ""
                Log.d(TAG, "Crop Name for ID $cropId: $cropName")
                cropName
            } else {
                Log.w(TAG, "No crop found for ID: $cropId")
                ""
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting crop name: ${e.message}")
            ""
        } finally {
            try {
                resultSet?.close()
                statement?.close()
            } catch (e: Exception) {
                Log.e(TAG, "Error closing resources: ${e.message}")
            }
        }
    }

    private suspend fun getVarietyName(connection: Connection, varietyId: String): String {
        var statement: PreparedStatement? = null
        var resultSet: ResultSet? = null

        return try {
            val query = """
            SELECT ISNULL(Variety_Name, '') as variety_name
            FROM VIPL_VarietyMaster
            WHERE id = ?
        """.trimIndent()

            statement = connection.prepareStatement(query)
            statement.setString(1, varietyId)
            statement.queryTimeout = 10000
            resultSet = statement.executeQuery()

            if (resultSet.next()) {
                val varietyName = resultSet.getString("variety_name")?.trim() ?: ""
                Log.d(TAG, "Variety Name for ID $varietyId: $varietyName")
                varietyName
            } else {
                Log.w(TAG, "No variety found for ID: $varietyId")
                ""
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting variety name: ${e.message}")
            ""
        } finally {
            try {
                resultSet?.close()
                statement?.close()
            } catch (e: Exception) {
                Log.e(TAG, "Error closing resources: ${e.message}")
            }
        }
    }

    private suspend fun getZoneInfo(
        connection: Connection,
        villageCode: String,
    ): Pair<String, String> {
        var statement: PreparedStatement? = null
        var resultSet: ResultSet? = null

        return try {
            val query = """
            SELECT zm.z_name, zm.z_code
            FROM Vipl_place_master pm
            LEFT JOIN Vipl_Zonemaster zm ON zm.z_code = pm.Zone_id
            WHERE pm.place_id = ?
        """.trimIndent()

            statement = connection.prepareStatement(query)
            statement.setString(1, villageCode)
            statement.queryTimeout = 10000
            resultSet = statement.executeQuery()

            if (resultSet.next()) {
                val zoneCode = resultSet.getString("z_code")?.trim() ?: ""
                val zoneName = resultSet.getString("z_name")?.trim() ?: ""
                Log.d(TAG, "Zone Info for Village $villageCode: Code=$zoneCode, Name=$zoneName")
                Pair(zoneCode, zoneName)
            } else {
                Log.w(TAG, "No zone found for village code: $villageCode")
                Pair("", "")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting zone info: ${e.message}")
            Pair("", "")
        } finally {
            try {
                resultSet?.close()
                statement?.close()
            } catch (e: Exception) {
                Log.e(TAG, "Error closing resources: ${e.message}")
            }
        }
    }

    suspend fun insertTokenRecords(
        truckNo: String,
        driverName: String,
        driverMobile: String,
        invoiceNo: String,
        invoiceWeight: String,
        numberOfBags: String,
        invoiceRate: String,
        invoiceAmount: String,
        remarks: String,
        modeCode: String,
        modeName: String,
        createdBy: String,
        plotAllocations: List<PlotAllocationData>,
        indentNumbers: List<Int>,  // List of I_No from indent table
        rfidTag: String = "",  // 👈 ADD THIS NEW PARAMETER
    ): InsertIndentResult = withContext(Dispatchers.IO) {
        var connection: Connection? = null
        var statement: PreparedStatement? = null
        var resultSet: ResultSet? = null
        var maxNoStatement: PreparedStatement? = null
        var maxNoResultSet: ResultSet? = null

        try {
            Log.d(TAG, "=========== INSERT TOKEN RECORDS START ===========")
            Log.d(TAG, "Truck No: $truckNo")
            Log.d(TAG, "RFID Tag: $rfidTag")  // 👈 LOG RFID TAG
            Log.d(TAG, "Number of Plot Allocations: ${plotAllocations.size}")
            Log.d(TAG, "Indent Numbers: $indentNumbers")

            // Check if indent numbers match plot allocations
            if (indentNumbers.size != plotAllocations.size) {
                Log.e(
                    TAG,
                    "Indent numbers count (${indentNumbers.size}) does not match plot allocations count (${plotAllocations.size})"
                )
                return@withContext InsertIndentResult(false, "Indent numbers count mismatch", 0)
            }

            val conStr = ConnectionStr()
            val dbIp = mainConstant.getIp()
            val dbName = mainConstant.getDbName()
            val dbUser = mainConstant.getDbUser()
            val dbPass = mainConstant.getDbPass()
            val instance = mainConstant.getInstance()

            connection = conStr.connectionclasss(dbUser, dbPass, dbName, dbIp, instance)

            if (connection == null) {
                Log.e(TAG, "Database connection failed")
                return@withContext InsertIndentResult(false, "Database connection failed", 0)
            }

            // STEP 1: Get the current MAX TK_Gate_NO
            var currentMaxGateNo = 0
            try {
                val maxQuery = "SELECT ISNULL(MAX(TK_Gate_NO), 0) as MaxNo FROM VIPL_TOKEN"
                Log.d(TAG, "Getting MAX TK_Gate_NO: $maxQuery")

                maxNoStatement = connection.prepareStatement(maxQuery)
                maxNoStatement.queryTimeout = 10000
                maxNoResultSet = maxNoStatement.executeQuery()

                if (maxNoResultSet.next()) {
                    currentMaxGateNo = maxNoResultSet.getInt("MaxNo")
                    Log.d(TAG, "Current MAX TK_Gate_NO: $currentMaxGateNo")
                } else {
                    Log.w(TAG, "No records found in VIPL_TOKEN, starting from 1")
                    currentMaxGateNo = 0
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error getting MAX TK_Gate_NO: ${e.message}")
                currentMaxGateNo = 0
            } finally {
                try {
                    maxNoResultSet?.close()
                    maxNoStatement?.close()
                } catch (e: Exception) {
                    Log.e(TAG, "Error closing MAX query resources: ${e.message}")
                }
            }

            // STEP 2: Get current day's max RFID sequence
            val currentDate =
                java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
                    .format(java.util.Date())
            val currentDateStr = java.text.SimpleDateFormat("ddMM", java.util.Locale.getDefault())
                .format(java.util.Date())

            var currentDaySeq = 0
            try {
                val seqQuery = """
              SELECT ISNULL(MAX(CAST(SUBSTRING(CAST(TK_RFID_YARD_SEQ AS VARCHAR(20)), 5, 3) AS INT)), 0) as MaxSeq
FROM VIPL_TOKEN
WHERE LEFT(CAST(TK_RFID_YARD_SEQ AS VARCHAR(20)), 4) = ?
            """.trimIndent()

                val seqStatement = connection.prepareStatement(seqQuery)
                seqStatement.setString(1, currentDateStr)
                seqStatement.queryTimeout = 10000
                val seqResult = seqStatement.executeQuery()

                if (seqResult.next()) {
                    currentDaySeq = seqResult.getInt("MaxSeq")
                    Log.d(TAG, "Current day's max sequence: $currentDaySeq")
                }
                seqResult.close()
                seqStatement.close()
            } catch (e: Exception) {
                Log.e(TAG, "Error getting day sequence: ${e.message}")
                currentDaySeq = 0
            }

            var insertedCount = 0
            val errors = mutableListOf<String>()
            var nextGateNo = currentMaxGateNo + 1
            var nextDaySeq = currentDaySeq + 1

            val rfidNo = if (rfidTag.length >= 8) {
                rfidTag
            } else {
                Log.w(
                    TAG,
                    "RFID tag is invalid (length ${rfidTag.length}), using default value 1000000"
                )
                "1000000" // Fallback default
            }
            Log.d(TAG, "Using RFID_NO: $rfidNo")

            // Process each plot allocation with corresponding indent number
            plotAllocations.forEachIndexed { index, plot ->
                try {
                    val indentNo = indentNumbers[index] // Get corresponding I_No from indent table

                    Log.d(TAG, "========== Processing Token Plot #${index + 1} ==========")
                    Log.d(TAG, "Plot ID: ${plot.plotId}")
                    Log.d(TAG, "Quantity Consumed: ${plot.quantityConsumed}")
                    Log.d(TAG, "Indent Number (I_No): $indentNo")

                    // Fetch additional data from master tables
                    val villageName = getVillageName(connection, plot.villageCode)
                    val cropName = getCropName(connection, plot.cropId)
                    val varietyName = getVarietyName(connection, plot.caneId)
                    val zoneInfo = getZoneInfo(connection, plot.villageCode)

                    // Use plot.varietyName as fallback if varietyName not found
                    val finalVarietyName =
                        if (varietyName.isNotEmpty()) varietyName else plot.varietyName
                    // Use plot.cropName as fallback if cropName not found
                    val finalCropName = if (cropName.isNotEmpty()) cropName else plot.cropName

                    // Generate RFID_YARD_SEQ: DDMM + 3-digit sequence
                    val rfidSeq = String.format("%s%03d", currentDateStr, nextDaySeq)
                    Log.d(TAG, "Generated RFID_YARD_SEQ: $rfidSeq")

                    Log.d(TAG, "Fetched Data:")
                    Log.d(TAG, "  Village Name: $villageName")
                    Log.d(TAG, "  Crop Name: $finalCropName")
                    Log.d(TAG, "  Variety Name: $finalVarietyName")
                    Log.d(TAG, "  Zone Code: ${zoneInfo.first}, Zone Name: ${zoneInfo.second}")
                    Log.d(TAG, "  RFID SEQ: $rfidSeq")
                    Log.d(TAG, "  RFID NO: $rfidNo")  // 👈 UPDATED LOG
                    Log.d(TAG, "  Indent No (I_No): $indentNo")

                    // Build insert query with TK_RFID_NO included
                    val insertQuery = """
                INSERT INTO VIPL_TOKEN (
                    TK_Gate_NO,
                    TK_IndentNo,
                    TK_LABOUR_SLIP_NO,
                    TK_WS_Type,
                    TK_LAYER,
                    TK_DATE,
                    TK_GROWER_CODE,
                    TK_GROWER_NAME,
                    TK_VEHICLE_CODE,
                    TK_VEHICLE_NO,
                    TK_CONT_CODE,
                    TK_CONT_NAME,
                    TK_CANE_NAME,
                    TK_CANE_TYPE_ID,
                    TK_CROP_TYPE,
                    TK_PLOT_ID,
                    TK_MODE,
                    TK_RFID_YARD_SEQ,
                    TK_RFID_NO,
                    TK_GROSS_FLAG,
                    TK_USER,
                    tk_del_flag,
                    TK_burntcane,
                    tk_distance,
                    tk_villname,
                    tk_zonecode,
                    tk_villcode,
                    tk_vill,
                    tk_counter,
                    TK_MODE1,
                    tk_DoubleTrolly,
                    TK_WBNO,
                    Tk_DaySrNo,
                    Tk_DriverName,
                    Tk_TruckNo,
                    Tk_DriverMobile,
                    Tk_InvoiceNo,
                    Tk_InvoiceWt
                ) VALUES (
                    ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?
                )
            """.trimIndent()

                    Log.d(TAG, "Insert Query: $insertQuery")

                    statement = connection.prepareStatement(insertQuery)

                    // Set parameters
                    var paramIndex = 1
                    statement.setInt(paramIndex++, nextGateNo)                      // TK_Gate_NO
                    statement.setInt(paramIndex++, indentNo)                        // TK_IndentNo
                    statement.setInt(
                        paramIndex++,
                        indentNo
                    )                        // TK_LABOUR_SLIP_NO
                    statement.setString(paramIndex++, "A")                          // TK_WS_Type
                    statement.setInt(paramIndex++, 1)                               // TK_LAYER
                    statement.setString(paramIndex++, currentDate)                  // TK_DATE
                    statement.setString(
                        paramIndex++,
                        plot.growerCode
                    )              // TK_GROWER_CODE
                    statement.setString(
                        paramIndex++,
                        plot.partyName ?: ""
                    )         // TK_GROWER_NAME
                    statement.setInt(
                        paramIndex++,
                        0
                    )                               // TK_VEHICLE_CODE
                    statement.setString(paramIndex++, truckNo)                      // TK_VEHICLE_NO
                    statement.setString(paramIndex++, plot.brokerCode)              // TK_CONT_CODE
                    statement.setString(paramIndex++, plot.brokerName ?: "")        // TK_CONT_NAME
                    statement.setString(paramIndex++, finalVarietyName)             // TK_CANE_NAME
                    statement.setString(
                        paramIndex++,
                        finalVarietyName
                    )             // TK_CANE_TYPE_ID
                    statement.setString(paramIndex++, finalCropName)                // TK_CROP_TYPE
                    statement.setString(paramIndex++, plot.plotId)                  // TK_PLOT_ID
                    statement.setString(paramIndex++, modeName)                     // TK_MODE
                    statement.setString(
                        paramIndex++,
                        rfidSeq
                    )                      // TK_RFID_YARD_SEQ
                    statement.setString(
                        paramIndex++,
                        rfidNo
                    )                       // 👈 TK_RFID_NO (USE RFID TAG VALUE)
                    statement.setInt(paramIndex++, 1)                               // TK_GROSS_FLAG
                    statement.setString(paramIndex++, createdBy)                    // TK_USER
                    statement.setInt(paramIndex++, 0)                               // tk_del_flag
                    statement.setString(paramIndex++, "No")                         // TK_burntcane
                    statement.setInt(paramIndex++, 0)                               // tk_distance
                    statement.setString(paramIndex++, villageName)                  // tk_villname
                    statement.setString(paramIndex++, zoneInfo.first)               // tk_zonecode
                    statement.setString(paramIndex++, plot.villageCode)             // tk_villcode
                    statement.setString(paramIndex++, plot.villageCode)             // tk_vill
                    statement.setInt(paramIndex++, 1)                               // tk_counter
                    statement.setString(paramIndex++, modeName)                     // TK_MODE1
                    statement.setInt(
                        paramIndex++,
                        0
                    )                               // tk_DoubleTrolly
                    statement.setInt(paramIndex++, 1)                               // TK_WBNO
                    statement.setInt(paramIndex++, nextDaySeq)                      // Tk_DaySrNo
                    statement.setString(paramIndex++, driverName)                   // Tk_DriverName
                    statement.setString(paramIndex++, truckNo)                      // Tk_TruckNo
                    statement.setString(
                        paramIndex++,
                        driverMobile
                    )                 // Tk_DriverMobile
                    statement.setString(paramIndex++, invoiceNo)                    // Tk_InvoiceNo
                    statement.setBigDecimal(
                        paramIndex++,
                        invoiceWeight.toDoubleOrNull()?.let { java.math.BigDecimal.valueOf(it) }
                            ?: java.math.BigDecimal.ZERO
                    )                                                                // Tk_InvoiceWt

                    Log.d(
                        TAG,
                        "Inserting Token with TK_Gate_NO: $nextGateNo, IndentNo: $indentNo, RFID_NO: $rfidNo, RFID_SEQ: $rfidSeq, DaySeq: $nextDaySeq"
                    )

                    val rowsAffected = statement.executeUpdate()

                    if (rowsAffected > 0) {
                        insertedCount++
                        Log.d(
                            TAG,
                            "✅ Inserted Token record for Plot #${index + 1} (Gate NO: $nextGateNo, Indent No: $indentNo, RFID_NO: $rfidNo)"
                        )
                        nextGateNo++
                        nextDaySeq++
                    } else {
                        Log.e(TAG, "❌ Failed to insert Token record for Plot #${index + 1}")
                        errors.add("Failed to insert Token for Plot ID: ${plot.plotId}")
                    }

                } catch (e: SQLException) {
                    Log.e(TAG, "❌ SQL Error inserting Token plot #${index + 1}: ${e.message}")
                    Log.e(TAG, "SQL State: ${e.sqlState}")
                    Log.e(TAG, "Error Code: ${e.errorCode}")
                    e.printStackTrace()
                    errors.add("Error for Plot ID ${plot.plotId}: ${e.message}")
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Error inserting Token plot #${index + 1}: ${e.message}")
                    e.printStackTrace()
                    errors.add("Error for Plot ID ${plot.plotId}: ${e.message}")
                } finally {
                    try {
                        statement?.close()
                    } catch (e: Exception) {
                        Log.e(TAG, "Error closing statement: ${e.message}")
                    }
                }
            }

            Log.d(TAG, "=========== INSERT TOKEN RECORDS COMPLETE ===========")
            Log.d(TAG, "Total Inserted: $insertedCount / ${plotAllocations.size}")
            Log.d(TAG, "Final TK_Gate_NO value: ${nextGateNo - 1}")
            Log.d(TAG, "Final Day Sequence: ${nextDaySeq - 1}")

            if (errors.isNotEmpty()) {
                Log.e(TAG, "Errors encountered: ${errors.joinToString("; ")}")
            }

            return@withContext InsertIndentResult(
                success = insertedCount == plotAllocations.size,
                message = if (insertedCount == plotAllocations.size) {
                    "All $insertedCount token records inserted successfully! (Gate NO: ${nextGateNo - insertedCount} to ${nextGateNo - 1})"
                } else {
                    "Inserted $insertedCount out of ${plotAllocations.size} token records. ${errors.size} errors."
                },
                insertedCount = insertedCount,
                errors = errors
            )

        } catch (e: SQLException) {
            Log.e(TAG, "❌ SQL Exception while inserting token records: ${e.message}")
            e.printStackTrace()
            return@withContext InsertIndentResult(false, "Database error: ${e.message}", 0)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Exception while inserting token records: ${e.message}")
            e.printStackTrace()
            return@withContext InsertIndentResult(false, "Error: ${e.message}", 0)
        } finally {
            try {
                resultSet?.close()
                statement?.close()
                connection?.close()
            } catch (e: Exception) {
                Log.e(TAG, "Error closing resources: ${e.message}")
            }
        }
    }

    fun clearCache() {
        modeListCache = null
        imageApiUrl = null
        imageUploadApiUrl = null
        Log.d(TAG, "All caches cleared")
    }
}