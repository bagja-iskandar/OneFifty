package com.example.onefifty.repository

import com.example.onefifty.ui.model.ActivityType
import com.example.onefifty.ui.model.WorkoutSessionItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class HistoricalWeekData(
    val weekOffset: Int,          // 0 for current, -1 for previous week, etc.
    val weekNumber: Int,
    val year: Int,
    val dateRange: String,
    val sessions: List<WorkoutSessionItem>,
    val targetMinutes: Int = 150,
    val mondayMillis: Long = 0L
) {
    val totalLoggedSeconds: Int get() = sessions.sumOf { it.durationSeconds }
    val totalLoggedMinutes: Int get() = totalLoggedSeconds / 60
}

// Backward compatibility alias
typealias WeekHistoryDto = HistoricalWeekData

data class ActiveSessionState(
    val activityType: ActivityType,
    val startTimestamp: Long,
    val sessionAccumulatedSeconds: Int
)

interface WorkoutRepository {
    val weeklySessions: StateFlow<List<WorkoutSessionItem>>
    fun checkMondayReset()
    fun addSession(
        activityType: ActivityType,
        durationSeconds: Int,
        dateString: String,
        dayOfWeek: String,
        formattedTime: String = "Now"
    )
    fun deleteSession(sessionId: String)
    fun getHistoricalWeeks(targetMinutes: Int = 150): List<HistoricalWeekData>
    fun getSessionsForWeekOffset(weekOffset: Int): List<WorkoutSessionItem>
    fun clearCurrentWeekData()
    fun clearAllData()
    fun saveActiveSessionState(activityType: ActivityType, startTimestamp: Long, sessionAccumulatedSeconds: Int)
    fun getActiveSessionState(): ActiveSessionState?
    fun clearActiveSessionState()
    suspend fun syncWithSupabase()
}

class InMemoryWorkoutRepository : WorkoutRepository {

    private val _weeklySessions = MutableStateFlow<List<WorkoutSessionItem>>(emptyList())
    override val weeklySessions: StateFlow<List<WorkoutSessionItem>> = _weeklySessions.asStateFlow()
    private val historicalWeeksList = mutableListOf<HistoricalWeekData>()
    private var activeSessionState: ActiveSessionState? = null

    override fun checkMondayReset() {
        // No-op for in-memory testing
    }

    override fun addSession(
        activityType: ActivityType,
        durationSeconds: Int,
        dateString: String,
        dayOfWeek: String,
        formattedTime: String
    ) {
        if (durationSeconds <= 0) return
        val current = _weeklySessions.value.toMutableList()
        val newItem = WorkoutSessionItem(
            id = "sess_${System.currentTimeMillis()}",
            dateString = dateString,
            dayOfWeek = dayOfWeek,
            activityType = activityType,
            durationSeconds = durationSeconds,
            formattedTime = formattedTime
        )
        current.add(newItem)
        _weeklySessions.value = current
    }

    override fun deleteSession(sessionId: String) {
        val current = _weeklySessions.value.toMutableList()
        val removed = current.removeAll { it.id == sessionId }
        if (removed) {
            _weeklySessions.value = current
        }
    }

    override fun getHistoricalWeeks(targetMinutes: Int): List<HistoricalWeekData> {
        return historicalWeeksList.toList()
    }

    override fun getSessionsForWeekOffset(weekOffset: Int): List<WorkoutSessionItem> {
        return if (weekOffset == 0) {
            _weeklySessions.value
        } else {
            historicalWeeksList.find { it.weekOffset == weekOffset }?.sessions ?: emptyList()
        }
    }

    override fun clearCurrentWeekData() {
        _weeklySessions.value = emptyList()
        activeSessionState = null
    }

    override fun clearAllData() {
        _weeklySessions.value = emptyList()
        historicalWeeksList.clear()
        activeSessionState = null
    }

    override fun saveActiveSessionState(
        activityType: ActivityType,
        startTimestamp: Long,
        sessionAccumulatedSeconds: Int
    ) {
        activeSessionState = ActiveSessionState(activityType, startTimestamp, sessionAccumulatedSeconds)
    }

    override fun getActiveSessionState(): ActiveSessionState? {
        return activeSessionState
    }

    override fun clearActiveSessionState() {
        activeSessionState = null
    }

    override suspend fun syncWithSupabase() {
        // Local-only mode
    }
}
