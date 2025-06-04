package com.workhubui.navigation

object Routes {
    const val SPLASH = "Splash"
    const val LOGIN = "Login"
    const val SIGNUP = "Signup"
    const val HOME = "Home" // Màn hình chính sau khi đăng nhập

    // Chat
    const val CHAT_LIST = "ChatList" // Danh sách các cuộc trò chuyện
    const val CHAT = "Chat" // Màn hình chi tiết cuộc trò chuyện (ví dụ: "Chat/{currentUser}/{chatWith}")
    const val ADD_FRIEND = "AddFriend" // Màn hình thêm bạn bè

    // Vault
    const val VAULT = "Vault"

    // Settings and Profile
    const val SETTINGS = "Settings" // Màn hình cài đặt
    const val PROFILE = "Profile" // Màn hình hồ sơ cá nhân

// Các màn hình cũ có thể đã được tích hợp hoặc loại bỏ
// const val SCHEDULE = "Schedule"
// const val TASKBOARD = "Taskboard"


}
