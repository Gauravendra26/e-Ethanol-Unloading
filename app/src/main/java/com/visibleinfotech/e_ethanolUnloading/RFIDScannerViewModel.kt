package com.visibleinfotech.e_ethanolUnloading

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rscja.deviceapi.RFIDWithUHFUART
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ScannerState(
    val isConnected: Boolean = false,
    val vehicleNumber: String = "",
    val tag1: String = "",
    val foundVehicle1: String = "",
    val isNewTag1: Boolean = false,
    val isScanning: Boolean = false
) {
    val hasValidTags: Boolean
        get() = tag1.isNotEmpty()
}

class RFIDScannerViewModel : ViewModel() {
    private val TAG = "RFIDScannerVM"
    private val TAG_SCAN = "RFIDScan"
    private val TAG_INIT = "RFIDInit"

    private var mReader: RFIDWithUHFUART? = null
    private val _scannerState = MutableStateFlow(ScannerState())
    val scannerState: StateFlow<ScannerState> = _scannerState.asStateFlow()

    private var isScanning = false
    private val SCAN_POWER = 20
    private val SCAN_TIMEOUT = 3000L
    private var isInitialized = false
    private var isInitializing = false

    fun initializeScanner(context: Context) {
        Log.d(TAG_INIT, "=== INITIALIZE SCANNER START ===")

        if (isInitialized && mReader != null) {
            Log.d(TAG_INIT, "Scanner already initialized")
            _scannerState.value = _scannerState.value.copy(
                isConnected = true
            )
            return
        }

        if (isInitializing) {
            Log.d(TAG_INIT, "Scanner initialization already in progress")
            return
        }

        isInitializing = true


        viewModelScope.launch {
            try {
                // Free any existing reader first
                try {
                    mReader?.free()
                    mReader = null
                } catch (e: Exception) {
                    Log.e(TAG_INIT, "Error freeing existing reader", e)
                }

                delay(200) // Small delay to ensure resources are released

                Log.d(TAG_INIT, "Getting RFIDWithUHFUART instance...")
                mReader = RFIDWithUHFUART.getInstance()
                Log.d(TAG_INIT, "mReader instance: ${mReader != null}")

                mReader?.let { reader ->
                    Log.d(TAG_INIT, "Calling reader.init()...")
                    val initResult = reader.init()
                    Log.d(TAG_INIT, "init() result: $initResult")

                    if (initResult) {
                        isInitialized = true
                        isInitializing = false
                        _scannerState.value = _scannerState.value.copy(
                            isConnected = true
                        )
                        Log.d(TAG_INIT, "✅ Scanner initialized successfully")
                    } else {
                        isInitialized = false
                        isInitializing = false
                        _scannerState.value = _scannerState.value.copy(
                            isConnected = false
                        )
                        Log.e(TAG_INIT, "❌ Scanner initialization failed")
                    }
                } ?: run {
                    isInitialized = false
                    isInitializing = false
                    Log.e(TAG_INIT, "❌ mReader is null after getInstance()")
                    _scannerState.value = _scannerState.value.copy(
                        isConnected = false
                    )
                }
            } catch (e: Exception) {
                isInitialized = false
                isInitializing = false
                Log.e(TAG_INIT, "❌ Scanner initialization error", e)
                _scannerState.value = _scannerState.value.copy(
                    isConnected = false
                )
            }
            Log.d(TAG_INIT, "=== INITIALIZE SCANNER END ===")
        }
    }

    fun scanSingleTag() {
        Log.d(TAG_SCAN, "=== SCAN SINGLE TAG START ===")

        if (_scannerState.value.isScanning) {
            Log.d(TAG_SCAN, "⚠️ Scan already in progress, ignoring request")
            return
        }

        if (mReader == null || !isInitialized) {
            Log.e(TAG_SCAN, "❌ Scanner not initialized")

            return
        }

        viewModelScope.launch {
            isScanning = true
            _scannerState.value = _scannerState.value.copy(
                isScanning = true
            )
            Log.d(TAG_SCAN, "✅ Scan started")

            try {
                // Clear previous tag and reset flags
                _scannerState.value = _scannerState.value.copy(
                    tag1 = "",
                    isNewTag1 = false,
                    foundVehicle1 = ""
                )

                val tag = performSingleTagScan()

                if (tag.isEmpty()) {
                    Log.w(TAG_SCAN, "⚠️ No tag found during scan")
                    _scannerState.value = _scannerState.value.copy(
                         isScanning = false,
                        tag1 = "",
                        isNewTag1 = false,
                        foundVehicle1 = ""
                    )
                    isScanning = false
                    return@launch
                }

                Log.d(TAG_SCAN, "✅ Found tag: $tag")

                // Update tag in state with proper flags
                _scannerState.value = _scannerState.value.copy(
                    tag1 = tag,
                    isNewTag1 = true,
                    foundVehicle1 = "New Tag",
                    isScanning = false
                )

            } catch (e: Exception) {
                Log.e(TAG_SCAN, "❌ Error during scan", e)
                _scannerState.value = _scannerState.value.copy(
                     isScanning = false,
                    isNewTag1 = false,
                    foundVehicle1 = ""
                )
            }

            isScanning = false
            _scannerState.value = _scannerState.value.copy(isScanning = false)
            Log.d(TAG_SCAN, "=== SCAN SINGLE TAG END ===")
        }
    }

