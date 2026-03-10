package com.example.loveapp.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.loveapp.data.api.models.MomentRequest
import com.example.loveapp.data.api.models.MomentResponse
import com.example.loveapp.data.repository.MomentsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MomentsViewModel @Inject constructor(
    private val momentsRepository: MomentsRepository
) : ViewModel() {

    private val _moments = MutableStateFlow<List<MomentResponse>>(emptyList())
    val moments: StateFlow<List<MomentResponse>> = _moments.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isSharing = MutableStateFlow(false)
    val isSharing: StateFlow<Boolean> = _isSharing.asStateFlow()

    private val _isUploading = MutableStateFlow(false)
    val isUploading: StateFlow<Boolean> = _isUploading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _momentShared = MutableStateFlow(false)
    val momentShared: StateFlow<Boolean> = _momentShared.asStateFlow()

    private var currentPage = 1

    init {
        loadMoments()
    }

    private fun loadMoments() {
        viewModelScope.launch {
            _isLoading.value = true
            currentPage = 1
            momentsRepository.getMoments(page = 1)
                .onSuccess { _moments.value = it }
                .onFailure { _errorMessage.value = it.message }
            _isLoading.value = false
        }
    }

    private var isLoadingMore = false

    fun loadMore() {
        if (isLoadingMore) return
        isLoadingMore = true
        viewModelScope.launch {
            momentsRepository.getMoments(page = currentPage + 1)
                .onSuccess { items ->
                    if (items.isNotEmpty()) {
                        currentPage++
                        _moments.value = _moments.value + items
                    }
                }
                .onFailure { _errorMessage.value = "Ошибка загрузки: ${it.message}" }
            isLoadingMore = false
        }
    }

    fun shareMoment(content: String, imageUrl: String? = null, mood: String? = null, locationName: String? = null) {
        viewModelScope.launch {
            _isSharing.value = true
            momentsRepository.shareMoment(
                MomentRequest(
                    content = content,
                    imageUrl = imageUrl,
                    mood = mood,
                    locationName = locationName
                )
            )
                .onSuccess {
                    _momentShared.value = true
                    loadMoments()
                }
                .onFailure { _errorMessage.value = it.message }
            _isSharing.value = false
        }
    }

    /** Upload image, then share moment with the resulting URL. */
    fun shareMomentWithImage(uri: Uri, content: String, mood: String? = null, locationName: String? = null) {
        viewModelScope.launch {
            _isSharing.value = true
            _isUploading.value = true
            try {
                momentsRepository.uploadImage(uri)
                    .onSuccess { url ->
                        _isUploading.value = false
                        momentsRepository.shareMoment(
                            MomentRequest(content = content, imageUrl = url, mood = mood, locationName = locationName)
                        )
                            .onSuccess {
                                _momentShared.value = true
                                loadMoments()
                            }
                            .onFailure { _errorMessage.value = it.message }
                    }
                    .onFailure { _errorMessage.value = "Ошибка загрузки фото: ${it.message}" }
            } catch (e: Exception) {
                _errorMessage.value = "Ошибка: ${e.message}"
            } finally {
                _isUploading.value = false
                _isSharing.value = false
            }
        }
    }

    private val deletingIds = mutableSetOf<Int>()

    fun deleteMoment(id: Int) {
        if (id in deletingIds) return
        deletingIds.add(id)
        viewModelScope.launch {
            momentsRepository.deleteMoment(id)
                .onSuccess { _moments.value = _moments.value.filter { it.id != id } }
                .onFailure { _errorMessage.value = it.message }
            deletingIds.remove(id)
        }
    }

    fun refresh() { loadMoments() }

    fun clearMessages() {
        _errorMessage.value = null
        _momentShared.value = false
    }
}
