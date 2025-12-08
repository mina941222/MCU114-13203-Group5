package com.example.healthylife.data

// 用於 Room DAO 查詢結果的資料結構 (輔助類別)

/**
 * 每日巨量營養素進度
 */
data class DailyMacroProgress(
    val total_calories: Int,
    val total_protein: Int
)

/**
 * 每週巨量營養素進度
 */
data class WeeklyMacroProgress(
    val date: String,
    val total_calories: Int,
    val total_protein: Int
)

/**
 * 每週飲水總量進度
 */
data class WeeklyWaterIntake(
    val date: String,
    val total_water: Int
)