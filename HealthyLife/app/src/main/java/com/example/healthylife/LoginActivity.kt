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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var db: AppDatabase
    private var isRegistering = false // 狀態：控制是登入還是註冊

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = AppDatabase.getDatabase(this)

        // 🚨 檢查登入狀態
        val sharedPrefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val loggedInUserId = sharedPrefs.getInt("logged_in_user_id", -1)
        if (loggedInUserId != -1) {
            checkGoalSetupAndNavigate(loggedInUserId)
            return
        }

        updateUiMode()

        binding.btnMainAction.setOnClickListener {
            handleMainAction()
        }

        binding.btnSwitchMode.setOnClickListener {
            isRegistering = !isRegistering
            updateUiMode()
        }

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
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "帳號和密碼不能為空", Toast.LENGTH_SHORT).show()
            return
        }

        // 使用協程處理
        lifecycleScope.launch(Dispatchers.IO) {
            if (isRegistering) {
                registerUser(email, password)
            } else {
                loginUser(email, password)
            }
        }
    }

    private suspend fun registerUser(email: String, password: String) {
        val existingUser = db.userDao().getUserByEmail(email)
        if (existingUser != null) {
            withContext(Dispatchers.Main) {
                Toast.makeText(this@LoginActivity, "此 Email 已註冊！請直接登入。", Toast.LENGTH_LONG).show()
                isRegistering = false
                updateUiMode()
            }
            return
        }

        // 🚨 確保 UserEntity 創建正確
        val newUser = UserEntity(email = email, passwordHash = password)
        val userIdLong = db.userDao().insertUser(newUser)
        val userId = userIdLong.toInt()

        if (userId > 0) {
            withContext(Dispatchers.Main) {
                Toast.makeText(this@LoginActivity, "註冊成功！", Toast.LENGTH_SHORT).show()
                saveLoginState(userId)
                // 註冊完跳轉至目標設定
                checkGoalSetupAndNavigate(userId)
            }
        } else {
            withContext(Dispatchers.Main) {
                Toast.makeText(this@LoginActivity, "註冊失敗，請重試。", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private suspend fun loginUser(email: String, password: String) {
        val user = db.userDao().getUserByEmail(email)

        if (user != null && user.passwordHash == password) {
            withContext(Dispatchers.Main) {
                Toast.makeText(this@LoginActivity, "登入成功！", Toast.LENGTH_SHORT).show()
                saveLoginState(user.id)
                checkGoalSetupAndNavigate(user.id)
            }
        } else {
            withContext(Dispatchers.Main) {
                Toast.makeText(this@LoginActivity, "登入失敗：帳號或密碼錯誤。", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun checkGoalSetupAndNavigate(userId: Int) {
        // 使用 lifecycleScope 確保在 Main 安全執行 UI 跳轉
        lifecycleScope.launch {
            val user = withContext(Dispatchers.IO) {
                db.userDao().getUser(userId).firstOrNull()
            }

            // 🚨 檢查 Intent 跳轉是否會崩潰
            try {
                if (user?.heightCm ?: 0 > 0 && user?.weightKg ?: 0 > 0) {
                    startActivity(Intent(this@LoginActivity, DashboardActivity::class.java))
                } else {
                    startActivity(Intent(this@LoginActivity, GoalSetupActivity::class.java))
                }
                finish()
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@LoginActivity, "跳轉失敗：請檢查 Manifest 是否註冊 Activity", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun saveLoginState(userId: Int) {
        val sharedPrefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        sharedPrefs.edit().putInt("logged_in_user_id", userId).apply()
    }

    private fun showKeyboard() {
        binding.etEmail.requestFocus()
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(binding.etEmail, InputMethodManager.SHOW_IMPLICIT)
    }
}