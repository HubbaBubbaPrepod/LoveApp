package com.example.loveapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.loveapp.data.entity.GalleryPhoto
import com.example.loveapp.data.repository.GalleryRepository
import com.example.loveapp.data.repository.RelationshipRepository
import com.example.loveapp.utils.TokenManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GalleryViewModel @Inject constructor(
    private val galleryRepository: GalleryRepository,
    private val relationshipRepository: RelationshipRepository,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _photos = MutableStateFlow<List<GalleryPhoto>>(emptyList())
    val photos: StateFlow<List<GalleryPhoto>> = _photos.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private var coupleKey: String = ""

    init {
        viewModelScope.launch {
            loadCoupleKey()
            refreshFromServer()
            observePhotos()
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

    private suspend fun observePhotos() {
        if (coupleKey.isBlank()) return
        galleryRepository.observePhotos(coupleKey)
            .distinctUntilChanged()
            .collect { _photos.value = it }
    }

    private suspend fun refreshFromServer() {
        _isLoading.value = true
        galleryRepository.refreshFromServer()
        _isLoading.value = false
    }

    fun addPhoto(imageUrl: String, thumbnailUrl: String? = null, caption: String = "") {
        viewModelScope.launch {
            galleryRepository.addPhoto(imageUrl, thumbnailUrl, caption)
                .onFailure { _errorMessage.value = it.message }
        }
    }

    fun deletePhoto(localId: Int) {
        viewModelScope.launch { galleryRepository.deletePhoto(localId) }
    }

    fun clearError() { _errorMessage.value = null }
}
