package com.example.onefifty

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
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
import com.example.onefifty.ui.theme.PrimaryOrange
import com.example.onefifty.ui.theme.SecondarySlate
import com.example.onefifty.ui.theme.SurfaceCardBorder
import com.example.onefifty.util.DateUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val MAX_NORMAL_SESSION_SECONDS = 3 * 3600 // 3 hours safeguard against forgotten timers

private data class LongSessionAlertState(
    val activityType: ActivityType,
    val startTimestamp: Long,
    val elapsedSeconds: Int
)

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

    // Session & Timer States
    var selectedActivity by remember { mutableStateOf<ActivityType?>(null) }
    var timerState by remember { mutableStateOf(TimerState.READY) }

    val targetWeeklyMinutes by remember { mutableIntStateOf(150) }
    val targetWeeklySeconds = targetWeeklyMinutes * 60 // 9000 seconds
    var keepScreenOn by remember { mutableStateOf(true) }

    // Single Source of Truth: Sum of durationSeconds from repository
    val totalWeeklyLoggedSeconds = weeklySessions.sumOf { it.durationSeconds }
    val currentWeeklyMinutes = totalWeeklyLoggedSeconds / 60

    // Precise Timestamp Delta Tracking for Screen-off and Background Accuracy
    var startTimestamp by remember { mutableLongStateOf(0L) }
    var currentRunSecs by remember { mutableIntStateOf(0) }
    var currentSessionSeconds by remember { mutableIntStateOf(0) }
    var sessionAccumulatedSeconds by remember { mutableIntStateOf(0) }
    var longSessionAlert by remember { mutableStateOf<LongSessionAlertState?>(null) }

    fun resetTimerUiState() {
        selectedActivity = null
        timerState = TimerState.READY
        startTimestamp = 0L
        sessionAccumulatedSeconds = 0
        currentRunSecs = 0
        currentSessionSeconds = 0
        longSessionAlert = null
    }

    fun triggerLongSessionGuard(activity: ActivityType, startedAt: Long, elapsedSecs: Int) {
        workoutRepository.clearActiveSessionState()
        timerState = TimerState.PAUSED
        startTimestamp = 0L
        currentRunSecs = 0
        currentSessionSeconds = sessionAccumulatedSeconds
        longSessionAlert = LongSessionAlertState(
            activityType = activity,
            startTimestamp = startedAt,
            elapsedSeconds = elapsedSecs
        )
    }

    // Crash / Process-Kill Auto-Recovery with 3-hour anomaly safeguard
    LaunchedEffect(Unit) {
        val saved = workoutRepository.getActiveSessionState()
        if (saved != null) {
            val elapsedSecs = ((System.currentTimeMillis() - saved.startTimestamp) / 1000L).toInt()
            if (elapsedSecs in 1..MAX_NORMAL_SESSION_SECONDS) {
                selectedActivity = saved.activityType
                startTimestamp = saved.startTimestamp
                sessionAccumulatedSeconds = saved.sessionAccumulatedSeconds
                timerState = TimerState.RUNNING
            } else if (elapsedSecs > MAX_NORMAL_SESSION_SECONDS) {
                selectedActivity = saved.activityType
                sessionAccumulatedSeconds = saved.sessionAccumulatedSeconds
                triggerLongSessionGuard(saved.activityType, saved.startTimestamp, elapsedSecs)
            } else {
                workoutRepository.clearActiveSessionState()
            }
        }
    }

    // Monday 00:00:00 Midnight Transition Check on App Resume
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                workoutRepository.checkMondayReset()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Real-Time Coroutine Ticker while RUNNING (immune to process suspension drift)
    LaunchedEffect(timerState) {
        while (timerState == TimerState.RUNNING) {
            val runSecs = if (startTimestamp > 0) {
                ((System.currentTimeMillis() - startTimestamp) / 1000L).toInt()
            } else 0

            if (runSecs > MAX_NORMAL_SESSION_SECONDS) {
                val act = selectedActivity
                if (act != null) {
                    triggerLongSessionGuard(act, startTimestamp, runSecs)
                } else {
                    workoutRepository.clearActiveSessionState()
                    timerState = TimerState.PAUSED
                    startTimestamp = 0L
                    currentRunSecs = 0
                }
                break
            }

            currentRunSecs = runSecs
            currentSessionSeconds = sessionAccumulatedSeconds + runSecs
            delay(200)
        }
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

    fun persistSegment(activity: ActivityType, durationSecs: Int, startedAt: Long) {
        if (durationSecs <= 0) return
        val effectiveTs = if (startedAt > 0L) startedAt else System.currentTimeMillis()
        workoutRepository.addSession(
            activityType = activity,
            durationSeconds = durationSecs,
            dateString = DateUtils.getDateIsoFromMillis(effectiveTs),
            dayOfWeek = DateUtils.getDayNameFromMillis(effectiveTs),
            formattedTime = DateUtils.getTimeFormattedFromMillis(effectiveTs)
        )
    }

    fun startTimer() {
        val act = selectedActivity ?: return
        timerState = TimerState.RUNNING
        val now = System.currentTimeMillis()
        startTimestamp = now
        workoutRepository.saveActiveSessionState(act, now, sessionAccumulatedSeconds)
    }

    fun pauseTimer() {
        val startedAt = startTimestamp
        val runSecs = if (startedAt > 0) {
            ((System.currentTimeMillis() - startedAt) / 1000L).toInt()
        } else 0

        if (runSecs > MAX_NORMAL_SESSION_SECONDS) {
            val act = selectedActivity
            if (act != null) {
                triggerLongSessionGuard(act, startedAt, runSecs)
                return
            }
        }

        sessionAccumulatedSeconds += runSecs
        currentSessionSeconds = sessionAccumulatedSeconds
        currentRunSecs = 0

        selectedActivity?.let { activity ->
            persistSegment(activity, runSecs, startedAt)
        }

        workoutRepository.clearActiveSessionState()
        timerState = TimerState.PAUSED
        startTimestamp = 0L
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
                        val liveWeeklySeconds = totalWeeklyLoggedSeconds + currentRunSecs
                        val remainingWeeklySeconds = (targetWeeklySeconds - liveWeeklySeconds).coerceAtLeast(0)
                        val overtimeSeconds = (liveWeeklySeconds - targetWeeklySeconds).coerceAtLeast(0)
                        val liveWeeklyMinutes = liveWeeklySeconds / 60

                        SessionScreen(
                            currentWeeklyMinutes = liveWeeklyMinutes,
                            targetWeeklyMinutes = targetWeeklyMinutes,
                            timerState = timerState,
                            selectedActivity = selectedActivity,
                            remainingWeeklySeconds = remainingWeeklySeconds,
                            currentSessionSeconds = currentSessionSeconds,
                            overtimeSeconds = overtimeSeconds,
                            onActivitySelected = { activity ->
                                if (timerState != TimerState.RUNNING) {
                                    selectedActivity = if (selectedActivity == activity) null else activity
                                    timerState = TimerState.READY
                                    workoutRepository.clearActiveSessionState()
                                    startTimestamp = 0L
                                    sessionAccumulatedSeconds = 0
                                    currentRunSecs = 0
                                    currentSessionSeconds = 0
                                }
                            },
                            onStartClick = { startTimer() },
                            onPauseClick = { pauseTimer() },
                            onResumeClick = { startTimer() },
                            onResetClick = {
                                timerState = TimerState.READY
                                workoutRepository.clearActiveSessionState()
                                startTimestamp = 0L
                                sessionAccumulatedSeconds = 0
                                currentRunSecs = 0
                                currentSessionSeconds = 0
                            }
                        )
                    }
                    BottomTab.WEEKLY -> {
                        var historyVersion by remember { mutableIntStateOf(0) }
                        val rememberedHistoricalWeeks = remember(weeklySessions, historyVersion, targetWeeklyMinutes) {
                            workoutRepository.getHistoricalWeeks(targetWeeklyMinutes)
                        }
                        WeeklyScreen(
                            weeklySessions = weeklySessions,
                            currentWeeklyMinutes = currentWeeklyMinutes,
                            targetWeeklyMinutes = targetWeeklyMinutes,
                            historicalWeeks = rememberedHistoricalWeeks,
                            onDeleteSession = { sessionId ->
                                workoutRepository.deleteSession(sessionId)
                                historyVersion++
                            },
                            onAddManualSession = { activity, durationSecs, dateStr, dayName ->
                                workoutRepository.addSession(
                                    activityType = activity,
                                    durationSeconds = durationSecs,
                                    dateString = dateStr,
                                    dayOfWeek = dayName,
                                    formattedTime = "Manual"
                                )
                                historyVersion++
                            },
                            onGetSessionsForOffset = { offset ->
                                workoutRepository.getSessionsForWeekOffset(offset)
                            }
                        )
                    }
                    BottomTab.SETTINGS -> {
                        SettingsScreen(
                            targetWeeklyMinutes = targetWeeklyMinutes,
                            currentWeeklyMinutes = currentWeeklyMinutes,
                            weeklySessions = weeklySessions,
                            keepScreenOn = keepScreenOn,
                            onKeepScreenOnChanged = { keepScreenOn = it },
                            onClearCurrentWeek = {
                                workoutRepository.clearCurrentWeekData()
                                resetTimerUiState()
                            },
                            onClearAllData = {
                                workoutRepository.clearAllData()
                                resetTimerUiState()
                            }
                        )
                    }
                }
            }
        }
    }

    // Long Session Safeguard Dialog (triggered when active run exceeds 3 hours)
    longSessionAlert?.let { alert ->
        LongSessionRecoveryDialog(
            alert = alert,
            onDiscard = {
                if (sessionAccumulatedSeconds == 0) {
                    timerState = TimerState.READY
                }
                longSessionAlert = null
            },
            onSaveTrimmedMinutes = { trimmedMins ->
                val trimmedSecs = trimmedMins * 60
                persistSegment(alert.activityType, trimmedSecs, alert.startTimestamp)
                sessionAccumulatedSeconds += trimmedSecs
                currentSessionSeconds = sessionAccumulatedSeconds
                longSessionAlert = null
            },
            onSaveFullDuration = {
                persistSegment(alert.activityType, alert.elapsedSeconds, alert.startTimestamp)
                sessionAccumulatedSeconds += alert.elapsedSeconds
                currentSessionSeconds = sessionAccumulatedSeconds
                longSessionAlert = null
            }
        )
    }
}

