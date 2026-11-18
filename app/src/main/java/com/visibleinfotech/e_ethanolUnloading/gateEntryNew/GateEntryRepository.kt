package com.visibleinfotech.e_ethanolUnloading.gateEntryNew

import android.content.Context
import android.net.Uri
import android.util.Log
import com.visibleinfotech.e_ethanolUnloading.MainConstant
import com.visibleinfotech.e_ethanolUnloading.gateEntryNew.models.*
import com.visibleinfotech.e_ethanolUnloading.gateEntryNew.models.DispatchDataForGateEntry
import com.visibleinfotech.e_ethanolUnloading.gateEntryNew.models.GateEntryInsertData
import com.visibleinfotech.e_ethanolUnloading.gateEntryNew.models.GateEntryInsertResult
import com.visibleinfotech.e_ethanolUnloading.sqlConnection.ConnectionStr
import com.visibleinfotech.e_ethanolUnloading.utils.ImageUploadUtils
import com.visibleinfotech.e_ethanolUnloading.utils.UriHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.SQLException

private const val TAG = "GateEntryRepository"

class GateEntryRepository(private val context: Context) {

    private val mainConstant = MainConstant(context)
    private var imageApiUrl: String? = null
    private var imageUploadApiUrl: String? = null
    private var modeListCache: List<ModeMasterData>? = null

