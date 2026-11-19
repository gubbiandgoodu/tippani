package com.example.data

import com.example.model.Note
import kotlinx.coroutines.flow.Flow

class NoteRepository(private val noteDao: NoteDao) {
    val activeNotes: Flow<List<Note>> = noteDao.getAllActiveNotes()
    val archivedNotes: Flow<List<Note>> = noteDao.getArchivedNotes()

    suspend fun getNote(id: Long): Note? = noteDao.getNoteById(id)
    suspend fun insert(note: Note): Long = noteDao.insert(note)
    suspend fun insertAll(notes: List<Note>) = noteDao.insertAll(notes)
    suspend fun update(note: Note) = noteDao.update(note)
    suspend fun delete(note: Note) = noteDao.delete(note)
    suspend fun deleteById(id: Long) = noteDao.deleteById(id)
    suspend fun clearAll() = noteDao.clearAll()
    suspend fun getAllDirect(): List<Note> = noteDao.getAllNotesDirect()
}
