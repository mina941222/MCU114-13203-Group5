package com.example.healthylife

import android.app.TimePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.healthylife.data.AppDatabase
import com.example.healthylife.data.MealEntity
import com.example.healthylife.databinding.ActivityAddMealBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class AddMealActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddMealBinding
    private lateinit var db: AppDatabase
    private var userId: Int = -1
    private val todayDate: String = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    private var selectedTime: String = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())

    // 🚨 修正：將 recordTypes 設為 lateinit，延遲到 onCreate 內初始化
    private lateinit var recordTypes: Array<String>
    private var currentRecordType: String = "飲食" // 預設值

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddMealBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = AppDatabase.getDatabase(this)

        // 取得使用者 ID (Content Provider 模擬)
        val sharedPrefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        userId = sharedPrefs.getInt("logged_in_user_id", -1)

        if (userId == -1) {
            Toast.makeText(this, "登入狀態無效，請重新登入。", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        // 🚨 修正：在 onCreate 內部安全地載入 resources
        recordTypes = resources.getStringArray(R.array.record_types)
        currentRecordType = recordTypes[0]

        setupRecordTypeDropdown()
        setupMealTypeDropdown()

        // 初始化時間按鈕顯示
        binding.btnSelectTime.text = selectedTime

        // 初始顯示飲食輸入區塊
        updateInputVisibility(currentRecordType)
        setupListeners()
    }

    private fun setupRecordTypeDropdown() {
        // 紀錄類型下拉選單
        val adapter = ArrayAdapter(this, R.layout.dropdown_menu_popup_item, recordTypes)

        val etRecordType = binding.tilRecordType.editText as? android.widget.AutoCompleteTextView

        if (etRecordType != null) {
            etRecordType.setAdapter(adapter)

            // 預設選擇第一個選項 (飲食)
            etRecordType.setText(currentRecordType, false)

            // 監聽選擇事件
            etRecordType.onItemClickListener = android.widget.AdapterView.OnItemClickListener {
                    parent, view, position, id ->
                currentRecordType = parent.getItemAtPosition(position).toString()
                updateInputVisibility(currentRecordType)
            }
        }
    }

    private fun setupMealTypeDropdown() {
        // 餐別下拉選單
        val mealTypes = resources.getStringArray(R.array.meal_types)
        val adapter = ArrayAdapter(this, R.layout.dropdown_menu_popup_item, mealTypes)

        val etMealType = binding.tilMealType.editText as? android.widget.AutoCompleteTextView

        if (etMealType != null) {
            etMealType.setAdapter(adapter)
            etMealType.setText(mealTypes[0], false) // 預設選擇早餐

            etMealType.onItemClickListener = android.widget.AdapterView.OnItemClickListener {
                    parent, view, position, id ->
                // Do nothing, read mealType directly from etMealType later
            }
        }
    }

    // 根據選擇的紀錄類型，動態顯示/隱藏輸入框
    private fun updateInputVisibility(type: String) {
        if (type == "飲食") {
            binding.tilMealType.visibility = View.VISIBLE
            binding.layoutMealInput.visibility = View.VISIBLE
            binding.layoutWaterInput.visibility = View.GONE
            binding.btnSaveMeal.setBackgroundColor(getColor(R.color.color_add_meal))
        } else if (type == "飲水") {
            binding.tilMealType.visibility = View.GONE
            binding.layoutMealInput.visibility = View.GONE
            binding.layoutWaterInput.visibility = View.VISIBLE
            binding.btnSaveMeal.setBackgroundColor(getColor(R.color.color_water))
        }
    }

    private fun setupListeners() {
        // DatePicker 模擬 (Button: btn_select_time)
        binding.btnSelectTime.setOnClickListener {
            showTimePicker()
        }

        // Button: 儲存紀錄
        binding.btnSaveMeal.setOnClickListener {
            saveRecord()
        }

        // 返回首頁
        binding.btnBackToDashboard.setOnClickListener {
            finish()
        }
    }

    private fun showTimePicker() {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        val timePickerDialog = TimePickerDialog(
            this,
            { _, hourOfDay, minute ->
                selectedTime = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute)
                binding.btnSelectTime.text = selectedTime
            },
            hour,
            minute,
            true // 24小時制
        )
        timePickerDialog.show()
    }

    private fun saveRecord() {
        when (currentRecordType) {
            "飲食" -> saveMeal()
            "飲水" -> saveWater()
            else -> Toast.makeText(this, "請選擇有效的紀錄類型。", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveMeal() {
        // 從輸入框取得資料
        val mealType = (binding.tilMealType.editText as? android.widget.AutoCompleteTextView)?.text.toString() ?: ""
        val name = binding.etMealName.text.toString().trim()
        val caloriesText = binding.etCalories.text.toString().trim()
        val proteinText = binding.etProtein.text.toString().trim()

        if (name.isEmpty() || caloriesText.isEmpty() || proteinText.isEmpty()) {
            Toast.makeText(this, "請填寫所有飲食欄位。", Toast.LENGTH_SHORT).show()
            return
        }

        val calories = caloriesText.toIntOrNull() ?: 0
        val protein = proteinText.toIntOrNull() ?: 0

        if (calories <= 0 || protein < 0) {
            Toast.makeText(this, "熱量必須大於 0。", Toast.LENGTH_SHORT).show()
            return
        }

        val newMeal = MealEntity(
            userId = userId,
            date = todayDate,
            time = selectedTime,
            type = "飲食", // 🚨 修正參數名稱：使用 'type'
            mealType = mealType,
            name = name,
            calories = calories,
            protein = protein,
            waterMl = 0 // 飲水紀錄為 0
        )

        // 寫入資料庫 (SQLite/Room)
        lifecycleScope.launch(Dispatchers.IO) {
            db.mealDao().insertMeal(newMeal)

            withContext(Dispatchers.Main) {
                Toast.makeText(this@AddMealActivity, "飲食紀錄儲存成功！🍔", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun saveWater() {
        // 從輸入框取得飲水量
        val amountText = binding.etWaterAmount.text.toString().trim()

        if (amountText.isEmpty()) {
            Toast.makeText(this, "請輸入飲水量。", Toast.LENGTH_SHORT).show()
            return
        }

        val amount = amountText.toIntOrNull()

        if (amount == null || amount <= 0) {
            Toast.makeText(this, "飲水量必須為有效數字 (大於 0)。", Toast.LENGTH_SHORT).show()
            return
        }

        val newWater = MealEntity(
            userId = userId,
            date = todayDate,
            time = selectedTime,
            type = "飲水", // 🚨 修正參數名稱：使用 'type'
            mealType = "N/A",
            name = "水", // 紀錄名稱
            calories = 0, // 熱量為 0
            protein = 0, // 蛋白質為 0
            waterMl = amount // 儲存飲水量
        )

        // 寫入資料庫 (SQLite/Room)
        lifecycleScope.launch(Dispatchers.IO) {
            db.mealDao().insertMeal(newWater)

            withContext(Dispatchers.Main) {
                Toast.makeText(this@AddMealActivity, "飲水紀錄儲存成功！💧", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
}