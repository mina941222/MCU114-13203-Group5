package com.example.homework3

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.homework3.databinding.ActivityConfirmBinding

class ConfirmActivity : AppCompatActivity() {
    private lateinit var binding: ActivityConfirmBinding
    private lateinit var currentOrder: Order

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityConfirmBinding.inflate(layoutInflater)
        setContentView(binding.root)

        currentOrder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("CURRENT_ORDER", Order::class.java)!!
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra("CURRENT_ORDER")!!
        }

        // **********************************************
        // 關鍵修改：確保畫面上永遠顯示 "Your order summary will appear here."
        // 且 CONFIRM 按鈕永遠是可點擊的。
        // **********************************************

        // 1. 移除所有設定 binding.tvOrderSummary.text 的程式碼
        //    讓它保持 XML 中設定的 "Your order summary will appear here."

        // 2. 移除所有 binding.btnConfirm.isEnabled = false 的程式碼，讓按鈕始終啟用。


        // 處理 "Confirm" 按鈕點擊
        binding.btnConfirm.setOnClickListener {

            // 1. 驗證 (Validation) - 訂單不完整
            if (currentOrder.isComplete() != true) {
                // 訂單不完整，跳出您要求的 Toast 提示
                Toast.makeText(
                    this,
                    "Please select a main meal, at least one side dish, and a drink.",
                    Toast.LENGTH_LONG
                ).show()
                return@setOnClickListener // 終止點擊事件，不繼續執行後面的 AlertDialog
            }

            // 2. 訂單完整 (isComplete() == true) 時，顯示 AlertDialog with Submit
            AlertDialog.Builder(this)
                .setTitle("Submit Order")
                // 在彈出框中顯示實際的訂單摘要
                .setMessage(currentOrder.toSummary() + "\n\nSubmit this order?")
                .setPositiveButton("Submit") { d, _ ->
                    // 提交後邏輯
                    val resultIntent = Intent().apply {
                        putExtra("ORDER_RESULT", currentOrder)
                    }
                    setResult(Activity.RESULT_OK, resultIntent)
                    d.dismiss()
                    Toast.makeText(this, "Order submitted!", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }
}