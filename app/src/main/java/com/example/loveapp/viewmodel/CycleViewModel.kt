package com.example.loveapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.loveapp.data.api.models.CycleRequest
import com.example.loveapp.data.api.models.CycleResponse
import com.example.loveapp.data.repository.AuthRepository
import com.example.loveapp.data.repository.CycleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import javax.inject.Inject

enum class CycleDayType { PERIOD_ACTUAL, PERIOD_PREDICTED, OVULATION_ACTUAL, OVULATION_PREDICTED, FERTILE_PREDICTED, NORMAL }

@HiltViewModel
class CycleViewModel @Inject constructor(
    private val cycleRepository: CycleRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    //  Raw data 
    private val _cycles = MutableStateFlow<List<CycleResponse>>(emptyList())
    val cycles: StateFlow<List<CycleResponse>> = _cycles.asStateFlow()

    // User can edit only if female
    private val _isGirl = MutableStateFlow(false)
    val isGirl: StateFlow<Boolean> = _isGirl.asStateFlow()

    //  Calendar state 
    private val today = LocalDate.now()
    private val _calendarYear  = MutableStateFlow(today.year)
    val calendarYear: StateFlow<Int> = _calendarYear.asStateFlow()
    private val _calendarMonth = MutableStateFlow(today.monthValue - 1) // 0-indexed
    val calendarMonth: StateFlow<Int> = _calendarMonth.asStateFlow()

    private val _selectedDate = MutableStateFlow<String?>(null)
    val selectedDate: StateFlow<String?> = _selectedDate.asStateFlow()

    //  Computed cycle map: "YYYY-MM-DD" -> CycleDayType 
    private val _cycleDayMap = MutableStateFlow<Map<String, CycleDayType>>(emptyMap())
    val cycleDayMap: StateFlow<Map<String, CycleDayType>> = _cycleDayMap.asStateFlow()

    //  Phase / prediction info 
    private val _currentPhase = MutableStateFlow("Загрузка...")
    val currentPhase: StateFlow<String> = _currentPhase.asStateFlow()

    private val _daysUntilNextPeriod = MutableStateFlow<Int?>(null)
    val daysUntilNextPeriod: StateFlow<Int?> = _daysUntilNextPeriod.asStateFlow()

    private val _nextPeriodDate = MutableStateFlow<String?>(null)
    val nextPeriodDate: StateFlow<String?> = _nextPeriodDate.asStateFlow()

    private val _avgCycleDuration = MutableStateFlow(28)
    val avgCycleDuration: StateFlow<Int> = _avgCycleDuration.asStateFlow()

    private val _avgPeriodDuration = MutableStateFlow(5)
    val avgPeriodDuration: StateFlow<Int> = _avgPeriodDuration.asStateFlow()

    //  Loading / messages 
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    // Legacy compat
    val currentCycle: StateFlow<CycleResponse?> get() = MutableStateFlow(_cycles.value.firstOrNull()).asStateFlow()

    init { loadAll() }

    //  Public API 

    fun loadAll() {
        viewModelScope.launch {
            _isLoading.value = true
            val gender = authRepository.getGender()
            _isGirl.value = gender.equals("female", ignoreCase = true)

            if (_isGirl.value) {
                // Female: load own cycles
                val myCycles = async { cycleRepository.getCycles() }
                myCycles.await().onSuccess { list ->
                    val sorted = list.sortedByDescending { it.cycleStartDate }
                    _cycles.value = sorted
                    recompute(sorted)
                }.onFailure { _errorMessage.value = it.message }
            } else {
                // Male partner: load partner's cycles in read-only mode
                cycleRepository.getPartnerCycles().onSuccess { list ->
                    val sorted = list.sortedByDescending { it.cycleStartDate }
                    _cycles.value = sorted
                    recompute(sorted)
                }.onFailure {
                    // No partner data yet — not an error shown to user
                    _cycles.value = emptyList()
                    recompute(emptyList())
                }
            }
            _isLoading.value = false
        }
    }

    fun prevCalendarMonth() {
        val cur = YearMonth.of(_calendarYear.value, _calendarMonth.value + 1)
        val prev = cur.minusMonths(1)
        _calendarYear.value  = prev.year
        _calendarMonth.value = prev.monthValue - 1
    }

    fun nextCalendarMonth() {
        val cur = YearMonth.of(_calendarYear.value, _calendarMonth.value + 1)
        val next = cur.plusMonths(1)
        _calendarYear.value  = next.year
        _calendarMonth.value = next.monthValue - 1
    }

    fun selectDate(date: String?) { _selectedDate.value = date }

    /** Mark first day of period for a given date  creates a new cycle */
    fun markPeriodStart(date: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val avgDur = _avgCycleDuration.value
            val avgPer = _avgPeriodDuration.value
            cycleRepository.createCycle(
                cycleStartDate = date,
                cycleDuration  = avgDur,
                periodDuration = avgPer
            ).onSuccess { newCycle ->
                val updated = (listOf(newCycle) + _cycles.value).sortedByDescending { it.cycleStartDate }
                _cycles.value = updated
                recompute(updated)
                _successMessage.value = "Начало цикла отмечено"
            }.onFailure { _errorMessage.value = it.message }
            _isLoading.value = false
        }
    }

    /** Save per-day symptom and mood data to the cycle that owns the date */
    fun saveDayData(date: String, symptoms: List<String>, mood: String) {
        val cycle = findCycleForDate(date) ?: run {
            // If the day is marked as "period start" or is before any cycle, create a cycle
            if (symptoms.contains("period_start")) {
                markPeriodStart(date)
                return
            }
            _errorMessage.value = "Нет цикла для этой даты. Сначала отметьте начало цикла."
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            cycleRepository.patchCycleDay(
                cycleId      = cycle.id,
                date         = date,
                symptomsDay  = symptoms.ifEmpty { null },
                moodDay      = mood.ifBlank { null }
            ).onSuccess { updated ->
                val list = _cycles.value.map { if (it.id == updated.id) updated else it }
                    .sortedByDescending { it.cycleStartDate }
                _cycles.value = list
                recompute(list)
                _successMessage.value = "Данные сохранены"
            }.onFailure { _errorMessage.value = it.message }
            _isLoading.value = false
        }
    }

    fun deleteCycleEntry(id: Int) {
        viewModelScope.launch {
            cycleRepository.deleteCycle(id).onSuccess {
                val updated = _cycles.value.filter { it.id != id }
                _cycles.value = updated
                recompute(updated)
                _successMessage.value = "Запись удалена"
            }.onFailure { _errorMessage.value = it.message }
        }
    }

    /** Delete the cycle whose cycleStartDate matches the given date string */
    fun deleteCycleForDate(dateStr: String) {
        // First try exact cycle start match, then fall back to any cycle containing the date
        val cycle = _cycles.value.firstOrNull { it.cycleStartDate.startsWith(dateStr) }
            ?: findCycleForDate(dateStr)
            ?: run {
                _errorMessage.value = "Запись для этой даты не найдена"
                return
            }
        deleteCycleEntry(cycle.id)
    }

    /** Returns true if a cycle starts exactly on this date */
    fun isCycleStartDate(dateStr: String): Boolean =
        _cycles.value.any { it.cycleStartDate.startsWith(dateStr) }

    fun clearMessages() {
        _errorMessage.value = null
        _successMessage.value = null
    }

    //  Day data helpers 

    fun getSymptomsForDate(date: String): List<String> {
        return findCycleForDate(date)?.symptoms?.get(date) ?: emptyList()
    }

    fun getMoodForDate(date: String): String {
        return findCycleForDate(date)?.mood?.get(date) ?: ""
    }

    /** Find the cycle record that contains the given date (start..start+duration) */
    private fun findCycleForDate(dateStr: String): CycleResponse? {
        val date = runCatching { LocalDate.parse(dateStr, fmt) }.getOrNull() ?: return null
        return _cycles.value.firstOrNull { cycle ->
            val start = runCatching { LocalDate.parse(cycle.cycleStartDate, fmt) }.getOrNull()
                ?: return@firstOrNull false
            val end   = start.plusDays(cycle.cycleDuration.toLong() - 1)
            !date.isBefore(start) && !date.isAfter(end)
        }
    }

    //  Prediction engine 

    private fun recompute(sorted: List<CycleResponse>) {
        if (sorted.isEmpty()) {
            _currentPhase.value = "Нет данных"
            _daysUntilNextPeriod.value = null
            _nextPeriodDate.value = null
            _cycleDayMap.value = emptyMap()
            return
        }

        // Avg durations
        val avgCycleDur = if (sorted.size >= 2) {
            sorted.zip(sorted.drop(1)).map { (newer, older) ->
                val n = runCatching { LocalDate.parse(newer.cycleStartDate, fmt) }.getOrNull()
                val o = runCatching { LocalDate.parse(older.cycleStartDate, fmt) }.getOrNull()
                if (n != null && o != null) ChronoUnit.DAYS.between(o, n).toInt() else null
            }.filterNotNull().let { diffs ->
                if (diffs.isEmpty()) sorted.first().cycleDuration
                else diffs.average().toInt().coerceIn(21, 45)
            }
        } else sorted.first().cycleDuration
        _avgCycleDuration.value = avgCycleDur

        val avgPeriodDur = sorted.map { it.periodDuration }.average().toInt().coerceIn(2, 10)
        _avgPeriodDuration.value = avgPeriodDur

        val latestStart = runCatching { LocalDate.parse(sorted.first().cycleStartDate, fmt) }.getOrNull()
            ?: return

        // Build day map: go back 6 months, forward 4 months
        val mapStart = today.minusMonths(6)
        val mapEnd   = today.plusMonths(4)
        val dayMap   = mutableMapOf<String, CycleDayType>()

        // Mark actual cycle days
        sorted.forEach { cycle ->
            val cStart = runCatching { LocalDate.parse(cycle.cycleStartDate, fmt) }.getOrNull() ?: return@forEach
            val perEnd = cStart.plusDays(cycle.periodDuration.toLong() - 1)
            val cycleEnd = cStart.plusDays(cycle.cycleDuration.toLong() - 1)
            val ovDay = cStart.plusDays((cycle.cycleDuration - 14).toLong().coerceAtLeast(0))

            var d = cStart
            while (!d.isAfter(perEnd)) {
                if (!d.isBefore(mapStart) && !d.isAfter(mapEnd))
                    dayMap[d.format(fmt)] = CycleDayType.PERIOD_ACTUAL
                d = d.plusDays(1)
            }
            // Ovulation window: ovDay-1 to ovDay+1
            for (offset in -1..1) {
                val od = ovDay.plusDays(offset.toLong())
                if (!od.isBefore(mapStart) && !od.isAfter(mapEnd))
                    if (dayMap[od.format(fmt)] == null)
                        dayMap[od.format(fmt)] = CycleDayType.OVULATION_ACTUAL
            }
        }

        // Predict future cycles
        var predictStart = latestStart.plusDays(avgCycleDur.toLong())
        repeat(4) {
            if (!predictStart.isAfter(mapEnd)) {
                val perEnd = predictStart.plusDays(avgPeriodDur.toLong() - 1)
                var d = predictStart
                while (!d.isAfter(perEnd)) {
                    if (!d.isBefore(today) && !d.isAfter(mapEnd))
                        dayMap.putIfAbsent(d.format(fmt), CycleDayType.PERIOD_PREDICTED)
                    d = d.plusDays(1)
                }
                // Fertile window: ovDay-3 to ovDay+1
                val ovDay = predictStart.plusDays((avgCycleDur - 14).toLong().coerceAtLeast(0))
                for (offset in -3..1) {
                    val od = ovDay.plusDays(offset.toLong())
                    if (!od.isBefore(today) && !od.isAfter(mapEnd))
                        dayMap.putIfAbsent(od.format(fmt),
                            if (offset == 0) CycleDayType.OVULATION_PREDICTED else CycleDayType.FERTILE_PREDICTED)
                }
            }
            predictStart = predictStart.plusDays(avgCycleDur.toLong())
        }

        _cycleDayMap.value = dayMap

        // Next period date  
        val nextPer = latestStart.plusDays(avgCycleDur.toLong())
        _nextPeriodDate.value = nextPer.format(fmt)
        val daysUntil = ChronoUnit.DAYS.between(today, nextPer).toInt()
        _daysUntilNextPeriod.value = daysUntil

        // Current phase
        val dayOfCycle = ChronoUnit.DAYS.between(latestStart, today).toInt() + 1
        _currentPhase.value = when {
            dayOfCycle < 1 -> "Следующий цикл"
            dayOfCycle <= avgPeriodDur -> "Менструация"
            dayOfCycle < avgCycleDur - 14 -> "Фолликулярная фаза"
            dayOfCycle <= avgCycleDur - 11 -> "Овуляция"
            dayOfCycle <= avgCycleDur -> "Лютеиновая фаза"
            else -> "Следующий цикл"
        }
    }
}