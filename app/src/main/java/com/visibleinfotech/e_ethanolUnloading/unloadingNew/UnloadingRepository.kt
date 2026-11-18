package com.visibleinfotech.e_ethanolUnloading.unloadingNew

import android.content.Context
import android.util.Log
import com.visibleinfotech.e_ethanolUnloading.MainConstant
import com.visibleinfotech.e_ethanolUnloading.sqlConnection.ConnectionStr
import com.visibleinfotech.e_ethanolUnloading.unloading.models.GodownData
import com.visibleinfotech.e_ethanolUnloading.unloading.models.GrossData
import com.visibleinfotech.e_ethanolUnloading.unloading.models.RejectData
import com.visibleinfotech.e_ethanolUnloading.unloading.models.ThekedarData
import com.visibleinfotech.e_ethanolUnloading.unloading.models.UnloadingRow
import com.visibleinfotech.e_ethanolUnloading.unloading.models.VehicleData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet

private const val TAG = "UnloadingRepositoryLog"

class UnloadingRepository(
    private val context: Context,
) {

    private val mainConstant = MainConstant(context)

    suspend fun getVehicleList(): List<VehicleData> =
        withContext(Dispatchers.IO) {

            val vehicleList = mutableListOf<VehicleData>()

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
                    Log.e(TAG, "Database connection failed")
                    return@withContext emptyList()
                }

                val query = """
                 SELECT DISTINCT
                        g.GR_GATEPASS,
                        g.Gr_TruckNo,
                        g.Gr_Indent
                    FROM VIPL_GROSS g
                    WHERE LTRIM(RTRIM(CAST(g.GR_TARE_FLAG AS VARCHAR(10)))) = '0'
                      AND (
                            -- Case 1: fresh vehicle, never unloaded
                            (
                                g.GR_UNLOADFLAG IS NULL
                                OR LTRIM(RTRIM(CAST(g.GR_UNLOADFLAG AS VARCHAR(10)))) = ''
                                OR LTRIM(RTRIM(CAST(g.GR_UNLOADFLAG AS VARCHAR(10)))) = 'N'
                            )
                            OR
                            -- Case 2: temp-rejected by operator + real dept decision
                            (
                                LTRIM(RTRIM(CAST(g.GR_UNLOADFLAG AS VARCHAR(10)))) = 'Y'
                                AND EXISTS (
                                    SELECT 1
                                    FROM Vipl_ETUnload u
                                    WHERE LTRIM(RTRIM(CAST(u.U_Indent    AS VARCHAR(50)))) =
                                          LTRIM(RTRIM(CAST(g.Gr_Indent    AS VARCHAR(50))))
                                      AND LTRIM(RTRIM(CAST(u.U_WsNo      AS VARCHAR(50)))) =
                                          LTRIM(RTRIM(CAST(g.GR_GATEPASS  AS VARCHAR(50))))
                                      AND LTRIM(RTRIM(CAST(u.U_VehicleNo AS VARCHAR(50)))) =
                                          LTRIM(RTRIM(CAST(g.Gr_TruckNo   AS VARCHAR(50))))
                                      AND LTRIM(RTRIM(CAST(u.U_UnloadStatus AS VARCHAR(10)))) = 'Y'
                                      -- operator temp-rejected
                                      AND LTRIM(RTRIM(CAST(u.U_RejectCode AS VARCHAR(50)))) <> '0'
                                      AND LTRIM(RTRIM(CAST(u.U_RejectCode AS VARCHAR(50)))) <> ''
                                      -- dept gave a real decision (not null/empty/0/4)
                                      AND u.U_DedCode IS NOT NULL
                                      AND LTRIM(RTRIM(CAST(u.U_DedCode AS VARCHAR(50)))) <> ''
                                      AND LTRIM(RTRIM(CAST(u.U_DedCode AS VARCHAR(50)))) <> '0'
                                      AND LTRIM(RTRIM(CAST(u.U_DedCode AS VARCHAR(50)))) <> '4'
                                      -- must be the latest finalized row
                                      AND u.U_Code = (
                                            SELECT MAX(u2.U_Code)
                                            FROM Vipl_ETUnload u2
                                            WHERE LTRIM(RTRIM(CAST(u2.U_Indent    AS VARCHAR(50)))) =
                                                  LTRIM(RTRIM(CAST(g.Gr_Indent    AS VARCHAR(50))))
                                              AND LTRIM(RTRIM(CAST(u2.U_WsNo      AS VARCHAR(50)))) =
                                                  LTRIM(RTRIM(CAST(g.GR_GATEPASS  AS VARCHAR(50))))
                                              AND LTRIM(RTRIM(CAST(u2.U_VehicleNo AS VARCHAR(50)))) =
                                                  LTRIM(RTRIM(CAST(g.Gr_TruckNo   AS VARCHAR(50))))
                                              AND LTRIM(RTRIM(CAST(u2.U_UnloadStatus AS VARCHAR(10)))) = 'Y'
                                          )
                                )
                            )
                          )
                    ORDER BY g.GR_GATEPASS DESC
                """.trimIndent()

                Log.d(TAG, "getVehicleList query : $query")

                statement = connection.prepareStatement(query)
                statement.queryTimeout = 10000
                resultSet = statement.executeQuery()

                while (resultSet.next()) {
                    val gp = resultSet.getString("GR_GATEPASS")?.trim() ?: ""
                    val vn = resultSet.getString("Gr_TruckNo")?.trim() ?: ""
                    val vi = resultSet.getString("Gr_Indent")?.trim() ?: ""

                    if (gp.isNotBlank() && vn.isNotBlank()) {
                        vehicleList.add(
                            VehicleData(
                                gatePass = gp,
                                vehicleNo = vn,
                                vehicleIndent = vi
                            )
                        )
                    }
                }

                Log.d(TAG, "Loaded ${vehicleList.size} vehicles (fresh + dept-approved-for-redo)")

            } catch (e: Exception) {
                Log.e(TAG, "Error loading vehicle list", e)
            } finally {
                try { resultSet?.close() } catch (_: Exception) {}
                try { statement?.close() } catch (_: Exception) {}
                try { connection?.close() } catch (_: Exception) {}
            }

            vehicleList
        }

    suspend fun getPreviousUnloadingRows(
        indent: String,
        wsNo: String,
        vehicleNo: String,
    ): List<UnloadingRow> = withContext(Dispatchers.IO) {

        val rows = mutableListOf<UnloadingRow>()

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
                Log.e(TAG, "getPreviousUnloadingRows: DB connection failed")
                return@withContext emptyList()
            }

            val query = """
                SELECT U_Code, U_NoOfBags, U_Moisture,
                       U_Avg, U_High,
                       U_RejectCode, U_DedCode, U_UnloadStatus
                FROM Vipl_ETUnload
                WHERE LTRIM(RTRIM(CAST(U_Indent AS VARCHAR(50)))) =
                      LTRIM(RTRIM(CAST(? AS VARCHAR(50))))
                  AND LTRIM(RTRIM(CAST(U_WsNo AS VARCHAR(50)))) =
                      LTRIM(RTRIM(CAST(? AS VARCHAR(50))))
                  AND LTRIM(RTRIM(CAST(U_VehicleNo AS VARCHAR(50)))) =
                      LTRIM(RTRIM(CAST(? AS VARCHAR(50))))
                  AND LTRIM(RTRIM(CAST(U_UnloadStatus AS VARCHAR(10)))) = 'Y'
                ORDER BY U_Code
            """.trimIndent()

            statement = connection.prepareStatement(query)
            statement.queryTimeout = 10000
            statement.setString(1, indent)
            statement.setString(2, wsNo)
            statement.setString(3, vehicleNo)

            resultSet = statement.executeQuery()

            var sr = 1
            while (resultSet.next()) {
                val uCode = resultSet.getLong("U_Code")
                val bags = resultSet.getInt("U_NoOfBags").toString()
                val moisture = resultSet.getDouble("U_Moisture").toString()

                rows.add(
                    UnloadingRow(
                        srNo = sr++,
                        bags = bags,
                        moisture = moisture,
                        uCode = if (uCode > 0) uCode else -1L,
                        isSaving = false
                    )
                )
            }

            Log.d(
                TAG,
                "Loaded ${rows.size} previous unloading rows " +
                        "for indent=$indent wsNo=$wsNo vehicle=$vehicleNo"
            )

        } catch (e: Exception) {
            Log.e(TAG, "getPreviousUnloadingRows error", e)
        } finally {
            try { resultSet?.close() } catch (_: Exception) {}
            try { statement?.close() } catch (_: Exception) {}
            try { connection?.close() } catch (_: Exception) {}
        }

        rows
    }

    suspend fun getPreviousGodownCode(
        indent: String,
        wsNo: String,
        vehicleNo: String,
    ): String = withContext(Dispatchers.IO) {

        var connection: Connection? = null
        var statement: PreparedStatement? = null
        var resultSet: ResultSet? = null
        var godownCode = ""

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
                Log.e(TAG, "getPreviousGodownCode: DB connection failed")
                return@withContext ""
            }

            val query = """
                SELECT TOP 1 U_GodownCode
                FROM Vipl_ETUnload
                WHERE LTRIM(RTRIM(CAST(U_Indent AS VARCHAR(50)))) =
                      LTRIM(RTRIM(CAST(? AS VARCHAR(50))))
                  AND LTRIM(RTRIM(CAST(U_WsNo AS VARCHAR(50)))) =
                      LTRIM(RTRIM(CAST(? AS VARCHAR(50))))
                  AND LTRIM(RTRIM(CAST(U_VehicleNo AS VARCHAR(50)))) =
                      LTRIM(RTRIM(CAST(? AS VARCHAR(50))))
                  AND U_GodownCode IS NOT NULL
                  AND LTRIM(RTRIM(CAST(U_GodownCode AS VARCHAR(50)))) <> ''
                ORDER BY U_Code DESC
            """.trimIndent()

            statement = connection.prepareStatement(query)
            statement.queryTimeout = 10000
            statement.setString(1, indent)
            statement.setString(2, wsNo)
            statement.setString(3, vehicleNo)

            resultSet = statement.executeQuery()
            if (resultSet.next()) {
                godownCode = resultSet.getString("U_GodownCode")?.trim() ?: ""
            }

            Log.d(
                TAG,
                "getPreviousGodownCode: indent=$indent wsNo=$wsNo vehicle=$vehicleNo -> '$godownCode'"
            )

        } catch (e: Exception) {
            Log.e(TAG, "getPreviousGodownCode error", e)
        } finally {
            try { resultSet?.close() } catch (_: Exception) {}
            try { statement?.close() } catch (_: Exception) {}
            try { connection?.close() } catch (_: Exception) {}
        }

        godownCode
    }

    suspend fun getPreviousTransporter(
        gatePass: String,
    ): Pair<String, String> = withContext(Dispatchers.IO) {

        var connection: Connection? = null
        var statement: PreparedStatement? = null
        var resultSet: ResultSet? = null
        var code = ""
        var name = ""

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
                Log.e(TAG, "getPreviousTransporter: DB connection failed")
                return@withContext Pair("", "")
            }

            val query = """
                SELECT GR_TRANSP_CODE, GR_TRANSP_NAME
                FROM VIPL_GROSS
                WHERE LTRIM(RTRIM(CAST(GR_GATEPASS AS VARCHAR(50)))) =
                      LTRIM(RTRIM(CAST(? AS VARCHAR(50))))
                  AND GR_TARE_FLAG = '0'
            """.trimIndent()

            statement = connection.prepareStatement(query)
            statement.queryTimeout = 10000
            statement.setString(1, gatePass)

            resultSet = statement.executeQuery()
            if (resultSet.next()) {
                code = resultSet.getString("GR_TRANSP_CODE")?.trim() ?: ""
                name = resultSet.getString("GR_TRANSP_NAME")?.trim() ?: ""
            }

            Log.d(
                TAG,
                "getPreviousTransporter: gatePass=$gatePass -> '$code' / '$name'"
            )

        } catch (e: Exception) {
            Log.e(TAG, "getPreviousTransporter error", e)
        } finally {
            try { resultSet?.close() } catch (_: Exception) {}
            try { statement?.close() } catch (_: Exception) {}
            try { connection?.close() } catch (_: Exception) {}
        }

        Pair(code, name)
    }

    suspend fun getGodownList(): List<GodownData> =
        withContext(Dispatchers.IO) {

            val godownList = mutableListOf<GodownData>()

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
                    Log.e(TAG, "Database connection failed")
                    return@withContext emptyList()
                }

                val query = """
                    SELECT g_code, g_name, g_capacity
                    FROM Vipl_godownmaster
                    ORDER BY g_name
                """.trimIndent()

                statement = connection.prepareStatement(query)
                statement.queryTimeout = 10000
                resultSet = statement.executeQuery()

                while (resultSet.next()) {
                    godownList.add(
                        GodownData(
                            code = resultSet.getString("g_code")?.trim() ?: "",
                            name = resultSet.getString("g_name")?.trim() ?: "",
                            capacity = resultSet.getString("g_capacity")?.trim() ?: ""
                        )
                    )
                }

                Log.d(TAG, "Loaded ${godownList.size} godowns")

            } catch (e: Exception) {
                Log.e(TAG, "Error loading godown list", e)
            } finally {
                try { resultSet?.close() } catch (_: Exception) {}
                try { statement?.close() } catch (_: Exception) {}
                try { connection?.close() } catch (_: Exception) {}
            }

            godownList
        }

    suspend fun getThekedarList(): List<ThekedarData> =
        withContext(Dispatchers.IO) {

            val thekedarList = mutableListOf<ThekedarData>()

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
                    Log.e(TAG, "Database connection failed")
                    return@withContext emptyList()
                }

                val query = """
                    SELECT account_id, account_name
                    FROM Vipl_Accounts_master
                    WHERE G_AcType = 'T'
                    ORDER BY account_name
                """.trimIndent()

                statement = connection.prepareStatement(query)
                statement.queryTimeout = 10000
                resultSet = statement.executeQuery()

                while (resultSet.next()) {
                    thekedarList.add(
                        ThekedarData(
                            accountId = resultSet.getString("account_id")?.trim() ?: "",
                            accountName = resultSet.getString("account_name")?.trim() ?: ""
                        )
                    )
                }

                Log.d(TAG, "Loaded ${thekedarList.size} Thekedars")

            } catch (e: Exception) {
                Log.e(TAG, "Error loading Thekedar list", e)
            } finally {
                try { resultSet?.close() } catch (_: Exception) {}
                try { statement?.close() } catch (_: Exception) {}
                try { connection?.close() } catch (_: Exception) {}
            }

            thekedarList
        }

    suspend fun getGrossDataByGatePass(gatePass: String): GrossData? =
        withContext(Dispatchers.IO) {

            var connection: Connection? = null
            var statement: PreparedStatement? = null
            var resultSet: ResultSet? = null
            var grossData: GrossData? = null

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
                    Log.e(TAG, "Database connection failed")
                    return@withContext null
                }

                val query = """
                    SELECT GR_GATEPASS, GR_SITE_CODE, GR_WS_TYPE, GR_GROSS_SLIP, GR_LAYER,
                           GR_TOKEN_DATE, GR_RFID_NO, GR_GROWER_CODE, GR_GROWER_NAME,
                           GR_VEHICLE_CODE, GR_VEHICLE_NO, GR_CONT_CODE, GR_CONT_NAME,
                           GR_CANE_NAME, GR_PURCHASE_RATE, GR_TRANSP_RATE, GR_HARV_RATE,
                           GR_GATE_NO, GR_SHIFT, GR_CHAIN_NO, GR_CANE_TYPE_ID, GR_LABOUR,
                           GR_CROP_TYPE, GR_PLOT_ID, GR_LABOUR_SLIP_NO, GR_PRINT_NOTE,
                           GR_DONGA_FLAG, GR_DATE, GR_COUNTER, GR_WT, GR_MODE, GR_ZOONA_PER,
                           GR_BRICKS, GR_DONGA_DATE, GR_RFID_YARD_SEQ, GR_RFID_YARD_DATE,
                           GR_TARE_FLAG, GR_USER_TK, GR_USER_GR, old_rfidno,
                           GR_TRANSP_CODE, GR_TRANSP_NAME, gr_distance, gr_barcode,
                           gr_del_flag, gr_indentno, gr_pcname, gr_tkpcname, gr_RouteCode,
                           gr_CaneName, gr_CropCode, gr_villCode, gr_zoneCode,
                           gr_vipldate, gr_vipl_date, gr_OtherDed, GR_BURNTCANE1,
                           GR_PATTI, GR_BAAN, TrFlag, GR_WBNO, GR_BURNTCANE, Gr_Indent,
                           Gr_TagSrNo, Gr_Rate, Gr_HRDed, Gr_DriverMobile, Gr_DriverName,
                           Gr_TruckNo, GR_GODOWN_CODE
                    FROM VIPL_GROSS
                    WHERE GR_GATEPASS = ?
                      AND GR_TARE_FLAG = '0'
                """.trimIndent()

                statement = connection.prepareStatement(query)
                statement.queryTimeout = 10000
                statement.setString(1, gatePass)
                resultSet = statement.executeQuery()

                if (resultSet.next()) {
                    grossData = GrossData(
                        gatePass = resultSet.getString("GR_GATEPASS")?.trim() ?: "",
                        siteCode = resultSet.getString("GR_SITE_CODE")?.trim() ?: "",
                        wsType = resultSet.getString("GR_WS_TYPE")?.trim() ?: "",
                        grossSlip = resultSet.getString("GR_GROSS_SLIP")?.trim() ?: "",
                        layer = resultSet.getString("GR_LAYER")?.trim() ?: "",
                        tokenDate = resultSet.getString("GR_TOKEN_DATE")?.trim() ?: "",
                        rfidNo = resultSet.getString("GR_RFID_NO")?.trim() ?: "",
                        growerCode = resultSet.getString("GR_GROWER_CODE")?.trim() ?: "",
                        growerName = resultSet.getString("GR_GROWER_NAME")?.trim() ?: "",
                        vehicleCode = resultSet.getString("GR_VEHICLE_CODE")?.trim() ?: "",
                        vehicleNo = resultSet.getString("GR_VEHICLE_NO")?.trim() ?: "",
                        contCode = resultSet.getString("GR_CONT_CODE")?.trim() ?: "",
                        contName = resultSet.getString("GR_CONT_NAME")?.trim() ?: "",
                        caneName = resultSet.getString("GR_CANE_NAME")?.trim() ?: "",
                        purchaseRate = resultSet.getString("GR_PURCHASE_RATE")?.trim() ?: "",
                        transRate = resultSet.getString("GR_TRANSP_RATE")?.trim() ?: "",
                        harvRate = resultSet.getString("GR_HARV_RATE")?.trim() ?: "",
                        gateNo = resultSet.getString("GR_GATE_NO")?.trim() ?: "",
                        shift = resultSet.getString("GR_SHIFT")?.trim() ?: "",
                        chainNo = resultSet.getString("GR_CHAIN_NO")?.trim() ?: "",
                        caneTypeId = resultSet.getString("GR_CANE_TYPE_ID")?.trim() ?: "",
                        labour = resultSet.getString("GR_LABOUR")?.trim() ?: "",
                        cropType = resultSet.getString("GR_CROP_TYPE")?.trim() ?: "",
                        plotId = resultSet.getString("GR_PLOT_ID")?.trim() ?: "",
                        labourSlipNo = resultSet.getString("GR_LABOUR_SLIP_NO")?.trim() ?: "",
                        printNote = resultSet.getString("GR_PRINT_NOTE")?.trim() ?: "",
                        dongaFlag = resultSet.getString("GR_DONGA_FLAG")?.trim() ?: "",
                        date = resultSet.getString("GR_DATE")?.trim() ?: "",
                        counter = resultSet.getString("GR_COUNTER")?.trim() ?: "",
                        wt = resultSet.getString("GR_WT")?.trim() ?: "",
                        mode = resultSet.getString("GR_MODE")?.trim() ?: "",
                        zoonePer = resultSet.getString("GR_ZOONA_PER")?.trim() ?: "",
                        bricks = resultSet.getString("GR_BRICKS")?.trim() ?: "",
                        dongaDate = resultSet.getString("GR_DONGA_DATE")?.trim() ?: "",
                        rfidYardSeq = resultSet.getString("GR_RFID_YARD_SEQ")?.trim() ?: "",
                        rfidYardDate = resultSet.getString("GR_RFID_YARD_DATE")?.trim() ?: "",
                        tareFlag = resultSet.getString("GR_TARE_FLAG")?.trim() ?: "",
                        userTk = resultSet.getString("GR_USER_TK")?.trim() ?: "",
                        userGr = resultSet.getString("GR_USER_GR")?.trim() ?: "",
                        oldRfidno = resultSet.getString("old_rfidno")?.trim() ?: "",
                        transpCode = resultSet.getString("GR_TRANSP_CODE")?.trim() ?: "",
                        transpName = resultSet.getString("GR_TRANSP_NAME")?.trim() ?: "",
                        distance = resultSet.getString("gr_distance")?.trim() ?: "",
                        barcode = resultSet.getString("gr_barcode")?.trim() ?: "",
                        delFlag = resultSet.getString("gr_del_flag")?.trim() ?: "",
                        indentNo = resultSet.getString("gr_indentno")?.trim() ?: "",
                        pcName = resultSet.getString("gr_pcname")?.trim() ?: "",
                        tkpcName = resultSet.getString("gr_tkpcname")?.trim() ?: "",
                        routeCode = resultSet.getString("gr_RouteCode")?.trim() ?: "",
                        caneName2 = resultSet.getString("gr_CaneName")?.trim() ?: "",
                        cropCode = resultSet.getString("gr_CropCode")?.trim() ?: "",
                        villCode = resultSet.getString("gr_villCode")?.trim() ?: "",
                        zoneCode = resultSet.getString("gr_zoneCode")?.trim() ?: "",
                        viplDate = resultSet.getString("gr_vipldate")?.trim() ?: "",
                        viplDate2 = resultSet.getString("gr_vipl_date")?.trim() ?: "",
                        otherDed = resultSet.getString("gr_OtherDed")?.trim() ?: "",
                        burnCane1 = resultSet.getString("GR_BURNTCANE1")?.trim() ?: "",
                        patti = resultSet.getString("GR_PATTI")?.trim() ?: "",
                        baan = resultSet.getString("GR_BAAN")?.trim() ?: "",
                        trFlag = resultSet.getString("TrFlag")?.trim() ?: "",
                        wbNo = resultSet.getString("GR_WBNO")?.trim() ?: "",
                        burnCane = resultSet.getString("GR_BURNTCANE")?.trim() ?: "",
                        indent = resultSet.getString("Gr_Indent")?.trim() ?: "",
                        tagSrNo = resultSet.getString("Gr_TagSrNo")?.trim() ?: "",
                        rate = resultSet.getString("Gr_Rate")?.trim() ?: "",
                        hrDed = resultSet.getString("Gr_HRDed")?.trim() ?: "",
                        driverMobile = resultSet.getString("Gr_DriverMobile")?.trim() ?: "",
                        driverName = resultSet.getString("Gr_DriverName")?.trim() ?: "",
                        truckNo = resultSet.getString("Gr_TruckNo")?.trim() ?: "",
                        godownCode = resultSet.getString("GR_GODOWN_CODE")?.trim() ?: ""
                    )
                }

                Log.d(TAG, "Loaded gross data for gatepass: $gatePass")

            } catch (e: Exception) {
                Log.e(TAG, "Error loading gross data", e)
            } finally {
                try { resultSet?.close() } catch (_: Exception) {}
                try { statement?.close() } catch (_: Exception) {}
                try { connection?.close() } catch (_: Exception) {}
            }

            grossData
        }

    suspend fun insertUnloadingRow(
        indent: String,
        wsNo: String,
        vehicleNo: String,
        noOfBags: Int,
        moisture: Double,
        avg: Double,
        high: Double,
        godownCode: String,
        contractorCode: String,
        username: String,
    ): Long = withContext(Dispatchers.IO) {

        var connection: Connection? = null
        var selectStmt: PreparedStatement? = null
        var insertStmt: PreparedStatement? = null

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
                Log.e(TAG, "insertUnloadingRow: DB connection failed")
                return@withContext -1L
            }

            val nextCode: Long
            selectStmt = connection.prepareStatement(
                "SELECT ISNULL(MAX(U_Code), 0) + 1 AS NEXT_CODE FROM Vipl_ETUnload"
            )
            selectStmt.queryTimeout = 10000
            selectStmt.executeQuery().use { r ->
                nextCode = if (r.next()) r.getLong("NEXT_CODE") else 1L
            }

            val indentLong = indent.toLongOrNull() ?: 0L
            val wsNoLong = wsNo.toLongOrNull() ?: 0L

            val insertSql = """
            INSERT INTO Vipl_ETUnload
                (U_Code, U_Indent, U_WsNo, U_VehicleNo,
                 U_NoOfBags, U_Moisture,
                 U_RejectCode, U_Avg, U_High,
                 U_UserName, U_UnloadStatus, U_GodownCode, U_Contractor, U_Date)
            VALUES
                (?, ?, ?, ?,
                 ?, ?,
                 ?, ?, ?,
                 ?, ?, ?, ?, GETDATE())
        """.trimIndent()

            insertStmt = connection.prepareStatement(insertSql)
            insertStmt.queryTimeout = 10000

            insertStmt.setLong(1, nextCode)
            insertStmt.setLong(2, indentLong)
            insertStmt.setLong(3, wsNoLong)
            insertStmt.setString(4, vehicleNo)
            insertStmt.setInt(5, noOfBags)
            insertStmt.setDouble(6, moisture)
            insertStmt.setInt(7, 0)
            insertStmt.setDouble(8, avg)
            insertStmt.setDouble(9, high)
            insertStmt.setString(10, username)
            insertStmt.setString(11, "N")
            insertStmt.setString(12, godownCode)
            insertStmt.setString(13, contractorCode)

            val rows = insertStmt.executeUpdate()
            Log.d(
                TAG,
                "insertUnloadingRow: rows=$rows, uCode=$nextCode, wsNo=$wsNo, " +
                        "godown=$godownCode, contractor=$contractorCode"
            )
            if (rows > 0) nextCode else -1L

        } catch (e: Exception) {
            Log.e(TAG, "insertUnloadingRow error", e)
            -1L
        } finally {
            try { selectStmt?.close() } catch (_: Exception) {}
            try { insertStmt?.close() } catch (_: Exception) {}
            try { connection?.close() } catch (_: Exception) {}
        }
    }

    suspend fun getPreviousContractorCode(
        indent: String,
        wsNo: String,
        vehicleNo: String,
    ): String = withContext(Dispatchers.IO) {

        var connection: Connection? = null
        var statement: PreparedStatement? = null
        var resultSet: ResultSet? = null
        var contractorCode = ""

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
                Log.e(TAG, "getPreviousContractorCode: DB connection failed")
                return@withContext ""
            }

            val query = """
            SELECT TOP 1 U_Contractor
            FROM Vipl_ETUnload
            WHERE LTRIM(RTRIM(CAST(U_Indent AS VARCHAR(50)))) =
                  LTRIM(RTRIM(CAST(? AS VARCHAR(50))))
              AND LTRIM(RTRIM(CAST(U_WsNo AS VARCHAR(50)))) =
                  LTRIM(RTRIM(CAST(? AS VARCHAR(50))))
              AND LTRIM(RTRIM(CAST(U_VehicleNo AS VARCHAR(50)))) =
                  LTRIM(RTRIM(CAST(? AS VARCHAR(50))))
              AND U_Contractor IS NOT NULL
              AND LTRIM(RTRIM(CAST(U_Contractor AS VARCHAR(50)))) <> ''
            ORDER BY U_Code DESC
        """.trimIndent()

            statement = connection.prepareStatement(query)
            statement.queryTimeout = 10000
            statement.setString(1, indent)
            statement.setString(2, wsNo)
            statement.setString(3, vehicleNo)

            resultSet = statement.executeQuery()
            if (resultSet.next()) {
                contractorCode = resultSet.getString("U_Contractor")?.trim() ?: ""
            }

            Log.d(
                TAG,
                "getPreviousContractorCode: indent=$indent wsNo=$wsNo vehicle=$vehicleNo -> '$contractorCode'"
            )

        } catch (e: Exception) {
            Log.e(TAG, "getPreviousContractorCode error", e)
        } finally {
            try { resultSet?.close() } catch (_: Exception) {}
            try { statement?.close() } catch (_: Exception) {}
            try { connection?.close() } catch (_: Exception) {}
        }

        contractorCode
    }

    suspend fun finalizeUnloadingRows(
        indent: String,
        wsNo: String,
        vehicleNo: String,
        finalAvg: Double,
        finalHigh: Double,
        rejectCode: String? = null,
        remark: String = "",
    ): Boolean = withContext(Dispatchers.IO) {

        var connection: Connection? = null
        var statement: PreparedStatement? = null

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
                Log.e(TAG, "finalizeUnloadingRows: DB connection failed")
                return@withContext false
            }

            val indentLong = indent.toLongOrNull() ?: 0L
            val wsNoLong = wsNo.toLongOrNull() ?: 0L

            val writeRemark = remark.isNotBlank()

            val sql: String
            if (rejectCode == null) {
                sql = if (writeRemark) {
                    """
                UPDATE Vipl_ETUnload
                   SET U_Avg          = ?,
                       U_High         = ?,
                       U_Remark       = ?,
                       U_UnloadStatus = 'Y'
                 WHERE U_Indent    = ?
                   AND U_WsNo      = ?
                   AND U_VehicleNo = ?
                """.trimIndent()
                } else {
                    """
                UPDATE Vipl_ETUnload
                   SET U_Avg          = ?,
                       U_High         = ?,
                       U_UnloadStatus = 'Y'
                 WHERE U_Indent    = ?
                   AND U_WsNo      = ?
                   AND U_VehicleNo = ?
                """.trimIndent()
                }
            } else {
                sql = if (writeRemark) {
                    """
                UPDATE Vipl_ETUnload
                   SET U_Avg          = ?,
                       U_High         = ?,
                       U_Remark       = ?,
                       U_RejectCode   = ?,
                       U_UnloadStatus = 'Y'
                 WHERE U_Indent    = ?
                   AND U_WsNo      = ?
                   AND U_VehicleNo = ?
                """.trimIndent()
                } else {
                    """
                UPDATE Vipl_ETUnload
                   SET U_Avg          = ?,
                       U_High         = ?,
                       U_RejectCode   = ?,
                       U_UnloadStatus = 'Y'
                 WHERE U_Indent    = ?
                   AND U_WsNo      = ?
                   AND U_VehicleNo = ?
                """.trimIndent()
                }
            }

            statement = connection.prepareStatement(sql)
            statement.queryTimeout = 10000

            var idx = 1
            statement.setDouble(idx++, finalAvg)
            statement.setDouble(idx++, finalHigh)

            if (writeRemark) {
                statement.setString(idx++, remark)
            }

            if (rejectCode != null) {
                val rejectLong = rejectCode.toLongOrNull() ?: 0L
                statement.setLong(idx++, rejectLong)
            }

            statement.setLong(idx++, indentLong)
            statement.setLong(idx++, wsNoLong)
            statement.setString(idx, vehicleNo)

            val rows = statement.executeUpdate()
            Log.d(
                TAG,
                "finalizeUnloadingRows: rows=$rows, indent=$indent, wsNo=$wsNo, " +
                        "vehicle=$vehicleNo, rejectCode=${rejectCode ?: "NONE"}, " +
                        "remarkWritten=$writeRemark, remark='$remark'"
            )
            rows > 0

        } catch (e: Exception) {
            Log.e(TAG, "finalizeUnloadingRows error", e)
            false
        } finally {
            try { statement?.close() } catch (_: Exception) {}
            try { connection?.close() } catch (_: Exception) {}
        }
    }

    suspend fun updateGrossUnloadFlags(
        gatePass: String,
        godownCode: String,
        transpCode: String,
        transpName: String,
        username: String,
        isReject: Boolean,
    ): Boolean = withContext(Dispatchers.IO) {

        var connection: Connection? = null
        var statement: PreparedStatement? = null

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
                Log.e(TAG, "updateGrossUnloadFlags: DB connection failed")
                return@withContext false
            }

            val sql = """
                UPDATE VIPL_GROSS
                   SET GR_GODOWN_CODE = ?,
                       GR_TRANSP_CODE = ?,
                       GR_TRANSP_NAME = ?,
                       GR_UNLOADFLAG  = 'Y',
                       GR_UNLOADUSER  = ?,
                       GR_UNLOADDATE  = GETDATE()
                 WHERE GR_GATEPASS = ?
                   AND GR_TARE_FLAG = '0'
            """.trimIndent()

            statement = connection.prepareStatement(sql)
            statement.queryTimeout = 10000

            statement.setString(1, godownCode)
            statement.setString(2, transpCode)
            statement.setString(3, transpName)
            statement.setString(4, username)
            statement.setString(5, gatePass)

            val rows = statement.executeUpdate()
            Log.d(
                TAG,
                "updateGrossUnloadFlags: rows=$rows, gp=$gatePass, reject=$isReject, " +
                        "transp=$transpCode/$transpName"
            )
            rows > 0

        } catch (e: Exception) {
            Log.e(TAG, "updateGrossUnloadFlags error", e)
            false
        } finally {
            try { statement?.close() } catch (_: Exception) {}
            try { connection?.close() } catch (_: Exception) {}
        }
    }

    suspend fun getRejectList(): List<RejectData> =
        withContext(Dispatchers.IO) {

            val rejectList = mutableListOf<RejectData>()

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
                    Log.e(TAG, "Database connection failed")
                    return@withContext emptyList()
                }

                val query = """
                    SELECT R_Code, R_Name
                    FROM Vipl_ETLabTestRejectMaster
                    ORDER BY R_Name
                """.trimIndent()

                statement = connection.prepareStatement(query)
                statement.queryTimeout = 10000
                resultSet = statement.executeQuery()

                while (resultSet.next()) {
                    rejectList.add(
                        RejectData(
                            code = resultSet.getString("R_Code")?.trim() ?: "",
                            name = resultSet.getString("R_Name")?.trim() ?: ""
                        )
                    )
                }

                Log.d(TAG, "Loaded ${rejectList.size} reject reasons")

            } catch (e: Exception) {
                Log.e(TAG, "Error loading reject list", e)
            } finally {
                try { resultSet?.close() } catch (_: Exception) {}
                try { statement?.close() } catch (_: Exception) {}
                try { connection?.close() } catch (_: Exception) {}
            }

            rejectList
        }

    suspend fun updateUnloadingRow(
        uCode: Long,
        noOfBags: Int,
        moisture: Double,
        avg: Double,
        high: Double,
    ): Boolean = withContext(Dispatchers.IO) {

        var connection: Connection? = null
        var statement: PreparedStatement? = null

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
                Log.e(TAG, "updateUnloadingRow: DB connection failed")
                return@withContext false
            }

            val sql = """
            UPDATE Vipl_ETUnload
               SET U_NoOfBags = ?,
                   U_Moisture = ?,
                   U_Avg      = ?,
                   U_High     = ?
             WHERE U_Code     = ?
        """.trimIndent()

            statement = connection.prepareStatement(sql)
            statement.queryTimeout = 10000

            statement.setInt(1, noOfBags)
            statement.setDouble(2, moisture)
            statement.setDouble(3, avg)
            statement.setDouble(4, high)
            statement.setLong(5, uCode)

            val rows = statement.executeUpdate()
            Log.d(TAG, "updateUnloadingRow: rows=$rows, uCode=$uCode")
            rows > 0

        } catch (e: Exception) {
            Log.e(TAG, "updateUnloadingRow error", e)
            false
        } finally {
            try { statement?.close() } catch (_: Exception) {}
            try { connection?.close() } catch (_: Exception) {}
        }
    }

    suspend fun getPendingUnloadingRows(
        indent: String,
        wsNo: String,
        vehicleNo: String,
    ): List<UnloadingRow> = withContext(Dispatchers.IO) {

        val rows = mutableListOf<UnloadingRow>()

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
                Log.e(TAG, "getPendingUnloadingRows: DB connection failed")
                return@withContext emptyList()
            }

            val query = """
            SELECT U_Code, U_NoOfBags, U_Moisture, U_Avg, U_High
            FROM Vipl_ETUnload
            WHERE LTRIM(RTRIM(CAST(U_Indent    AS VARCHAR(50)))) = LTRIM(RTRIM(CAST(? AS VARCHAR(50))))
              AND LTRIM(RTRIM(CAST(U_WsNo      AS VARCHAR(50)))) = LTRIM(RTRIM(CAST(? AS VARCHAR(50))))
              AND LTRIM(RTRIM(CAST(U_VehicleNo AS VARCHAR(50)))) = LTRIM(RTRIM(CAST(? AS VARCHAR(50))))
              AND LTRIM(RTRIM(CAST(U_UnloadStatus AS VARCHAR(10)))) = 'N'
            ORDER BY U_Code
        """.trimIndent()

            statement = connection.prepareStatement(query)
            statement.queryTimeout = 10000
            statement.setString(1, indent)
            statement.setString(2, wsNo)
            statement.setString(3, vehicleNo)

            resultSet = statement.executeQuery()

            var sr = 1
            while (resultSet.next()) {
                val uCode = resultSet.getLong("U_Code")
                val bags = resultSet.getInt("U_NoOfBags").toString()
                val moisture = resultSet.getDouble("U_Moisture").toString()

                rows.add(
                    UnloadingRow(
                        srNo = sr++,
                        bags = bags,
                        moisture = moisture,
                        uCode = if (uCode > 0) uCode else -1L,
                        isSaving = false
                        // isPersisted = true by default → so Done button shows green
                    )
                )
            }

            Log.d(
                TAG,
                "getPendingUnloadingRows: ${rows.size} pending rows for indent=$indent wsNo=$wsNo vehicle=$vehicleNo"
            )

        } catch (e: Exception) {
            Log.e(TAG, "getPendingUnloadingRows error", e)
        } finally {
            try { resultSet?.close() } catch (_: Exception) {}
            try { statement?.close() } catch (_: Exception) {}
            try { connection?.close() } catch (_: Exception) {}
        }

        rows
    }

    suspend fun getMoistureRejectionValue(): Double = withContext(Dispatchers.IO) {

        var connection: Connection? = null
        var statement: PreparedStatement? = null
        var resultSet: ResultSet? = null
        var value = 0.0

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
                Log.e(TAG, "getMoistureRejectionValue: DB connection failed")
                return@withContext 0.0
            }

            // Log every column so we can see the real column name in VIPL_Sys1
            val query = """
            SELECT TOP 1 *
            FROM VIPL_Sys1
        """.trimIndent()

            Log.d(TAG, "getMoistureRejectionValue query : $query")

            statement = connection.prepareStatement(query)
            statement.queryTimeout = 10000
            resultSet = statement.executeQuery()

            if (resultSet.next()) {
                // Log all column names + raw values so we can verify the column exists
                val meta = resultSet.metaData
                val colCount = meta.columnCount
                for (i in 1..colCount) {
                    val colName = meta.getColumnName(i)
                    val raw = resultSet.getObject(i)
                    Log.d(TAG, "VIPL_Sys1 col[$i] = '$colName' -> $raw (${raw?.javaClass?.simpleName})")
                }

                // Now read the specific column
                val raw = resultSet.getObject("C_MoistureRejectionValue")
                Log.d(TAG, "getMoistureRejectionValue raw object = $raw")

                value = when (raw) {
                    null -> 0.0
                    is Number -> raw.toDouble()
                    else -> raw.toString().trim().toDoubleOrNull() ?: 0.0
                }
            } else {
                Log.w(TAG, "getMoistureRejectionValue: no rows in VIPL_Sys1")
            }

            Log.d(TAG, "getMoistureRejectionValue -> $value")

        } catch (e: Exception) {
            Log.e(TAG, "getMoistureRejectionValue error", e)
        } finally {
            try { resultSet?.close() } catch (_: Exception) {}
            try { statement?.close() } catch (_: Exception) {}
            try { connection?.close() } catch (_: Exception) {}
        }

        value
    }

}