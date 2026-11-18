package com.visibleinfotech.e_ethanolUnloading.unloadRecord

import android.content.Context
import android.util.Log
import com.visibleinfotech.e_ethanolUnloading.MainConstant
import com.visibleinfotech.e_ethanolUnloading.sqlConnection.ConnectionStr
import com.visibleinfotech.e_ethanolUnloading.unloadRecord.models.UnloadStatus
import com.visibleinfotech.e_ethanolUnloading.unloadRecord.models.UnloadingDetailModel
import com.visibleinfotech.e_ethanolUnloading.unloadRecord.models.UnloadingRecordModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet

private const val TAG = "UnloadingRecordRepo"

class UnloadingRecordRepository(private val context: Context) {

    private val mainConstant = MainConstant(context)

    suspend fun getUnloadingRecords(): List<UnloadingRecordModel> =
        withContext(Dispatchers.IO) {

            val list = mutableListOf<UnloadingRecordModel>()
            var connection: Connection? = null
            var statement: PreparedStatement? = null
            var resultSet: ResultSet? = null

            try {
                Log.d(TAG, "getUnloadingRecords() – opening DB connection")
                val conStr = ConnectionStr()
                connection = conStr.connectionclasss(
                    mainConstant.getDbUser(),
                    mainConstant.getDbPass(),
                    mainConstant.getDbName(),
                    mainConstant.getIp(),
                    mainConstant.getInstance()
                )

                if (connection == null) {
                    Log.e(TAG, "DB connection failed")
                    return@withContext emptyList()
                }
                Log.d(TAG, "DB connection established")

                val query = """
                WITH LatestRow AS (
                    SELECT
                        u.U_Indent,
                        u.U_WsNo,
                        u.U_VehicleNo,
                        u.U_RejectCode,
                        u.U_DedCode,
                        ROW_NUMBER() OVER (
                            PARTITION BY u.U_Indent, u.U_WsNo, u.U_VehicleNo
                            ORDER BY u.U_Code DESC
                        ) AS rn
                    FROM Vipl_ETUnload u
                )
                SELECT
                    g.GR_GATEPASS                                       AS WS_No,
                    g.GR_GROWER_NAME                                    AS PartyName,
                    g.GR_CONT_NAME                                      AS BrokerName,
                    g.GR_WT                                             AS GrossWeight,
                    g.GR_TRANSP_NAME                                    AS ThekedarName,
                    g.Gr_Indent                                         AS IndentNo,
                    g.Gr_TruckNo                                        AS VehicleNo,
                    gd.g_name                                           AS GodownName,
                    g.GR_UnloadUser                                     AS UnloadUser,
                    CONVERT(VARCHAR(19), g.GR_UnloadDate, 120)          AS UnloadDate,
                    LTRIM(RTRIM(CAST(g.GR_UNLOADFLAG AS VARCHAR(10))))  AS UnloadFlag,
                    lr.U_RejectCode,
                    lr.U_DedCode,
                    CASE WHEN lr.U_Indent IS NULL THEN 0 ELSE 1 END     AS HasEtUnloadEntry
                FROM VIPL_GROSS g
                LEFT JOIN Vipl_godownmaster gd
                       ON LTRIM(RTRIM(CAST(gd.g_code AS VARCHAR(50)))) =
                          LTRIM(RTRIM(CAST(g.GR_GODOWN_CODE AS VARCHAR(50))))
                LEFT JOIN LatestRow lr
                       ON lr.rn = 1
                      AND LTRIM(RTRIM(CAST(lr.U_Indent    AS VARCHAR(50)))) =
                          LTRIM(RTRIM(CAST(g.Gr_Indent    AS VARCHAR(50))))
                      AND LTRIM(RTRIM(CAST(lr.U_WsNo      AS VARCHAR(50)))) =
                          LTRIM(RTRIM(CAST(g.GR_GATEPASS  AS VARCHAR(50))))
                      AND LTRIM(RTRIM(CAST(lr.U_VehicleNo AS VARCHAR(50)))) =
                          LTRIM(RTRIM(CAST(g.Gr_TruckNo   AS VARCHAR(50))))
                WHERE 1 = 1

                  -- ❌ CONDITION 1: UNLOADFLAG NULL/'N', NO ETUnload entry → HIDE
                  AND NOT (
                        (g.GR_UNLOADFLAG IS NULL
                         OR LTRIM(RTRIM(CAST(g.GR_UNLOADFLAG AS VARCHAR(10)))) = 'N')
                        AND lr.U_Indent IS NULL
                      )

                  -- ❌ CONDITION 4: UNLOADFLAG='N', entry found, reject set, ded set & ≠ 4 → HIDE
                  AND NOT (
                        LTRIM(RTRIM(CAST(g.GR_UNLOADFLAG AS VARCHAR(10)))) = 'N'
                        AND lr.U_Indent IS NOT NULL
                        AND lr.U_RejectCode IS NOT NULL
                        AND LTRIM(RTRIM(CAST(lr.U_RejectCode AS VARCHAR(50)))) NOT IN ('', '0')
                        AND lr.U_DedCode IS NOT NULL
                        AND LTRIM(RTRIM(CAST(lr.U_DedCode AS VARCHAR(50)))) NOT IN ('', '0', '4')
                      )
                ORDER BY g.GR_UnloadDate DESC
            """.trimIndent()

                Log.d(TAG, "Executing master query…")
                statement = connection.prepareStatement(query)
                statement.queryTimeout = 15000
                resultSet = statement.executeQuery()
                Log.d(TAG, "Master query executed, iterating rows…")

                var rowCount = 0
                var skippedUnknown = 0

                while (resultSet.next()) {
                    rowCount++

                    val unloadFlag = (resultSet.getString("UnloadFlag")   ?: "").trim()
                    val rejectCode = (resultSet.getString("U_RejectCode") ?: "").trim()
                    val dedCode    = (resultSet.getString("U_DedCode")    ?: "").trim()

                    val status = deriveStatus(unloadFlag, rejectCode, dedCode)

                    Log.v(
                        TAG,
                        "Row#$rowCount flag='$unloadFlag' reject='$rejectCode' " +
                                "ded='$dedCode' → status=$status"
                    )

                    // Safety: skip any record that SQL filters missed
                    if (status == UnloadStatus.UNKNOWN) {
                        skippedUnknown++
                        Log.w(
                            TAG,
                            "Skipping UNKNOWN record ws=${resultSet.getString("WS_No")} " +
                                    "indent=${resultSet.getString("IndentNo")} " +
                                    "vehicle=${resultSet.getString("VehicleNo")} " +
                                    "flag=$unloadFlag reject=$rejectCode ded=$dedCode"
                        )
                        continue
                    }

                    list.add(
                        UnloadingRecordModel(
                            wsNo         = resultSet.getString("WS_No")?.trim() ?: "",
                            partyName    = resultSet.getString("PartyName")?.trim() ?: "",
                            brokerName   = resultSet.getString("BrokerName")?.trim() ?: "",
                            grossWeight  = resultSet.getString("GrossWeight")?.trim() ?: "",
                            thekedarName = resultSet.getString("ThekedarName")?.trim() ?: "",
                            indentNo     = resultSet.getString("IndentNo")?.trim() ?: "",
                            vehicleNo    = resultSet.getString("VehicleNo")?.trim() ?: "",
                            godownName   = resultSet.getString("GodownName")?.trim() ?: "",
                            unloadUser   = resultSet.getString("UnloadUser")?.trim() ?: "",
                            unloadDate   = resultSet.getString("UnloadDate")?.trim() ?: "",
                            status       = status
                        )
                    )
                }

                Log.d(
                    TAG,
                    "Master list loaded: ${list.size} records " +
                            "(rows scanned=$rowCount, skippedUnknown=$skippedUnknown)"
                )

            } catch (e: Exception) {
                Log.e(TAG, "Error loading master list", e)
            } finally {
                Log.d(TAG, "Closing master-list DB resources")
                try { resultSet?.close() } catch (e: Exception) { Log.w(TAG, "resultSet close failed", e) }
                try { statement?.close() } catch (e: Exception) { Log.w(TAG, "statement close failed", e) }
                try { connection?.close() } catch (e: Exception) { Log.w(TAG, "connection close failed", e) }
            }

            list
        }

