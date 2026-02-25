package com.example.appuhfkit

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import android.widget.RadioButton
import android.widget.RadioGroup
import android.util.Log
import android.view.View
import android.widget.LinearLayout

class FabricTypeSelectionActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "FabricTypeSelectionActivity"
        const val EXTRA_TAG_LIST = "tag_list"
        const val EXTRA_SELECTED_FABRIC_TYPE = "selected_fabric_type"
        const val EXTRA_SELECTED_LAUNDRY = "selected_laundry"
        const val EXTRA_SELECTED_LAUNDRY_LABEL = "selected_laundry_label"
        /** ใช้โหมดเลือกแค่โรงซัก (สำหรับ Scan Out) ไม่แสดงส่วนเลือกประเภทผ้า */
        const val EXTRA_LAUNDRY_ONLY = "laundry_only"
    }

    private lateinit var radioGroup: RadioGroup
    private lateinit var spinnerLaundry: Spinner
    private lateinit var btnConfirm: Button
    private lateinit var btnBack: Button
    private lateinit var fabricTypeSection: LinearLayout

    private var selectedLaundry: String = ""
    private var selectedLaundryLabel: String = ""
    private var laundryOnlyMode: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fabric_type_selection)

        laundryOnlyMode = intent.getBooleanExtra(EXTRA_LAUNDRY_ONLY, false)

        // Setup Toolbar
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = "AppUHFKit"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_arrow_back)

        radioGroup = findViewById(R.id.radioGroup)
        spinnerLaundry = findViewById(R.id.spinnerLaundry)
        btnConfirm = findViewById(R.id.btnConfirm)
        btnBack = findViewById(R.id.btnBack)
        fabricTypeSection = findViewById(R.id.fabricTypeSection)

        val headerText = findViewById<TextView>(R.id.headerText)
        if (laundryOnlyMode) {
            headerText.text = getString(R.string.title_select_laundry)
            fabricTypeSection.visibility = View.GONE
        }

        // Setup Laundry dropdown
        val laundryAdapter = ArrayAdapter.createFromResource(
            this,
            R.array.laundry_options,
            android.R.layout.simple_spinner_item
        )
        laundryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerLaundry.adapter = laundryAdapter
        spinnerLaundry.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedLaundry = resources.getStringArray(R.array.laundry_values)[position]
                selectedLaundryLabel = resources.getStringArray(R.array.laundry_options)[position]
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        selectedLaundry = resources.getStringArray(R.array.laundry_values)[0]
        selectedLaundryLabel = resources.getStringArray(R.array.laundry_options)[0]

        btnConfirm.setOnClickListener {
            if (laundryOnlyMode) {
                // โหมดเลือกแค่โรงซัก (Scan Out): ส่งกลับแค่โรงซัก
                val resultIntent = Intent()
                resultIntent.putExtra(EXTRA_SELECTED_LAUNDRY, selectedLaundry)
                resultIntent.putExtra(EXTRA_SELECTED_LAUNDRY_LABEL, selectedLaundryLabel)
                setResult(RESULT_OK, resultIntent)
                finish()
                return@setOnClickListener
            }

            val selectedId = radioGroup.checkedRadioButtonId
            if (selectedId == -1) {
                return@setOnClickListener
            }

            val fabricType = when (selectedId) {
                R.id.radioStainedCloth -> "stainedCloth"
                R.id.radioAttachedCloth -> "attachedCloth"
                R.id.radioReWashCloth -> "reWashCloth"
                else -> "stainedCloth"
            }

            val resultIntent = Intent()
            resultIntent.putExtra(EXTRA_SELECTED_FABRIC_TYPE, fabricType)
            resultIntent.putExtra(EXTRA_SELECTED_LAUNDRY, selectedLaundry)
            resultIntent.putExtra(EXTRA_SELECTED_LAUNDRY_LABEL, selectedLaundryLabel)
            setResult(RESULT_OK, resultIntent)
            finish()
        }

        btnBack.setOnClickListener {
            onBackPressed()
        }

        // ใช้ฟอนต์ SukhumvitSet
        applyFonts()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    private fun applyFonts() {
        try {
            val headerText = findViewById<TextView>(R.id.headerText)
            FontHelper.applySukhumvitBold(headerText)
            
            // ใช้ฟอนต์ SukhumvitSet-Bold สำหรับปุ่ม
            FontHelper.applySukhumvitBold(btnConfirm)
            FontHelper.applySukhumvitBold(btnBack)
        } catch (e: Exception) {
            Log.e(TAG, "Error applying fonts", e)
        }
    }
}
