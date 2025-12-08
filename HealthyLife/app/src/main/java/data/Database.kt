package com.example.healthylife.data

import android.content.Context
import androidx.room.*
import androidx.room.Dao
import androidx.room.Database
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

// --- 1. 使用者 (UserEntity) ---
// 用於儲存登入帳號密碼，以及用戶的身高/體重/年齡資訊
@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val email: String,
    val passwordHash: String, // 實際應用中應儲存安全的 Hash 值，此處為簡化版
    val name: String = "未設定", // 新增用戶名稱欄位

    // 用於目標計算的新增欄位
    val heightCm: Int = 0, // 身高 (公分)
    val weightKg: Int = 0, // 體重 (公斤)
    val ageYears: Int = 0 // 年齡 (歲)
)

// --- 2. 每日營養目標 (UserGoals) ---
// 用於儲存用戶的每日熱量、蛋白質和飲水目標
@Entity(tableName = "user_goals")
data class UserGoals(
    @PrimaryKey val userId: Int, // 外鍵，與 UserEntity 連結
    val targetCalories: Int, // 熱量目標 (大卡)
    val targetProtein: Int, // 蛋白質目標 (克)
    val targetWaterMl: Int // 飲水目標 (毫升)
)

// --- 3. 飲食紀錄 (MealEntity) ---
// 用於儲存用戶的每一筆飲食或飲水紀錄
@Entity(tableName = "meals")
data class MealEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: Int, // 外鍵
    val date: String, // 紀錄日期 (e.g., "YYYY-MM-DD")
    val time: String, // 紀錄時間 (e.g., "HH:MM")
    val type: String, // 類型: "飲食" 或 "飲水"
    val mealType: String, // 餐點類型 (早餐/午餐/晚餐/點心/飲水)
    val name: String, // 紀錄名稱
    val calories: Int = 0, // 熱量 (大卡)
    val protein: Int = 0, // 蛋白質 (克)
    val waterMl: Int = 0 // 飲水量 (毫升)
)

// --- DAO (Data Access Object) ---
// 定義資料庫操作介面
@Dao
interface UserDao {
    // 使用者操作
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertUser(user: UserEntity): Long

    @Query("SELECT * FROM users WHERE email = :email")
    suspend fun getUserByEmail(email: String): UserEntity?

    // 取得使用者所有資訊 (包含 H/W/A)
    @Query("SELECT * FROM users WHERE id = :userId")
    fun getUser(userId: Int): Flow<UserEntity?>

    // 更新使用者 H/W/A 資訊
    @Update
    suspend fun updateUser(user: UserEntity)

    // 目標操作
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateGoals(goals: UserGoals)

    // 取得使用者目標 (Flow 實現實時更新)
    @Query("SELECT * FROM user_goals WHERE userId = :userId")
    fun getUserGoals(userId: Int): Flow<UserGoals?>
}

@Dao
interface MealDao {
    // 紀錄操作
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeal(meal: MealEntity)

    @Delete
    suspend fun deleteMeal(meal: MealEntity)

    @Query("SELECT * FROM meals WHERE userId = :userId AND date = :date ORDER BY time DESC")
    fun getDailyMeals(userId: Int, date: String): Flow<List<MealEntity>>

    // 每日營養總進度 (使用 DatabaseModels.kt 中的 DailyMacroProgress)
    @Query("""
        SELECT SUM(calories) as total_calories, SUM(protein) as total_protein 
        FROM meals 
        WHERE userId = :userId AND date = :date AND type = '飲食'
    """)
    fun getDailyMacroProgress(userId: Int, date: String): Flow<DailyMacroProgress?>

    // 每日總飲水量
    @Query("""
        SELECT SUM(waterMl) as total_water 
        FROM meals 
        WHERE userId = :userId AND date = :date AND type = '飲水'
    """)
    fun getTotalWaterIntake(userId: Int, date: String): Flow<Int?>

    // 🚨 修正：每週巨量營養素進度 (使用 DatabaseModels.kt 中的 WeeklyMacroProgress)
    @Query("""
        SELECT date, SUM(calories) as total_calories, SUM(protein) as total_protein 
        FROM meals 
        WHERE userId = :userId AND date BETWEEN :startDate AND :endDate AND type = '飲食' 
        GROUP BY date ORDER BY date ASC
    """)
    suspend fun getWeeklyMacroProgress(userId: Int, startDate: String, endDate: String): List<WeeklyMacroProgress>

    // 🚨 修正：每週飲水總量進度 (使用 DatabaseModels.kt 中的 WeeklyWaterIntake)
    @Query("""
        SELECT date, SUM(waterMl) as total_water 
        FROM meals 
        WHERE userId = :userId AND date BETWEEN :startDate AND :endDate AND type = '飲水' 
        GROUP BY date ORDER BY date ASC
    """)
    suspend fun getWeeklyWaterIntake(userId: Int, startDate: String, endDate: String): List<WeeklyWaterIntake>
}

// --- AppDatabase ---
@Database(
    entities = [UserEntity::class, UserGoals::class, MealEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun mealDao(): MealDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "healthy_life_db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}