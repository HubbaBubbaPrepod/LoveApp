package com.example.loveapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.loveapp.data.api.models.ActivityResponse
import com.example.loveapp.data.repository.ActivityRepository
import com.example.loveapp.utils.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class ActivityViewModel @Inject constructor(
    private val activityRepository: ActivityRepository
) : ViewModel() {

    // Today
    private val _myTodayActivities = MutableStateFlow<List<ActivityResponse>>(emptyList())
    val myTodayActivities: StateFlow<List<ActivityResponse>> = _myTodayActivities.asStateFlow()

    private val _partnerTodayActivities = MutableStateFlow<List<ActivityResponse>>(emptyList())
    val partnerTodayActivities: StateFlow<List<ActivityResponse>> = _partnerTodayActivities.asStateFlow()

    // Names
    private val _myName = MutableStateFlow<String?>(null)
    val myName: StateFlow<String?> = _myName.asStateFlow()

    private val _partnerName = MutableStateFlow<String?>(null)
    val partnerName: StateFlow<String?> = _partnerName.asStateFlow()

    // Calendar (month maps: "yyyy-MM-dd" -> list)
    private val _myMonthActivities = MutableStateFlow<Map<String, List<ActivityResponse>>>(emptyMap())
    val myMonthActivities: StateFlow<Map<String, List<ActivityResponse>>> = _myMonthActivities.asStateFlow()

    private val _partnerMonthActivities = MutableStateFlow<Map<String, List<ActivityResponse>>>(emptyMap())
    val partnerMonthActivities: StateFlow<Map<String, List<ActivityResponse>>> = _partnerMonthActivities.asStateFlow()

    private val _calendarYear = MutableStateFlow(Calendar.getInstance().get(Calendar.YEAR))
    val calendarYear: StateFlow<Int> = _calendarYear.asStateFlow()

    private val _calendarMonth = MutableStateFlow(Calendar.getInstance().get(Calendar.MONTH))
    val calendarMonth: StateFlow<Int> = _calendarMonth.asStateFlow()

    // Legacy compat
    private val _activities = MutableStateFlow<List<ActivityResponse>>(emptyList())
    val activities: StateFlow<List<ActivityResponse>> = _activities.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isCalendarLoading = MutableStateFlow(false)
    val isCalendarLoading: StateFlow<Boolean> = _isCalendarLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    init { loadToday() }

    fun loadToday() {
        viewModelScope.launch {
            _isLoading.value = true
            val today = DateUtils.getTodayDateString()
            val myDef      = async { activityRepository.getActivities(date = today) }
            val partnerDef = async { activityRepository.getPartnerActivities(date = today) }
            myDef.await().onSuccess { list ->
                _myTodayActivities.value = list
                _activities.value = list
                if (_myName.value == null) _myName.value = list.firstOrNull()?.displayName
            }
            partnerDef.await().onSuccess { list ->
                _partnerTodayActivities.value = list
                if (_partnerName.value == null) _partnerName.value = list.firstOrNull()?.displayName
            }.onFailure { /* no partner is ok */ }
            _isLoading.value = false
        }
    }

    fun createActivity(
        activityType: String,
        durationMinutes: Int,
        startTime: String,
        note: String
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            activityRepository.createActivity(activityType, durationMinutes, startTime, note)
                .onSuccess {
                    loadToday()
                    _successMessage.value = "Активность сохранена"
                }
                .onFailure { _errorMessage.value = it.message }
            _isLoading.value = false
        }
    }

    fun deleteActivity(id: Int) {
        viewModelScope.launch {
            activityRepository.deleteActivity(id)
                .onSuccess { loadToday() }
                .onFailure { _errorMessage.value = it.message }
        }
    }

    fun loadCalendarMonth(year: Int, month: Int) {
        _calendarYear.value = year
        _calendarMonth.value = month
        viewModelScope.launch {
            _isCalendarLoading.value = true
            val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val cal = Calendar.getInstance()
            cal.set(year, month, 1)
            val firstDay = fmt.format(cal.time)
            cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
            val lastDay = fmt.format(cal.time)
            val myDef      = async { activityRepository.getActivities(startDate = firstDay, endDate = lastDay) }
            val partnerDef = async { activityRepository.getPartnerActivities(startDate = firstDay, endDate = lastDay) }
            myDef.await().onSuccess { list ->
                _myMonthActivities.value = list.groupBy { it.date.take(10) }
                if (_myName.value == null) _myName.value = list.firstOrNull()?.displayName
            }
            partnerDef.await().onSuccess { list ->
                _partnerMonthActivities.value = list.groupBy { it.date.take(10) }
                if (_partnerName.value == null) _partnerName.value = list.firstOrNull()?.displayName
            }.onFailure { /* ok */ }
            _isCalendarLoading.value = false
        }
    }

    // Legacy compat
    fun loadActivities() = loadToday()

    fun clearMessages() {
        _errorMessage.value = null
        _successMessage.value = null
    }
}
