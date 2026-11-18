package com.visibleinfotech.e_ethanolUnloading.indentAndToken

import android.content.Context
import android.util.Log
import com.visibleinfotech.e_ethanolUnloading.MainConstant
import com.visibleinfotech.e_ethanolUnloading.sqlConnection.ConnectionStr
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.SQLException

private const val TAG = "IndentTokenRepo"

class IndentAndTokenRepository(private val context: Context) {

    private val mainConstant = MainConstant(context)
    private var pendingOptionsCache: List<TruckOption>? = null

    private fun openConnection(): Connection? = try {
        ConnectionStr().connectionclasss(
            mainConstant.getDbUser(),
            mainConstant.getDbPass(),
            mainConstant.getDbName(),
            mainConstant.getIp(),
            mainConstant.getInstance()
        )
    } catch (e: Exception) {
        Log.e(TAG, "openConnection failed: ${e.message}", e)
        null
    }

    suspend fun getPendingTruckOptions(): List<TruckOption> = withContext(Dispatchers.IO) {
        pendingOptionsCache?.let {
            Log.d(TAG, "Returning cached options: ${it.size}")
            return@withContext it
        }

        var connection: Connection? = null
        var statement: PreparedStatement? = null
        var resultSet: ResultSet? = null
        val options = mutableListOf<TruckOption>()

        try {
            connection = openConnection()
            if (connection == null) {
                Log.e(TAG, "DB connection failed")
                return@withContext emptyList()
            }

            val query = """
                SELECT G_Code, G_TruckNo
                FROM Vipl_ETGateEntry
                WHERE G_PartyCode  IS NOT NULL
                  AND G_BrokerCode IS NOT NULL
                  AND G_DO         IS NOT NULL
                  AND G_InvoiceNo  IS NOT NULL
                  AND G_InvoiceNo <> ''
                  AND G_Status     = 'N'
                  AND G_Direction  = 'Inward'
                ORDER BY G_Code DESC
            """.trimIndent()

            Log.d(TAG, "getPendingTruckOptions:\n$query")

            statement = connection.prepareStatement(query)
            statement.queryTimeout = 10000
            resultSet = statement.executeQuery()

            while (resultSet.next()) {
                val code = resultSet.getInt("G_Code")
                val truck = resultSet.getString("G_TruckNo")?.trim().orEmpty()
                if (truck.isNotEmpty()) {
                    options.add(TruckOption(code, truck))
                }
            }

            Log.d(TAG, "✅ Loaded ${options.size} pending options (no DISTINCT)")
            pendingOptionsCache = options
        } catch (e: SQLException) {
            Log.e(TAG, "SQL Exception: ${e.message}", e)
        } catch (e: Exception) {
            Log.e(TAG, "Exception: ${e.message}", e)
        } finally {
            closeQuietly(resultSet, statement, connection)
        }
        return@withContext options
    }

