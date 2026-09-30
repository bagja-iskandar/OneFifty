package com.example.onefifty

import android.os.Bundle
import android.os.CountDownTimer
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.onefifty.repository.LocalWorkoutRepository
import com.example.onefifty.repository.WorkoutRepository
import com.example.onefifty.ui.components.BottomTab
import com.example.onefifty.ui.components.TempoBottomNavigation
import com.example.onefifty.ui.model.ActivityType
import com.example.onefifty.ui.model.TimerState
import com.example.onefifty.ui.screens.SessionScreen
import com.example.onefifty.ui.screens.SettingsScreen
import com.example.onefifty.ui.screens.WeeklyScreen
import com.example.onefifty.ui.theme.NeutralDark
import com.example.onefifty.ui.theme.OneFiftyTheme
import kotlinx.coroutines.launch
import java.util.Calendar

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            OneFiftyTheme {
                OneFiftyAppMain()
            }
        }
    }
}

@Composable
fun OneFiftyAppMain() {
    val context = LocalContext.current
    val workoutRepository: WorkoutRepository = remember { LocalWorkoutRepository(context.applicationContext) }
    val weeklySessions by workoutRepository.weeklySessions.collectAsStateWithLifecycle()

    val pagerState = rememberPagerState(initialPage = 0) { BottomTab.entries.size }
    val coroutineScope = rememberCoroutineScope()
    var selectedTab by remember { mutableStateOf(BottomTab.SESSION) }

    // Bi-directional Synchronization: Pager Page -> Selected Tab
    LaunchedEffect(pagerState.currentPage) {
        selectedTab = BottomTab.entries[pagerState.currentPage]
    }

    // Session & Timer States (Initially null to enforce Activity-First selection)
    var selectedActivity by remember { mutableStateOf<ActivityType?>(null) }
    var timerState by remember { mutableStateOf(TimerState.READY) }

    val targetWeeklyMinutes by remember { mutableIntStateOf(150) }
    val targetWeeklySeconds = targetWeeklyMinutes * 60 // Fixed 9000 seconds
    var keepScreenOn by remember { mutableStateOf(true) }

    // Single Source of Truth: Sum of durationSeconds from repository
    val totalWeeklyLoggedSeconds = weeklySessions.sumOf { it.durationSeconds }
    val currentWeeklyMinutes = totalWeeklyLoggedSeconds / 60

    // Direct Countdown of Remaining Weekly Seconds (9000 - totalWeeklyLoggedSeconds)
    var remainingWeeklySeconds by remember(totalWeeklyLoggedSeconds, targetWeeklySeconds) {
        mutableIntStateOf((targetWeeklySeconds - totalWeeklyLoggedSeconds).coerceAtLeast(0))
    }

    // Precise Timestamp Delta Tracking for Seconds-Based Truth & Pause Time Exclusion
    var startTimestamp by remember { mutableStateOf(0L) }
    var accumulatedSessionSeconds by remember { mutableIntStateOf(0) }

    // Timer Controller Reference
    var countDownTimer: CountDownTimer? by remember { mutableStateOf(null) }

    // Helper to get real-time day name for accurate weekly timeline matching
    fun getRealTimeDayName(): String {
        val cal = Calendar.getInstance()
        return when (cal.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> "Monday"
            Calendar.TUESDAY -> "Tuesday"
            Calendar.WEDNESDAY -> "Wednesday"
            Calendar.THURSDAY -> "Thursday"
            Calendar.FRIDAY -> "Friday"
            Calendar.SATURDAY -> "Saturday"
            Calendar.SUNDAY -> "Sunday"
            else -> "Monday"
        }
    }

    fun getCurrentDateString(): String {
        val cal = Calendar.getInstance()
        val y = cal.get(Calendar.YEAR)
        val m = cal.get(Calendar.MONTH) + 1
        val d = cal.get(Calendar.DAY_OF_MONTH)
        val mStr = if (m < 10) "0$m" else "$m"
        val dStr = if (d < 10) "0$d" else "$d"
        return "$y-$mStr-$dStr"
    }

    // Keep Screen On Effect
    DisposableEffect(keepScreenOn, timerState) {
        val activity = context as? ComponentActivity
        if (activity != null) {
            if (keepScreenOn && timerState == TimerState.RUNNING) {
                activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            } else {
                activity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    fun startTimer() {
        if (selectedActivity == null || remainingWeeklySeconds <= 0) return

        countDownTimer?.cancel()
        timerState = TimerState.RUNNING
        startTimestamp = System.currentTimeMillis()

        val initialRemainingAtStart = remainingWeeklySeconds

        countDownTimer = object : CountDownTimer((remainingWeeklySeconds * 1000).toLong(), 200) {
            override fun onTick(millisUntilFinished: Long) {
                val elapsedCurrentRunSecs = ((System.currentTimeMillis() - startTimestamp) / 1000).toInt()
                remainingWeeklySeconds = (initialRemainingAtStart - elapsedCurrentRunSecs).coerceAtLeast(0)
            }

            override fun onFinish() {
                val elapsedCurrentRunSecs = ((System.currentTimeMillis() - startTimestamp) / 1000).toInt()
                val totalWorkoutSecs = accumulatedSessionSeconds + elapsedCurrentRunSecs
                remainingWeeklySeconds = (initialRemainingAtStart - elapsedCurrentRunSecs).coerceAtLeast(0)
                timerState = TimerState.FINISHED

                if (totalWorkoutSecs > 0) {
                    selectedActivity?.let { activity ->
                        workoutRepository.addSession(activity, totalWorkoutSecs, getCurrentDateString(), getRealTimeDayName())
                    }
                }
                accumulatedSessionSeconds = 0
            }
        }.start()
    }

    fun pauseTimer() {
        countDownTimer?.cancel()
        timerState = TimerState.PAUSED

        val elapsedCurrentRunSecs = if (startTimestamp > 0) {
            ((System.currentTimeMillis() - startTimestamp) / 1000).toInt()
        } else 0
        val totalWorkoutSecs = accumulatedSessionSeconds + elapsedCurrentRunSecs

        if (totalWorkoutSecs > 0) {
            selectedActivity?.let { activity ->
                workoutRepository.addSession(activity, totalWorkoutSecs, getCurrentDateString(), getRealTimeDayName())
            }
        }
        accumulatedSessionSeconds = 0
        startTimestamp = 0L
    }

    fun resumeTimer() {
        startTimer()
    }

    Scaffold(
        containerColor = NeutralDark,
        bottomBar = {
            TempoBottomNavigation(
                selectedTab = BottomTab.entries[pagerState.currentPage],
                onTabSelected = { tab ->
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(tab.ordinal)
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                when (BottomTab.entries[page]) {
                    BottomTab.SESSION -> {
                        SessionScreen(
                            currentWeeklyMinutes = currentWeeklyMinutes,
                            targetWeeklyMinutes = targetWeeklyMinutes,
                            timerState = timerState,
                            selectedActivity = selectedActivity,
                            remainingWeeklySeconds = remainingWeeklySeconds,
                            onActivitySelected = { activity ->
                                if (timerState != TimerState.RUNNING) {
                                    selectedActivity = if (selectedActivity == activity) null else activity
                                    timerState = TimerState.READY
                                }
                            },
                            onStartClick = { startTimer() },
                            onPauseClick = { pauseTimer() },
                            onResumeClick = { resumeTimer() }
                        )
                    }
                    BottomTab.WEEKLY -> {
                        WeeklyScreen(
                            weeklySessions = weeklySessions,
                            currentWeeklyMinutes = currentWeeklyMinutes,
                            targetWeeklyMinutes = targetWeeklyMinutes,
                            historicalWeeks = workoutRepository.getHistoricalWeeks(targetWeeklyMinutes)
                        )
                    }
                    BottomTab.SETTINGS -> {
                        SettingsScreen(
                            targetWeeklyMinutes = targetWeeklyMinutes,
                            currentWeeklyMinutes = currentWeeklyMinutes,
                            weeklySessions = weeklySessions,
                            keepScreenOn = keepScreenOn,
                            onKeepScreenOnChanged = { keepScreenOn = it },
                            onClearAllData = {
                                countDownTimer?.cancel()
                                workoutRepository.clearAllData()
                                selectedActivity = null
                                timerState = TimerState.READY
                            }
                        )
                    }
                }
            }
        }
    }
}
