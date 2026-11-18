package com.visibleinfotech.e_ethanolUnloading


import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.TextView
import com.journeyapps.barcodescanner.CaptureActivity

class QRScannerActivity : CaptureActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val scanType = intent.getStringExtra("SCAN_TYPE") ?: "QR"
        val scanTitle = intent.getStringExtra("SCAN_TITLE") ?: "SCANNER"
        val scanColorHex = intent.getStringExtra("SCAN_COLOR") ?: "#0D47A1"

        val titleColor = try {
            Color.parseColor(scanColorHex)
        } catch (e: Exception) {
            Color.parseColor("#0D47A1")
        }

        val root = findViewById<FrameLayout>(android.R.id.content)
        val emoji = if (scanType == "BARCODE") "📊" else "📷"

        val titleView = TextView(this).apply {
            text = "$emoji  $scanTitle"
            textSize = 20f
            setTextColor(Color.WHITE)
            setBackgroundColor(titleColor)
            gravity = Gravity.CENTER
            setPadding(0, 60, 0, 60)
            setTypeface(typeface, Typeface.BOLD)
            elevation = 20f
            letterSpacing = 0.08f
        }

        val params = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.TOP
        }

        root.addView(titleView, params)
    }


}