package com.visibleinfotech.e_ethanolUnloading.dispatch

import android.content.Context
import android.util.Log
import com.visibleinfotech.e_ethanolUnloading.MainConstant
import com.visibleinfotech.e_ethanolUnloading.dispatch.models.DispatchRecord
import com.visibleinfotech.e_ethanolUnloading.dispatch.models.DispatchRequest
import com.visibleinfotech.e_ethanolUnloading.dispatch.models.DispatchResult
import com.visibleinfotech.e_ethanolUnloading.dispatch.models.Party
import com.visibleinfotech.e_ethanolUnloading.dispatch.models.PartyAllocation
import com.visibleinfotech.e_ethanolUnloading.sqlConnection.ConnectionStr
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.SQLException

class DispatchRepository(
    private val context: Context,
) {
    companion object {
        private const val TAG = "DispatchRepositoryLog"
    }

    private val mainConstant = MainConstant(context)

    suspend fun getAllParties(): List<Party> {
        return withContext(Dispatchers.IO) {
            var connection: Connection? = null
            var statement: PreparedStatement? = null
            var resultSet: ResultSet? = null
            val partyList = mutableListOf<Party>()

            try {
                Log.d(TAG, "=========== GET ALL PARTIES START ===========")
                val t0 = System.currentTimeMillis()

                val conStr = ConnectionStr()
                connection = conStr.connectionclasss(
                    mainConstant.getDbUser(),
                    mainConstant.getDbPass(),
                    mainConstant.getDbName(),
                    mainConstant.getIp(),
                    mainConstant.getInstance()
                )

                if (connection == null) {
                    Log.e(TAG, "❌ Database connection failed")
                    return@withContext emptyList()
                }
                Log.d(TAG, "   ✅ DB connected")

                val query = """
                SELECT b.account_id,
                       b.account_name,
                       c.place_name,
                       c.place_id
                FROM VIPL_CanePlotSurvey a
                INNER JOIN Vipl_Accounts_master b ON a.PL_GrowerCode = b.account_id
                INNER JOIN Vipl_place_master   c ON c.place_id = b.place_id
                WHERE b.G_Approve='Y' AND b.G_Enable=1 AND b.G_AcType='P'
                GROUP BY b.account_id, b.account_name, c.place_name, c.place_id
                ORDER BY b.account_name ASC
            """.trimIndent()

                Log.d(TAG, "   Query: $query")
                statement = connection.prepareStatement(query)
                resultSet = statement.executeQuery()

                while (resultSet.next()) {
                    partyList.add(
                        Party(
                            accountId   = resultSet.getString("account_id")?.trim() ?: "",
                            accountName = resultSet.getString("account_name")?.trim() ?: "",
                            placeName   = resultSet.getString("place_name")?.trim() ?: "",
                            placeId     = resultSet.getString("place_id")?.trim() ?: "",
                            brokerCode  = "",
                            brokerName  = ""
                        )
                    )
                }

                Log.d(TAG, "   ✅ Total parties loaded: ${partyList.size}")
                Log.d(TAG, "   ⏱ Elapsed: ${System.currentTimeMillis() - t0} ms")
                Log.d(TAG, "=========== GET ALL PARTIES END ===========")
                partyList

            } catch (e: SQLException) {
                Log.e(TAG, "❌ SQL Error in getAllParties: ${e.message}", e)
                emptyList()
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error in getAllParties: ${e.message}", e)
                emptyList()
            } finally {
                try {
                    resultSet?.close()
                    statement?.close()
                    connection?.close()
                    Log.d(TAG, "   🔒 Resources closed")
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Close Error in getAllParties: ${e.message}")
                }
            }
        }
    }

    suspend fun getBrokersForParty(partyAccountId: String): List<Party> {
        return withContext(Dispatchers.IO) {
            var connection: Connection? = null
            var statement: PreparedStatement? = null
            var resultSet: ResultSet? = null
            val brokerList = mutableListOf<Party>()

            try {
                Log.d(TAG, "=========== GET BROKERS FOR PARTY START ===========")
                Log.d(TAG, "   partyAccountId = $partyAccountId")
                val t0 = System.currentTimeMillis()

                val conStr = ConnectionStr()
                connection = conStr.connectionclasss(
                    mainConstant.getDbUser(),
                    mainConstant.getDbPass(),
                    mainConstant.getDbName(),
                    mainConstant.getIp(),
                    mainConstant.getInstance()
                )

                if (connection == null) {
                    Log.e(TAG, "❌ [Brokers] DB connection failed")
                    return@withContext emptyList()
                }
                Log.d(TAG, "   ✅ [Brokers] DB connected")

                val query = """
                    SELECT a.PL_BrokerCode,
                       d.account_name AS broker_name,
                       b.account_id,
                       b.account_name,
                       c.place_name,
                       c.place_id
                    FROM VIPL_CanePlotSurvey a
                    INNER JOIN Vipl_Accounts_master b on a.PL_GrowerCode=b.account_id
                    INNER JOIN Vipl_place_master c ON c.place_id = b.place_id
                    INNER JOIN Vipl_Accounts_master d on d.account_id=a.PL_BrokerCode
                    WHERE a.PL_GrowerCode=? and b.G_Approve='Y' AND b.G_Enable=1 and b.G_AcType='P' and isnull(PL_PlotEndType,0)=0
                    group by b.account_id,b.account_name,c.place_name,c.place_id,a.PL_BrokerCode,d.account_name
                    ORDER BY b.account_name ASC
            """.trimIndent()

                Log.d(TAG, "   [Brokers] Query: $query")
                statement = connection.prepareStatement(query)
                statement.setString(1, partyAccountId)
                resultSet = statement.executeQuery()

                while (resultSet.next()) {
                    brokerList.add(
                        Party(
                            accountId   = resultSet.getString("account_id")?.trim() ?: "",
                            accountName = resultSet.getString("account_name")?.trim() ?: "",
                            placeName   = resultSet.getString("place_name")?.trim() ?: "",
                            placeId     = resultSet.getString("place_id")?.trim() ?: "",
                            brokerCode  = resultSet.getString("PL_BrokerCode")?.trim() ?: "",
                            brokerName  = resultSet.getString("broker_name")?.trim() ?: ""
                        )
                    )
                }

                Log.d(TAG, "   [Brokers] ✅ Total brokers loaded: ${brokerList.size}")
                Log.d(TAG, "   [Brokers] ⏱ Elapsed: ${System.currentTimeMillis() - t0} ms")
                Log.d(TAG, "=========== GET BROKERS FOR PARTY END ===========")
                brokerList

            } catch (e: Exception) {
                Log.e(TAG, "❌ [Brokers] Error: ${e.message}", e)
                emptyList()
            } finally {
                try {
                    resultSet?.close()
                    statement?.close()
                    connection?.close()
                    Log.d(TAG, "   [Brokers] 🔒 Resources closed")
                } catch (e: Exception) {
                    Log.e(TAG, "❌ [Brokers] Close Error: ${e.message}")
                }
            }
        }
    }

    suspend fun submitDispatch(dispatchRequest: DispatchRequest): DispatchResult {
        return withContext(Dispatchers.IO) {
            var connection: Connection? = null
            var statement: PreparedStatement? = null

            try {
                Log.d(TAG, "=========== SUBMIT DISPATCH START ===========")
                Log.d(TAG, "Party ID: ${dispatchRequest.partyId}")
                Log.d(TAG, "Broker ID: ${dispatchRequest.brokerId}")
                Log.d(TAG, "Vehicle: ${dispatchRequest.vehicleNumber}")
                Log.d(TAG, "Invoice: ${dispatchRequest.invoiceNumber}")
                Log.d(TAG, "Quantity: ${dispatchRequest.dispatchQuantity}")
                Log.d(TAG, "Anugya: ${dispatchRequest.anugyaNumber}")
                val t0 = System.currentTimeMillis()

                val conStr = ConnectionStr()
                connection = conStr.connectionclasss(
                    mainConstant.getDbUser(),
                    mainConstant.getDbPass(),
                    mainConstant.getDbName(),
                    mainConstant.getIp(),
                    mainConstant.getInstance()
                )

                if (connection == null) {
                    Log.e(TAG, "❌ Database connection failed")
                    return@withContext DispatchResult(false, "Database connection failed")
                }
                Log.d(TAG, "   ✅ DB connected")

                val userName = mainConstant.getUsername()
                Log.d(TAG, "   UserName: $userName")

                val brokerCodeNumeric = dispatchRequest.brokerId.toLongOrNull() ?: 0
                val partyCodeNumeric = dispatchRequest.partyId.toLongOrNull() ?: 0
                val driverMobileNumeric = if (dispatchRequest.driverMobileNumber.isNotEmpty()) {
                    dispatchRequest.driverMobileNumber.toLongOrNull()
                } else {
                    null
                }

                val anugyaNoValue: String? = dispatchRequest.anugyaNumber
                    .takeIf { it.isNotBlank() }

                Log.d(TAG, "   brokerCodeNumeric   = $brokerCodeNumeric")
                Log.d(TAG, "   partyCodeNumeric    = $partyCodeNumeric")
                Log.d(TAG, "   driverMobileNumeric = $driverMobileNumeric")
                Log.d(TAG, "   Anugya (D_DONo1)    = [${anugyaNoValue ?: "<null>"}]")

                val insertQuery = """
                INSERT INTO Vipl_ETDispatch (
                    D_Code, D_BrokerCode, D_PartyCode, D_TruckNo, D_DriverName, 
                    D_DriverMobile, D_InvoiceNo, D_Qty, D_Date, D_User, D_Status, D_DONo1,
                    D_InvoiceDate, D_Doc1, D_Doc2, D_Doc3, D_Doc4, D_Doc5
                ) 
                SELECT 
                    ISNULL(MAX(D_Code), 0) + 1 AS D_Code, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?
                FROM Vipl_ETDispatch
            """.trimIndent()

                Log.d(TAG, "   Query: $insertQuery")

                statement = connection.prepareStatement(insertQuery)

                statement.setLong(1, brokerCodeNumeric)
                statement.setLong(2, partyCodeNumeric)
                statement.setString(3, dispatchRequest.vehicleNumber)
                statement.setString(4, dispatchRequest.driverName)

                if (driverMobileNumeric != null) {
                    statement.setLong(5, driverMobileNumeric)
                } else {
                    statement.setNull(5, java.sql.Types.INTEGER)
                }

                statement.setString(6, dispatchRequest.invoiceNumber)
                statement.setBigDecimal(7, java.math.BigDecimal(dispatchRequest.dispatchQuantity))
                statement.setString(8, dispatchRequest.dateTime)
                statement.setString(9, userName)
                statement.setString(10, "N")

                if (anugyaNoValue != null) {
                    statement.setString(11, anugyaNoValue)
                } else {
                    statement.setNull(11, java.sql.Types.VARCHAR)
                }

                if (dispatchRequest.invoiceDate.isNotBlank()) {
                    statement.setString(12, dispatchRequest.invoiceDate)
                    Log.d(TAG, "   ✅ D_InvoiceDate = ${dispatchRequest.invoiceDate}")
                } else {
                    statement.setNull(12, java.sql.Types.VARCHAR)
                    Log.d(TAG, "   ⚠ D_InvoiceDate = NULL")
                }

                val anugyaImageName = uploadImageWithProgress(
                    dispatchRequest.anugyaImageUri, "Anugya"
                ) { p, s -> Log.d(TAG, "  Anugya: $p% $s") }

                val biltyImageName = uploadImageWithProgress(
                    dispatchRequest.biltyImageUri, "Bilty"
                ) { p, s -> Log.d(TAG, "  Bilty: $p% $s") }

                val invoiceImageName = uploadImageWithProgress(
                    dispatchRequest.invoiceImageUri, "Invoice"
                ) { p, s -> Log.d(TAG, "  Invoice: $p% $s") }

                val weighmentImageName = uploadImageWithProgress(
                    dispatchRequest.weighmentSlipImageUri, "Weighment"
                ) { p, s -> Log.d(TAG, "  Weighment: $p% $s") }

                val otherDocImageName = uploadImageWithProgress(
                    dispatchRequest.otherDocumentImageUri, "Other"
                ) { p, s -> Log.d(TAG, "  Other: $p% $s") }

                if (anugyaImageName != null) statement.setString(13, anugyaImageName)
                else statement.setNull(13, java.sql.Types.VARCHAR)

                if (biltyImageName != null) statement.setString(14, biltyImageName)
                else statement.setNull(14, java.sql.Types.VARCHAR)

                if (invoiceImageName != null) statement.setString(15, invoiceImageName)
                else statement.setNull(15, java.sql.Types.VARCHAR)

                if (weighmentImageName != null) statement.setString(16, weighmentImageName)
                else statement.setNull(16, java.sql.Types.VARCHAR)

                if (otherDocImageName != null) statement.setString(17, otherDocImageName)
                else statement.setNull(17, java.sql.Types.VARCHAR)

                val rowsAffected = statement.executeUpdate()
                Log.d(TAG, "   rowsAffected = $rowsAffected")

                if (rowsAffected > 0) {
                    Log.d(TAG, "✅ Dispatch submitted successfully")
                    Log.d(TAG, "   ⏱ Elapsed: ${System.currentTimeMillis() - t0} ms")
                    Log.d(TAG, "=========== SUBMIT DISPATCH END ===========")
                    DispatchResult(true, "Dispatch submitted successfully!")
                } else {
                    Log.e(TAG, "❌ No rows inserted")
                    DispatchResult(false, "Failed to submit dispatch")
                }

            } catch (e: SQLException) {
                Log.e(TAG, "❌ SQL Error in submitDispatch: ${e.message}", e)
                DispatchResult(false, "Database error: ${e.message}")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error in submitDispatch: ${e.message}", e)
                DispatchResult(false, "Error: ${e.message}")
            } finally {
                try {
                    statement?.close()
                    connection?.close()
                    Log.d(TAG, "   🔒 Resources closed")
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Close Error in submitDispatch: ${e.message}")
                }
            }
        }
    }

    suspend fun getPartyAllocation(
        partyAccountId: String,
        brokerAccountId: String,
    ): PartyAllocation? {
        return withContext(Dispatchers.IO) {
            var connection: Connection? = null
            var statement: PreparedStatement? = null
            var resultSet: ResultSet? = null

            try {
                Log.d(TAG, "=========== GET PARTY ALLOCATION START ===========")
                Log.d(TAG, "Party: $partyAccountId, Broker: $brokerAccountId")
                val t0 = System.currentTimeMillis()

                val conStr = ConnectionStr()
                connection = conStr.connectionclasss(
                    mainConstant.getDbUser(),
                    mainConstant.getDbPass(),
                    mainConstant.getDbName(),
                    mainConstant.getIp(),
                    mainConstant.getInstance()
                )

                if (connection == null) {
                    Log.e(TAG, "❌ Database connection failed")
                    return@withContext null
                }
                Log.d(TAG, "   ✅ DB connected")

                val existenceQuery = """
                SELECT COUNT(*) AS cnt
                FROM VIPL_CanePlotSurvey
                WHERE PL_Enable = 1 AND PL_Approve='Y'
                  AND PL_BrokerCode = ?
                  AND PL_GrowerCode = ?
            """.trimIndent()

                Log.d(TAG, "   Existence Query: $existenceQuery")
                statement = connection.prepareStatement(existenceQuery)
                statement.setString(1, brokerAccountId)
                statement.setString(2, partyAccountId)
                resultSet = statement.executeQuery()

                var rowCount = 0
                if (resultSet.next()) {
                    rowCount = resultSet.getInt("cnt")
                }
                resultSet.close()
                statement.close()

                Log.d(TAG, "   → Rows found in CanePlotSurvey = $rowCount")

                if (rowCount == 0) {
                    Log.w(TAG, "   ⚠ No sauda entry found for party=$partyAccountId broker=$brokerAccountId")
                    Log.d(TAG, "=========== GET PARTY ALLOCATION END (NO ENTRY) ===========")
                    return@withContext null
                }

                val query1 = """
                    SELECT ISNULL(sum(PL_Area), 0)  + SUM(ISNULL(PL_ExtraAllow, 0)) AS PL_EstTons FROM VIPL_CanePlotSurvey a 
                    WHERE PL_Enable = 1 AND PL_Approve = 'Y' AND PL_BrokerCode = ? AND PL_GrowerCode = ?
            """.trimIndent()

                Log.d(TAG, "   Survey Query: $query1")

                statement = connection.prepareStatement(query1)
                statement.setString(1, brokerAccountId)
                statement.setString(2, partyAccountId)
                resultSet = statement.executeQuery()

                var plEstTons = 0.0
                if (resultSet.next()) {
                    plEstTons = resultSet.getDouble("PL_EstTons")
                }

                resultSet.close()
                statement.close()

                Log.d(TAG, "   plEstTons  = ${String.format("%.2f", plEstTons)}")

                val query2 = """
    SELECT SUM(RecQty) AS D_Qty
    FROM (
        SELECT ISNULL(SUM(b.Pl_InvoiceWt), 0) AS RecQty
        FROM VIPL_CanePlotSurvey a
        INNER JOIN VIPL_Indent b ON a.PL_PLOTID = b.I_PlotNumber
        WHERE a.PL_Enable = 1
          AND b.I_Enable = 1
          AND a.PL_GrowerCode = ?
          AND a.PL_BrokerCode = ?
        UNION ALL
        SELECT ISNULL(SUM(D_Qty), 0) AS RecQty
        FROM Vipl_ETDispatch
        WHERE D_Status = 'N'
          AND D_BrokerCode = ?
          AND D_PartyCode = ?
    ) main
""".trimIndent()

                Log.d(TAG, "   Dispatch Query: $query2")

                statement = connection.prepareStatement(query2)
                statement.setLong(1, partyAccountId.toLongOrNull() ?: 0L)
                statement.setLong(2, brokerAccountId.toLongOrNull() ?: 0L)
                statement.setLong(3, brokerAccountId.toLongOrNull() ?: 0L)
                statement.setLong(4, partyAccountId.toLongOrNull() ?: 0L)
                resultSet = statement.executeQuery()

                var dispatchedQty = 0.0
                if (resultSet.next()) {
                    dispatchedQty = resultSet.getDouble("D_Qty")
                }

                resultSet.close()
                statement.close()

                Log.d(TAG, "   D_Qty (already dispatched) = ${String.format("%.2f", dispatchedQty)}")

                val finalAvailableQuantity = plEstTons - dispatchedQty
                val isValid = finalAvailableQuantity > 0

                Log.d(TAG, "   Available  = ${String.format("%.2f", finalAvailableQuantity)} " +
                        "(= ${String.format("%.2f", plEstTons)} - ${String.format("%.2f", dispatchedQty)})")
                Log.d(TAG, "   isValid    = $isValid")
                Log.d(TAG, "   ⏱ Elapsed: ${System.currentTimeMillis() - t0} ms")
                Log.d(TAG, "=========== GET PARTY ALLOCATION END ===========")

                return@withContext PartyAllocation(
                    plEstTons = plEstTons,
                    plWeigh = 0.0,
                    dQty = dispatchedQty,
                    finalAvailableQuantity = finalAvailableQuantity,
                    isValid = isValid
                )

            } catch (e: SQLException) {
                Log.e(TAG, "❌ SQL Error in getPartyAllocation: ${e.message}", e)
                return@withContext null
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error in getPartyAllocation: ${e.message}", e)
                return@withContext null
            } finally {
                try {
                    resultSet?.close()
                    statement?.close()
                    connection?.close()
                    Log.d(TAG, "   🔒 Resources closed")
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Close Error: ${e.message}")
                }
            }
        }
    }

    suspend fun getPartyAllocation2(
        partyAccountId: String,
        brokerAccountId: String,
    ): PartyAllocation? {
        return withContext(Dispatchers.IO) {
            var connection: Connection? = null
            var statement: PreparedStatement? = null
            var resultSet: ResultSet? = null

            try {
                Log.d(TAG, "=========== GET PARTY ALLOCATION 2 (ALL STATUSES) START ===========")
                Log.d(TAG, "Party: $partyAccountId, Broker: $brokerAccountId")
                val t0 = System.currentTimeMillis()

                val conStr = ConnectionStr()
                connection = conStr.connectionclasss(
                    mainConstant.getDbUser(),
                    mainConstant.getDbPass(),
                    mainConstant.getDbName(),
                    mainConstant.getIp(),
                    mainConstant.getInstance()
                )

                if (connection == null) {
                    Log.e(TAG, "❌ [M2] Database connection failed")
                    return@withContext null
                }
                Log.d(TAG, "   ✅ [M2] DB connected")

                // existence — ANY sauda status
                val existenceQuery = """
                SELECT COUNT(*) AS cnt
                FROM VIPL_CanePlotSurvey
                WHERE PL_Enable = 1
                  AND PL_BrokerCode = ?
                  AND PL_GrowerCode = ?
            """.trimIndent()

                Log.d(TAG, "   [M2] Existence Query: $existenceQuery")
                statement = connection.prepareStatement(existenceQuery)
                statement.setString(1, brokerAccountId)
                statement.setString(2, partyAccountId)
                resultSet = statement.executeQuery()

                var rowCount = 0
                if (resultSet.next()) {
                    rowCount = resultSet.getInt("cnt")
                }
                resultSet.close()
                statement.close()

                Log.d(TAG, "   [M2] → Rows found (any approve status) = $rowCount")

                if (rowCount == 0) {
                    Log.w(TAG, "   [M2] ⚠ No sauda entry found (even unapproved) party=$partyAccountId broker=$brokerAccountId")
                    Log.d(TAG, "=========== GET PARTY ALLOCATION 2 END (NO ENTRY) ===========")
                    return@withContext null
                }

                // sum — ANY sauda status
                val query1 = """
                SELECT 
                    SUM(ISNULL(PL_EstTons, 0) + ISNULL(PL_ExtraWt, PL_EstTons * 5 / 100)) 
                        + SUM(ISNULL(PL_ExtraAllow, 0)) AS PL_EstTons
                FROM VIPL_CanePlotSurvey a 
                WHERE PL_Enable = 1
                  AND PL_BrokerCode = ?
                  AND PL_GrowerCode = ?
            """.trimIndent()

                Log.d(TAG, "   [M2] Survey Query: $query1")
                statement = connection.prepareStatement(query1)
                statement.setString(1, brokerAccountId)
                statement.setString(2, partyAccountId)
                resultSet = statement.executeQuery()

                var plEstTons = 0.0
                if (resultSet.next()) {
                    plEstTons = resultSet.getDouble("PL_EstTons")
                }
                resultSet.close()
                statement.close()

                Log.d(TAG, "   [M2] plEstTons = ${String.format("%.2f", plEstTons)}")

                val query2 = """
                SELECT SUM(ISNULL(D_Qty, 0)) AS D_Qty 
                FROM Vipl_ETDispatch 
                WHERE D_BrokerCode = ? 
                  AND D_PartyCode = ?
            """.trimIndent()

                Log.d(TAG, "   [M2] Dispatch Query: $query2")
                statement = connection.prepareStatement(query2)
                statement.setLong(1, brokerAccountId.toLongOrNull() ?: 0L)
                statement.setLong(2, partyAccountId.toLongOrNull() ?: 0L)
                resultSet = statement.executeQuery()

                var dispatchedQty = 0.0
                if (resultSet.next()) {
                    dispatchedQty = resultSet.getDouble("D_Qty")
                }
                resultSet.close()
                statement.close()

                Log.d(TAG, "   [M2] D_Qty = ${String.format("%.2f", dispatchedQty)}")

                val finalAvailableQuantity = plEstTons - dispatchedQty
                val isValid = finalAvailableQuantity > 0

                Log.d(TAG, "   [M2] Available = ${String.format("%.2f", finalAvailableQuantity)} " +
                        "(= ${String.format("%.2f", plEstTons)} - ${String.format("%.2f", dispatchedQty)})")
                Log.d(TAG, "   [M2] isValid = $isValid")
                Log.d(TAG, "   [M2] ⏱ Elapsed: ${System.currentTimeMillis() - t0} ms")
                Log.d(TAG, "=========== GET PARTY ALLOCATION 2 END ===========")

                return@withContext PartyAllocation(
                    plEstTons = plEstTons,
                    plWeigh = 0.0,
                    dQty = dispatchedQty,
                    finalAvailableQuantity = finalAvailableQuantity,
                    isValid = isValid
                )

            } catch (e: SQLException) {
                Log.e(TAG, "❌ [M2] SQL Error: ${e.message}", e)
                return@withContext null
            } catch (e: Exception) {
                Log.e(TAG, "❌ [M2] Error: ${e.message}", e)
                return@withContext null
            } finally {
                try {
                    resultSet?.close()
                    statement?.close()
                    connection?.close()
                    Log.d(TAG, "   [M2] 🔒 Resources closed")
                } catch (e: Exception) {
                    Log.e(TAG, "❌ [M2] Close Error: ${e.message}")
                }
            }
        }
    }

    // ============================================================
    //  WEIGHT LIMIT
    // ============================================================
    suspend fun getWeightLimit(): Double {
        return withContext(Dispatchers.IO) {
            var connection: Connection? = null
            var statement: PreparedStatement? = null
            var resultSet: ResultSet? = null

            try {
                Log.d(TAG, "=========== GET WEIGHT LIMIT START ===========")

                val conStr = ConnectionStr()
                connection = conStr.connectionclasss(
                    mainConstant.getDbUser(),
                    mainConstant.getDbPass(),
                    mainConstant.getDbName(),
                    mainConstant.getIp(),
                    mainConstant.getInstance()
                )

                if (connection == null) {
                    Log.e(TAG, "❌ DB connection failed, default = 50.0")
                    return@withContext 50.0
                }

                val query = "SELECT ISNULL(C_Wtlimit, 50) AS C_Wtlimit FROM VIPL_Sys1"
                Log.d(TAG, "   Query: $query")
                statement = connection.prepareStatement(query)
                resultSet = statement.executeQuery()

                var weightLimit = 50.0
                if (resultSet.next()) {
                    weightLimit = resultSet.getDouble("C_Wtlimit")
                }

                Log.d(TAG, "   Weight limit = $weightLimit")
                Log.d(TAG, "=========== GET WEIGHT LIMIT END ===========")
                weightLimit

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error in getWeightLimit: ${e.message}", e)
                50.0
            } finally {
                try {
                    resultSet?.close()
                    statement?.close()
                    connection?.close()
                    Log.d(TAG, "   🔒 Resources closed")
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Close Error: ${e.message}")
                }
            }
        }
    }

    // ============================================================
    //  TRUCK IN USE
    // ============================================================
    suspend fun isTruckAlreadyInUse(truckNo: String): Boolean {
        return withContext(Dispatchers.IO) {
            var connection: Connection? = null
            var statement: PreparedStatement? = null
            var resultSet: ResultSet? = null

            try {
                Log.d(TAG, "=========== IS TRUCK IN USE START ===========")
                Log.d(TAG, "   TruckNo: $truckNo")

                if (truckNo.isBlank()) {
                    Log.w(TAG, "   ⚠ Blank truck → false")
                    return@withContext false
                }

                val conStr = ConnectionStr()
                connection = conStr.connectionclasss(
                    mainConstant.getDbUser(),
                    mainConstant.getDbPass(),
                    mainConstant.getDbName(),
                    mainConstant.getIp(),
                    mainConstant.getInstance()
                )

                if (connection == null) {
                    Log.e(TAG, "❌ DB connection failed → false")
                    return@withContext false
                }

                val query = """
                    SELECT Gr_TruckNo 
                    FROM VIPL_GROSS 
                    WHERE Gr_TruckNo = ? AND GR_TARE_FLAG = 0
                """.trimIndent()

                Log.d(TAG, "   Query: $query")
                statement = connection.prepareStatement(query)
                statement.setString(1, truckNo)
                resultSet = statement.executeQuery()

                val inUse = resultSet.next()
                Log.d(TAG, "   inUse = $inUse")
                Log.d(TAG, "=========== IS TRUCK IN USE END ===========")
                inUse

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error in isTruckAlreadyInUse: ${e.message}", e)
                false
            } finally {
                try {
                    resultSet?.close()
                    statement?.close()
                    connection?.close()
                    Log.d(TAG, "   🔒 Resources closed")
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Close Error: ${e.message}")
                }
            }
        }
    }

    // ============================================================
    //  DISPATCH RECORDS
    // ============================================================
    suspend fun getDispatchRecords(): List<DispatchRecord> {
        return withContext(Dispatchers.IO) {
            var connection: Connection? = null
            var statement: PreparedStatement? = null
            var resultSet: ResultSet? = null

            val recordList = mutableListOf<DispatchRecord>()

            try {
                Log.d(TAG, "=========== GET DISPATCH RECORDS START ===========")
                val t0 = System.currentTimeMillis()

                val conStr = ConnectionStr()
                connection = conStr.connectionclasss(
                    mainConstant.getDbUser(),
                    mainConstant.getDbPass(),
                    mainConstant.getDbName(),
                    mainConstant.getIp(),
                    mainConstant.getInstance()
                )

                if (connection == null) {
                    Log.e(TAG, "❌ Database connection failed")
                    return@withContext emptyList()
                }
                Log.d(TAG, "   ✅ DB connected")

                val brokerName = mainConstant.getUsername()
                Log.d(TAG, "   Loading dispatch records for broker: $brokerName")

                val query = """
                SELECT CAST(d.D_Code AS VARCHAR(20))   AS D_Code,
                    CAST(d.D_BrokerCode AS VARCHAR(20)) AS D_BrokerCode,
                    ISNULL(bm.account_name, '')     AS broker_name,
                    CAST(d.D_PartyCode AS VARCHAR(20)) AS D_PartyCode,
                    ISNULL(pm.account_name, '')     AS party_name,
                    ISNULL(d.D_TruckNo, '')         AS D_TruckNo,
                    ISNULL(d.D_DriverName, '')      AS D_DriverName,
                    ISNULL(CAST(d.D_DriverMobile AS VARCHAR(20)), '') AS D_DriverMobile,
                    ISNULL(d.D_InvoiceNo, '')       AS D_InvoiceNo,
                    CAST(ISNULL(d.D_Qty, 0) AS VARCHAR(20)) AS D_Qty,
                    ISNULL(d.D_Date, '')            AS D_Date,
                    ISNULL(d.D_User, '')            AS D_User,
                    ISNULL(d.D_Status, '')          AS D_Status
                FROM Vipl_ETDispatch d
                LEFT JOIN VIPL_Accounts_master bm ON bm.account_id = d.D_BrokerCode
                LEFT JOIN VIPL_Accounts_master pm ON pm.account_id = d.D_PartyCode
                WHERE d.D_User = ?
                ORDER BY d.D_Code DESC
            """.trimIndent()

                Log.d(TAG, "   Query: $query")

                statement = connection.prepareStatement(query)
                statement.setString(1, brokerName)
                resultSet = statement.executeQuery()

                while (resultSet.next()) {
                    val record = DispatchRecord(
                        dCode = resultSet.getString("D_Code") ?: "",
                        brokerCode = resultSet.getString("D_BrokerCode") ?: "",
                        brokerName = resultSet.getString("broker_name") ?: "",
                        partyCode = resultSet.getString("D_PartyCode") ?: "",
                        partyName = resultSet.getString("party_name") ?: "",
                        truckNo = resultSet.getString("D_TruckNo") ?: "",
                        driverName = resultSet.getString("D_DriverName") ?: "",
                        driverMobile = resultSet.getString("D_DriverMobile") ?: "",
                        invoiceNo = resultSet.getString("D_InvoiceNo") ?: "",
                        quantity = resultSet.getString("D_Qty") ?: "0",
                        date = resultSet.getString("D_Date") ?: "",
                        userName = resultSet.getString("D_User") ?: "",
                        status = resultSet.getString("D_Status") ?: ""
                    )
                    recordList.add(record)
                }

                Log.d(TAG, "   ✅ Total records loaded: ${recordList.size}")
                Log.d(TAG, "   ⏱ Elapsed: ${System.currentTimeMillis() - t0} ms")
                Log.d(TAG, "=========== GET DISPATCH RECORDS END ===========")

                return@withContext recordList

            } catch (e: SQLException) {
                Log.e(TAG, "❌ SQL Error in getDispatchRecords: ${e.message}", e)
                return@withContext emptyList()
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error in getDispatchRecords: ${e.message}", e)
                return@withContext emptyList()
            } finally {
                try {
                    resultSet?.close()
                    statement?.close()
                    connection?.close()
                    Log.d(TAG, "   🔒 Resources closed")
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Close Error in getDispatchRecords: ${e.message}")
                }
            }
        }
    }

    // ============================================================
    //  IMAGE UPLOAD
    // ============================================================
    suspend fun uploadDocumentImage(imageFile: java.io.File, documentType: String): Pair<Boolean, String> {
        return withContext(Dispatchers.IO) {
            try {
                val imageApiUrl = mainConstant.getImageApiUrl()
                if (imageApiUrl.isEmpty()) {
                    Log.e(TAG, "❌ Image API URL is empty")
                    return@withContext Pair(false, "")
                }
                if (!imageFile.exists()) {
                    Log.e(TAG, "❌ Image file does not exist: ${imageFile.absolutePath}")
                    return@withContext Pair(false, "")
                }

                val imageName = com.visibleinfotech.e_ethanolUnloading.utils.ImageUploadUtils
                    .generateImageName(documentType.capitalize())

                Log.d(TAG, "📤 Uploading $documentType image: $imageName (${imageFile.length() / 1024} KB)")

                val (success, name) = com.visibleinfotech.e_ethanolUnloading.utils.ImageUploadUtils
                    .uploadImageToApi(
                        apiUrl = imageApiUrl,
                        imageFile = imageFile,
                        siteCode = mainConstant.getSiteCode(),
                        documentType = "dispatch_$documentType",
                        imageName = imageName
                    )

                Pair(success, if (success) name else "")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error uploading $documentType: ${e.message}", e)
                Pair(false, "")
            }
        }
    }

    private suspend fun uploadImageWithProgress(
        uriString: String?,
        documentType: String,
        onProgress: (Int, String) -> Unit,
    ): String? {
        if (uriString.isNullOrEmpty()) {
            Log.d(TAG, "No image for $documentType")
            return null
        }

        return try {
            onProgress(0, "Preparing $documentType...")
            val uri = android.net.Uri.parse(uriString)
            val imageFile = com.visibleinfotech.e_ethanolUnloading.utils.ImageUploadUtils
                .getOptimizedImageFile(context, uri)

            if (imageFile != null && imageFile.exists()) {
                onProgress(40, "Uploading $documentType...")
                val uploadResult = uploadDocumentImage(imageFile, documentType)

                if (imageFile.exists()) imageFile.delete()

                if (uploadResult.first) {
                    onProgress(100, "$documentType uploaded")
                    uploadResult.second
                } else {
                    null
                }
            } else {
                Log.e(TAG, "❌ Failed to optimize $documentType")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error processing $documentType: ${e.message}", e)
            null
        }
    }
}