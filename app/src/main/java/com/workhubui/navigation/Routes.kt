//package com.workhubui.navigation
//
//object Routes {
//    const val SPLASH = "Splash"
//    const val LOGIN = "Login"
//    const val SIGNUP = "Signup"
//    const val HOME = "Home"
//    const val SCHEDULE = "Schedule"
//
//    // Sử dụng một tên hằng số cho màn hình danh sách các cuộc trò chuyện
//    const val CHAT_LIST = "ChatList"
//
//    // Sử dụng một tên hằng số khác (hoặc giữ nguyên CHAT nếu bạn đã quen)
//    // cho màn hình chi tiết một cuộc trò chuyện cụ thể.
//    // Để phân biệt rõ ràng, có thể đặt là CHAT_THREAD hoặc CHAT_DETAIL.
//    // Trong các ví dụ trước, chúng ta đã dùng Routes.CHAT cho màn hình chi tiết.
//    // Hãy giữ nguyên nó cho màn hình chi tiết để ít thay đổi các phần khác.
//    const val CHAT = "Chat" // Sẽ được dùng cho route có dạng "Chat/{currentUser}/{chatWith}"
//
//    const val TASKBOARD = "Taskboard"
//    const val VAULT = "Vault"
//    const val SETTINGS = "settings"
//}
package com.workhubui.navigation

object Routes {
    const val SPLASH = "Splash"
    const val LOGIN = "Login"
    const val SIGNUP = "Signup"
    const val HOME = "Home"
    const val SCHEDULE = "Schedule" // Màn hình Schedule cũ (sẽ được tích hợp vào Home)

    const val CHAT_LIST = "ChatList" // Màn hình danh sách các cuộc trò chuyện
    const val CHAT = "Chat" // Màn hình chi tiết một cuộc trò chuyện cụ thể (dùng với tham số)

    const val TASKBOARD = "Taskboard" // Màn hình Taskboard cũ (sẽ được tích hợp vào Home)
    const val VAULT = "Vault" // Màn hình Vault

    const val SETTINGS = "Settings" // Màn hình cài đặt
    const val PROFILE = "Profile" // Màn hình hồ sơ cá nhân
    const val ADD_FRIEND = "AddFriend" // Màn hình thêm bạn bè
}