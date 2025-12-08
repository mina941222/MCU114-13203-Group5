package com.example.healthylife

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.healthylife.data.AppDatabase
import com.example.healthylife.databinding.ActivityFeedbackBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.os.Build
import com.example.healthylife.data.UserEntity
import com.example.healthylife.data.MealEntity
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date
import java.util.Calendar
import kotlin.math.roundToInt
import com.example.healthylife.data.UserGoals // 🚨 修正導入：新增 UserGoals

class FeedbackActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFeedbackBinding
    private lateinit var db: AppDatabase
    private var userId: Int = 0
    private val PREF_KEY_REMINDER_ENABLED = "daily_reminder_enabled"

    // 儲存當前使用者 Entity (包含 H/W/A 數據)
    private var currentUser: UserEntity? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFeedbackBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 1. 初始化資料庫
        db = AppDatabase.getDatabase(this)

        // 2. 獲取當前登入用戶 ID
        val sharedPrefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        userId = sharedPrefs.getInt("logged_in_user_id", -1)

        if (userId == -1) {
            Toast.makeText(this, "錯誤：找不到使用者 ID，請重新登入。", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        // 3. 設置通知狀態
        val isEnabled = sharedPrefs.getBoolean(PREF_KEY_REMINDER_ENABLED, true)
        binding.switchDailyReminder.isChecked = isEnabled

        loadAndObserveUser() // 載入使用者數據和現有目標
        setupListeners()
    }

    // 載入使用者數據並觀察目標
    private fun loadAndObserveUser() {
        // 觀察 UserEntity (用於讀取 H/W/A)
        lifecycleScope.launch {
            db.userDao().getUser(userId).collect { user ->
                currentUser = user
                user?.let {
                    // 載入 H/W/A 數據到輸入框 (如果存在)
                    if (it.heightCm > 0) binding.etHeight.setText(it.heightCm.toString())
                    if (it.weightKg > 0) binding.etWeight.setText(it.weightKg.toString())
                    if (it.ageYears > 0) binding.etAge.setText(it.ageYears.toString())
                }
            }
        }
    }

    private fun setupListeners() {
        // Button: 返回 (左上角的 ImageButton)
        binding.btnBackToDashboard.setOnClickListener { finish() }

        // Button: 返回 (底部的按鈕)
        binding.btnBackToDashboardBottom.setOnClickListener { finish() }

        // Button: 提交回饋 (原功能)
        binding.btnSubmitFeedback.setOnClickListener {
            handleSubmitFeedback()
        }

        // 🚨 新功能：計算並儲存目標
        binding.btnCalculateGoals.setOnClickListener {
            handleGoalCalculation()
        }

        // Switch: 每日通知提醒開關 (原功能)
        binding.switchDailyReminder.setOnCheckedChangeListener { _, isChecked ->
            // 儲存狀態 (SharedPreferences)
            getSharedPreferences("app_prefs", MODE_PRIVATE)
                .edit()
                .putBoolean(PREF_KEY_REMINDER_ENABLED, isChecked)
                .apply()

            // 啟動或停止服務 (Broadcast Receiver 邏輯)
            toggleReminderService(isChecked)

            if (isChecked) {
                Toast.makeText(this, "每日提醒已開啟！", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this, "每日提醒已關閉！", Toast.LENGTH_LONG).show()
            }
        }
    }

    // 處理目標計算與儲存
    private fun handleGoalCalculation() {
        val heightText = binding.etHeight.text.toString()
        val weightText = binding.etWeight.text.toString()
        val ageText = binding.etAge.text.toString()

        if (heightText.isEmpty() || weightText.isEmpty() || ageText.isEmpty()) {
            Toast.makeText(this, "請完整輸入身高、體重和年齡。", Toast.LENGTH_SHORT).show()
            return
        }

        // 嘗試轉換為數字，失敗則使用 0
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

                // 2. 更新 UserEntity 中的 H/W/A 數據 (以便下次載入)
                currentUser?.let {
                    val updatedUser = it.copy(
                        heightCm = heightCm,
                        weightKg = weightKg,
                        ageYears = ageYears
                    )
                    db.userDao().updateUser(updatedUser)
                }

                withContext(Dispatchers.Main) {
                    Toast.makeText(this@FeedbackActivity,
                        "目標計算成功！\n熱量:${newCalories}kcal, 蛋白質:${newProtein}g, 飲水:${newWater}ml",
                        Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Log.e("FeedbackActivity", "儲存目標失敗", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@FeedbackActivity, "目標儲存失敗: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // 實作 BMR/TDEE/蛋白質/飲水計算的簡化函數 (與 FeedbackActivity 綁定)
    private fun calculateGoals(heightCm: Int, weightKg: Int, ageYears: Int): Triple<Int, Int, Int> {
        // BMR (女性簡化版) = 655 + (9.6 * W) + (1.8 * H) - (4.7 * A)
        val bmr = 655 + (9.6 * weightKg) + (1.8 * heightCm) - (4.7 * ageYears)

        // TDEE (總熱量消耗) - 假設輕度活動量 (Activity Factor = 1.2)
        val tdee = bmr * 1.2
        val targetCalories = tdee.roundToInt()

        // 蛋白質目標 - 建議值為 1.5g/kg (一般健康維持)
        val targetProtein = (weightKg * 1.5).roundToInt()

        // 飲水目標 - 建議值為 35ml/kg
        val targetWaterMl = (weightKg * 35)

        return Triple(targetCalories, targetProtein, targetWaterMl)
    }

    // 處理提交回饋 (原功能)
    private fun handleSubmitFeedback() {
        val rating = binding.ratingBar.rating
        val feedbackText = binding.etFeedbackContent.text.toString().trim()

        if (feedbackText.isEmpty()) {
            Toast.makeText(this, "請填寫回饋內容。", Toast.LENGTH_SHORT).show()
            return
        }

        // 模擬回饋提交邏輯 (將評分/建議存入 MealEntity 中，以實現資料庫寫入要求)
        lifecycleScope.launch(Dispatchers.IO) {
            val feedbackMeal = MealEntity(
                userId = userId,
                date = "FEEDBACK_${System.currentTimeMillis()}",
                time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date()),
                type = "FEEDBACK", // 特殊標記為回饋紀錄
                mealType = "N/A",
                name = "評分:${rating.roundToInt()}/5 - 建議:$feedbackText",
                calories = 0,
                protein = 0,
                waterMl = 0
            )
            db.mealDao().insertMeal(feedbackMeal)

            withContext(Dispatchers.Main) {
                Toast.makeText(this@FeedbackActivity, "感謝您的 ${rating.roundToInt()} 星評分和建議！已送出。", Toast.LENGTH_LONG).show()
                binding.etFeedbackContent.setText("") // 清空 TextInput
            }
        }
    }

    // Background Services 模擬 (原功能)
    private fun toggleReminderService(enable: Boolean) {
        val intent = Intent(this, ReminderService::class.java)

        if (enable) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
        } else {
            stopService(intent)
        }
    }
}