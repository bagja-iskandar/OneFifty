package com.example.onefifty.data

/**
 * Data Transfer Object corresponding to Supabase 'workout_sessions' table.
 */
data class WorkoutSessionDto(
    val id: String,
    val userId: String? = null,
    val activityType: String,
    val durationMinutes: Int,
    val dayOfWeek: String,
    val startTimestamp: Long,
    val weekNumber: Int,
    val year: Int
)
