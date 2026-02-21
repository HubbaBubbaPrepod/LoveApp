package com.example.loveapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.loveapp.data.api.models.NoteResponse
import com.example.loveapp.data.repository.NoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NoteViewModel @Inject constructor(
    private val noteRepository: NoteRepository
) : ViewModel() {

    private val _notes = MutableStateFlow<List<NoteResponse>>(emptyList())
    val notes: StateFlow<List<NoteResponse>> = _notes.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    private val _currentPage = MutableStateFlow(1)

    init {
        loadNotes()
    }

    fun loadNotes() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            val result = noteRepository.getNotes(_currentPage.value)
            result.onSuccess { noteList ->
                _notes.value = noteList
                _isLoading.value = false
            }.onFailure { error ->
                _errorMessage.value = error.message ?: "Failed to load notes"
                _isLoading.value = false
            }
        }
    }

    fun createNote(title: String, content: String, isPrivate: Boolean = false, tags: String = "") {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            val result = noteRepository.createNote(
                title = title,
                content = content,
                userId = 1,
                isPrivate = isPrivate,
                tags = tags
            )
            
            result.onSuccess { note ->
                _notes.value = listOf(note) + _notes.value
                _successMessage.value = "Note created successfully"
                _isLoading.value = false
            }.onFailure { error ->
                _errorMessage.value = error.message ?: "Failed to create note"
                _isLoading.value = false
            }
        }
    }

    fun updateNote(id: Int, title: String, content: String, isPrivate: Boolean = false) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            val noteRequest = com.example.loveapp.data.api.models.NoteRequest(
                title = title,
                content = content,
                isPrivate = isPrivate
            )
            
            val result = noteRepository.updateNote(id, noteRequest)
            
            result.onSuccess { updatedNote ->
                _notes.value = _notes.value.map {
                    if (it.id == id) updatedNote else it
                }
                _successMessage.value = "Note updated successfully"
                _isLoading.value = false
            }.onFailure { error ->
                _errorMessage.value = error.message ?: "Failed to update note"
                _isLoading.value = false
            }
        }
    }

    fun deleteNote(id: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            val result = noteRepository.deleteNote(id)
            
            result.onSuccess {
                _notes.value = _notes.value.filter { it.id != id }
                _successMessage.value = "Note deleted successfully"
                _isLoading.value = false
            }.onFailure { error ->
                _errorMessage.value = error.message ?: "Failed to delete note"
                _isLoading.value = false
            }
        }
    }

    fun clearMessages() {
        _errorMessage.value = null
        _successMessage.value = null
    }
}
