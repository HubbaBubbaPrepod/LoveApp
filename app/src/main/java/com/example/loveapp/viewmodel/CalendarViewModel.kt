package com.example.loveapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.loveapp.data.api.models.CalendarEventResponse
import com.example.loveapp.data.api.models.CustomCalendarResponse
import com.example.loveapp.data.repository.CalendarRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val calendarRepository: CalendarRepository
) : ViewModel() {

    private val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    //  Calendars list 
    private val _calendars = MutableStateFlow<List<CustomCalendarResponse>>(emptyList())
    val calendars: StateFlow<List<CustomCalendarResponse>> = _calendars.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    //  Events:  calendarId -> list of event responses 
    private val _events = MutableStateFlow<Map<Int, List<CalendarEventResponse>>>(emptyMap())
    val events: StateFlow<Map<Int, List<CalendarEventResponse>>> = _events.asStateFlow()

    //  Detail-screen navigation state 
    private val today: LocalDate = LocalDate.now()

    private val _calendarMonth = MutableStateFlow(YearMonth.of(today.year, today.monthValue))
    val calendarMonth: StateFlow<YearMonth> = _calendarMonth.asStateFlow()

    private val _selectedCalendarId = MutableStateFlow<Int?>(null)
    val selectedCalendarId: StateFlow<Int?> = _selectedCalendarId.asStateFlow()

    init {
        loadCalendars()
    }

    //  Load all calendars (mine + partner's) and prefetch their events 
    fun loadCalendars() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            val myDeferred      = async { calendarRepository.getCalendars() }
            val partnerDeferred = async { calendarRepository.getPartnerCalendars() }

            val mine    = myDeferred.await().getOrElse { emptyList() }
            val partner = partnerDeferred.await().getOrElse { emptyList() }

            val all = (mine + partner).distinctBy { it.id }
            _calendars.value = all

            if (myDeferred.await().isFailure) {
                _errorMessage.value = myDeferred.await().exceptionOrNull()?.message
            }

            // Prefetch events for all calendars in parallel (for mini-week preview)
            all.forEach { cal ->
                launch {
                    calendarRepository.getCalendarEvents(cal.id).onSuccess { list ->
                        _events.value = _events.value + (cal.id to list)
                    }
                }
            }
            _isLoading.value = false
        }
    }

    //  Select a calendar and ensure its events are loaded 
    fun selectCalendar(calendarId: Int) {
        _selectedCalendarId.value = calendarId
        _calendarMonth.value = YearMonth.of(today.year, today.monthValue)
        if (_events.value[calendarId] == null) {
            viewModelScope.launch {
                calendarRepository.getCalendarEvents(calendarId).onSuccess { list ->
                    _events.value = _events.value + (calendarId to list)
                }.onFailure {
                    _errorMessage.value = it.message
                }
            }
        }
    }

    //  Toggle a day: mark if not marked, unmark if already marked 
    fun toggleDay(calendarId: Int, date: LocalDate) {
        val dateStr     = date.format(fmt)
        val currentList = _events.value[calendarId] ?: emptyList()
        val existing    = currentList.find { it.eventDate == dateStr }

        viewModelScope.launch {
            if (existing != null) {
                calendarRepository.unmarkDay(existing.id).onSuccess {
                    _events.value = _events.value + (calendarId to currentList.filter { it.id != existing.id })
                }.onFailure { _errorMessage.value = it.message }
            } else {
                calendarRepository.markDay(calendarId, dateStr).onSuccess { event ->
                    _events.value = _events.value + (calendarId to (currentList + event))
                }.onFailure { _errorMessage.value = it.message }
            }
        }
    }

    //  Helpers 
    fun isMarked(calendarId: Int, date: LocalDate): Boolean =
        _events.value[calendarId]?.any { it.eventDate == date.format(fmt) } == true

    fun markedDatesForCalendar(calendarId: Int): Set<String> =
        _events.value[calendarId]?.map { it.eventDate }?.toSet() ?: emptySet()

    fun calendarById(id: Int): CustomCalendarResponse? = _calendars.value.find { it.id == id }

    //  Month navigation 
    fun prevMonth() { _calendarMonth.value = _calendarMonth.value.minusMonths(1) }
    fun nextMonth() { _calendarMonth.value = _calendarMonth.value.plusMonths(1) }

    //  CRUD 
    fun createCalendar(name: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val colors = listOf("#FF4D6D", "#4D7FFF", "#30D158", "#FF9F0A", "#BF5AF2", "#FF6B6B")
            val color  = colors.random()
            calendarRepository.createCalendar(name, "custom", color).onSuccess { cal ->
                _calendars.value = listOf(cal) + _calendars.value
                _successMessage.value = "Создан $name"
            }.onFailure {
                _errorMessage.value = it.message ?: "Ошибка создания"
            }
            _isLoading.value = false
        }
    }

    fun deleteCalendar(id: Int) {
        viewModelScope.launch {
            calendarRepository.deleteCalendar(id).onSuccess {
                _calendars.value = _calendars.value.filter { it.id != id }
                val updatedEvents = _events.value.toMutableMap().also { it.remove(id) }
                _events.value = updatedEvents
                _successMessage.value = "Удалено"
            }.onFailure {
                _errorMessage.value = it.message ?: "Ошибка удаления"
            }
        }
    }

    fun clearMessages() {
        _errorMessage.value = null
        _successMessage.value = null
    }
}