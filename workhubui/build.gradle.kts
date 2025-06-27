// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    // Đã thay đổi phiên bản Kotlin xuống 1.9.22 để tương thích tốt hơn với Compose BOM 2025.05.01
    kotlin("jvm") version "1.9.22" apply false
    id("com.google.gms.google-services") version "4.4.2" apply false
}