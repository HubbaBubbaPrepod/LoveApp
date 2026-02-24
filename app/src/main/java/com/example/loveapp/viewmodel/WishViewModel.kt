package com.example.loveapp.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.loveapp.data.api.models.WishRequest
import com.example.loveapp.data.api.models.WishResponse
import com.example.loveapp.data.repository.WishRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WishViewModel @Inject constructor(
    private val wishRepository: WishRepository
) : ViewModel() {

    private val _wishes = MutableStateFlow<List<WishResponse>>(emptyList())
    val wishes: StateFlow<List<WishResponse>> = _wishes.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    private val _currentWish = MutableStateFlow<WishResponse?>(null)
    val currentWish: StateFlow<WishResponse?> = _currentWish.asStateFlow()

    private val _uploadedImageUrl = MutableStateFlow<String?>(null)
    val uploadedImageUrl: StateFlow<String?> = _uploadedImageUrl.asStateFlow()

    private val _currentUserId = MutableStateFlow<Int?>(null)
    val currentUserId: StateFlow<Int?> = _currentUserId.asStateFlow()

    init {
        loadWishes()
        viewModelScope.launch {
            _currentUserId.value = wishRepository.getCurrentUserId()
        }
    }

    fun loadWishes() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            val result = wishRepository.getWishes()
            result.onSuccess { wishes ->
                _wishes.value = wishes
                _isLoading.value = false
            }.onFailure { error ->
                _errorMessage.value = error.message ?: "Failed to load wishes"
                _isLoading.value = false
            }
        }
    }

    fun loadWishById(id: Int) {
        viewModelScope.launch {
            _currentWish.value = null
            val result = wishRepository.getWishById(id)
            result.onSuccess { wish -> _currentWish.value = wish }
                .onFailure { _errorMessage.value = it.message }
        }
    }

    fun clearCurrentWish() {
        _currentWish.value = null
        _uploadedImageUrl.value = null
    }

    fun uploadImage(uri: Uri) {
        viewModelScope.launch {
            val result = wishRepository.uploadImage(uri)
            result.onSuccess { url -> _uploadedImageUrl.value = url }
                .onFailure { _errorMessage.value = "Upload failed: ${it.message}" }
        }
    }

    fun createWish(
        title: String,
        description: String,
        priority: Int = 1,
        category: String = "",
        isPrivate: Boolean = false,
        imageUrls: String? = null,
        emoji: String? = null
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            val result = wishRepository.createWish(title, description, priority, category, isPrivate, imageUrls, emoji)
            result.onSuccess { wish ->
                _wishes.value = listOf(wish) + _wishes.value
                _successMessage.value = "Wish created"
                _isLoading.value = false
            }.onFailure { error ->
                _errorMessage.value = error.message ?: "Failed to create wish"
                _isLoading.value = false
            }
        }
    }

    fun updateWish(
        id: Int,
        title: String,
        description: String,
        isPrivate: Boolean = false,
        imageUrls: String? = null,
        emoji: String? = null
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            val request = WishRequest(
                title = title,
                description = description,
                priority = 1,
                isPrivate = isPrivate,
                imageUrls = imageUrls,
                emoji = emoji
            )
            val result = wishRepository.updateWish(id, request)
            result.onSuccess { updated ->
                _wishes.value = _wishes.value.map { if (it.id == id) updated else it }
                _successMessage.value = "Wish updated"
                _isLoading.value = false
            }.onFailure { error ->
                _errorMessage.value = error.message ?: "Failed to update wish"
                _isLoading.value = false
            }
        }
    }

    fun completeWish(id: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            val result = wishRepository.completeWish(id)
            result.onSuccess { updatedWish ->
                _wishes.value = _wishes.value.map { if (it.id == id) updatedWish else it }
                _successMessage.value = "Wish completed!"
                _isLoading.value = false
            }.onFailure { error ->
                _errorMessage.value = error.message ?: "Failed to complete wish"
                _isLoading.value = false
            }
        }
    }

    fun deleteWish(id: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            val result = wishRepository.deleteWish(id)
            result.onSuccess {
                _wishes.value = _wishes.value.filter { it.id != id }
                _successMessage.value = "Wish deleted"
                _isLoading.value = false
            }.onFailure { error ->
                _errorMessage.value = error.message ?: "Failed to delete wish"
                _isLoading.value = false
            }
        }
    }

    fun clearMessages() {
        _errorMessage.value = null
        _successMessage.value = null
    }
}
