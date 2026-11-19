package com.example.onefifty

import com.example.onefifty.util.DateUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class DateUtilsTest {

    @Test
    fun testMondayCalculationIsStrictlyMonday() {
        val monday = DateUtils.getWeekMonday(0)
        assertEquals(Calendar.MONDAY, monday.get(Calendar.DAY_OF_WEEK))
        assertEquals(0, monday.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, monday.get(Calendar.MINUTE))
        assertEquals(0, monday.get(Calendar.SECOND))
    }

    @Test
    fun testPreviousWeekOffsetIsExactly7DaysEarlier() {
        val currentMonday = DateUtils.getWeekMonday(0)
        val prevMonday = DateUtils.getWeekMonday(-1)

        val diffDays = (currentMonday.timeInMillis - prevMonday.timeInMillis) / (24 * 3600 * 1000)
        assertEquals(7L, diffDays)
        assertEquals(Calendar.MONDAY, prevMonday.get(Calendar.DAY_OF_WEEK))
    }

    @Test
    fun testMultipleWeekOffsets() {
        val currentMonday = DateUtils.getWeekMonday(0)
        val threeWeeksAgo = DateUtils.getWeekMonday(-3)

        val diffWeeks = ((currentMonday.timeInMillis - threeWeeksAgo.timeInMillis) / (7L * 24 * 3600 * 1000)).toInt()
        assertEquals(3, diffWeeks)
    }

    @Test
    fun testWeekDailyDatesHas7DaysWithValidIso() {
        val days = DateUtils.getWeekDailyDates(0)
        assertEquals(7, days.size)
        assertEquals("Monday", days[0].first)
        assertEquals("Sunday", days[6].first)

        // Verify ISO format YYYY-MM-DD
        days.forEach { (_, _, isoDate) ->
            assertTrue(isoDate.matches(Regex("\\d{4}-\\d{2}-\\d{2}")))
        }
    }

    @Test
    fun testDateRangeFormatting() {
        val range = DateUtils.getWeekDateRangeString(0)
        assertNotNull(range)
        assertTrue(range.contains("–"))
    }

    @Test
    fun testFindWeekOffsetForPastIsoDate() {
        val twoWeeksAgoWednesdayIso = DateUtils.getWeekDailyDates(-2)[2].third
        val foundOffset = DateUtils.findWeekOffsetForIsoDate(twoWeeksAgoWednesdayIso)
        assertEquals(-2, foundOffset)
    }

    @Test
    fun testDateIsoFromMillisMatchesCalendar() {
        val monday = DateUtils.getWeekMonday(-1)
        val expectedIso = DateUtils.getWeekDailyDates(-1)[0].third
        assertEquals(expectedIso, DateUtils.getDateIsoFromMillis(monday.timeInMillis))
    }
}
