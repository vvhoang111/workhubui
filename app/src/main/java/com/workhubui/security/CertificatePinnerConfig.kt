package com.workhubui.security

import okhttp3.CertificatePinner

// Cấu hình Certificate Pinning cho OkHttp.
// Bạn cần thay thế các SHA256 hash bằng hash SPKI của các chứng chỉ Firebase của bạn.
// Để lấy SHA256 hash của chứng chỉ, bạn có thể dùng lệnh openssl:
// openssl s_client -servername <your-firebase-project-id>.firebaseio.com -port 443 -showcerts </dev/null 2>/dev/null | openssl x509 -pubkey -noout | openssl pkey -pubin -outform der | openssl dgst -sha256 -binary | openssl enc -base64
// Hoặc sử dụng các công cụ online để lấy SPKI SHA256.
object CertificatePinnerConfig {
    val certificatePinner: CertificatePinner = CertificatePinner.Builder()
        // Firebase Realtime Database (ví dụ)
        .add("*.firebaseio.com", "sha256/YOUR_FIREBASE_REALTIME_DB_SPKI_SHA256_HASH_HERE")
        // Firebase Authentication (ví dụ)
        .add("securetoken.googleapis.com", "sha256/YOUR_FIREBASE_AUTH_SPKI_SHA256_HASH_HERE")
        // Các domain khác của Firebase mà bạn tương tác (ví dụ: Cloud Firestore, Storage)
        .add("firestore.googleapis.com", "sha256/YOUR_FIREBASE_FIRESTORE_SPKI_SHA256_HASH_HERE")
        .add("firebasestorage.googleapis.com", "sha256/YOUR_FIREBASE_STORAGE_SPKI_SHA256_HASH_HERE")
        .build()
}