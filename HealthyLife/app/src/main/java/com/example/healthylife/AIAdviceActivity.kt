package com.example.healthylife

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.healthylife.data.AppDatabase
import com.example.healthylife.databinding.ActivityAiAdviceBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class AIAdviceActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAiAdviceBinding
    private lateinit var db: AppDatabase
    private var userId: Int = 0
    private val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    // 🚨 目標值變數 (從資料庫載入或使用預設值)
    private var calorieThreshold = 2000
    private var proteinMinGrams = 80
    private var waterGoal = 2500

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAiAdviceBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 1. 初始化資料庫
        db = AppDatabase.getDatabase(this)

        // 2. 獲取當前登入用戶 ID
        val sharedPrefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        userId = sharedPrefs.getInt("logged_in_user_id", -1)

        if (userId == -1) {
            binding.tvAiAdvice.text = "請先登入以獲得 AI 建議。"
            return
        }

        // 3. 設置返回按鈕
        binding.btnBackToDashboard.setOnClickListener { finish() } // 🚨 修正 ID
        // binding.btnBack.setOnClickListener { finish() } // 舊的錯誤 ID 已移除

        // 4. 載入資料並執行分析
        loadAndAnalyzeData()

        // 5. 重新分析按鈕
        binding.btnRunAiAnalysis.setOnClickListener {
            Toast.makeText(this, "AI 正在重新分析您的數據...", Toast.LENGTH_SHORT).show()
            loadAndAnalyzeData()
        }
    }

    // 模擬 AI 建議生成邏輯
    private fun generateAdvice(calories: Int, protein: Int, water: Int): String {
        val waterDeficit = waterGoal - water

        return when {
            // 規則 1: 熱量過高
            calories > calorieThreshold ->
                "今天總熱量($calories kcal)已超過 ${calorieThreshold} kcal 的目標。晚餐建議清淡一些，減少高油食物～"

            // 規則 2: 蛋白質不足
            protein < proteinMinGrams ->
                "蛋白質攝取($protein g)偏少，低於 ${proteinMinGrams} g 的目標。建議在下一餐多吃點豆腐、雞胸肉！"

            // 規則 3: 飲水不足
            waterDeficit > 500 -> {
                "您的飲水量似乎偏低，還差 ${waterDeficit} ml 才能達標。請記得隨時補充水分！"
            }

            // 規則 4: 均衡
            else ->
                "今天的飲食很均衡，熱量和蛋白質都控制得很好，繼續保持！"
        }
    }

    // 載入資料並更新 TextView
    private fun loadAndAnalyzeData() {
        lifecycleScope.launch(Dispatchers.IO) {

            // 🚨 修正：取得使用者目標 (個人化目標)
            val userGoals = db.userDao().getUserGoals(userId).firstOrNull()
            if (userGoals != null) {
                calorieThreshold = userGoals.targetCalories
                proteinMinGrams = userGoals.targetProtein
                waterGoal = userGoals.targetWaterMl
            }

            // 取得當日紀錄數據
            val macroProgress = db.mealDao().getDailyMacroProgress(userId, todayDate).firstOrNull()
            val totalWater = db.mealDao().getTotalWaterIntake(userId, todayDate).firstOrNull() ?: 0

            val totalCalories = macroProgress?.total_calories ?: 0
            val totalProtein = macroProgress?.total_protein ?: 0

            // 2. 執行模擬 AI 分析
            val advice = generateAdvice(totalCalories, totalProtein, totalWater)

            withContext(Dispatchers.Main) {
                // TextView: 顯示 AI 建議
                binding.tvAiAdvice.text = advice

                // TextView: 顯示數據摘要
                binding.tvDataSummary.text = """
                    --- 今日數據摘要 ---
                    總熱量：$totalCalories / $calorieThreshold kcal
                    總蛋白質：$totalProtein / $proteinMinGrams g
                    總飲水量：$totalWater / $waterGoal ml
                """.trimIndent()
            }
        }
    }
}