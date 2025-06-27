//package com.workhubui.screens.home
//
//import android.app.Application
//import android.os.Build
//import android.util.Log
//import androidx.annotation.RequiresApi
//import androidx.lifecycle.AndroidViewModel
//import androidx.lifecycle.viewModelScope
//import com.workhubui.data.local.entity.ScheduleItemEntity // THAY ĐỔI: Import ScheduleItemEntity
//import com.workhubui.data.repository.ChatRepository
//import com.workhubui.model.ChatMessage
//// import com.workhubui.model.ScheduleItem // XÓA: Không cần ScheduleItem đơn giản nữa
//import com.workhubui.screens.Meeting // THÊM: Import Meeting data class
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.flow.MutableStateFlow
//import kotlinx.coroutines.flow.StateFlow
//import kotlinx.coroutines.launch
//import kotlinx.coroutines.withContext
//import org.json.JSONArray
//import java.io.BufferedReader
//import java.text.SimpleDateFormat
//import java.time.LocalDate // THÊM: Cho Lịch mini
//import java.time.LocalTime // THÊM: Cho Lịch mini
//import java.util.Calendar
//import java.util.Locale
//
//@RequiresApi(Build.VERSION_CODES.O) // THÊM: Cần cho LocalDate/LocalTime
//class HomeViewModel(
//    application: Application,
//    private val chatRepository: ChatRepository
//) : AndroidViewModel(application) {
//
//    // THAY ĐỔI: Sử dụng ScheduleItemEntity cho _scheduleList
//    private val _scheduleList = MutableStateFlow<List<ScheduleItemEntity>>(emptyList())
//    val scheduleList: StateFlow<List<ScheduleItemEntity>> = _scheduleList
//
//    private val _recentChats = MutableStateFlow<List<ChatMessage>>(emptyList())
//    val recentChats: StateFlow<List<ChatMessage>> = _recentChats
//
//    // THÊM: StateFlow cho Lịch mini và ngày được chọn
//    private val _upcomingMeetings = MutableStateFlow<List<Meeting>>(emptyList())
//    val upcomingMeetings: StateFlow<List<Meeting>> = _upcomingMeetings
//
//    private val _selectedDateForCalendar = MutableStateFlow(LocalDate.now())
//    val selectedDateForCalendar: StateFlow<LocalDate> = _selectedDateForCalendar
//
//    init {
//        loadScheduleFromAssets()
//        loadRecentChats()
//        loadUpcomingMeetings() // THÊM: Gọi hàm tải dữ liệu cho lịch mini
//    }
//
//    // THAY ĐỔI: Hàm này giờ sẽ parse thành ScheduleItemEntity
//    fun loadScheduleFromAssets() {
//        viewModelScope.launch {
//            try {
//                val context = getApplication<Application>().applicationContext
//                val jsonText = withContext(Dispatchers.IO) {
//                    context.assets.open("schedule.json")
//                        .bufferedReader().use(BufferedReader::readText)
//                }
//                val arr = JSONArray(jsonText)
//                val list = mutableListOf<ScheduleItemEntity>()
//                val defaultDurationMillis = 60 * 60 * 1000 // Mặc định 1 giờ
//
//                for (i in 0 until arr.length()) {
//                    val obj = arr.getJSONObject(i)
//                    val title = obj.getString("title")
//                    val timeStr = obj.getString("time") // Ví dụ: "7:00 AM"
//
//                    val startTime = parseTimeToTodayMillis(timeStr)
//                    val endTime = startTime + defaultDurationMillis // Giả sử lịch trình kéo dài 1 giờ
//
//                    list += ScheduleItemEntity(
//                        // id có thể không quá quan trọng nếu chỉ load từ asset và không lưu DB ở đây
//                        id = i, // Hoặc dùng một cơ chế tạo ID ổn định hơn nếu cần
//                        title = title,
//                        description = "Lịch trình tải từ assets.", // Mô tả mẫu
//                        startTime = startTime,
//                        endTime = endTime
//                    )
//                }
//                _scheduleList.value = list.sortedBy { it.startTime }
//            } catch (e: Exception) {
//                Log.e("HomeViewModel", "Lỗi khi tải lịch trình từ assets", e)
//                _scheduleList.value = emptyList() // Đặt danh sách rỗng nếu có lỗi
//            }
//        }
//    }
//
//    fun loadRecentChats() {
//        viewModelScope.launch {
//            // Giả sử getRecentMessages là suspend function và đã xử lý IO dispatcher bên trong repository
//            _recentChats.value = chatRepository.getRecentMessages()
//        }
//    }
//
//    // THÊM: Hàm tải dữ liệu cho Lịch mini (Upcoming Meetings)
//    @RequiresApi(Build.VERSION_CODES.O)
//    fun loadUpcomingMeetings() {
//        // Đây là dữ liệu mẫu, trong ứng dụng thực tế bạn sẽ lấy từ repository/database
//        val sampleMeetings = listOf(
//            Meeting("Team Sync", LocalDate.now().plusDays(1), LocalTime.of(10, 0)),
//            Meeting("Project Alpha Review", LocalDate.now().plusDays(2), LocalTime.of(14, 30)),
//            Meeting("Client Call", LocalDate.now().plusDays(2), LocalTime.of(16, 0)), // Thêm cuộc họp cùng ngày
//            Meeting("Design Sprint Planning", LocalDate.now().plusWeeks(1), LocalTime.of(9, 0))
//        )
//        _upcomingMeetings.value = sampleMeetings
//    }
//
//    // THÊM: Hàm xử lý khi người dùng chọn một ngày trên Lịch mini
//    @RequiresApi(Build.VERSION_CODES.O)
//    fun onDateSelectedInCalendar(date: LocalDate) {
//        _selectedDateForCalendar.value = date
//        // Bạn có thể thêm logic ở đây, ví dụ: lọc danh sách _upcomingMeetings
//        // để chỉ hiển thị các cuộc họp của ngày được chọn.
//    }
//
//    // THÊM: Hàm tiện ích để parse chuỗi thời gian từ JSON (ví dụ "7:00 AM") thành milliseconds cho ngày hôm nay
//    private fun parseTimeToTodayMillis(timeStr: String): Long {
//        // Định dạng này xử lý các chuỗi như "7:00 AM", "2:00 PM"
//        val sdf = SimpleDateFormat("h:mm a", Locale.US) // Sử dụng Locale.US để đảm bảo parse AM/PM đúng
//        val todayCalendar = Calendar.getInstance() // Lấy ngày giờ hiện tại
//
//        try {
//            val parsedDate = sdf.parse(timeStr)
//            if (parsedDate != null) {
//                val itemCalendar = Calendar.getInstance()
//                itemCalendar.time = parsedDate // Calendar này giờ chứa giờ và phút từ timeStr
//
//                // Đặt giờ và phút từ parsedDate vào calendar của ngày hôm nay
//                todayCalendar.set(Calendar.HOUR_OF_DAY, itemCalendar.get(Calendar.HOUR_OF_DAY))
//                todayCalendar.set(Calendar.MINUTE, itemCalendar.get(Calendar.MINUTE))
//                todayCalendar.set(Calendar.SECOND, 0)
//                todayCalendar.set(Calendar.MILLISECOND, 0)
//            } else {
//                // Xử lý trường hợp parse thất bại, ví dụ, log lỗi và trả về thời gian hiện tại
//                Log.w("HomeViewModel", "Không thể parse chuỗi thời gian: $timeStr. Sử dụng thời gian hiện tại.")
//            }
//        } catch (e: Exception) {
//            Log.e("HomeViewModel", "Lỗi khi parse thời gian: $timeStr", e)
//            // Có thể trả về thời gian hiện tại hoặc một giá trị mặc định khác
//        }
//        return todayCalendar.timeInMillis
//    }
//}

