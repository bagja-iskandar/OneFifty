package com.example.onefifty.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.onefifty.ui.model.ActivityType
import com.example.onefifty.ui.theme.PrimaryOrange
import com.example.onefifty.ui.theme.SecondarySlate
import com.example.onefifty.ui.theme.SurfaceCardBorder
import com.example.onefifty.ui.theme.SurfaceChip

@Composable
fun ActivitySelector(
    selectedActivity: ActivityType?,
    onActivitySelected: (ActivityType) -> Unit,
    modifier: Modifier = Modifier
) {
    val activities = ActivityType.entries
    val row1 = activities.take(4)
    val row2 = activities.drop(4).take(4)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
    ) {
        Text(
            text = "SELECT ACTIVITY",
            color = SecondarySlate,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Row 1 (4 items)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            row1.forEach { activity ->
                ActivitySquareTile(
                    activity = activity,
                    isSelected = activity == selectedActivity,
                    onSelect = { onActivitySelected(activity) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Row 2 (4 items)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            row2.forEach { activity ->
                ActivitySquareTile(
                    activity = activity,
                    isSelected = activity == selectedActivity,
                    onSelect = { onActivitySelected(activity) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun ActivitySquareTile(
    activity: ActivityType,
    isSelected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tileScale by animateFloatAsState(
        targetValue = if (isSelected) 1.05f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "tileSpringScale"
    )

    val backgroundColor = if (isSelected) PrimaryOrange else SurfaceChip
    val borderStrokeColor = if (isSelected) PrimaryOrange else SurfaceCardBorder
    val textColor = if (isSelected) Color.White else SecondarySlate

    Box(
        modifier = modifier
            .height(68.dp)
            .graphicsLayer {
                scaleX = tileScale
                scaleY = tileScale
            }
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .border(1.dp, borderStrokeColor, RoundedCornerShape(16.dp))
            .clickable { onSelect() }
            .padding(vertical = 8.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = activity.iconEmoji,
                fontSize = 18.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = activity.displayName,
                color = textColor,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1
            )
        }
    }
}
