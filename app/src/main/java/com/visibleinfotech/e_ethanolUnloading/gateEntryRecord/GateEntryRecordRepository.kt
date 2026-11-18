package com.visibleinfotech.e_ethanolUnloading.gateEntryRecord

import android.content.Context
import android.util.Log
import com.visibleinfotech.e_ethanolUnloading.MainConstant
import com.visibleinfotech.e_ethanolUnloading.gateEntryRecord.models.GateEntryRecordModel
import com.visibleinfotech.e_ethanolUnloading.sqlConnection.ConnectionStr
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.SQLException

private const val TAG = "GateEntryRecordRepo"

class GateEntryRecordRepository(private val context: Context) {

    private val mainConstant = MainConstant(context)

    suspend fun getGateEntryRecords(): List<GateEntryRecordModel> = withContext(Dispatchers.IO) {
        var connection: Connection? = null
        var statement: PreparedStatement? = null
        var resultSet: ResultSet? = null
        val records = mutableListOf<GateEntryRecordModel>()

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
                SELECT  CAST(g.G_Code AS VARCHAR(50)) as G_Code,
                    g.G_TruckNo,
                    CAST(g.G_PartyCode AS VARCHAR(50)) as G_PartyCode,
                    CAST(g.G_BrokerCode AS VARCHAR(50)) as G_BrokerCode,
                    CAST(g.G_DO AS VARCHAR(50)) as G_DO,
                    CAST(g.G_Qty AS VARCHAR(50)) as G_Qty,
                    g.G_InvoiceNo,
                    CAST(g.G_InvQty AS VARCHAR(50)) as G_InvQty,
                    CAST(g.G_InvAmt AS VARCHAR(50)) as G_InvAmt,
                    g.G_Doc1,
                    g.G_Doc2,
                    g.G_Doc3,
                    g.G_Doc4,
                    g.G_Doc5,
                    CONVERT(VARCHAR(23), g.G_Date, 121) as G_Date,
                    g.G_User,
                    CAST(g.G_ModeCode AS VARCHAR(50)) as G_ModeCode,
                    CAST(g.G_Bags AS VARCHAR(50)) as G_Bags,
                    CAST(g.G_InvRate AS VARCHAR(50)) as G_InvRate,
                    g.G_Remarks,
                    ISNULL(am.account_name, '') as party_name,
                    ISNULL(bm.account_name, '') as broker_name,
                    ISNULL(mm.M_Name, '') as mode_name,
                    ISNULL(d.D_DriverName, '') as driver_name,
                    ISNULL(CAST(d.D_DriverMobile AS VARCHAR(50)), '') as driver_mobile,
                    ISNULL(d.D_Status, 'N') as dispatch_status
                FROM Vipl_ETGateEntry g
                LEFT JOIN VIPL_Accounts_master am ON am.account_id = g.G_PartyCode
                LEFT JOIN VIPL_Accounts_master bm ON bm.account_id = g.G_BrokerCode
                LEFT JOIN VIPL_ModeMaster mm ON mm.M_Code = g.G_ModeCode
                LEFT JOIN Vipl_ETDispatch d ON d.D_Code = g.G_DO
                ORDER BY g.G_Code DESC
            """.trimIndent()

            Log.d(TAG, "Fetching gate entry records...")
            Log.d(TAG, "Query: $query")

            statement = connection.prepareStatement(query)
            statement.queryTimeout = 30000
            resultSet = statement.executeQuery()

            while (resultSet.next()) {
                val record = GateEntryRecordModel(
                    gCode = resultSet.getString("G_Code")?.trim() ?: "",
                    truckNo = resultSet.getString("G_TruckNo")?.trim() ?: "",
                    driverName = resultSet.getString("driver_name")?.trim() ?: "",
                    driverMobile = resultSet.getString("driver_mobile")?.trim() ?: "",
                    invoiceNo = resultSet.getString("G_InvoiceNo")?.trim() ?: "",
                    qty = resultSet.getString("G_Qty")?.trim() ?: "0",
                    partyName = resultSet.getString("party_name")?.trim() ?: "",
                    brokerName = resultSet.getString("broker_name")?.trim() ?: "",
                    modeCode = resultSet.getString("G_ModeCode")?.trim() ?: "",
                    modeName = resultSet.getString("mode_name")?.trim() ?: "",
                    bags = resultSet.getString("G_Bags")?.trim() ?: "0",
                    invQty = resultSet.getString("G_InvQty")?.trim() ?: "0",
                    invAmt = resultSet.getString("G_InvAmt")?.trim() ?: "0",
                    invRate = resultSet.getString("G_InvRate")?.trim() ?: "0",
                    date = resultSet.getString("G_Date")?.trim() ?: "",
                    status = resultSet.getString("dispatch_status")?.trim() ?: "N",
                    remarks = resultSet.getString("G_Remarks")?.trim() ?: "",
                    user = resultSet.getString("G_User")?.trim() ?: "",
                    doc1 = resultSet.getString("G_Doc1")?.trim() ?: "",
                    doc2 = resultSet.getString("G_Doc2")?.trim() ?: "",
                    doc3 = resultSet.getString("G_Doc3")?.trim() ?: "",
                    doc4 = resultSet.getString("G_Doc4")?.trim() ?: "",
                    doc5 = resultSet.getString("G_Doc5")?.trim() ?: ""
                )
                records.add(record)
                Log.d(TAG, "Loaded: G_Code=${record.gCode}, Truck=${record.truckNo}, Driver=${record.driverName}")
            }

            Log.d(TAG, "✅ Loaded ${records.size} gate entry records")

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
        return@withContext records
    }


}