    private suspend fun performSingleTagScan(): String {
        Log.d(TAG_SCAN, "=== PERFORM SINGLE TAG SCAN START ===")

        try {
            mReader?.let { reader ->
                Log.d(TAG_SCAN, "Setting scan power to $SCAN_POWER")
                reader.setPower(SCAN_POWER)

                delay(100)

                Log.d(TAG_SCAN, "Calling inventorySingleTag()...")
                val uhfTagInfo = reader.inventorySingleTag()

                if (uhfTagInfo != null) {
                    val epc = uhfTagInfo.getEPC()
                    Log.d(TAG_SCAN, "✅ inventorySingleTag returned tag: $epc")

                    reader.setPower(30)
                    Log.d(TAG_SCAN, "Power reset to 30")

                    if (isValidTag(epc)) {
                        Log.d(TAG_SCAN, "✅ Valid tag: $epc")
                        return epc
                    } else {
                        Log.w(TAG_SCAN, "⚠️ Invalid tag format: $epc")
                    }
                } else {
                    Log.d(TAG_SCAN, "inventorySingleTag returned null (no tag detected)")
                    reader.setPower(30)
                }
            } ?: run {
                Log.e(TAG_SCAN, "❌ mReader is null")
            }
        } catch (e: Exception) {
            Log.e(TAG_SCAN, "❌ Error in performSingleTagScan", e)
        }

        Log.d(TAG_SCAN, "=== PERFORM SINGLE TAG SCAN END ===")
        return ""
    }

    private fun isValidTag(tag: String): Boolean {
        if (tag.isEmpty()) {
            Log.d(TAG_SCAN, "❌ Tag is empty")
            return false
        }
        if (tag.length != 24) {
            Log.d(TAG_SCAN, "❌ Invalid tag length: ${tag.length}, expected 24")
            return false
        }
        if (tag.matches(Regex("^0+$"))) {
            Log.d(TAG_SCAN, "❌ Tag is all zeros")
            return false
        }
        if (!tag.matches(Regex("^[0-9A-Fa-f]+$"))) {
            Log.d(TAG_SCAN, "❌ Tag contains non-hex characters")
            return false
        }
        Log.d(TAG_SCAN, "✅ Tag validation passed")
        return true
    }

    fun updateTag1(value: String) {
        Log.d(TAG, "updateTag1 called with: '$value'")
        _scannerState.value = _scannerState.value.copy(tag1 = value)
    }

    fun updateVehicleNumber(value: String) {
        Log.d(TAG, "updateVehicleNumber called with: '$value'")
        _scannerState.value = _scannerState.value.copy(vehicleNumber = value)
    }

    fun clearTags() {
        Log.d(TAG, "=== CLEAR TAGS START ===")
        _scannerState.value = _scannerState.value.copy(
            tag1 = "",
            foundVehicle1 = "",
            isNewTag1 = false
        )
        Log.d(TAG, "=== CLEAR TAGS END ===")
    }

    fun saveTags(context: Context) {
        Log.d(TAG, "=== SAVE TAGS START ===")
        val state = _scannerState.value
        Log.d(TAG, "Current state: $state")

        if (!state.hasValidTags) {
            Log.w(TAG, "No tags to save")

            return
        }

        // Show success message (in real implementation, save to database)
        Log.d(TAG, "Saving tags...")
        _scannerState.value = state.copy(
           isNewTag1 = false,
            foundVehicle1 = state.vehicleNumber
        )
        Log.d(TAG, "Tags saved, state updated")
        Log.d(TAG, "=== SAVE TAGS END ===")
    }

    fun resetScanner() {
        Log.d(TAG, "=== RESET SCANNER START ===")
        try {
            mReader?.free()
            mReader = null
            isInitialized = false
            isInitializing = false
            _scannerState.value = ScannerState()
            Log.d(TAG, "Scanner reset successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error resetting scanner", e)
        }
        Log.d(TAG, "=== RESET SCANNER END ===")
    }

    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "=== VIEW MODEL CLEARED ===")
        try {
            mReader?.free()
            mReader = null
            isInitialized = false
            isInitializing = false
            Log.d(TAG, "Scanner resources freed")
        } catch (e: Exception) {
            Log.e(TAG, "Error freeing scanner", e)
        }
        Log.d(TAG, "=== VIEW MODEL CLEARED END ===")
    }
}