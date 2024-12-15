package com.example.valenote.repository

import androidx.lifecycle.LiveData
import com.example.valenote.api.ApiService
import com.example.valenote.database.NoteDao
import com.example.valenote.database.models.Note
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class NoteRepository(private val noteDao: NoteDao, private val apiService: ApiService) {

    fun getAllNotesForUser(userId: String): LiveData<List<Note>> {
        return noteDao.getAllNotesForUser(userId)
    }

    suspend fun getNoteById(id: Int): Note? {
        return noteDao.getNoteById(id)
    }

    fun searchNotes(userId: String, query: String): LiveData<List<Note>> {
        return noteDao.searchNotes(userId, query)
    }

    suspend fun updatePinStatus(noteId: Int, isPinned: Boolean) {
        noteDao.updatePinStatus(noteId, isPinned)
    }


    suspend fun addNote(note: Note): Long {
        return noteDao.insertNote(note)
    }

    suspend fun updateNote(note: Note): Int {
        return noteDao.updateNote(note)
    }

    suspend fun deleteNote(note: Note): Int {
        return noteDao.deleteNote(note)
    }

    suspend fun deleteNoteById(noteId: Int) {
        noteDao.deleteNoteById(noteId)
    }

    // Tambahkan catatan secara offline
    suspend fun addNoteOffline(note: Note): Long {
        val offlineNote = note.copy(isSynced = false)
        return noteDao.insertNote(offlineNote)
    }

    suspend fun markNoteAsDeleted(noteId: Int) {
        val note = noteDao.getNoteById(noteId)
        if (note != null) {
            val updatedNote = note.copy(isDeleted = true, isSynced = false)
            noteDao.updateNote(updatedNote)
        }
    }

    suspend fun syncOfflineNotes() = withContext(Dispatchers.IO) {
        val offlineNotes = noteDao.getUnsyncedNotes()
        for (note in offlineNotes) {
            try {
                val response = apiService.createNote(note.copy(id = 0)).execute() // id = 0 untuk menghindari Room id
                if (response.isSuccessful) {
                    response.body()?.let { apiNote ->
                        noteDao.updateNote(
                            note.copy(
                                apiId = apiNote.apiId,
                                isSynced = true,
                                updatedAt = apiNote.updatedAt
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }



    suspend fun syncDeletedNotes() = withContext(Dispatchers.IO) {
        val deletedNotes = noteDao.getDeletedNotes()
        for (note in deletedNotes) {
            note.apiId?.let { apiId ->
                try {
                    val response = apiService.deleteNote(apiId).execute()
                    if (response.isSuccessful) {
                        noteDao.deleteNoteById(note.id)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    suspend fun syncNotesFromApiToRoom() = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getAllNotes().execute()
            if (response.isSuccessful) {
                response.body()?.let { notesFromApi ->
                    for (apiNote in notesFromApi) {
                        val localNote = noteDao.getNoteByApiId(apiNote.apiId.orEmpty())
                        if (localNote != null) {
                            noteDao.updateNote(
                                localNote.copy(
                                    title = apiNote.title,
                                    content = apiNote.content,
                                    updatedAt = apiNote.updatedAt,
                                    isDeleted = apiNote.isDeleted,
                                    isPinned = apiNote.isPinned,
                                    reminderAt = apiNote.reminderAt,
                                    isSynced = true
                                )
                            )
                        } else {
                            noteDao.insertNote(
                                Note(
                                    apiId = apiNote.apiId,
                                    userId = apiNote.userId,
                                    title = apiNote.title,
                                    content = apiNote.content,
                                    createdAt = apiNote.createdAt,
                                    updatedAt = apiNote.updatedAt,
                                    isDeleted = apiNote.isDeleted,
                                    isPinned = apiNote.isPinned,
                                    reminderAt = apiNote.reminderAt,
                                    isSynced = true
                                )
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    suspend fun syncAllNotes() {
        try {
            syncOfflineNotes()
            syncDeletedNotes()
            syncNotesFromApiToRoom()
            noteDao.deleteUnsyncedNotesWithoutApiId()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }



}
