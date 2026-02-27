package com.example.loveapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.loveapp.data.api.models.RelationshipResponse
import com.example.loveapp.data.repository.RelationshipRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import javax.inject.Inject

data class MilestoneEvent(
    val title: String,
    val date: LocalDate,
    val dayNumber: Long,       // relationship day count on that date (1 = start day)
    val type: MilestoneType,
    val daysRelative: Long     // negative = days ago, 0 = today, positive = days until
) {
    val isPast: Boolean get() = daysRelative < 0
    val isToday: Boolean get() = daysRelative == 0L
}

enum class MilestoneType { DAY_COUNT, ANNIVERSARY, HOLIDAY, BIRTHDAY }

@HiltViewModel
class RelationshipViewModel @Inject constructor(
    private val relationshipRepository: RelationshipRepository
) : ViewModel() {

    private val _relationship = MutableStateFlow<RelationshipResponse?>(null)
    val relationship: StateFlow<RelationshipResponse?> = _relationship.asStateFlow()

    private val _partnerDisplayName = MutableStateFlow<String?>(null)
    val partnerDisplayName: StateFlow<String?> = _partnerDisplayName.asStateFlow()

    private val _daysSinceStart = MutableStateFlow(0L)
    val daysSinceStart: StateFlow<Long> = _daysSinceStart.asStateFlow()

    private val _milestones = MutableStateFlow<List<MilestoneEvent>>(emptyList())
    val milestones: StateFlow<List<MilestoneEvent>> = _milestones.asStateFlow()

    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    private val _calendarYearMonth = MutableStateFlow(YearMonth.now())
    val calendarYearMonth: StateFlow<YearMonth> = _calendarYearMonth.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    private val PARSE_DATE_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    init {
        loadRelationship()
    }

    fun loadRelationship() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            val result = relationshipRepository.getRelationship()
            result.onSuccess { rel ->
                _relationship.value = rel
                _partnerDisplayName.value = rel.partnerDisplayName
                val days = calculateDaysSinceStart(rel)
                _daysSinceStart.value = days
                val startDate = parseDate(rel.relationshipStartDate)
                if (startDate != null) {
                    _milestones.value = generateMilestones(
                        startDate = startDate,
                        myBirthday = parseDate(rel.myBirthday),
                        partnerBirthday = parseDate(rel.partnerBirthday),
                        partnerName = rel.partnerDisplayName
                    )
                }
                _isLoading.value = false
            }.onFailure { error ->
                val msg = error.message?.lowercase() ?: ""
                val isNotFound = msg.contains("not found") || msg.contains("no relationship") ||
                    msg.contains("404") || msg.contains("no relationship data")
                if (!isNotFound) {
                    _errorMessage.value = error.message ?: "Failed to load relationship"
                }
                _isLoading.value = false
            }
        }
    }

    fun updateRelationship(
        startDate: String,
        firstKissDate: String? = null,
        anniversaryDate: String? = null,
        myBirthday: String? = null,
        partnerBirthday: String? = null
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            val result = relationshipRepository.updateRelationship(
                startDate, firstKissDate, anniversaryDate, myBirthday, partnerBirthday
            )
            result.onSuccess { rel ->
                _relationship.value = rel
                _partnerDisplayName.value = rel.partnerDisplayName
                val days = calculateDaysSinceStart(rel)
                _daysSinceStart.value = days
                val start = parseDate(rel.relationshipStartDate)
                if (start != null) {
                    _milestones.value = generateMilestones(
                        startDate = start,
                        myBirthday = parseDate(rel.myBirthday),
                        partnerBirthday = parseDate(rel.partnerBirthday),
                        partnerName = rel.partnerDisplayName
                    )
                }
                _successMessage.value = "Данные обновлены"
                _isLoading.value = false
            }.onFailure { error ->
                _errorMessage.value = error.message ?: "Failed to update relationship"
                _isLoading.value = false
            }
        }
    }

    fun selectTab(index: Int) { _selectedTab.value = index }
    fun prevMonth() { _calendarYearMonth.value = _calendarYearMonth.value.minusMonths(1) }
    fun nextMonth() { _calendarYearMonth.value = _calendarYearMonth.value.plusMonths(1) }

    private fun parseDate(dateString: String?): LocalDate? {
        if (dateString.isNullOrEmpty()) return null
        return try {
            LocalDate.parse(dateString.split("T")[0], PARSE_DATE_FMT)
        } catch (e: Exception) { null }
    }

    private fun calculateDaysSinceStart(relationship: RelationshipResponse): Long {
        val start = parseDate(relationship.relationshipStartDate) ?: return 0L
        return ChronoUnit.DAYS.between(start, LocalDate.now())
    }

    private fun generateMilestones(
        startDate: LocalDate,
        myBirthday: LocalDate? = null,
        partnerBirthday: LocalDate? = null,
        partnerName: String? = null
    ): List<MilestoneEvent> {
        val today = LocalDate.now()
        val milestones = mutableListOf<MilestoneEvent>()

        // ── Day-count milestones ──────────────────────────────────────────
        val dayTargets = mutableListOf(10L, 30L, 50L)
        for (i in 1..100) dayTargets.add(i * 100L)          // 100, 200 … 10000
        for (days in dayTargets) {
            val date = startDate.plusDays(days)
            milestones.add(MilestoneEvent(
                title = "$days ${dayForm(days)} вместе",
                date = date,
                dayNumber = days,
                type = MilestoneType.DAY_COUNT,
                daysRelative = ChronoUnit.DAYS.between(today, date)
            ))
        }

        // ── Anniversary milestones ────────────────────────────────────────
        for (years in 1..15) {
            val date = startDate.plusYears(years.toLong())
            milestones.add(MilestoneEvent(
                title = "$years ${yearForm(years)} вместе",
                date = date,
                dayNumber = ChronoUnit.DAYS.between(startDate, date),
                type = MilestoneType.ANNIVERSARY,
                daysRelative = ChronoUnit.DAYS.between(today, date)
            ))
        }

        // ── Holiday milestones (current year ± 1) ────────────────────────
        val holidays = listOf(
            1 to 1 to "Новый год \uD83C\uDF89",
            1 to 7 to "Рождество \u2728",
            2 to 14 to "День Валентина \u2764\uFE0F",
            2 to 23 to "День защитника Отечества",
            3 to 8 to "Международный женский день \uD83C\uDF37"
        )
        for (year in today.year - 1..today.year + 2) {
            for ((monthDay, title) in holidays) {
                val (month, day) = monthDay
                val date = LocalDate.of(year, month, day)
                val dayNumber = ChronoUnit.DAYS.between(startDate, date)
                if (dayNumber >= 0) {
                    milestones.add(MilestoneEvent(
                        title = title,
                        date = date,
                        dayNumber = dayNumber,
                        type = MilestoneType.HOLIDAY,
                        daysRelative = ChronoUnit.DAYS.between(today, date)
                    ))
                }
            }
        }

        // ── Birthday milestones ───────────────────────────────────────────
        val birthdayEntries = listOfNotNull(
            myBirthday?.let { it to "\uD83C\uDF82 Мой день рождения" },
            partnerBirthday?.let { it to "\uD83C\uDF82 День рождения ${partnerName ?: "партнёра"}" }
        )
        for ((bday, bdayTitle) in birthdayEntries) {
            for (year in today.year - 1..today.year + 2) {
                val date = bday.withYear(year)
                val dayNumber = ChronoUnit.DAYS.between(startDate, date)
                if (dayNumber >= 0) {
                    milestones.add(MilestoneEvent(
                        title = bdayTitle,
                        date = date,
                        dayNumber = dayNumber,
                        type = MilestoneType.BIRTHDAY,
                        daysRelative = ChronoUnit.DAYS.between(today, date)
                    ))
                }
            }
        }

        return milestones.sortedBy { it.date }
    }

    fun clearMessages() {
        _errorMessage.value = null
        _successMessage.value = null
    }

    companion object {
        fun dayForm(n: Long): String = when {
            n % 100 in 11L..19L -> "дней"
            n % 10 == 1L -> "день"
            n % 10 in 2L..4L -> "дня"
            else -> "дней"
        }

        fun yearForm(n: Int): String = when {
            n % 100 in 11..19 -> "лет"
            n % 10 == 1 -> "год"
            n % 10 in 2..4 -> "года"
            else -> "лет"
        }
    }
}
