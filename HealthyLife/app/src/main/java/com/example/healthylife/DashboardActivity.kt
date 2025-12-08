package com.example.healthylife

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.healthylife.data.AppDatabase
import com.example.healthylife.data.MealEntity
import com.example.healthylife.data.UserGoals
import com.example.healthylife.databinding.ActivityDashboardBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import com.example.healthylife.data.DailyMacroProgress

class DashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDashboardBinding
    private lateinit var db: AppDatabase
    private lateinit var mealAdapter: MealAdapter
    private var userId: Int = -1
    private var currentDate: String = ""

    // 儲存當前目標，用於計算剩餘量
    private var currentGoals = UserGoals(userId = -1, targetCalories = 2000, targetProtein = 80, targetWaterMl = 2000)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = AppDatabase.getDatabase(this)

        // 1. 獲取使用者 ID
        val sharedPrefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        userId = sharedPrefs.getInt("logged_in_user_id", -1)

        if (userId == -1) {
            Log.e("Dashboard", "User ID not found, logging out.")
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        // 2. 初始化日期
        val today = Calendar.getInstance()
        updateDateDisplay(today) // 設置今日日期

        // 3. 初始化 UI
        setupRecyclerView()
        setupListeners()

        // 4. 載入並實時觀察 goals 和 meals 數據
        loadAndObserveGoals() // 🚨 初始載入 goals
    }

    override fun onResume() {
        super.onResume()
        // 🚨 確保在 Activity 恢復時，重新檢查數據
        val today = Calendar.getInstance()
        updateDateDisplay(today)
    }

    // 設置日期顯示 (格式: 2025年11月30日 (星期六))
    private fun updateDateDisplay(calendar: Calendar) {
        // 🚨 修正格式: 加入年份、月份、日 和 星期幾
        val dateFormat = SimpleDateFormat("yyyy年MM月dd日 (EEEE)", Locale.TRADITIONAL_CHINESE)

        currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
        val displayDate = dateFormat.format(calendar.time)

        // 更新標題顯示
        binding.tvDateDisplay.text = displayDate

        // 重新觀察當日餐點記錄
        loadAndObserveMeals()
    }

    private fun setupRecyclerView() {
        mealAdapter = MealAdapter(emptyList())
        binding.rvMealRecords.apply {
            layoutManager = LinearLayoutManager(this@DashboardActivity)
            adapter = mealAdapter
        }
        // 新增：沒有紀錄時的提示文字
        binding.tvNoRecords.visibility = View.VISIBLE
    }

    private fun setupListeners() {
        // 登出按鈕監聽器
        binding.btnLogout.setOnClickListener {
            logout()
        }

        // Navigation 導航監聽器 (下方導航列)
        binding.navAddMeal.setOnClickListener { startActivity(Intent(this, AddMealActivity::class.java)) }
        binding.navStats.setOnClickListener { startActivity(Intent(this, StatsActivity::class.java)) }
        binding.navAiAdvice.setOnClickListener { startActivity(Intent(this, AIAdviceActivity::class.java)) }
        binding.navFeedback.setOnClickListener { startActivity(Intent(this, FeedbackActivity::class.java)) }
    }

    // ----------------------------------------------------
    // 實時觀察目標數據 (UserGoals) - 解決目標不更新的問題
    // ----------------------------------------------------
    private fun loadAndObserveGoals() {
        if (userId == -1) return

        lifecycleScope.launch {
            // 從 DAO 獲取 Flow<UserGoals?>，Room 會在數據變更時自動發射新值
            db.userDao().getUserGoals(userId).collect { goals ->
                withContext(Dispatchers.Main) {
                    // 如果 goals 為 null，則使用預設值
                    currentGoals = goals ?: UserGoals(userId, 2000, 80, 2500)

                    // 1. 更新目標顯示 (這個必須先更新)
                    updateTargetUI(currentGoals)

                    // 2. 由於目標已經改變，觸發餐點數據的重新計算 (Meal Flow 會自動執行)
                    Log.d("DashboardActivity", "Goals updated from DB: ${currentGoals.targetCalories} kcal")
                }
            }
        }
    }

    // ----------------------------------------------------
    // 實時觀察當日餐點數據 (MealEntity)
    // ----------------------------------------------------
    private fun loadAndObserveMeals() {
        if (userId == -1 || currentDate.isEmpty()) return

        lifecycleScope.launch {
            // 監聽當日餐點與飲水總量
            combine(
                db.mealDao().getDailyMacroProgress(userId, currentDate),
                db.mealDao().getTotalWaterIntake(userId, currentDate),
                db.mealDao().getDailyMeals(userId, currentDate)
            ) { progress, totalWater, meals ->
                Triple(progress, totalWater, meals)
            }.collect { (progress, totalWater, meals) ->
                withContext(Dispatchers.Main) {
                    // 1. 更新 RecyclerView
                    mealAdapter.updateMeals(meals)

                    // 2. 顯示/隱藏沒有紀錄的提示
                    binding.tvNoRecords.visibility = if (meals.isEmpty()) View.VISIBLE else View.GONE

                    // 3. 重新計算總覽數據 (傳遞最新的 meals 數據)
                    calculateAndDisplaySummary(progress, totalWater)
                }
            }
        }
    }

    // ----------------------------------------------------
    // 獨立更新目標文字 (確保目標值是 Flow 監聽到的最新值)
    // ----------------------------------------------------
    private fun updateTargetUI(goals: UserGoals) {
        // 更新目標文字
        binding.tvTargetCalories.text = "${goals.targetCalories} kcal"
        binding.tvTargetProtein.text = "${goals.targetProtein} g"
        binding.tvTargetWater.text = "${goals.targetWaterMl} ml"
    }

    // ----------------------------------------------------
    // 計算並顯示當日總覽 (使用 Flow 監聽到的最新進度)
    // ----------------------------------------------------
    private fun calculateAndDisplaySummary(progress: DailyMacroProgress? = null, totalWater: Int? = null) {
        val currentCalories = progress?.total_calories ?: 0
        val currentProtein = progress?.total_protein ?: 0
        val currentWater = totalWater ?: 0

        // 計算剩餘量 (使用 Flow 監聽到的 currentGoals)
        val remainingCalories = currentGoals.targetCalories - currentCalories
        val remainingProtein = currentGoals.targetProtein - currentProtein
        val remainingWater = currentGoals.targetWaterMl - currentWater

        // ------------------------------------
        // 1. 更新總攝入量
        // ------------------------------------
        binding.tvCurrentCalories.text = "$currentCalories"
        binding.tvCurrentProtein.text = "$currentProtein"
        binding.tvCurrentWater.text = "$currentWater"

        // ------------------------------------
        // 2. 更新進度條
        // ------------------------------------
        // 確保 max 至少為 1
        binding.pbCalories.max = currentGoals.targetCalories.coerceAtLeast(1)
        binding.pbCalories.progress = currentCalories.coerceAtMost(currentGoals.targetCalories)

        binding.pbProtein.max = currentGoals.targetProtein.coerceAtLeast(1)
        binding.pbProtein.progress = currentProtein.coerceAtMost(currentGoals.targetProtein)

        binding.pbWater.max = currentGoals.targetWaterMl.coerceAtLeast(1)
        binding.pbWater.progress = currentWater.coerceAtMost(currentGoals.targetWaterMl)

        // ------------------------------------
        // 3. 更新剩餘量
        // ------------------------------------
        binding.tvRemainingCalories.text = formatRemainingText(remainingCalories, "kcal")
        binding.tvRemainingProtein.text = formatRemainingText(remainingProtein, "g")
        binding.tvRemainingWater.text = formatRemainingText(remainingWater, "ml")
    }

    // 格式化剩餘文字顯示
    private fun formatRemainingText(remaining: Int, unit: String): String {
        return if (remaining >= 0) {
            "剩餘: $remaining $unit"
        } else {
            "超標: ${remaining.times(-1)} $unit"
        }
    }

    private fun logout() {
        // 清除登入狀態
        val sharedPrefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        sharedPrefs.edit().remove("logged_in_user_id").apply()

        // 導航回登入頁面
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK // 清除所有活動堆棧
        startActivity(intent)
        finish()
    }
}