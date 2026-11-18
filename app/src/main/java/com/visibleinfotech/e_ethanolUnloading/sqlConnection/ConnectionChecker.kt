package com.visibleinfotech.e_ethanolUnloading.sqlConnection

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

import com.visibleinfotech.e_ethanolUnloading.MainConstant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.sql.Connection

class ConnectionChecker(
    private val context: Context
) {

    private val mainConstant = MainConstant(context)

    suspend fun checkConnection(): ConnectionStatus {

        // Check Internet First
        if (!isInternetAvailable()) {
            return ConnectionStatus.NO_INTERNET
        }

        // Check SQL Server
        return withContext(Dispatchers.IO) {

            var connection: Connection? = null

            try {

                val conStr = ConnectionStr()

                connection = conStr.connectionclasss(
                    mainConstant.getDbUser(),
                    mainConstant.getDbPass(),
                    mainConstant.getDbName(),
                    mainConstant.getIp(),
                    mainConstant.getInstance()
                )

                if (connection != null && !connection.isClosed) {

                    connection.close()

                    ConnectionStatus.CONNECTED

                } else {

                    ConnectionStatus.SERVER_DOWN
                }

            } catch (e: Exception) {

                e.printStackTrace()

                ConnectionStatus.SERVER_DOWN
            }
        }
    }

    private fun isInternetAvailable(): Boolean {

        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE)
                    as ConnectivityManager

        val network =
            connectivityManager.activeNetwork ?: return false

        val capabilities =
            connectivityManager.getNetworkCapabilities(network)
                ?: return false

        return capabilities.hasCapability(
            NetworkCapabilities.NET_CAPABILITY_INTERNET
        )
    }
}