package com.example.appuhfkit

import android.app.Application
import android.util.Log
import com.tencent.mmkv.MMKV
import android.content.Intent
import android.content.Context
import android.content.IntentFilter

class MyApplication : Application() {
    
    companion object {
        private const val TAG = "MyApplication"
        private var uhfWrapper: UHFWrapper? = null
        private var screenOffReceiver: ScreenOffReceiver? = null
        
        fun getUHFWrapper(context: android.content.Context): UHFWrapper {
            if (uhfWrapper == null) {
                Log.d(TAG, "Creating new UHF wrapper instance")
                uhfWrapper = UHFWrapper(context.applicationContext)
            } else {
                Log.d(TAG, "Returning existing UHF wrapper instance")
            }
            return uhfWrapper!!
        }
        
        fun releaseUHFWrapper() {
            Log.d(TAG, "Releasing global UHF wrapper")
            try {
                uhfWrapper?.release()
                uhfWrapper = null
                Log.d(TAG, "UHF wrapper released successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Error releasing UHF wrapper: ${e.message}")
                uhfWrapper = null
            }
        }
        
        fun forceReleaseUHF() {
            Log.d(TAG, "Force releasing UHF wrapper")
            uhfWrapper = null
        }
        
        fun stopSystemScanningServices(context: Context) {
            Log.d(TAG, "Stopping system scanning services")
            try {
                // หยุด iScanPlus service
                val stopScanIntent = Intent("com.idata.iscan.action.STOP_SCAN")
                stopScanIntent.setPackage("com.idata.iscanplus")
                context.sendBroadcast(stopScanIntent)
                
                // หยุด iSEScannerService  
                val stopScannerIntent = Intent("com.idata.iscan.action.CLOSE_SCAN")
                stopScannerIntent.setPackage("iSEScannerService")
                context.sendBroadcast(stopScannerIntent)
                
                // ส่ง broadcast เพื่อหยุด scanning
                val scanOffIntent = Intent("com.idata.iscan.action.SCAN_OFF")
                context.sendBroadcast(scanOffIntent)
                
                Log.d(TAG, "System scanning services stop commands sent")
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping system scanning services: ${e.message}")
            }
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Application created")
        // เริ่มต้น MMKV
        MMKV.initialize(this)
        
        // ลงทะเบียน Screen Off/On Receiver
        registerScreenReceiver()
    }
    
    private fun registerScreenReceiver() {
        try {
            screenOffReceiver = ScreenOffReceiver()
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_OFF)
                addAction(Intent.ACTION_SCREEN_ON)
            }
            registerReceiver(screenOffReceiver, filter)
            Log.d(TAG, "Screen receiver registered")
        } catch (e: Exception) {
            Log.e(TAG, "Error registering screen receiver: ${e.message}")
        }
    }
    
    private fun unregisterScreenReceiver() {
        try {
            screenOffReceiver?.let {
                unregisterReceiver(it)
                screenOffReceiver = null
                Log.d(TAG, "Screen receiver unregistered")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering screen receiver: ${e.message}")
        }
    }
    
    override fun onTerminate() {
        super.onTerminate()
        Log.d(TAG, "Application terminating - releasing UHF resources")
        unregisterScreenReceiver()
        stopSystemScanningServices(this)
        releaseUHFWrapper()
    }
    
    override fun onLowMemory() {
        super.onLowMemory()
        Log.d(TAG, "Low memory - releasing UHF resources")
        stopSystemScanningServices(this)
        releaseUHFWrapper()
    }
} 