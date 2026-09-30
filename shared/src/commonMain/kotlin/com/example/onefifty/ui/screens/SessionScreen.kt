package com.example.onefifty.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.onefifty.ui.components.ActivitySelector
import com.example.onefifty.ui.components.TempoTopBar
import com.example.onefifty.ui.components.TimerCircleView
import com.example.onefifty.ui.model.ActivityType
import com.example.onefifty.ui.model.TimerState

@Composable
fun SessionScreen(
    currentWeeklyMinutes: Int,
    targetWeeklyMinutes: Int,
    timerState: TimerState,
    selectedActivity: ActivityType?,
    remainingWeeklySeconds: Int,
    onActivitySelected: (ActivityType) -> Unit,
    onStartClick: () -> Unit,
    onPauseClick: () -> Unit,
    onResumeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Fixed Single Screen Container (Zero Scroll Required, Pure Timer Focus!)
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(bottom = 12.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // 1. Top Header with Live Progress Ring Logo
        TempoTopBar(
            currentWeeklyMinutes = currentWeeklyMinutes,
            targetWeeklyMinutes = targetWeeklyMinutes
        )

        // 2. Enlarged Main Timer Circle Hero Element (320dp)
        TimerCircleView(
            currentWeeklyMinutes = currentWeeklyMinutes,
            targetWeeklyMinutes = targetWeeklyMinutes,
            timerState = timerState,
            selectedActivity = selectedActivity,
            remainingWeeklySeconds = remainingWeeklySeconds,
            onStartClick = onStartClick,
            onPauseClick = onPauseClick,
            onResumeClick = onResumeClick
        )

        // 3. Activity Selector Grid (Symmetrical 4x2 Square Rounded Buttons)
        ActivitySelector(
            selectedActivity = selectedActivity,
            onActivitySelected = onActivitySelected
        )
    }
}
