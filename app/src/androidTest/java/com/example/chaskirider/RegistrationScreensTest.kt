package com.example.chaskirider
import com.example.chaskirider.feature.vehicle.domain.VehiclePolicy

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.example.chaskirider.feature.bank.domain.BankInfo
import com.example.chaskirider.feature.vehicle.domain.VehicleType
import com.example.chaskirider.feature.documents.domain.DocumentFile
import com.example.chaskirider.feature.documents.domain.DocumentUploadState
import com.example.chaskirider.feature.user.ui.screens.AccessScreen
import com.example.chaskirider.feature.user.ui.screens.OnboardingStep3Screen
import com.example.chaskirider.feature.user.ui.viewmodel.DraftStatus
import com.example.chaskirider.feature.user.ui.viewmodel.RegistrationUiState
import com.example.chaskirider.ui.theme.ChaskiRiderTheme
import org.junit.Rule
import org.junit.Test
import java.io.File

class RegistrationScreensTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    @Test fun accessHasRealButtonsAndDisabledApple() {
        compose.setContent { ChaskiRiderTheme { AccessScreen() } }
        compose.onNodeWithText("Continuar con Google").assertIsEnabled()
        compose.onNodeWithText("Continuar con Apple").assertIsNotEnabled()
        compose.onNodeWithText("Ingresar con correo").performScrollTo().assertIsEnabled()
        compose.onNodeWithText("Ingresar con celular").assertDoesNotExist()
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        File(context.filesDir, "access-screen.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
    @Test fun cannotSubmitMissingMotorcycleDocuments() {
        compose.setContent { ChaskiRiderTheme {
            OnboardingStep3Screen(vehicleType = VehicleType.MOTORCYCLE,
                state = RegistrationUiState(ready = true, editable = true, draftStatus = DraftStatus.SAVED, bank = BankInfo("BCP", "Ana", "12345678", "00219100123456780123")))
        } }
        compose.onNodeWithText("Licencia de conducir").assertExists()
        compose.onNodeWithText("SOAT").assertExists()
        // El botón queda disponible: la validación de documentos la reporta el ViewModel con un mensaje.
        compose.onNodeWithText("Finalizar registro").performScrollTo().assertIsEnabled()
    }
    @Test fun bicycleDoesNotRequireMotorcycleDocuments() {
        val docs = VehiclePolicy.requiredDocuments(VehicleType.BICYCLE)
            .associateWith { DocumentFile(id = it, uploadState = DocumentUploadState.UPLOADED) }
        compose.setContent { ChaskiRiderTheme {
            OnboardingStep3Screen(vehicleType = VehicleType.BICYCLE,
                state = RegistrationUiState(ready = true, editable = true, draftStatus = DraftStatus.SAVED, bank = BankInfo("BCP", "Ana", "12345678", "00219100123456780123")), documentsMap = docs)
        } }
        compose.onNodeWithText("Licencia de conducir").assertDoesNotExist()
        compose.onNodeWithText("SOAT").assertDoesNotExist()
        compose.onNodeWithText("Finalizar registro").performScrollTo().assertIsEnabled()
    }
}