package com.cdcs.screens.home

import android.app.Application
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cdcs.data.local.entity.ScheduleItemEntity
import com.cdcs.data.repository.ChatRepository
import com.cdcs.model.ChatMessage
import com.cdcs.screens.Meeting
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.BufferedReader
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalTime
import java.util.Calendar
import java.util.Locale

@RequiresApi(Build.VERSION_CODES.O)
class HomeViewModel(
    application: Application,
    private val chatRepository: ChatRepository
) : AndroidViewModel(application) {

    private val _scheduleList = MutableStateFlow<List<ScheduleItemEntity>>(emptyList())
    val scheduleList: StateFlow<List<ScheduleItemEntity>> = _scheduleList

    private val _recentChats = MutableStateFlow<List<ChatMessage>>(emptyList())
    val recentChats: StateFlow<List<ChatMessage>> = _recentChats

    private val _upcomingMeetings = MutableStateFlow<List<Meeting>>(emptyList())
    val upcomingMeetings: StateFlow<List<Meeting>> = _upcomingMeetings

    private val _selectedDateForCalendar = MutableStateFlow(LocalDate.now())
    val selectedDateForCalendar: StateFlow<LocalDate> = _selectedDateForCalendar

    init {
        loadScheduleFromAssets()
        loadRecentChats()
        loadUpcomingMeetings()
    }

    // Tải lịch trình từ assets (schedule.json)
    fun loadScheduleFromAssets() {
        viewModelScope.launch {
            try {
                val context = getApplication<Application>().applicationContext
                val jsonText = withContext(Dispatchers.IO) {
                    context.assets.open("schedule.json")
                        .bufferedReader().use(BufferedReader::readText)
                }
                val arr = JSONArray(jsonText)
                val list = mutableListOf<ScheduleItemEntity>()
                val defaultDurationMillis = 60 * 60 * 1000 // Mặc định 1 giờ

                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val title = obj.getString("title")
                    val detail = obj.optString("detail", "") // Đọc trường 'detail'
                    val timeStr = obj.getString("time")

                    val startTime = parseTimeToTodayMillis(timeStr)
                    val endTime = startTime + defaultDurationMillis

                    list += ScheduleItemEntity(
                        id = i,
                        title = title,
                        detail = detail, // Gán giá trị cho detail
                        startTime = startTime,
                        endTime = endTime
                    )
                }
                _scheduleList.value = list.sortedBy { it.startTime }
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Lỗi khi tải lịch trình từ assets", e)
                _scheduleList.value = emptyList()
            }
        }
    }

    // Tải các cuộc trò chuyện gần đây
    fun loadRecentChats() {
        viewModelScope.launch {
            _recentChats.value = chatRepository.getRecentMessages()
        }
    }

    // Tải dữ liệu cho Lịch mini (Upcoming Meetings)
    @RequiresApi(Build.VERSION_CODES.O)
    fun loadUpcomingMeetings() {
        // Đây là dữ liệu mẫu, trong ứng dụng thực tế bạn sẽ lấy từ repository/database
        val sampleMeetings = listOf(
            Meeting("Team Sync", LocalDate.now().plusDays(1), LocalTime.of(10, 0)),
            Meeting("Project Alpha Review", LocalDate.now().plusDays(2), LocalTime.of(14, 30)),
            Meeting("Client Call", LocalDate.now().plusDays(2), LocalTime.of(16, 0)),
            Meeting("Design Sprint Planning", LocalDate.now().plusWeeks(1), LocalTime.of(9, 0))
        )
        _upcomingMeetings.value = sampleMeetings
    }

    // Xử lý khi người dùng chọn một ngày trên Lịch mini
    @RequiresApi(Build.VERSION_CODES.O)
    fun onDateSelectedInCalendar(date: LocalDate) {
        _selectedDateForCalendar.value = date
        // Logic lọc lịch trình theo ngày được chọn sẽ được thực hiện trong Composable (HomeScreen)
    }

    // Hàm tiện ích để parse chuỗi thời gian từ JSON thành milliseconds cho ngày hôm nay
    private fun parseTimeToTodayMillis(timeStr: String): Long {
        val sdf = SimpleDateFormat("h:mm a", Locale.US)
        val todayCalendar = Calendar.getInstance()

        try {
            val parsedDate = sdf.parse(timeStr)
            if (parsedDate != null) {
                val itemCalendar = Calendar.getInstance()
                itemCalendar.time = parsedDate

                todayCalendar.set(Calendar.HOUR_OF_DAY, itemCalendar.get(Calendar.HOUR_OF_DAY))
                todayCalendar.set(Calendar.MINUTE, itemCalendar.get(Calendar.MINUTE))
                todayCalendar.set(Calendar.SECOND, 0)
                todayCalendar.set(Calendar.MILLISECOND, 0)
            } else {
                Log.w("HomeViewModel", "Không thể parse chuỗi thời gian: $timeStr. Sử dụng thời gian hiện tại.")
            }
        } catch (e: Exception) {
            Log.e("HomeViewModel", "Lỗi khi parse thời gian: $timeStr", e)
        }
        return todayCalendar.timeInMillis
    }
}