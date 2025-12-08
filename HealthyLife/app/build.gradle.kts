plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    // ** 必須加入：用於 Room Database **
    id("kotlin-kapt")
}

android {
    // ... 其他設定 ...

    namespace = "com.example.healthylife"
    compileSdk = 34
    // ** 必須加入：啟用 View Binding **
    defaultConfig {
        applicationId = "com.example.healthylife"
        // 設定目標 SDK 為 API 34 (Android 14)
        targetSdk = 34

        // 確保編譯 SDK 也是 API 34

        // 最小 SDK 保持 24 或更高，以支援大多數裝置
        minSdk = 24

        // ... 其他 defaultConfig 設定 ...
    }

    // 確保這裡也指定了編譯版本
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        viewBinding = true
    }
    // ... 其他設定 ...
}

dependencies {
    // 確保您已將以下內容添加到 dependencies 區塊中

    // Room components (SQLite/Room 功能)
    implementation("androidx.room:room-runtime:2.6.1")
    implementation(libs.material)
    kapt("androidx.room:room-compiler:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")

    // Lifecycle components (ViewModel/LiveData)
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.7.0")
    implementation("androidx.activity:activity-ktx:1.9.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")

    // Material Components (確保 UI 元件美觀)
    implementation("com.google.android.material:material:1.12.0")

    // Core
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}