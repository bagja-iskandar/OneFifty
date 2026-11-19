package com.example.onefifty.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.onefifty.ui.model.ActivityType
import com.example.onefifty.ui.model.TimerState
import com.example.onefifty.ui.theme.ActiveGreen
import com.example.onefifty.ui.theme.PrimaryOrange
import com.example.onefifty.ui.theme.ProgressSegmentTrack
import com.example.onefifty.ui.theme.SecondarySlate
import com.example.onefifty.ui.theme.SurfaceCardBorder

@Composable
fun TimerCircleView(
    currentWeeklyMinutes: Int,
    targetWeeklyMinutes: Int = 150,
    timerState: TimerState,
    selectedActivity: ActivityType?,
    remainingWeeklySeconds: Int,
    currentSessionSeconds: Int = 0,
    overtimeSeconds: Int = 0,
    onStartClick: () -> Unit,
    onPauseClick: () -> Unit,
    onResumeClick: () -> Unit,
    onResetClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isActivitySelected = selectedActivity != null
    val isGoalCompleted = remainingWeeklySeconds <= 0 || overtimeSeconds > 0

    // Forward Filling Ring (0% to 100%): 0s = 0f, 9000s = 1f
    val totalTargetSecs = (targetWeeklyMinutes * 60f).coerceAtLeast(1f)
    val totalLoggedSecs = (totalTargetSecs - remainingWeeklySeconds).coerceAtLeast(0f)
    val forwardProgressFraction = (totalLoggedSecs / totalTargetSecs).coerceIn(0f, 1f)

    val animatedProgress by animateFloatAsState(
        targetValue = if (isGoalCompleted) 1f else forwardProgressFraction,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "weeklyProgressForward"
    )

    // Smooth Color Morphing Animation
    val targetThemeColor = when {
        isGoalCompleted -> Color(0xFF4ADE80)
        timerState == TimerState.RUNNING -> ActiveGreen
        timerState == TimerState.PAUSED -> PrimaryOrange.copy(alpha = 0.8f)
        else -> PrimaryOrange
    }
    val animatedThemeColor by animateColorAsState(
        targetValue = targetThemeColor,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "animatedThemeColor"
    )

    // Hero Scale Animation: Expands gracefully when RUNNING
    val heroScale by animateFloatAsState(
        targetValue = if (timerState == TimerState.RUNNING) 1.05f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "heroScale"
    )

    // Format Strings
    val mainTimeDisplay: String
    val subGoalDisplay: String

    if (isGoalCompleted) {
        val otH = overtimeSeconds / 3600
        val otM = (overtimeSeconds % 3600) / 60
        val otS = overtimeSeconds % 60
        val otMStr = if (otM < 10) "0$otM" else "$otM"
        val otSStr = if (otS < 10) "0$otS" else "$otS"
        val otHStr = if (otH < 10) "0$otH" else "$otH"
        mainTimeDisplay = "+$otHStr:$otMStr:$otSStr"
        val otMinutesTotal = overtimeSeconds / 60
        subGoalDisplay = if (otMinutesTotal > 0) "✓ 150m goal reached (+${otMinutesTotal}m)" else "✓ 150m goal reached!"
    } else {
        val h = remainingWeeklySeconds / 3600
        val m = (remainingWeeklySeconds % 3600) / 60
        val s = remainingWeeklySeconds % 60
        val mStr = if (m < 10) "0$m" else "$m"
        val sStr = if (s < 10) "0$s" else "$s"
        mainTimeDisplay = "$h:$mStr:$sStr"
        subGoalDisplay = "/ $targetWeeklyMinutes min weekly goal"
    }

    // Format Current Session Duration (e.g. "14:35")
    val sessionM = currentSessionSeconds / 60
    val sessionS = currentSessionSeconds % 60
    val sessionSStr = if (sessionS < 10) "0$sessionS" else "$sessionS"
    val sessionText = if (sessionM < 60) "${sessionM}:${sessionSStr}" else "${sessionM / 60}h ${sessionM % 60}m"

    // Organic Dual-Pulse (Lub-Dub) Heartbeat Animation (2400ms cycle)
    val infiniteTransition = rememberInfiniteTransition(label = "organicHeartbeatPulse")
    val heartbeatAlpha by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.15f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 2400
                0.15f at 0
                0.55f at 180 using FastOutSlowInEasing  // Beat 1 Peak (lub)
                0.22f at 320 using FastOutSlowInEasing  // Valley
                0.70f at 500 using FastOutSlowInEasing  // Beat 2 Peak (dub)
                0.15f at 850 using FastOutSlowInEasing  // Return to rest
                0.15f at 2400                           // Rest period
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "heartbeatAlpha"
    )
    val heartbeatRadiusDp by infiniteTransition.animateFloat(
        initialValue = 220f,
        targetValue = 220f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 2400
                220f at 0
                300f at 180 using FastOutSlowInEasing  // Beat 1 Peak
                240f at 320 using FastOutSlowInEasing  // Valley
                340f at 500 using FastOutSlowInEasing  // Beat 2 Peak
                220f at 850 using FastOutSlowInEasing  // Return to rest
                220f at 2400
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "heartbeatRadius"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        val circleAlpha = if (isActivitySelected) 1f else 0.45f

        // Circle Container with Hero Scaling and Dual-Layer Organic Heartbeat Aura
        Box(
            modifier = Modifier
                .size(320.dp)
                .graphicsLayer {
                    scaleX = heroScale
                    scaleY = heroScale
                }
                .alpha(circleAlpha)
                .drawBehind {
                    if (isActivitySelected) {
                        val activeColor = animatedThemeColor
                        val isRunning = timerState == TimerState.RUNNING

                        val outerGlowAlpha = if (isRunning) heartbeatAlpha else if (isGoalCompleted) 0.40f else 0.28f
                        val outerRadiusPx = if (isRunning) heartbeatRadiusDp.dp.toPx() else 245.dp.toPx()
                        val innerRadiusPx = outerRadiusPx * 0.72f

                        // Layer 1: Outer Soft Ambient Diffusion
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    activeColor.copy(alpha = outerGlowAlpha * 0.5f),
                                    activeColor.copy(alpha = outerGlowAlpha * 0.15f),
                                    Color.Transparent
                                ),
                                center = center,
                                radius = outerRadiusPx
                            ),
                            radius = outerRadiusPx,
                            center = center
                        )

                        // Layer 2: Inner Vibrant Core Aura
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    activeColor.copy(alpha = outerGlowAlpha * 0.85f),
                                    activeColor.copy(alpha = outerGlowAlpha * 0.35f),
                                    Color.Transparent
                                ),
                                center = center,
                                radius = innerRadiusPx
                            ),
                            radius = innerRadiusPx,
                            center = center
                        )
                    }
                }
                .clip(CircleShape)
                .background(Color(0xFF13151B))
                .clickable(enabled = isActivitySelected) {
                    when (timerState) {
                        TimerState.READY -> onStartClick()
                        TimerState.RUNNING -> onPauseClick()
                        TimerState.PAUSED -> onResumeClick()
                        TimerState.FINISHED -> onStartClick()
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            // Live Progress Ring Arc (Smooth forward filling 0% to 100%)
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
            ) {
                val strokeWidth = 12.dp.toPx()
                drawCircle(
                    color = ProgressSegmentTrack,
                    style = Stroke(width = strokeWidth)
                )

                // Soft neon halo glow behind active arc
                if (timerState == TimerState.RUNNING || isGoalCompleted) {
                    drawArc(
                        color = animatedThemeColor.copy(alpha = 0.35f),
                        startAngle = -90f,
                        sweepAngle = 360f * animatedProgress,
                        useCenter = false,
                        style = Stroke(width = strokeWidth + 6.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                // Sharp foreground arc
                drawArc(
                    color = animatedThemeColor,
                    startAngle = -90f,
                    sweepAngle = 360f * animatedProgress,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }

            // Interior Elements
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(24.dp)
            ) {
                // Top Badge Inside Circle
                val badgeBg = when {
                    isGoalCompleted -> Color(0xFF0F3820)
                    timerState == TimerState.RUNNING -> Color(0xFF064E3B)
                    timerState == TimerState.PAUSED -> Color(0xFF2C2216)
                    else -> Color(0xFF222631)
                }
                val badgeBorder = when {
                    isGoalCompleted -> Color(0xFF4ADE80).copy(alpha = 0.6f)
                    timerState == TimerState.RUNNING -> ActiveGreen.copy(alpha = 0.6f)
                    timerState == TimerState.PAUSED -> PrimaryOrange.copy(alpha = 0.6f)
                    else -> SurfaceCardBorder
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(badgeBg)
                        .border(1.dp, badgeBorder, RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 5.dp)
                ) {
                    val badgeText = when {
                        isGoalCompleted && timerState == TimerState.RUNNING -> "🔥 OVERTIME WORKOUT"
                        isGoalCompleted -> "🎉 GOAL COMPLETED"
                        timerState == TimerState.RUNNING -> "ACTIVE ${selectedActivity?.displayName?.uppercase() ?: ""}"
                        timerState == TimerState.PAUSED -> "PAUSED • TAP TO RESUME"
                        selectedActivity != null -> "${selectedActivity.iconEmoji}  ${selectedActivity.displayName.uppercase()}"
                        else -> "SELECT ACTIVITY"
                    }

                    Text(
                        text = badgeText,
                        color = animatedThemeColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Main Direct Countdown OR Overtime Count-Up Text (Tabular figures to prevent jitter)
                Text(
                    text = mainTimeDisplay,
                    color = if (isGoalCompleted) Color(0xFF4ADE80) else Color.White,
                    fontSize = 50.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-1.5).sp,
                    style = androidx.compose.ui.text.TextStyle(fontFeatureSettings = "tnum")
                )

                Spacer(modifier = Modifier.height(2.dp))

                // Goal Subtext
                Text(
                    text = subGoalDisplay,
                    color = if (isGoalCompleted) Color(0xFF4ADE80).copy(alpha = 0.8f) else SecondarySlate,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )

                // Current Session Elapsed Time Sub-Indicator
                if (timerState == TimerState.RUNNING || timerState == TimerState.PAUSED || currentSessionSeconds > 0) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF1E2129))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "This session: $sessionText",
                            color = if (timerState == TimerState.RUNNING) ActiveGreen else SecondarySlate,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}
