package com.cdcs.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.cdcs.data.local.entity.ScheduleItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ScheduleDao {
    @Query("SELECT * FROM schedule ORDER BY startTime ASC")
    fun getAllSchedules(): Flow<List<ScheduleItemEntity>>

    @Insert
    suspend fun insertSchedule(item: ScheduleItemEntity)

    @Query("DELETE FROM schedule WHERE id = :id")
    suspend fun deleteSchedule(id: Int)
}
