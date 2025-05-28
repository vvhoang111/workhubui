package com.workhubui.data.repository

import com.workhubui.data.local.dao.ScheduleDao
import com.workhubui.data.local.entity.ScheduleItemEntity
import kotlinx.coroutines.flow.Flow

class ScheduleRepository(private val dao: ScheduleDao) {
    val schedules: Flow<List<ScheduleItemEntity>> = dao.getAllSchedules()

    suspend fun addSchedule(item: ScheduleItemEntity) = dao.insertSchedule(item)

    suspend fun deleteSchedule(id: Int) = dao.deleteSchedule(id)
}
