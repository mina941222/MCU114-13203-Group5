package com.example.homework9

import com.google.gson.annotations.SerializedName

// 頂層資料結構 (用來包含所有回傳內容，包括 records 列表)
// 由於這個 API 的根物件包含多個欄位 (fields, records, total 等)，我們只需要取出 records。
data class AirQualityData(
    val records: List<AirStationRecord>? // GSON 會自動找到 "records" 陣列
)

// 個別測站資料結構
data class AirStationRecord(
    @SerializedName("sitename") // 修正為小寫
    val siteName: String,

    @SerializedName("county") // 修正為小寫
    val county: String,

    @SerializedName("aqi") // 修正為小寫
    val aqi: String,

    @SerializedName("status") // 修正為小寫
    val status: String
)

// 由於您的 MainActivity.kt 只需要 sitename, county, aqi, status 這幾個欄位，
// 其他欄位如 pm2.5, pm10 等雖在 JSON 中存在，但在 Kotlin 類別中可以省略不寫。