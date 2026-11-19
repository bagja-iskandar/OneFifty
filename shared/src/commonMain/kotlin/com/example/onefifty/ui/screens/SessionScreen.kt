package com.example.onefifty.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
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
    currentSessionSeconds: Int = 0,
    overtimeSeconds: Int = 0,
    onActivitySelected: (ActivityType) -> Unit,
    onStartClick: () -> Unit,
    onPauseClick: () -> Unit,
    onResumeClick: () -> Unit,
    onResetClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(bottom = 12.dp)
    ) {
        // 1. Top Header with Live Progress Ring Logo
        TempoTopBar(
            currentWeeklyMinutes = currentWeeklyMinutes,
            targetWeeklyMinutes = targetWeeklyMinutes,
            subTitle = "SESSION"
        )

        // 2. Main Timer Circle Hero Element (Fills available space and centers dynamically)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            TimerCircleView(
                currentWeeklyMinutes = currentWeeklyMinutes,
                targetWeeklyMinutes = targetWeeklyMinutes,
                timerState = timerState,
                selectedActivity = selectedActivity,
                remainingWeeklySeconds = remainingWeeklySeconds,
                currentSessionSeconds = currentSessionSeconds,
                overtimeSeconds = overtimeSeconds,
                onStartClick = onStartClick,
                onPauseClick = onPauseClick,
                onResumeClick = onResumeClick,
                onResetClick = onResetClick
            )
        }

        // 3. Activity Selector Grid with Hide-to-Bottom on START and Slide-Up on PAUSE
        AnimatedVisibility(
            visible = timerState != TimerState.RUNNING,
            enter = slideInVertically(
                initialOffsetY = { fullHeight -> fullHeight },
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            ) + fadeIn(animationSpec = tween(300)),
            exit = slideOutVertically(
                targetOffsetY = { fullHeight -> fullHeight },
                animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing)
            ) + fadeOut(animationSpec = tween(250))
        ) {
            ActivitySelector(
                selectedActivity = selectedActivity,
                onActivitySelected = onActivitySelected
            )
        }
    }
}
