package com.example.onefifty.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.onefifty.repository.HistoricalWeekData
import com.example.onefifty.ui.model.ActivityType
import com.example.onefifty.ui.model.WorkoutSessionItem
import com.example.onefifty.ui.theme.ActiveGreen
import com.example.onefifty.ui.theme.NeutralDark
import com.example.onefifty.ui.theme.PrimaryOrange
import com.example.onefifty.ui.theme.ProgressSegmentTrack
import com.example.onefifty.ui.theme.SecondarySlate
import com.example.onefifty.ui.theme.SurfaceCard
import com.example.onefifty.ui.theme.SurfaceCardBorder
import com.example.onefifty.ui.theme.SurfaceChip
import com.example.onefifty.util.DateUtils

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WeeklyScreen(
    weeklySessions: List<WorkoutSessionItem>,
    currentWeeklyMinutes: Int,
    targetWeeklyMinutes: Int = 150,
    historicalWeeks: List<HistoricalWeekData> = emptyList(),
    onDeleteSession: (String) -> Unit = {},
    onAddManualSession: (ActivityType, Int, String, String) -> Unit = { _, _, _, _ -> },
    onGetSessionsForOffset: (Int) -> List<WorkoutSessionItem> = { emptyList() },
    modifier: Modifier = Modifier
) {
    // Week Navigation State: 0 = current week, -1 = last week, -2 = 2 weeks ago...
    var selectedWeekOffset by remember { mutableIntStateOf(0) }
    var showManualAddDialog by remember { mutableStateOf(false) }
    var pendingDeleteGroup by remember { mutableStateOf<DayActivityGroup?>(null) }

    // Fetch active sessions for the selected week offset (Memoized to prevent UI-thread lag)
    val activeWeekSessions = remember(selectedWeekOffset, weeklySessions, historicalWeeks) {
        if (selectedWeekOffset == 0) {
            weeklySessions
        } else {
            historicalWeeks.find { it.weekOffset == selectedWeekOffset }?.sessions
                ?: onGetSessionsForOffset(selectedWeekOffset)
        }
    }

    val dailyDates = remember(selectedWeekOffset) {
        DateUtils.getWeekDailyDates(selectedWeekOffset)
    }

    val totalLoggedSeconds = remember(activeWeekSessions) {
        activeWeekSessions.sumOf { it.durationSeconds }
    }
    val totalLoggedMinutes = totalLoggedSeconds / 60
    val targetWeeklyGoalSeconds = (targetWeeklyMinutes * 60f).coerceAtLeast(1f) // 9000s FIXED DENOMINATOR

    val progressFraction = (totalLoggedSeconds.toFloat() / targetWeeklyGoalSeconds).coerceIn(0f, 1f)
    val percentageInt = (progressFraction * 100).toInt()
    val minutesLeft = (targetWeeklyMinutes - totalLoggedMinutes).coerceAtLeast(0)
    val isGoalCompleted = totalLoggedMinutes >= targetWeeklyMinutes

    // Grouping by ActivityType for Weekly Share & Composition (Memoized)
    val activityBreakdown = remember(activeWeekSessions) {
        activeWeekSessions
            .groupBy { it.activityType }
            .map { (actType, list) ->
                DayActivityGroup(
                    activityType = actType,
                    totalSeconds = list.sumOf { it.durationSeconds },
                    sessions = list
                )
            }
            .sortedByDescending { it.totalSeconds }
    }

    // Pre-compute per-day sessions & grouped activities once per week/session change (Zero filter/sort during scroll)
    val daySummaryMap = remember(activeWeekSessions, dailyDates) {
        dailyDates.associate { (dayName, _, isoDate) ->
            val sessionsForDay = activeWeekSessions.filter {
                if (it.dateString.isNotEmpty()) it.dateString == isoDate else it.dayOfWeek.equals(dayName, ignoreCase = true)
            }
            val aggregated = sessionsForDay
                .groupBy { it.activityType }
                .map { (actType, list) ->
                    DayActivityGroup(
                        activityType = actType,
                        totalSeconds = list.sumOf { it.durationSeconds },
                        sessions = list
                    )
                }
                .sortedByDescending { it.totalSeconds }
            val dayTotalSecs = sessionsForDay.sumOf { it.durationSeconds }
            isoDate to Triple(sessionsForDay, aggregated, dayTotalSecs)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(top = 16.dp)
    ) {
        // 1. WEEK NAVIGATION HEADER WITH < and > BUTTONS (Requirement #4)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Previous Week Button (<)
            IconButton(
                onClick = { selectedWeekOffset -= 1 },
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color(0xFF1E2129))
                    .size(38.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ChevronLeft,
                    contentDescription = "Previous Week",
                    tint = Color.White
                )
            }

            // Center Week Date Range Indicator with Slide Animation
            AnimatedContent(
                targetState = selectedWeekOffset,
                transitionSpec = {
                    if (targetState > initialState) {
                        (slideInHorizontally { width -> width } + fadeIn()).togetherWith(slideOutHorizontally { width -> -width } + fadeOut())
                    } else {
                        (slideInHorizontally { width -> -width } + fadeIn()).togetherWith(slideOutHorizontally { width -> width } + fadeOut())
                    }
                },
                label = "weekDateHeaderTransition"
            ) { offset ->
                val rangeStr = remember(offset) { DateUtils.getWeekDateRangeString(offset) }
                val offsetSessions = remember(offset, weeklySessions, historicalWeeks) {
                    if (offset == 0) {
                        weeklySessions
                    } else {
                        historicalWeeks.find { it.weekOffset == offset }?.sessions
                            ?: onGetSessionsForOffset(offset)
                    }
                }
                val isOffsetGoalCompleted = offsetSessions.sumOf { it.durationSeconds } >= (targetWeeklyMinutes * 60)

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFF1A1C23))
                            .border(
                                1.dp,
                                if (isOffsetGoalCompleted) Color(0xFF4ADE80).copy(alpha = 0.5f) else PrimaryOrange.copy(alpha = 0.4f),
                                RoundedCornerShape(20.dp)
                            )
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = rangeStr,
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    val weekLabel = when (offset) {
                        0 -> "Current Week (Live)"
                        -1 -> "Last Week"
                        else -> "${-offset} Weeks Ago"
                    }
                    Text(
                        text = weekLabel,
                        color = if (offset == 0) (if (isOffsetGoalCompleted) Color(0xFF4ADE80) else PrimaryOrange) else SecondarySlate,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Next Week Button (>) - disabled if on current week (cannot navigate to future)
            IconButton(
                onClick = {
                    if (selectedWeekOffset < 0) {
                        selectedWeekOffset += 1
                    }
                },
                enabled = selectedWeekOffset < 0,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(if (selectedWeekOffset < 0) Color(0xFF1E2129) else Color(0xFF14161C))
                    .size(38.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Next Week",
                    tint = if (selectedWeekOffset < 0) Color.White else SecondarySlate.copy(alpha = 0.3f)
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
            item(key = "weekly_summary_card", contentType = "summary_card") {
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

                            val durationSummaryText = when {
                                totalLoggedSeconds < 60 && totalLoggedSeconds > 0 -> "${totalLoggedSeconds}s"
                                else -> "${totalLoggedMinutes} min"
                            }

                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = durationSummaryText,
                                    color = if (isGoalCompleted) Color(0xFF4ADE80) else Color.White,
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "/ $targetWeeklyMinutes min goal",
                                    color = SecondarySlate,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            val statusText = when {
                                totalLoggedSeconds < 9000 -> "$minutesLeft min remaining"
                                totalLoggedSeconds == 9000 -> "✓ Goal reached"
                                else -> "✓ Goal exceeded (+${(totalLoggedSeconds - 9000) / 60} min)"
                            }
                            val statusColor = if (isGoalCompleted) Color(0xFF4ADE80) else SecondarySlate

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
                                    color = if (isGoalCompleted) Color(0xFF4ADE80) else PrimaryOrange,
                                    startAngle = -90f,
                                    sweepAngle = 360f * progressFraction,
                                    useCenter = false,
                                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                                )
                            }

                            Text(
                                text = "$percentageInt%",
                                color = if (isGoalCompleted) Color(0xFF4ADE80) else PrimaryOrange,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // 3. WORKOUT SHARE & DISTRIBUTION (Redesigned Full-Width Proportional Bar + Per-Sport Share Rows)
            if (activityBreakdown.isNotEmpty() && totalLoggedSeconds > 0) {
                item(key = "workout_share_card", contentType = "share_card") {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder)
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            // Header Row with Active Sport Count Badge
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "WORKOUT SHARE & DISTRIBUTION",
                                    color = SecondarySlate,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF1E222D))
                                        .border(1.dp, SurfaceCardBorder, RoundedCornerShape(8.dp))
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = "${activityBreakdown.size} Olahraga • ${activeWeekSessions.size} Sesi",
                                        color = Color.White.copy(alpha = 0.85f),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Full-Width 100% Proportional Multi-Segment Composition Bar
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(28.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(ProgressSegmentTrack),
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                activityBreakdown.forEach { group ->
                                    val shareFraction = (group.totalSeconds.toFloat() / totalLoggedSeconds.toFloat()).coerceIn(0.02f, 1f)
                                    val sharePctInt = ((group.totalSeconds.toFloat() / totalLoggedSeconds.toFloat()) * 100f).toInt().coerceAtLeast(1)

                                    Box(
                                        modifier = Modifier
                                            .weight(shareFraction)
                                            .height(28.dp)
                                            .background(group.activityType.themeColor),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (shareFraction >= 0.22f) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = group.activityType.iconEmoji,
                                                    fontSize = 12.sp
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "$sharePctInt%",
                                                    color = Color.White,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        } else if (shareFraction >= 0.08f) {
                                            Text(
                                                text = group.activityType.iconEmoji,
                                                fontSize = 12.sp
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Structured Per-Sport Share Rows with Mini Proportional Progress Bars
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                activityBreakdown.forEach { group ->
                                    val activity = group.activityType
                                    val seconds = group.totalSeconds
                                    val shareFraction = (seconds.toFloat() / totalLoggedSeconds.toFloat()).coerceIn(0f, 1f)
                                    val sharePct = (shareFraction * 100f).toInt().coerceAtLeast(1)

                                    val goalPct = (seconds.toFloat() / targetWeeklyGoalSeconds * 100f)
                                    val goalPctStr = if (goalPct < 1.0f && goalPct > 0.0f) "<1%" else "${goalPct.toInt()}%"

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color(0xFF13151B))
                                            .border(1.dp, SurfaceCardBorder.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Sport Icon Badge
                                        Box(
                                            modifier = Modifier
                                                .size(34.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(activity.themeColor.copy(alpha = 0.16f))
                                                .border(1.dp, activity.themeColor.copy(alpha = 0.45f), RoundedCornerShape(10.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(text = activity.iconEmoji, fontSize = 16.sp)
                                        }

                                        Spacer(modifier = Modifier.width(10.dp))

                                        // Center: Name, Session Count & Mini Share Progress Bar
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        text = activity.displayName,
                                                        color = Color.White,
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = "${group.sessions.size}x sesi",
                                                        color = SecondarySlate,
                                                        fontSize = 11.sp
                                                    )
                                                }

                                                Text(
                                                    text = formatDurationSeconds(seconds),
                                                    color = Color.White,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }

                                            Spacer(modifier = Modifier.height(6.dp))

                                            // Mini Proportional Bar + Share & Goal % Labels
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .height(6.dp)
                                                        .clip(RoundedCornerShape(3.dp))
                                                        .background(ProgressSegmentTrack)
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxWidth(shareFraction.coerceIn(0.03f, 1f))
                                                            .height(6.dp)
                                                            .clip(RoundedCornerShape(3.dp))
                                                            .background(activity.themeColor)
                                                    )
                                                }

                                                Spacer(modifier = Modifier.width(10.dp))

                                                Text(
                                                    text = "$sharePct% share • $goalPctStr goal",
                                                    color = activity.themeColor,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.SemiBold
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

            // 4. DAILY TIMELINE LOG HEADER & QUICK ADD MANUAL BUTTON (Requirement #5)
            item(key = "daily_timeline_header", contentType = "timeline_header") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "DAILY TIMELINE LOG",
                        color = SecondarySlate,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    // Quick Add Manual Button
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF222631))
                            .border(1.dp, PrimaryOrange.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .clickable { showManualAddDialog = true }
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Manual",
                            tint = PrimaryOrange,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Add Manual",
                            color = PrimaryOrange,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // 5. DAILY TIMELINE LOG WITH STACKED BAR & COLLAPSIBLE 3D CARD STACK
            items(
                items = dailyDates,
                key = { it.third },
                contentType = { "daily_timeline_card" }
            ) { (dayName, displayDate, isoDate) ->
                val dayData = daySummaryMap[isoDate]
                val sessionsForDay = dayData?.first ?: emptyList()
                val aggregatedByActivity = dayData?.second ?: emptyList()
                val dayTotalSeconds = dayData?.third ?: 0

                var isDayStackExpanded by remember(isoDate, selectedWeekOffset) { mutableStateOf(false) }
                var expandedSubActivity by remember(isoDate, selectedWeekOffset) { mutableStateOf<ActivityType?>(null) }

                val hasActivity = sessionsForDay.isNotEmpty()
                val cardBorderColor = if (hasActivity) {
                    if (isGoalCompleted) Color(0xFF4ADE80).copy(alpha = 0.4f) else PrimaryOrange.copy(alpha = 0.4f)
                } else SurfaceCardBorder

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                    border = androidx.compose.foundation.BorderStroke(1.dp, cardBorderColor)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .animateContentSize(
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioNoBouncy,
                                    stiffness = Spring.StiffnessMedium
                                )
                            )
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
                                    text = "($displayDate)",
                                    color = SecondarySlate,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Normal
                                )
                            }

                            if (dayTotalSeconds > 0) {
                                val formattedDayTotal = if (dayTotalSeconds < 60) "${dayTotalSeconds}s total" else "${dayTotalSeconds / 60} min total"
                                Text(
                                    text = formattedDayTotal,
                                    color = if (isGoalCompleted) Color(0xFF4ADE80) else PrimaryOrange,
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
                            Spacer(modifier = Modifier.height(10.dp))

                            // A. Daily Proportional Stacked Horizontal Bar (100% of that day's workout)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(20.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(ProgressSegmentTrack),
                                horizontalArrangement = Arrangement.spacedBy(1.dp)
                            ) {
                                aggregatedByActivity.forEach { group ->
                                    val fraction = (group.totalSeconds.toFloat() / dayTotalSeconds.toFloat()).coerceIn(0f, 1f)
                                    if (fraction > 0f) {
                                        Box(
                                            modifier = Modifier
                                                .weight(fraction)
                                                .height(20.dp)
                                                .background(group.activityType.themeColor),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (fraction >= 0.10f) {
                                                Text(
                                                    text = group.activityType.iconEmoji,
                                                    fontSize = 11.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // B. Single Session vs. Multi-Session 3D Collapsible Stack
                            if (sessionsForDay.size == 1) {
                                val singleSession = sessionsForDay.first()
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFF13151B))
                                        .border(1.dp, SurfaceCardBorder, RoundedCornerShape(10.dp))
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(text = singleSession.activityType.iconEmoji, fontSize = 16.sp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = singleSession.activityType.displayName,
                                                color = Color.White,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "${singleSession.formattedTime} • ${singleSession.formatDurationString()}",
                                                color = SecondarySlate,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }

                                    DeletePillButton(
                                        onClick = { onDeleteSession(singleSession.id) },
                                        compact = false
                                    )
                                }
                            } else if (!isDayStackExpanded) {
                                // COLLAPSED 3D CARD STACK DECK VIEW
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { isDayStackExpanded = true },
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    // Top Front Summary Card of the Stack
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color(0xFF13151B))
                                            .border(
                                                1.dp,
                                                PrimaryOrange.copy(alpha = 0.35f),
                                                RoundedCornerShape(12.dp)
                                            )
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            // Stacked Emoji Badges (up to 3 unique activity icons)
                                            Row(horizontalArrangement = Arrangement.spacedBy((-6).dp)) {
                                                aggregatedByActivity.take(3).forEach { group ->
                                                    Box(
                                                        modifier = Modifier
                                                            .size(28.dp)
                                                            .clip(CircleShape)
                                                            .background(Color(0xFF1E222D))
                                                            .border(1.dp, group.activityType.themeColor.copy(alpha = 0.7f), CircleShape),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(text = group.activityType.iconEmoji, fontSize = 13.sp)
                                                    }
                                                }
                                            }

                                            Spacer(modifier = Modifier.width(10.dp))

                                            Column {
                                                val stackTitle = if (aggregatedByActivity.size == 1) {
                                                    "${aggregatedByActivity.first().activityType.displayName} (${sessionsForDay.size}x sesi digabung)"
                                                } else {
                                                    aggregatedByActivity.joinToString(", ") { it.activityType.displayName }
                                                }

                                                Text(
                                                    text = stackTitle,
                                                    color = Color.White,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1
                                                )
                                                Text(
                                                    text = "${aggregatedByActivity.size} olahraga • ${sessionsForDay.size} sesi • Tap buka & hapus",
                                                    color = SecondarySlate,
                                                    fontSize = 11.sp
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(8.dp))

                                        // Stack Count Pill Badge
                                        Row(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color(0xFF222631))
                                                .border(1.dp, PrimaryOrange.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                                .padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Stack (${sessionsForDay.size})",
                                                color = PrimaryOrange,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Icon(
                                                imageVector = Icons.Default.KeyboardArrowDown,
                                                contentDescription = "Expand Stack",
                                                tint = PrimaryOrange,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }

                                    // 2nd Peeking Card Layer Underneath (3D Deck Effect)
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 10.dp)
                                            .height(6.dp)
                                            .clip(RoundedCornerShape(bottomStart = 10.dp, bottomEnd = 10.dp))
                                            .background(Color(0xFF13151B).copy(alpha = 0.75f))
                                            .border(
                                                1.dp,
                                                SurfaceCardBorder.copy(alpha = 0.7f),
                                                RoundedCornerShape(bottomStart = 10.dp, bottomEnd = 10.dp)
                                            )
                                    )

                                    // 3rd Peeking Card Layer Underneath (when >= 3 sessions)
                                    if (sessionsForDay.size >= 3) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 20.dp)
                                                .height(5.dp)
                                                .clip(RoundedCornerShape(bottomStart = 10.dp, bottomEnd = 10.dp))
                                                .background(Color(0xFF13151B).copy(alpha = 0.45f))
                                                .border(
                                                    1.dp,
                                                    SurfaceCardBorder.copy(alpha = 0.4f),
                                                    RoundedCornerShape(bottomStart = 10.dp, bottomEnd = 10.dp)
                                                )
                                        )
                                    }
                                }
                            } else {
                                // EXPANDED STACK VIEW (Grouped by ActivityType + Expandable Sub-Sessions)
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    // Collapse Stack Header Bar
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 2.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "STACK OLAHRAGA (${aggregatedByActivity.size} JENIS • ${sessionsForDay.size} SESI)",
                                            color = SecondarySlate,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 0.5.sp
                                        )

                                        Row(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color(0xFF222631))
                                                .clickable {
                                                    isDayStackExpanded = false
                                                    expandedSubActivity = null
                                                }
                                                .padding(horizontal = 8.dp, vertical = 3.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Tutup Stack",
                                                color = PrimaryOrange,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Icon(
                                                imageVector = Icons.Default.KeyboardArrowUp,
                                                contentDescription = "Collapse Stack",
                                                tint = PrimaryOrange,
                                                modifier = Modifier.size(15.dp)
                                            )
                                        }
                                    }

                                    // Merged Activity Cards (1 card per ActivityType)
                                    aggregatedByActivity.forEach { group ->
                                        val isSubExpanded = expandedSubActivity == group.activityType
                                        val hasMultipleSubSessions = group.sessions.size > 1

                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(Color(0xFF13151B))
                                                .border(
                                                    1.dp,
                                                    group.activityType.themeColor.copy(alpha = 0.35f),
                                                    RoundedCornerShape(10.dp)
                                                )
                                                .padding(horizontal = 10.dp, vertical = 8.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable(enabled = hasMultipleSubSessions) {
                                                        expandedSubActivity = if (isSubExpanded) null else group.activityType
                                                    },
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(28.dp)
                                                            .clip(CircleShape)
                                                            .background(group.activityType.themeColor.copy(alpha = 0.18f)),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(text = group.activityType.iconEmoji, fontSize = 15.sp)
                                                    }
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Column {
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            Text(
                                                                text = group.activityType.displayName,
                                                                color = Color.White,
                                                                fontSize = 13.sp,
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                            Spacer(modifier = Modifier.width(6.dp))
                                                            Text(
                                                                text = "• ${formatDurationSeconds(group.totalSeconds)}",
                                                                color = group.activityType.themeColor,
                                                                fontSize = 12.sp,
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                        }

                                                        val subLabel = if (hasMultipleSubSessions) {
                                                            "${group.sessions.size}x sesi digabung (${group.sessions.first().formattedTime}–${group.sessions.last().formattedTime})"
                                                        } else {
                                                            "Pukul ${group.sessions.first().formattedTime}"
                                                        }
                                                        Text(
                                                            text = subLabel,
                                                            color = SecondarySlate,
                                                            fontSize = 11.sp
                                                        )
                                                    }
                                                }

                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    if (hasMultipleSubSessions) {
                                                        Row(
                                                            modifier = Modifier
                                                                .clip(RoundedCornerShape(6.dp))
                                                                .background(Color(0xFF1E222D))
                                                                .clickable {
                                                                    expandedSubActivity = if (isSubExpanded) null else group.activityType
                                                                }
                                                                .padding(horizontal = 6.dp, vertical = 3.dp),
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Text(
                                                                text = "${group.sessions.size}x",
                                                                color = SecondarySlate,
                                                                fontSize = 10.sp,
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                            Icon(
                                                                imageVector = if (isSubExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                                                contentDescription = "Toggle Sub-sessions",
                                                                tint = SecondarySlate,
                                                                modifier = Modifier.size(14.dp)
                                                            )
                                                        }
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                    }

                                                    // Delete entire ActivityType group (with confirmation if multi-session)
                                                    IconButton(
                                                        onClick = {
                                                            if (hasMultipleSubSessions) {
                                                                pendingDeleteGroup = group
                                                            } else {
                                                                group.sessions.forEach { s -> onDeleteSession(s.id) }
                                                            }
                                                        },
                                                        modifier = Modifier.size(28.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Delete,
                                                            contentDescription = "Delete Activity Group",
                                                            tint = Color(0xFFEF4444).copy(alpha = 0.85f),
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                }
                                            }

                                            // Expandable Sub-Sessions List (Handled cleanly by parent animateContentSize for 60fps)
                                            if (isSubExpanded && hasMultipleSubSessions) {
                                                Column(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(top = 8.dp, start = 12.dp),
                                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    group.sessions.forEachIndexed { idx, subSession ->
                                                        Row(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .clip(RoundedCornerShape(8.dp))
                                                                .background(Color(0xFF1A1D26))
                                                                .padding(horizontal = 10.dp, vertical = 6.dp),
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Text(
                                                                text = "#${idx + 1} • Pukul ${subSession.formattedTime} • ${subSession.formatDurationString()}",
                                                                color = SecondarySlate,
                                                                fontSize = 11.sp
                                                            )
                                                            DeletePillButton(
                                                                onClick = { onDeleteSession(subSession.id) },
                                                                compact = true
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
            }

            item(key = "bottom_spacer") {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    // QUICK ADD MANUAL WORKOUT DIALOG (Requirement #5)
    if (showManualAddDialog) {
        var selectedActivity by remember { mutableStateOf(ActivityType.RUNNING) }
        var inputMinutesText by remember { mutableStateOf("30") }
        var selectedDayTriple by remember { mutableStateOf(dailyDates.firstOrNull() ?: Triple("Monday", "Sep 28", "2026-09-28")) }

        Dialog(onDismissRequest = { showManualAddDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = NeutralDark),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, PrimaryOrange.copy(alpha = 0.7f))
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
                            text = "QUICK ADD MANUAL WORKOUT",
                            color = PrimaryOrange,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        TextButton(onClick = { showManualAddDialog = false }) {
                            Text(text = "Close", color = SecondarySlate, fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(text = "1. Select Activity", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(6.dp))

                    // Activity selection chips
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ActivityType.entries.forEach { act ->
                            val isSelected = act == selectedActivity
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) act.themeColor else SurfaceChip)
                                    .border(1.dp, if (isSelected) act.themeColor else SurfaceCardBorder, RoundedCornerShape(12.dp))
                                    .clickable { selectedActivity = act }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "${act.iconEmoji} ${act.displayName}",
                                    color = if (isSelected) Color.White else SecondarySlate,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(text = "2. Duration (Minutes)", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(6.dp))

                    // Preset duration buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(15, 30, 45, 60).forEach { mins ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (inputMinutesText == "$mins") PrimaryOrange else Color(0xFF1E2129))
                                    .clickable { inputMinutesText = "$mins" }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${mins}m",
                                    color = if (inputMinutesText == "$mins") Color.White else SecondarySlate,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = inputMinutesText,
                        onValueChange = { inputMinutesText = it.filter { ch -> ch.isDigit() } },
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

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(text = "3. Day of Week", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(6.dp))

                    // Day of week selection row
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(dailyDates) { dayTriple ->
                            val isSelected = dayTriple.third == selectedDayTriple.third
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) PrimaryOrange else Color(0xFF1E2129))
                                    .clickable { selectedDayTriple = dayTriple }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "${dayTriple.first.take(3)} (${dayTriple.second})",
                                    color = if (isSelected) Color.White else SecondarySlate,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Submit Button
                    Button(
                        onClick = {
                            val mins = inputMinutesText.toIntOrNull() ?: 0
                            if (mins > 0) {
                                onAddManualSession(
                                    selectedActivity,
                                    mins * 60,
                                    selectedDayTriple.third,
                                    selectedDayTriple.first
                                )
                                showManualAddDialog = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text(text = "Save Workout", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        }
    }

    pendingDeleteGroup?.let { groupToDelete ->
        AlertDialog(
            onDismissRequest = { pendingDeleteGroup = null },
            containerColor = NeutralDark,
            title = {
                Text(
                    text = "Hapus ${groupToDelete.sessions.size}x Sesi ${groupToDelete.activityType.displayName}?",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Semua ${groupToDelete.sessions.size} log (${formatDurationSeconds(groupToDelete.totalSeconds)}) pada grup ini akan dihapus. Untuk menghapus 1 sesi saja, ketuk \"${groupToDelete.sessions.size}x ⌄\" lalu pilih Hapus pada log terkait.",
                    color = SecondarySlate
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        groupToDelete.sessions.forEach { s -> onDeleteSession(s.id) }
                        pendingDeleteGroup = null
                    }
                ) {
                    Text(text = "Hapus Grup", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteGroup = null }) {
                    Text(text = "Batal", color = SecondarySlate)
                }
            }
        )
    }
}

@Composable
private fun DeletePillButton(
    onClick: () -> Unit,
    compact: Boolean
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF2C1919))
            .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.45f), RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(
                horizontal = if (compact) 7.dp else 9.dp,
                vertical = if (compact) 3.dp else 5.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Delete,
            contentDescription = "Hapus Log",
            tint = Color(0xFFEF4444),
            modifier = Modifier.size(if (compact) 12.dp else 14.dp)
        )
        Spacer(modifier = Modifier.width(3.dp))
        Text(
            text = "Hapus",
            color = Color(0xFFEF4444),
            fontSize = if (compact) 10.sp else 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

private data class DayActivityGroup(
    val activityType: ActivityType,
    val totalSeconds: Int,
    val sessions: List<WorkoutSessionItem>
)

private fun formatDurationSeconds(durationSeconds: Int): String {
    if (durationSeconds < 60) {
        return "${durationSeconds}s"
    }
    val mins = durationSeconds / 60
    val secs = durationSeconds % 60
    return if (secs > 0) "${mins}m ${secs}s" else "${mins}m"
}

