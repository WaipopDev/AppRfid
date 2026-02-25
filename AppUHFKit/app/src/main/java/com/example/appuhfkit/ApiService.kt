package com.example.appuhfkit

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import android.util.Log

class ApiService {
    companion object {
        private const val BASE_URL = "http://35.198.228.196/api/interface/"
        private const val BASE_URL_API = "http://35.198.228.196/api/"
        private const val DELIVERBOUND_URL = "${BASE_URL}deliverbound"
        private const val TIMEOUT_SECONDS = 30L
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json".toMediaType()

    /**
     * ส่งข้อมูล inbound โดยใช้ path ตามโรงซักที่เลือก (เช่น inbound-1, inbound-2)
     */
    fun sendInboundData(jsonArray: JSONArray, inboundPath: String, callback: (Boolean, String) -> Unit) {
        val url = "${BASE_URL}$inboundPath"
        sendData(url, jsonArray, callback)
    }

    /**
     * ส่งข้อมูล outbound โดยใช้ path ตามโรงซักที่เลือก (เช่น outbound-1, outbound-2)
     */
    fun sendOutboundData(jsonArray: JSONArray, outboundPath: String, callback: (Boolean, String) -> Unit) {
        val url = "${BASE_URL}$outboundPath"
        sendData(url, jsonArray, callback)
    }

    fun sendDeliverboundData(jsonArray: JSONArray, callback: (Boolean, String) -> Unit) {
        sendData(DELIVERBOUND_URL, jsonArray, callback)
    }

    fun getFabricWarehouseList(callback: (Boolean, String, ApiResponse?) -> Unit) {
        getData("${BASE_URL_API}select/fabric-warehouse-list", callback)
    }

    fun getDepartmentList(callback: (Boolean, String, ApiResponse?) -> Unit) {
        getData("${BASE_URL_API}select/department-list", callback)
    }

    data class ApiResponse(val data: JSONArray)

    private fun getData(url: String, callback: (Boolean, String, ApiResponse?) -> Unit) {
        Thread {
            try {
                Log.d("ApiService", "Making GET request to: $url")
                val request = Request.Builder()
                    .url(url)
                    .get()
                    .addHeader("Content-Type", "application/json")
                    .build()

                val response: Response = client.newCall(request).execute()
                val responseBody = response.body?.string() ?: ""
                
                Log.d("ApiService", "Response code: ${response.code}")
                Log.d("ApiService", "Response body: $responseBody")

                if (response.isSuccessful) {
                    try {
                        val jsonObject = JSONObject(responseBody)
                        Log.d("ApiService", "Parsed JSON object: ${jsonObject.toString(2)}")
                        
                        val dataArray = jsonObject.getJSONArray("data")
                        Log.d("ApiService", "Data array length: ${dataArray.length()}")
                        
                        val apiResponse = ApiResponse(dataArray)
                        Log.d("ApiService", "Created ApiResponse successfully")
                        callback(true, "ดึงข้อมูลสำเร็จ", apiResponse)
                    } catch (e: Exception) {
                        Log.e("ApiService", "Error parsing response: ${e.message}", e)
                        callback(false, "เกิดข้อผิดพลาดในการแปลงข้อมูล: ${e.message}", null)
                    }
                } else {
                    Log.e("ApiService", "HTTP error: ${response.code} - $responseBody")
                    callback(false, "เกิดข้อผิดพลาด: ${response.code} - $responseBody", null)
                }
            } catch (e: Exception) {
                Log.e("ApiService", "Network error: ${e.message}", e)
                callback(false, "เกิดข้อผิดพลาดในการเชื่อมต่อ: ${e.message}", null)
            }
        }.start()
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