package com.example.onefifty.util

import java.util.Calendar

object DateUtils {

    // Computes Monday of the current calendar week
    fun getCurrentWeekMonday(): Calendar {
        val cal = Calendar.getInstance()
        cal.firstDayOfWeek = Calendar.MONDAY
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        return cal
    }

    // Formats dynamic current week range (e.g., "28 Sep – 4 Oct, 2025" or "14–20 Oct, 2025")
    fun getCurrentWeekDateRangeString(): String {
        val monday = getCurrentWeekMonday()
        val sunday = monday.clone() as Calendar
        sunday.add(Calendar.DAY_OF_MONTH, 6)

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

    // Formats current day indicator (e.g., "Today is Monday, Sep 28")
    fun getTodayString(): String {
        val cal = Calendar.getInstance()
        val dayName = getDayName(cal.get(Calendar.DAY_OF_WEEK))
        val monthName = getMonthAbbreviation(cal.get(Calendar.MONTH))
        val dayNumber = cal.get(Calendar.DAY_OF_MONTH)
        return "Today is $dayName, $monthName $dayNumber"
    }

    // Generates list of (DayName, DateString) for Monday through Sunday of current week
    fun getCurrentWeekDailyDates(): List<Pair<String, String>> {
        val monday = getCurrentWeekMonday()
        val list = mutableListOf<Pair<String, String>>()
        val dayNames = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")

        for (i in 0..6) {
            val dayCal = monday.clone() as Calendar
            dayCal.add(Calendar.DAY_OF_MONTH, i)
            val monthStr = getMonthAbbreviation(dayCal.get(Calendar.MONTH))
            val dayNum = dayCal.get(Calendar.DAY_OF_MONTH)
            list.add(Pair(dayNames[i], "$monthStr $dayNum"))
        }
        return list
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
            else -> "Today"
        }
    }
}
