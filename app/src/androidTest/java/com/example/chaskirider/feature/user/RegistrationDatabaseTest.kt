package com.example.chaskirider.feature.user
import com.example.chaskirider.database.openChaskiDatabase
import com.example.chaskirider.feature.user.data.local.RoomRegistrationDraftRepository

import androidx.test.platform.app.InstrumentationRegistry
import com.example.chaskirider.feature.user.domain.RegistrationDraft
import com.example.chaskirider.feature.user.domain.PendingDocumentCapture
import com.example.chaskirider.feature.bank.domain.BankInfo
import com.example.chaskirider.feature.vehicle.domain.VehicleType
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class RegistrationDatabaseTest {
    @Test fun diskDraftAndCaptureSurviveDatabaseReopenWithoutMixingUsers(): Unit = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "registration-test-${UUID.randomUUID()}.db"
        fun open() = openChaskiDatabase(context, name, importLegacy = false)
        var db = open()
        try {
            val draft = RegistrationDraft("ana", name = "Ana", personalDirty = true,
                vehicle = VehicleType.BICYCLE, vehicleDirty = true,
                bank = BankInfo("BCP", "Ana", "123", "00219100123456780123"), bankDirty = true)
            val capture = PendingDocumentCapture("ana", "dniFront", "content://example/capture.jpg")
            RoomRegistrationDraftRepository(db.registrationDrafts()).apply {
                save(draft); saveCapture(capture)
                save(RegistrationDraft("bob", name = "Bob", personalDirty = true))
            }
            db.close(); db = open()
            RoomRegistrationDraftRepository(db.registrationDrafts()).apply {
                assertEquals(draft, load("ana")); assertEquals(capture, loadCapture("ana"))
                assertEquals("Bob", load("bob")!!.name); assertNull(loadCapture("bob"))
                clear("ana"); clearCapture("ana")
                assertNull(load("ana")); assertNull(loadCapture("ana"))
                assertNotNull(load("bob"))
            }
        } finally { db.close(); context.deleteDatabase(name) }
    }
}
