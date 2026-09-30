package com.example.onefifty.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.onefifty.ui.theme.PrimaryOrange
import com.example.onefifty.ui.theme.ProgressSegmentTrack
import com.example.onefifty.ui.theme.SecondarySlate
import com.example.onefifty.ui.theme.SurfaceCardBorder
import com.example.onefifty.util.DateUtils

@Composable
fun TempoTopBar(
    currentWeeklyMinutes: Int = 0,
    targetWeeklyMinutes: Int = 150,
    dateFormattedText: String = DateUtils.getCurrentWeekDateRangeString(),
    modifier: Modifier = Modifier
) {
    val progressFraction = (currentWeeklyMinutes.toFloat() / targetWeeklyMinutes.toFloat()).coerceIn(0f, 1f)
    val percentageInt = (progressFraction * 100).toInt()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Logo with Dynamic Live Progress Ring
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF13151B)),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize().padding(3.dp)) {
                    val strokeWidth = 3.dp.toPx()
                    drawCircle(
                        color = ProgressSegmentTrack,
                        style = Stroke(width = strokeWidth)
                    )
                    drawArc(
                        color = PrimaryOrange,
                        startAngle = -90f,
                        sweepAngle = 360f * progressFraction,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }

                Text(
                    text = "$percentageInt%",
                    color = PrimaryOrange,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column {
                Text(
                    text = "TEMPO 150",
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = "SESSION",
                    color = SecondarySlate,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp
                )
            }
        }

        // Real-Time Calendar Date Badge (e.g., "28 Sep – 4 Oct, 2025")
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF1A1C23))
                .border(1.dp, SurfaceCardBorder, RoundedCornerShape(20.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = dateFormattedText,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
