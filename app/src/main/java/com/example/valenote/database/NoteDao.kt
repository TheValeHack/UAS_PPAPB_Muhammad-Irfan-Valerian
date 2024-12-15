package com.example.valenote.database

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.valenote.database.models.Note

@Dao
interface NoteDao {

    @Query("SELECT * FROM notes WHERE userId = :userId AND isDeleted = 0 ORDER BY createdAt DESC")
    fun getAllNotesForUser(userId: String): LiveData<List<Note>>

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getNoteById(id: Int): Note?

    @Query("SELECT * FROM notes WHERE apiId = :apiId")
    suspend fun getNoteByApiId(apiId: String): Note?

    @Query("SELECT * FROM notes WHERE isSynced = 0 AND isDeleted = 0")
    suspend fun getUnsyncedNotes(): List<Note>

    @Query("SELECT * FROM notes WHERE isDeleted = 1")
    suspend fun getDeletedNotes(): List<Note>

    @Query("SELECT * FROM notes WHERE userId = :userId AND (title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%') AND isDeleted = 0 ORDER BY createdAt DESC")
    fun searchNotes(userId: String, query: String): LiveData<List<Note>>

    @Query("UPDATE notes SET isPinned = :isPinned WHERE id = :id")
    suspend fun updatePinStatus(id: Int, isPinned: Boolean)


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: Note): Long

    @Update
    suspend fun updateNote(note: Note): Int

    @Delete
    suspend fun deleteNote(note: Note): Int

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteNoteById(id: Int)

    @Query("DELETE FROM notes WHERE apiId IS NULL AND isSynced = 0")
    suspend fun deleteUnsyncedNotesWithoutApiId()


    @Transaction
    suspend fun insertOrUpdate(notes: List<Note>) {
        for (note in notes) {
            val existingNote = note.apiId?.let { getNoteByApiId(it) }
            if (existingNote != null) {
                updateNote(existingNote.copy(
                    title = note.title,
                    content = note.content,
                    updatedAt = note.updatedAt,
                    isDeleted = note.isDeleted,
                    isPinned = note.isPinned,
                    reminderAt = note.reminderAt,
                    isSynced = true
                ))
            } else {
                insertNote(note)
            }
        }
    }
}
