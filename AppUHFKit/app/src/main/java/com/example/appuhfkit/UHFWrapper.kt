package com.example.appuhfkit

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.uhf.base.UHFManager
import com.uhf.base.UHFModuleType
import java.util.concurrent.Executors

class UHFWrapper(private val context: Context) {
    
    companion object {
        private const val TAG = "UHFWrapper"
    }
    
    private var uhfManager: UHFManager? = null
    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())
    private var isScanning = false
    private var onTagScannedListener: ((String) -> Unit)? = null
    
    init {
        initializeUHF()
    }
    
    private fun initializeUHF() {
        executor.execute {
            try {
                // ใช้ SLR module
                uhfManager = UHFManager.getUHFImplSigleInstance(UHFModuleType.SLR_MODULE)
                
                // เริ่มต้น UHF module
                val powerOnResult = uhfManager?.powerOn() ?: false
                Log.d(TAG, "UHF Power On: $powerOnResult")
                
                if (powerOnResult) {
                    // ตั้งค่าพื้นฐาน
                    uhfManager?.changeConfig(true)
                    uhfManager?.powerSet(30) // ตั้งค่าพลังงาน 30dBm
                    uhfManager?.frequencyModeSet(3) // ตั้งค่าความถี่ US
                    Log.d(TAG, "UHF initialized successfully")
                } else {
                    Log.e(TAG, "Failed to power on UHF module")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error initializing UHF: ${e.message}")
            }
        }
    }
    
    fun startScan(onTagScanned: (String) -> Unit) {
        if (isScanning) return
        
        onTagScannedListener = onTagScanned
        isScanning = true
        
        executor.execute {
            try {
                // ตั้งค่า SLR inventory mode
                uhfManager?.slrInventoryModeSet(4)
                
                // เริ่มการสแกน
                val startResult = uhfManager?.startInventoryTag() ?: false
                Log.d(TAG, "Start inventory: $startResult")
                
                if (startResult) {
                    // เริ่ม thread สำหรับอ่าน tag
                    startTagReadingThread()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error starting scan: ${e.message}")
                isScanning = false
            }
        }
    }
    
    fun stopScan() {
        if (!isScanning) return
        
        isScanning = false
        onTagScannedListener = null
        
        executor.execute {
            try {
                val stopResult = uhfManager?.stopInventory() ?: false
                Log.d(TAG, "Stop inventory: $stopResult")
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping scan: ${e.message}")
            }
        }
    }
    
    private fun startTagReadingThread() {
        Thread {
            while (isScanning) {
                try {
                    val tagData = uhfManager?.readTagFromBuffer()
                    if (tagData != null && tagData.isNotEmpty()) {
                        // Based on UHFDemo implementation:
                        // tagData[0] = TID (or other data depending on mode)
                        // tagData[1] = EPC 
                        // tagData[2] = RSSI
                        val epc = if (tagData.size > 1) tagData[1] else ""
                        val rssi = if (tagData.size > 2) tagData[2] else ""
                        
                        if (epc.isNotEmpty()) {
                            mainHandler.post {
                                onTagScannedListener?.invoke("EPC: $epc, RSSI: $rssi")
                            }
                        }
                    }
                    
                    Thread.sleep(100) // อ่านทุก 100ms
                } catch (e: Exception) {
                    Log.e(TAG, "Error reading tag: ${e.message}")
                    break
                }
            }
        }.start()
    }
    
    fun isScanning(): Boolean = isScanning
    
    fun release() {
        stopScan()
        executor.shutdown()
        uhfManager = null
    }
} 