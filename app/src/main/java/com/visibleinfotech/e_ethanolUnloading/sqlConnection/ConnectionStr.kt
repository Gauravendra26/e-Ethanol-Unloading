package com.visibleinfotech.e_ethanolUnloading.sqlConnection

import android.annotation.SuppressLint
import android.os.StrictMode
import android.util.Log
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException

class ConnectionStr {

    @SuppressLint("NewApi")
    fun connectionclasss(user: String, password: String, database: String, server: String, instance: String?): Connection? {
        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)
        var connection: Connection? = null
        val connectionURL: String

        try {
            Class.forName("net.sourceforge.jtds.jdbc.Driver")

            connectionURL = if (!instance.isNullOrEmpty()) {
                "jdbc:jtds:sqlserver://$server;instance=$instance;databaseName=$database;user=$user;password=$password;"
            } else {
                "jdbc:jtds:sqlserver://$server;databaseName=$database;user=$user;password=$password;"
            }

            Log.d("DBConnection", "Attempting connection with URL: $connectionURL")
            connection = DriverManager.getConnection(connectionURL)
            Log.d("DBConnection", "Connection successful")

        } catch (e: ClassNotFoundException) {
            Log.e("DBConnection", "JDBC Driver not found: ${e.message}", e)
        } catch (e: SQLException) {
            Log.e("DBConnection", "SQL Exception: ${e.message}", e)
        } catch (e: Exception) {
            Log.e("DBConnection", "Unexpected error: ${e.message}", e)
        }

        return connection
    }
}