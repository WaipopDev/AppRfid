package com.example.appuhfkit

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import android.widget.Button
import android.widget.TextView
import android.widget.Spinner
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.util.Log
import android.view.View
import android.widget.AdapterView
import org.json.JSONArray
import org.json.JSONObject

class DeliverSelectionActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "DeliverSelectionActivity"
        const val EXTRA_FABRIC_WAREHOUSE_CODE = "fabric_warehouse_code"
        const val EXTRA_DROP_OFF_POINT_CODE = "drop_off_point_code"
        const val EXTRA_STATUS_SEND_TYPE = "status_send_type"
        const val EXTRA_REMARK_DELIVER = "remark_deliver"
    }

    private lateinit var spinnerWarehouse: Spinner
    private lateinit var spinnerDepartment: Spinner
    private lateinit var radioGroupSendType: RadioGroup
    private lateinit var editTextRemark: EditText
    private lateinit var btnConfirm: Button
    private lateinit var btnBack: Button
    private lateinit var btnLoadData: Button

    private val apiService = ApiService()
    private var warehouseList = mutableListOf<WarehouseItem>()
    private var departmentList = mutableListOf<DepartmentItem>()
    private var selectedWarehouseCode: String? = null
    private var selectedDepartmentCode: String? = null
    private var selectedSendType: String? = null

    data class WarehouseItem(val code: String, val name: String)
    data class DepartmentItem(val code: String, val name: String)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_deliver_selection)

        // Setup Toolbar
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = "AppUHFKit"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_arrow_back)

        initializeViews()
        setupListeners()
        loadData()
        
        // ใช้ฟอนต์ SukhumvitSet
        applyFonts()
    }

    private fun initializeViews() {
        spinnerWarehouse = findViewById(R.id.spinnerWarehouse)
        spinnerDepartment = findViewById(R.id.spinnerDepartment)
        radioGroupSendType = findViewById(R.id.radioGroupSendType)
        editTextRemark = findViewById(R.id.editTextRemark)
        btnConfirm = findViewById(R.id.btnConfirm)
        btnBack = findViewById(R.id.btnBack)
        btnLoadData = findViewById(R.id.btnLoadData)
    }

    private fun setupListeners() {
        btnConfirm.setOnClickListener {
            if (validateInputs()) {
                val remark = editTextRemark.text.toString()
                
                // Log selected values before returning
                Log.d(TAG, "Selected values before returning:")
                Log.d(TAG, "  - fabricWarehouseCode: $selectedWarehouseCode")
                Log.d(TAG, "  - dropOffPointCode: $selectedDepartmentCode")
                Log.d(TAG, "  - statusSendType: $selectedSendType")
                Log.d(TAG, "  - remarkDeliver: $remark")
                
                val resultIntent = Intent()
                resultIntent.putExtra(EXTRA_FABRIC_WAREHOUSE_CODE, selectedWarehouseCode)
                resultIntent.putExtra(EXTRA_DROP_OFF_POINT_CODE, selectedDepartmentCode)
                resultIntent.putExtra(EXTRA_STATUS_SEND_TYPE, selectedSendType)
                resultIntent.putExtra(EXTRA_REMARK_DELIVER, remark)
                setResult(RESULT_OK, resultIntent)
                finish()
            }
        }

        btnBack.setOnClickListener {
            onBackPressed()
        }

        btnLoadData.setOnClickListener {
            loadData()
        }

        spinnerWarehouse.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position > 0) {
                    selectedWarehouseCode = warehouseList[position - 1].code
                } else {
                    selectedWarehouseCode = null
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                selectedWarehouseCode = null
            }
        }

        spinnerDepartment.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position > 0) {
                    selectedDepartmentCode = departmentList[position - 1].code
                } else {
                    selectedDepartmentCode = null
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                selectedDepartmentCode = null
            }
        }

        radioGroupSendType.setOnCheckedChangeListener { _, checkedId ->
            selectedSendType = when (checkedId) {
                R.id.radioSendStandard -> "sendStandard"
                R.id.radioSendSupplement -> "sendSupplement"
                R.id.radioSendUrgent -> "sendUrgent"
                R.id.radioSendBeforeWashing -> "sendBeforeWashing"
                else -> null
            }
        }
    }

    private fun loadData() {
        btnLoadData.isEnabled = false
        btnLoadData.text = "กำลังโหลด..."

        // Load warehouse list
        Log.d(TAG, "Loading fabric warehouse list...")
        apiService.getFabricWarehouseList { success, message, data ->
            Log.d(TAG, "Fabric warehouse list response - Success: $success, Message: $message")
            if (data != null) {
                Log.d(TAG, "Fabric warehouse data received: ${data.data.toString(2)}")
            }
            
            runOnUiThread {
                if (success && data != null) {
                    warehouseList.clear()
                    Log.d(TAG, "Processing ${data.data.length()} warehouse items")
                    for (i in 0 until data.data.length()) {
                        val item = data.data.getJSONObject(i)
                        val warehouseCode = item.getString("fabricWarehouseCode")
                        val warehouseName = item.getString("fabricWarehouseName")
                        Log.d(TAG, "Warehouse item $i: Code=$warehouseCode, Name=$warehouseName")
                        warehouseList.add(
                            WarehouseItem(warehouseCode, warehouseName)
                        )
                    }
                    Log.d(TAG, "Warehouse list populated with ${warehouseList.size} items")
                    setupWarehouseSpinner()
                } else {
                    Log.e(TAG, "Failed to load warehouse list: $message")
                }
            }
        }

        // Load department list
        Log.d(TAG, "Loading department list...")
        apiService.getDepartmentList { success, message, data ->
            Log.d(TAG, "Department list response - Success: $success, Message: $message")
            if (data != null) {
                Log.d(TAG, "Department data received: ${data.data.toString(2)}")
            }
            
            runOnUiThread {
                if (success && data != null) {
                    departmentList.clear()
                    Log.d(TAG, "Processing ${data.data.length()} department items")
                    for (i in 0 until data.data.length()) {
                        val item = data.data.getJSONObject(i)
                        val deptCode = item.getString("departmentCode")
                        val deptName = item.getString("departmentName")
                        Log.d(TAG, "Department item $i: Code=$deptCode, Name=$deptName")
                        departmentList.add(
                            DepartmentItem(deptCode, deptName)
                        )
                    }
                    Log.d(TAG, "Department list populated with ${departmentList.size} items")
                    setupDepartmentSpinner()
                } else {
                    Log.e(TAG, "Failed to load department list: $message")
                }
                
                btnLoadData.isEnabled = true
                btnLoadData.text = "โหลดข้อมูลใหม่"
            }
        }
    }

    private fun setupWarehouseSpinner() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, 
            listOf("เลือกจุดส่งผ้า") + warehouseList.map { it.name })
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerWarehouse.adapter = adapter
    }

    private fun setupDepartmentSpinner() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, 
            listOf("เลือกจุดรับผ้า") + departmentList.map { it.name })
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerDepartment.adapter = adapter
    }

    private fun validateInputs(): Boolean {
        if (selectedWarehouseCode == null) {
            showAlert("กรุณาเลือกจุดส่งผ้า")
            return false
        }
        if (selectedDepartmentCode == null) {
            showAlert("กรุณาเลือกจุดรับผ้า")
            return false
        }
        if (selectedSendType == null) {
            showAlert("กรุณาเลือกรอบการส่งผ้า")
            return false
        }
        return true
    }

    private fun showAlert(message: String) {
        android.app.AlertDialog.Builder(this)
            .setTitle("แจ้งเตือน")
            .setMessage(message)
            .setPositiveButton("ตกลง", null)
            .show()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    private fun applyFonts() {
        try {
            // ใช้ฟอนต์ SukhumvitSet-Bold สำหรับ header
            val headerText = findViewById<TextView>(R.id.headerText)
            FontHelper.applySukhumvitBold(headerText)
            
            // ใช้ฟอนต์ SukhumvitSet-Bold สำหรับปุ่ม
            FontHelper.applySukhumvitBold(btnConfirm)
            FontHelper.applySukhumvitBold(btnBack)
            FontHelper.applySukhumvitBold(btnLoadData)
        } catch (e: Exception) {
            Log.e(TAG, "Error applying fonts", e)
        }
    }
}