    suspend fun getPendingEntryByGCode(gCode: Int): PendingIndentEntry? =
        withContext(Dispatchers.IO) {
            var connection: Connection? = null
            var statement: PreparedStatement? = null
            var resultSet: ResultSet? = null

            try {
                connection = openConnection()
                if (connection == null) {
                    Log.e(TAG, "DB connection failed")
                    return@withContext null
                }

                val query = """
                    SELECT G_Code,G_TruckNo,G_PartyCode,G_BrokerCode,G_DO,CAST(G_Qty AS VARCHAR) AS G_Qty,
                    G_InvoiceNo,CAST(G_InvQty AS VARCHAR) AS G_InvQty,CAST(G_InvAmt AS VARCHAR) AS G_InvAmt,
                    G_Doc1, G_Doc2, G_Doc3, G_Doc4, G_Doc5,CONVERT(VARCHAR(19), G_Date, 120) AS G_Date,G_User,
                    b.M_Name As M_Name,G_Bags,CAST(G_InvRate AS VARCHAR) AS G_InvRate,G_Remarks,G_PartyName,
                    G_BrokerName,G_DriverName,G_DriverMobile,G_ItemCode,G_ChallanNo,
                    CONVERT(VARCHAR(19), G_ChallanDate, 120) AS G_ChallanDate,G_CapturedPhoto
FROM Vipl_ETGateEntry a inner join VIPL_ModeMaster b ON b.M_Code = a.G_ModeCode
WHERE G_Code = ? AND G_PartyCode  IS NOT NULL AND G_BrokerCode IS NOT NULL AND G_DO IS NOT NULL AND G_InvoiceNo IS NOT NULL
AND G_InvoiceNo <> '' AND G_Status     = 'N' AND G_Direction  = 'Inward'
                """.trimIndent()

                statement = connection.prepareStatement(query)
                statement.setInt(1, gCode)
                statement.queryTimeout = 10000
                resultSet = statement.executeQuery()

                if (resultSet.next()) {
                    val data = PendingIndentEntry(
                        gCode = resultSet.getInt("G_Code"),
                        truckNo = resultSet.getString("G_TruckNo")?.trim().orEmpty(),
                        partyCode = resultSet.getInt("G_PartyCode").takeIf { !resultSet.wasNull() },
                        brokerCode = resultSet.getInt("G_BrokerCode")
                            .takeIf { !resultSet.wasNull() },
                        doNo = resultSet.getInt("G_DO").takeIf { !resultSet.wasNull() },
                        qty = resultSet.getString("G_Qty")?.trim().orEmpty(),
                        invoiceNo = resultSet.getString("G_InvoiceNo")?.trim().orEmpty(),
                        invQty = resultSet.getString("G_InvQty")?.trim().orEmpty(),
                        invAmt = resultSet.getString("G_InvAmt")?.trim().orEmpty(),
                        doc1 = resultSet.getString("G_Doc1")?.trim().orEmpty(),
                        doc2 = resultSet.getString("G_Doc2")?.trim().orEmpty(),
                        doc3 = resultSet.getString("G_Doc3")?.trim().orEmpty(),
                        doc4 = resultSet.getString("G_Doc4")?.trim().orEmpty(),
                        doc5 = resultSet.getString("G_Doc5")?.trim().orEmpty(),
                        date = resultSet.getString("G_Date")?.trim().orEmpty(),
                        user = resultSet.getString("G_User")?.trim().orEmpty(),
                        modeName = resultSet.getString("M_Name").trim().orEmpty(),
                        bags = resultSet.getInt("G_Bags").takeIf { !resultSet.wasNull() },
                        invRate = resultSet.getString("G_InvRate")?.trim().orEmpty(),
                        remarks = resultSet.getString("G_Remarks")?.trim().orEmpty(),
                        partyName = resultSet.getString("G_PartyName")?.trim().orEmpty(),
                        brokerName = resultSet.getString("G_BrokerName")?.trim().orEmpty(),
                        driverName = resultSet.getString("G_DriverName")?.trim().orEmpty(),
                        driverMobile = resultSet.getString("G_DriverMobile")?.trim().orEmpty(),
                        itemCode = resultSet.getInt("G_ItemCode").takeIf { !resultSet.wasNull() },
                        challanNo = resultSet.getString("G_ChallanNo")?.trim().orEmpty(),
                        challanDate = resultSet.getString("G_ChallanDate")?.trim().orEmpty(),
                        capturedPhoto = resultSet.getString("G_CapturedPhoto")?.trim().orEmpty()
                    )
                    Log.d(TAG, "✅ Found entry G_Code=${data.gCode} truck=${data.truckNo}")
                    return@withContext data
                } else {
                    Log.d(TAG, "❌ No entry for G_Code=$gCode")
                    return@withContext null
                }
            } catch (e: SQLException) {
                Log.e(TAG, "SQL Exception: ${e.message}", e)
            } catch (e: Exception) {
                Log.e(TAG, "Exception: ${e.message}", e)
            } finally {
                closeQuietly(resultSet, statement, connection)
            }
            return@withContext null
        }


    private fun closeQuietly(rs: ResultSet?, st: PreparedStatement?, cn: Connection?) {
        try {
            rs?.close()
        } catch (_: Exception) {
        }
        try {
            st?.close()
        } catch (_: Exception) {
        }
        try {
            cn?.close()
        } catch (_: Exception) {
        }
    }

    fun clearCache() {
        pendingOptionsCache = null
        Log.d(TAG, "Cache cleared")
    }
}