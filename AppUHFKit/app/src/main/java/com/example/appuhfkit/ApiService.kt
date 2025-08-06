package com.example.appuhfkit

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Response
import org.json.JSONArray
import java.util.concurrent.TimeUnit

class ApiService {
    companion object {
        private const val BASE_URL = "http://35.198.228.196/api/interface/"
        private const val INBOUND_URL = "${BASE_URL}inbound"
        private const val OUTBOUND_URL = "${BASE_URL}outbound"
        private const val DELIVERBOUND_URL = "${BASE_URL}deliverbound"
        private const val TIMEOUT_SECONDS = 30L
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json".toMediaType()

    fun sendInboundData(jsonArray: JSONArray, callback: (Boolean, String) -> Unit) {
        sendData(INBOUND_URL, jsonArray, callback)
    }

    fun sendOutboundData(jsonArray: JSONArray, callback: (Boolean, String) -> Unit) {
        sendData(OUTBOUND_URL, jsonArray, callback)
    }

    fun sendDeliverboundData(jsonArray: JSONArray, callback: (Boolean, String) -> Unit) {
        sendData(DELIVERBOUND_URL, jsonArray, callback)
    }

    private fun sendData(url: String, jsonArray: JSONArray, callback: (Boolean, String) -> Unit) {
        Thread {
            try {
                val jsonString = jsonArray.toString()
                val mediaType = "application/json".toMediaType()
                val requestBody = RequestBody.create(mediaType, jsonString)
                
                val request = Request.Builder()
                    .url(url)
                    .post(requestBody)
                    .addHeader("Content-Type", "application/json")
                    .build()

                val response: Response = client.newCall(request).execute()
                val responseBody = response.body?.string() ?: ""

                if (response.isSuccessful) {
                    callback(true, "ส่งข้อมูลสำเร็จ")
                } else {
                    callback(false, "เกิดข้อผิดพลาด: ${response.code} - $responseBody")
                }
            } catch (e: Exception) {
                callback(false, "เกิดข้อผิดพลาดในการเชื่อมต่อ: ${e.message}")
            }
        }.start()
    }
} 