package com.example.appuhfkit

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button
import android.widget.TextView
import android.widget.ImageView
import android.util.Log
import android.provider.Settings

class MainActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "MainActivity"
    }

    private lateinit var logoImage: ImageView
    private lateinit var btnScanIn: Button
    private lateinit var btnScanOut: Button
    private lateinit var footerText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        logoImage = findViewById(R.id.logoImage)
        btnScanIn = findViewById(R.id.btnScanIn)
        btnScanOut = findViewById(R.id.btnScanOut)
        footerText = findViewById(R.id.footerText)
        
        // แสดง Android Device ID ใน footer
        val deviceId = getAndroidDeviceId()
        footerText.text = "Device ID: $deviceId"
        
        // ใช้ฟอนต์ SukhumvitSet
        applyFonts()

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
    
    private fun applyFonts() {
        try {
            // ใช้ฟอนต์ SukhumvitSet-Bold สำหรับ header
            val headerText = findViewById<TextView>(R.id.headerText)
            FontHelper.applySukhumvitBold(headerText)
            
            // ใช้ฟอนต์ SukhumvitSet-Medium สำหรับ footer
            FontHelper.applySukhumvitMedium(footerText)
            
            // ใช้ฟอนต์ SukhumvitSet-Bold สำหรับปุ่ม
            FontHelper.applySukhumvitBold(btnScanIn)
            FontHelper.applySukhumvitBold(btnScanOut)
        } catch (e: Exception) {
            Log.e(TAG, "Error applying fonts", e)
        }
    }
} 