    private fun deriveStatus(
        unloadFlag: String,
        rejectCode: String,
        dedCode: String
    ): UnloadStatus {

        val flagY = unloadFlag.equals("Y", true)
        if (!flagY) return UnloadStatus.UNKNOWN

        val rejectZero    = rejectCode.isEmpty() || rejectCode == "0"
        val dedNullOrZero = dedCode.isEmpty() || dedCode == "0"

        val result = when {
            rejectZero && dedNullOrZero   -> UnloadStatus.UNLOADED
            !rejectZero && dedNullOrZero  -> UnloadStatus.TEMP_REJECTED
            !rejectZero && !dedNullOrZero -> UnloadStatus.REJECTED
            else                          -> UnloadStatus.UNKNOWN
        }

        Log.v(
            TAG,
            "deriveStatus(flag='$unloadFlag', reject='$rejectCode', ded='$dedCode') → $result"
        )
        return result
    }

    suspend fun getUnloadingDetail(
        indentNo: String,
        wsNo: String,
        vehicleNo: String,
    ): List<UnloadingDetailModel> = withContext(Dispatchers.IO) {

        Log.d(TAG, "getUnloadingDetail() indent='$indentNo' ws='$wsNo' vehicle='$vehicleNo'")

        val list = mutableListOf<UnloadingDetailModel>()
        var connection: Connection? = null
        var statement: PreparedStatement? = null
        var resultSet: ResultSet? = null

        try {
            val conStr = ConnectionStr()
            connection = conStr.connectionclasss(
                mainConstant.getDbUser(),
                mainConstant.getDbPass(),
                mainConstant.getDbName(),
                mainConstant.getIp(),
                mainConstant.getInstance()
            )

            if (connection == null) {
                Log.e(TAG, "DB connection failed (detail)")
                return@withContext emptyList()
            }
            Log.d(TAG, "Detail DB connection established")

            val query = """
                SELECT
                    U_Code,
                    U_NoOfBags   AS Bags,
                    U_Moisture   AS Moisture,
                    U_Avg        AS AvgMoisture,
                    U_High       AS HighMoisture,
                    U_UnloadStatus,
                    CONVERT(VARCHAR(19), U_Date, 120) AS UDate
                FROM Vipl_ETUnload
                WHERE LTRIM(RTRIM(CAST(U_Indent    AS VARCHAR(50)))) =
                      LTRIM(RTRIM(CAST(? AS VARCHAR(50))))
                  AND LTRIM(RTRIM(CAST(U_WsNo      AS VARCHAR(50)))) =
                      LTRIM(RTRIM(CAST(? AS VARCHAR(50))))
                  AND LTRIM(RTRIM(CAST(U_VehicleNo AS VARCHAR(50)))) =
                      LTRIM(RTRIM(CAST(? AS VARCHAR(50))))
                ORDER BY U_Code ASC
            """.trimIndent()

            statement = connection.prepareStatement(query)
            statement.queryTimeout = 15000
            statement.setString(1, indentNo)
            statement.setString(2, wsNo)
            statement.setString(3, vehicleNo)

            Log.d(TAG, "Executing detail query…")
            resultSet = statement.executeQuery()

            while (resultSet.next()) {
                list.add(
                    UnloadingDetailModel(
                        uCode = resultSet.getString("U_Code")?.trim() ?: "",
                        bags = resultSet.getString("Bags")?.trim() ?: "",
                        moisture = resultSet.getString("Moisture")?.trim() ?: "",
                        avg = resultSet.getString("AvgMoisture")?.trim() ?: "",
                        high = resultSet.getString("HighMoisture")?.trim() ?: "",
                        unloadStatus = resultSet.getString("U_UnloadStatus")?.trim() ?: "",
                        date = resultSet.getString("UDate")?.trim() ?: ""
                    )
                )
            }

            Log.d(TAG, "Detail rows loaded: ${list.size} for ws=$wsNo indent=$indentNo veh=$vehicleNo")

        } catch (e: Exception) {
            Log.e(TAG, "Error loading detail rows", e)
        } finally {
            Log.d(TAG, "Closing detail DB resources")
            try { resultSet?.close() } catch (e: Exception) { Log.w(TAG, "resultSet close failed", e) }
            try { statement?.close() } catch (e: Exception) { Log.w(TAG, "statement close failed", e) }
            try { connection?.close() } catch (e: Exception) { Log.w(TAG, "connection close failed", e) }
        }

        list
    }
}