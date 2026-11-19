package com.example.onefifty.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.onefifty.ui.model.ActivityType
import com.example.onefifty.ui.model.WorkoutSessionItem
import com.example.onefifty.util.DateUtils
import com.example.onefifty.widget.OneFiftyWidgetProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar
import java.util.concurrent.atomic.AtomicInteger

class LocalWorkoutRepository(private val context: Context) : WorkoutRepository {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val _weeklySessions = MutableStateFlow<List<WorkoutSessionItem>>(emptyList())
    override val weeklySessions: StateFlow<List<WorkoutSessionItem>> = _weeklySessions.asStateFlow()
    private val sessionIdCounter = AtomicInteger(0)

    init {
        checkMondayReset()
        loadCurrentSessions()
        syncWidget()
    }

    override fun checkMondayReset() {
        val currentMondayMillis = DateUtils.getWeekMonday(0).timeInMillis
        val lastMondayMillis = prefs.getLong(KEY_LAST_MONDAY_MILLIS, -1L)

        if (lastMondayMillis != -1L && currentMondayMillis > lastMondayMillis) {
            // New Monday 00:00:00 reached! Archive previous active week
            val previousSessions = loadSessionsFromJson(prefs.getString(KEY_SESSIONS, null))
            if (previousSessions.isNotEmpty()) {
                archiveWeek(lastMondayMillis, previousSessions)
            }

            // Reset current week sessions
            _weeklySessions.value = emptyList()
            saveSessionsToPrefs(emptyList())

            prefs.edit()
                .putLong(KEY_LAST_MONDAY_MILLIS, currentMondayMillis)
                .apply()
        } else if (lastMondayMillis == -1L) {
            prefs.edit()
                .putLong(KEY_LAST_MONDAY_MILLIS, currentMondayMillis)
                .apply()
        }
    }

