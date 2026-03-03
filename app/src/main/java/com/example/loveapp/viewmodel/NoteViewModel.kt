package com.example.loveapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.loveapp.data.api.models.NoteResponse
import com.example.loveapp.data.repository.NoteRepository
import com.example.loveapp.utils.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
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

    private val _currentNote = MutableStateFlow<NoteResponse?>(null)
    val currentNote: StateFlow<NoteResponse?> = _currentNote.asStateFlow()

    private val _currentUserId = MutableStateFlow<Int?>(null)
    val currentUserId: StateFlow<Int?> = _currentUserId.asStateFlow()

    init {
        // Observe Room — instantly shows cached data, auto-updates when server sync writes new rows
        viewModelScope.launch {
            noteRepository.observeAllNotes()
                .map { list ->
                    list.filter { it.deletedAt == null }
                        .sortedByDescending { it.updatedAt }
                        .map { note ->
                            NoteResponse(
                                id          = note.serverId ?: note.id,
                                title       = note.title,
                                content     = note.content,
                                userId      = note.userId,
                                createdAt   = DateUtils.timestampToDateString(note.createdAt),
                                updatedAt   = DateUtils.timestampToDateString(note.updatedAt),
                                isPrivate   = note.isPrivate,
                                tags        = note.tags,
                                displayName = note.displayName,
                                userAvatar  = note.userAvatar
                            )
                        }
                }
                .collect { _notes.value = it }
        }
        viewModelScope.launch {
            _currentUserId.value = noteRepository.getCurrentUserId()
        }
        // Pull fresh data from server in the background; Room flow updates the UI automatically
        viewModelScope.launch { noteRepository.refreshFromServer() }
    }

    /** Triggers a background pull from server; Room flow updates the list automatically. */
    fun loadNotes() {
        viewModelScope.launch {
            _isLoading.value = true
            noteRepository.refreshFromServer()
            _isLoading.value = false
        }
    }

    fun loadNoteById(id: Int) {
        viewModelScope.launch {
            _currentNote.value = null
            val result = noteRepository.getNoteById(id)
            result.onSuccess { note ->
                _currentNote.value = note
            }.onFailure { error ->
                _errorMessage.value = error.message ?: "Failed to load note"
            }
        }
    }

    fun clearCurrentNote() {
        _currentNote.value = null
    }

    fun createNote(title: String, content: String, isPrivate: Boolean = false, tags: String = "") {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            val result = noteRepository.createNote(
                title = title,
                content = content,
                userId = _currentUserId.value ?: 0,
                isPrivate = isPrivate,
                tags = tags
            )
            
            result.onSuccess {
                _successMessage.value = "Note created successfully"
                _isLoading.value = false
                // Room flow auto-updates the list
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
            
            val result = noteRepository.updateNote(id, title, content, isPrivate, "")
            
            result.onSuccess {
                _successMessage.value = "Note updated successfully"
                _isLoading.value = false
                // Room flow auto-updates the list
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
                _successMessage.value = "Note deleted successfully"
                _isLoading.value = false
                // Room flow auto-updates the list
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
