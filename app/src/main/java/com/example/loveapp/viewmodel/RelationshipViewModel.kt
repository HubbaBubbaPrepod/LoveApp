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
import javax.inject.Inject

@HiltViewModel
class RelationshipViewModel @Inject constructor(
    private val relationshipRepository: RelationshipRepository
) : ViewModel() {

    private val _relationship = MutableStateFlow<RelationshipResponse?>(null)
    val relationship: StateFlow<RelationshipResponse?> = _relationship.asStateFlow()

    private val _daysSinceStart = MutableStateFlow(0L)
    val daysSinceStart: StateFlow<Long> = _daysSinceStart.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    init {
        loadRelationship()
    }

    fun loadRelationship() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            val result = relationshipRepository.getRelationship()
            result.onSuccess { relationship ->
                _relationship.value = relationship
                calculateDaysSinceStart(relationship)
                _isLoading.value = false
            }.onFailure { error ->
                _errorMessage.value = error.message ?: "Failed to load relationship"
                _isLoading.value = false
            }
        }
    }

    fun updateRelationship(startDate: String, firstKissDate: String? = null, anniversaryDate: String? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            val result = relationshipRepository.updateRelationship(startDate, firstKissDate, anniversaryDate)
            
            result.onSuccess { relationship ->
                _relationship.value = relationship
                calculateDaysSinceStart(relationship)
                _successMessage.value = "Relationship info updated"
                _isLoading.value = false
            }.onFailure { error ->
                _errorMessage.value = error.message ?: "Failed to update relationship"
                _isLoading.value = false
            }
        }
    }

    private fun calculateDaysSinceStart(relationship: RelationshipResponse) {
        try {
            val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            val startDate = dateFormat.parse(relationship.relationshipStartDate)
            if (startDate != null) {
                val days = (System.currentTimeMillis() - startDate.time) / (1000 * 60 * 60 * 24)
                _daysSinceStart.value = days
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun clearMessages() {
        _errorMessage.value = null
        _successMessage.value = null
    }
}
