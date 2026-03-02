package com.example.loveapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.loveapp.data.api.models.MoodResponse
import com.example.loveapp.data.repository.MoodRepository
import com.example.loveapp.utils.DateUtils
import com.example.loveapp.widget.WidgetUpdater
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
class MoodViewModel @Inject constructor(
    private val moodRepository: MoodRepository,
    private val widgetUpdater: WidgetUpdater
) : ViewModel() {

    private val _myTodayMoods = MutableStateFlow<List<MoodResponse>>(emptyList())
    val myTodayMoods: StateFlow<List<MoodResponse>> = _myTodayMoods.asStateFlow()

    private val _partnerTodayMoods = MutableStateFlow<List<MoodResponse>>(emptyList())
    val partnerTodayMoods: StateFlow<List<MoodResponse>> = _partnerTodayMoods.asStateFlow()

    private val _partnerName = MutableStateFlow<String?>(null)
    val partnerName: StateFlow<String?> = _partnerName.asStateFlow()

    private val _myName = MutableStateFlow<String?>(null)
    val myName: StateFlow<String?> = _myName.asStateFlow()

    private val _myAvatar = MutableStateFlow<String?>(null)
    val myAvatar: StateFlow<String?> = _myAvatar.asStateFlow()

    private val _partnerAvatar = MutableStateFlow<String?>(null)
    val partnerAvatar: StateFlow<String?> = _partnerAvatar.asStateFlow()

    // Maps: date "yyyy-MM-dd" -> list of moods for calendar view
    private val _myMonthMoods = MutableStateFlow<Map<String, List<MoodResponse>>>(emptyMap())
    val myMonthMoods: StateFlow<Map<String, List<MoodResponse>>> = _myMonthMoods.asStateFlow()

    private val _partnerMonthMoods = MutableStateFlow<Map<String, List<MoodResponse>>>(emptyMap())
    val partnerMonthMoods: StateFlow<Map<String, List<MoodResponse>>> = _partnerMonthMoods.asStateFlow()

    private val _calendarYear = MutableStateFlow(Calendar.getInstance().get(Calendar.YEAR))
    val calendarYear: StateFlow<Int> = _calendarYear.asStateFlow()

    private val _calendarMonth = MutableStateFlow(Calendar.getInstance().get(Calendar.MONTH)) // 0-based
    val calendarMonth: StateFlow<Int> = _calendarMonth.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isCalendarLoading = MutableStateFlow(false)
    val isCalendarLoading: StateFlow<Boolean> = _isCalendarLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    // Legacy compat
    val moods: StateFlow<List<MoodResponse>> get() = _myTodayMoods

    init {
        loadToday()
    }

    fun loadToday() {
        viewModelScope.launch {
            _isLoading.value = true
            val today      = DateUtils.getTodayDateString()
            val myDef      = async { moodRepository.getMoods(date = today) }
            val partnerDef = async { moodRepository.getPartnerMoods(date = today) }
            val myResult   = myDef.await()
            val ptResult   = partnerDef.await()
            myResult.onSuccess {
                _myTodayMoods.value = it
                if (_myName.value == null) _myName.value = it.firstOrNull()?.displayName
                if (_myAvatar.value == null) _myAvatar.value = it.firstOrNull()?.userAvatar
            }
            ptResult.onSuccess { moods ->
                _partnerTodayMoods.value = moods
                if (_partnerName.value == null) _partnerName.value = moods.firstOrNull()?.displayName
                if (_partnerAvatar.value == null) _partnerAvatar.value = moods.firstOrNull()?.userAvatar
            }.onFailure { /* no partner is ok */ }
            // Push both users' moods to the home-screen widget after both loads finish
            val my      = myResult.getOrElse { emptyList() }
            val pt      = ptResult.getOrElse { emptyList() }
            val myFirst = my.firstOrNull()
            val ptFirst = pt.firstOrNull()
            viewModelScope.launch {
                widgetUpdater.pushMoodUpdate(
                    myType = myFirst?.moodType ?: "",
                    myNote = myFirst?.note     ?: "",
                    myName = _myName.value,
                    ptType = ptFirst?.moodType ?: "",
                    ptNote = ptFirst?.note     ?: "",
                    ptName = _partnerName.value
                )
            }
            _isLoading.value = false
        }
    }

    fun addMood(moodType: String, note: String = "") {
        viewModelScope.launch {
            _isLoading.value = true
            val today = DateUtils.getTodayDateString()
            moodRepository.createMood(moodType, today, note)
                .onSuccess {
                    loadToday()
                    _successMessage.value = "Запись сохранена"
                }
                .onFailure { _errorMessage.value = it.message }
            _isLoading.value = false
        }
    }

    fun deleteMood(id: Int) {
        viewModelScope.launch {
            moodRepository.deleteMood(id).onSuccess { loadToday() }
                .onFailure { _errorMessage.value = it.message }
        }
    }

    fun loadCalendarMonth(year: Int, month: Int) { // month 0-based
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
            val myDef = async { moodRepository.getMoods(startDate = firstDay, endDate = lastDay) }
            val partnerDef = async { moodRepository.getPartnerMoods(startDate = firstDay, endDate = lastDay) }
            myDef.await().onSuccess { list ->
                _myMonthMoods.value = list.groupBy { it.date.take(10) }
                if (_myName.value == null) {
                    _myName.value = list.firstOrNull()?.displayName
                }
            }
            partnerDef.await().onSuccess { list ->
                _partnerMonthMoods.value = list.groupBy { it.date.take(10) }
                if (_partnerName.value == null) {
                    _partnerName.value = list.firstOrNull()?.displayName
                }
            }.onFailure { /* no partner is ok */ }
            _isCalendarLoading.value = false
        }
    }

    // Legacy compat for existing call sites
    fun createMood(moodType: String) = addMood(moodType)
    fun loadTodayMoods() = loadToday()

    fun clearMessages() {
        _errorMessage.value = null
        _successMessage.value = null
    }
}

