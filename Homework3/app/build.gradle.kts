// build.gradle.kts (Module: app) - 最終修正版本
// 這個版本不依賴 libs.versions.toml，修正了所有 Gradle 相關的紅字問題

plugins {
    // 解決 build.gradle.kts 本身一片紅字的問題
    id("com.android.application")
    id("org.jetbrains.kotlin.android")

    // 【✅ 解決 Order.kt 的 @Parcelize 紅字】
    id("kotlin-parcelize")
}

android {
    namespace = "com.example.homework3"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.homework3"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    // 【✅ 解決 Unknown Kotlin JVM target: 21 紅字】
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }

    buildFeatures {
        // 您的專案是 Activity/XML 結構，不需要 Compose，建議移除以簡化
        // compose = true

        // 【✅ 解決所有 ActivityXxxBinding 紅字】
        viewBinding = true
    }
}

dependencies {

    // 常用 AndroidX 依賴 (直接使用版本字串，避免 libs.xxx 紅字)
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.activity:activity-ktx:1.9.0")

    // Test dependencies
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}