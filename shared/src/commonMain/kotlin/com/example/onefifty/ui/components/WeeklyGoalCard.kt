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
import com.example.onefifty.ui.theme.PrimaryOrange
import com.example.onefifty.ui.theme.ProgressSegmentTrack
import com.example.onefifty.ui.theme.SecondarySlate
import com.example.onefifty.ui.theme.SurfaceCard
import com.example.onefifty.ui.theme.SurfaceCardBorder

@Composable
fun WeeklyGoalCard(
    currentMinutes: Int = 87,
    targetMinutes: Int = 150,
    modifier: Modifier = Modifier
) {
    val progressFraction = (currentMinutes.toFloat() / targetMinutes.toFloat()).coerceIn(0f, 1f)
    val percentDone = (progressFraction * 100).toInt()
    val minutesLeft = (targetMinutes - currentMinutes).coerceAtLeast(0)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(PrimaryOrange)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "WEEK GOAL",
                        color = SecondarySlate,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "$currentMinutes",
                        color = Color.White,
                        fontSize = 38.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = " / $targetMinutes min",
                        color = SecondarySlate,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF2A1C19))
                        .border(1.dp, PrimaryOrange.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "$percentDone% done",
                        color = PrimaryOrange,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val numSegments = 5
                val activeSegments = (progressFraction * numSegments).toInt()
                val partialSegmentFraction = (progressFraction * numSegments) - activeSegments

                for (i in 0 until numSegments) {
                    val segmentColor = when {
                        i < activeSegments -> PrimaryOrange
                        i == activeSegments && partialSegmentFraction > 0 -> PrimaryOrange.copy(alpha = 0.6f)
                        else -> ProgressSegmentTrack
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(segmentColor)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (minutesLeft > 0) "✓  $minutesLeft min left to reach guideline" else "🎉 Guideline completed!",
                    color = SecondarySlate,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
