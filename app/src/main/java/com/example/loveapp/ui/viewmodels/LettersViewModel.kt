package com.example.loveapp.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.loveapp.data.api.models.LoveLetterRequest
import com.example.loveapp.data.api.models.LoveLetterResponse
import com.example.loveapp.data.api.models.LoveLetterStatsResponse
import com.example.loveapp.data.repository.LettersRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LettersViewModel @Inject constructor(
    private val repository: LettersRepository
) : ViewModel() {

    private val _letters = MutableStateFlow<List<LoveLetterResponse>>(emptyList())
    val letters: StateFlow<List<LoveLetterResponse>> = _letters

    private val _stats = MutableStateFlow(LoveLetterStatsResponse())
    val stats: StateFlow<LoveLetterStatsResponse> = _stats

    private val _selectedLetter = MutableStateFlow<LoveLetterResponse?>(null)
    val selectedLetter: StateFlow<LoveLetterResponse?> = _selectedLetter

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _filter = MutableStateFlow<String?>(null)
    val filter: StateFlow<String?> = _filter

    private val _showCreateDialog = MutableStateFlow(false)
    val showCreateDialog: StateFlow<Boolean> = _showCreateDialog

    init {
        loadLetters()
    }

    fun loadLetters() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            repository.getLetters(_filter.value).onSuccess {
                _letters.value = it
            }.onFailure {
                _error.value = it.message
            }

            repository.getStats().onSuccess {
                _stats.value = it
            }

            _isLoading.value = false
        }
    }

    fun setFilter(f: String?) {
        _filter.value = if (_filter.value == f) null else f
        loadLetters()
    }

    fun openLetter(id: Long) {
        viewModelScope.launch {
            repository.getLetter(id).onSuccess { letter ->
                _selectedLetter.value = letter
                // Refresh list to update opened status
                loadLetters()
            }.onFailure {
                _error.value = it.message
            }
        }
    }

    fun closeLetter() {
        _selectedLetter.value = null
    }

    fun createLetter(title: String, content: String, mood: String?, openDate: String) {
        viewModelScope.launch {
            _isLoading.value = true
            repository.createLetter(
                LoveLetterRequest(
                    title = title,
                    content = content,
                    mood = mood,
                    openDate = openDate
                )
            ).onSuccess {
                _showCreateDialog.value = false
                loadLetters()
            }.onFailure {
                _error.value = it.message
            }
            _isLoading.value = false
        }
    }

    fun deleteLetter(id: Long) {
        viewModelScope.launch {
            repository.deleteLetter(id).onSuccess {
                _selectedLetter.value = null
                loadLetters()
            }.onFailure {
                _error.value = it.message
            }
        }
    }

    fun toggleCreateDialog() {
        _showCreateDialog.value = !_showCreateDialog.value
    }
}
