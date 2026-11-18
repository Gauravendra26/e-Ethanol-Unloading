package com.visibleinfotech.e_ethanolUnloading.sauda

import android.content.Context
import android.util.Log
import com.visibleinfotech.e_ethanolUnloading.MainConstant
import com.visibleinfotech.e_ethanolUnloading.sauda.models.SaudaApproveResult
import com.visibleinfotech.e_ethanolUnloading.sauda.models.SaudaItem
import com.visibleinfotech.e_ethanolUnloading.sqlConnection.ConnectionStr
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.SQLException

class SaudaRepository(
    private val context: Context,
) {
    companion object {
        private const val TAG = "SaudaRepositoryLog"
    }

    private val mainConstant = MainConstant(context)

    suspend fun getSaudaList(userId: String): List<SaudaItem> {
        return withContext(Dispatchers.IO) {
            var connection: Connection? = null
            var statement: PreparedStatement? = null
            var resultSet: ResultSet? = null

            val saudaList = mutableListOf<SaudaItem>()

            try {
                Log.d(TAG, "=========== GET SAUDA LIST START ===========")
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
SELECT
    CAST(ac.PL_Plotid AS VARCHAR(30))                     AS PL_Plotid,
    ISNULL(am.account_name, '')                           AS partyName,
    ISNULL(pm.place_name, '')                             AS place_name,
    ISNULL(ct.cCropType, '')                              AS cCropType,
    ISNULL(ac.PL_LandName, '')                            AS PL_LandName,
    ISNULL(CAST(ac.PL_Area           AS VARCHAR(20)), '') AS PL_Area,
    ISNULL(CAST(ac.PL_Rate           AS VARCHAR(20)), '') AS PL_Rate,
    ISNULL(CAST(ac.PL_Amt            AS VARCHAR(20)), '') AS PL_Amt,
    ISNULL(CAST(ac.PL_PaymentDueDays AS VARCHAR(20)), '') AS PL_PaymentDueDays,
    ISNULL(CAST(ac.PL_CashDisc       AS VARCHAR(20)), '') AS PL_CashDisc,
    ISNULL(CAST(ac.PL_WtDed          AS VARCHAR(20)), '') AS PL_WtDed,
    ISNULL(ac.Pl_PaymentCond, '')                         AS Pl_PaymentCond,
    ISNULL(CAST(ac.PL_BrokerCode AS VARCHAR(20)), '')     AS PL_BrokerCode,
    ISNULL(bm.account_name, '')                           AS brokerName,
    ISNULL(CONVERT(VARCHAR(19), ac.PL_CreatedOn, 120), '') AS PL_CreatedOn,
    ISNULL(ac.PL_CreatedBy, '')                           AS PL_CreatedBy,
    ISNULL(ac.PL_Approve, '')                             AS PL_Approve,
    CAST(ac.PL_PerBagDed AS VARCHAR(30))                     AS PL_PerBagDed 
FROM VIPL_CanePlotSurvey ac
INNER JOIN VIPL_Accounts_master am ON am.account_id = ac.PL_GrowerCode
INNER JOIN Vipl_place_master pm   ON pm.place_id  = ac.PL_VillageCode
INNER JOIN VIPL_CropType ct       ON ct.nCropId   = ac.PL_CropId
LEFT JOIN VIPL_Accounts_master bm ON bm.account_id = ac.PL_BrokerCode
WHERE ac.PL_Enable = 1
  AND (ac.PL_Approve = 'N' OR ac.PL_Approve IS NULL)
  AND EXISTS (
      SELECT 1
      FROM VIPL_PaymentPermission pp
      WHERE pp.PerCode = ac.PL_PerCode
        AND pp.PerUserId = ?
  )
ORDER BY ac.PL_PlotID DESC
""".trimIndent()

                Log.d(TAG, "   Query: $query")
                Log.d(TAG, "   PerUserId: $userId")

                statement = connection.prepareStatement(query)
                statement.setString(1, userId)
                resultSet = statement.executeQuery()

                while (resultSet.next()) {
                    val item = SaudaItem(
                        plotId = resultSet.getString("PL_Plotid")?.trim() ?: "",
                        partyName = resultSet.getString("partyName")?.trim() ?: "",
                        placeName = resultSet.getString("place_name")?.trim() ?: "",
                        cropType = resultSet.getString("cCropType")?.trim() ?: "",
                        landName = resultSet.getString("PL_LandName")?.trim() ?: "",
                        area = resultSet.getString("PL_Area")?.trim() ?: "0",
                        rate = resultSet.getString("PL_Rate")?.trim() ?: "0",
                        amount = resultSet.getString("PL_Amt")?.trim() ?: "0",
                        paymentCond = resultSet.getString("Pl_PaymentCond")?.trim() ?: "",
                        paymentDueDays = resultSet.getString("PL_PaymentDueDays")?.trim() ?: "0",
                        cashDisc = resultSet.getString("PL_CashDisc")?.trim() ?: "0",
                        wtDed = resultSet.getString("PL_WtDed")?.trim() ?: "0",
                        brokerCode = resultSet.getString("PL_BrokerCode")?.trim() ?: "",
                        brokerName = resultSet.getString("brokerName")?.trim() ?: "",
                        createdOn = resultSet.getString("PL_CreatedOn")?.trim() ?: "",
                        createdBy = resultSet.getString("PL_CreatedBy")?.trim() ?: "",
                        perBagDed = resultSet.getString("PL_PerBagDed")?.trim() ?: "",
                        approve = resultSet.getString("PL_Approve")?.trim() ?: ""
                    )
                    saudaList.add(item)
                }

                Log.d(TAG, "   ✅ Total sauda records loaded: ${saudaList.size}")
                Log.d(TAG, "   ⏱ Elapsed: ${System.currentTimeMillis() - t0} ms")
                Log.d(TAG, "=========== GET SAUDA LIST END ===========")
                return@withContext saudaList

            } catch (e: SQLException) {
                Log.e(TAG, "❌ SQL Error in getSaudaList: ${e.message}", e)
                return@withContext emptyList()
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error in getSaudaList: ${e.message}", e)
                return@withContext emptyList()
            } finally {
                try {
                    resultSet?.close()
                    statement?.close()
                    connection?.close()
                    Log.d(TAG, "   🔒 Resources closed")
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Close Error in getSaudaList: ${e.message}")
                }
            }
        }
    }

    suspend fun approveSauda(
        plotId: String,
        approvedBy: String,
        approvedDate: String,
    ): SaudaApproveResult {
        return withContext(Dispatchers.IO) {
            var connection: Connection? = null
            var statement: PreparedStatement? = null

            try {
                Log.d(TAG, "=========== APPROVE SAUDA START ===========")
                Log.d(TAG, "   PlotId      : $plotId")
                Log.d(TAG, "   ApprovedBy  : $approvedBy")
                Log.d(TAG, "   ApprovedDate: $approvedDate")
                val t0 = System.currentTimeMillis()

                if (plotId.isBlank()) {
                    Log.w(TAG, "   ⚠ Blank plotId")
                    return@withContext SaudaApproveResult(false, "Plot ID is required")
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
                    Log.e(TAG, "❌ Database connection failed")
                    return@withContext SaudaApproveResult(false, "Database connection failed")
                }
                Log.d(TAG, "   ✅ DB connected")

                val query = """
            UPDATE VIPL_CanePlotSurvey
               SET PL_Approve      = 'Y',
                   PL_ApprovedBy   = ?,
                   PL_ApprovedDate = ?,
                   PL_Verify       = 'Y'
             WHERE PL_Plotid = ?
               AND PL_Enable  = 1
            """.trimIndent()

                Log.d(TAG, "   Query: $query")

                statement = connection.prepareStatement(query)
                statement.setString(1, approvedBy)
                statement.setString(2, approvedDate)
                statement.setString(3, plotId)

                val rowsAffected = statement.executeUpdate()
                Log.d(TAG, "   rowsAffected = $rowsAffected")

                Log.d(TAG, "   ⏱ Elapsed: ${System.currentTimeMillis() - t0} ms")
                Log.d(TAG, "=========== APPROVE SAUDA END ===========")

                if (rowsAffected > 0) {
                    SaudaApproveResult(true, "Sauda $plotId approved successfully")
                } else {
                    SaudaApproveResult(false, "No record updated (plot not found)")
                }

            } catch (e: SQLException) {
                Log.e(TAG, "❌ SQL Error in approveSauda: ${e.message}", e)
                SaudaApproveResult(false, "Database error: ${e.message}")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error in approveSauda: ${e.message}", e)
                SaudaApproveResult(false, "Error: ${e.message}")
            } finally {
                try {
                    statement?.close()
                    connection?.close()
                    Log.d(TAG, "   🔒 Resources closed")
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Close Error in approveSauda: ${e.message}")
                }
            }
        }
    }

    suspend fun rejectSauda(
        plotId: String,
        rejectedBy: String,
        rejectedDate: String,
    ): Result<Unit> {
        return withContext(Dispatchers.IO) {
            var connection: Connection? = null
            var statement: PreparedStatement? = null

            try {
                Log.d(TAG, "=========== REJECT SAUDA START ===========")
                Log.d(TAG, "   PlotId      : $plotId")
                Log.d(TAG, "   RejectedBy  : $rejectedBy")
                Log.d(TAG, "   RejectedDate: $rejectedDate")
                val t0 = System.currentTimeMillis()

                if (plotId.isBlank()) {
                    Log.w(TAG, "   ⚠ Blank plotId")
                    return@withContext Result.failure(IllegalArgumentException("Plot ID is required"))
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
                    Log.e(TAG, "❌ Database connection failed")
                    return@withContext Result.failure(IllegalStateException("Database connection failed"))
                }
                Log.d(TAG, "   ✅ DB connected")

                val query = """
            UPDATE VIPL_CanePlotSurvey
               SET PL_Approve      = 'R',
                   PL_ApprovedBy   = ?,
                   PL_ApprovedDate = ?,
                   PL_Verify       = 'R'
             WHERE PL_Plotid = ?
               AND PL_Enable  = 1
            """.trimIndent()

                Log.d(TAG, "   Query: $query")

                statement = connection.prepareStatement(query)
                statement.setString(1, rejectedBy)
                statement.setString(2, rejectedDate)
                statement.setString(3, plotId)

                val rowsAffected = statement.executeUpdate()
                Log.d(TAG, "   rowsAffected = $rowsAffected")

                Log.d(TAG, "   ⏱ Elapsed: ${System.currentTimeMillis() - t0} ms")
                Log.d(TAG, "=========== REJECT SAUDA END ===========")

                if (rowsAffected > 0) {
                    Result.success(Unit)
                } else {
                    Result.failure(IllegalStateException("No record updated (plot not found)"))
                }

            } catch (e: SQLException) {
                Log.e(TAG, "❌ SQL Error in rejectSauda: ${e.message}", e)
                Result.failure(e)
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error in rejectSauda: ${e.message}", e)
                Result.failure(e)
            } finally {
                try {
                    statement?.close()
                    connection?.close()
                    Log.d(TAG, "   🔒 Resources closed")
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Close Error in rejectSauda: ${e.message}")
                }
            }
        }
    }

    suspend fun updateSauda(
        plotId: String,
        rate: String,
        area: String,
        paymentDueDays: String,
        cashDisc: String,
        wtDed: String,
        perBagDed: String,
    ): Result<Unit> {
        return withContext(Dispatchers.IO) {
            var connection: Connection? = null
            var statement: PreparedStatement? = null

            try {
                Log.d(TAG, "=========== UPDATE SAUDA START ===========")
                Log.d(TAG, "   PlotId        : $plotId")
                Log.d(TAG, "   Rate          : $rate")
                Log.d(TAG, "   Area          : $area")
                Log.d(TAG, "   DueDays       : $paymentDueDays")
                Log.d(TAG, "   CashDisc      : $cashDisc")
                Log.d(TAG, "   WtDed         : $wtDed")
                Log.d(TAG, "   PerBagDed     : $perBagDed")

                if (plotId.isBlank()) {
                    Log.w(TAG, "   ⚠ Blank plotId")
                    return@withContext Result.failure(IllegalArgumentException("Plot ID is required"))
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
                    Log.e(TAG, "❌ Database connection failed")
                    return@withContext Result.failure(IllegalStateException("Database connection failed"))
                }
                Log.d(TAG, "   ✅ DB connected")

                val query = """
                    UPDATE VIPL_CanePlotSurvey
                       SET PL_Rate            = TRY_CAST(? AS DECIMAL(18,2)),
                           PL_Area            = TRY_CAST(? AS DECIMAL(18,3)),
                           PL_PaymentDueDays  = TRY_CAST(? AS INT),
                           PL_CashDisc        = TRY_CAST(? AS DECIMAL(18,2)),
                           PL_WtDed           = TRY_CAST(? AS DECIMAL(18,3)),
                           PL_PerBagDed       = TRY_CAST(? AS DECIMAL(18,3))
                     WHERE PL_Plotid = ?
                       AND PL_Enable  = 1
                """.trimIndent()

                Log.d(TAG, "   Query: $query")

                statement = connection.prepareStatement(query)
                statement.setString(1, rate.trim().ifBlank { "0" })
                statement.setString(2, area.trim().ifBlank { "0" })
                statement.setString(3, paymentDueDays.trim().ifBlank { "0" })
                statement.setString(4, cashDisc.trim().ifBlank { "0" })
                statement.setString(5, wtDed.trim().ifBlank { "0" })
                statement.setString(6, perBagDed.trim().ifBlank { "0" })
                statement.setString(7, plotId)

                val rows = statement.executeUpdate()
                Log.d(TAG, "   rowsAffected = $rows")
                Log.d(TAG, "=========== UPDATE SAUDA END ===========")

                if (rows > 0) Result.success(Unit)
                else Result.failure(IllegalStateException("No record updated (plot not found)"))
            } catch (e: SQLException) {
                Log.e(TAG, "❌ SQL Error in updateSauda: ${e.message}", e)
                Result.failure(e)
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error in updateSauda: ${e.message}", e)
                Result.failure(e)
            } finally {
                try {
                    statement?.close()
                    connection?.close()
                    Log.d(TAG, "   🔒 Resources closed")
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Close Error in updateSauda: ${e.message}")
                }
            }
        }
    }

}