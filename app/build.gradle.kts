plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("plugin.compose") // Sử dụng plugin compose của Kotlin
    id ("kotlin-kapt")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.workhubui"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.workhubui"
        minSdk = 27 // Phù hợp với java.time và các API hiện đại
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false // Tạm thời tắt để dễ debug, bật lại sau
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
        // Kiểm tra phiên bản tương thích tại: developer.android.com/jetpack/androidx/releases/compose-kotlin
        kotlinCompilerExtensionVersion = "1.5.11" // Điều chỉnh nếu cần dựa trên phiên bản Kotlin/Compose BOM
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
    // AndroidX Core
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.0")
    implementation("androidx.activity:activity-compose:1.9.0")

    // Compose - Sử dụng BOM
    val composeBom = platform("androidx.compose:compose-bom:2024.05.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // Navigation Compose
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // Material Icons Extended
    implementation("androidx.compose.material:material-icons-extended")

    // Room
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")

    // SQLCipher
    implementation("net.zetetic:android-database-sqlcipher:4.5.4")

    // Firebase - Sử dụng BOM
    implementation(platform("com.google.firebase:firebase-bom:33.1.1"))
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-messaging")
    implementation("com.google.firebase:firebase-analytics")
    // implementation("com.google.firebase:firebase-database-ktx") // Cho Realtime DB
    // implementation("com.google.firebase:firebase-storage-ktx") // Cho Cloud Storage

    // Google Play Services Auth (Cần cho Google Sign-In, có thể không cần nếu chỉ dùng Email/Pass Auth)
    implementation("com.google.android.gms:play-services-auth:21.2.0")

    // AndroidX Security (Cho EncryptedSharedPreferences)
    implementation("androidx.security:security-crypto:1.1.0-alpha06") // Hoặc phiên bản ổn định mới hơn

    // Testing
    testImplementation("junit:junit:4.13.2")
    // androidTestImplementation("androidx.test.ext:junit:1.1.5")
    // androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}