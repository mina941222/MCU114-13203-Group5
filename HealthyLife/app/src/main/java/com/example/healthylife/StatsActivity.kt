package com.example.healthylife

import android.app.DatePickerDialog
import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.healthylife.data.AppDatabase
// 🚨 修正導入：確保導入輔助數據類別
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
// 🚨 注意：createLayoutHeightAnimator 在同一個 package (Utils.kt) 中，不需要 import

class StatsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStatsBinding
    private lateinit var db: AppDatabase
    private var userId: Int = -1

    // 星期顯示順序: 星期一(Mon) 到 星期日(Sun)
    private val dayLabels = arrayOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

    // 記錄當前顯示週次的「星期一」日期
    private var currentWeekStart: Calendar = Calendar.getInstance(Locale.TAIWAN).apply {
        firstDayOfWeek = Calendar.MONDAY
        // 調整到本週一
        set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        // 清除時分秒，避免干擾
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

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
        setupChart(binding.chartWeeklyWater, "飲水總量 (ml)")

        // 初始載入
        loadWeeklyData()
        setupListeners()
    }

    private fun setupListeners() {
        binding.btnBackFromStats.setOnClickListener { finish() }

        // 上一週
        binding.btnPrevWeek.setOnClickListener {
            currentWeekStart.add(Calendar.DAY_OF_YEAR, -7)
            loadWeeklyData()
        }

        // 下一週
        binding.btnNextWeek.setOnClickListener {
            currentWeekStart.add(Calendar.DAY_OF_YEAR, 7)
            loadWeeklyData()
        }

        // 日曆選擇
        binding.btnCalendarSelect.setOnClickListener {
            showDatePicker()
        }
    }

    private fun showDatePicker() {
        val datePickerDialog = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                // 使用者選擇日期後，計算該日期所在的星期一
                val selectedDate = Calendar.getInstance(Locale.TAIWAN).apply {
                    set(year, month, dayOfMonth)
                    firstDayOfWeek = Calendar.MONDAY
                    set(Calendar.DAY_OF_WEEK, Calendar.MONDAY) // 自動跳到該週的週一

                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                currentWeekStart = selectedDate
                loadWeeklyData()
            },
            currentWeekStart.get(Calendar.YEAR),
            currentWeekStart.get(Calendar.MONTH),
            currentWeekStart.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.show()
    }

    private fun loadWeeklyData() {
        // 計算這一週的結束日 (週一 + 6天 = 週日)
        val endOfWeek = (currentWeekStart.clone() as Calendar).apply {
            add(Calendar.DAY_OF_YEAR, 6)
        }

        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val displayFormat = SimpleDateFormat("MM/dd", Locale.getDefault())

        val startDateStr = dateFormat.format(currentWeekStart.time)
        val endDateStr = dateFormat.format(endOfWeek.time)

        // 更新 UI 顯示日期範圍
        binding.tvDateRange.text = "${displayFormat.format(currentWeekStart.time)} - ${displayFormat.format(endOfWeek.time)}"

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // 資料庫查詢
                val macroData = db.mealDao().getWeeklyMacroProgress(userId, startDateStr, endDateStr)
                val (calorieEntries, proteinEntries) = processMacroData(macroData, startDateStr)

                val waterData = db.mealDao().getWeeklyWaterIntake(userId, startDateStr, endDateStr)
                val waterEntries = processWaterData(waterData, startDateStr)

                withContext(Dispatchers.Main) {
                    // 更新圖表
                    updateChart(binding.chartWeeklyCalories, calorieEntries, ContextCompat.getColor(this@StatsActivity, R.color.color_calories), "熱量")
                    updateChart(binding.chartWeeklyProtein, proteinEntries, ContextCompat.getColor(this@StatsActivity, R.color.color_add_meal), "蛋白質")
                    updateChart(binding.chartWeeklyWater, waterEntries, ContextCompat.getColor(this@StatsActivity, R.color.color_water), "飲水量")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@StatsActivity, "載入數據失敗: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // 處理營養數據
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
        calendar.time = dateFormat.parse(startDateStr) ?: Date()

        for (i in dayLabels.indices) {
            val dateStr = dateFormat.format(calendar.time)
            val calories = calorieMap[dateStr] ?: 0f
            val protein = proteinMap[dateStr] ?: 0f

            calorieEntries.add(BarEntry(i.toFloat(), calories))
            proteinEntries.add(BarEntry(i.toFloat(), protein))
            calendar.add(Calendar.DATE, 1)
        }
        return Pair(calorieEntries, proteinEntries)
    }

    // 處理飲水數據
    private fun processWaterData(
        data: List<WeeklyWaterIntake>,
        startDateStr: String
    ): List<BarEntry> {
        val waterMap = data.associate { it.date to it.total_water.toFloat() }
        val waterEntries = mutableListOf<BarEntry>()

        val calendar = Calendar.getInstance(Locale.TAIWAN)
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        calendar.time = dateFormat.parse(startDateStr) ?: Date()

        for (i in dayLabels.indices) {
            val dateStr = dateFormat.format(calendar.time)
            val water = waterMap[dateStr] ?: 0f
            waterEntries.add(BarEntry(i.toFloat(), water))
            calendar.add(Calendar.DATE, 1)
        }
        return waterEntries
    }

    private fun setupChart(chart: BarChart, description: String) {
        chart.description.isEnabled = false
        chart.setMaxVisibleValueCount(7)
        chart.setPinchZoom(false)
        chart.setDrawGridBackground(false)
        chart.setExtraOffsets(5f, 10f, 5f, 10f)

        val xAxis = chart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.granularity = 1f
        xAxis.textColor = Color.WHITE
        xAxis.valueFormatter = IndexAxisValueFormatter(dayLabels)
        xAxis.axisMinimum = -0.5f
        xAxis.labelCount = dayLabels.size

        val leftAxis = chart.axisLeft
        leftAxis.setDrawGridLines(true)
        leftAxis.textColor = Color.WHITE
        leftAxis.axisMinimum = 0f

        chart.axisRight.isEnabled = false

        val legend = chart.legend
        legend.verticalAlignment = Legend.LegendVerticalAlignment.TOP
        legend.horizontalAlignment = Legend.LegendHorizontalAlignment.RIGHT
        legend.orientation = Legend.LegendOrientation.HORIZONTAL
        legend.setDrawInside(false)
        legend.textColor = Color.WHITE

        chart.setNoDataText("此週無資料")
        chart.setNoDataTextColor(Color.GRAY)
        chart.invalidate()
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
        barData.barWidth = 0.5f

        chart.data = barData
        chart.invalidate()
        chart.animateY(800)
    }
}