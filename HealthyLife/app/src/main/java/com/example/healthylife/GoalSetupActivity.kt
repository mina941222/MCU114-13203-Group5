package com.example.healthylife

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.healthylife.data.AppDatabase
import com.example.healthylife.data.UserGoals
import com.example.healthylife.data.UserEntity
import com.example.healthylife.databinding.ActivityGoalSetupBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

class GoalSetupActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGoalSetupBinding
    private lateinit var db: AppDatabase
    private var userId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGoalSetupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = AppDatabase.getDatabase(this)

        // 1. 獲取使用者 ID
        val sharedPrefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        userId = sharedPrefs.getInt("logged_in_user_id", -1)

        if (userId == -1) {
            Toast.makeText(this, "錯誤：使用者 ID 遺失，請重新登入。", Toast.LENGTH_LONG).show()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        setupListener()
    }

    private fun setupListener() {
        binding.btnCalculateAndSave.setOnClickListener {
            handleGoalCalculation()
        }
    }

    // 處理目標計算與儲存 (複製自 FeedbackActivity 的核心邏輯)
    private fun handleGoalCalculation() {
        val heightText = binding.etHeight.text.toString()
        val weightText = binding.etWeight.text.toString()
        val ageText = binding.etAge.text.toString()

        if (heightText.isEmpty() || weightText.isEmpty() || ageText.isEmpty()) {
            Toast.makeText(this, "請完整輸入身高、體重和年齡。", Toast.LENGTH_SHORT).show()
            return
        }

        val heightCm = heightText.toIntOrNull() ?: 0
        val weightKg = weightText.toIntOrNull() ?: 0
        val ageYears = ageText.toIntOrNull() ?: 0

        if (heightCm <= 0 || weightKg <= 0 || ageYears <= 0) {
            Toast.makeText(this, "請輸入有效的數值。", Toast.LENGTH_SHORT).show()
            return
        }

        // 執行目標計算
        val (newCalories, newProtein, newWater) = calculateGoals(heightCm, weightKg, ageYears)
        val newGoals = UserGoals(
            userId = userId,
            targetCalories = newCalories,
            targetProtein = newProtein,
            targetWaterMl = newWater
        )

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // 1. 儲存新的目標到 UserGoals 表
                db.userDao().insertOrUpdateGoals(newGoals)

                // 2. 更新 UserEntity 中的 H/W/A 數據 (標記為已設定，避免下次再跳出此頁)
                val userEntity = db.userDao().getUser(userId).firstOrNull()
                userEntity?.let {
                    val updatedUser = it.copy(
                        heightCm = heightCm,
                        weightKg = weightKg,
                        ageYears = ageYears
                    )
                    db.userDao().updateUser(updatedUser)
                }

                withContext(Dispatchers.Main) {
                    Toast.makeText(this@GoalSetupActivity,
                        "目標設定成功！\n熱量:${newCalories}kcal, 蛋白質:${newProtein}g, 飲水:${newWater}ml",
                        Toast.LENGTH_LONG).show()

                    // 🚨 儲存成功後，強制導航到首頁
                    startActivity(Intent(this@GoalSetupActivity, DashboardActivity::class.java))
                    finish() // 關閉此頁，用戶無法返回
                }
            } catch (e: Exception) {
                Log.e("GoalSetupActivity", "目標儲存失敗", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@GoalSetupActivity, "目標儲存失敗: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // 實作 BMR/TDEE/蛋白質/飲水計算的簡化函數 (與 FeedbackActivity 相同)
    private fun calculateGoals(heightCm: Int, weightKg: Int, ageYears: Int): Triple<Int, Int, Int> {
        // BMR (女性簡化版) = 655 + (9.6 * W) + (1.8 * H) - (4.7 * A)
        val bmr = 655 + (9.6 * weightKg) + (1.8 * heightCm) - (4.7 * ageYears)

        // TDEE (總熱量消耗) - 假設輕度活動量 (Activity Factor = 1.2)
        val tdee = bmr * 1.2
        val targetCalories = tdee.roundToInt()

        // 蛋白質目標 - 建議值為 1.5g/kg
        val targetProtein = (weightKg * 1.5).roundToInt()

        // 飲水目標 - 建議值為 35ml/kg
        val targetWaterMl = (weightKg * 35)

        return Triple(targetCalories, targetProtein, targetWaterMl)
    }
}