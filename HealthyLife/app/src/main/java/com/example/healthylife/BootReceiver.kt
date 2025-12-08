package com.example.healthylife

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast

/**
 * Broadcast Receiver：接收提醒廣播（重開機後重新設定通知）
 * 監聽 BOOT_COMPLETED 事件，如果使用者設定開啟提醒，則重新啟動服務。
 */
class BootReceiver : BroadcastReceiver() {

    private val PREF_KEY_REMINDER_ENABLED = "daily_reminder_enabled"

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED && context != null) {

            // 檢查使用者是否開啟了通知設定 (SharedPreferences)
            val sharedPrefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            val isEnabled = sharedPrefs.getBoolean(PREF_KEY_REMINDER_ENABLED, true) // 預設開啟

            if (isEnabled) {
                val serviceIntent = Intent(context, ReminderService::class.java)

                // 重新啟動服務以重設定時鬧鐘
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }

                // Toast.makeText(context, "HealthyLife: 重開機後已重設每日提醒。", Toast.LENGTH_LONG).show()
            }
        }
    }
}