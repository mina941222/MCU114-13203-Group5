package com.example.homework8

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CarDao {

    // 新增 (Create)
    @Insert
    suspend fun insert(car: UsedCar)

    // 讀取所有 (Read All) - 使用 Flow 實現即時更新
    @Query("SELECT * FROM used_cars ORDER BY id DESC")
    fun getAllCars(): Flow<List<UsedCar>>

    // 更新 (Update)
    @Update
    suspend fun update(car: UsedCar)

    // 刪除 (Delete)
    @Delete
    suspend fun delete(car: UsedCar)

    // 依據 ID 查詢 (Read by ID) - 用於修改時找到單一物件
    @Query("SELECT * FROM used_cars WHERE id = :carId")
    suspend fun getCarById(carId: Int): UsedCar?
}