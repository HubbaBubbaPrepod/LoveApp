package com.example.loveapp.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DateUtils {
    private val ruLocale = Locale("ru", "RU")

    // Shared formatter instances — never recreate on every call
    private val ISO_FMT   = SimpleDateFormat("yyyy-MM-dd", ruLocale)
    private val DISP_FMT  = SimpleDateFormat("d MMMM yyyy", ruLocale)
    private val MONTH_FMT = SimpleDateFormat("LLLL yyyy", ruLocale)

    fun getTodayDateString(): String = ISO_FMT.format(Date())

    fun timestampToDateString(timestamp: Long): String = ISO_FMT.format(Date(timestamp))

    fun dateStringToTimestamp(dateString: String): Long =
        ISO_FMT.parse(dateString)?.time ?: System.currentTimeMillis()

    fun formatDateForDisplay(dateString: String): String {
        return try {
            val date = ISO_FMT.parse(dateString) ?: return dateString
            DISP_FMT.format(date)
        } catch (_: Exception) {
            dateString
        }
    }

    fun formatMonthYear(year: Int, month: Int): String {
        val calendar = java.util.Calendar.getInstance(ruLocale)
        calendar.set(year, month - 1, 1)
        return MONTH_FMT.format(calendar.time)
    }

    fun getDaysBetween(startDate: Long, endDate: Long): Long =
        (endDate - startDate) / (1000 * 60 * 60 * 24)

    fun getAllDatesInMonth(year: Int, month: Int): List<String> {
        val dates = mutableListOf<String>()
        val calendar = java.util.Calendar.getInstance(ruLocale)
        calendar.set(year, month - 1, 1)
        val maxDay = calendar.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)
        for (day in 1..maxDay) {
            calendar.set(year, month - 1, day)
            dates.add(ISO_FMT.format(calendar.time))
        }
        return dates
    }
}
