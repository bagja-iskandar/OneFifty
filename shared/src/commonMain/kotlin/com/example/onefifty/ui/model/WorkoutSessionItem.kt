package com.example.onefifty.ui.model

data class WorkoutSessionItem(
    val id: String,
    val dateString: String,      // e.g. "2026-10-14"
    val dayOfWeek: String,       // e.g. "Monday", "Tuesday"
    val activityType: ActivityType,
    val durationSeconds: Int,    // Actual duration in SECONDS (Single Source of Truth!)
    val formattedTime: String    // e.g. "07:30" or "Now"
) {
    val durationMinutes: Int get() = durationSeconds / 60

    fun formatDurationString(): String {
        return when {
            durationSeconds < 60 -> "${durationSeconds}s"
            durationSeconds % 60 == 0 -> "${durationSeconds / 60}m"
            durationSeconds < 3600 -> "${durationSeconds / 60}m ${durationSeconds % 60}s"
            else -> {
                val hrs = durationSeconds / 3600
                val mins = (durationSeconds % 3600) / 60
                if (mins == 0) "${hrs}h" else "${hrs}h ${mins}m"
            }
        }
    }
}
