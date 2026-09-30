package com.example.onefifty.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.onefifty.ui.model.ActivityType
import com.example.onefifty.ui.model.WorkoutSessionItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar

class LocalWorkoutRepository(context: Context) : WorkoutRepository {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val _weeklySessions = MutableStateFlow<List<WorkoutSessionItem>>(emptyList())
    override val weeklySessions: StateFlow<List<WorkoutSessionItem>> = _weeklySessions.asStateFlow()

    init {
        checkMondayReset()
        loadSessions()
    }

    private fun checkMondayReset() {
        val calendar = Calendar.getInstance()
        val currentWeek = calendar.get(Calendar.WEEK_OF_YEAR)
        val currentYear = calendar.get(Calendar.YEAR)

        val lastWeek = prefs.getInt(KEY_LAST_WEEK, -1)
        val lastYear = prefs.getInt(KEY_LAST_YEAR, -1)

        if (lastWeek != -1 && (currentYear > lastYear || currentWeek > lastWeek)) {
            // Monday or new week detected: Reset active week sessions
            _weeklySessions.value = emptyList()
            saveSessionsToPrefs(emptyList())

            prefs.edit()
                .putInt(KEY_LAST_WEEK, currentWeek)
                .putInt(KEY_LAST_YEAR, currentYear)
                .apply()
        } else if (lastWeek == -1) {
            prefs.edit()
                .putInt(KEY_LAST_WEEK, currentWeek)
                .putInt(KEY_LAST_YEAR, currentYear)
                .apply()
        }
    }

    private fun loadSessions() {
        val jsonString = prefs.getString(KEY_SESSIONS, null)
        if (jsonString.isNullOrEmpty()) {
            _weeklySessions.value = emptyList()
            saveSessionsToPrefs(emptyList())
        } else {
            try {
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
                _weeklySessions.value = list
            } catch (e: Exception) {
                _weeklySessions.value = emptyList()
            }
        }
    }

    override fun addSession(activityType: ActivityType, durationSeconds: Int, dateString: String, dayOfWeek: String) {
        if (durationSeconds <= 0) return

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
        saveSessionsToPrefs(current)
    }

    override fun getHistoricalWeeks(targetMinutes: Int): List<WeekHistoryDto> {
        return emptyList()
    }

    override fun clearAllData() {
        prefs.edit().clear().apply()
        _weeklySessions.value = emptyList()
    }

    override suspend fun syncWithSupabase() {
        // Local-only mode
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

    companion object {
        private const val PREFS_NAME = "tempo150_local_prefs"
        private const val KEY_SESSIONS = "key_weekly_sessions"
        private const val KEY_LAST_WEEK = "key_last_week_num"
        private const val KEY_LAST_YEAR = "key_last_year_num"
    }
}
