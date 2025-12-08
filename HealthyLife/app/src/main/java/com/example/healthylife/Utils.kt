package com.example.healthylife

import android.animation.ObjectAnimator
import android.util.Property
import android.view.View

/**
 * 輔助函式：用於動畫化 View 的 layout height。
 * 避免與 ObjectAnimator 的 Kotlin 擴展衝突，改用靜態方法。
 * @param view 要動畫化的 View
 * @param startHeight 初始高度
 * @param endHeight 最終高度
 */
fun createLayoutHeightAnimator(view: View, startHeight: Int, endHeight: Int): ObjectAnimator {

    // 1. 定義一個自定義的屬性 (Property) 來操作 View 的 layoutParams.height
    val heightProperty = object : Property<View, Int>(Int::class.java, "layoutHeight") {
        override fun get(view: View): Int {
            return view.layoutParams.height
        }
        override fun set(view: View, value: Int) {
            view.layoutParams.height = value
            view.requestLayout()
        }
    }

    // 2. 使用 ObjectAnimator.ofInt 搭配這個自定義屬性
    return ObjectAnimator.ofInt(view, heightProperty, startHeight, endHeight)
}