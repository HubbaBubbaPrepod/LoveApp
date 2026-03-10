package com.example.loveapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.loveapp.data.entity.CoupleTask
import com.example.loveapp.data.repository.RelationshipRepository
import com.example.loveapp.data.repository.TaskRepository
import com.example.loveapp.utils.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TaskViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val relationshipRepository: RelationshipRepository
) : ViewModel() {

    private val _tasks = MutableStateFlow<List<CoupleTask>>(emptyList())
    val tasks: StateFlow<List<CoupleTask>> = _tasks.asStateFlow()

    private val _totalPoints = MutableStateFlow(0)
    val totalPoints: StateFlow<Int> = _totalPoints.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    private var coupleKey: String = ""
    private val today = DateUtils.getTodayDateString()

    init {
        viewModelScope.launch {
            loadCoupleKey()
            taskRepository.refreshFromServer(today)
            observeTasks()
        }
    }

    private suspend fun loadCoupleKey() {
        relationshipRepository.getRelationship().onSuccess {
            val uid1 = it.userId1
            val uid2 = it.userId2
            if (uid1 > 0 && uid2 > 0) {
                coupleKey = "${minOf(uid1, uid2)}_${maxOf(uid1, uid2)}"
            }
        }
    }

    private suspend fun observeTasks() {
        if (coupleKey.isBlank()) return
        viewModelScope.launch {
            taskRepository.observeTotalPoints(coupleKey, today)
                .distinctUntilChanged()
                .collect { _totalPoints.value = it }
        }
        taskRepository.observeByDate(coupleKey, today)
            .distinctUntilChanged()
            .collect { _tasks.value = it }
    }

    fun createTask(title: String, description: String = "", icon: String = "💕", points: Int = 10) {
        viewModelScope.launch {
            _isLoading.value = true
            taskRepository.createTask(title, description, "custom", icon, points, today)
                .onSuccess { _successMessage.value = "Задание создано" }
                .onFailure { _errorMessage.value = it.message }
            _isLoading.value = false
        }
    }

    fun completeTask(localId: Int) {
        viewModelScope.launch {
            taskRepository.completeTask(localId)
                .onSuccess { _successMessage.value = "Задание выполнено!" }
                .onFailure { _errorMessage.value = it.message }
        }
    }

    fun deleteTask(localId: Int) {
        viewModelScope.launch {
            taskRepository.deleteTask(localId)
        }
    }

    fun clearMessages() { _errorMessage.value = null; _successMessage.value = null }
}
