package com.example.onefifty.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.onefifty.ui.model.WorkoutSessionItem
import com.example.onefifty.ui.theme.PrimaryOrange
import com.example.onefifty.ui.theme.ProgressSegmentTrack
import com.example.onefifty.ui.theme.SecondarySlate
import com.example.onefifty.ui.theme.SurfaceCard
import com.example.onefifty.ui.theme.SurfaceCardBorder

@Composable
fun WeeklyBreakdownCard(
    weeklySessions: List<WorkoutSessionItem>,
    modifier: Modifier = Modifier
) {
    val totalLoggedSeconds = weeklySessions.sumOf { it.durationSeconds }
    val totalLoggedMinutes = totalLoggedSeconds / 60
    val targetWeeklyGoalSeconds = 9000f // 150 minutes = 9000 seconds FIXED DENOMINATOR

    // Top 5 breakdown sorted by duration
    val activityBreakdown = weeklySessions.groupBy { it.activityType }
        .mapValues { (_, sessions) -> sessions.sumOf { it.durationSeconds } }
        .toList()
        .sortedByDescending { it.second }
        .take(5)

    if (activityBreakdown.isEmpty()) return

    val hrs = totalLoggedMinutes / 60
    val mins = totalLoggedMinutes % 60
    val minsStr = if (mins < 10) "0$mins" else "$mins"
    val totalFormatted = if (totalLoggedSeconds < 60) "${totalLoggedSeconds}s Total" else "$hrs:$minsStr Total"

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header Row with Total Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "WEEKLY BREAKDOWN",
                    color = SecondarySlate,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF222631))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = totalFormatted,
                        color = PrimaryOrange,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            activityBreakdown.forEach { (activity, seconds) ->
                // Distribution calculated against 150 min (9000s) DENOMINATOR
                val percentage = (seconds.toFloat() / targetWeeklyGoalSeconds * 100f)
                val percentageFormatted = if (percentage < 1.0f && percentage > 0.0f) {
                    "<1%"
                } else {
                    "${percentage.toInt()}%"
                }

                Column(modifier = Modifier.padding(vertical = 5.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF222631)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = activity.iconEmoji,
                                    fontSize = 13.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = activity.displayName,
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        val durText = if (seconds < 60) "${seconds}s" else "${seconds / 60} min"
                        Text(
                            text = "$durText • $percentageFormatted",
                            color = activity.themeColor,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(5.dp))

                    // Thick Progress Bar using fixed activity theme color
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(ProgressSegmentTrack)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth((seconds.toFloat() / targetWeeklyGoalSeconds).coerceIn(0f, 1f))
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(activity.themeColor)
                        )
                    }
                }
            }
        }
    }
}
