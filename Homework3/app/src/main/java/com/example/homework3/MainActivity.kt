package com.example.homework3

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.homework3.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    // 將 lastOrder 改為 currentOrder，因為現在我們在主畫面追蹤並修改它
    private var currentOrder: Order = Order()

    // 使用同一個 Launcher 處理所有子 Activity 的返回結果
    private val orderResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data: Intent? = result.data
            // 獲取從子 Activity 傳回的更新後的 Order 物件
            val updatedOrder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                data?.getParcelableExtra("ORDER_RESULT", Order::class.java)
            } else {
                @Suppress("DEPRECATION")
                data?.getParcelableExtra("ORDER_RESULT")
            }
            updatedOrder?.let {
                currentOrder = it // 更新主畫面的訂單物件
                updateSummary()    // 更新畫面顯示
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 初始化摘要顯示
        updateSummary()

        // 設置三個選項按鈕的點擊事件
        binding.btnMainMeal.setOnClickListener {
            startOrderActivity(MainMealActivity::class.java)
        }

        binding.btnSideDishes.setOnClickListener {
            startOrderActivity(SideDishesActivity::class.java)
        }

        binding.btnDrink.setOnClickListener {
            startOrderActivity(DrinkActivity::class.java)
        }

        // 設置 ORDER 按鈕的點擊事件 (進入 Confirm 畫面)
        binding.btnOrder.setOnClickListener {
            // ORDER 按鈕現在直接導航到 ConfirmActivity
            val intent = Intent(this, ConfirmActivity::class.java).apply {
                putExtra("CURRENT_ORDER", currentOrder)
            }
            orderResultLauncher.launch(intent)
        }
    }

    // 輔助函數：啟動子 Activity
    private fun startOrderActivity(activityClass: Class<*>) {
        val intent = Intent(this, activityClass).apply {
            // 傳遞當前訂單狀態
            putExtra("CURRENT_ORDER", currentOrder)
        }
        orderResultLauncher.launch(intent)
    }

    // 更新訂單摘要顯示
    private fun updateSummary() {
        // 使用 Order.toSummary() 格式化輸出
        // 替換掉原本的 "Submitted Order" 邏輯，改為顯示當前選擇
        val summaryText = currentOrder.toSummary()
        binding.tvCurrentSummary.text = "Current Selection:\n$summaryText"
    }

    // 當 ConfirmActivity 提交成功回來時，我們需要重置 currentOrder
    override fun onResume() {
        super.onResume()
        // 檢查是否有提交成功的標誌或檢查 currentOrder 是否應該被清空
        // 在 ConfirmActivity.kt 中，我們已經通過 Launcher 處理了結果，
        // 但如果想要在提交成功後重置，這裡需要調整邏輯。

        // 由於 ConfirmActivity 提交成功會返回 RESULT_OK 並帶回 Order 物件，
        // 這裡的邏輯需要確保我們顯示的是 'Submitted' 還是 'Current Selection'。
        //
        // 為了符合「提交後跳回主畫面並顯示所選餐點」的原始要求，我們讓 updateSummary 顯示 currentOrder 即可，
        // 只需要確保 ConfirmActivity 能夠**結束**整個訂單流程。

        // 當 OrderActivity 流程完成並返回 RESULT_OK 時，
        // Launcher 會更新 currentOrder，所以這裡不需要額外操作。
    }
}