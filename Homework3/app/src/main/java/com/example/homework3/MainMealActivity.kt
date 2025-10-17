package com.example.homework3

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.homework3.databinding.ActivityMainMealBinding

class MainMealActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainMealBinding
    private lateinit var currentOrder: Order

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainMealBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 獲取從上一個 Activity 傳來的訂單物件
        currentOrder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("CURRENT_ORDER", Order::class.java)!!
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra("CURRENT_ORDER")!!
        }

        // 處理 "Next" 按鈕點擊
        binding.btnNext.setOnClickListener {
            // 根據選中的 RadioButton 更新主餐
            val selectedMainMeal = when (binding.radioGroupMainMeal.checkedRadioButtonId) {
                binding.rbBurger.id -> "Burger"
                binding.rbFriedChicken.id -> "Fried Chicken"
                binding.rbChickenNuggets.id -> "Chicken Nuggets"
                else -> null
            }
            currentOrder.mainMeal = selectedMainMeal

            // 導航到下一個畫面 (Side Dishes)
            val intent = Intent(this, SideDishesActivity::class.java).apply {
                putExtra("CURRENT_ORDER", currentOrder)
            }
            // 使用 startActivityForResult 啟動 Activity，以便接收回傳的最終結果
            startActivityForResult(intent, REQUEST_CODE_ORDER_WIZARD)
        }
    }

    // 處理後續 Activity 返回的結果，並將結果向 MainActivity 傳遞
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_ORDER_WIZARD && resultCode == RESULT_OK) {
            setResult(RESULT_OK, data) // 將結果設定為 RESULT_OK 並傳遞 Intent
            finish() // 結束自己，回到上一個 Activity (MainActivity)
        }
    }

    companion object {
        private const val REQUEST_CODE_ORDER_WIZARD = 1
    }
}