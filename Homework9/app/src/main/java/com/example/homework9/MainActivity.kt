package com.example.homework9

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import okhttp3.*
import java.io.IOException

class MainActivity : AppCompatActivity() {

    private val API_KEY = "08b84f2d-4d9b-4695-877b-7b6c6818f2e3"
    private val API_URL = "https://data.moenv.gov.tw/api/v2/aqx_p_432?api_key=$API_KEY&limit=1000&format=json"

    private lateinit var btnQuery: AppCompatButton
    // 用於儲存從 API 獲取的所有空氣品質數據
    private var allAirRecords: List<AirStationRecord> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnQuery = findViewById(R.id.btnQuery)

        btnQuery.setOnClickListener {
            btnQuery.isEnabled = false
            btnQuery.text = "查詢中..."
            sendRequest()
        }
    }

    private fun sendRequest() {

        val request = Request.Builder()
            .url(API_URL)
            .get()
            .build()

        OkHttpClient().newCall(request).enqueue(object : Callback {

            override fun onFailure(call: Call, e: IOException) {
                Log.e("OkHttp", "網路連線失敗: ${e.message}")
                runOnUiThread {
                    btnQuery.isEnabled = true
                    btnQuery.text = "查詢全台空氣品質"
                    showErrorDialog("連線錯誤", "無法連線至 API 伺服器，請檢查網路。")
                }
            }

            override fun onResponse(call: Call, response: Response) {

                if (!response.isSuccessful) {
                    runOnUiThread {
                        btnQuery.isEnabled = true
                        btnQuery.text = "查詢全台空氣品質"
                        showErrorDialog("HTTP 請求失敗", "狀態碼: ${response.code}")
                    }
                    return
                }

                val jsonString = response.body?.string()

                if (jsonString != null) {
                    // 臨時打印原始回傳，幫助偵錯
                    Log.d("API_Response", "Raw JSON: $jsonString")

                    if (jsonString.contains("查無資料") || jsonString.contains("API")) {
                        runOnUiThread {
                            btnQuery.isEnabled = true
                            btnQuery.text = "查詢全台空氣品質"
                            showErrorDialog("API 錯誤", "伺服器回傳訊息: $jsonString \n\n請檢查 API Key 是否有效。")
                        }
                        return
                    }

                    try {
                        val gson = Gson()
                        val data: AirQualityData = gson.fromJson(jsonString, AirQualityData::class.java)

                        // 儲存所有數據到全域變數
                        data.records?.let { records ->
                            allAirRecords = records
                            runOnUiThread {
                                btnQuery.isEnabled = true
                                btnQuery.text = "查詢全台空氣品質"
                                showCountySelectionDialog(records) // 顯示縣市選擇對話框
                            }
                        } ?: run {
                            runOnUiThread {
                                btnQuery.isEnabled = true
                                btnQuery.text = "查詢全台空氣品質"
                                showErrorDialog("解析錯誤", "API 回傳資料格式不符。")
                            }
                        }

                    } catch (e: JsonSyntaxException) {
                        Log.e("GSON", "JSON 解析失敗: ${e.message}")
                        runOnUiThread {
                            btnQuery.isEnabled = true
                            btnQuery.text = "查詢全台空氣品質"
                            showErrorDialog("解析錯誤", "無法將伺服器回傳的資料轉換成物件。")
                        }
                    }
                }
            }
        })
    }

    // 新增：顯示縣市選擇對話框
    private fun showCountySelectionDialog(records: List<AirStationRecord>) {

        val targetCounties = listOf(
            "基隆市", "臺北市", "新北市", "桃園市", "新竹縣", "新竹市",
            "苗栗縣", "臺中市", "彰化縣", "南投縣", "雲林縣", "嘉義市",
            "嘉義縣", "臺南市", "高雄市", "屏東縣", "宜蘭縣", "花蓮縣",
            "臺東縣", "澎湖縣", "金門縣", "連江縣"
        )

        // 從所有記錄中提取不重複且在目標列表中的縣市名稱
        val availableCounties = records
            .map { it.county }
            .distinct()
            .filter { it in targetCounties }
            .toTypedArray()

        if (availableCounties.isEmpty()) {
            showErrorDialog("無可用數據", "目前無您指定縣市的空氣品質數據。")
            return
        }

        AlertDialog.Builder(this)
            .setTitle("請選擇欲查詢的縣市")
            .setItems(availableCounties) { dialog, which ->
                val selectedCounty = availableCounties[which]
                // 根據選擇的縣市過濾數據並顯示結果
                showAirQualityDialog(selectedCounty)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    // 顯示單一縣市結果的列表式 AlertDialog
    private fun showAirQualityDialog(county: String) {

        // 從所有數據中過濾出選定的縣市
        val filteredRecords = allAirRecords.filter { it.county == county }

        // 將 Record 轉換成要顯示的字串列表（格式與範例圖相似）
        val items = filteredRecords.map {
            "地區: ${it.siteName}, 狀態: ${it.status} (AQI: ${it.aqi})"
        }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("${county}空氣品質") // 縣市名稱作為標題
            .setItems(items) { dialog, which ->
                // 點擊列表項目後不做任何事
            }
            .setPositiveButton("關閉", null)
            .show()
    }

    // 輔助方法：顯示簡單錯誤對話框
    private fun showErrorDialog(title: String, message: String) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("關閉", null)
            .show()
    }
}