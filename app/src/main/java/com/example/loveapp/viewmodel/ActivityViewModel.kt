package com.example.loveapp.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.loveapp.data.api.models.ActivityResponse
import com.example.loveapp.data.api.models.CustomActivityTypeResponse
import com.example.loveapp.data.repository.ActivityRepository
import com.example.loveapp.ui.screens.CUSTOM_ICON_MAP
import com.example.loveapp.widget.WidgetActivityIcons
import com.example.loveapp.widget.WidgetIconPreparer
import com.example.loveapp.utils.DateUtils
import com.example.loveapp.widget.WidgetUpdater
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ActivityViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val activityRepository: ActivityRepository,
    private val widgetUpdater: WidgetUpdater
) : ViewModel() {

    // Shared class-level formatter — not created on every loadCalendarMonth() call
    private val calFmt = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
    private val initCal = java.util.Calendar.getInstance()
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

    private val _calendarYear = MutableStateFlow(initCal.get(java.util.Calendar.YEAR))
    val calendarYear: StateFlow<Int> = _calendarYear.asStateFlow()

    private val _calendarMonth = MutableStateFlow(initCal.get(java.util.Calendar.MONTH))
    val calendarMonth: StateFlow<Int> = _calendarMonth.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isCalendarLoading = MutableStateFlow(false)
    val isCalendarLoading: StateFlow<Boolean> = _isCalendarLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    // Custom activity types (mine + partner's)
    private val _customActivityTypes = MutableStateFlow<List<CustomActivityTypeResponse>>(emptyList())
    val customActivityTypes: StateFlow<List<CustomActivityTypeResponse>> = _customActivityTypes.asStateFlow()

    init { loadToday(); loadCustomActivityTypes() }

    fun loadToday() {
        viewModelScope.launch {
            _isLoading.value = true
            val today      = DateUtils.getTodayDateString()
            val myDef      = async { activityRepository.getActivities(date = today) }
            val partnerDef = async { activityRepository.getPartnerActivities(date = today) }
            val myResult   = myDef.await()
            val ptResult   = partnerDef.await()
            myResult.onSuccess { list ->
                _myTodayActivities.value = list
                if (_myName.value == null) _myName.value = list.firstOrNull()?.displayName
            }
            ptResult.onSuccess { list ->
                _partnerTodayActivities.value = list
                if (_partnerName.value == null) _partnerName.value = list.firstOrNull()?.displayName
            }.onFailure { /* no partner is ok */ }
            // Push both users' activities to the home-screen widget after both loads finish
            val my      = myResult.getOrElse { emptyList() }
            val pt      = ptResult.getOrElse { emptyList() }
            val customTypes = _customActivityTypes.value

            // Build (displayName, rawIconValue) pairs — rawIconValue is the Material icon
            // key or /uploads/... path, NOT yet an emoji
            fun buildTypeIconPairs(list: List<ActivityResponse>) =
                list.map { a ->
                    val displayName = if (a.activityType.startsWith("c_")) a.title else a.activityType
                    val rawIconValue = if (a.activityType.startsWith("c_")) {
                        val id = a.activityType.removePrefix("c_").toIntOrNull()
                        customTypes.find { it.id == id }?.emoji ?: ""
                    } else {
                        a.activityType  // built-in key like "work", "sport"
                    }
                    Pair(displayName, rawIconValue)
                }.distinctBy { it.first }.take(4)

            val myPairs = buildTypeIconPairs(my)
            val ptPairs = buildTypeIconPairs(pt)
            val myTypes = myPairs.joinToString(",") { it.first }
            val ptTypes = ptPairs.joinToString(",") { it.first }
            // Pre-render each icon to a cached PNG file; fallback to emoji string
            val myIcons = WidgetIconPreparer.prepareIcons(
                context, myPairs.map { it.second }, CUSTOM_ICON_MAP, "my")
            val ptIcons = WidgetIconPreparer.prepareIcons(
                context, ptPairs.map { it.second }, CUSTOM_ICON_MAP, "pt")
            viewModelScope.launch {
                widgetUpdater.pushActivityUpdate(
                    myCount = my.size,
                    myTypes = myTypes,
                    myIcons = myIcons,
                    myName  = _myName.value,
                    ptCount = pt.size,
                    ptTypes = ptTypes,
                    ptIcons = ptIcons,
                    ptName  = _partnerName.value
                )
            }
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
            // Resolve a human-readable title for custom activity types ("c_{id}")
            val displayTitle = if (activityType.startsWith("c_")) {
                val id = activityType.removePrefix("c_").toIntOrNull()
                _customActivityTypes.value.find { it.id == id }?.name ?: activityType
            } else {
                activityType
            }
            activityRepository.createActivity(displayTitle, activityType, durationMinutes, startTime, note)
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
            val cal = java.util.Calendar.getInstance()
            cal.set(year, month, 1)
            val firstDay = calFmt.format(cal.time)
            cal.set(java.util.Calendar.DAY_OF_MONTH, cal.getActualMaximum(java.util.Calendar.DAY_OF_MONTH))
            val lastDay = calFmt.format(cal.time)
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

    fun clearMessages() {
        _errorMessage.value = null
        _successMessage.value = null
    }

    fun loadCustomActivityTypes() {
        viewModelScope.launch {
            activityRepository.getCustomActivityTypes()
                .onSuccess { _customActivityTypes.value = it }
        }
    }

    fun createCustomActivityType(name: String, emoji: String, colorHex: String) {
        viewModelScope.launch {
            activityRepository.createCustomActivityType(name, emoji, colorHex)
                .onSuccess {
                    loadCustomActivityTypes()
                    _successMessage.value = "Тип активности создан"
                }
                .onFailure { _errorMessage.value = it.message }
        }
    }

    fun deleteCustomActivityType(id: Int) {
        viewModelScope.launch {
            activityRepository.deleteCustomActivityType(id)
                .onSuccess { loadCustomActivityTypes() }
                .onFailure { _errorMessage.value = it.message }
        }
    }

    private val _isIconUploading = MutableStateFlow(false)
    val isIconUploading: StateFlow<Boolean> = _isIconUploading.asStateFlow()

    private val _iconUploadUrl = MutableStateFlow<String?>(null)
    val iconUploadUrl: StateFlow<String?> = _iconUploadUrl.asStateFlow()

    fun uploadActivityIcon(uri: Uri) {
        viewModelScope.launch {
            _isIconUploading.value = true
            activityRepository.uploadImage(uri)
                .onSuccess { _iconUploadUrl.value = it }
                .onFailure { _errorMessage.value = it.message }
            _isIconUploading.value = false
        }
    }

    fun clearIconUpload() {
        _iconUploadUrl.value = null
        _isIconUploading.value = false
    }
}