@Composable
private fun LongSessionRecoveryDialog(
    alert: LongSessionAlertState,
    onDiscard: () -> Unit,
    onSaveTrimmedMinutes: (Int) -> Unit,
    onSaveFullDuration: () -> Unit
) {
    var trimMinutesText by remember(alert) { mutableStateOf("30") }
    val hours = alert.elapsedSeconds / 3600
    val mins = (alert.elapsedSeconds % 3600) / 60
    val elapsedLabel = "${hours}j ${mins}m"

    Dialog(onDismissRequest = { /* User must explicitly choose Discard or Save */ }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = NeutralDark),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, PrimaryOrange.copy(alpha = 0.8f))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "⚠️ SESI TERLALU LAMA TERDETEKSI",
                    color = PrimaryOrange,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "${alert.activityType.iconEmoji} ${alert.activityType.displayName} berjalan selama $elapsedLabel",
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Timer otomatis dihentikan karena melewati batas wajar 3 jam. Apakah Anda lupa mematikan Start?",
                    color = SecondarySlate,
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Pangkas ke durasi sebenarnya (menit):",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(15, 30, 45, 60).forEach { preset ->
                        val isSelected = trimMinutesText == "$preset"
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) PrimaryOrange else Color(0xFF1E2129))
                                .clickable { trimMinutesText = "$preset" }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${preset}m",
                                color = if (isSelected) Color.White else SecondarySlate,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = trimMinutesText,
                    onValueChange = { trimMinutesText = it.filter { ch -> ch.isDigit() } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryOrange,
                        unfocusedBorderColor = SurfaceCardBorder,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                val parsedMins = trimMinutesText.toIntOrNull()?.coerceIn(1, 180) ?: 0
                Button(
                    onClick = { if (parsedMins > 0) onSaveTrimmedMinutes(parsedMins) },
                    enabled = parsedMins > 0,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                ) {
                    Text(
                        text = "Pangkas & Simpan (${parsedMins}m)",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = onDiscard,
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.7f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                ) {
                    Text(
                        text = "🗑️ Buang Sesi Ini (Salah Pencet / Lupa)",
                        color = Color(0xFFEF4444),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                TextButton(
                    onClick = onSaveFullDuration,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text(
                        text = "Tetap Simpan Penuh ($elapsedLabel)",
                        color = SecondarySlate,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}
