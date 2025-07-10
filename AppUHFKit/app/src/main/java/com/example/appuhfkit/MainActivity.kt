package com.example.appuhfkit

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button
import android.widget.TextView
import android.util.Log
import android.provider.Settings

class MainActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "MainActivity"
    }

    private lateinit var btnScanIn: Button
    private lateinit var btnScanOut: Button
    private lateinit var footerText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnScanIn = findViewById(R.id.btnScanIn)
        btnScanOut = findViewById(R.id.btnScanOut)
        footerText = findViewById(R.id.footerText)
        
        // แสดง Android Device ID ใน footer
        val deviceId = getAndroidDeviceId()
        footerText.text = "Device ID: $deviceId"

        btnScanIn.setOnClickListener {
            Log.d(TAG, "Navigate to Scan In screen")
            val intent = Intent(this, ScanInActivity::class.java)
            startActivity(intent)
        }

        btnScanOut.setOnClickListener {
            Log.d(TAG, "Navigate to Scan Out screen")
            val intent = Intent(this, ScanOutActivity::class.java)
            startActivity(intent)
        }
    }
    
    private fun getAndroidDeviceId(): String {
        return Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown_device"
    }
} 