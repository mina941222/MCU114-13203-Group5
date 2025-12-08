package com.example.healthylife.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

// 假設 AppDao 包含所有實體的 DAO 介面
@Dao
interface AppDao {
    // --- UserEntity (用戶資訊) ---
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertUser(user: UserEntity): Long

    @Query("SELECT * FROM user_table WHERE email = :email")
    suspend fun getUserByEmail(email: String): UserEntity?

    @Query("SELECT * FROM user_table WHERE id = :userId")
    fun getUser(userId: Int): Flow<UserEntity?>

    @Update
    suspend fun updateUser(user: UserEntity)

    // --- UserGoals (目標資訊) ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateGoals(goals: UserGoals)

    // 🚨 關鍵點: 返回 Flow 以實現實時觀察 (Room 會在目標變更時自動通知)
    @Query("SELECT * FROM user_goals WHERE userId = :userId LIMIT 1")
    fun getUserGoals(userId: Int): Flow<UserGoals?>

    // --- MealEntity (餐點記錄) ---
    @Insert
    suspend fun insertMeal(meal: MealEntity)

    // 實時觀察當日餐點記錄
    @Query("SELECT * FROM meal_table WHERE userId = :userId AND date = :date AND type = 'MEAL' ORDER BY time DESC")
    fun getMealsForDate(userId: Int, date: String): Flow<List<MealEntity>>
}