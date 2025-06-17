// app/src/main/java/com/workhubui/security/CertificatePinnerConfig.kt
package com.workhubui.security

import okhttp3.CertificatePinner

// Cấu hình Certificate Pinning cho OkHttp.
// QUAN TRỌNG:
// 1. Bạn cần thay thế các "YOUR_..." bằng SHA256 hash SPKI thực tế của các chứng chỉ Firebase của bạn.
// 2. Certificate Pinning này chỉ áp dụng cho các OkHttpClient TÙY CHỈNH mà bạn tạo và sử dụng
//    trong ứng dụng của mình (ví dụ: để gọi các API riêng của bạn).
// 3. Các SDK của Firebase (Authentication, Firestore, Realtime Database, Storage) thường sử dụng
//    cơ chế kết nối mạng nội bộ của riêng chúng và KHÔNG TRỰC TIẾP sử dụng OkHttpClient này
//    cho các kết nối Firebase mặc định của chúng. Việc ghim chứng chỉ cho Firebase SDK
//    thường phức tạp hơn và được thực hiện ở cấp độ hệ thống (ví dụ: thay đổi trust anchors)
//    hoặc thông qua các thư viện đặc biệt.
//
// CÁCH LẤY SHA256 SPKI HASH CỦA CHỨNG CHỈ (ví dụ cho googleapis.com):
// Chạy lệnh openssl sau trong Terminal (trên máy Linux/macOS có openssl, hoặc WSL trên Windows):
// openssl s_client -servername googleapis.com -port 443 -showcerts </dev/null 2>/dev/null | \
// openssl x509 -pubkey -noout | openssl pkey -pubin -outform der | \
// openssl dgst -sha256 -binary | openssl enc -base64
//
// Lệnh này sẽ trả về một chuỗi base64. Thêm "sha256/" vào trước chuỗi đó.
// Ví dụ: sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=
object CertificatePinnerConfig {
    val certificatePinner: CertificatePinner = CertificatePinner.Builder()
        // Firebase Realtime Database (ví dụ)
        .add("*.firebaseio.com", "sha256/YOUR_FIREBASE_REALTIME_DB_SPKI_SHA256_HASH_HERE")
        // Firebase Authentication và các dịch vụ Google API khác
        .add("securetoken.googleapis.com", "sha256/YOUR_FIREBASE_AUTH_SPKI_SHA256_HASH_HERE")
        .add("firestore.googleapis.com", "sha256/YOUR_FIREBASE_FIRESTORE_SPKI_SHA256_HASH_HERE")
        .add("firebasestorage.googleapis.com", "sha256/YOUR_FIREBASE_STORAGE_SPKI_SHA256_HASH_HERE")
        // Thêm các domain khác mà ứng dụng của bạn kết nối qua OkHttp và bạn muốn ghim chứng chỉ
        // .add("your.custom.api.domain.com", "sha256/YOUR_CUSTOM_API_SPKI_SHA256_HASH_HERE")
        .build()
}

// CÁCH SỬ DỤNG CertificatePinner này với OkHttpClient TÙY CHỈNH (Ví dụ):
// Nếu bạn có một OkHttpClient trong Repository hoặc NetworkClient của mình, bạn sẽ làm như sau:
/*
import okhttp3.OkHttpClient

class YourNetworkClient {
    private val client: OkHttpClient = OkHttpClient.Builder()
        .certificatePinner(CertificatePinnerConfig.certificatePinner) // Áp dụng pinner ở đây
        .build()

    // Sử dụng client này để thực hiện các HTTP requests
    fun makeApiCall() {
        // ...
    }
}
*/