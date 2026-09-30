package com.example.onefifty.ui.model

import androidx.compose.ui.graphics.Color

enum class ActivityType(
    val displayName: String,
    val iconEmoji: String,
    val themeColor: Color
) {
    RUNNING("Running", "🏃", Color(0xFFFF6B4A)),   // Signature Orange
    WALKING("Walking", "🚶", Color(0xFF38BDF8)),   // Sky Blue
    CYCLING("Cycling", "🚴", Color(0xFFF59E0B)),   // Amber Yellow
    SWIMMING("Swimming", "🏊", Color(0xFF2DD4BF)), // Turquoise Cyan
    GYM("Gym", "🏋️", Color(0xFFC084FC)),          // Purple
    YOGA("Yoga", "🧘", Color(0xFF34D399)),         // Emerald Green
    CARDIO("Cardio", "❤️", Color(0xFFF43F5E)),      // Rose Red
    OTHER("Other", "⚡", Color(0xFF8B9BB4))         // Slate Gray
}
