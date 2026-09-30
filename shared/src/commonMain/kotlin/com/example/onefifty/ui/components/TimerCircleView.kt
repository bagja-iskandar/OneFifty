package com.example.onefifty.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
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
    onStartClick: () -> Unit,
    onPauseClick: () -> Unit,
    onResumeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isActivitySelected = selectedActivity != null
    val isGoalCompleted = currentWeeklyMinutes >= targetWeeklyMinutes || remainingWeeklySeconds <= 0

    // Dynamic Live Progress Fraction based on countdown remaining seconds
    val totalTargetSecs = (targetWeeklyMinutes * 60f).coerceAtLeast(1f)
    val liveProgressFraction = (remainingWeeklySeconds.toFloat() / totalTargetSecs).coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = if (isGoalCompleted) 1f else liveProgressFraction,
        label = "weeklyProgressCountdown"
    )

    // Format Remaining Weekly Countdown in H:MM:SS format (e.g. 2:30:00 -> 2:29:59)
    val h = remainingWeeklySeconds / 3600
    val m = (remainingWeeklySeconds % 3600) / 60
    val s = remainingWeeklySeconds % 60
    val mStr = if (m < 10) "0$m" else "$m"
    val sStr = if (s < 10) "0$s" else "$s"
    val countdownText = "$h:$mStr:$sStr"

    // Clean 1x1 Single Heartbeat Pulse Animation (2400ms cycle)
    val infiniteTransition = rememberInfiniteTransition(label = "singleBeatHeartGlow")
    val heartbeatAlpha by infiniteTransition.animateFloat(
        initialValue = 0.18f,
        targetValue = 0.18f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 2400
                0.18f at 0
                0.65f at 400 using FastOutSlowInEasing   // Single 1x1 beat peak
                0.18f at 1000 using FastOutSlowInEasing  // Smooth rest
                0.18f at 2400
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "singleBeatAlpha"
    )
    val heartbeatRadiusDp by infiniteTransition.animateFloat(
        initialValue = 220f,
        targetValue = 220f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 2400
                220f at 0
                350f at 400 using FastOutSlowInEasing   // Single 1x1 beat peak
                220f at 1000 using FastOutSlowInEasing  // Smooth rest
                220f at 2400
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "singleBeatRadius"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        val circleAlpha = if (isActivitySelected || isGoalCompleted) 1f else 0.45f

        // Circle Container (Enlarged 320dp Hero Element with ZERO layout overflow)
        Box(
            modifier = Modifier
                .size(320.dp)
                .alpha(circleAlpha)
                .drawBehind {
                    if (isActivitySelected || isGoalCompleted) {
                        val glowColor = when {
                            isGoalCompleted -> Color(0xFF4ADE80).copy(alpha = 0.55f) // Green celebration glow
                            timerState == TimerState.RUNNING -> ActiveGreen.copy(alpha = heartbeatAlpha) // Active Emerald Green
                            timerState == TimerState.PAUSED -> PrimaryOrange.copy(alpha = 0.15f)
                            else -> PrimaryOrange.copy(alpha = 0.32f)
                        }

                        val currentRadiusPx = if (timerState == TimerState.RUNNING) {
                            heartbeatRadiusDp.dp.toPx()
                        } else {
                            250.dp.toPx()
                        }

                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    glowColor,
                                    glowColor.copy(alpha = glowColor.alpha * 0.40f),
                                    Color.Transparent
                                ),
                                center = center,
                                radius = currentRadiusPx
                            ),
                            radius = currentRadiusPx,
                            center = center
                        )
                    }
                }
                .clip(CircleShape)
                .background(Color(0xFF13151B))
                .clickable(enabled = isActivitySelected && !isGoalCompleted) {
                    when (timerState) {
                        TimerState.READY -> onStartClick()
                        TimerState.RUNNING -> onPauseClick()
                        TimerState.PAUSED -> onResumeClick()
                        TimerState.FINISHED -> onStartClick()
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            // Live Countdown Ring Progress Arc
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
                val arcColor = when {
                    isGoalCompleted -> Color(0xFF4ADE80) // Green completed ring
                    timerState == TimerState.RUNNING -> ActiveGreen // Active Emerald Green Ring Arc
                    !isActivitySelected -> SecondarySlate.copy(alpha = 0.3f)
                    timerState == TimerState.PAUSED -> PrimaryOrange.copy(alpha = 0.4f)
                    else -> PrimaryOrange
                }
                drawArc(
                    color = arcColor,
                    startAngle = -90f,
                    sweepAngle = 360f * animatedProgress,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }

            // Interior
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(24.dp)
            ) {
                // Top Badge Inside Circle
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (timerState == TimerState.RUNNING) Color(0xFF064E3B) else Color(0xFF222631))
                        .border(1.dp, if (timerState == TimerState.RUNNING) ActiveGreen.copy(alpha = 0.6f) else SurfaceCardBorder, RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 5.dp)
                ) {
                    val badgeText = when {
                        isGoalCompleted -> "GOAL COMPLETED"
                        selectedActivity != null -> "${selectedActivity.iconEmoji}  ${selectedActivity.displayName.uppercase()}"
                        else -> "SELECT ACTIVITY"
                    }
                    val badgeColor = when {
                        isGoalCompleted -> Color(0xFF4ADE80)
                        timerState == TimerState.RUNNING -> ActiveGreen
                        isActivitySelected -> PrimaryOrange
                        else -> SecondarySlate
                    }

                    Text(
                        text = badgeText,
                        color = badgeColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (isGoalCompleted) {
                    // Big Green Checkmark Celebration
                    Text(
                        text = "✓",
                        color = Color(0xFF4ADE80),
                        fontSize = 72.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = "150 / 150 min reached!",
                        color = SecondarySlate,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    // Direct Countdown of Remaining Goal Time in H:MM:SS
                    Text(
                        text = countdownText,
                        color = Color.White,
                        fontSize = 52.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-1.5).sp
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = "/ $targetWeeklyMinutes min remaining goal",
                        color = SecondarySlate,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
