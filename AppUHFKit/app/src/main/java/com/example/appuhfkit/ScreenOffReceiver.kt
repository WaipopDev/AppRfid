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
                MyApplication.stopSystemScanningServices(context)
                MyApplication.releaseUHFWrapper()
            }
            Intent.ACTION_SCREEN_ON -> {
                Log.d(TAG, "Screen turned on")
            }
        }
    }
}