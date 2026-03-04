package com.example.loveapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.loveapp.data.api.models.MoodResponse
import com.example.loveapp.data.entity.MoodEntry
import com.example.loveapp.data.repository.AuthRepository
import com.example.loveapp.data.repository.MoodRepository
import com.example.loveapp.data.repository.RelationshipRepository
import com.example.loveapp.utils.DateUtils
import com.example.loveapp.utils.TokenManager
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
    private val widgetUpdater: WidgetUpdater,
    private val authRepository: AuthRepository,
    private val relationshipRepository: RelationshipRepository,
    private val tokenManager: TokenManager
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

    // Month cache: (year, month) → (myMoods grouped by date, partnerMoods grouped by date)
    private val monthCache =
        mutableMapOf<Pair<Int, Int>, Pair<Map<String, List<MoodResponse>>, Map<String, List<MoodResponse>>>>()

    // Current user's server ID, resolved once during init
    private var myUserId: Int = 0

    init {
        loadUserInfo()
        viewModelScope.launch {
            myUserId = tokenManager.getUserId()?.toIntOrNull() ?: 0
            observeToday()
        }
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

    /** Subscribe to Room for today's moods — auto-updates on any write without a network call. */
    private suspend fun observeToday() {
        val today = DateUtils.getTodayDateString()
        moodRepository.observeByDate(today).collect { all ->
            val mine   = all.filter { it.userId == myUserId || it.userId == 0 }.map { it.toResponse() }
            val theirs = all.filter { it.userId != myUserId && it.userId != 0 }.map { it.toResponse() }
            _myTodayMoods.value    = mine
            _partnerTodayMoods.value = theirs
            // Push widget update when today's moods change
            val myFirst = mine.firstOrNull()
            val ptFirst = theirs.firstOrNull()
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
        }
    }

    /** Maps a Room entity to the API response model used by the UI. */
    private fun MoodEntry.toResponse() = MoodResponse(
        id        = serverId ?: id,
        userId    = userId,
        moodType  = moodType,
        timestamp = "",
        date      = date,
        note      = note,
        color     = color.ifBlank { null },
        displayName = null,  // comes from loadUserInfo()
        userAvatar  = null,   // comes from loadUserInfo()
    )

    // loadToday() kept as a public no-op for backward-compat call sites that may still reference it
    @Deprecated("Room Flow now drives today's moods — no manual reload needed", ReplaceWith(""))
    fun loadToday() { /* no-op: Room observeByDate handles updates reactively */ }

    fun addMood(moodType: String, note: String = "") {
        viewModelScope.launch {
            _isLoading.value = true
            val today = DateUtils.getTodayDateString()
            moodRepository.createMood(moodType, today, note)
                .onSuccess { _successMessage.value = "Запись сохранена" }
                .onFailure { _errorMessage.value = it.message }
            _isLoading.value = false
            // No loadToday() call — Room Flow auto-updates via observeByDate
        }
    }

    fun deleteMood(id: Int) {
        viewModelScope.launch {
            moodRepository.deleteMood(id)
                .onFailure { _errorMessage.value = it.message }
            // No loadToday() call — Room Flow auto-updates
        }
    }

    fun loadCalendarMonth(year: Int, month: Int) { // month 0-based
        _calendarYear.value = year
        _calendarMonth.value = month

        // Return cached data immediately without a network round-trip
        val key = year to month
        monthCache[key]?.let { (mine, partner) ->
            _myMonthMoods.value    = mine
            _partnerMonthMoods.value = partner
            return
        }

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
            val myGrouped = myDef.await().getOrElse { emptyList() }.groupBy { it.date.take(10) }
            val partnerGrouped = partnerDef.await().getOrElse { emptyList() }.groupBy { it.date.take(10) }

            _myMonthMoods.value      = myGrouped
            _partnerMonthMoods.value = partnerGrouped
            monthCache[key]          = myGrouped to partnerGrouped

            // Back-fill name/avatar from response if not yet set
            myGrouped.values.flatten().firstOrNull()?.let { m ->
                if (_myName.value == null && !m.displayName.isNullOrBlank()) _myName.value = m.displayName
            }
            partnerGrouped.values.flatten().firstOrNull()?.let { m ->
                if (_partnerName.value == null && !m.displayName.isNullOrBlank()) _partnerName.value = m.displayName
            }
            _isCalendarLoading.value = false
        }
    }

    // Legacy compat for existing call sites
    fun createMood(moodType: String) = addMood(moodType)
    fun loadTodayMoods() { /* no-op: Room Flow handles it */ }

    fun clearMessages() {
        _errorMessage.value = null
        _successMessage.value = null
    }
}

