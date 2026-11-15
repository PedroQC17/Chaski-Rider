package com.example.chaskirider.feature.rider
import com.example.chaskirider.R
import com.example.chaskirider.feature.rider.domain.VehiclePolicy

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.example.chaskirider.feature.rider.domain.Rider
import com.example.chaskirider.feature.rider.domain.RiderStatus
import com.example.chaskirider.feature.bank.domain.BankAccount
import com.example.chaskirider.feature.rider.domain.VehicleType
import com.example.chaskirider.feature.documents.domain.DocumentFile
import com.example.chaskirider.feature.documents.domain.DocumentUploadState
import com.example.chaskirider.feature.documents.domain.DocumentReviewStatus
import com.example.chaskirider.feature.documents.domain.DocumentReview
import com.example.chaskirider.feature.rider.ui.screens.RiderStatusScreen
import com.example.chaskirider.feature.rider.ui.viewmodel.RiderOnboardingReviewUiState
import com.example.chaskirider.feature.rider.ui.screens.OnboardingStep3Screen
import com.example.chaskirider.feature.rider.ui.viewmodel.DraftStatus
import com.example.chaskirider.feature.rider.ui.viewmodel.RiderOnboardingUiState
import com.example.chaskirider.ui.theme.ChaskiRiderTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File

class RiderOnboardingReviewScreensTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private fun screenshot(name: String) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        File(context.getExternalFilesDir(null), name).outputStream().use {
            compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }

    @Test fun rejectedRiderOnboardingExplainsReasonAndOffersNoCorrectionOrWork() {
        compose.setContent { ChaskiRiderTheme {
            RiderStatusScreen(RiderOnboardingReviewUiState(Rider(status = RiderStatus.REJECTED,
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

    @Test fun correctableRiderOnboardingShowsObservedDocumentAndAllowsResume() {
        var resumed = false
        compose.setContent { ChaskiRiderTheme {
            RiderStatusScreen(RiderOnboardingReviewUiState(
                    user = Rider(status = RiderStatus.NEEDS_CORRECTION,
                        vehicleTypeId = VehicleType.BICYCLE.name, rejectionReason = "Vuelve a tomar la foto del DNI"),
                    documentReviews = mapOf("dniFront" to DocumentReview(DocumentReviewStatus.OBSERVED, "El número no es legible"))),
                onResumeRiderOnboardingClick = { resumed = true })
        } }
        compose.onNodeWithText("El número no es legible").assertExists()
        compose.onNodeWithText("Corregir información").performScrollTo().performClick()
        assertTrue(resumed)
        screenshot("registration-correction.png")
    }

    @Test fun pendingRiderOnboardingCanRefreshButCannotEdit() {
        var refreshed = false
        compose.setContent { ChaskiRiderTheme {
            RiderStatusScreen(RiderOnboardingReviewUiState(Rider(status = RiderStatus.PENDING_REVIEW)),
                onRefresh = { refreshed = true })
        } }
        compose.onNodeWithText("Corregir información").assertDoesNotExist()
        compose.onNodeWithText("Actualizar estado").performScrollTo().performClick()
        assertTrue(refreshed)
        screenshot("registration-pending.png")
    }

    @Test fun disabledAccountDoesNotPresentPendingReviewAsTheNextAction() {
        compose.setContent { ChaskiRiderTheme {
            RiderStatusScreen(RiderOnboardingReviewUiState(Rider(status = RiderStatus.PENDING_REVIEW, isEnabled = false)))
        } }
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        compose.onNodeWithText(context.getString(R.string.registration_disabled_title)).assertExists()
        compose.onNodeWithText(context.getString(R.string.registration_pending_title)).assertDoesNotExist()
        compose.onNodeWithText("Continuar registro").assertDoesNotExist()
    }

    @Test fun uploadedObservedDocumentPreventsSendingAgain() {
        val documents = VehiclePolicy.requiredDocuments(VehicleType.BICYCLE)
            .associateWith { DocumentFile(id = it, uploadState = DocumentUploadState.UPLOADED) }
        compose.setContent { ChaskiRiderTheme {
            OnboardingStep3Screen(VehicleType.BICYCLE, RiderOnboardingUiState(ready = true, editable = true,
                draftStatus = DraftStatus.SAVED, registrationStatus = RiderStatus.NEEDS_CORRECTION,
                reviewReason = "Reemplaza el DNI", bank = BankAccount("BCP", "Ana", "123", "00219100123456780123"),
                documentReviews = mapOf("dniFront" to DocumentReview(DocumentReviewStatus.OBSERVED, "Borrosa"))),
                documentsMap = documents)
        } }
        compose.onNodeWithText("Documento observado").assertExists()
        compose.onNodeWithText("Volver a enviar a revisión").performScrollTo().assertIsNotEnabled()
    }

    @Test fun firestoreCanDecodeRejectedProfileAndNestedDocumentReview() {
        val raw = mapOf("id" to "ana", "status" to "REJECTED", "submissionVersion" to 2L)
        val reference = com.google.firebase.firestore.FirebaseFirestore.getInstance().document("riders/ana")
        val user = com.google.firebase.firestore.util.CustomClassMapper.convertToCustomClass(raw, Rider::class.java, reference)
        assertEquals(RiderStatus.REJECTED, user.status)
        assertEquals(2L, user.submissionVersion)
    }
}