    private fun archiveWeek(mondayMillis: Long, sessions: List<WorkoutSessionItem>) {
        try {
            val currentMondayMillis = DateUtils.getWeekMonday(0).timeInMillis
            val diffWeeks = ((currentMondayMillis - mondayMillis) / (7L * 24 * 3600 * 1000)).toInt()
            val weekOffset = -diffWeeks.coerceAtLeast(1)

            val weekData = buildHistoricalWeekData(mondayMillis, sessions, weekOffset)
            val existingHistory = loadHistoricalWeeksFromPrefs().toMutableList()

            // Remove any existing entry for this weekNumber and year to avoid duplicates
            existingHistory.removeAll { it.weekNumber == weekData.weekNumber && it.year == weekData.year }
            existingHistory.add(0, weekData) // Most recent first

            saveHistoricalWeeksToPrefs(existingHistory)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun buildHistoricalWeekData(
        mondayMillis: Long,
        sessions: List<WorkoutSessionItem>,
        weekOffset: Int
    ): HistoricalWeekData {
        val cal = Calendar.getInstance().apply { timeInMillis = mondayMillis }
        val weekNum = DateUtils.getIsoWeekNumber(cal)
        val year = cal.get(Calendar.YEAR)

        val sunday = (cal.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, 6) }
        val startDay = cal.get(Calendar.DAY_OF_MONTH)
        val endDay = sunday.get(Calendar.DAY_OF_MONTH)
        val startMonth = cal.get(Calendar.MONTH) + 1
        val endMonth = sunday.get(Calendar.MONTH) + 1
        val rangeStr = "$startDay/$startMonth – $endDay/$endMonth, $year"

        return HistoricalWeekData(
            weekOffset = weekOffset,
            weekNumber = weekNum,
            year = year,
            dateRange = rangeStr,
            sessions = sessions,
            targetMinutes = 150,
            mondayMillis = mondayMillis
        )
    }

    private fun loadCurrentSessions() {
        _weeklySessions.value = loadSessionsFromJson(prefs.getString(KEY_SESSIONS, null))
    }

    private fun loadSessionsFromJson(jsonString: String?): List<WorkoutSessionItem> {
        if (jsonString.isNullOrEmpty()) return emptyList()
        return try {
            val array = JSONArray(jsonString)
            val list = mutableListOf<WorkoutSessionItem>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val id = obj.getString("id")
                val dateString = obj.optString("dateString", "")
                val dayOfWeek = obj.getString("dayOfWeek")
                val activityTypeName = obj.getString("activityType")
                val durationSeconds = if (obj.has("durationSeconds")) {
                    obj.getInt("durationSeconds")
                } else {
                    obj.optInt("durationMinutes", 0) * 60
                }
                val formattedTime = obj.optString("formattedTime", "00:00")

                val activityType = try {
                    ActivityType.valueOf(activityTypeName)
                } catch (e: Exception) {
                    ActivityType.OTHER
                }

                list.add(WorkoutSessionItem(id, dateString, dayOfWeek, activityType, durationSeconds, formattedTime))
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    override fun addSession(
        activityType: ActivityType,
        durationSeconds: Int,
        dateString: String,
        dayOfWeek: String,
        formattedTime: String
    ) {
        if (durationSeconds <= 0) return

        val newItem = WorkoutSessionItem(
            id = "sess_${System.currentTimeMillis()}_${sessionIdCounter.incrementAndGet()}",
            dateString = dateString,
            dayOfWeek = dayOfWeek,
            activityType = activityType,
            durationSeconds = durationSeconds,
            formattedTime = formattedTime
        )

        val currentWeekDates = DateUtils.getWeekDailyDates(0).map { it.third }
        if (currentWeekDates.contains(dateString) || dateString.isEmpty()) {
            // Add to current active week
            val current = _weeklySessions.value.toMutableList()
            current.add(newItem)
            _weeklySessions.value = current
            saveSessionsToPrefs(current)
            syncWidget()
        } else {
            // Check if it belongs to any existing historical week
            val history = loadHistoricalWeeksFromPrefs().toMutableList()
            var matchedIndex = -1
            for (i in history.indices) {
                val week = history[i]
                val weekDates = DateUtils.getWeekDailyDates(week.weekOffset).map { it.third }
                if (weekDates.contains(dateString)) {
                    matchedIndex = i
                    break
                }
            }

            if (matchedIndex >= 0) {
                val targetWeek = history[matchedIndex]
                val updatedSessions = targetWeek.sessions.toMutableList().apply { add(newItem) }
                history[matchedIndex] = targetWeek.copy(sessions = updatedSessions)
                saveHistoricalWeeksToPrefs(history)
            } else {
                val pastOffset = DateUtils.findWeekOffsetForIsoDate(dateString)
                if (pastOffset != null && pastOffset < 0) {
                    val pastMondayMillis = DateUtils.getWeekMonday(pastOffset).timeInMillis
                    val newArchiveWeek = buildHistoricalWeekData(pastMondayMillis, listOf(newItem), pastOffset)
                    history.add(newArchiveWeek)
                    saveHistoricalWeeksToPrefs(history)
                } else {
                    // Fallback to active week
                    val current = _weeklySessions.value.toMutableList()
                    current.add(newItem)
                    _weeklySessions.value = current
                    saveSessionsToPrefs(current)
                    syncWidget()
                }
            }
        }
    }

    override fun deleteSession(sessionId: String) {
        // 1. Try deleting from current active week
        val current = _weeklySessions.value.toMutableList()
        val removedFromCurrent = current.removeAll { it.id == sessionId }
        if (removedFromCurrent) {
            _weeklySessions.value = current
            saveSessionsToPrefs(current)
            syncWidget()
            return
        }

        // 2. Try deleting from historical weeks
        val history = loadHistoricalWeeksFromPrefs().toMutableList()
        var modified = false
        for (i in history.indices) {
            val week = history[i]
            val sessionList = week.sessions.toMutableList()
            if (sessionList.removeAll { it.id == sessionId }) {
                history[i] = week.copy(sessions = sessionList)
                modified = true
                break
            }
        }
        if (modified) {
            saveHistoricalWeeksToPrefs(history)
        }
    }

    override fun getHistoricalWeeks(targetMinutes: Int): List<HistoricalWeekData> {
        val archived = loadHistoricalWeeksFromPrefs()
        val currentWeekMonday = DateUtils.getWeekMonday(0)
        val currentWeekNum = DateUtils.getIsoWeekNumber(currentWeekMonday)
        val currentYear = currentWeekMonday.get(Calendar.YEAR)
        val currentDateRange = DateUtils.getWeekDateRangeString(0)

        val currentWeekData = HistoricalWeekData(
            weekOffset = 0,
            weekNumber = currentWeekNum,
            year = currentYear,
            dateRange = currentDateRange,
            sessions = _weeklySessions.value,
            targetMinutes = targetMinutes,
            mondayMillis = currentWeekMonday.timeInMillis
        )

        val allWeeks = mutableListOf<HistoricalWeekData>()
        allWeeks.add(currentWeekData)
        allWeeks.addAll(archived)
        return allWeeks
    }

    override fun getSessionsForWeekOffset(weekOffset: Int): List<WorkoutSessionItem> {
        if (weekOffset == 0) {
            return _weeklySessions.value
        }
        val history = loadHistoricalWeeksFromPrefs()
        return history.find { it.weekOffset == weekOffset }?.sessions ?: emptyList()
    }

    override fun clearCurrentWeekData() {
        _weeklySessions.value = emptyList()
        saveSessionsToPrefs(emptyList())
        clearActiveSessionState()
        syncWidget()
    }

    override fun clearAllData() {
        prefs.edit().clear().apply()
        cachedHistoricalWeeks = emptyList()
        _weeklySessions.value = emptyList()
        clearActiveSessionState()
        syncWidget()
    }

    override fun saveActiveSessionState(
        activityType: ActivityType,
        startTimestamp: Long,
        sessionAccumulatedSeconds: Int
    ) {
        prefs.edit()
            .putString(KEY_ACTIVE_SESSION_ACTIVITY, activityType.name)
            .putLong(KEY_ACTIVE_SESSION_START_TIME, startTimestamp)
            .putInt(KEY_ACTIVE_SESSION_ACC_SECS, sessionAccumulatedSeconds)
            .apply()
    }

    override fun getActiveSessionState(): ActiveSessionState? {
        val startTimestamp = prefs.getLong(KEY_ACTIVE_SESSION_START_TIME, 0L)
        if (startTimestamp <= 0L) return null
        val activityName = prefs.getString(KEY_ACTIVE_SESSION_ACTIVITY, null) ?: return null
        val activity = try {
            ActivityType.valueOf(activityName)
        } catch (e: Exception) {
            return null
        }
        val accSecs = prefs.getInt(KEY_ACTIVE_SESSION_ACC_SECS, 0)
        return ActiveSessionState(activity, startTimestamp, accSecs)
    }

    override fun clearActiveSessionState() {
        prefs.edit()
            .remove(KEY_ACTIVE_SESSION_ACTIVITY)
            .remove(KEY_ACTIVE_SESSION_START_TIME)
            .remove(KEY_ACTIVE_SESSION_ACC_SECS)
            .apply()
    }

    override suspend fun syncWithSupabase() {
        // Local-only mode
    }

    private fun syncWidget() {
        val totalSecs = _weeklySessions.value.sumOf { it.durationSeconds }
        OneFiftyWidgetProvider.updateAllWidgets(context, totalSecs / 60, 150)
    }

    private fun saveSessionsToPrefs(sessions: List<WorkoutSessionItem>) {
        try {
            val array = JSONArray()
            for (item in sessions) {
                val obj = JSONObject()
                obj.put("id", item.id)
                obj.put("dateString", item.dateString)
                obj.put("dayOfWeek", item.dayOfWeek)
                obj.put("activityType", item.activityType.name)
                obj.put("durationSeconds", item.durationSeconds)
                obj.put("formattedTime", item.formattedTime)
                array.put(obj)
            }
            prefs.edit().putString(KEY_SESSIONS, array.toString()).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private var cachedHistoricalWeeks: List<HistoricalWeekData>? = null

    private fun loadHistoricalWeeksFromPrefs(): List<HistoricalWeekData> {
        cachedHistoricalWeeks?.let { return it }
        val jsonStr = prefs.getString(KEY_HISTORICAL_WEEKS, null) ?: return emptyList<HistoricalWeekData>().also {
            cachedHistoricalWeeks = it
        }
        return try {
            val array = JSONArray(jsonStr)
            val list = mutableListOf<HistoricalWeekData>()
            val currentMondayMillis = DateUtils.getWeekMonday(0).timeInMillis

            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val storedMondayMillis = obj.optLong("mondayMillis", 0L)
                val storedOffset = obj.optInt("weekOffset", -1)

                // Dynamically compute offset relative to today's Monday
                val computedOffset = if (storedMondayMillis > 0L) {
                    val diffWeeks = ((currentMondayMillis - storedMondayMillis) / (7L * 24 * 3600 * 1000)).toInt()
                    -diffWeeks.coerceAtLeast(1)
                } else {
                    storedOffset
                }

                val weekNum = obj.getInt("weekNumber")
                val year = obj.getInt("year")
                val dateRange = obj.getString("dateRange")
                val targetMinutes = obj.optInt("targetMinutes", 150)
                val sessionsArray = obj.getJSONArray("sessions")
                val sessions = mutableListOf<WorkoutSessionItem>()

                for (j in 0 until sessionsArray.length()) {
                    val sObj = sessionsArray.getJSONObject(j)
                    val actType = try {
                        ActivityType.valueOf(sObj.getString("activityType"))
                    } catch (e: Exception) {
                        ActivityType.OTHER
                    }
                    sessions.add(
                        WorkoutSessionItem(
                            id = sObj.getString("id"),
                            dateString = sObj.optString("dateString", ""),
                            dayOfWeek = sObj.getString("dayOfWeek"),
                            activityType = actType,
                            durationSeconds = sObj.getInt("durationSeconds"),
                            formattedTime = sObj.optString("formattedTime", "")
                        )
                    )
                }

                list.add(HistoricalWeekData(computedOffset, weekNum, year, dateRange, sessions, targetMinutes, storedMondayMillis))
            }
            cachedHistoricalWeeks = list
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun saveHistoricalWeeksToPrefs(history: List<HistoricalWeekData>) {
        cachedHistoricalWeeks = history
        try {
            val array = JSONArray()
            for (week in history) {
                val obj = JSONObject()
                obj.put("weekOffset", week.weekOffset)
                obj.put("weekNumber", week.weekNumber)
                obj.put("year", week.year)
                obj.put("dateRange", week.dateRange)
                obj.put("targetMinutes", week.targetMinutes)
                obj.put("mondayMillis", week.mondayMillis)

                val sArray = JSONArray()
                for (s in week.sessions) {
                    val sObj = JSONObject()
                    sObj.put("id", s.id)
                    sObj.put("dateString", s.dateString)
                    sObj.put("dayOfWeek", s.dayOfWeek)
                    sObj.put("activityType", s.activityType.name)
                    sObj.put("durationSeconds", s.durationSeconds)
                    sObj.put("formattedTime", s.formattedTime)
                    sArray.put(sObj)
                }
                obj.put("sessions", sArray)
                array.put(obj)
            }
            prefs.edit().putString(KEY_HISTORICAL_WEEKS, array.toString()).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    companion object {
        private const val PREFS_NAME = "tempo150_local_prefs"
        private const val KEY_SESSIONS = "key_weekly_sessions"
        private const val KEY_LAST_MONDAY_MILLIS = "key_last_monday_millis"
        private const val KEY_HISTORICAL_WEEKS = "key_historical_weeks"
        private const val KEY_ACTIVE_SESSION_ACTIVITY = "key_active_session_activity"
        private const val KEY_ACTIVE_SESSION_START_TIME = "key_active_session_start_time"
        private const val KEY_ACTIVE_SESSION_ACC_SECS = "key_active_session_acc_secs"
    }
}
