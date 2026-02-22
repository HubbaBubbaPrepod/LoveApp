package com.example.loveapp.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DateUtils {
    private val ruLocale = Locale("ru", "RU")

    fun getTodayDateString(): String {
        val format = SimpleDateFormat("yyyy-MM-dd", ruLocale)
        return format.format(Date())
    }

    fun timestampToDateString(timestamp: Long): String {
        val format = SimpleDateFormat("yyyy-MM-dd", ruLocale)
        return format.format(Date(timestamp))
    }

    fun dateStringToTimestamp(dateString: String): Long {
        val format = SimpleDateFormat("yyyy-MM-dd", ruLocale)
        return format.parse(dateString)?.time ?: System.currentTimeMillis()
    }

    fun formatDateForDisplay(dateString: String): String {
        return try {
            val parseFormat = SimpleDateFormat("yyyy-MM-dd", ruLocale)
            val displayFormat = SimpleDateFormat("d MMMM yyyy", ruLocale)
            val date = parseFormat.parse(dateString) ?: return dateString
            displayFormat.format(date)
        } catch (_: Exception) {
            dateString
        }
    }

    fun formatMonthYear(year: Int, month: Int): String {
        val calendar = java.util.Calendar.getInstance(ruLocale)
        calendar.set(year, month - 1, 1)
        val format = SimpleDateFormat("LLLL yyyy", ruLocale)
        return format.format(calendar.time)
    }

    fun getDaysBetween(startDate: Long, endDate: Long): Long {
        return (endDate - startDate) / (1000 * 60 * 60 * 24)
    }

    fun getAllDatesInMonth(year: Int, month: Int): List<String> {
        val dates = mutableListOf<String>()
        val format = SimpleDateFormat("yyyy-MM-dd", ruLocale)
        val calendar = java.util.Calendar.getInstance(ruLocale)
        calendar.set(year, month - 1, 1)
        
        val maxDay = calendar.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)
        for (day in 1..maxDay) {
            calendar.set(year, month - 1, day)
            dates.add(format.format(calendar.time))
        }
        return dates
    }
}
