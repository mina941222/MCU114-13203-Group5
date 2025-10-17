package com.example.homework3

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

// 使用 @Parcelize 讓 Kotlin 資料類別可以在 Intent 中高效傳遞
@Parcelize
data class Order(
    var mainMeal: String? = null,
    var sideDishes: List<String> = emptyList(),
    var drink: String? = null
) : Parcelable {

    // 檢查訂單是否完整 (主餐, 副餐(至少一項), 飲料)
    fun isComplete(): Boolean {
        // 副餐至少選一項，其他兩項必須有值
        return !mainMeal.isNullOrEmpty() && sideDishes.isNotEmpty() && !drink.isNullOrEmpty()
    }

    // 格式化輸出訂單摘要 (用於 AlertDialog 和 MainActivity 顯示)
    fun toSummary(): String {
        return "Main: ${mainMeal ?: "Not selected"}\n" +
                "Sides: ${sideDishes.joinToString(", ").ifEmpty { "Not selected" }}\n" +
                "Drink: ${drink ?: "Not selected"}"
    }
}