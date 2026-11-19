package com.example.onefifty.util

import java.util.Calendar

object DateUtils {

    /**
     * Computes Monday 00:00:00 of the week with an optional week offset.
     * offsetWeeks = 0: current week
     * offsetWeeks = -1: previous week
     *
     * Uses strict Monday-first ISO-8601 calculation regardless of system locale.
     */
    fun getWeekMonday(offsetWeeks: Int = 0): Calendar {
        val cal = Calendar.getInstance()
        cal.firstDayOfWeek = Calendar.MONDAY

        // Java Calendar: Sunday = 1, Monday = 2, Tuesday = 3, ..., Saturday = 7
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        val daysFromMonday = if (dayOfWeek == Calendar.SUNDAY) 6 else dayOfWeek - Calendar.MONDAY

        cal.add(Calendar.DAY_OF_MONTH, -daysFromMonday + (offsetWeeks * 7))
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal
    }

    fun getCurrentWeekMonday(): Calendar {
        return getWeekMonday(0)
    }

    /**
     * Computes Sunday 23:59:59 of the week with an optional week offset.
     */
    fun getWeekSunday(offsetWeeks: Int = 0): Calendar {
        val monday = getWeekMonday(offsetWeeks)
        val sunday = monday.clone() as Calendar
        sunday.add(Calendar.DAY_OF_MONTH, 6)
        sunday.set(Calendar.HOUR_OF_DAY, 23)
        sunday.set(Calendar.MINUTE, 59)
        sunday.set(Calendar.SECOND, 59)
        return sunday
    }

    /**
     * Formats week date range string (e.g., "28 Sep – 4 Oct, 2026" or "14–20 Oct, 2026").
     */
    fun getWeekDateRangeString(offsetWeeks: Int = 0): String {
        val monday = getWeekMonday(offsetWeeks)
        val sunday = getWeekSunday(offsetWeeks)

        val startDay = monday.get(Calendar.DAY_OF_MONTH)
        val startMonth = getMonthAbbreviation(monday.get(Calendar.MONTH))
        val endDay = sunday.get(Calendar.DAY_OF_MONTH)
        val endMonth = getMonthAbbreviation(sunday.get(Calendar.MONTH))
        val year = sunday.get(Calendar.YEAR)

        return if (startMonth == endMonth) {
            "$startDay–$endDay $startMonth, $year"
        } else {
            "$startDay $startMonth – $endDay $endMonth, $year"
        }
    }

    fun getCurrentWeekDateRangeString(): String {
        return getWeekDateRangeString(0)
    }

    /**
     * Returns "Today is Monday, Sep 28".
     */
    fun getTodayString(): String {
        val cal = Calendar.getInstance()
        val dayName = getDayName(cal.get(Calendar.DAY_OF_WEEK))
        val monthName = getMonthAbbreviation(cal.get(Calendar.MONTH))
        val dayNumber = cal.get(Calendar.DAY_OF_MONTH)
        return "Today is $dayName, $monthName $dayNumber"
    }

    /**
     * Generates list of (DayName, DisplayDate, IsoDateString) for Monday through Sunday.
     * Example: ("Monday", "Sep 28", "2026-09-28")
     */
    fun getWeekDailyDates(offsetWeeks: Int = 0): List<Triple<String, String, String>> {
        val monday = getWeekMonday(offsetWeeks)
        val list = mutableListOf<Triple<String, String, String>>()
        val dayNames = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")

        for (i in 0..6) {
            val dayCal = monday.clone() as Calendar
            dayCal.add(Calendar.DAY_OF_MONTH, i)
            val monthStr = getMonthAbbreviation(dayCal.get(Calendar.MONTH))
            val dayNum = dayCal.get(Calendar.DAY_OF_MONTH)
            val isoDate = formatCalendarIso(dayCal)

            list.add(Triple(dayNames[i], "$monthStr $dayNum", isoDate))
        }
        return list
    }

    fun getCurrentWeekDailyDates(): List<Pair<String, String>> {
        return getWeekDailyDates(0).map { Pair(it.first, it.second) }
    }

    /**
     * Finds the week offset (0, -1, -2, ...) containing [isoDate], or null if outside range.
     */
    fun findWeekOffsetForIsoDate(isoDate: String, maxWeeksBack: Int = 52): Int? {
        for (offset in 0 downTo -maxWeeksBack) {
            if (getWeekDailyDates(offset).any { it.third == isoDate }) {
                return offset
            }
        }
        return null
    }

    fun getDateIsoFromMillis(millis: Long): String {
        val cal = Calendar.getInstance().apply { timeInMillis = millis }
        return formatCalendarIso(cal)
    }

    fun getDayNameFromMillis(millis: Long): String {
        val cal = Calendar.getInstance().apply { timeInMillis = millis }
        return getDayName(cal.get(Calendar.DAY_OF_WEEK))
    }

    fun getTimeFormattedFromMillis(millis: Long): String {
        val cal = Calendar.getInstance().apply { timeInMillis = millis }
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val min = cal.get(Calendar.MINUTE)
        val hStr = if (hour < 10) "0$hour" else "$hour"
        val mStr = if (min < 10) "0$min" else "$min"
        return "$hStr:$mStr"
    }

    fun getCurrentDateIso(): String = getDateIsoFromMillis(System.currentTimeMillis())

    fun getCurrentDayName(): String = getDayNameFromMillis(System.currentTimeMillis())

    fun getCurrentTimeFormatted(): String = getTimeFormattedFromMillis(System.currentTimeMillis())

    private fun formatCalendarIso(cal: Calendar): String {
        val y = cal.get(Calendar.YEAR)
        val m = cal.get(Calendar.MONTH) + 1
        val d = cal.get(Calendar.DAY_OF_MONTH)
        val mStr = if (m < 10) "0$m" else "$m"
        val dStr = if (d < 10) "0$d" else "$d"
        return "$y-$mStr-$dStr"
    }

    fun getIsoWeekNumber(cal: Calendar): Int {
        val copy = cal.clone() as Calendar
        copy.firstDayOfWeek = Calendar.MONDAY
        copy.minimalDaysInFirstWeek = 4
        return copy.get(Calendar.WEEK_OF_YEAR)
    }

    private fun getMonthAbbreviation(month: Int): String {
        return when (month) {
            Calendar.JANUARY -> "Jan"
            Calendar.FEBRUARY -> "Feb"
            Calendar.MARCH -> "Mar"
            Calendar.APRIL -> "Apr"
            Calendar.MAY -> "May"
            Calendar.JUNE -> "Jun"
            Calendar.JULY -> "Jul"
            Calendar.AUGUST -> "Aug"
            Calendar.SEPTEMBER -> "Sep"
            Calendar.OCTOBER -> "Oct"
            Calendar.NOVEMBER -> "Nov"
            Calendar.DECEMBER -> "Dec"
            else -> ""
        }
    }

    private fun getDayName(dayOfWeek: Int): String {
        return when (dayOfWeek) {
            Calendar.MONDAY -> "Monday"
            Calendar.TUESDAY -> "Tuesday"
            Calendar.WEDNESDAY -> "Wednesday"
            Calendar.THURSDAY -> "Thursday"
            Calendar.FRIDAY -> "Friday"
            Calendar.SATURDAY -> "Saturday"
            Calendar.SUNDAY -> "Sunday"
            else -> "Monday"
        }
    }
}