    // ========== IMAGE UPLOAD METHODS ==========

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
                    "doc1" -> ImageUploadUtils.generateImageName("Anugya")
                    "doc2" -> ImageUploadUtils.generateImageName("Bilty")
                    "doc3" -> ImageUploadUtils.generateImageName("Invoice")
                    "doc4" -> ImageUploadUtils.generateImageName("Weighment")
                    "doc5" -> ImageUploadUtils.generateImageName("Other")
                    else -> ImageUploadUtils.generateImageName("Document")
                }

                Log.d(TAG, "Uploading $documentType image: $imageName")

                val result = ImageUploadUtils.uploadImageToApi(
                    apiUrl = imageApiUrl,
                    imageFile = imageFile,
                    siteCode = mainConstant.getSiteCode(),
                    documentType = "gateentry_$documentType",
                    imageName = imageName
                )

                if (result.first) {
                    Log.d(TAG, "Image upload successful for $documentType. Image name: ${result.second}")
                    return@withContext Pair(true, result.second)
                } else {
                    Log.e(TAG, "Image upload failed for $documentType")
                    return@withContext Pair(false, "")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error uploading image for $documentType", e)
                return@withContext Pair(false, "")
            }
        }
    }

    private suspend fun processAndUploadImageWithProgress(
        uriString: String?,
        documentType: String,
        onImageProgress: (String, Int, String) -> Unit,
        onImageComplete: (String, Boolean) -> Unit,
        imageName: String
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
                onImageProgress(imageName, 20, "Optimizing $imageName image...")

                val optimizedFile = ImageUploadUtils.optimizeImage(imageFile)

                onImageProgress(imageName, 40, "Uploading $imageName image...")

                val uploadResult = uploadDocumentImage(optimizedFile, documentType)

                try {
                    optimizedFile.delete()
                } catch (e: Exception) {
                    Log.w(TAG, "Could not delete temporary file: ${e.message}")
                }

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

    suspend fun getDispatchDataForTruck(truckNo: String): DispatchDataForGateEntry? = withContext(Dispatchers.IO) {
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
                    CAST(d.D_Code AS VARCHAR) as D_Code,
                    CAST(d.D_Qty AS VARCHAR) as D_Qty,
                    d.D_InvoiceNo,
                    d.D_PartyCode,
                    d.D_BrokerCode,
                    ISNULL(am.account_name, '') as party_name,
                    ISNULL(bm.account_name, '') as broker_name,
                    d.D_Doc1,
                    d.D_Doc2,
                    d.D_Doc3,
                    d.D_Doc4,
                    d.D_Doc5,
                    d.D_DriverName,
                    d.D_DriverMobile,
                    d.D_Status
                FROM Vipl_ETDispatch d
                LEFT JOIN VIPL_Accounts_master am ON am.account_id = d.D_PartyCode
                LEFT JOIN VIPL_Accounts_master bm ON bm.account_id = d.D_BrokerCode
                WHERE d.D_TruckNo = ? 
                AND ISNULL(d.D_Status, 'N') = 'N'
                ORDER BY d.D_Code DESC
            """.trimIndent()

            Log.d(TAG, "Getting dispatch data for truck: $truckNo")

            statement = connection.prepareStatement(query)
            statement.setString(1, truckNo)
            statement.queryTimeout = 10000
            resultSet = statement.executeQuery()

            if (resultSet.next()) {
                val data = DispatchDataForGateEntry(
                    dCode = resultSet.getString("D_Code")?.trim() ?: "",
                    truckNo = truckNo,
                    driverName = resultSet.getString("D_DriverName")?.trim() ?: "",
                    driverMobile = resultSet.getString("D_DriverMobile")?.trim() ?: "",
                    invoiceNo = resultSet.getString("D_InvoiceNo")?.trim() ?: "",
                    quantity = resultSet.getString("D_Qty")?.trim() ?: "",
                    partyCode = resultSet.getString("D_PartyCode")?.trim() ?: "",
                    brokerCode = resultSet.getString("D_BrokerCode")?.trim() ?: "",
                    partyName = resultSet.getString("party_name")?.trim() ?: "",
                    brokerName = resultSet.getString("broker_name")?.trim() ?: "",
                    doc1 = resultSet.getString("D_Doc1")?.trim() ?: "",
                    doc2 = resultSet.getString("D_Doc2")?.trim() ?: "",
                    doc3 = resultSet.getString("D_Doc3")?.trim() ?: "",
                    doc4 = resultSet.getString("D_Doc4")?.trim() ?: "",
                    doc5 = resultSet.getString("D_Doc5")?.trim() ?: "",
                    status = resultSet.getString("D_Status")?.trim() ?: "N"
                )
                Log.d(TAG, "✅ Found dispatch data: D_Code=${data.dCode}, Qty=${data.quantity}")
                return@withContext data
            } else {
                Log.d(TAG, "❌ No dispatch data found for truck: $truckNo")
                return@withContext null
            }

        } catch (e: SQLException) {
            Log.e(TAG, "❌ SQL Exception: ${e.message}")
            e.printStackTrace()
        } catch (e: Exception) {
            Log.e(TAG, "❌ Exception: ${e.message}")
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

    suspend fun insertGateEntryRecord(
        gateEntryData: GateEntryInsertData
    ): GateEntryInsertResult = withContext(Dispatchers.IO) {
        var connection: Connection? = null
        var statement: PreparedStatement? = null
        var maxNoStatement: PreparedStatement? = null
        var maxNoResultSet: ResultSet? = null

        try {
            Log.d(TAG, "=========== INSERT GATE ENTRY RECORD ===========")
            Log.d(TAG, "Truck No: ${gateEntryData.truckNo}")
            Log.d(TAG, "D_Code: ${gateEntryData.dCode}")

            val conStr = ConnectionStr()
            val dbIp = mainConstant.getIp()
            val dbName = mainConstant.getDbName()
            val dbUser = mainConstant.getDbUser()
            val dbPass = mainConstant.getDbPass()
            val instance = mainConstant.getInstance()

            connection = conStr.connectionclasss(dbUser, dbPass, dbName, dbIp, instance)

            if (connection == null) {
                Log.e(TAG, "Database connection failed")
                return@withContext GateEntryInsertResult(false, "Database connection failed")
            }

            // Get MAX G_Code
            var currentMaxCode = 0
            try {
                val maxQuery = "SELECT ISNULL(MAX(G_Code), 0) as MaxCode FROM Vipl_ETGateEntry"
                maxNoStatement = connection.prepareStatement(maxQuery)
                maxNoStatement.queryTimeout = 10000
                maxNoResultSet = maxNoStatement.executeQuery()

                if (maxNoResultSet.next()) {
                    currentMaxCode = maxNoResultSet.getInt("MaxCode")
                    Log.d(TAG, "Current MAX G_Code: $currentMaxCode")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error getting MAX G_Code: ${e.message}")
            } finally {
                try {
                    maxNoResultSet?.close()
                    maxNoStatement?.close()
                } catch (e: Exception) {
                    Log.e(TAG, "Error closing MAX query resources: ${e.message}")
                }
            }

            val nextGCode = currentMaxCode + 1
            Log.d(TAG, "Next G_Code: $nextGCode")

            // INSERT query
            val insertQuery = """
                INSERT INTO Vipl_ETGateEntry (
                    G_Code, G_TruckNo, G_PartyCode, G_BrokerCode, G_DO, G_Qty, 
                    G_InvoiceNo, G_InvQty, G_InvAmt, G_Doc1, G_Doc2, G_Doc3, 
                    G_Doc4, G_Doc5, G_Date, G_User, G_ModeCode, G_Bags, 
                    G_InvRate, G_Remarks
                ) VALUES (
                    ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?
                )
            """.trimIndent()

            statement = connection.prepareStatement(insertQuery)

            var paramIndex = 1
            statement.setInt(paramIndex++, nextGCode)
            statement.setString(paramIndex++, gateEntryData.truckNo)
            statement.setString(paramIndex++, gateEntryData.partyCode)
            statement.setString(paramIndex++, gateEntryData.brokerCode)
            statement.setString(paramIndex++, gateEntryData.dCode)
            statement.setBigDecimal(paramIndex++,
                gateEntryData.qty.toDoubleOrNull()?.let { java.math.BigDecimal.valueOf(it) } ?: java.math.BigDecimal.ZERO
            )
            statement.setString(paramIndex++, gateEntryData.invoiceNo)
            statement.setBigDecimal(paramIndex++,
                gateEntryData.invQty.toDoubleOrNull()?.let { java.math.BigDecimal.valueOf(it) } ?: java.math.BigDecimal.ZERO
            )
            statement.setBigDecimal(paramIndex++,
                gateEntryData.invAmt.toDoubleOrNull()?.let { java.math.BigDecimal.valueOf(it) } ?: java.math.BigDecimal.ZERO
            )
            statement.setString(paramIndex++, gateEntryData.doc1)
            statement.setString(paramIndex++, gateEntryData.doc2)
            statement.setString(paramIndex++, gateEntryData.doc3)
            statement.setString(paramIndex++, gateEntryData.doc4)
            statement.setString(paramIndex++, gateEntryData.doc5)
            statement.setString(paramIndex++, gateEntryData.date)
            statement.setString(paramIndex++, gateEntryData.user)

            val modeCodeInt = gateEntryData.modeCode.toIntOrNull() ?: 0
            statement.setInt(paramIndex++, modeCodeInt)

            statement.setInt(paramIndex++, gateEntryData.bags.toIntOrNull() ?: 0)
            statement.setBigDecimal(paramIndex++,
                gateEntryData.invRate.toDoubleOrNull()?.let { java.math.BigDecimal.valueOf(it) } ?: java.math.BigDecimal.ZERO
            )
            statement.setString(paramIndex++, gateEntryData.remarks)


            val rowsAffected = statement.executeUpdate()

            if (rowsAffected > 0) {
                Log.d(TAG, "✅ Gate Entry inserted successfully with G_Code: $nextGCode")
                return@withContext GateEntryInsertResult(
                    success = true,
                    message = "Gate entry created with G_Code: $nextGCode",
                    gCode = nextGCode
                )
            } else {
                Log.e(TAG, "❌ Failed to insert gate entry")
                return@withContext GateEntryInsertResult(false, "Failed to insert gate entry")
            }

        } catch (e: SQLException) {
            Log.e(TAG, "❌ SQL Exception: ${e.message}")
            e.printStackTrace()
            return@withContext GateEntryInsertResult(false, "Database error: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Exception: ${e.message}")
            e.printStackTrace()
            return@withContext GateEntryInsertResult(false, "Error: ${e.message}")
        } finally {
            try {
                statement?.close()
                connection?.close()
            } catch (e: Exception) {
                Log.e(TAG, "Error closing resources: ${e.message}")
            }
        }
    }


    suspend fun updateGateEntryWithImages(
        updateRequest: GateEntryUpdateRequest,
        onImageProgress: (String, Int, String) -> Unit = { _, _, _ -> },
        onImageComplete: (String, Boolean) -> Unit = { _, _ -> }
    ): UpdateResult = withContext(Dispatchers.IO) {
        var connection: Connection? = null
        var statement: PreparedStatement? = null

        try {
            Log.d(TAG, "=========== UPDATE GATE ENTRY ===========")
            Log.d(TAG, "D_Code: ${updateRequest.dCode}")

            // Upload images
            val doc1ImageName = processAndUploadImageWithProgress(
                updateRequest.doc1Uri, "doc1", onImageProgress, onImageComplete, "Anugya"
            )
            val doc2ImageName = processAndUploadImageWithProgress(
                updateRequest.doc2Uri, "doc2", onImageProgress, onImageComplete, "Bilty"
            )
            val doc3ImageName = processAndUploadImageWithProgress(
                updateRequest.doc3Uri, "doc3", onImageProgress, onImageComplete, "Invoice"
            )
            val doc4ImageName = processAndUploadImageWithProgress(
                updateRequest.doc4Uri, "doc4", onImageProgress, onImageComplete, "Weighment"
            )
            val doc5ImageName = if (!updateRequest.doc5Uri.isNullOrEmpty()) {
                processAndUploadImageWithProgress(
                    updateRequest.doc5Uri, "doc5", onImageProgress, onImageComplete, "Other"
                )
            } else {
                null
            }

            Log.d(TAG, "Image Upload Summary:")
            Log.d(TAG, "Doc1: ${doc1ImageName ?: "Keep existing"}")
            Log.d(TAG, "Doc2: ${doc2ImageName ?: "Keep existing"}")
            Log.d(TAG, "Doc3: ${doc3ImageName ?: "Keep existing"}")
            Log.d(TAG, "Doc4: ${doc4ImageName ?: "Keep existing"}")
            Log.d(TAG, "Doc5: ${doc5ImageName ?: "Keep existing"}")

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

            if (updateRequest.driverName.isNotBlank()) {
                updateFields.add("D_DriverName = ?")
                parameters.add(updateRequest.driverName)
            }

            if (updateRequest.driverMobile.isNotBlank()) {
                val mobileNumber = updateRequest.driverMobile.toLongOrNull()
                if (mobileNumber != null) {
                    updateFields.add("D_DriverMobile = ?")
                    parameters.add(mobileNumber)
                }
            }

            if (updateRequest.invoiceNo.isNotBlank()) {
                updateFields.add("D_InvoiceNo = ?")
                parameters.add(updateRequest.invoiceNo)
            }

            if (updateRequest.quantity.isNotBlank()) {
                val qty = updateRequest.quantity.toDoubleOrNull()
                if (qty != null && qty > 0) {
                    updateFields.add("D_Qty = ?")
                    parameters.add(java.math.BigDecimal(qty))
                }
            }

            if (doc1ImageName != null) {
                updateFields.add("D_Doc1 = ?")
                parameters.add(doc1ImageName)
            }

            if (doc2ImageName != null) {
                updateFields.add("D_Doc2 = ?")
                parameters.add(doc2ImageName)
            }

            if (doc3ImageName != null) {
                updateFields.add("D_Doc3 = ?")
                parameters.add(doc3ImageName)
            }

            if (doc4ImageName != null) {
                updateFields.add("D_Doc4 = ?")
                parameters.add(doc4ImageName)
            }

            if (doc5ImageName != null) {
                updateFields.add("D_Doc5 = ?")
                parameters.add(doc5ImageName)
            }

            updateFields.add("D_Status = ?")
            parameters.add("Y")

            if (updateRequest.updateBy.isNotBlank()) {
                updateFields.add("D_UpdateBy = ?")
                parameters.add(updateRequest.updateBy)
            }

            // FIXED: Directly pass the date string without parsing
            if (updateRequest.updateDate.isNotBlank()) {
                updateFields.add("D_UpdateDate = ?")
                parameters.add(updateRequest.updateDate) // Pass as-is
                Log.d(TAG, "Update Date: ${updateRequest.updateDate}")
            }

            if (updateFields.isEmpty()) {
                Log.e(TAG, "No fields to update")
                return@withContext UpdateResult(false, "No fields to update")
            }

            val query = """
            UPDATE Vipl_ETDispatch 
            SET ${updateFields.joinToString(", ")}
            WHERE D_Code = ? AND D_TruckNo = ?
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

            Log.d(TAG, "Executing update...")
            val rowsAffected = statement.executeUpdate()

            if (rowsAffected > 0) {
                Log.d(TAG, "✅ Gate Entry updated successfully, rows affected: $rowsAffected")
                UpdateResult(true, "Gate Entry updated successfully!")
            } else {
                Log.e(TAG, "❌ No rows updated")
                UpdateResult(false, "Failed to update gate entry")
            }

        } catch (e: SQLException) {
            Log.e(TAG, "SQL Error: ${e.message}", e)
            UpdateResult(false, "Database error: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "Error: ${e.message}", e)
            UpdateResult(false, "Error: ${e.message}")
        } finally {
            try {
                statement?.close()
                connection?.close()
            } catch (e: Exception) {
                Log.e(TAG, "Close Error: ${e.message}")
            }
        }
    }
    suspend fun getTruckDispatchData(truckNo: String): TruckDispatchData? = withContext(Dispatchers.IO) {
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
                    CAST(d.D_Code AS VARCHAR) as D_Code,
                    d.D_TruckNo,
                    d.D_DriverName,
                    d.D_DriverMobile,
                    d.D_InvoiceNo,
                    CAST(d.D_Qty AS VARCHAR) as D_Qty,
                    ISNULL(am.account_name, '') as party_name,
                    ISNULL(bm.account_name, '') as broker_name,
                    d.D_Doc1,
                    d.D_Doc2,
                    d.D_Doc3,
                    d.D_Doc4,
                    d.D_Doc5
                FROM Vipl_ETDispatch d
                LEFT JOIN VIPL_Accounts_master am ON am.account_id = d.D_PartyCode
                LEFT JOIN VIPL_Accounts_master bm ON bm.account_id = d.D_BrokerCode
                WHERE d.D_TruckNo = ? AND ISNULL(d.D_Status, 'N') = 'N'
                ORDER BY d.D_Code DESC
            """.trimIndent()

            statement = connection.prepareStatement(query)
            statement.setString(1, truckNo)
            statement.queryTimeout = 10000
            resultSet = statement.executeQuery()

            if (resultSet.next()) {
                val data = TruckDispatchData(
                    dCode = resultSet.getString("D_Code")?.trim() ?: "",
                    truckNo = resultSet.getString("D_TruckNo")?.trim() ?: "",
                    driverName = resultSet.getString("D_DriverName")?.trim() ?: "",
                    driverMobile = resultSet.getString("D_DriverMobile")?.trim() ?: "",
                    invoiceNo = resultSet.getString("D_InvoiceNo")?.trim() ?: "",
                    quantity = resultSet.getString("D_Qty")?.trim() ?: "",
                    partyName = resultSet.getString("party_name")?.trim() ?: "",
                    brokerName = resultSet.getString("broker_name")?.trim() ?: "",
                    doc1 = resultSet.getString("D_Doc1")?.trim() ?: "",
                    doc2 = resultSet.getString("D_Doc2")?.trim() ?: "",
                    doc3 = resultSet.getString("D_Doc3")?.trim() ?: "",
                    doc4 = resultSet.getString("D_Doc4")?.trim() ?: "",
                    doc5 = resultSet.getString("D_Doc5")?.trim() ?: ""
                )
                return@withContext data
            } else {
                return@withContext null
            }

        } catch (e: SQLException) {
            Log.e(TAG, "SQL Exception: ${e.message}")
            e.printStackTrace()
        } catch (e: Exception) {
            Log.e(TAG, "Exception: ${e.message}")
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
                Log.e(TAG, "Database connection failed")
                return@withContext emptyList()
            }

            val query = """
                SELECT DISTINCT D_TruckNo 
                FROM Vipl_ETDispatch 
                WHERE ISNULL(D_Status, 'N') = 'N'
                ORDER BY D_TruckNo
            """.trimIndent()

            statement = connection.prepareStatement(query)
            statement.queryTimeout = 10000
            resultSet = statement.executeQuery()

            while (resultSet.next()) {
                val truckNo = resultSet.getString("D_TruckNo")?.trim() ?: ""
                if (truckNo.isNotEmpty()) {
                    trucks.add(truckNo)
                }
            }

            Log.d(TAG, "✅ Total Trucks: ${trucks.size}")

        } catch (e: SQLException) {
            Log.e(TAG, "SQL Exception: ${e.message}")
            e.printStackTrace()
        } catch (e: Exception) {
            Log.e(TAG, "Exception: ${e.message}")
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
        return@withContext trucks
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
                Log.e(TAG, "Database connection failed")
                return@withContext emptyList()
            }

            val query = """
                SELECT M_Code, M_Name 
                FROM VIPL_ModeMaster 
                ORDER BY M_Name
            """.trimIndent()

            statement = connection.prepareStatement(query)
            statement.queryTimeout = 10000
            resultSet = statement.executeQuery()

            while (resultSet.next()) {
                val code = resultSet.getString("M_Code")?.trim() ?: ""
                val name = resultSet.getString("M_Name")?.trim() ?: ""

                if (code.isNotEmpty() && name.isNotEmpty()) {
                    modeList.add(ModeMasterData(code = code, name = name))
                }
            }

            modeListCache = modeList
            Log.d(TAG, "✅ Loaded ${modeList.size} modes")

        } catch (e: SQLException) {
            Log.e(TAG, "SQL Exception: ${e.message}")
            e.printStackTrace()
        } catch (e: Exception) {
            Log.e(TAG, "Exception: ${e.message}")
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
        return@withContext modeList
    }

    suspend fun getImageApiUrl(): String? = withContext(Dispatchers.IO) {
        if (imageApiUrl != null) {
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
                Log.e(TAG, "Database connection failed")
                return@withContext null
            }

            val query = "SELECT C_IMG_DOW_API, C_IMAGE_API FROM VIPL_sys1"
            statement = connection.prepareStatement(query)
            statement.queryTimeout = 10000
            resultSet = statement.executeQuery()

            if (resultSet.next()) {
                imageApiUrl = resultSet.getString("C_IMG_DOW_API")?.trim()
                imageUploadApiUrl = resultSet.getString("C_IMAGE_API")?.trim()
                return@withContext imageApiUrl
            }

        } catch (e: SQLException) {
            Log.e(TAG, "SQL Exception: ${e.message}")
            e.printStackTrace()
        } catch (e: Exception) {
            Log.e(TAG, "Exception: ${e.message}")
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

    fun clearCache() {
        modeListCache = null
        imageApiUrl = null
        imageUploadApiUrl = null
        Log.d(TAG, "All caches cleared")
    }
}