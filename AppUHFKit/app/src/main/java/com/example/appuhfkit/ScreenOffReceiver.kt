package com.example.appuhfkit

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class ScreenOffReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "ScreenOffReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_SCREEN_OFF -> {
                Log.d(TAG, "Screen turned off - stopping system scanning services")
                // ใช้ Thread แยกเพื่อไม่ให้ block BroadcastReceiver
                Thread {
                    try {
                        // หยุด system scanning services ก่อน
                        MyApplication.stopSystemScanningServices(context)
                        Thread.sleep(200) // รอให้ system services หยุดก่อน
                        
                        // จากนั้นปิด UHF wrapper
                        MyApplication.releaseUHFWrapper()
                        Log.d(TAG, "Screen off - all services stopped")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error during screen off handling: ${e.message}")
                    }
                }.start()
            }
            Intent.ACTION_SCREEN_ON -> {
                Log.d(TAG, "Screen turned on")
                // เมื่อเปิดหน้าจอ อาจต้องเตรียม UHF wrapper ใหม่
                // แต่จะให้ Activity ที่ต้องการใช้เป็นคนสร้างเอง
            }
        }
    }
}