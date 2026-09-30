package com.example.onefifty.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.onefifty.repository.WeekHistoryDto
import com.example.onefifty.ui.model.WorkoutSessionItem
import com.example.onefifty.ui.theme.NeutralDark
import com.example.onefifty.ui.theme.PrimaryOrange
import com.example.onefifty.ui.theme.ProgressSegmentTrack
import com.example.onefifty.ui.theme.SecondarySlate
import com.example.onefifty.ui.theme.SurfaceCard
import com.example.onefifty.ui.theme.SurfaceCardBorder
import com.example.onefifty.util.DateUtils

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WeeklyScreen(
    weeklySessions: List<WorkoutSessionItem>,
    currentWeeklyMinutes: Int,
    targetWeeklyMinutes: Int = 150,
    historicalWeeks: List<WeekHistoryDto> = emptyList(),
    modifier: Modifier = Modifier
) {
    val dailyDates = DateUtils.getCurrentWeekDailyDates()
    val totalLoggedSeconds = weeklySessions.sumOf { it.durationSeconds }
    val totalLoggedMinutes = totalLoggedSeconds / 60
    val targetWeeklyGoalSeconds = (targetWeeklyMinutes * 60f).coerceAtLeast(1f) // 9000s FIXED DENOMINATOR

    val progressFraction = (totalLoggedSeconds.toFloat() / targetWeeklyGoalSeconds).coerceIn(0f, 1f)
    val percentageInt = (progressFraction * 100).toInt()
    val minutesLeft = (targetWeeklyMinutes - totalLoggedMinutes).coerceAtLeast(0)

    var showCalendarModal by remember { mutableStateOf(false) }

    // Grouping by ActivityType for Weekly Composition
    val activityBreakdown = weeklySessions.groupBy { it.activityType }
        .mapValues { (_, sessions) -> sessions.sumOf { it.durationSeconds } }
        .toList()
        .sortedByDescending { it.second }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(top = 16.dp)
    ) {
        // 1. Live Today Indicator Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF1A1C23))
                    .border(1.dp, PrimaryOrange.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                    .clickable {
                        if (historicalWeeks.isNotEmpty()) {
                            showCalendarModal = true
                        }
                    }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = DateUtils.getTodayString(),
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 2. Top Overview Card with Donut Ring
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "WEEKLY SUMMARY",
                                color = SecondarySlate,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            // Human readable duration string (No "0:13" timer countdown format!)
                            val durationSummaryText = when {
                                totalLoggedSeconds < 60 -> "${totalLoggedSeconds}s"
                                else -> "${totalLoggedMinutes} min"
                            }

                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = durationSummaryText,
                                    color = Color.White,
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "/ 150 min goal",
                                    color = SecondarySlate,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Correct Wording & Success States (No checkmark when under 150m!)
                            val statusText = when {
                                totalLoggedSeconds < 9000 -> "$minutesLeft min remaining"
                                totalLoggedSeconds == 9000 -> "✓ Goal reached"
                                else -> "✓ Goal exceeded (+${(totalLoggedSeconds - 9000) / 60} min)"
                            }
                            val statusColor = if (totalLoggedSeconds >= 9000) Color(0xFF4ADE80) else SecondarySlate

                            Text(
                                text = statusText,
                                color = statusColor,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        // Donut Ring Chart Graphic
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF13151B)),
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize().padding(6.dp)) {
                                val strokeWidth = 6.dp.toPx()
                                drawCircle(
                                    color = ProgressSegmentTrack,
                                    style = Stroke(width = strokeWidth)
                                )
                                drawArc(
                                    color = if (totalLoggedSeconds >= 9000) Color(0xFF4ADE80) else PrimaryOrange,
                                    startAngle = -90f,
                                    sweepAngle = 360f * progressFraction,
                                    useCenter = false,
                                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                                )
                            }

                            Text(
                                text = "$percentageInt%",
                                color = if (totalLoggedSeconds >= 9000) Color(0xFF4ADE80) else PrimaryOrange,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // 3. WORKOUT SHARE & DISTRIBUTION (Full Track = 150m Goal, Colored Portion = Actual Progress)
            if (activityBreakdown.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder)
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Text(
                                text = "WORKOUT SHARE & DISTRIBUTION",
                                color = SecondarySlate,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Active Color Legend
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                activityBreakdown.forEach { (activity, seconds) ->
                                    val goalPct = (seconds.toFloat() / targetWeeklyGoalSeconds * 100f)
                                    val pctStr = if (goalPct < 1.0f && goalPct > 0.0f) {
                                        "<1%"
                                    } else {
                                        "${goalPct.toInt()}%"
                                    }
                                    val durStr = if (seconds < 60) "${seconds}s" else "${seconds / 60}m"

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(activity.themeColor)
                                        )
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text(
                                            text = "${activity.displayName} ($durStr • $pctStr)",
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Large Single Weekly Stacked Horizontal Bar
                            // Full Track = 150-Minute Goal (100% width)
                            // Colored Portion = Progress Fraction (totalLoggedSeconds / 9000s)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(24.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(ProgressSegmentTrack)
                            ) {
                                val coloredFraction = (totalLoggedSeconds.toFloat() / targetWeeklyGoalSeconds).coerceIn(0f, 1f)
                                if (coloredFraction > 0f) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth(coloredFraction)
                                            .height(24.dp)
                                            .clip(RoundedCornerShape(8.dp)),
                                        horizontalArrangement = Arrangement.spacedBy(1.dp)
                                    ) {
                                        activityBreakdown.forEach { (activity, seconds) ->
                                            val segmentFraction = if (totalLoggedSeconds > 0) {
                                                (seconds.toFloat() / totalLoggedSeconds.toFloat()).coerceIn(0f, 1f)
                                            } else 0f

                                            if (segmentFraction > 0f) {
                                                Box(
                                                    modifier = Modifier
                                                        .weight(segmentFraction)
                                                        .height(24.dp)
                                                        .background(activity.themeColor),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    // Embed Activity Emoji Icon directly inside segment if wide enough
                                                    if (segmentFraction >= 0.08f) {
                                                        Text(
                                                            text = activity.iconEmoji,
                                                            fontSize = 12.sp
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 4. DAILY TIMELINE LOG (Daily Stacked Bar with Embedded Icons)
            item {
                Text(
                    text = "DAILY TIMELINE LOG",
                    color = SecondarySlate,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }

            items(dailyDates) { (dayName, dateStr) ->
                val rawSessionsForDay = weeklySessions.filter { it.dayOfWeek.equals(dayName, ignoreCase = true) }
                
                // Aggregation by ActivityType for the same day!
                val aggregatedForDay = rawSessionsForDay.groupBy { it.activityType }
                    .mapValues { (_, sessions) -> sessions.sumOf { it.durationSeconds } }
                    .toList()
                    .sortedByDescending { it.second }

                val dayTotalSeconds = aggregatedForDay.sumOf { it.second }
                val hasActivity = dayTotalSeconds > 0
                val cardBorderColor = if (hasActivity) PrimaryOrange.copy(alpha = 0.5f) else SurfaceCardBorder

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                    border = androidx.compose.foundation.BorderStroke(1.dp, cardBorderColor)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = dayName,
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "($dateStr)",
                                    color = SecondarySlate,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Normal
                                )
                            }

                            if (dayTotalSeconds > 0) {
                                val formattedDayTotal = if (dayTotalSeconds < 60) "${dayTotalSeconds}s total" else "${dayTotalSeconds / 60} min total"
                                Text(
                                    text = formattedDayTotal,
                                    color = PrimaryOrange,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            } else {
                                Text(
                                    text = "No activity logged",
                                    color = SecondarySlate.copy(alpha = 0.5f),
                                    fontSize = 12.sp
                                )
                            }
                        }

                        if (hasActivity) {
                            Spacer(modifier = Modifier.height(12.dp))

                            // Proportional Stacked Horizontal Bar with Embedded Activity Emoji Icons
                            // Full Bar = Total Workout on That Day
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(20.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(ProgressSegmentTrack),
                                horizontalArrangement = Arrangement.spacedBy(1.dp)
                            ) {
                                aggregatedForDay.forEach { (activity, seconds) ->
                                    val fraction = (seconds.toFloat() / dayTotalSeconds.toFloat()).coerceIn(0f, 1f)
                                    if (fraction > 0f) {
                                        Box(
                                            modifier = Modifier
                                                .weight(fraction)
                                                .height(20.dp)
                                                .background(activity.themeColor),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            // Embed activity emoji icon if segment is wide enough
                                            if (fraction >= 0.12f) {
                                                Text(
                                                    text = activity.iconEmoji,
                                                    fontSize = 11.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    // Real-Time Calendar History Dialog
    if (showCalendarModal && historicalWeeks.isNotEmpty()) {
        Dialog(onDismissRequest = { showCalendarModal = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = NeutralDark),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, PrimaryOrange.copy(alpha = 0.6f))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "REAL-TIME CALENDAR HISTORY",
                            color = PrimaryOrange,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )

                        TextButton(onClick = { showCalendarModal = false }) {
                            Text(text = "Close", color = SecondarySlate, fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    historicalWeeks.forEach { week ->
                        val isCompleted = week.loggedMinutes >= week.targetMinutes
                        val percentage = if (week.targetMinutes > 0) (week.loggedMinutes * 100) / week.targetMinutes else 0

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(SurfaceCard)
                                .border(1.dp, SurfaceCardBorder, RoundedCornerShape(14.dp))
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Week ${week.weekNumber} (${week.dateRange})",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${week.loggedMinutes} / ${week.targetMinutes} min logged",
                                    color = SecondarySlate,
                                    fontSize = 11.sp
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isCompleted) Color(0xFF143823) else Color(0xFF222631))
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = if (isCompleted) "100% ✓" else "$percentage%",
                                    color = if (isCompleted) Color(0xFF4ADE80) else PrimaryOrange,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}
