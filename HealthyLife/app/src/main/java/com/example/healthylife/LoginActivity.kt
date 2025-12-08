package com.example.healthylife

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.healthylife.data.AppDatabase
import com.example.healthylife.data.UserEntity
import com.example.healthylife.databinding.ActivityLoginBinding
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var db: AppDatabase
    private var isRegistering = false // 狀態：控制是登入還是註冊

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 使用 View Binding 實例化佈局
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 初始化資料庫
        db = AppDatabase.getDatabase(this)

        updateUiMode() // 初始化 UI 顯示模式

        // Button: 主要動作按鈕 (登入或註冊)
        binding.btnMainAction.setOnClickListener {
            handleMainAction()
        }

        // Button: 切換模式按鈕
        binding.btnSwitchMode.setOnClickListener {
            isRegistering = !isRegistering
            updateUiMode()
        }

        // 🚨 修正：確保鍵盤彈出
        showKeyboard()
    }

    private fun updateUiMode() {
        if (isRegistering) {
            binding.btnMainAction.text = "註冊新帳號"
            binding.btnSwitchMode.text = "已有帳號？返回登入"
        } else {
            binding.btnMainAction.text = "登入系統"
            binding.btnSwitchMode.text = "還沒有帳號？前往註冊"
        }
    }

    private fun handleMainAction() {
        val email = binding.etEmail.text.toString().trim() // TextInput
        val password = binding.etPassword.text.toString().trim() // TextInput

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "帳號和密碼不能為空", Toast.LENGTH_SHORT).show()
            return
        }

        // 啟動一個協程來處理資料庫操作 (SQLite/Room)
        lifecycleScope.launch {
            if (isRegistering) {
                registerUser(email, password) // 存入資料庫
            } else {
                loginUser(email, password) // 檢查資料庫
            }
        }
    }

    private suspend fun registerUser(email: String, password: String) {
        val existingUser = db.userDao().getUserByEmail(email)
        if (existingUser != null) {
            runOnUiThread {
                Toast.makeText(this@LoginActivity, "此 Email 已註冊！請直接登入。", Toast.LENGTH_LONG).show()
                isRegistering = false
                updateUiMode()
            }
            return
        }

        val newUser = UserEntity(email = email, passwordHash = password)
        val userId = db.userDao().insertUser(newUser)

        runOnUiThread {
            if (userId > 0) {
                Toast.makeText(this@LoginActivity, "註冊成功！請登入。", Toast.LENGTH_LONG).show()
                isRegistering = false
                updateUiMode()
            } else {
                Toast.makeText(this@LoginActivity, "註冊失敗，請重試。", Toast.LENGTH_LONG).show()
            }
        }
    }

    private suspend fun loginUser(email: String, password: String) {
        val user = db.userDao().getUserByEmail(email)

        if (user != null && user.passwordHash == password) {
            runOnUiThread {
                Toast.makeText(this@LoginActivity, "登入成功！", Toast.LENGTH_SHORT).show()

                // 登入成功，儲存使用者 ID (模擬 SharedPreferences 儲存登入狀態)
                val sharedPrefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
                sharedPrefs.edit().putInt("logged_in_user_id", user.id).apply()

                // 導航到首頁 (DashboardActivity)
                val intent = Intent(this@LoginActivity, DashboardActivity::class.java)
                startActivity(intent)
                finish() // 關閉登入頁面
            }
        } else {
            runOnUiThread {
                Toast.makeText(this@LoginActivity, "登入失敗：帳號或密碼錯誤。", Toast.LENGTH_LONG).show()
            }
        }
    }

    // 🚨 修正：用於強制彈出鍵盤
    private fun showKeyboard() {
        binding.etEmail.requestFocus() // 將焦點設置到 Email 輸入框
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(binding.etEmail, InputMethodManager.SHOW_IMPLICIT) // 強制彈出鍵盤
    }
}