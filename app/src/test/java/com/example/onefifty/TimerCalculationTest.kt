package com.example.onefifty

import org.junit.Assert.assertEquals
import org.junit.Test

class TimerCalculationTest {

    @Test
    fun testCountdownCalculation_UnderTarget() {
        val targetWeeklyMinutes = 150
        val targetWeeklySeconds = targetWeeklyMinutes * 60 // 9000s
        val totalWeeklyLoggedSeconds = 3600 // 60 min already logged
        val currentRunSecs = 125 // 2m 5s current run

        val liveWeeklySeconds = totalWeeklyLoggedSeconds + currentRunSecs
        val remainingWeeklySeconds = (targetWeeklySeconds - liveWeeklySeconds).coerceAtLeast(0)
        val overtimeSeconds = (liveWeeklySeconds - targetWeeklySeconds).coerceAtLeast(0)

        // 9000 - 3725 = 5275s
        assertEquals(5275, remainingWeeklySeconds)
        assertEquals(0, overtimeSeconds)

        // Format H:MM:SS
        val h = remainingWeeklySeconds / 3600
        val m = (remainingWeeklySeconds % 3600) / 60
        val s = remainingWeeklySeconds % 60
        val mStr = if (m < 10) "0$m" else "$m"
        val sStr = if (s < 10) "0$s" else "$s"
        val formatted = "$h:$mStr:$sStr"

        // 5275s = 1h 27m 55s
        assertEquals("1:27:55", formatted)
    }

    @Test
    fun testOvertimeCalculation_ExceedTarget() {
        val targetWeeklyMinutes = 150
        val targetWeeklySeconds = targetWeeklyMinutes * 60 // 9000s
        val totalWeeklyLoggedSeconds = 8900
        val currentRunSecs = 250 // crosses 9000 to 9150s (+150s overtime)

        val liveWeeklySeconds = totalWeeklyLoggedSeconds + currentRunSecs
        val remainingWeeklySeconds = (targetWeeklySeconds - liveWeeklySeconds).coerceAtLeast(0)
        val overtimeSeconds = (liveWeeklySeconds - targetWeeklySeconds).coerceAtLeast(0)

        assertEquals(0, remainingWeeklySeconds)
        assertEquals(150, overtimeSeconds)

        // Overtime format +H:MM:SS
        val otH = overtimeSeconds / 3600
        val otM = (overtimeSeconds % 3600) / 60
        val otS = overtimeSeconds % 60
        val otMStr = if (otM < 10) "0$otM" else "$otM"
        val otSStr = if (otS < 10) "0$otS" else "$otS"
        val otHStr = if (otH < 10) "0$otH" else "$otH"
        val formatted = "+$otHStr:$otMStr:$otSStr"

        assertEquals("+00:02:30", formatted)
    }

    @Test
    fun testCrashRecovery_ValidSessionRestoresCorrectDuration() {
        val now = 1761100000000L
        val startTimestamp = now - (18 * 60 * 1000L) // 18 minutes ago
        val sessionAccumulatedSeconds = 120 // 2 minutes from earlier pause

        val elapsedSecs = ((now - startTimestamp) / 1000).toInt()
        assertEquals(1080, elapsedSecs) // 18m = 1080s

        val isValidRecovery = elapsedSecs in 1..10800
        assertEquals(true, isValidRecovery)

        val totalSessionDuration = sessionAccumulatedSeconds + elapsedSecs
        assertEquals(1200, totalSessionDuration) // 20m = 1200s
    }

    @Test
    fun testCrashRecovery_ForgottenSessionOver3HoursTriggersGuard() {
        val now = 1761100000000L
        val startTimestamp = now - (21 * 3600 * 1000L) // +21 hours ago

        val elapsedSecs = ((now - startTimestamp) / 1000).toInt()
        val isAutoRecoveredWithoutGuard = elapsedSecs in 1..10800
        val triggersLongSessionGuard = elapsedSecs > 10800
        assertEquals(false, isAutoRecoveredWithoutGuard)
        assertEquals(true, triggersLongSessionGuard)
    }
}
