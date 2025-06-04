plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("plugin.compose")
    id ("kotlin-kapt")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.workhubui"
    compileSdk = 36 // << THAY ĐỔI Ở ĐÂY: Nâng cấp từ 34 lên 36

    defaultConfig {
        applicationId = "com.workhubui"
        minSdk = 27
        targetSdk = 34 // Bạn có thể giữ targetSdk là 34 hoặc nâng cấp lên 36 nếu muốn
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
        // Phiên bản này (1.5.11) tương thích với Compose BOM 2025.05.01 và Kotlin 1.9.22
        kotlinCompilerExtensionVersion = "1.5.11"
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
    arguments {
        arg("room.schemaLocation", "$projectDir/schemas")
    }
}

dependencies {
    // Core Android
    implementation("androidx.core:core-ktx:1.12.0") // Giữ nguyên hoặc cập nhật nếu cần
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0") // Giữ nguyên hoặc cập nhật
    // Cập nhật activity-compose lên phiên bản mới nhất tương thích với Compose BOM
    implementation("androidx.activity:activity-compose:1.9.0") // Đã là phiên bản khá mới

    // Sử dụng Compose BOM phiên bản mới nhất (2025.05.01)
    implementation(platform("androidx.compose:compose-bom:2025.05.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:32.7.0")) // Xem xét cập nhật lên bản mới nhất nếu có, ví dụ 33.0.0
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-database-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-storage-ktx")
    implementation("com.google.firebase:firebase-messaging-ktx")

    // OkHttp
    implementation("com.squareup.okhttp3:okhttp:4.12.0") // Giữ nguyên hoặc cập nhật

    // Biometric
    implementation("androidx.biometric:biometric:1.2.0-alpha05") // Có thể có bản stable hơn

    // Room (SQLCipher)
    implementation("androidx.room:room-runtime:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    implementation("net.zetetic:android-database-sqlcipher:4.5.4") // Kiểm tra bản mới nhất

    // Security Crypto
    implementation("androidx.security:security-crypto:1.1.0-alpha06") // Có thể có bản stable hơn

    // Coil
    implementation("io.coil-kt:coil-compose:2.5.0") // Kiểm tra bản mới nhất, ví dụ 2.6.0

    // Test
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    // Đảm bảo BOM cho androidTest cũng là phiên bản mới nhất
    androidTestImplementation(platform("androidx.compose:compose-bom:2025.05.01"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
