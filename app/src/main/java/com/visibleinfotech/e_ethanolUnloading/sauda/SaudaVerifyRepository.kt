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

class SaudaVerifyRepository(
    private val context: Context,
) {
    companion object {
        private const val TAG = "SaudaVerifyRepo"
    }

    private val mainConstant = MainConstant(context)

    suspend fun getVerifyList(userId: String): List<SaudaItem> {
        return withContext(Dispatchers.IO) {
            var connection: Connection? = null
            var statement: PreparedStatement? = null
            var resultSet: ResultSet? = null
            val list = mutableListOf<SaudaItem>()

            try {
                Log.d(TAG, "=========== GET VERIFY LIST START ===========")
                val t0 = System.currentTimeMillis()

                // 🔒 Guard: must be numeric PerUserId, not display name
                val perUserId = userId.trim().toIntOrNull()
                if (perUserId == null) {
                    Log.e(TAG, "❌ getVerifyList: userId '$userId' is not numeric — pass PerUserId, not the name")
                    return@withContext emptyList()
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
                    return@withContext emptyList()
                }
                Log.d(TAG, "   ✅ DB connected")

                val query = """
SELECT
    CAST(ac.PL_Plotid AS VARCHAR(30))                          AS PL_Plotid,
    ISNULL(am.account_name, '')                                AS partyName,
    ISNULL(pm.place_name, '')                                  AS place_name,
    ISNULL(ct.cCropType, '')                                   AS cCropType,
    ISNULL(ac.PL_LandName, '')                                 AS PL_LandName,
    ISNULL(CAST(ac.PL_Area           AS VARCHAR(20)), '')      AS PL_Area,
    ISNULL(CAST(ac.PL_Rate           AS VARCHAR(20)), '')      AS PL_Rate,
    ISNULL(CAST(ac.PL_Amt            AS VARCHAR(20)), '')      AS PL_Amt,
    ISNULL(CAST(ac.PL_PaymentDueDays AS VARCHAR(20)), '')      AS PL_PaymentDueDays,
    ISNULL(CAST(ac.PL_CashDisc       AS VARCHAR(20)), '')      AS PL_CashDisc,
    ISNULL(CAST(ac.PL_WtDed          AS VARCHAR(20)), '')      AS PL_WtDed,

    ISNULL(ac.Pl_PaymentCond, '')                              AS Pl_PaymentCond,
    ISNULL(CAST(ac.PL_BrokerCode AS VARCHAR(20)), '')          AS PL_BrokerCode,
    ISNULL(bm.account_name, '')                                AS brokerName,
    ISNULL(CONVERT(VARCHAR(19), ac.PL_CreatedOn, 120), '')     AS PL_CreatedOn,
    ISNULL(ac.PL_CreatedBy, '')                                AS PL_CreatedBy,
    ISNULL(ac.PL_Approve, '')                                  AS PL_Approve,
    ISNULL(ac.PL_Verify, '')                                   AS PL_Verify, 
    ISNULL(ac.PL_ApprovedBy, '')                               AS PL_ApprovedBy,
    ISNULL(CONVERT(VARCHAR(19), ac.PL_ApprovedDate, 120), '')  AS PL_ApprovedDate,
    CAST(ac.PL_PerBagDed AS VARCHAR(30))                     AS PL_PerBagDed 
FROM VIPL_CanePlotSurvey ac
INNER JOIN VIPL_Accounts_master am ON am.account_id = ac.PL_GrowerCode
INNER JOIN Vipl_place_master pm   ON pm.place_id  = ac.PL_VillageCode
INNER JOIN VIPL_CropType ct       ON ct.nCropId   = ac.PL_CropId
LEFT JOIN VIPL_Accounts_master bm ON bm.account_id = ac.PL_BrokerCode
WHERE ac.PL_Enable = 1
  AND ac.PL_Approve = 'Y'
  AND (ac.PL_Verify = 'N' OR ac.PL_Verify IS NULL)
  AND EXISTS (
      SELECT 1
      FROM VIPL_PaymentPermission pp
      WHERE pp.PerCode = ac.PL_PerCode
        AND pp.PerUserId = ?
  )
ORDER BY ac.PL_PlotID DESC
""".trimIndent()

                Log.d(TAG, "   Query: $query")
                Log.d(TAG, "   PerUserId (int): $perUserId")

                statement = connection.prepareStatement(query)
                statement.setInt(1, perUserId)     // 👈 bind numeric PerUserId
                resultSet = statement.executeQuery()

                while (resultSet.next()) {
                    list.add(
                        SaudaItem(
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
                            approve = resultSet.getString("PL_Approve")?.trim() ?: "",
                            saudaApprovedBy = resultSet.getString("PL_ApprovedBy")?.trim() ?: "",
                            perBagDed = resultSet.getString("PL_PerBagDed")?.trim() ?: "",
                            saudaApprovedDate = resultSet.getString("PL_ApprovedDate")?.trim() ?: ""

                        )
                    )
                }

                Log.d(TAG, "   ✅ Total records loaded: ${list.size}")
                Log.d(TAG, "   ⏱ Elapsed: ${System.currentTimeMillis() - t0} ms")
                Log.d(TAG, "=========== GET VERIFY LIST END ===========")
                return@withContext list

            } catch (e: SQLException) {
                Log.e(TAG, "❌ SQL Error in getVerifyList: ${e.message}", e)
                emptyList()
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error in getVerifyList: ${e.message}", e)
                emptyList()
            } finally {
                try {
                    resultSet?.close()
                    statement?.close()
                    connection?.close()
                    Log.d(TAG, "   🔒 Resources closed")
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Close Error in getVerifyList: ${e.message}")
                }
            }
        }
    }

    // ✅ UNCHANGED — no permission check, same signature as before
    suspend fun verifySauda(
        plotId: String,
        verifiedBy: String,
        verifiedDate: String,
    ): SaudaApproveResult {
        return withContext(Dispatchers.IO) {
            var connection: Connection? = null
            var statement: PreparedStatement? = null

            try {
                Log.d(TAG, "=========== VERIFY SAUDA START ===========")
                Log.d(TAG, "   PlotId      : $plotId")
                Log.d(TAG, "   VerifiedBy  : $verifiedBy")
                Log.d(TAG, "   VerifiedDate: $verifiedDate")
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
                       SET PL_Verify       = 'Y',
                           PL_VerifiedBy   = ?,
                           PL_VerifiedDate = ?
                     WHERE PL_Plotid = ?
                       AND PL_Enable  = 1
                """.trimIndent()

                Log.d(TAG, "   Query: $query")

                statement = connection.prepareStatement(query)
                statement.setString(1, verifiedBy)
                statement.setString(2, verifiedDate)
                statement.setString(3, plotId)

                val rowsAffected = statement.executeUpdate()
                Log.d(TAG, "   rowsAffected = $rowsAffected")

                Log.d(TAG, "   ⏱ Elapsed: ${System.currentTimeMillis() - t0} ms")
                Log.d(TAG, "=========== VERIFY SAUDA END ===========")

                if (rowsAffected > 0) {
                    SaudaApproveResult(true, "Sauda $plotId verified successfully")
                } else {
                    SaudaApproveResult(false, "No record updated (plot not found)")
                }

            } catch (e: SQLException) {
                Log.e(TAG, "❌ SQL Error in verifySauda: ${e.message}", e)
                SaudaApproveResult(false, "Database error: ${e.message}")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error in verifySauda: ${e.message}", e)
                SaudaApproveResult(false, "Error: ${e.message}")
            } finally {
                try {
                    statement?.close()
                    connection?.close()
                    Log.d(TAG, "   🔒 Resources closed")
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Close Error in verifySauda: ${e.message}")
                }
            }
        }
    }

    // ✅ NEW: allows editing Rate / Area / DueDays / CashDisc / WtDed / PerBagDed
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
                Log.d(TAG, "=========== UPDATE SAUDA (VERIFY) START ===========")
                Log.d(TAG, "   PlotId    : $plotId")
                Log.d(TAG, "   Rate      : $rate")
                Log.d(TAG, "   Area      : $area")
                Log.d(TAG, "   DueDays   : $paymentDueDays")
                Log.d(TAG, "   CashDisc  : $cashDisc")
                Log.d(TAG, "   WtDed     : $wtDed")
                Log.d(TAG, "   PerBagDed : $perBagDed")

                if (plotId.isBlank()) {
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
                    return@withContext Result.failure(IllegalStateException("Database connection failed"))
                }
                Log.d(TAG, "   ✅ DB connected")

                val query = """
                    UPDATE VIPL_CanePlotSurvey
                       SET PL_Rate           = TRY_CAST(? AS DECIMAL(18,2)),
                           PL_Area           = TRY_CAST(? AS DECIMAL(18,3)),
                           PL_PaymentDueDays = TRY_CAST(? AS INT),
                           PL_CashDisc       = TRY_CAST(? AS DECIMAL(18,2)),
                           PL_WtDed          = TRY_CAST(? AS DECIMAL(18,3)),
                           PL_PerBagDed      = TRY_CAST(? AS DECIMAL(18,3))
                     WHERE PL_Plotid = ?
                       AND PL_Enable  = 1
                """.trimIndent()

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
                Log.d(TAG, "=========== UPDATE SAUDA (VERIFY) END ===========")

                if (rows > 0) Result.success(Unit)
                else Result.failure(IllegalStateException("No record updated"))
            } catch (e: SQLException) {
                Log.e(TAG, "❌ SQL Error in updateSauda: ${e.message}", e)
                Result.failure(e)
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error in updateSauda: ${e.message}", e)
                Result.failure(e)
            } finally {
                try { statement?.close() } catch (_: Exception) {}
                try { connection?.close() } catch (_: Exception) {}
            }
        }
    }

}