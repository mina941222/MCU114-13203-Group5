package com.example.healthylife

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat

/**
 * 接收 ReminderService 設置的定時鬧鐘，並發送通知。
 */
class AlarmReceiver : BroadcastReceiver() {

    private val NOTIFICATION_ID = 102
    private val CHANNEL_ID = "healthylife_channel"

    override fun onReceive(context: Context, intent: Intent) {

        // 1. 取得通知管理器
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // 2. 建立通知內容 (TextView)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("🥗 HealthyLife 飲食提醒")
            .setContentText("早上 8 點囉！別忘了記錄今日的飲食與飲水量！")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        // 3. 發送通知
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}