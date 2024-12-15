package com.example.valenote.ui.note

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.valenote.database.models.Note
import com.example.valenote.repository.NoteRepository
import kotlinx.coroutines.launch

class NoteViewModel(private val repository: NoteRepository) : ViewModel() {
    fun searchNotes(userId: String, query: String): LiveData<List<Note>> {
        return repository.searchNotes(userId, query)
    }

    fun updatePinStatus(noteId: Int, isPinned: Boolean) {
        viewModelScope.launch {
            repository.updatePinStatus(noteId, isPinned)
        }
    }
    fun deleteNotes(noteIds: Set<Int>) {
        Log.d("NOTE_DELETE_MULTI", "====================")
        Log.d("NOTE_DELETE_MULTI", "$noteIds")
        val safeNoteIds = noteIds.toSet()
        viewModelScope.launch {
            Log.d("NOTE_DELETE_MULTI viewmodel lifescope", "$safeNoteIds")
            safeNoteIds.forEach { noteId ->
                Log.d("NOTE_DELETE_MULTI", "${noteId}")
                repository.markNoteAsDeleted(noteId)
            }
        }
    }

    fun pinNotes(noteIds: Set<Int>) {
        Log.d("NOTE_PIN_MULTI", "====================")
        Log.d("NOTE_PIN_MULTI", "$noteIds")
        val safeNoteIds = noteIds.toSet()
        viewModelScope.launch {
            Log.d("NOTE_PIN_MULTI viewmodel lifescope", "$safeNoteIds")
            safeNoteIds.forEach { noteId ->
                val note = repository.getNoteById(noteId)
                if (note != null) {
                    val newPinStatus = !note.isPinned
                    Log.d("NOTE_PIN_MULTI", "Updating noteId: $noteId to isPinned: $newPinStatus")
                    repository.updatePinStatus(noteId, newPinStatus)
                } else {
                    Log.e("NOTE_PIN_MULTI", "Note with id $noteId not found")
                }
            }
        }
    }

    fun getNoteById(noteId: Int): LiveData<Note?> {
        val noteData = MutableLiveData<Note?>()
        viewModelScope.launch {
            noteData.postValue(repository.getNoteById(noteId))
        }
        return noteData
    }

    fun addNote(note: Note) {
        viewModelScope.launch {
            repository.addNoteOffline(note)
        }
    }

    fun updateNote(note: Note) {
        viewModelScope.launch {
            repository.updateNote(note)
        }
    }

    fun deleteNoteById(noteId: Int) {
        viewModelScope.launch {
            repository.markNoteAsDeleted(noteId)
        }
    }

    fun syncAllNotes(onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                repository.syncAllNotes()
                onComplete(true)
            } catch (e: Exception) {
                onComplete(false)
            }
        }
    }
}
