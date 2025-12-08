package com.example.healthylife

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import java.util.*

class ReminderService : Service() {

    private val NOTIFICATION_ID = 101
    private val CHANNEL_ID = "healthylife_channel"

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // 1. 確保服務在前台運行 (Foreground Service)
        startForeground(NOTIFICATION_ID, createNotification("HealthyLife", "正在運行每日提醒服務"))

        // 2. 設置定時鬧鐘 (AlarmManager)
        setDailyAlarm(this)

        return START_STICKY // 服務被殺死後會自動重啟
    }

    // 建立一個簡單的前台通知
    private fun createNotification(title: String, content: String) =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // 使用預設圖標
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

    // 設置每天早上 8 點的通知鬧鐘 (Broadcast Receiver 模擬)
    fun setDailyAlarm(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java) // 鬧鐘觸發的 Receiver

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        )

        // 設置時間：每天早上 8 點
        val calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, 8) // 早上 8 點
            set(Calendar.MINUTE, 0)      // 0 分
            set(Calendar.SECOND, 0)

            // 如果當前時間已經過了早上 8 點，則設置為明天的 8 點
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        // 設置重複鬧鐘 (每天一次)
        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP, // 喚醒設備
            calendar.timeInMillis,
            AlarmManager.INTERVAL_DAY, // 間隔一天
            pendingIntent
        )

        createNotificationChannel()
    }

    // 建立通知頻道
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "每日提醒通知"
            val descriptionText = "每日早上提醒記錄飲食"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // 移除鬧鐘
        cancelDailyAlarm(this)
    }

    // 取消鬧鐘
    fun cancelDailyAlarm(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        )
        alarmManager.cancel(pendingIntent)
    }
}