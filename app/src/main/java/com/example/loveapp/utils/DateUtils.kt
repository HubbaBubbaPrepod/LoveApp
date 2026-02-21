package com.example.loveapp.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DateUtils {
    fun getTodayDateString(): String {
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return format.format(Date())
    }

    fun timestampToDateString(timestamp: Long): String {
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return format.format(Date(timestamp))
    }

    fun dateStringToTimestamp(dateString: String): Long {
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return format.parse(dateString)?.time ?: System.currentTimeMillis()
    }

    fun getDaysBetween(startDate: Long, endDate: Long): Long {
        return (endDate - startDate) / (1000 * 60 * 60 * 24)
    }

    fun getAllDatesInMonth(year: Int, month: Int): List<String> {
        val dates = mutableListOf<String>()
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val calendar = java.util.Calendar.getInstance()
        calendar.set(year, month - 1, 1)
        
        val maxDay = calendar.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)
        for (day in 1..maxDay) {
            calendar.set(year, month - 1, day)
            dates.add(format.format(calendar.time))
        }
        return dates
    }
}
