// app/build.gradle.kts
plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("plugin.compose")
    id("kotlin-kapt") // Đảm bảo plugin này được áp dụng
    id("com.google.gms.google-services")
}

android {
    namespace = "com.workhubui"
    compileSdk = 36 // Giữ nguyên hoặc đảm bảo đây là phiên bản bạn muốn

    defaultConfig {
        applicationId = "com.workhubui"
        minSdk = 27
        targetSdk = 34 // Cân nhắc nâng cấp lên 36 nếu compileSdk là 36
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

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.11" // Đảm bảo phiên bản này tương thích
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/LICENSE.md"
            excludes += "/META-INF/LICENSE-notice.md"
        }
    }
}

kapt {
    correctErrorTypes = true // Giúp hiển thị lỗi từ annotation processor chính xác hơn
    arguments {
        // Chỉ định vị trí schema cho Room
        // Đảm bảo "room.schemaLocation" là argument duy nhất nếu không có các argument Room khác
        arg("room.schemaLocation", "$projectDir/schemas")
        // ĐÃ XÓA DÒNG `kapt.kotlin.generated` TẠI ĐÂY
    }
    // Thuộc tính 'kotlinSourcesDestinationDir' không nên cấu hình thủ công trừ khi có lý do đặc biệt.
    // kapt tự quản lý thư mục chứa code Kotlin được sinh ra.
}

dependencies {
    // Core Android
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-compose:1.9.0")

    // Compose BOM
    implementation(platform("androidx.compose:compose-bom:2024.06.00")) // Sử dụng BOM mới nhất hoặc phiên bản bạn đã chọn (ví dụ 2025.05.01)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.7.7") // Hoặc phiên bản mới hơn nếu có

    // Firebase BOM
    implementation(platform("com.google.firebase:firebase-bom:33.0.0")) // Sử dụng Firebase BOM mới nhất
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-database-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-storage-ktx")
    implementation("com.google.firebase:firebase-messaging-ktx")

    // OkHttp
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // Biometric
    implementation("androidx.biometric:biometric:1.2.0-alpha05") // Cân nhắc bản stable nếu có

    // Room (SQLCipher)
    implementation("androidx.room:room-runtime:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1") // Annotation processor cho Room
    implementation("androidx.room:room-ktx:2.6.1")
    implementation("net.zetetic:android-database-sqlcipher:4.5.4") // Kiểm tra phiên bản mới nhất

    // Security Crypto
    implementation("androidx.security:security-crypto:1.1.0-alpha06") // Cân nhắc bản stable

    // Coil
    implementation("io.coil-kt:coil-compose:2.6.0") // Cập nhật Coil

    // Test
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.06.00")) // Đồng bộ BOM
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}