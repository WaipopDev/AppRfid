package com.example.appuhfkit

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.uhf.base.UHFManager
import com.uhf.base.UHFModuleType
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class UHFWrapper(private val context: Context) {
    
    companion object {
        private const val TAG = "UHFWrapper"
    }
    
    private var uhfManager: UHFManager? = null
    private var executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())
    private var isScanning = false
    private var onTagScannedListener: ((String) -> Unit)? = null
    private var isReleased = false
    
    init {
        initializeUHF()
    }
    
    private fun initializeUHF() {
        if (isReleased) {
            Log.w(TAG, "Cannot initialize UHF - wrapper is released")
            return
        }
        
        try {
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
                        uhfManager?.powerSet(33) // ตั้งค่าพลังงาน 33dBm
                        uhfManager?.frequencyModeSet(3) // ตั้งค่าความถี่ US
                        Log.d(TAG, "UHF initialized successfully")
                    } else {
                        Log.e(TAG, "Failed to power on UHF module")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error initializing UHF: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error executing initialization: ${e.message}")
        }
    }
    
    fun startScan(onTagScanned: (String) -> Unit) {
        if (isScanning) {
            Log.w(TAG, "Already scanning")
            return
        }
        
        if (isReleased) {
            Log.e(TAG, "Cannot start scan - UHF wrapper is released")
            return
        }
        
        if (executor.isShutdown) {
            Log.e(TAG, "Cannot start scan - executor is shutdown, recreating...")
            executor = Executors.newSingleThreadExecutor()
        }
        
        onTagScannedListener = onTagScanned
        isScanning = true
        
        try {
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
                    } else {
                        Log.e(TAG, "Failed to start inventory")
                        isScanning = false
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error starting scan: ${e.message}")
                    isScanning = false
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error executing start scan: ${e.message}")
            isScanning = false
        }
    }
    
    fun stopScan() {
        if (!isScanning) return
        
        Log.d(TAG, "Stopping scan...")
        isScanning = false
        onTagScannedListener = null
        
        if (isReleased || executor.isShutdown) {
            Log.w(TAG, "Cannot execute stop scan - executor is shutdown")
            return
        }
        
        try {
            executor.execute {
                try {
                    val stopResult = uhfManager?.stopInventory() ?: false
                    Log.d(TAG, "Stop inventory: $stopResult")
                    
                    // รอให้ thread หยุดทำงาน
                    Thread.sleep(200)
                    Log.d(TAG, "Scan stopped completely")
                } catch (e: Exception) {
                    Log.e(TAG, "Error stopping scan: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error executing stop scan: ${e.message}")
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
        Log.d(TAG, "Releasing UHF resources...")
        isReleased = true
        stopScan()
        
        // Move the blocking operations to executor thread to avoid blocking main thread
        if (!executor.isShutdown) {
            try {
                executor.execute {
                    performHardwareShutdown()
                }
                
                // Wait briefly for the shutdown task to start
                Thread.sleep(100)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error scheduling hardware shutdown: ${e.message}")
                // Fallback: try immediate shutdown
                performHardwareShutdown()
            }
        } else {
            // If executor is already shutdown, do immediate shutdown
            performHardwareShutdown()
        }
        
        // Clean up executor
        try {
            if (!executor.isShutdown) {
                executor.shutdown()
                if (!executor.awaitTermination(3, TimeUnit.SECONDS)) {
                    executor.shutdownNow()
                    Log.w(TAG, "Executor did not terminate gracefully, forced shutdown")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error shutting down executor: ${e.message}")
            if (!executor.isShutdown) {
                executor.shutdownNow()
            }
        }
        
        Log.d(TAG, "UHF resources released completely")
    }
    
    private fun performHardwareShutdown() {
        try {
            Log.d(TAG, "Starting hardware shutdown sequence...")
            
            // ขั้นตอนที่ 1: หยุดการสแกนและ inventory ทั้งหมด
            uhfManager?.stopInventory()
            Log.d(TAG, "Stop inventory called - step 1")
            Thread.sleep(300) // รอให้ inventory หยุดสมบูรณ์
            
            // ขั้นตอนที่ 2: หยุดการทำงานของ antenna และ RF
            uhfManager?.stopInventory() // เรียกอีกครั้งเพื่อให้แน่ใจ
            Log.d(TAG, "Stop inventory called - step 2")
            Thread.sleep(200)
            
            // ขั้นตอนที่ 3: ลดพลังงาน antenna ลงเป็น 0 ก่อนปิด power
            try {
                uhfManager?.powerSet(0)
                Log.d(TAG, "Antenna power set to 0")
                Thread.sleep(200)
            } catch (e: Exception) {
                Log.w(TAG, "Could not set antenna power to 0: ${e.message}")
            }
            
            // ขั้นตอนที่ 4: ปิด power ของ UHF module
            val powerOffResult = uhfManager?.powerOff() ?: false
            Log.d(TAG, "UHF module power off result: $powerOffResult")
            
            // ขั้นตอนที่ 5: รอให้ hardware ปิดสมบูรณ์
            Thread.sleep(500)
            
            // ขั้นตอนที่ 6: ล้าง reference
            uhfManager = null
            Log.d(TAG, "Hardware shutdown sequence completed")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error during hardware shutdown: ${e.message}")
            // ในกรณีเกิด error ให้ล้าง reference ทันที
            uhfManager = null
        }
    }
} 