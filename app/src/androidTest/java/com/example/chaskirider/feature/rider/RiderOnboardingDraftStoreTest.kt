package com.example.chaskirider.feature.rider
import com.example.chaskirider.feature.rider.data.local.StoredRiderOnboardingDraftRepository

import androidx.test.platform.app.InstrumentationRegistry
import com.example.chaskirider.feature.rider.domain.RiderOnboardingDraft
import com.example.chaskirider.feature.rider.domain.PendingDocumentCapture
import com.example.chaskirider.feature.bank.domain.BankAccount
import com.example.chaskirider.feature.rider.domain.VehicleType
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class RiderOnboardingDraftStoreTest {
    @Test fun draftAndCaptureSurviveStoreRecreationWithoutMixingUsers(): Unit = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        context.getSharedPreferences("rider_onboarding", 0).edit().clear().commit()
        val suffix = UUID.randomUUID().toString()
        val ana = "ana-$suffix"
        val bob = "bob-$suffix"
        val draft = RiderOnboardingDraft(ana, name = "Ana", personalDirty = true,
            vehicle = VehicleType.BICYCLE, vehicleDirty = true,
            bank = BankAccount("BCP", "Ana", "123", "00219100123456780123"), bankDirty = true)
        val capture = PendingDocumentCapture(ana, "dniFront", "content://example/capture.jpg")
        StoredRiderOnboardingDraftRepository(context).apply {
            save(draft); saveCapture(capture)
            save(RiderOnboardingDraft(bob, name = "Bob", personalDirty = true))
        }
        StoredRiderOnboardingDraftRepository(context).apply {
            assertEquals(draft, load(ana)); assertEquals(capture, loadCapture(ana))
            assertEquals("Bob", load(bob)!!.name); assertNull(loadCapture(bob))
            clear(ana); clearCapture(ana)
            assertNull(load(ana)); assertNull(loadCapture(ana))
            assertNotNull(load(bob))
        }
    }
}
