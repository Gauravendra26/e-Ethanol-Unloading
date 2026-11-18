package com.visibleinfotech.e_ethanolUnloading.sauda

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

class VerifiedSaudaRecordsRepository(
    private val context: Context,
) {
    companion object { private const val TAG = "VerifiedSaudaRepo" }

    private val mainConstant = MainConstant(context)

    suspend fun getVerifiedRecords(
        fromDate: String? = null,
        toDate: String? = null,
    ): List<VerifiedSaudaRecord> = withContext(Dispatchers.IO) {

        var connection: Connection? = null
        var statement: PreparedStatement? = null
        var resultSet: ResultSet? = null
        val list = mutableListOf<VerifiedSaudaRecord>()

        try {
            Log.d(TAG, "=========== GET VERIFIED SAUDA RECORDS START ===========")
            Log.d(TAG, "   fromDate=$fromDate, toDate=$toDate")

            val conStr = ConnectionStr()
            connection = conStr.connectionclasss(
                mainConstant.getDbUser(),
                mainConstant.getDbPass(),
                mainConstant.getDbName(),
                mainConstant.getIp(),
                mainConstant.getInstance()
            )
            if (connection == null) {
                Log.e(TAG, "❌ DB connection failed")
                return@withContext emptyList()
            }

            val hasFrom = !fromDate.isNullOrBlank()
            val hasTo = !toDate.isNullOrBlank()

            val dateClause = when {
                hasFrom && hasTo -> "AND CAST(ac.PL_VerifiedDate AS DATE) BETWEEN ? AND ?"
                hasFrom -> "AND CAST(ac.PL_VerifiedDate AS DATE) >= ?"
                hasTo -> "AND CAST(ac.PL_VerifiedDate AS DATE) <= ?"
                else -> ""
            }

            val query = """
                SELECT
                    CAST(ac.PL_Plotid AS VARCHAR(30))                          AS PL_Plotid,
                    ISNULL(am.account_name, '')                                AS partyName,
                    ISNULL(pm.place_name, '')                                  AS place_name,
                    ISNULL(ct.cCropType, '')                                   AS cCropType,
                    ISNULL(bm.account_name, '')                                AS brokerName,
                    ISNULL(CAST(ac.PL_Area           AS VARCHAR(20)), '')      AS PL_Area,
                    ISNULL(CAST(ac.PL_Rate           AS VARCHAR(20)), '')      AS PL_Rate,
                    ISNULL(CAST(ac.PL_PerBagDed      AS VARCHAR(20)), '')      AS PL_PerBagDed,
                    ISNULL(CAST(ac.PL_PaymentDueDays AS VARCHAR(20)), '')      AS PL_PaymentDueDays,
                    ISNULL(CAST(ac.PL_WtDed          AS VARCHAR(20)), '')      AS PL_WtDed,
                    ISNULL(CAST(ac.PL_CashDisc       AS VARCHAR(20)), '')      AS PL_CashDisc,
                    ISNULL(ac.PL_ApprovedBy, '')                               AS PL_ApprovedBy,
                    ISNULL(CONVERT(VARCHAR(19), ac.PL_ApprovedDate, 120), '')  AS PL_ApprovedDate,
                    ISNULL(ac.PL_VerifiedBy, '')                               AS PL_VerifiedBy,
                    ISNULL(CONVERT(VARCHAR(19), ac.PL_VerifiedDate, 120), '')  AS PL_VerifiedDate,
                    ISNULL(CONVERT(VARCHAR(19), ac.PL_CreatedOn, 120), '')     AS PL_CreatedOn
                FROM VIPL_CanePlotSurvey ac
                INNER JOIN VIPL_Accounts_master am ON am.account_id = ac.PL_GrowerCode
                INNER JOIN Vipl_place_master pm    ON pm.place_id  = ac.PL_VillageCode
                INNER JOIN VIPL_CropType ct        ON ct.nCropId   = ac.PL_CropId
                LEFT  JOIN VIPL_Accounts_master bm ON bm.account_id = ac.PL_BrokerCode
                WHERE ac.PL_Enable  = 1
                  AND ac.PL_Approve = 'Y'
                  AND ac.PL_Verify  = 'Y'
                  $dateClause
                ORDER BY ac.PL_VerifiedDate DESC, ac.PL_PlotID DESC
            """.trimIndent()

            Log.d(TAG, "   Query: $query")

            statement = connection.prepareStatement(query)

            var idx = 1
            if (hasFrom) statement.setString(idx++, fromDate)
            if (hasTo) statement.setString(idx++, toDate)

            resultSet = statement.executeQuery()

            while (resultSet.next()) {
                list.add(
                    VerifiedSaudaRecord(
                        plotId         = resultSet.getString("PL_Plotid")?.trim() ?: "",
                        partyName      = resultSet.getString("partyName")?.trim() ?: "",
                        placeName      = resultSet.getString("place_name")?.trim() ?: "",
                        cropType       = resultSet.getString("cCropType")?.trim() ?: "",
                        brokerName     = resultSet.getString("brokerName")?.trim() ?: "",
                        area           = resultSet.getString("PL_Area")?.trim() ?: "0",
                        rate           = resultSet.getString("PL_Rate")?.trim() ?: "0",
                        perBagDed      = resultSet.getString("PL_PerBagDed")?.trim() ?: "0",
                        paymentDueDays = resultSet.getString("PL_PaymentDueDays")?.trim() ?: "0",
                        wtDed          = resultSet.getString("PL_WtDed")?.trim() ?: "0",
                        cashDisc       = resultSet.getString("PL_CashDisc")?.trim() ?: "0",
                        approvedBy     = resultSet.getString("PL_ApprovedBy")?.trim() ?: "",
                        approvedDate   = resultSet.getString("PL_ApprovedDate")?.trim() ?: "",
                        verifiedBy     = resultSet.getString("PL_VerifiedBy")?.trim() ?: "",
                        verifiedDate   = resultSet.getString("PL_VerifiedDate")?.trim() ?: "",
                        createdOn      = resultSet.getString("PL_CreatedOn")?.trim() ?: "",
                    )
                )
            }

            Log.d(TAG, "   ✅ Loaded ${list.size} verified sauda records")
            return@withContext list

        } catch (e: SQLException) {
            Log.e(TAG, "❌ SQL Error: ${e.message}", e)
            emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error: ${e.message}", e)
            emptyList()
        } finally {
            try { resultSet?.close() } catch (_: Exception) {}
            try { statement?.close() } catch (_: Exception) {}
            try { connection?.close() } catch (_: Exception) {}
        }
    }
}