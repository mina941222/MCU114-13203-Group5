package com.example.healthylife

import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.healthylife.data.AppDatabase
// 🚨 修正導入：從獨立檔案導入輔助數據類別 (DatabaseModels.kt)
import com.example.healthylife.data.WeeklyMacroProgress
import com.example.healthylife.data.WeeklyWaterIntake
import com.example.healthylife.databinding.ActivityStatsBinding
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max

class StatsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStatsBinding
    private lateinit var db: AppDatabase
    private var userId: Int = -1

    // 星期顯示順序: 星期一(Mon) 到 星期日(Sun)
    private val dayLabels = arrayOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStatsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = AppDatabase.getDatabase(this)

        // 取得使用者 ID
        val sharedPrefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        userId = sharedPrefs.getInt("logged_in_user_id", -1)

        if (userId == -1) {
            Toast.makeText(this, "登入狀態無效。", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        // 初始化圖表
        setupChart(binding.chartWeeklyCalories, "熱量攝取 (kcal)")
        setupChart(binding.chartWeeklyProtein, "蛋白質攝取 (g)")
        setupChart(binding.chartWeeklyWater, "飲水總量 (ml)") // 飲水圖表

        // 載入本週資料
        loadWeeklyData()

        // 返回按鈕
        binding.btnBackFromStats.setOnClickListener {
            finish()
        }
    }

    // --- 輔助函式: 獲取日期範圍 ---

    /**
     * 計算本週的起始日和結束日 (從星期一到星期日)
     * @return Pair<String, String> (startDate, endDate)
     */
    private fun getWeekRange(): Pair<String, String> {
        val calendar = Calendar.getInstance(Locale.TAIWAN)
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        // 設定每週的第一天為星期一，並調整到本週的星期一
        calendar.firstDayOfWeek = Calendar.MONDAY
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        val startDate = dateFormat.format(calendar.time)

        // 取得本週的星期日
        calendar.add(Calendar.DATE, 6)
        val endDate = dateFormat.format(calendar.time)

        // 顯示日期範圍在 UI 上
        binding.tvDateRange.text = "本週統計: $startDate ~ $endDate"

        return Pair(startDate, endDate)
    }

    // --- 輔助函式: 資料載入與處理 ---

    private fun loadWeeklyData() {
        val (startDate, endDate) = getWeekRange()

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // 1. 取得熱量和蛋白質資料
                val macroData = db.mealDao().getWeeklyMacroProgress(userId, startDate, endDate)
                val (calorieEntries, proteinEntries) = processMacroData(macroData, startDate)

                // 2. 取得飲水資料
                val waterData = db.mealDao().getWeeklyWaterIntake(userId, startDate, endDate)
                val waterEntries = processWaterData(waterData, startDate)

                withContext(Dispatchers.Main) {
                    // 繪製圖表
                    updateChart(binding.chartWeeklyCalories, calorieEntries, ContextCompat.getColor(this@StatsActivity, R.color.color_calories), "熱量")
                    updateChart(binding.chartWeeklyProtein, proteinEntries, ContextCompat.getColor(this@StatsActivity, R.color.color_add_meal), "蛋白質")
                    updateChart(binding.chartWeeklyWater, waterEntries, ContextCompat.getColor(this@StatsActivity, R.color.color_water), "飲水量")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    // 為了除錯，顯示錯誤訊息
                    Toast.makeText(this@StatsActivity, "載入數據失敗: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    /**
     * 處理營養數據，並根據星期一到星期日的順序填入 BarEntry 列表
     */
    private fun processMacroData(
        data: List<WeeklyMacroProgress>,
        startDateStr: String
    ): Pair<List<BarEntry>, List<BarEntry>> {
        val calorieMap = data.associate { it.date to it.total_calories.toFloat() }
        val proteinMap = data.associate { it.date to it.total_protein.toFloat() }

        val calorieEntries = mutableListOf<BarEntry>()
        val proteinEntries = mutableListOf<BarEntry>()

        val calendar = Calendar.getInstance(Locale.TAIWAN)
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        // 將日曆設定為本週的星期一
        calendar.time = dateFormat.parse(startDateStr) ?: Date()

        for (i in dayLabels.indices) {
            val dateStr = dateFormat.format(calendar.time)

            // 根據順序 (0=Mon, 6=Sun) 填入資料
            val calories = calorieMap[dateStr] ?: 0f
            val protein = proteinMap[dateStr] ?: 0f

            calorieEntries.add(BarEntry(i.toFloat(), calories))
            proteinEntries.add(BarEntry(i.toFloat(), protein))

            // 移到下一天
            calendar.add(Calendar.DATE, 1)
        }

        return Pair(calorieEntries, proteinEntries)
    }

    /**
     * 處理飲水數據，並根據星期一到星期日的順序填入 BarEntry 列表
     */
    private fun processWaterData(
        data: List<WeeklyWaterIntake>,
        startDateStr: String
    ): List<BarEntry> {
        val waterMap = data.associate { it.date to it.total_water.toFloat() }
        val waterEntries = mutableListOf<BarEntry>()

        val calendar = Calendar.getInstance(Locale.TAIWAN)
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        // 將日曆設定為本週的星期一
        calendar.time = dateFormat.parse(startDateStr) ?: Date()

        for (i in dayLabels.indices) {
            val dateStr = dateFormat.format(calendar.time)

            // 根據順序 (0=Mon, 6=Sun) 填入資料
            val water = waterMap[dateStr] ?: 0f
            waterEntries.add(BarEntry(i.toFloat(), water))

            // 移到下一天
            calendar.add(Calendar.DATE, 1)
        }

        return waterEntries
    }

    // --- 輔助函式: 圖表設定與更新 (使用 MPAndroidChart) ---

    private fun setupChart(chart: BarChart, description: String) {
        chart.description.isEnabled = false // 關閉描述
        chart.setMaxVisibleValueCount(7) // 最多顯示7個數據點
        chart.setPinchZoom(false)
        chart.setDrawGridBackground(false)
        chart.setExtraOffsets(5f, 10f, 5f, 10f)

        // X 軸設定
        val xAxis = chart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.granularity = 1f
        xAxis.textColor = Color.WHITE
        // 使用 Mon, Tue, ..., Sun 順序
        xAxis.valueFormatter = IndexAxisValueFormatter(dayLabels)
        xAxis.axisMinimum = -0.5f // 讓圖表從第一個標籤開始
        xAxis.labelCount = dayLabels.size

        // 左 Y 軸設定
        val leftAxis = chart.axisLeft
        leftAxis.setDrawGridLines(true)
        leftAxis.textColor = Color.WHITE
        leftAxis.axisMinimum = 0f // Y 軸從 0 開始

        // 右 Y 軸設定
        chart.axisRight.isEnabled = false

        // 圖例設定
        val legend = chart.legend
        legend.verticalAlignment = Legend.LegendVerticalAlignment.TOP
        legend.horizontalAlignment = Legend.LegendHorizontalAlignment.RIGHT
        legend.orientation = Legend.LegendOrientation.HORIZONTAL
        legend.setDrawInside(false)
        legend.textColor = Color.WHITE

        chart.setNoDataText("無本週資料可供顯示")
        chart.setNoDataTextColor(Color.GRAY)
        chart.invalidate() // 刷新圖表
    }

    private fun updateChart(chart: BarChart, entries: List<BarEntry>, color: Int, label: String) {
        if (entries.isEmpty() || entries.all { it.y == 0f }) {
            chart.data = null
            chart.invalidate()
            return
        }

        val dataSet = BarDataSet(entries, label)
        dataSet.color = color
        dataSet.valueTextColor = Color.WHITE
        dataSet.valueTextSize = 10f

        val barData = BarData(dataSet)
        barData.barWidth = 0.5f // 柱狀圖寬度

        chart.data = barData
        chart.invalidate()
        chart.animateY(1000) // 加入動畫
    }
}