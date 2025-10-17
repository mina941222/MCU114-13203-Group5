package com.example.homework3

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Order(
    var mainMeal: String? = null,
    var sideDishes: List<String> = emptyList(),
    var drink: String? = null
) : Parcelable {

    fun isComplete(): Boolean {
        return !mainMeal.isNullOrEmpty() && sideDishes.isNotEmpty() && !drink.isNullOrEmpty()
    }

    fun toSummary(): String {
        return "Main: ${mainMeal ?: "Not selected"}\n" +
                "Sides: ${sideDishes.joinToString(", ").ifEmpty { "Not selected" }}\n" +
                "Drink: ${drink ?: "Not selected"}"
    }
}