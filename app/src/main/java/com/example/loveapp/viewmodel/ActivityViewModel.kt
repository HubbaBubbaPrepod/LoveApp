package com.example.loveapp.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.loveapp.data.api.models.ActivityResponse
import com.example.loveapp.data.api.models.CustomActivityTypeResponse
import com.example.loveapp.data.entity.ActivityLog
import com.example.loveapp.data.repository.ActivityRepository
import com.example.loveapp.data.repository.AuthRepository
import com.example.loveapp.data.repository.RelationshipRepository
import com.example.loveapp.ui.screens.CUSTOM_ICON_MAP
import com.example.loveapp.widget.WidgetIconPreparer
import com.example.loveapp.utils.DateUtils
import com.example.loveapp.utils.TokenManager
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
    private val widgetUpdater: WidgetUpdater,
    private val authRepository: AuthRepository,
    private val relationshipRepository: RelationshipRepository,
    private val tokenManager: TokenManager
) : ViewModel() {

    // Shared class-level formatter вЂ” not created on every loadCalendarMonth() call
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

    private val _myAvatar = MutableStateFlow<String?>(null)
    val myAvatar: StateFlow<String?> = _myAvatar.asStateFlow()

    private val _partnerAvatar = MutableStateFlow<String?>(null)
    val partnerAvatar: StateFlow<String?> = _partnerAvatar.asStateFlow()

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

    // Month cache: (year, month) в†’ (myActivities grouped by date, partnerActivities grouped by date)
    private val monthCache =
        mutableMapOf<Pair<Int, Int>, Pair<Map<String, List<ActivityResponse>>, Map<String, List<ActivityResponse>>>>()

    // Current user's server ID, resolved once during init
    private var myUserId: Int = 0

    init {
        loadUserInfo()
        loadCustomActivityTypes()
        viewModelScope.launch {
            myUserId = tokenManager.getUserId()?.toIntOrNull() ?: 0
            observeToday()
        }
        // Pull fresh data from server; Room Flow in observeToday() auto-updates the UI
        viewModelScope.launch { activityRepository.refreshFromServer() }
    }

    private fun loadUserInfo() {
        viewModelScope.launch {
            val profileDef = async { authRepository.getProfile() }
            val relDef     = async { relationshipRepository.getRelationship() }
            profileDef.await().onSuccess {
                _myName.value   = it.displayName
                _myAvatar.value = it.profileImage
            }
            relDef.await().onSuccess {
                if (!it.partnerDisplayName.isNullOrBlank())
                    _partnerName.value = it.partnerDisplayName
                if (!it.partnerAvatar.isNullOrBlank())
                    _partnerAvatar.value = it.partnerAvatar
                if (_myAvatar.value.isNullOrBlank() && !it.myAvatar.isNullOrBlank())
                    _myAvatar.value = it.myAvatar
            }
        }
    }

    /** Subscribe to Room for today's activities вЂ” auto-updates on any write without a network call. */
    private suspend fun observeToday() {
        val today = DateUtils.getTodayDateString()
        activityRepository.observeByDate(today).collect { all ->
            val mine   = all.filter { it.userId == myUserId || it.userId == 0 }.map { it.toResponse() }
            val theirs = all.filter { it.userId != myUserId && it.userId != 0 }.map { it.toResponse() }
            _myTodayActivities.value      = mine
            _partnerTodayActivities.value = theirs
            // Push widget update when today's activities change
            viewModelScope.launch { pushWidgetUpdate(mine, theirs) }
        }
    }

    /** Maps a Room entity to the API response model used by the UI. */
    private fun ActivityLog.toResponse() = ActivityResponse(
        id             = serverId ?: id,
        userId         = userId,
        title          = title,
        description    = description,
        timestamp      = "",
        date           = date,
        imageUrls      = imageUrls,
        category       = category,
        activityType   = category,
        durationMinutes = 0,
        startTime      = "",
        note           = description,
        displayName    = null,
        userAvatar     = null,
    )

    private suspend fun pushWidgetUpdate(my: List<ActivityResponse>, pt: List<ActivityResponse>) {
        val customTypes = _customActivityTypes.value

        fun buildTypeIconPairs(list: List<ActivityResponse>) =
            list.map { a ->
                val displayName = if (a.activityType.startsWith("c_")) a.title else a.activityType
                val rawIconValue = if (a.activityType.startsWith("c_")) {
                    val id = a.activityType.removePrefix("c_").toIntOrNull()
                    customTypes.find { it.id == id }?.emoji ?: ""
                } else a.activityType
                Pair(displayName, rawIconValue)
            }.distinctBy { it.first }.take(4)

        val myPairs = buildTypeIconPairs(my)
        val ptPairs = buildTypeIconPairs(pt)
        val myTypes = myPairs.joinToString(",") { it.first }
        val ptTypes = ptPairs.joinToString(",") { it.first }
        val myIcons = WidgetIconPreparer.prepareIcons(context, myPairs.map { it.second }, CUSTOM_ICON_MAP, "my")
        val ptIcons = WidgetIconPreparer.prepareIcons(context, ptPairs.map { it.second }, CUSTOM_ICON_MAP, "pt")
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

    // loadToday() kept for backward-compat call sites
    @Deprecated("Room Flow now drives today's activities вЂ” no manual reload needed", ReplaceWith(""))
    fun loadToday() { /* no-op: Room observeByDate handles updates reactively */ }

    fun createActivity(
        activityType: String,
        durationMinutes: Int,
        startTime: String,
        note: String
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            val displayTitle = if (activityType.startsWith("c_")) {
                val id = activityType.removePrefix("c_").toIntOrNull()
                _customActivityTypes.value.find { it.id == id }?.name ?: activityType
            } else activityType
            activityRepository.createActivity(displayTitle, activityType, durationMinutes, startTime, note)
                .onSuccess { _successMessage.value = "Активность сохранена" }
                .onFailure { _errorMessage.value = it.message }
            _isLoading.value = false
            // No loadToday() call вЂ” Room Flow auto-updates via observeByDate
        }
    }

    fun deleteActivity(id: Int) {
        viewModelScope.launch {
            activityRepository.deleteActivity(id)
                .onFailure { _errorMessage.value = it.message }
            // No loadToday() call вЂ” Room Flow auto-updates
        }
    }

    fun loadCalendarMonth(year: Int, month: Int) {
        _calendarYear.value = year
        _calendarMonth.value = month

        // Return cached data immediately without a network round-trip
        val key = year to month
        monthCache[key]?.let { (mine, partner) ->
            _myMonthActivities.value    = mine
            _partnerMonthActivities.value = partner
            return
        }

        viewModelScope.launch {
            _isCalendarLoading.value = true
            val cal = java.util.Calendar.getInstance()
            cal.set(year, month, 1)
            val firstDay = calFmt.format(cal.time)
            cal.set(java.util.Calendar.DAY_OF_MONTH, cal.getActualMaximum(java.util.Calendar.DAY_OF_MONTH))
            val lastDay = calFmt.format(cal.time)
            val myDef      = async { activityRepository.getActivities(startDate = firstDay, endDate = lastDay) }
            val partnerDef = async { activityRepository.getPartnerActivities(startDate = firstDay, endDate = lastDay) }
            val myGrouped      = myDef.await().getOrElse { emptyList() }.groupBy { it.date.take(10) }
            val partnerGrouped = partnerDef.await().getOrElse { emptyList() }.groupBy { it.date.take(10) }

            _myMonthActivities.value      = myGrouped
            _partnerMonthActivities.value = partnerGrouped
            monthCache[key]               = myGrouped to partnerGrouped

            myGrouped.values.flatten().firstOrNull()?.let { a ->
                if (_myName.value == null && !a.displayName.isNullOrBlank()) _myName.value = a.displayName
            }
            partnerGrouped.values.flatten().firstOrNull()?.let { a ->
                if (_partnerName.value == null && !a.displayName.isNullOrBlank()) _partnerName.value = a.displayName
            }
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
