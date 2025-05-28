package com.workhubui.navigation

object Routes {
    const val SPLASH = "Splash"
    const val LOGIN = "Login"
    const val SIGNUP = "Signup"
    const val HOME = "Home"
    const val SCHEDULE = "Schedule"

    // Sử dụng một tên hằng số cho màn hình danh sách các cuộc trò chuyện
    const val CHAT_LIST = "ChatList"

    // Sử dụng một tên hằng số khác (hoặc giữ nguyên CHAT nếu bạn đã quen)
    // cho màn hình chi tiết một cuộc trò chuyện cụ thể.
    // Để phân biệt rõ ràng, có thể đặt là CHAT_THREAD hoặc CHAT_DETAIL.
    // Trong các ví dụ trước, chúng ta đã dùng Routes.CHAT cho màn hình chi tiết.
    // Hãy giữ nguyên nó cho màn hình chi tiết để ít thay đổi các phần khác.
    const val CHAT = "Chat" // Sẽ được dùng cho route có dạng "Chat/{currentUser}/{chatWith}"

    const val TASKBOARD = "Taskboard"
    const val VAULT = "Vault"
    const val SETTINGS = "settings"
}