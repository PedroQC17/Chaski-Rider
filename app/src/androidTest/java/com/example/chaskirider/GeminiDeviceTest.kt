package com.example.chaskirider

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.example.chaskirider.data.chat.*
import com.example.chaskirider.data.support.*
import com.example.chaskirider.domain.model.*
import com.example.chaskirider.domain.repository.RiderSession
import com.example.chaskirider.domain.support.*
import com.example.chaskirider.ui.screens.support.*
import com.example.chaskirider.ui.theme.ChaskiRiderTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.tasks.await
import org.junit.*
import org.junit.Assert.*
import java.util.UUID

class GeminiDeviceTest {
    @get:Rule val compose = createComposeRule()

    @Test fun realGeminiConversationPersistsAndCanBeHandedToHumanSupport() = runBlocking {
        Assume.assumeTrue("Consume una consulta real de Gemini solo con autorización explícita",
            InstrumentationRegistry.getArguments().getString("testGeminiLive") == "true")
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        assertNotNull(uid)
        val owner = uid!!
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val id = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val remote = FirebaseFirestore.getInstance().document("riders/$owner/deliveryRecords/$id")
        remote.set(mapOf("orderId" to id, "riderId" to owner, "status" to "ON_THE_WAY", "revision" to 1,
            "merchantName" to "Prueba técnica Gemini", "pickup" to mapOf("latitude" to -12.04, "longitude" to -77.03),
            "destination" to mapOf("latitude" to -12.05, "longitude" to -77.04), "acceptedAt" to now-3000,
            "arrivedAt" to now-2000, "pickedUpAt" to now-1000, "deliveredAt" to null, "cancelledAt" to null,
            "earningsQuote" to mapOf("approachMeters" to 1000, "deliveryMeters" to 1000, "chaskiCents" to 200,
                "tipCents" to 50, "totalCents" to 250, "pricingVersion" to "PEN_1_KM_V1", "distanceSource" to "SIMULATED_GEODESIC"),
            "updatedAt" to FieldValue.serverTimestamp())).await()
        val repository = FirebaseAssistantRepository(FirebaseSupportRepository())
        val database = Room.inMemoryDatabaseBuilder(context, ChatDraftDatabase::class.java).build()
        val drafts = RoomChatDraftStore(database.drafts())
        val holder = ViewModelStore()
        val session = object : RiderSession { override val user = MutableStateFlow<RiderUser?>(RiderUser(id = owner)) }
        lateinit var model: AssistantViewModel
        try {
            compose.runOnIdle {
                model = ViewModelProvider(holder, viewModelFactory { initializer {
                    AssistantViewModel(repository, drafts, session) { error("No debe finalizar un pedido sin requisitos") }
                } })[AssistantViewModel::class.java]
                model.bind(id)
            }
            compose.setContent { ChaskiRiderTheme {
                val state by model.uiState.collectAsState()
                AssistantDialog(state, model::changeBody, { model.send() }, { model.send(human = true) },
                    { model.resolve {} }, {}, {})
            } }
            compose.waitUntil(15000) { !model.uiState.value.loading }
            compose.onNodeWithText("Escribe tu consulta").performScrollTo().performTextInput("El cliente no responde. ¿Cuáles son los pasos antes de finalizar?")
            compose.onNodeWithText("Enviar").performScrollTo().performClick()
            compose.waitUntil(60000) { !model.uiState.value.busy && model.uiState.value.messages.size >= 2 }
            assertEquals("GEMINI", model.uiState.value.mode)
            assertFalse(model.uiState.value.canResolve)
            compose.onNodeWithText("Gemini · Asistente Chaski").assertExists()
            val messages = withTimeout(15000) { repository.messages(owner,id).first { it.size >= 2 } }
            assertEquals(2,messages.size)
            val first = messages.first { it.sender == SupportSender.RIDER }
            repository.send(owner,id,first.id.removeSuffix("_rider"),first.body,false)
            assertEquals(2,withTimeout(15000) { repository.messages(owner,id).first { it.size >= 2 } }.size)
            compose.onNodeWithText("Solicitar asesor humano").performScrollTo().performClick()
            compose.waitUntil(60000) { !model.uiState.value.busy && model.uiState.value.needsHuman }
            val ticket = FirebaseFirestore.getInstance().document("riders/$owner/supportTickets/absence_$id").get(Source.SERVER).await()
            assertEquals(true,ticket.getBoolean("needsHuman"));assertEquals("OPEN",ticket.getString("status"))
            assertEquals("ON_THE_WAY",remote.get(Source.SERVER).await().getString("status"))
            assertTrue(model.uiState.value.messages.any { it.mode == "GEMINI" })
        } finally { compose.runOnIdle { holder.clear() };database.close() }
    }
}
