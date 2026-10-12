package com.example.chaskirider

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.example.chaskirider.domain.model.*
import com.example.chaskirider.ui.screens.auth.*
import com.example.chaskirider.ui.screens.onboarding.*
import com.example.chaskirider.ui.theme.ChaskiRiderTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File

class RegistrationReviewScreensTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private fun screenshot(name: String) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        File(context.getExternalFilesDir(null), name).outputStream().use {
            compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }

    @Test fun rejectedRegistrationExplainsReasonAndOffersNoCorrectionOrWork() {
        compose.setContent { ChaskiRiderTheme {
            RegistrationStatusScreen(RegistrationReviewUiState(RiderUser(status = RegistrationStatus.REJECTED,
                rejectionReason = "La identidad no cumple los requisitos")))
        } }
        compose.onNodeWithText("Registro no aprobado").assertExists()
        compose.onNodeWithText("La identidad no cumple los requisitos").assertExists()
        compose.onNodeWithText("Corregir información").assertDoesNotExist()
        compose.onNodeWithText("Continuar registro").assertDoesNotExist()
        compose.onNodeWithText("Ir al panel principal").assertDoesNotExist()
        compose.onNodeWithText("Actualizar estado").assertDoesNotExist()
        screenshot("registration-rejected.png")
    }

    @Test fun correctableRegistrationShowsObservedDocumentAndAllowsResume() {
        var resumed = false
        compose.setContent { ChaskiRiderTheme {
            RegistrationStatusScreen(RegistrationReviewUiState(RiderUser(status = RegistrationStatus.NEEDS_CORRECTION,
                vehicleType = VehicleType.BICYCLE, rejectionReason = "Vuelve a tomar la foto del DNI",
                documentReviews = mapOf("dniFront" to DocumentReview(DocumentReviewStatus.OBSERVED, "El número no es legible")))),
                onResumeRegistrationClick = { resumed = true })
        } }
        compose.onNodeWithText("El número no es legible").assertExists()
        compose.onNodeWithText("Corregir información").performScrollTo().performClick()
        assertTrue(resumed)
        screenshot("registration-correction.png")
    }

    @Test fun pendingRegistrationCanRefreshButCannotEdit() {
        var refreshed = false
        compose.setContent { ChaskiRiderTheme {
            RegistrationStatusScreen(RegistrationReviewUiState(RiderUser(status = RegistrationStatus.PENDING_REVIEW)),
                onRefresh = { refreshed = true })
        } }
        compose.onNodeWithText("Corregir información").assertDoesNotExist()
        compose.onNodeWithText("Actualizar estado").performScrollTo().performClick()
        assertTrue(refreshed)
        screenshot("registration-pending.png")
    }

    @Test fun disabledAccountDoesNotPresentPendingReviewAsTheNextAction() {
        compose.setContent { ChaskiRiderTheme {
            RegistrationStatusScreen(RegistrationReviewUiState(RiderUser(status = RegistrationStatus.PENDING_REVIEW, isEnabled = false)))
        } }
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        compose.onNodeWithText(context.getString(R.string.registration_disabled_title)).assertExists()
        compose.onNodeWithText(context.getString(R.string.registration_pending_title)).assertDoesNotExist()
        compose.onNodeWithText("Continuar registro").assertDoesNotExist()
    }

    @Test fun uploadedObservedDocumentPreventsSendingAgain() {
        val documents = RegistrationValidation.requiredDocuments(VehicleType.BICYCLE)
            .associateWith { DocumentFile(id = it, uploadState = DocumentUploadState.UPLOADED) }
        compose.setContent { ChaskiRiderTheme {
            OnboardingStep3Screen(VehicleType.BICYCLE, RegistrationUiState(ready = true, editable = true,
                draftStatus = DraftStatus.SAVED, registrationStatus = RegistrationStatus.NEEDS_CORRECTION,
                reviewReason = "Reemplaza el DNI", bank = BankInfo("BCP", "Ana", "123", "00219100123456780123"),
                documentReviews = mapOf("dniFront" to DocumentReview(DocumentReviewStatus.OBSERVED, "Borrosa"))),
                documentsMap = documents)
        } }
        compose.onNodeWithText("Documento observado").assertExists()
        compose.onNodeWithText("Volver a enviar a revisión").performScrollTo().assertIsNotEnabled()
    }

    @Test fun firestoreCanDecodeRejectedProfileAndNestedDocumentReview() {
        val raw = mapOf("id" to "ana", "status" to "REJECTED", "submissionVersion" to 2L,
            "documentReviews" to mapOf("dniFront" to mapOf("status" to "OBSERVED", "reason" to "Borrosa")))
        val reference = com.google.firebase.firestore.FirebaseFirestore.getInstance().document("riders/ana")
        val user = com.google.firebase.firestore.util.CustomClassMapper.convertToCustomClass(raw, RiderUser::class.java, reference)
        assertEquals(RegistrationStatus.REJECTED, user.status)
        assertEquals(DocumentReviewStatus.OBSERVED, user.documentReviews["dniFront"]!!.status)
        assertEquals(2L, user.submissionVersion)
    }
}
