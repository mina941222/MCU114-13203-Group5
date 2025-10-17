// build.gradle.kts (Project-level)

// 這是專案根目錄下的檔案！
plugins {
    // 確保有這些版本的定義
    // 這裡我們不再使用 alias(libs.plugins.xxx)，直接用 id 和 version
    id("com.android.application") version "8.13.0" apply false // 版本號請用您目前的 Android Gradle Plugin 版本
    id("org.jetbrains.kotlin.android") version "1.9.0" apply false // 版本號請用您目前的 Kotlin 版本
}

// ... 專案的其他設定