package com.example.loveapp.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.loveapp.data.api.models.BucketItemRequest
import com.example.loveapp.data.api.models.BucketItemResponse
import com.example.loveapp.data.api.models.BucketListStatsResponse
import com.example.loveapp.data.repository.BucketListRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BucketListViewModel @Inject constructor(
    private val repository: BucketListRepository
) : ViewModel() {

    private val _items = MutableStateFlow<List<BucketItemResponse>>(emptyList())
    val items: StateFlow<List<BucketItemResponse>> = _items

    private val _stats = MutableStateFlow(BucketListStatsResponse())
    val stats: StateFlow<BucketListStatsResponse> = _stats

    private val _categories = MutableStateFlow<List<String>>(emptyList())
    val categories: StateFlow<List<String>> = _categories

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory

    private val _showCompleted = MutableStateFlow<Boolean?>(null)
    val showCompleted: StateFlow<Boolean?> = _showCompleted

    private val _showCreateDialog = MutableStateFlow(false)
    val showCreateDialog: StateFlow<Boolean> = _showCreateDialog

    private val _isCreating = MutableStateFlow(false)
    val isCreating: StateFlow<Boolean> = _isCreating

    init {
        loadItems()
    }

    fun loadItems() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            repository.getItems(
                category = _selectedCategory.value,
                completed = _showCompleted.value?.toString()
            ).onSuccess { data ->
                _items.value = data.items
                _stats.value = data.stats ?: BucketListStatsResponse()
                _categories.value = data.categories
            }.onFailure {
                _error.value = it.message
            }

            _isLoading.value = false
        }
    }

    fun selectCategory(category: String?) {
        _selectedCategory.value = if (_selectedCategory.value == category) null else category
        loadItems()
    }

    fun setShowCompleted(show: Boolean?) {
        _showCompleted.value = show
        loadItems()
    }

    fun createItem(title: String, description: String?, category: String, emoji: String, targetDate: String?) {
        if (_isCreating.value) return
        viewModelScope.launch {
            _isCreating.value = true
            repository.createItem(
                BucketItemRequest(
                    title = title,
                    description = description,
                    category = category,
                    emoji = emoji,
                    targetDate = targetDate
                )
            ).onSuccess {
                _showCreateDialog.value = false
                loadItems()
            }.onFailure {
                _error.value = it.message
            }
            _isCreating.value = false
        }
    }

    private val operatingIds = mutableSetOf<Long>()

    fun completeItem(id: Long) {
        if (id in operatingIds) return
        operatingIds.add(id)
        viewModelScope.launch {
            repository.completeItem(id).onSuccess {
                loadItems()
            }.onFailure {
                _error.value = it.message
            }
            operatingIds.remove(id)
        }
    }

    fun uncompleteItem(id: Long) {
        if (id in operatingIds) return
        operatingIds.add(id)
        viewModelScope.launch {
            repository.uncompleteItem(id).onSuccess {
                loadItems()
            }.onFailure {
                _error.value = it.message
            }
            operatingIds.remove(id)
        }
    }

    fun deleteItem(id: Long) {
        if (id in operatingIds) return
        operatingIds.add(id)
        viewModelScope.launch {
            repository.deleteItem(id).onSuccess {
                loadItems()
            }.onFailure {
                _error.value = it.message
            }
            operatingIds.remove(id)
        }
    }

    fun toggleCreateDialog() {
        _showCreateDialog.value = !_showCreateDialog.value
    }
}
