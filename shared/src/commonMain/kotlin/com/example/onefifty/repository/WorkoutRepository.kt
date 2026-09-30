package com.example.onefifty.repository

import com.example.onefifty.ui.model.ActivityType
import com.example.onefifty.ui.model.WorkoutSessionItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

interface WorkoutRepository {
    val weeklySessions: StateFlow<List<WorkoutSessionItem>>
    fun addSession(activityType: ActivityType, durationSeconds: Int, dateString: String, dayOfWeek: String)
    fun getHistoricalWeeks(targetMinutes: Int = 150): List<WeekHistoryDto>
    fun clearAllData()
    suspend fun syncWithSupabase()
}

data class WeekHistoryDto(
    val weekNumber: Int,
    val dateRange: String,
    val loggedMinutes: Int,
    val targetMinutes: Int = 150
)

class InMemoryWorkoutRepository : WorkoutRepository {

    private val _weeklySessions = MutableStateFlow<List<WorkoutSessionItem>>(emptyList())
    override val weeklySessions: StateFlow<List<WorkoutSessionItem>> = _weeklySessions.asStateFlow()

    override fun addSession(activityType: ActivityType, durationSeconds: Int, dateString: String, dayOfWeek: String) {
        val current = _weeklySessions.value.toMutableList()
        val newItem = WorkoutSessionItem(
            id = "sess_${System.currentTimeMillis()}",
            dateString = dateString,
            dayOfWeek = dayOfWeek,
            activityType = activityType,
            durationSeconds = durationSeconds,
            formattedTime = "Now"
        )
        current.add(newItem)
        _weeklySessions.value = current
    }

    override fun getHistoricalWeeks(targetMinutes: Int): List<WeekHistoryDto> {
        return emptyList()
    }

    override fun clearAllData() {
        _weeklySessions.value = emptyList()
    }

    override suspend fun syncWithSupabase() {
        // Local-only mode
    }
}
