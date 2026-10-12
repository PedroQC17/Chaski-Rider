package com.example.chaskirider

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.example.chaskirider.data.local.*
import com.example.chaskirider.domain.model.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class RegistrationDatabaseTest {
    @Test fun diskDraftAndCaptureSurviveDatabaseReopenWithoutMixingUsers(): Unit = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "registration-test-${UUID.randomUUID()}.db"
        fun open() = Room.databaseBuilder(context, RegistrationDatabase::class.java, name).build()
        var db = open()
        try {
            val draft = RegistrationDraft("ana", name = "Ana", personalDirty = true,
                vehicle = VehicleType.BICYCLE, vehicleDirty = true,
                bank = BankInfo("BCP", "Ana", "123", "00219100123456780123"), bankDirty = true)
            val capture = PendingDocumentCapture("ana", "dniFront", "content://example/capture.jpg")
            RoomRegistrationDraftRepository(db.drafts()).apply {
                save(draft); saveCapture(capture)
                save(RegistrationDraft("bob", name = "Bob", personalDirty = true))
            }
            db.close(); db = open()
            RoomRegistrationDraftRepository(db.drafts()).apply {
                assertEquals(draft, load("ana")); assertEquals(capture, loadCapture("ana"))
                assertEquals("Bob", load("bob")!!.name); assertNull(loadCapture("bob"))
                clear("ana"); clearCapture("ana")
                assertNull(load("ana")); assertNull(loadCapture("ana"))
                assertNotNull(load("bob"))
            }
        } finally { db.close(); context.deleteDatabase(name) }
    }
}
