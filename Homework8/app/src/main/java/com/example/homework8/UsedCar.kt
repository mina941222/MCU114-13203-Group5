package com.example.homework8

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "used_cars")
data class UsedCar(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0, // 主鍵 (自動遞增)
    val brand: String, // 廠牌
    val year: Int, // 年份
    val price: Int // 價格 (假設單位為 NTD 或 USD)
)