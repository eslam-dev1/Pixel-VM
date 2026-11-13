package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.NoteEntity
import com.example.data.PixelSimDatabase
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class NotesPersistenceTest {

    private lateinit var context: Context
    private lateinit var dbFile: File
    private var db: PixelSimDatabase? = null

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        dbFile = File(context.filesDir, "test_notes.db")
        if (dbFile.exists()) dbFile.delete()
        db = Room.databaseBuilder(context, PixelSimDatabase::class.java, dbFile.absolutePath).build()
    }

    @After
    fun tearDown() {
        db?.close()
        if (dbFile.exists()) dbFile.delete()
    }

    @Test
    fun testNotesCrudAndPersistenceAcrossReopen() = runBlocking {
        val noteDao = db!!.noteDao()

        // 1. Create (Insert)
        val note1 = NoteEntity(title = "Shopping List", content = "Apples, Milk, Bread", isPinned = true)
        val id1 = noteDao.insertNote(note1)
        assertTrue(id1 > 0)

        val note2 = NoteEntity(title = "Idea", content = "Simulate native phone OS", isPinned = false)
        val id2 = noteDao.insertNote(note2)
        assertTrue(id2 > 0)

        // 2. Read
        val fetched1 = noteDao.getNoteById(id1)
        assertNotNull(fetched1)
        assertEquals("Shopping List", fetched1?.title)
        assertTrue(fetched1?.isPinned == true)

        // 3. Update
        val updatedNote1 = fetched1!!.copy(title = "Supermarket List")
        noteDao.updateNote(updatedNote1)
        val reFetched1 = noteDao.getNoteById(id1)
        assertEquals("Supermarket List", reFetched1?.title)

        // 4. Close database
        db!!.close()
        db = null

        // 5. Reopen database on the same file
        val reopenedDb = Room.databaseBuilder(context, PixelSimDatabase::class.java, dbFile.absolutePath).build()
        db = reopenedDb

        val reopenedNoteDao = reopenedDb.noteDao()
        val persistedNote = reopenedNoteDao.getNoteById(id1)
        assertNotNull("Note should survive DB close and reopen", persistedNote)
        assertEquals("Supermarket List", persistedNote?.title)
        assertEquals("Apples, Milk, Bread", persistedNote?.content)

        // 6. Delete
        reopenedNoteDao.deleteNote(persistedNote!!)
        val deletedNote = reopenedNoteDao.getNoteById(id1)
        assertNull("Note should be deleted", deletedNote)
    }
}
