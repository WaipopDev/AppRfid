package com.example.appuhfkit

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button
import android.widget.TextView
import android.widget.ImageView
import android.util.Log
import android.provider.Settings
import android.os.Process
import kotlin.system.exitProcess

class MainActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "MainActivity"
    }

    private lateinit var logoImage: ImageView
    private lateinit var btnScanIn: Button
    private lateinit var btnScanOut: Button
    private lateinit var btnScanDeliver: Button
    private lateinit var footerText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        logoImage = findViewById(R.id.logoImage)
        btnScanIn = findViewById(R.id.btnScanIn)
        btnScanOut = findViewById(R.id.btnScanOut)
        btnScanDeliver = findViewById(R.id.btnScanDeliver)
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

        btnScanDeliver.setOnClickListener {
            Log.d(TAG, "Navigate to Scan Deliver screen")
            val intent = Intent(this, ScanDeliverActivity::class.java)
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
            FontHelper.applySukhumvitBold(btnScanDeliver)
        } catch (e: Exception) {
            Log.e(TAG, "Error applying fonts", e)
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "MainActivity destroyed")
        // เมื่อ MainActivity ถูก destroy ให้ปล่อย UHF resources
        if (isFinishing) {
            Log.d(TAG, "App is finishing - releasing UHF resources")
            // หยุด system scanning services ก่อน
            MyApplication.stopSystemScanningServices(this)
            MyApplication.releaseUHFWrapper()
            
            // รอให้ UHF ปิดเสร็จสิ้น
            Thread {
                try {
                    Thread.sleep(1500) // รอ 1.5 วินาที ให้ system services ปิด
                    Log.d(TAG, "Force terminating app process")
                    Process.killProcess(Process.myPid())
                    exitProcess(0)
                } catch (e: Exception) {
                    Log.e(TAG, "Error force terminating: ${e.message}")
                }
            }.start()
        }
    }
    
    override fun onPause() {
        super.onPause()
        Log.d(TAG, "MainActivity paused")
    }
    
    override fun onStop() {
        super.onStop()
        Log.d(TAG, "MainActivity stopped")
        // ไม่ปิด UHF ทันทีใน onStop เพราะอาจจะกลับมาใช้งานเร็ว
        // จะปิดเฉพาะใน onDestroy เมื่อแอพจบการทำงานจริงๆ
    }
    
    override fun onBackPressed() {
        Log.d(TAG, "Back button pressed - exiting app")
        // หยุด system scanning services เมื่อออกจากแอพ
        MyApplication.stopSystemScanningServices(this)
        // เมื่อกดปุ่ม back ให้ปิดแอพทันที
        super.onBackPressed()
        finishAffinity() // ปิดทุก Activity ใน task
    }
} 