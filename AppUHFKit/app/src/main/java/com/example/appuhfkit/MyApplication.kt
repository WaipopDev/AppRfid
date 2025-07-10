package com.example.appuhfkit

import android.app.Application
import com.tencent.mmkv.MMKV

class MyApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        // เริ่มต้น MMKV
        MMKV.initialize(this)
    }
} 