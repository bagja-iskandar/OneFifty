package com.example.onefifty.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.onefifty.ui.components.TempoTopBar
import com.example.onefifty.ui.model.WorkoutSessionItem
import com.example.onefifty.ui.theme.NeutralDark
import com.example.onefifty.ui.theme.PrimaryOrange
import com.example.onefifty.ui.theme.SecondarySlate
import com.example.onefifty.ui.theme.SurfaceCard
import com.example.onefifty.ui.theme.SurfaceCardBorder

@Composable
fun SettingsScreen(
    targetWeeklyMinutes: Int,
    currentWeeklyMinutes: Int = 0,
    weeklySessions: List<WorkoutSessionItem> = emptyList(),
    keepScreenOn: Boolean,
    onKeepScreenOnChanged: (Boolean) -> Unit,
    onClearCurrentWeek: () -> Unit = {},
    onClearAllData: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showResetWeekDialog by remember { mutableStateOf(false) }
    var showResetAllDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp)
    ) {
        TempoTopBar(
            currentWeeklyMinutes = currentWeeklyMinutes,
            targetWeeklyMinutes = targetWeeklyMinutes,
            subTitle = "SETTINGS"
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        ) {
            Text(
                text = "SETTINGS & LOCAL DATA",
                color = SecondarySlate,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 1. Weekly Target Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Weekly Target",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Recommended by WHO / AHA",
                            color = SecondarySlate,
                            fontSize = 12.sp
                        )
                    }

                    Text(
                        text = "$targetWeeklyMinutes min",
                        color = PrimaryOrange,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 2. Keep Screen On Toggle Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Keep Screen On During Session",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Prevent phone lock while workout timer is active",
                            color = SecondarySlate,
                            fontSize = 12.sp
                        )
                    }

                    Switch(
                        checked = keepScreenOn,
                        onCheckedChange = onKeepScreenOnChanged,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = PrimaryOrange,
                            uncheckedThumbColor = SecondarySlate,
                            uncheckedTrackColor = SurfaceCardBorder
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 3. Stored Data Inspector Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Text(
                        text = "STORED LOCAL DATA INSPECTOR",
                        color = PrimaryOrange,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Logged Sessions:", color = SecondarySlate, fontSize = 13.sp)
                        Text(text = "${weeklySessions.size} sessions", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Logged Minutes:", color = SecondarySlate, fontSize = 13.sp)
                        Text(text = "$currentWeeklyMinutes min / $targetWeeklyMinutes min", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Local Storage Footprint:", color = SecondarySlate, fontSize = 13.sp)
                        Text(text = "< 0.2 KB (SharedPreferences)", color = Color(0xFF4ADE80), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 4. Reset Current Week Only Button (Preserves past week archives)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showResetWeekDialog = true },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF282016)),
                border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryOrange.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "🔄  Reset Progress Minggu Ini Saja",
                        color = PrimaryOrange,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Mengosongkan log minggu aktif tanpa menghapus arsip minggu lalu",
                        color = SecondarySlate,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 5. Danger Action: Clear All Local Data & Archives Button
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showResetAllDialog = true },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2C1919)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🗑️  Clear All Data & Archives (Reset All)",
                        color = Color(0xFFEF4444),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 6. About App Info Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Text(
                        text = "About TEMPO 150",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Simple workout countdown recorder for personal weekly goal tracking.",
                        color = SecondarySlate,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }

    if (showResetWeekDialog) {
        ConfirmDestructiveDialog(
            title = "Reset Progress Minggu Ini?",
            message = "Semua log olahraga di minggu berjalan akan dihapus ke 0 menit, sedangkan arsip minggu-minggu sebelumnya tetap tersimpan aman.",
            confirmLabel = "Ya, Reset Minggu Ini",
            confirmColor = PrimaryOrange,
            onConfirm = {
                onClearCurrentWeek()
                showResetWeekDialog = false
            },
            onDismiss = { showResetWeekDialog = false }
        )
    }

    if (showResetAllDialog) {
        ConfirmDestructiveDialog(
            title = "Clear All Data & Archives?",
            message = "Ini akan menghapus permanen seluruh riwayat minggu lalu sekaligus mereset progress minggu ini ke 0 menit.",
            confirmLabel = "Ya, Hapus Semua",
            confirmColor = Color(0xFFEF4444),
            onConfirm = {
                onClearAllData()
                showResetAllDialog = false
            },
            onDismiss = { showResetAllDialog = false }
        )
    }
}

@Composable
private fun ConfirmDestructiveDialog(
    title: String,
    message: String,
    confirmLabel: String,
    confirmColor: Color,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NeutralDark,
        title = {
            Text(text = title, color = Color.White, fontWeight = FontWeight.Bold)
        },
        text = {
            Text(text = message, color = SecondarySlate)
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = confirmLabel, color = confirmColor, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Batal", color = SecondarySlate)
            }
        }
    )
}
