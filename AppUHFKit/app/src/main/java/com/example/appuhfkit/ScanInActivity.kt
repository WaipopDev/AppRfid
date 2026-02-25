package com.example.appuhfkit

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import android.widget.Button
import android.widget.TextView
import android.util.Log
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.app.AlertDialog
import android.provider.Settings
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import android.os.Handler
import android.os.Looper
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContracts

class ScanInActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "ScanInActivity"
    }

    private lateinit var uhfWrapper: UHFWrapper
    private lateinit var btnScan: Button
    private lateinit var btnSave: Button
    private lateinit var footerText: TextView
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: TagAdapter
    private var isScanning = false
    private val tagList = mutableListOf<TagItem>()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    private val apiService = ApiService()
    private val mainHandler = Handler(Looper.getMainLooper())
    private var selectedFabricType: String? = null
    private var selectedLaundry: String? = null
    private var selectedLaundryLabel: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scan_in)

        // Setup Toolbar
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = "AppUHFKit"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_arrow_back)

        btnScan = findViewById(R.id.btnScan)
        btnSave = findViewById(R.id.btnSave)
        footerText = findViewById(R.id.footerText)
        recyclerView = findViewById(R.id.recyclerView)

        adapter = TagAdapter(tagList)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        uhfWrapper = MyApplication.getUHFWrapper(this)

        btnScan.setOnClickListener {
            if (isScanning) {
                stopScan()
            } else {
                startScan()
            }
        }

        btnSave.setOnClickListener {
            if (tagList.isEmpty()) {
                showAlert("ไม่มีข้อมูล", "กรุณาสแกนข้อมูลก่อนบันทึก")
                return@setOnClickListener
            }
            // Launch fabric type selection screen
            launchFabricTypeSelection()
        }

        updateFooter()
        
        // ใช้ฟอนต์ SukhumvitSet
        applyFonts()
        
        // Setup activity result launcher
        setupActivityResultLauncher()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
    
    override fun onPause() {
        super.onPause()
        Log.d(TAG, "ScanInActivity paused - stopping scan")
        // หยุดการสแกนเมื่อ Activity ถูก pause
        if (isScanning) {
            stopScan()
        }
    }
    
    override fun onStop() {
        super.onStop()
        Log.d(TAG, "ScanInActivity stopped")
        // ให้แน่ใจว่าหยุดการสแกนเมื่อ Activity หยุดทำงาน
        if (isScanning) {
            isScanning = false
            uhfWrapper.stopScan()
        }
    }

    private fun startScan() {
        isScanning = true
        btnScan.text = "Stop Scan"
        btnScan.setBackgroundColor(resources.getColor(android.R.color.holo_red_dark))
        tagList.clear()
        adapter.notifyDataSetChanged()
        updateFooter()
        Log.d(TAG, "Started scanning for Scan In, cleared tag list")

        uhfWrapper.startScan { tagData ->
            Log.d(TAG, "Tag scanned in Scan In: $tagData")
            // แปลงข้อมูล EPC, RSSI จาก string
            val epc = tagData.substringAfter("EPC: ").substringBefore(", RSSI:").trim()
            val rssi = tagData.substringAfter("RSSI: ", "").trim()
            val time = dateFormat.format(Date())
            
            Log.d(TAG, "Parsed EPC: $epc, RSSI: $rssi, Time: $time")
            Log.d(TAG, "Current tag list size: ${tagList.size}")
            
            // ตรวจสอบซ้ำ
            if (tagList.none { it.epc == epc }) {
                val newTag = TagItem(epc, time, rssi)
                tagList.add(0, newTag)
                Log.d(TAG, "Added new tag: $epc, Total tags now: ${tagList.size}")
                
                // Update adapter on main thread
                runOnUiThread {
                    adapter.notifyDataSetChanged()
                    recyclerView.scrollToPosition(0)
                    updateFooter()
                    Log.d(TAG, "Updated UI - Tag list size: ${tagList.size}, Adapter item count: ${adapter.itemCount}")
                }
            } else {
                Log.d(TAG, "Tag already exists: $epc")
            }
        }
    }

    private fun stopScan() {
        isScanning = false
        btnScan.text = "Start Scan"
        btnScan.setBackgroundColor(resources.getColor(android.R.color.holo_green_dark))
        uhfWrapper.stopScan()
    }

    private fun updateFooter() {
        footerText.text = "Total Items: ${tagList.size}"
    }

    private val fabricTypeSelectionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            selectedFabricType = result.data?.getStringExtra(FabricTypeSelectionActivity.EXTRA_SELECTED_FABRIC_TYPE)
            selectedLaundry = result.data?.getStringExtra(FabricTypeSelectionActivity.EXTRA_SELECTED_LAUNDRY)
            selectedLaundryLabel = result.data?.getStringExtra(FabricTypeSelectionActivity.EXTRA_SELECTED_LAUNDRY_LABEL)
            if (selectedFabricType != null) {
                // Proceed with saving data
                saveScanData()
            }
        }
    }

    private fun setupActivityResultLauncher() {
        // Launcher is already initialized above
    }

    private fun launchFabricTypeSelection() {
        val intent = Intent(this, FabricTypeSelectionActivity::class.java)
        fabricTypeSelectionLauncher.launch(intent)
    }

    private fun getAndroidDeviceId(): String {
        return Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown_device"
    }

    private fun saveScanData() {
        if (tagList.isEmpty()) {
            showAlert("ไม่มีข้อมูล", "กรุณาสแกนข้อมูลก่อนบันทึก")
            return
        }

        if (selectedFabricType == null) {
            showAlert("ไม่พบประเภทผ้า", "กรุณาเลือกประเภทผ้า")
            return
        }

        // 1. หยุด scan ถ้ากำลังสแกน
        if (isScanning) {
            stopScan()
        }

        // 2. แสดง Alert ยืนยันการบันทึก
        val laundryText = selectedLaundryLabel?.let { "โรงซัก: $it\n" } ?: ""
        AlertDialog.Builder(this)
            .setTitle("ยืนยันการบันทึก")
            .setMessage("คุณต้องการบันทึกข้อมูล ${tagList.size} รายการหรือไม่?\n${laundryText}ประเภทผ้า: ${getFabricTypeDisplayName(selectedFabricType!!)}")
            .setPositiveButton("บันทึก") { dialog, _ ->
                dialog.dismiss()
                performSave()
            }
            .setNegativeButton("ยกเลิก") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun getFabricTypeDisplayName(fabricType: String): String {
        return when (fabricType) {
            "stainedCloth" -> "ผ้าเปื้อน"
            "attachedCloth" -> "ผ้าติดเชื้อ"
            "reWashCloth" -> "ผ้าซักใหม่ (Re-wash)"
            else -> "ไม่ทราบ"
        }
    }

    private fun performSave() {
        try {
            val jsonArray = JSONArray()
            for (tag in tagList) {
                val jsonObject = JSONObject().apply {
                    put("tagId", tag.epc)
                    put("readDateTime", tag.time)
                    put("readerId", getAndroidDeviceId())
                    put("typeSend", "appMobile")
                    put("statusGroupType", selectedFabricType)
                }
                jsonArray.put(jsonObject)
            }
            
            val inboundPath = selectedLaundry ?: "inbound-1"
            Log.d(TAG, "Sending data to inbound API (path: $inboundPath) with statusGroupType: $selectedFabricType")
            Log.d(TAG, "Sending data to inbound API: ${jsonArray.toString(2)}")
            
            // แสดง loading dialog
            val loadingDialog = showLoadingDialog()
            
            // ส่งข้อมูลไปยัง inbound API ตามโรงซักที่เลือก (inbound-1, inbound-2)
            apiService.sendInboundData(jsonArray, inboundPath) { success, message ->
                mainHandler.post {
                    // ปิด loading dialog
                    loadingDialog.dismiss()
                    
                    if (success) {
                        showAlert("ส่งข้อมูลสำเร็จ", message) {
                            // ล้าง tagList และอัปเดต UI
                            tagList.clear()
                            adapter.notifyDataSetChanged()
                            updateFooter()
                            selectedFabricType = null
                            selectedLaundry = null
                            selectedLaundryLabel = null
                        }
                    } else {
                        showAlert("เกิดข้อผิดพลาด", message)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error saving scan data", e)
            showAlert("เกิดข้อผิดพลาด", "ไม่สามารถบันทึกข้อมูลได้: ${e.message}")
        }
    }

    private fun showLoadingDialog(): AlertDialog {
        val dialog = AlertDialog.Builder(this)
            .setView(R.layout.loading_dialog)
            .setCancelable(false)
            .create()
        dialog.show()
        return dialog
    }

    // ปรับ showAlert ให้รองรับ callback หลังปิด dialog
    private fun showAlert(title: String, message: String, onDismiss: (() -> Unit)? = null) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("ตกลง") { dialog, _ ->
                dialog.dismiss()
                onDismiss?.invoke()
            }
            .show()
    }

    private fun applyFonts() {
        try {
            // ใช้ฟอนต์ SukhumvitSet-Bold สำหรับ header
            val headerText = findViewById<TextView>(R.id.headerText)
            FontHelper.applySukhumvitBold(headerText)
            
            // ใช้ฟอนต์ SukhumvitSet-Medium สำหรับ footer
            FontHelper.applySukhumvitMedium(footerText)
            
            // ใช้ฟอนต์ SukhumvitSet-Bold สำหรับปุ่ม
            FontHelper.applySukhumvitBold(btnScan)
            FontHelper.applySukhumvitBold(btnSave)
        } catch (e: Exception) {
            Log.e(TAG, "Error applying fonts", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // หยุดการสแกนเมื่อออกจากหน้า
        if (isScanning) {
            stopScan()
        }
        Log.d(TAG, "ScanInActivity destroyed")
    }
} 