package com.example.loveapp.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.filter
import androidx.paging.map
import com.example.loveapp.data.api.models.WishRequest
import com.example.loveapp.data.api.models.WishResponse
import com.example.loveapp.data.repository.WishRepository
import com.example.loveapp.utils.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class WishViewModel @Inject constructor(
    private val wishRepository: WishRepository
) : ViewModel() {

    private val _wishes = MutableStateFlow<List<WishResponse>>(emptyList())
    val wishes: StateFlow<List<WishResponse>> = _wishes.asStateFlow()

    // ── Search query with debounce ────────────────────────────────────────
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    /**
     * Paged wishes driven by [searchQuery].
     * Use [androidx.paging.compose.collectAsLazyPagingItems] in the UI.
     */
    val pagedWishes: Flow<PagingData<WishResponse>> = _searchQuery
        .debounce(300L)
        .distinctUntilChanged()
        .flatMapLatest { query ->
            Pager(PagingConfig(pageSize = 20, enablePlaceholders = false)) {
                wishRepository.pagingSource(query)
            }.flow.map { pagingData ->
                pagingData
                    .filter { it.deletedAt == null }
                    .map { wish ->
                        WishResponse(
                            id          = wish.serverId ?: wish.id,
                            title       = wish.title,
                            description = wish.description,
                            userId      = wish.userId,
                            createdAt   = DateUtils.timestampToDateString(wish.createdAt),
                            isCompleted = wish.isCompleted,
                            priority    = wish.priority,
                            category    = wish.category,
                            isPrivate   = wish.isPrivate,
                            imageUrls   = wish.imageUrl,
                            emoji       = wish.emoji,
                            displayName = wish.displayName,
                            userAvatar  = wish.userAvatar
                        )
                    }
            }
        }
        .cachedIn(viewModelScope)

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
        // Observe Room — distinctUntilChanged avoids redundant UI recompositions
        viewModelScope.launch {
            wishRepository.observeAllWishes()
                .distinctUntilChanged()
                .map { list ->
                    list.filter { it.deletedAt == null }
                        .sortedByDescending { it.createdAt }
                        .map { wish ->
                            WishResponse(
                                id          = wish.serverId ?: wish.id,
                                title       = wish.title,
                                description = wish.description,
                                userId      = wish.userId,
                                createdAt   = DateUtils.timestampToDateString(wish.createdAt),
                                isCompleted = wish.isCompleted,
                                priority    = wish.priority,
                                category    = wish.category,
                                isPrivate   = wish.isPrivate,
                                imageUrls   = wish.imageUrl,
                                emoji       = wish.emoji,
                                displayName = wish.displayName,
                                userAvatar  = wish.userAvatar
                            )
                        }
                }
                .collect { _wishes.value = it }
        }
        viewModelScope.launch {
            _currentUserId.value = wishRepository.getCurrentUserId()
        }
        // Pull fresh data from server in the background; Room flow updates the UI automatically
        viewModelScope.launch { wishRepository.refreshFromServer() }
    }

    /** Update the search / filter query. Paged flow reacts automatically via debounce. */
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    /** Triggers a background pull from server; Room flow updates the list automatically. */
    fun loadWishes() {
        viewModelScope.launch {
            _isLoading.value = true
            wishRepository.refreshFromServer()
            _isLoading.value = false
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
            result.onSuccess {
                _successMessage.value = "Wish created"
                _isLoading.value = false
                // Room flow auto-updates the list
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
            val existingPriority = _wishes.value.find { it.id == id }?.priority ?: 0
            val request = WishRequest(
                title = title,
                description = description,
                priority = existingPriority,
                isPrivate = isPrivate,
                imageUrls = imageUrls,
                emoji = emoji
            )
            val result = wishRepository.updateWish(id, request)
            result.onSuccess {
                _successMessage.value = "Wish updated"
                _isLoading.value = false
                // Room flow auto-updates the list
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
            result.onSuccess {
                _successMessage.value = "Wish completed!"
                _isLoading.value = false
                // Room flow auto-updates the list
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
                _successMessage.value = "Wish deleted"
                _isLoading.value = false
                // Room flow auto-updates the list
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
